package com.example.timerapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.example.timerapp.models.Timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                // Methode 1: Hole alle GlanceIds und update jedes Widget einzeln
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(TimerWidget::class.java)

                Log.d(TAG, "📱 Gefunden: ${glanceIds.size} Glance Widgets")

                glanceIds.forEach { glanceId ->
                    Log.d(TAG, "🔄 Update Widget: $glanceId")
                    TimerWidget().update(context, glanceId)
                }

                // Methode 2: Auch updateAll aufrufen
                TimerWidget().updateAll(context)
                Log.d(TAG, "✅ Glance Updates erfolgreich")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Glance Update fehlgeschlagen: ${e.message}", e)
            }
        }

        // Methode 3: Broadcast senden als Backup
        sendUpdateBroadcast(context)
    }

    /**
     * Erzwingt Widget-Refresh über AppWidgetManager.
     */
    private fun forceWidgetRefresh(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TimerWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                // Force update für jedes Widget
                appWidgetIds.forEach { widgetId ->
                    appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, android.R.id.list)
                }
                Log.d(TAG, "🔄 Force-Refresh für ${appWidgetIds.size} Widgets")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Force-Refresh fehlgeschlagen: ${e.message}")
        }
    }

    /**
     * Aktualisiert Cache UND Widget in einem Schritt (synchron).
     * Diese Funktion stellt sicher, dass der Cache geschrieben ist,
     * bevor das Widget aktualisiert wird.
     */
    suspend fun updateCacheAndWidgets(context: Context, timers: List<Timer>) {
        Log.d(TAG, "🔄 updateCacheAndWidgets() mit ${timers.size} Timern")

        // 1. Cache schreiben (synchron)
        WidgetDataCache.cacheTimers(context, timers)
        Log.d(TAG, "✅ Cache geschrieben")

        // 2. Widget aktualisieren
        try {
            TimerWidget().updateAll(context)
            Log.d(TAG, "✅ Widget aktualisiert")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Widget-Update fehlgeschlagen: ${e.message}", e)
        }
    }

    /**
     * Sendet einen Update-Broadcast an alle Widgets.
     */
    fun sendUpdateBroadcast(context: Context) {
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
