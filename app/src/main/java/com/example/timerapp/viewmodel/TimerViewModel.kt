package com.example.timerapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timerapp.models.Category
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.models.Result
import com.example.timerapp.models.Timer
import com.example.timerapp.models.TimerTemplate
import com.example.timerapp.models.onError
import com.example.timerapp.models.onSuccess
import com.example.timerapp.SettingsManager
import com.example.timerapp.repository.TimerRepository
import com.example.timerapp.utils.AlarmScheduler
import com.example.timerapp.widget.WidgetDataCache
import com.example.timerapp.widget.WidgetUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimerRepository()
    private val alarmScheduler = AlarmScheduler(application.applicationContext)
    private val settingsManager = SettingsManager.getInstance(application.applicationContext)

    // ✅ Mutex verhindert Race Conditions bei Timer-Operationen
    private val alarmMutex = Mutex()

    // ✅ Debouncing für rescheduleAllAlarms (Performance-Optimierung)
    private var rescheduleJob: Job? = null

    val timers: StateFlow<List<Timer>> = repository.timers
    val categories: StateFlow<List<Category>> = repository.categories
    val templates: StateFlow<List<TimerTemplate>> = repository.templates
    val qrCodes: StateFlow<List<QRCodeData>> = repository.qrCodes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ✅ Error-StateFlow für User-Feedback
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ✅ Undo-Delete: Timer-IDs die gerade "soft deleted" sind
    private val _pendingDeleteTimerIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingDeleteTimerIds: StateFlow<Set<String>> = _pendingDeleteTimerIds.asStateFlow()
    private val pendingDeleteJobs = mutableMapOf<String, Job>()

    init {
        sync()
    }

    // Hilfsfunktion zum Setzen von Fehlern
    private fun setError(message: String) {
        _error.value = message
        Log.e("TimerViewModel", "❌ Error: $message")
    }

    // Hilfsfunktion zum Löschen von Fehlern
    fun clearError() {
        _error.value = null
    }

    // ✅ Hilfsfunktion: Widget-Cache SOFORT aktualisieren
    private fun updateWidgetCache() {
        viewModelScope.launch {
            val currentTimers = timers.value
            Log.d("TimerViewModel", "🔄 Widget-Cache Update: ${currentTimers.size} Timer")

            // Cache aktualisieren
            WidgetDataCache.cacheTimers(getApplication(), currentTimers)

            // Widget aktualisieren
            WidgetUtils.updateWidgets(getApplication())

            Log.d("TimerViewModel", "✅ Widget aktualisiert!")
        }
    }

    // ✅ Debounced Reschedule - verhindert zu häufige Reschedule-Operationen
    // Wartet 500ms und bündelt mehrere Operationen
    private fun debouncedRescheduleAlarms() {
        rescheduleJob?.cancel()
        rescheduleJob = viewModelScope.launch {
            delay(500) // Warte 500ms
            try {
                val allActive = timers.value.filter { !it.is_completed }
                val klasseFilter = settingsManager.klasseFilter
                val toSchedule = if (klasseFilter != null) {
                    allActive.filter { it.klasse == klasseFilter }
                } else {
                    allActive
                }
                // Erst ALLE canceln, dann nur gefilterte neu planen
                alarmScheduler.rescheduleAllAlarms(allActive, toSchedule)
                Log.d("TimerViewModel", "✅ Alarme neu geplant: ${toSchedule.size}/${allActive.size} (Filter: ${klasseFilter ?: "Alle"})")
            } catch (e: Exception) {
                Log.e("TimerViewModel", "❌ Fehler beim Reschedule: ${e.message}")
            }
        }
    }

    // Klassen-Filter ändern und Alarme sofort neu planen
    fun updateKlasseFilter(klasse: String?) {
        settingsManager.klasseFilter = klasse
        debouncedRescheduleAlarms()
        Log.d("TimerViewModel", "🔄 Klassen-Filter geändert: ${klasse ?: "Alle"}")
    }

    fun sync() {
        viewModelScope.launch {
            _isLoading.value = true

            // Alte Timer-IDs merken (vor dem Refresh)
            val oldTimerIds = timers.value.map { it.id }.toSet()

            // Daten aus Supabase laden
            repository.refreshTimers()
                .onError { exception, retryable ->
                    setError(exception.message ?: "Fehler beim Laden der Timer")
                }

            repository.refreshCategories()
                .onError { exception, _ ->
                    Log.w("TimerViewModel", "Kategorien konnten nicht geladen werden: ${exception.message}")
                }

            repository.refreshTemplates()
                .onError { exception, _ ->
                    Log.w("TimerViewModel", "Templates konnten nicht geladen werden: ${exception.message}")
                }

            repository.refreshQRCodes()
                .onError { exception, _ ->
                    Log.w("TimerViewModel", "QR-Codes konnten nicht geladen werden: ${exception.message}")
                }

            // Neue Timer-IDs nach dem Refresh
            val newTimerIds = timers.value.map { it.id }.toSet()

            // Timer, die gelöscht wurden (in alten IDs, aber nicht in neuen)
            val deletedTimerIds = oldTimerIds - newTimerIds

            // Alarme für gelöschte Timer abbrechen
            deletedTimerIds.forEach { timerId ->
                alarmScheduler.cancelAlarm(timerId)
            }

            // ✅ NEU: Alle Alarme canceln, dann nur gefilterte Klasse neu planen
            val allActive = timers.value.filter { !it.is_completed }
            val klasseFilter = settingsManager.klasseFilter
            val toSchedule = if (klasseFilter != null) {
                allActive.filter { it.klasse == klasseFilter }
            } else {
                allActive
            }
            alarmScheduler.rescheduleAllAlarms(allActive, toSchedule)
            Log.d("TimerViewModel", "🔔 Alarme geplant: ${toSchedule.size}/${allActive.size} (Filter: ${klasseFilter ?: "Alle"})")

            // ✅ Auto-Aufräumen: Abgeschlossene Timer nach X Tagen löschen
            if (settingsManager.isAutoCleanupEnabled) {
                cleanupCompletedTimers()
            }

            // ✅ Widget-Cache aktualisieren (mit Delay für StateFlow-Propagierung)
            updateWidgetCache()

            _isLoading.value = false
        }
    }

    private suspend fun cleanupCompletedTimers() {
        try {
            val days = settingsManager.autoCleanupDays
            val cutoff = java.time.ZonedDateTime.now().minusDays(days.toLong())
            val completedTimers = timers.value.filter { it.is_completed }

            var deletedCount = 0
            for (timer in completedTimers) {
                try {
                    val targetTime = java.time.ZonedDateTime.parse(
                        timer.target_time,
                        java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    )
                    if (targetTime.isBefore(cutoff)) {
                        repository.deleteTimer(timer.id)
                        deletedCount++
                    }
                } catch (e: Exception) {
                    Log.w("TimerViewModel", "⚠️ Fehler beim Cleanup von Timer ${timer.id}: ${e.message}")
                }
            }
            if (deletedCount > 0) {
                Log.d("TimerViewModel", "🧹 Auto-Cleanup: $deletedCount abgeschlossene Timer gelöscht (älter als $days Tage)")
            }
        } catch (e: Exception) {
            Log.e("TimerViewModel", "❌ Fehler beim Auto-Cleanup: ${e.message}")
        }
    }

    // Timer Operations
    fun createTimer(timer: Timer) {
        viewModelScope.launch {
            alarmMutex.withLock {
                repository.createTimer(timer)
                    .onSuccess { createdTimer ->
                        repository.refreshTimers()
                        debouncedRescheduleAlarms()
                        updateWidgetCache()
                        Log.d("TimerViewModel", "✅ Timer erfolgreich erstellt: ${createdTimer.name}")
                    }
                    .onError { exception, retryable ->
                        val message = if (retryable) {
                            "Timer konnte nicht erstellt werden. Bitte Internetverbindung prüfen."
                        } else {
                            "Fehler beim Erstellen des Timers: ${exception.message}"
                        }
                        setError(message)
                    }
            }
        }
    }

    fun updateTimer(id: String, timer: Timer) {
        viewModelScope.launch {
            alarmMutex.withLock {
                repository.updateTimer(id, timer)
                    .onSuccess {
                        // Repository ruft bereits refreshTimers() auf
                        debouncedRescheduleAlarms()
                        updateWidgetCache()
                        Log.d("TimerViewModel", "✅ Timer aktualisiert: $id")
                    }
                    .onError { exception, _ ->
                        setError("Fehler beim Aktualisieren: ${exception.message}")
                    }
            }
        }
    }

    fun deleteTimer(id: String) {
        viewModelScope.launch {
            alarmMutex.withLock {
                Log.d("TimerViewModel", "🗑️ Starte Löschen von Timer: $id")

                // Finde den Timer BEVOR er gelöscht wird, um seine Gruppe zu identifizieren
                val timerToDelete = timers.value.find { it.id == id }

                if (timerToDelete != null) {
                    try {
                        val targetTime = java.time.ZonedDateTime.parse(
                            timerToDelete.target_time,
                            java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        )
                        val groupId = "group_${targetTime.toLocalDate()}_${targetTime.hour}_${targetTime.minute}"

                        // Breche ALLE Alarm-Varianten ab
                        alarmScheduler.cancelAlarm(id)
                        alarmScheduler.cancelAlarm("${id}_pre")
                        alarmScheduler.cancelGroupAlarm(groupId)

                        Log.d("TimerViewModel", "🔕 Alle Alarme abgebrochen für Timer $id (Gruppe: $groupId)")
                    } catch (e: Exception) {
                        Log.e("TimerViewModel", "⚠️ Fehler beim Parsen der Timer-Zeit: ${e.message}")
                        alarmScheduler.cancelAlarm(id)
                        alarmScheduler.cancelAlarm("${id}_pre")
                    }
                } else {
                    Log.w("TimerViewModel", "⚠️ Timer nicht gefunden, lösche trotzdem Alarme: $id")
                    alarmScheduler.cancelAlarm(id)
                    alarmScheduler.cancelAlarm("${id}_pre")
                }

                // Dann Timer aus der Datenbank löschen
                // Repository ruft bereits refreshTimers() auf
                repository.deleteTimer(id)
                    .onSuccess {
                        debouncedRescheduleAlarms()
                        updateWidgetCache()
                        Log.d("TimerViewModel", "✅ Timer erfolgreich gelöscht: $id")
                    }
                    .onError { exception, _ ->
                        setError("Fehler beim Löschen: ${exception.message}")
                        Log.e("TimerViewModel", "❌ Fehler beim Löschen des Timers: ${exception.message}")
                    }
            }
        }
    }

    // ✅ Soft-Delete: Timer wird visuell ausgeblendet, tatsächliche Löschung nach 5s
    fun softDeleteTimer(id: String) {
        // Sofort aus der Anzeige entfernen
        _pendingDeleteTimerIds.value = _pendingDeleteTimerIds.value + id

        // Alarm sofort canceln
        viewModelScope.launch {
            alarmMutex.withLock {
                val timerToDelete = timers.value.find { it.id == id }
                if (timerToDelete != null) {
                    try {
                        val targetTime = java.time.ZonedDateTime.parse(
                            timerToDelete.target_time,
                            java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        )
                        val groupId = "group_${targetTime.toLocalDate()}_${targetTime.hour}_${targetTime.minute}"
                        alarmScheduler.cancelAlarm(id)
                        alarmScheduler.cancelAlarm("${id}_pre")
                        alarmScheduler.cancelGroupAlarm(groupId)
                    } catch (e: Exception) {
                        alarmScheduler.cancelAlarm(id)
                        alarmScheduler.cancelAlarm("${id}_pre")
                    }
                }
            }
        }

        // Tatsächliche Löschung nach 5 Sekunden (Snackbar-Dauer)
        val job = viewModelScope.launch {
            delay(5000)
            if (_pendingDeleteTimerIds.value.contains(id)) {
                _pendingDeleteTimerIds.value = _pendingDeleteTimerIds.value - id
                deleteTimer(id)
            }
        }
        pendingDeleteJobs[id] = job
    }

    // ✅ Undo: Soft-Delete rückgängig machen
    fun undoDeleteTimer(id: String) {
        pendingDeleteJobs[id]?.cancel()
        pendingDeleteJobs.remove(id)
        _pendingDeleteTimerIds.value = _pendingDeleteTimerIds.value - id

        // Alarme neu planen für den wiederhergestellten Timer
        viewModelScope.launch {
            debouncedRescheduleAlarms()
        }
        Log.d("TimerViewModel", "↩️ Timer-Löschung rückgängig gemacht: $id")
    }

    fun markTimerCompleted(id: String) {
        viewModelScope.launch {
            alarmMutex.withLock {
                val timer = timers.value.find { it.id == id }

                // Repository ruft bereits refreshTimers() auf
                repository.markTimerCompleted(id)
                    .onSuccess {
                        alarmScheduler.cancelAlarm(id)

                        // ✅ Wenn Timer wiederholt werden soll, erstelle nächste Instanz
                        if (timer != null && timer.recurrence != null) {
                            val nextTimer = alarmScheduler.calculateNextOccurrence(timer)
                            if (nextTimer != null) {
                                viewModelScope.launch {
                                    repository.createTimer(nextTimer)
                                        .onSuccess { created ->
                                            repository.refreshTimers()
                                            updateWidgetCache()
                                            Log.d("TimerViewModel", "🔁 Wiederholender Timer erstellt: ${created.name}")
                                        }
                                        .onError { exception, _ ->
                                            Log.e("TimerViewModel", "Fehler beim Erstellen des wiederkehrenden Timers: ${exception.message}")
                                        }
                                }
                            }
                        }

                        debouncedRescheduleAlarms()
                        updateWidgetCache()
                        Log.d("TimerViewModel", "✅ Timer abgeschlossen: $id")
                    }
                    .onError { exception, _ ->
                        setError("Fehler beim Abschließen: ${exception.message}")
                    }
            }
        }
    }

    // Category Operations
    fun createCategory(category: Category) {
        viewModelScope.launch {
            repository.createCategory(category)
                .onSuccess {
                    Log.d("TimerViewModel", "✅ Kategorie erstellt")
                }
                .onError { exception, _ ->
                    setError("Fehler beim Erstellen der Kategorie: ${exception.message}")
                }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
                .onSuccess {
                    Log.d("TimerViewModel", "✅ Kategorie gelöscht")
                }
                .onError { exception, _ ->
                    setError("Fehler beim Löschen der Kategorie: ${exception.message}")
                }
        }
    }

    // Template Operations
    fun createTemplate(template: TimerTemplate) {
        viewModelScope.launch {
            repository.createTemplate(template)
                .onSuccess {
                    Log.d("TimerViewModel", "✅ Template erstellt")
                }
                .onError { exception, _ ->
                    setError("Fehler beim Erstellen des Templates: ${exception.message}")
                }
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            repository.deleteTemplate(id)
                .onSuccess {
                    Log.d("TimerViewModel", "✅ Template gelöscht")
                }
                .onError { exception, _ ->
                    setError("Fehler beim Löschen des Templates: ${exception.message}")
                }
        }
    }

    // QR Code Operations
    fun createQRCode(qrCode: QRCodeData) {
        viewModelScope.launch {
            repository.createQRCode(qrCode)
                .onSuccess { createdQRCode ->
                    repository.addQRCodeToLocalList(createdQRCode)
                    Log.d("TimerViewModel", "✅ QR-Code erstellt")
                }
                .onError { exception, _ ->
                    setError("Fehler beim Erstellen des QR-Codes: ${exception.message}")
                }
        }
    }

    fun deleteQRCode(id: String) {
        viewModelScope.launch {
            repository.deleteQRCode(id)
                .onSuccess {
                    Log.d("TimerViewModel", "✅ QR-Code gelöscht")
                }
                .onError { exception, _ ->
                    setError("Fehler beim Löschen des QR-Codes: ${exception.message}")
                }
        }
    }
}
