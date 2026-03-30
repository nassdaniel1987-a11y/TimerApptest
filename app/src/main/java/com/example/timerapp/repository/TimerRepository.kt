package com.example.timerapp.repository

import android.util.Log
import com.example.timerapp.data.dao.CategoryDao
import com.example.timerapp.data.dao.PendingSyncDao
import com.example.timerapp.data.dao.QRCodeDao
import com.example.timerapp.data.dao.TimerDao
import com.example.timerapp.data.dao.TimerTemplateDao
import com.example.timerapp.data.entity.PendingSyncEntity
import com.example.timerapp.fcm.FcmTokenManager
import com.example.timerapp.fcm.PushNotificationManager
import com.example.timerapp.models.Category
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.models.Result
import com.example.timerapp.models.Timer
import com.example.timerapp.models.TimerTemplate
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val pendingSyncDao: PendingSyncDao,
    private val fcmTokenManager: FcmTokenManager,
    private val pushNotificationManager: PushNotificationManager
) {
    private val TAG = "TimerRepository"
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val pushScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

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

    // Realtime
    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null
    private var realtimeDebounceJob: Job? = null

    // Mutex verhindert gleichzeitige Refreshes (FCM + Realtime + manuell)
    private val refreshMutex = Mutex()

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

    /**
     * Startet Supabase Realtime — lauscht auf Änderungen in der timers-Tabelle.
     * Bei jeder Änderung wird ein debounced Refresh gemacht (300ms),
     * damit mehrere schnelle Events zu einem Refresh zusammengefasst werden.
     */
    fun startRealtime(scope: CoroutineScope) {
        if (realtimeChannel != null) return // Bereits aktiv

        scope.launch {
            try {
                val channel = client.channel("timer-realtime")
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "timers"
                }

                realtimeJob = changeFlow.onEach { action ->
                    Log.d(TAG, "Realtime-Event: ${action::class.simpleName}")
                    // Debounce: Mehrere Events innerhalb 300ms → ein Refresh
                    realtimeDebounceJob?.cancel()
                    realtimeDebounceJob = scope.launch {
                        delay(300)
                        refreshTimers()
                    }
                }.launchIn(scope)

                channel.subscribe()
                realtimeChannel = channel
                Log.d(TAG, "Realtime-Subscription aktiv für timers-Tabelle")
            } catch (e: Exception) {
                Log.e(TAG, "Realtime-Subscription fehlgeschlagen: ${e.message}")
            }
        }
    }

    fun stopRealtime() {
        realtimeJob?.cancel()
        realtimeDebounceJob?.cancel()
        realtimeJob = null
        realtimeDebounceJob = null
        realtimeChannel = null
        Log.d(TAG, "Realtime-Subscription gestoppt")
    }

    // ── Timer Operations (Room-First) ──

    suspend fun createTimer(timer: Timer): Result<Timer> {
        return try {
            val deviceId = fcmTokenManager.getDeviceId()
            val newTimer = if (timer.id.isBlank()) {
                timer.copy(
                    id = UUID.randomUUID().toString(),
                    created_at = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    source_device_id = deviceId
                )
            } else timer.copy(source_device_id = deviceId)

            timerDao.insertTimer(newTimer)
            enqueueSyncOperation("timer", "CREATE", newTimer.id, json.encodeToString(newTimer))

            // ✅ Push SOFORT senden (parallel, non-blocking, unabhängig vom Sync)
            pushScope.launch {
                pushNotificationManager.sendPushNotification(
                    eventType = "timer_created",
                    timerName = newTimer.name,
                    sourceDeviceId = deviceId
                )
            }

            Log.d(TAG, "✅ Timer erstellt (lokal): ${newTimer.name}")
            Result.Success(newTimer)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Erstellen: ${e.message}", e)
            Result.Error(e, userMessage = "Fehler beim Erstellen des Timers")
        }
    }

    suspend fun updateTimer(id: String, timer: Timer): Result<Unit> {
        return try {
            val timerWithDevice = timer.copy(source_device_id = fcmTokenManager.getDeviceId())
            timerDao.updateTimer(timerWithDevice)
            enqueueSyncOperation("timer", "UPDATE", id, json.encodeToString(timerWithDevice))
            Log.d(TAG, "✅ Timer aktualisiert (lokal): $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Aktualisieren: ${e.message}", e)
            Result.Error(e, userMessage = "Fehler beim Aktualisieren")
        }
    }

    suspend fun deleteTimer(id: String): Result<Unit> {
        return try {
            // Vor dem Löschen: source_device_id setzen, damit der DB-Trigger
            // das richtige Gerät kennt und keinen Push an dieses Gerät sendet
            val deviceId = fcmTokenManager.getDeviceId()
            val existingTimer = timerDao.getTimerById(id)
            if (existingTimer != null) {
                val updatedTimer = existingTimer.copy(source_device_id = deviceId)
                enqueueSyncOperation("timer", "UPDATE", id, json.encodeToString(updatedTimer))

                // ✅ Push SOFORT senden — ABER NICHT für abgelaufene/abgeschlossene Timer
                val isExpired = try {
                    val targetTime = java.time.ZonedDateTime.parse(
                        existingTimer.target_time,
                        java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    )
                    targetTime.toInstant().isBefore(java.time.Instant.now())
                } catch (e: Exception) { true }

                if (!existingTimer.is_completed && !isExpired) {
                    pushScope.launch {
                        pushNotificationManager.sendPushNotification(
                            eventType = "timer_deleted",
                            timerName = existingTimer.name,
                            sourceDeviceId = deviceId
                        )
                    }
                } else {
                    Log.d(TAG, "🔕 Kein Push für gelöschten Timer (abgelaufen/abgeschlossen): ${existingTimer.name}")
                }
            }
            timerDao.deleteTimer(id)
            enqueueSyncOperation("timer", "DELETE", id, "")
            Log.d(TAG, "✅ Timer gelöscht (lokal): $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Löschen: ${e.message}", e)
            Result.Error(e, userMessage = "Fehler beim Löschen")
        }
    }

    suspend fun cleanupOldTimers(cutoffTimeStr: String): Result<Int> {
        return try {
            val idsToDelete = timerDao.getOldCompletedTimerIds(cutoffTimeStr)
            if (idsToDelete.isEmpty()) return Result.Success(0)

            timerDao.deleteOldCompletedTimers(cutoffTimeStr)
            idsToDelete.forEach { id ->
                enqueueSyncOperation("timer", "DELETE", id, "")
            }
            Log.d(TAG, "🧹 Auto-Cleanup: ${idsToDelete.size} Timer gelöscht (älter als $cutoffTimeStr)")
            Result.Success(idsToDelete.size)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Auto-Cleanup: ${e.message}", e)
            Result.Error(e, userMessage = "Fehler beim Auto-Cleanup")
        }
    }

    suspend fun markTimerCompleted(id: String): Result<Unit> {
        return try {
            timerDao.markCompleted(id)
            val updatedTimer = timerDao.getTimerById(id)
            if (updatedTimer != null) {
                val timerWithDevice = updatedTimer.copy(source_device_id = fcmTokenManager.getDeviceId())
                enqueueSyncOperation("timer", "UPDATE", id, json.encodeToString(timerWithDevice))
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
     * Nutzt @Transaction damit die UI keinen Zwischenzustand (leere Liste) sieht.
     * Mutex verhindert gleichzeitige Refreshes von FCM/Realtime/manuell.
     */
    suspend fun refreshTimers(): Result<Unit> {
        return refreshMutex.withLock {
            try {
                val response = client.from("timers").select().decodeList<Timer>()
                timerDao.replaceAllTimers(response)
                Log.d(TAG, "✅ ${response.size} Timer vom Server geladen")
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Server-Refresh fehlgeschlagen (offline?): ${e.message}")
                Result.Error(e, retryable = true, userMessage = "Offline — zeige lokale Daten")
            }
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
            categoryDao.replaceAllCategories(response)
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
