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

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimerRepository()
    private val alarmScheduler = AlarmScheduler(application.applicationContext)

    val timers: StateFlow<List<Timer>> = repository.timers
    val categories: StateFlow<List<Category>> = repository.categories
    val templates: StateFlow<List<TimerTemplate>> = repository.templates
    val qrCodes: StateFlow<List<QRCodeData>> = repository.qrCodes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        sync()
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
            val createdTimer = repository.createTimer(timer)
            if (createdTimer != null) {
                repository.refreshTimers()
                // ✅ NEU: Alle Alarme neu gruppieren
                val activeTimers = timers.value.filter { !it.is_completed }
                alarmScheduler.rescheduleAllAlarms(activeTimers)
            }
        }
    }

    fun updateTimer(id: String, timer: Timer) {
        viewModelScope.launch {
            repository.updateTimer(id, timer)
            // ✅ NEU: Alle Alarme neu gruppieren
            val activeTimers = timers.value.filter { !it.is_completed }
            alarmScheduler.rescheduleAllAlarms(activeTimers)
        }
    }

    fun deleteTimer(id: String) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(id)
            repository.deleteTimer(id)
            // ✅ NEU: Alle Alarme neu gruppieren
            val activeTimers = timers.value.filter { !it.is_completed }
            alarmScheduler.rescheduleAllAlarms(activeTimers)
        }
    }

    fun markTimerCompleted(id: String) {
        viewModelScope.launch {
            // ✅ Prüfe ob Timer eine Wiederholung hat
            val timer = timers.value.find { it.id == id }

            repository.markTimerCompleted(id)
            alarmScheduler.cancelAlarm(id)

            // ✅ Wenn Timer wiederholt werden soll, erstelle nächste Instanz
            if (timer?.recurrence != null) {
                val nextTimer = alarmScheduler.calculateNextOccurrence(timer)
                if (nextTimer != null) {
                    val createdTimer = repository.createTimer(nextTimer)
                    if (createdTimer != null) {
                        repository.refreshTimers()
                        Log.d("TimerViewModel", "🔁 Wiederholender Timer erstellt: ${nextTimer.name}")
                    }
                }
            }

            // ✅ NEU: Alle Alarme neu gruppieren
            val activeTimers = timers.value.filter { !it.is_completed }
            alarmScheduler.rescheduleAllAlarms(activeTimers)
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