package com.example.timerapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver für das Timer Widget.
 * Wird vom System aufgerufen, wenn das Widget aktualisiert werden soll.
 */
class TimerWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = TimerWidget()

    companion object {
        private const val TAG = "TimerWidgetReceiver"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "🔄 onUpdate aufgerufen für ${appWidgetIds.size} Widgets")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Bei jedem Broadcast das Widget neu laden
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            Log.d(TAG, "📡 Update-Broadcast empfangen")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    glanceAppWidget.updateAll(context)
                    Log.d(TAG, "✅ Widget aktualisiert nach Broadcast")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Fehler beim Widget-Update: ${e.message}")
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "✅ Widget wurde zum ersten Mal hinzugefügt")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "🗑️ Letztes Widget wurde entfernt")
    }
}
