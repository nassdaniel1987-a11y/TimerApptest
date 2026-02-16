package com.example.timerapp.repository

import android.util.Log
import com.example.timerapp.models.Category
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.models.Result
import com.example.timerapp.models.Timer
import com.example.timerapp.models.TimerTemplate
import com.example.timerapp.utils.retry
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TimerRepository(
    private val client: io.github.jan.supabase.SupabaseClient
) {

    private val _timers = MutableStateFlow<List<Timer>>(emptyList())
    val timers: StateFlow<List<Timer>> = _timers.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _templates = MutableStateFlow<List<TimerTemplate>>(emptyList())
    val templates: StateFlow<List<TimerTemplate>> = _templates.asStateFlow()

    private val _qrCodes = MutableStateFlow<List<QRCodeData>>(emptyList())
    val qrCodes: StateFlow<List<QRCodeData>> = _qrCodes.asStateFlow()

    // Timer Operations
    suspend fun refreshTimers(): Result<Unit> = retry {
        val response = client.from("timers")
            .select()
            .decodeList<Timer>()
        _timers.value = response.sortedBy {
            ZonedDateTime.parse(it.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        }
        Log.d("TimerRepository", "✅ ${response.size} Timer geladen")
    }

    suspend fun createTimer(timer: Timer): Result<Timer> = retry {
        val response = client.from("timers").insert(timer) { select() }.decodeSingle<Timer>()
        Log.d("TimerRepository", "✅ Timer erstellt: ${response.name}")
        response
    }

    suspend fun deleteTimer(id: String): Result<Unit> = retry {
        client.from("timers").delete { filter { eq("id", id) } }
        refreshTimers()
        Log.d("TimerRepository", "✅ Timer gelöscht: $id")
    }

    suspend fun updateTimer(id: String, timer: Timer): Result<Unit> = retry {
        client.from("timers").update(timer) { filter { eq("id", id) } }
        refreshTimers()
        Log.d("TimerRepository", "✅ Timer aktualisiert: $id")
    }

    suspend fun markTimerCompleted(id: String): Result<Unit> = retry {
        client.from("timers").update(mapOf("is_completed" to true)) { filter { eq("id", id) } }
        refreshTimers()
        Log.d("TimerRepository", "✅ Timer abgeschlossen: $id")
    }

    // Category Operations
    suspend fun refreshCategories(): Result<Unit> = retry {
        val response = client.from("categories").select().decodeList<Category>()
        _categories.value = response
        Log.d("TimerRepository", "✅ ${response.size} Kategorien geladen")
    }

    suspend fun createCategory(category: Category): Result<Unit> = retry {
        client.from("categories").insert(category)
        refreshCategories()
        Log.d("TimerRepository", "✅ Kategorie erstellt: ${category.name}")
    }

    suspend fun deleteCategory(id: String): Result<Unit> = retry {
        client.from("categories").delete { filter { eq("id", id) } }
        refreshCategories()
        Log.d("TimerRepository", "✅ Kategorie gelöscht: $id")
    }

    // Template Operations
    suspend fun refreshTemplates(): Result<Unit> = retry {
        val response = client.from("timer_templates").select().decodeList<TimerTemplate>()
        _templates.value = response
        Log.d("TimerRepository", "✅ ${response.size} Templates geladen")
    }

    suspend fun createTemplate(template: TimerTemplate): Result<Unit> = retry {
        client.from("timer_templates").insert(template)
        refreshTemplates()
        Log.d("TimerRepository", "✅ Template erstellt: ${template.name}")
    }

    suspend fun deleteTemplate(id: String): Result<Unit> = retry {
        client.from("timer_templates").delete { filter { eq("id", id) } }
        refreshTemplates()
        Log.d("TimerRepository", "✅ Template gelöscht: $id")
    }

    // QR Code Operations
    suspend fun refreshQRCodes(): Result<Unit> = retry {
        val response = client.from("qr_codes").select().decodeList<QRCodeData>()
        _qrCodes.value = response
        Log.d("TimerRepository", "✅ ${response.size} QR-Codes geladen")
    }

    suspend fun createQRCode(qrCode: QRCodeData): Result<QRCodeData> = retry {
        val response = client.from("qr_codes").insert(qrCode) { select() }.decodeSingle<QRCodeData>()
        Log.d("TimerRepository", "✅ QR-Code in DB erstellt: ${response.name}")
        response
    }

    fun addQRCodeToLocalList(qrCode: QRCodeData) {
        _qrCodes.value = _qrCodes.value + qrCode
        Log.d("TimerRepository", "✅ QR-Code zur lokalen Liste hinzugefügt.")
    }

    suspend fun deleteQRCode(id: String): Result<Unit> = retry {
        client.from("qr_codes").delete { filter { eq("id", id) } }
        refreshQRCodes()
        Log.d("TimerRepository", "✅ QR-Code gelöscht: $id")
    }
}