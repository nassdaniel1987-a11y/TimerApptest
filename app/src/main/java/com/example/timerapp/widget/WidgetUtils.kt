package com.example.timerapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility-Funktionen für das Timer Widget.
 */
object WidgetUtils {

    private const val TAG = "WidgetUtils"

    /**
     * Aktualisiert alle Timer-Widgets SOFORT.
     * Sollte aufgerufen werden, wenn Timer erstellt, geändert oder gelöscht werden.
     */
    fun updateWidgets(context: Context) {
        Log.d(TAG, "🔄 updateWidgets() aufgerufen")

        // IO-Dispatcher für bessere Performance
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Glance Widget aktualisieren
                TimerWidget().updateAll(context)
                Log.d(TAG, "✅ Glance updateAll() erfolgreich")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Glance Update fehlgeschlagen: ${e.message}", e)
            }
        }

        // Zusätzlich: Broadcast senden als Fallback
        try {
            sendUpdateBroadcast(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Broadcast fehlgeschlagen: ${e.message}")
        }
    }

    /**
     * Sendet einen Update-Broadcast an alle Widgets.
     */
    private fun sendUpdateBroadcast(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, TimerWidgetReceiver::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (appWidgetIds.isNotEmpty()) {
            val intent = Intent(context, TimerWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "📡 Broadcast gesendet für ${appWidgetIds.size} Widgets")
        } else {
            Log.d(TAG, "📭 Keine Widgets gefunden")
        }
    }

    /**
     * Prüft, ob Widgets aktiv sind.
     */
    suspend fun hasActiveWidgets(context: Context): Boolean {
        return try {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(TimerWidget::class.java).isNotEmpty()
        } catch (e: Exception) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, TimerWidgetReceiver::class.java)
                appWidgetManager.getAppWidgetIds(componentName).isNotEmpty()
            } catch (e2: Exception) {
                false
            }
        }
    }
}
