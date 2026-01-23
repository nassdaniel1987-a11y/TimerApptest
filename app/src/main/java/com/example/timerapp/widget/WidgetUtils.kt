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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Utility-Funktionen für das Timer Widget.
 */
object WidgetUtils {

    private const val TAG = "WidgetUtils"

    /**
     * Aktualisiert alle Timer-Widgets.
     * Sollte aufgerufen werden, wenn Timer erstellt, geändert oder gelöscht werden.
     */
    fun updateWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Kurzer Delay, damit die Datenbank-Operation abgeschlossen ist
                delay(500)

                // Methode 1: Glance updateAll
                TimerWidget().updateAll(context)
                Log.d(TAG, "✅ Widgets aktualisiert (Glance)")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Glance Update fehlgeschlagen: ${e.message}")

                // Methode 2: Fallback mit Broadcast
                try {
                    sendUpdateBroadcast(context)
                    Log.d(TAG, "✅ Widgets aktualisiert (Broadcast)")
                } catch (e2: Exception) {
                    Log.e(TAG, "❌ Broadcast Update fehlgeschlagen: ${e2.message}")
                }
            }
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
            Log.d(TAG, "📡 Update-Broadcast gesendet für ${appWidgetIds.size} Widgets")
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
            // Fallback: Prüfe über AppWidgetManager
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
