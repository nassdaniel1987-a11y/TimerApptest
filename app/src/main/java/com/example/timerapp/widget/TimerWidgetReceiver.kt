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
        const val ACTION_REFRESH_FROM_SERVER = "com.example.timerapp.REFRESH_WIDGET_FROM_SERVER"
        const val ACTION_AUTO_UPDATE = "com.example.timerapp.WIDGET_AUTO_UPDATE"

        /**
         * Sendet einen Broadcast um das Widget vom Server zu aktualisieren.
         */
        fun sendRefreshBroadcast(context: Context) {
            Log.d(TAG, "📤 Sende Refresh-Broadcast...")
            val intent = Intent(context, TimerWidgetReceiver::class.java).apply {
                action = ACTION_REFRESH_FROM_SERVER
            }
            context.sendBroadcast(intent)
        }
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

        when (intent.action) {
            // Manueller Refresh vom Server (Refresh-Button)
            ACTION_REFRESH_FROM_SERVER -> {
                Log.d(TAG, "🌐 Server-Refresh-Broadcast empfangen")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // 1. Daten vom Server laden
                        Log.d(TAG, "📥 Lade Daten von Supabase...")
                        val success = WidgetDataCache.refreshFromServer(context)

                        if (success) {
                            Log.d(TAG, "✅ Server-Daten geladen")
                        } else {
                            Log.w(TAG, "⚠️ Server-Refresh fehlgeschlagen")
                        }

                        // 2. Widget aktualisieren
                        glanceAppWidget.updateAll(context)
                        Log.d(TAG, "✅ Widget nach Server-Refresh aktualisiert")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Fehler beim Server-Refresh: ${e.message}", e)
                    }
                }
            }

            // Auto-Update für Echtzeit-Countdown
            ACTION_AUTO_UPDATE -> {
                Log.d(TAG, "⏰ Auto-Update für Countdown")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        glanceAppWidget.updateAll(context)
                        Log.d(TAG, "✅ Widget Countdown aktualisiert")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Fehler beim Auto-Update: ${e.message}")
                    }
                }
            }

            // Standard Widget-Update
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
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
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "✅ Widget wurde zum ersten Mal hinzugefügt")

        // Beim ersten Hinzufügen direkt vom Server laden
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetDataCache.refreshFromServer(context)
                glanceAppWidget.updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Fehler beim initialen Laden: ${e.message}")
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetAutoUpdater.cancelAutoUpdate(context)
        Log.d(TAG, "🗑️ Letztes Widget wurde entfernt, Auto-Update gestoppt")
    }
}
