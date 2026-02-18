package com.example.timerapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timerapp.models.Category
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.models.Timer
import com.example.timerapp.models.TimerTemplate
import com.example.timerapp.models.onError
import com.example.timerapp.models.onSuccess
import com.example.timerapp.SettingsManager
import com.example.timerapp.repository.TimerRepository
import com.example.timerapp.sync.SyncManager
import com.example.timerapp.utils.AlarmScheduler
import com.example.timerapp.widget.TimerWidget
import com.example.timerapp.widget.WidgetDataCache
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    application: Application,
    private val repository: TimerRepository,
    private val alarmScheduler: AlarmScheduler,
    private val settingsManager: SettingsManager,
    private val syncManager: SyncManager
) : AndroidViewModel(application) {

    // Mutex verhindert Race Conditions bei Timer-Operationen
    private val alarmMutex = Mutex()

    // Debouncing für rescheduleAllAlarms
    private var rescheduleJob: Job? = null

    val timers: StateFlow<List<Timer>> = repository.timers
    val categories: StateFlow<List<Category>> = repository.categories
    val templates: StateFlow<List<TimerTemplate>> = repository.templates
    val qrCodes: StateFlow<List<QRCodeData>> = repository.qrCodes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error-StateFlow für User-Feedback
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Undo-Delete: Timer-IDs die gerade "soft deleted" sind
    private val _pendingDeleteTimerIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingDeleteTimerIds: StateFlow<Set<String>> = _pendingDeleteTimerIds.asStateFlow()
    private val pendingDeleteJobs = mutableMapOf<String, Job>()

    // Sync-Status für UI
    val pendingSyncCount: Flow<Int> = repository.pendingSyncCount
    val isOnline: StateFlow<Boolean> = syncManager.isOnline
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing

    init {
        // Room-Flows → StateFlows (automatische Updates bei DB-Änderungen)
        repository.observeAll(viewModelScope)
        // Server-Daten laden (falls online — falls offline bleiben Room-Daten erhalten)
        sync()
    }

    private fun setError(message: String) {
        _error.value = message
        Log.e("TimerViewModel", "❌ Error: $message")
    }

    fun clearError() {
        _error.value = null
    }

    // Widget-Cache + Dynamic Shortcuts aktualisieren
    private suspend fun updateWidgetCache() {
        val currentTimers = timers.value
        val app = getApplication<Application>()

        WidgetDataCache.cacheTimers(app, currentTimers)

        try {
            TimerWidget().updateAll(app)
        } catch (e: Exception) {
            Log.e("TimerViewModel", "Widget update failed: ${e.message}", e)
        }

        com.example.timerapp.shortcuts.ShortcutManagerHelper
            .updateDynamicShortcuts(app, currentTimers)
    }

    // Debounced Reschedule — verhindert zu häufige Reschedule-Operationen
    private fun debouncedRescheduleAlarms() {
        rescheduleJob?.cancel()
        rescheduleJob = viewModelScope.launch {
            delay(500)
            try {
                val allActive = timers.value.filter { !it.is_completed }
                val klasseFilter = settingsManager.klasseFilter
                val toSchedule = if (klasseFilter != null) {
                    allActive.filter { it.klasse == klasseFilter }
                } else {
                    allActive
                }
                alarmScheduler.rescheduleAllAlarms(allActive, toSchedule)
                Log.d("TimerViewModel", "✅ Alarme neu geplant: ${toSchedule.size}/${allActive.size} (Filter: ${klasseFilter ?: "Alle"})")
            } catch (e: Exception) {
                Log.e("TimerViewModel", "❌ Fehler beim Reschedule: ${e.message}")
            }
        }
    }

    fun updateKlasseFilter(klasse: String?) {
        settingsManager.klasseFilter = klasse
        debouncedRescheduleAlarms()
        Log.d("TimerViewModel", "🔄 Klassen-Filter geändert: ${klasse ?: "Alle"}")
    }

    fun sync() {
        viewModelScope.launch {
            _isLoading.value = true

            val oldTimerIds = timers.value.map { it.id }.toSet()

            // Zuerst ausstehende Sync-Operationen hochladen
            syncManager.processPendingSync()

            // Dann Server-Daten laden (bei Offline-Fehler bleiben Room-Daten erhalten)
            repository.refreshTimers()
                .onError { exception, _ ->
                    if (timers.value.isEmpty()) {
                        setError(exception.message ?: "Fehler beim Laden der Timer")
                    }
                    // Wenn Room-Daten vorhanden, kein Fehler anzeigen (Offline ok)
                }

            repository.refreshCategories()
            repository.refreshTemplates()
            repository.refreshQRCodes()

            val newTimerIds = timers.value.map { it.id }.toSet()
            val deletedTimerIds = oldTimerIds - newTimerIds

            deletedTimerIds.forEach { timerId ->
                alarmScheduler.cancelAlarm(timerId)
            }

            val allActive = timers.value.filter { !it.is_completed }
            val klasseFilter = settingsManager.klasseFilter
            val toSchedule = if (klasseFilter != null) {
                allActive.filter { it.klasse == klasseFilter }
            } else {
                allActive
            }
            alarmScheduler.rescheduleAllAlarms(allActive, toSchedule)
            Log.d("TimerViewModel", "🔔 Alarme geplant: ${toSchedule.size}/${allActive.size} (Filter: ${klasseFilter ?: "Alle"})")

            if (settingsManager.isAutoCleanupEnabled) {
                cleanupCompletedTimers()
            }

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

    // ── Timer Operations (Room-First) ──

    fun createTimer(timer: Timer) {
        viewModelScope.launch {
            alarmMutex.withLock {
                repository.createTimer(timer)
                    .onSuccess { createdTimer ->
                        // Room-Flow aktualisiert UI automatisch
                        updateWidgetCache()
                        debouncedRescheduleAlarms()
                        Log.d("TimerViewModel", "✅ Timer erstellt: ${createdTimer.name}")
                    }
                    .onError { exception, _ ->
                        setError("Fehler beim Erstellen des Timers: ${exception.message}")
                    }
            }
        }
    }

    fun updateTimer(id: String, timer: Timer) {
        viewModelScope.launch {
            alarmMutex.withLock {
                repository.updateTimer(id, timer)
                    .onSuccess {
                        updateWidgetCache()
                        debouncedRescheduleAlarms()
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
                } else {
                    alarmScheduler.cancelAlarm(id)
                    alarmScheduler.cancelAlarm("${id}_pre")
                }

                // Room löscht lokal → Flow aktualisiert UI + Sync-Queue
                repository.deleteTimer(id)
                    .onSuccess {
                        updateWidgetCache()
                        debouncedRescheduleAlarms()
                        Log.d("TimerViewModel", "✅ Timer gelöscht: $id")
                    }
                    .onError { exception, _ ->
                        setError("Fehler beim Löschen: ${exception.message}")
                    }
            }
        }
    }

    // Soft-Delete: Timer wird visuell ausgeblendet, tatsächliche Löschung nach 5s
    fun softDeleteTimer(id: String) {
        _pendingDeleteTimerIds.value = _pendingDeleteTimerIds.value + id

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

        val job = viewModelScope.launch {
            delay(5000)
            if (_pendingDeleteTimerIds.value.contains(id)) {
                _pendingDeleteTimerIds.value = _pendingDeleteTimerIds.value - id
                deleteTimer(id)
            }
        }
        pendingDeleteJobs[id] = job
    }

    fun undoDeleteTimer(id: String) {
        pendingDeleteJobs[id]?.cancel()
        pendingDeleteJobs.remove(id)
        _pendingDeleteTimerIds.value = _pendingDeleteTimerIds.value - id

        viewModelScope.launch {
            debouncedRescheduleAlarms()
        }
        Log.d("TimerViewModel", "↩️ Timer-Löschung rückgängig gemacht: $id")
    }

    fun markTimerCompleted(id: String) {
        viewModelScope.launch {
            alarmMutex.withLock {
                val timer = timers.value.find { it.id == id }

                alarmScheduler.cancelAlarm(id)

                // Room markiert als erledigt + Sync-Queue
                repository.markTimerCompleted(id)
                    .onSuccess {
                        updateWidgetCache()

                        // Wiederholung: nächste Instanz erstellen
                        if (timer != null && timer.recurrence != null) {
                            val nextTimer = alarmScheduler.calculateNextOccurrence(timer)
                            if (nextTimer != null) {
                                repository.createTimer(nextTimer)
                                    .onSuccess { created ->
                                        updateWidgetCache()
                                        Log.d("TimerViewModel", "🔁 Wiederholender Timer erstellt: ${created.name}")
                                    }
                            }
                        }

                        debouncedRescheduleAlarms()
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
