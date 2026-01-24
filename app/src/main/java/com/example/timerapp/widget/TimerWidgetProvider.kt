package com.example.timerapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.timerapp.MainActivity
import com.example.timerapp.R
import com.example.timerapp.SupabaseClient
import com.example.timerapp.models.Timer
import com.example.timerapp.utils.DateTimeUtils
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Klassischer AppWidgetProvider für sofortige Widget-Updates.
 * Ersetzt den Glance-basierten Ansatz für bessere Kontrolle.
 */
class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "TimerWidgetProvider"
        const val ACTION_REFRESH = "com.example.timerapp.WIDGET_REFRESH"

        // Timer Item IDs (Container, Name, Time, Category)
        private val TIMER_ITEMS = listOf(
            TimerItemIds(R.id.timer_item_1, R.id.timer_name_1, R.id.timer_time_1, R.id.timer_category_1),
            TimerItemIds(R.id.timer_item_2, R.id.timer_name_2, R.id.timer_time_2, R.id.timer_category_2),
            TimerItemIds(R.id.timer_item_3, R.id.timer_name_3, R.id.timer_time_3, R.id.timer_category_3),
            TimerItemIds(R.id.timer_item_4, R.id.timer_name_4, R.id.timer_time_4, R.id.timer_category_4),
            TimerItemIds(R.id.timer_item_5, R.id.timer_name_5, R.id.timer_time_5, R.id.timer_category_5),
        )

        /**
         * Aktualisiert alle Widgets SOFORT.
         * Diese Methode kann von überall aufgerufen werden.
         */
        fun updateAllWidgets(context: Context) {
            Log.d(TAG, "🔄 updateAllWidgets() aufgerufen")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TimerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                Log.d(TAG, "📱 ${appWidgetIds.size} Widgets gefunden")

                // Lade Timer aus Cache
                val timers = WidgetDataCache.loadTimers(context)
                Log.d(TAG, "📋 ${timers.size} Timer aus Cache")

                // Update jedes Widget
                appWidgetIds.forEach { widgetId ->
                    updateWidget(context, appWidgetManager, widgetId, timers)
                }

                Log.d(TAG, "✅ Alle Widgets aktualisiert!")
            } else {
                Log.d(TAG, "📭 Keine Widgets gefunden")
            }
        }

        /**
         * Aktualisiert ein einzelnes Widget.
         */
        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            timers: List<WidgetTimer>
        ) {
            Log.d(TAG, "🔄 Update Widget $appWidgetId mit ${timers.size} Timern")

            val views = RemoteViews(context.packageName, R.layout.widget_timer)

            // Click auf Widget öffnet App
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

            // Refresh Button
            val refreshIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            // Filtere nur zukünftige Timer
            val now = ZonedDateTime.now()
            val upcomingTimers = timers.filter { timer ->
                val targetTime = DateTimeUtils.parseIsoDateTime(timer.target_time)
                targetTime != null && targetTime.isAfter(now)
            }.take(5)

            // Zeige/Verstecke Empty State
            if (upcomingTimers.isEmpty()) {
                views.setViewVisibility(R.id.widget_empty_state, View.VISIBLE)
                // Verstecke alle Timer Items
                TIMER_ITEMS.forEach { item ->
                    views.setViewVisibility(item.containerId, View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.widget_empty_state, View.GONE)

                // Fülle Timer Items
                TIMER_ITEMS.forEachIndexed { index, item ->
                    if (index < upcomingTimers.size) {
                        val timer = upcomingTimers[index]
                        val targetTime = DateTimeUtils.parseIsoDateTime(timer.target_time)
                        val timeText = if (targetTime != null) {
                            DateTimeUtils.getTimeUntilText(targetTime)
                        } else {
                            "Unbekannt"
                        }

                        // Urgency-basierte Farbe
                        val timeColor = if (targetTime != null) {
                            val minutesUntil = DateTimeUtils.getMinutesUntil(targetTime)
                            when {
                                minutesUntil < 0 -> 0xFFEF5350.toInt()    // Rot
                                minutesUntil < 60 -> 0xFFFFB74D.toInt()   // Orange
                                else -> 0xFF81C784.toInt()                // Grün
                            }
                        } else {
                            0xFF9E9E9E.toInt()  // Grau
                        }

                        views.setViewVisibility(item.containerId, View.VISIBLE)
                        views.setTextViewText(item.nameId, timer.name)
                        views.setTextViewText(item.timeId, timeText)
                        views.setTextColor(item.timeId, timeColor)
                        views.setTextViewText(item.categoryId, timer.category)
                    } else {
                        views.setViewVisibility(item.containerId, View.GONE)
                    }
                }
            }

            // Widget aktualisieren
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "✅ Widget $appWidgetId aktualisiert")
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "📡 onUpdate für ${appWidgetIds.size} Widgets")
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH -> {
                Log.d(TAG, "🔄 Refresh-Button gedrückt - lade von Server...")

                // Lade Daten vom Server im Hintergrund
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val client = SupabaseClient.client
                        val response = client.from("timers")
                            .select()
                            .decodeList<Timer>()

                        // Sortiere nach Zeit
                        val sortedTimers = response.sortedBy {
                            try {
                                ZonedDateTime.parse(it.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                    .toInstant()
                                    .toEpochMilli()
                            } catch (e: Exception) {
                                Long.MAX_VALUE
                            }
                        }

                        Log.d(TAG, "✅ ${sortedTimers.size} Timer von Server geladen")

                        // Cache aktualisieren (ohne Widget-Update, das machen wir selbst)
                        WidgetDataCache.cacheTimersWithoutUpdate(context, sortedTimers)

                        // Widget sofort aktualisieren
                        updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Server-Refresh fehlgeschlagen: ${e.message}", e)
                        // Trotzdem Widget aktualisieren mit Cache
                        updateAllWidgets(context)
                    }
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "✅ Erstes Widget hinzugefügt")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "🗑️ Letztes Widget entfernt")
    }
}

/**
 * Hilfsklasse für Timer-Item-IDs.
 */
data class TimerItemIds(
    val containerId: Int,
    val nameId: Int,
    val timeId: Int,
    val categoryId: Int
)
