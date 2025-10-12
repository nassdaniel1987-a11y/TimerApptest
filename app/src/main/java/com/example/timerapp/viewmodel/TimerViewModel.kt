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
            val createdTimer = repository.createTimer(timer)
            createdTimer?.let {
                alarmScheduler.scheduleAlarm(it)
                repository.refreshTimers()
            }
        }
    }

    fun updateTimer(id: String, timer: Timer) {
        viewModelScope.launch {
            repository.updateTimer(id, timer)
            alarmScheduler.cancelAlarm(id)
            alarmScheduler.scheduleAlarm(timer.copy(id = id))
        }
    }

    fun deleteTimer(id: String) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(id)
            repository.deleteTimer(id)
        }
    }

    fun markTimerCompleted(id: String) {
        viewModelScope.launch {
            repository.markTimerCompleted(id)
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