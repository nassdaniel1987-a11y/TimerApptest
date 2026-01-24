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
                val activeTimers = timers.value.filter { !it.is_completed }
                alarmScheduler.rescheduleAllAlarms(activeTimers)
                Log.d("TimerViewModel", "✅ Alarme neu geplant (debounced): ${activeTimers.size} Timer")
            } catch (e: Exception) {
                Log.e("TimerViewModel", "❌ Fehler beim Reschedule: ${e.message}")
            }
        }
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

            // ✅ NEU: Alle Alarme neu gruppieren und planen
            val activeTimers = timers.value.filter { !it.is_completed }
            alarmScheduler.rescheduleAllAlarms(activeTimers)

            // ✅ Widget-Cache aktualisieren (mit Delay für StateFlow-Propagierung)
            updateWidgetCache()

            _isLoading.value = false
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
