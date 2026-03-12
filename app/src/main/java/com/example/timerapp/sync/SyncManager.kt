package com.example.timerapp.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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

    /**
     * Startet Netzwerk-Monitoring via ConnectivityManager.
     * Bei Verbindungswiederherstellung wird automatisch synchronisiert.
     */
    fun startMonitoring() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Initialen Status prüfen
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "🌐 Netzwerk verfügbar — starte Sync")
                _isOnline.value = true
                scope.launch { processPendingSync() }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "📴 Netzwerk verloren")
                _isOnline.value = false
            }
        })

        Log.d(TAG, "✅ Netzwerk-Monitoring gestartet (online: ${_isOnline.value})")
    }

    // Flag: Sync erneut starten nachdem aktueller Durchlauf fertig ist
    private var syncAgainAfterCurrent = false

    /**
     * Triggert Sync im Hintergrund, falls online.
     * Wird nach jeder CRUD-Operation aufgerufen.
     * Wenn bereits ein Sync läuft, wird ein erneuter Durchlauf nach Abschluss geplant.
     */
    fun triggerSyncIfOnline() {
        if (!_isOnline.value) return

        if (_isSyncing.value) {
            // Sync läuft bereits — markiere für erneuten Durchlauf
            syncAgainAfterCurrent = true
            Log.d(TAG, "🔄 Sync läuft bereits — erneuter Durchlauf geplant")
            return
        }

        scope.launch { processPendingSync() }
    }

    /**
     * Verarbeitet die Sync-Queue FIFO.
     * Bei Fehler wird gestoppt — nächster Versuch bei nächstem Trigger.
     * Timeout: 30s pro Operation, 120s gesamt.
     * Nach Abschluss wird geprüft, ob neue Operationen hinzugekommen sind.
     */
    suspend fun processPendingSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        syncAgainAfterCurrent = false

        try {
            withTimeout(120_000L) {
                val pending = pendingSyncDao.getAllPending()
                if (pending.isEmpty()) {
                    Log.d(TAG, "✅ Keine ausstehenden Sync-Operationen")
                    return@withTimeout
                }

                Log.d(TAG, "🔄 Verarbeite ${pending.size} Sync-Operationen...")

                for (op in pending) {
                    try {
                        withTimeout(30_000L) {
                            executeSyncOperation(op)
                        }
                        pendingSyncDao.delete(op)
                        Log.d(TAG, "✅ Sync erfolgreich: ${op.operation} ${op.entity_type} ${op.entity_id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Sync fehlgeschlagen bei ${op.entity_type}/${op.operation}: ${e.message}")
                        break // Bei Fehler stoppen, nächstes Mal erneut versuchen
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e(TAG, "⏱️ Sync-Timeout — wird beim nächsten Trigger erneut versucht")
        } finally {
            _isSyncing.value = false

            // Prüfe ob während des Syncs neue Operationen hinzugekommen sind
            if (syncAgainAfterCurrent) {
                syncAgainAfterCurrent = false
                Log.d(TAG, "🔄 Starte erneuten Sync-Durchlauf (neue Operationen während Sync)")
                scope.launch { processPendingSync() }
            }
        }
    }

    private suspend fun executeSyncOperation(op: PendingSyncEntity) {
        val table = when (op.entity_type) {
            "timer" -> "timers"
            "category" -> "categories"
            "template" -> "timer_templates"
            "qr_code" -> "qr_codes"
            else -> throw IllegalArgumentException("Unbekannter Entity-Typ: ${op.entity_type}")
        }

        when (op.operation) {
            "CREATE", "UPDATE" -> {
                // Upsert: Last-Write-Wins
                when (op.entity_type) {
                    "timer" -> {
                        val timer = json.decodeFromString<Timer>(op.payload)
                        supabaseClient.from(table).upsert(timer)
                    }
                    "category" -> {
                        val category = json.decodeFromString<Category>(op.payload)
                        supabaseClient.from(table).upsert(category)
                    }
                    "template" -> {
                        val template = json.decodeFromString<TimerTemplate>(op.payload)
                        supabaseClient.from(table).upsert(template)
                    }
                    "qr_code" -> {
                        val qrCode = json.decodeFromString<QRCodeData>(op.payload)
                        supabaseClient.from(table).upsert(qrCode)
                    }
                }
            }
            "DELETE" -> {
                supabaseClient.from(table).delete {
                    filter { eq("id", op.entity_id) }
                }
            }
        }
    }
}
