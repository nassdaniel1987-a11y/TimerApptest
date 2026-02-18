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

    /**
     * Verarbeitet die Sync-Queue FIFO.
     * Bei Fehler wird gestoppt — nächster Versuch bei nächstem Trigger.
     */
    suspend fun processPendingSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true

        try {
            val pending = pendingSyncDao.getAllPending()
            if (pending.isEmpty()) {
                Log.d(TAG, "✅ Keine ausstehenden Sync-Operationen")
                return
            }

            Log.d(TAG, "🔄 Verarbeite ${pending.size} Sync-Operationen...")

            for (op in pending) {
                try {
                    executeSyncOperation(op)
                    pendingSyncDao.delete(op)
                    Log.d(TAG, "✅ Sync erfolgreich: ${op.operation} ${op.entity_type} ${op.entity_id}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Sync fehlgeschlagen bei ${op.entity_type}/${op.operation}: ${e.message}")
                    break // Bei Fehler stoppen, nächstes Mal erneut versuchen
                }
            }
        } finally {
            _isSyncing.value = false
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
