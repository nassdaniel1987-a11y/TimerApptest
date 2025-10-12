package com.example.timerapp.viewmodel

import android.app.Application
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
            repository.refreshTimers()
            repository.refreshCategories()
            repository.refreshTemplates()
            repository.refreshQRCodes()
            _isLoading.value = false
        }
    }

    // Timer Operations
    fun createTimer(timer: Timer) {
        viewModelScope.launch {
            // 1. Warte auf den in der DB erstellten Timer
            val createdTimer = repository.createTimer(timer)

            // 2. Wenn die Erstellung erfolgreich war (nicht null)...
            createdTimer?.let {
                // ...plane den Alarm mit dem korrekten Timer-Objekt...
                alarmScheduler.scheduleAlarm(it)
                // ...und aktualisiere danach die sichtbare Liste.
                repository.refreshTimers()
            }
        }
    }

    fun updateTimer(id: String, timer: Timer) {
        viewModelScope.launch {
            repository.updateTimer(id, timer)
            // Alarm neu setzen
            alarmScheduler.cancelAlarm(id)
            alarmScheduler.scheduleAlarm(timer.copy(id = id))
        }
    }

    fun deleteTimer(id: String) {
        viewModelScope.launch {
            // Alarm abbrechen
            alarmScheduler.cancelAlarm(id)
            repository.deleteTimer(id)
        }
    }

    fun markTimerCompleted(id: String) {
        viewModelScope.launch {
            repository.markTimerCompleted(id)
            // Alarm abbrechen
            alarmScheduler.cancelAlarm(id)
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
            repository.createQRCode(qrCode)
        }
    }

    fun deleteQRCode(id: String) {
        viewModelScope.launch {
            repository.deleteQRCode(id)
        }
    }
}