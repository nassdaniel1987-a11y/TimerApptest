package com.example.timerapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timerapp.models.Category
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.models.Timer
import com.example.timerapp.models.TimerTemplate
import com.example.timerapp.repository.TimerRepository
import com.example.timerapp.utils.AlarmScheduler
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
            repository.refreshCategories()
            repository.refreshTemplates()
            repository.refreshQRCodes()

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

            _isLoading.value = false
        }
    }

    // Timer Operations
    fun createTimer(timer: Timer) {
        viewModelScope.launch {
            alarmMutex.withLock {
                try {
                    val createdTimer = repository.createTimer(timer)
                    if (createdTimer != null) {
                        repository.refreshTimers()
                        // ✅ Performance: Debounced Reschedule
                        debouncedRescheduleAlarms()
                    } else {
                        setError("Timer konnte nicht erstellt werden. Bitte Internetverbindung prüfen.")
                    }
                } catch (e: Exception) {
                    setError("Fehler beim Erstellen des Timers: ${e.message}")
                }
            }
        }
    }

    fun updateTimer(id: String, timer: Timer) {
        viewModelScope.launch {
            alarmMutex.withLock {
                repository.updateTimer(id, timer)
                // ✅ Performance: Debounced Reschedule
                debouncedRescheduleAlarms()
            }
        }
    }

    fun deleteTimer(id: String) {
        viewModelScope.launch {
            alarmMutex.withLock {
                try {
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
                            alarmScheduler.cancelAlarm(id)  // Timer-ID basiert
                            alarmScheduler.cancelAlarm("${id}_pre")  // Timer-ID Pre-Reminder
                            alarmScheduler.cancelGroupAlarm(groupId)  // Gruppen-Alarm

                            Log.d("TimerViewModel", "🔕 Alle Alarme abgebrochen für Timer $id (Gruppe: $groupId)")
                        } catch (e: Exception) {
                            Log.e("TimerViewModel", "⚠️ Fehler beim Parsen der Timer-Zeit: ${e.message}")
                            // Versuche trotzdem Timer-basierte Alarme zu löschen
                            alarmScheduler.cancelAlarm(id)
                            alarmScheduler.cancelAlarm("${id}_pre")
                        }
                    } else {
                        // Timer nicht gefunden, versuche trotzdem ID-basierte Alarme zu löschen
                        Log.w("TimerViewModel", "⚠️ Timer nicht gefunden, lösche trotzdem Alarme: $id")
                        alarmScheduler.cancelAlarm(id)
                        alarmScheduler.cancelAlarm("${id}_pre")
                    }

                    // Dann Timer aus der Datenbank löschen
                    repository.deleteTimer(id)

                    // WICHTIG: Warte bis refreshTimers() fertig ist
                    // Dies stellt sicher, dass der gelöschte Timer nicht mehr in timers.value ist
                    repository.refreshTimers()

                    // ✅ Performance: Debounced Reschedule
                    debouncedRescheduleAlarms()

                    Log.d("TimerViewModel", "✅ Timer erfolgreich gelöscht: $id")
                } catch (e: Exception) {
                    Log.e("TimerViewModel", "❌ Fehler beim Löschen des Timers: ${e.message}", e)
                }
            }
        }
    }

    fun markTimerCompleted(id: String) {
        viewModelScope.launch {
            alarmMutex.withLock {
                // ✅ Prüfe ob Timer eine Wiederholung hat
                val timer = timers.value.find { it.id == id }

                repository.markTimerCompleted(id)
                alarmScheduler.cancelAlarm(id)

                // ✅ Wenn Timer wiederholt werden soll, erstelle nächste Instanz
                if (timer != null && timer.recurrence != null) {
                    val nextTimer = alarmScheduler.calculateNextOccurrence(timer)
                    if (nextTimer != null) {
                        val createdTimer = repository.createTimer(nextTimer)
                        if (createdTimer != null) {
                            repository.refreshTimers()
                            Log.d("TimerViewModel", "🔁 Wiederholender Timer erstellt: ${nextTimer.name}")
                        }
                    }
                }

                // ✅ Performance: Debounced Reschedule
                debouncedRescheduleAlarms()
            }
        }
    }

    // Category Operations
    fun createCategory(category: Category) {
        viewModelScope.launch {
            repository.createCategory(category)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    // Template Operations
    fun createTemplate(template: TimerTemplate) {
        viewModelScope.launch {
            repository.createTemplate(template)
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            repository.deleteTemplate(id)
        }
    }

    // QR Code Operations
    fun createQRCode(qrCode: QRCodeData) {
        viewModelScope.launch {
            val createdQRCode = repository.createQRCode(qrCode)
            createdQRCode?.let {
                repository.addQRCodeToLocalList(it)
            }
        }
    }

    fun deleteQRCode(id: String) {
        viewModelScope.launch {
            repository.deleteQRCode(id)
        }
    }
}