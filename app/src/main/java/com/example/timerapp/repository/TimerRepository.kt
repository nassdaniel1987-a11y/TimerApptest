package com.example.timerapp.repository

import android.util.Log
import com.example.timerapp.SupabaseClient
import com.example.timerapp.models.Category
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.models.Timer
import com.example.timerapp.models.TimerTemplate
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TimerRepository {

    private val client = SupabaseClient.client

    private val _timers = MutableStateFlow<List<Timer>>(emptyList())
    val timers: StateFlow<List<Timer>> = _timers.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _templates = MutableStateFlow<List<TimerTemplate>>(emptyList())
    val templates: StateFlow<List<TimerTemplate>> = _templates.asStateFlow()

    private val _qrCodes = MutableStateFlow<List<QRCodeData>>(emptyList())
    val qrCodes: StateFlow<List<QRCodeData>> = _qrCodes.asStateFlow()

    // Timer Operations
    suspend fun refreshTimers() {
        try {
            val response = client.from("timers")
                .select()
                .decodeList<Timer>()
            _timers.value = response.sortedBy {
                ZonedDateTime.parse(it.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toInstant()
                    .toEpochMilli()
            }
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler beim Laden der Timer: ${e.message}")
        }
    }

    suspend fun createTimer(timer: Timer): Timer? {
        return try {
            val response = client.from("timers").insert(timer) { select() }.decodeSingle<Timer>()
            Log.d("TimerRepository", "✅ Timer erstellt: ${response.name}")
            response
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler beim Erstellen des Timers: ${e.message}")
            null
        }
    }

    suspend fun deleteTimer(id: String) {
        try {
            client.from("timers").delete { filter { eq("id", id) } }
            refreshTimers()
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler beim Löschen des Timers: ${e.message}")
        }
    }

    suspend fun updateTimer(id: String, timer: Timer) {
        try {
            client.from("timers").update(timer) { filter { eq("id", id) } }
            refreshTimers()
            Log.d("TimerRepository", "✅ Timer aktualisiert: $id")
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler beim Aktualisieren: ${e.message}")
        }
    }

    suspend fun markTimerCompleted(id: String) {
        try {
            client.from("timers").update(mapOf("is_completed" to true)) { filter { eq("id", id) } }
            refreshTimers()
            Log.d("TimerRepository", "✅ Timer abgeschlossen: $id")
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler: ${e.message}")
        }
    }

    // Category Operations
    suspend fun refreshCategories() {
        try {
            val response = client.from("categories").select().decodeList<Category>()
            _categories.value = response
            Log.d("TimerRepository", "✅ ${response.size} Kategorien geladen")
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler: ${e.message}")
        }
    }

    suspend fun createCategory(category: Category) {
        try {
            client.from("categories").insert(category)
            refreshCategories()
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler: ${e.message}")
        }
    }

    suspend fun deleteCategory(id: String) {
        try {
            client.from("categories").delete { filter { eq("id", id) } }
            refreshCategories()
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler: ${e.message}")
        }
    }

    // Template Operations
    suspend fun refreshTemplates() {
        try {
            val response = client.from("timer_templates").select().decodeList<TimerTemplate>()
            _templates.value = response
            Log.d("TimerRepository", "✅ ${response.size} Templates geladen")
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler: ${e.message}")
        }
    }

    suspend fun createTemplate(template: TimerTemplate) {
        try {
            client.from("timer_templates").insert(template)
            refreshTemplates()
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler: ${e.message}")
        }
    }

    suspend fun deleteTemplate(id: String) {
        try {
            client.from("timer_templates").delete { filter { eq("id", id) } }
            refreshTemplates()
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler: ${e.message}")
        }
    }

    // QR Code Operations
    suspend fun refreshQRCodes() {
        try {
            val response = client.from("qr_codes").select().decodeList<QRCodeData>()
            _qrCodes.value = response
            Log.d("TimerRepository", "✅ ${response.size} QR-Codes geladen")
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler beim Laden der QR-Codes: ${e.message}")
        }
    }

    suspend fun createQRCode(qrCode: QRCodeData): QRCodeData? {
        return try {
            val response = client.from("qr_codes").insert(qrCode) { select() }.decodeSingle<QRCodeData>()
            Log.d("TimerRepository", "✅ QR-Code in DB erstellt: ${response.name}")
            response
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler beim Erstellen des QR-Codes: ${e.message}")
            null
        }
    }

    fun addQRCodeToLocalList(qrCode: QRCodeData) {
        _qrCodes.value = _qrCodes.value + qrCode
        Log.d("TimerRepository", "✅ QR-Code zur lokalen Liste hinzugefügt.")
    }

    suspend fun deleteQRCode(id: String) {
        try {
            client.from("qr_codes").delete { filter { eq("id", id) } }
            refreshQRCodes()
        } catch (e: Exception) {
            Log.e("TimerRepository", "❌ Fehler beim Löschen des QR-Codes: ${e.message}")
        }
    }
}