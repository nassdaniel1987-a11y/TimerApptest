package com.example.timerapp.repository

import android.util.Log
import com.example.timerapp.data.dao.CategoryDao
import com.example.timerapp.data.dao.PendingSyncDao
import com.example.timerapp.data.dao.QRCodeDao
import com.example.timerapp.data.dao.TimerDao
import com.example.timerapp.data.dao.TimerTemplateDao
import com.example.timerapp.data.entity.PendingSyncEntity
import com.example.timerapp.models.Category
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.models.Result
import com.example.timerapp.models.Timer
import com.example.timerapp.models.TimerTemplate
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class TimerRepository(
    private val client: io.github.jan.supabase.SupabaseClient,
    private val timerDao: TimerDao,
    private val categoryDao: CategoryDao,
    private val templateDao: TimerTemplateDao,
    private val qrCodeDao: QRCodeDao,
    private val pendingSyncDao: PendingSyncDao
) {
    private val TAG = "TimerRepository"
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // StateFlows — gleiche Interface wie vorher, ViewModel ändert sich nicht
    private val _timers = MutableStateFlow<List<Timer>>(emptyList())
    val timers: StateFlow<List<Timer>> = _timers.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _templates = MutableStateFlow<List<TimerTemplate>>(emptyList())
    val templates: StateFlow<List<TimerTemplate>> = _templates.asStateFlow()

    private val _qrCodes = MutableStateFlow<List<QRCodeData>>(emptyList())
    val qrCodes: StateFlow<List<QRCodeData>> = _qrCodes.asStateFlow()

    // Sync-Status für UI
    val pendingSyncCount: Flow<Int> = pendingSyncDao.getPendingCount()

    /**
     * Startet das Beobachten der Room-Flows.
     * Room-Änderungen werden automatisch in die StateFlows propagiert.
     */
    fun observeAll(scope: CoroutineScope) {
        scope.launch {
            timerDao.getAllTimers().collect { list ->
                _timers.value = list.sortedBy { timer ->
                    try {
                        ZonedDateTime.parse(timer.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            .toInstant().toEpochMilli()
                    } catch (e: Exception) { Long.MAX_VALUE }
                }
            }
        }
        scope.launch {
            categoryDao.getAllCategories().collect { _categories.value = it }
        }
        scope.launch {
            templateDao.getAllTemplates().collect { _templates.value = it }
        }
        scope.launch {
            qrCodeDao.getAllQRCodes().collect { _qrCodes.value = it }
        }
    }

    // ── Timer Operations (Room-First) ──

    suspend fun createTimer(timer: Timer): Result<Timer> {
        return try {
            val newTimer = if (timer.id.isBlank()) {
                timer.copy(
                    id = UUID.randomUUID().toString(),
                    created_at = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            } else timer

            timerDao.insertTimer(newTimer)
            enqueueSyncOperation("timer", "CREATE", newTimer.id, json.encodeToString(newTimer))
            Log.d(TAG, "✅ Timer erstellt (lokal): ${newTimer.name}")
            Result.Success(newTimer)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Erstellen: ${e.message}", e)
            Result.Error(e, userMessage = "Fehler beim Erstellen des Timers")
        }
    }

    suspend fun updateTimer(id: String, timer: Timer): Result<Unit> {
        return try {
            timerDao.updateTimer(timer)
            enqueueSyncOperation("timer", "UPDATE", id, json.encodeToString(timer))
            Log.d(TAG, "✅ Timer aktualisiert (lokal): $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Aktualisieren: ${e.message}", e)
            Result.Error(e, userMessage = "Fehler beim Aktualisieren")
        }
    }

    suspend fun deleteTimer(id: String): Result<Unit> {
        return try {
            timerDao.deleteTimer(id)
            enqueueSyncOperation("timer", "DELETE", id, "")
            Log.d(TAG, "✅ Timer gelöscht (lokal): $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Löschen: ${e.message}", e)
            Result.Error(e, userMessage = "Fehler beim Löschen")
        }
    }

    suspend fun markTimerCompleted(id: String): Result<Unit> {
        return try {
            timerDao.markCompleted(id)
            val updatedTimer = timerDao.getTimerById(id)
            if (updatedTimer != null) {
                enqueueSyncOperation("timer", "UPDATE", id, json.encodeToString(updatedTimer))
            }
            Log.d(TAG, "✅ Timer abgeschlossen (lokal): $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Abschließen: ${e.message}", e)
            Result.Error(e, userMessage = "Fehler beim Abschließen")
        }
    }

    /**
     * Pull vom Server — ersetzt lokale Daten mit Server-Daten.
     * Bei Netzwerk-Fehler bleiben die Room-Daten erhalten.
     */
    suspend fun refreshTimers(): Result<Unit> {
        return try {
            val response = client.from("timers").select().decodeList<Timer>()
            timerDao.deleteAllTimers()
            timerDao.insertTimers(response)
            Log.d(TAG, "✅ ${response.size} Timer vom Server geladen")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Server-Refresh fehlgeschlagen (offline?): ${e.message}")
            Result.Error(e, retryable = true, userMessage = "Offline — zeige lokale Daten")
        }
    }

    // Optimistic-Update-Methoden — jetzt No-Ops (Room-Flow macht das automatisch)
    fun addTimerToLocalList(timer: Timer) { /* Room-Flow aktualisiert UI automatisch */ }
    fun removeTimerFromLocalList(id: String) { /* Room-Flow aktualisiert UI automatisch */ }
    fun markTimerCompletedLocally(id: String) { /* Room-Flow aktualisiert UI automatisch */ }

    // ── Category Operations ──

    suspend fun refreshCategories(): Result<Unit> {
        return try {
            val response = client.from("categories").select().decodeList<Category>()
            categoryDao.deleteAllCategories()
            categoryDao.insertCategories(response)
            Log.d(TAG, "✅ ${response.size} Kategorien vom Server geladen")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Kategorien-Refresh fehlgeschlagen: ${e.message}")
            Result.Error(e, retryable = true)
        }
    }

    suspend fun createCategory(category: Category): Result<Unit> {
        return try {
            val newCategory = if (category.id.isBlank()) {
                category.copy(id = UUID.randomUUID().toString())
            } else category

            categoryDao.insertCategory(newCategory)
            enqueueSyncOperation("category", "CREATE", newCategory.id, json.encodeToString(newCategory))
            Log.d(TAG, "✅ Kategorie erstellt (lokal): ${newCategory.name}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, userMessage = "Fehler beim Erstellen der Kategorie")
        }
    }

    suspend fun deleteCategory(id: String): Result<Unit> {
        return try {
            categoryDao.deleteCategory(id)
            enqueueSyncOperation("category", "DELETE", id, "")
            Log.d(TAG, "✅ Kategorie gelöscht (lokal): $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, userMessage = "Fehler beim Löschen der Kategorie")
        }
    }

    // ── Template Operations ──

    suspend fun refreshTemplates(): Result<Unit> {
        return try {
            val response = client.from("timer_templates").select().decodeList<TimerTemplate>()
            templateDao.deleteAllTemplates()
            templateDao.insertTemplates(response)
            Log.d(TAG, "✅ ${response.size} Templates vom Server geladen")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Templates-Refresh fehlgeschlagen: ${e.message}")
            Result.Error(e, retryable = true)
        }
    }

    suspend fun createTemplate(template: TimerTemplate): Result<Unit> {
        return try {
            val newTemplate = if (template.id.isBlank()) {
                template.copy(id = UUID.randomUUID().toString())
            } else template

            templateDao.insertTemplate(newTemplate)
            enqueueSyncOperation("template", "CREATE", newTemplate.id, json.encodeToString(newTemplate))
            Log.d(TAG, "✅ Template erstellt (lokal): ${newTemplate.name}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, userMessage = "Fehler beim Erstellen des Templates")
        }
    }

    suspend fun deleteTemplate(id: String): Result<Unit> {
        return try {
            templateDao.deleteTemplate(id)
            enqueueSyncOperation("template", "DELETE", id, "")
            Log.d(TAG, "✅ Template gelöscht (lokal): $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, userMessage = "Fehler beim Löschen des Templates")
        }
    }

    // ── QR Code Operations ──

    suspend fun refreshQRCodes(): Result<Unit> {
        return try {
            val response = client.from("qr_codes").select().decodeList<QRCodeData>()
            qrCodeDao.deleteAllQRCodes()
            qrCodeDao.insertQRCodes(response)
            Log.d(TAG, "✅ ${response.size} QR-Codes vom Server geladen")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ QR-Codes-Refresh fehlgeschlagen: ${e.message}")
            Result.Error(e, retryable = true)
        }
    }

    suspend fun createQRCode(qrCode: QRCodeData): Result<QRCodeData> {
        return try {
            val newQR = if (qrCode.id.isBlank()) {
                qrCode.copy(id = UUID.randomUUID().toString())
            } else qrCode

            qrCodeDao.insertQRCode(newQR)
            enqueueSyncOperation("qr_code", "CREATE", newQR.id, json.encodeToString(newQR))
            Log.d(TAG, "✅ QR-Code erstellt (lokal): ${newQR.name}")
            Result.Success(newQR)
        } catch (e: Exception) {
            Result.Error(e, userMessage = "Fehler beim Erstellen des QR-Codes")
        }
    }

    fun addQRCodeToLocalList(qrCode: QRCodeData) { /* Room-Flow aktualisiert UI automatisch */ }

    suspend fun deleteQRCode(id: String): Result<Unit> {
        return try {
            qrCodeDao.deleteQRCode(id)
            enqueueSyncOperation("qr_code", "DELETE", id, "")
            Log.d(TAG, "✅ QR-Code gelöscht (lokal): $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, userMessage = "Fehler beim Löschen des QR-Codes")
        }
    }

    // ── Sync-Queue ──

    private suspend fun enqueueSyncOperation(
        entityType: String, operation: String, entityId: String, payload: String
    ) {
        pendingSyncDao.insert(
            PendingSyncEntity(
                entity_type = entityType,
                operation = operation,
                entity_id = entityId,
                payload = payload
            )
        )
        Log.d(TAG, "📤 Sync-Operation eingereiht: $operation $entityType $entityId")
    }
}
