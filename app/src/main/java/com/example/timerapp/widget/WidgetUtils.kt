package com.example.timerapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.example.timerapp.models.Timer

/**
 * Utility-Funktionen für das Timer Widget.
 */
object WidgetUtils {

    private const val TAG = "WidgetUtils"

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
