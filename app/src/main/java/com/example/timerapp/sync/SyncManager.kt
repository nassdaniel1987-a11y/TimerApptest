package com.example.timerapp.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.example.timerapp.data.dao.PendingSyncDao
import com.example.timerapp.data.entity.PendingSyncEntity
import com.example.timerapp.models.Category
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.models.Timer
import com.example.timerapp.models.TimerTemplate
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

class SyncManager(
    private val context: Context,
    private val supabaseClient: io.github.jan.supabase.SupabaseClient,
    private val pendingSyncDao: PendingSyncDao
) {
    private val TAG = "SyncManager"
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Conflated Channel: Stellt sicher, dass Sync-Trigger nicht 'verloren' gehen,
    // führt aber nicht zu Warteschlangen aus hunderten gleichen Sync-Requests.
    private val syncChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        // Consumer-Coroutine, die den Sync-Channel sicher sequentiell abarbeitet
        scope.launch {
            for (trigger in syncChannel) {
                if (_isOnline.value) {
                    processPendingSyncInternal()
                }
            }
        }
    }

    /**
     * Startet Netzwerk-Monitoring via ConnectivityManager.
     * Bei Verbindungswiederherstellung wird automatisch synchronisiert.
     */
    fun startMonitoring() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Initialen Status prüfen
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "🌐 Netzwerk verfügbar — starte Sync")
                _isOnline.value = true
                triggerSyncIfOnline()
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "📴 Netzwerk verloren")
                _isOnline.value = false
            }
        })

        Log.d(TAG, "✅ Netzwerk-Monitoring gestartet (online: ${_isOnline.value})")
    }

    /**
     * Triggert Sync im Hintergrund, falls online.
     * Wird nach jeder CRUD-Operation aufgerufen.
     * Nutzt den sicheren Channel (keine Data-Races).
     */
    fun triggerSyncIfOnline() {
        if (!_isOnline.value) return
        syncChannel.trySend(Unit)
    }

    /**
     * Direkter Aufruf für den WorkManager (SyncWorker).
     * Sichert ab, dass der Sync getriggert wird, auch wenn wir die Status-Änderung von UI ignorieren.
     */
    suspend fun processPendingSync() {
        if (!_isOnline.value) return
        processPendingSyncInternal()
    }

    private suspend fun processPendingSyncInternal() {
        _isSyncing.value = true

        try {
            withTimeout(120_000L) {
                val pending = pendingSyncDao.getAllPending()
                if (pending.isEmpty()) {
                    Log.d(TAG, "✅ Keine ausstehenden Sync-Operationen")
                    return@withTimeout
                }

                Log.d(TAG, "🔄 Verarbeite ${pending.size} Sync-Operationen als Bulk-Upload...")

                // Gruppieren nach Table (Entity-Type)
                val byType = pending.groupBy { it.entity_type }

                for ((type, ops) in byType) {
                    val table = when (type) {
                        "timer" -> "timers"
                        "category" -> "categories"
                        "template" -> "timer_templates"
                        "qr_code" -> "qr_codes"
                        else -> {
                            Log.w(TAG, "Unbekannter Entity-Typ übersprungen: $type")
                            continue
                        }
                    }

                    // Für jede Entity die finale Operation bestimmen (Upsert vs. Delete)
                    val opsByEntity = ops.groupBy { it.entity_id }
                    val deletes = mutableListOf<String>()
                    val upserts = mutableListOf<PendingSyncEntity>()

                    opsByEntity.forEach { (entityId, entityOps) ->
                        val finalOp = entityOps.last()
                        if (finalOp.operation == "DELETE") {
                            deletes.add(entityId)
                        } else {
                            upserts.add(finalOp)
                        }
                    }

                    try {
                        withTimeout(30_000L) {
                            // 1. Bulk Upsert anwenden
                            if (upserts.isNotEmpty()) {
                                when (type) {
                                    "timer" -> {
                                        val items = upserts.map { json.decodeFromString<Timer>(it.payload) }
                                        supabaseClient.from(table).upsert(items)
                                    }
                                    "category" -> {
                                        val items = upserts.map { json.decodeFromString<Category>(it.payload) }
                                        supabaseClient.from(table).upsert(items)
                                    }
                                    "template" -> {
                                        val items = upserts.map { json.decodeFromString<TimerTemplate>(it.payload) }
                                        supabaseClient.from(table).upsert(items)
                                    }
                                    "qr_code" -> {
                                        val items = upserts.map { json.decodeFromString<QRCodeData>(it.payload) }
                                        supabaseClient.from(table).upsert(items)
                                    }
                                }
                            }

                            // 2. Bulk Delete anwenden
                            if (deletes.isNotEmpty()) {
                                supabaseClient.from(table).delete {
                                    filter { isIn("id", deletes) }
                                }
                            }
                        }

                        // Bei Erfolg die Operationen löschen
                        ops.forEach { pendingSyncDao.delete(it) }
                        Log.d(TAG, "✅ Sync Tabellen-Batch erfolgreich: $table (${upserts.size} Upserts, ${deletes.size} Deletes)")

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Sync Tabellen-Batch fehlgeschlagen bei $table: ${e.message}")
                        // Bei Fehlern dieser Tabelle brechen wir den Block ab — Retry beim nächsten Sync
                        throw e
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "⏱️ Sync-Prozess unterbrochen/Timeout — erneuter Versuch beim nächsten Trigger: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }
}
