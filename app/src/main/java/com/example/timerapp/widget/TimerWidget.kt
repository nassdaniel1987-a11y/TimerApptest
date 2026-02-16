package com.example.timerapp.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.timerapp.MainActivity
import com.example.timerapp.SupabaseClient
import com.example.timerapp.models.Timer
import com.example.timerapp.utils.DateTimeUtils
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Timer Widget - Zeigt die nächsten anstehenden Timer als Liste an.
 */
class TimerWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "TimerWidget"
    }

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "🔄 provideGlance aufgerufen")

        // Lade Timer aus dem lokalen Cache
        val cachedTimers = WidgetDataCache.loadTimers(context)
        val now = ZonedDateTime.now()

        // Filtere nur zukünftige Timer
        val upcomingTimers = cachedTimers.filter { timer ->
            val targetTime = DateTimeUtils.parseIsoDateTime(timer.target_time)
            targetTime != null && targetTime.isAfter(now)
        }.take(5)

        Log.d(TAG, "📋 ${upcomingTimers.size} zukünftige Timer für Widget")

        // Plane minütliches Update wenn Timer in der nächsten Stunde ablaufen
        WidgetAutoUpdater.scheduleNextUpdate(context, upcomingTimers)

        provideContent {
            TimerWidgetContent(timers = upcomingTimers)
        }
    }
}

/**
 * ActionCallback für den Aktualisieren-Button.
 * Lädt Daten DIREKT von Supabase und aktualisiert das Widget.
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("TimerWidget", "🔄 Refresh-Button gedrückt - lade direkt von Supabase...")

        try {
            // 1. Daten direkt von Supabase laden (im IO-Thread)
            val timers = withContext(Dispatchers.IO) {
                try {
                    val client = SupabaseClient.client
                    val response = client.from("timers")
                        .select()
                        .decodeList<Timer>()

                    // Sortiere nach Zeit
                    response.sortedBy {
                        try {
                            ZonedDateTime.parse(it.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                .toInstant()
                                .toEpochMilli()
                        } catch (e: Exception) {
                            Long.MAX_VALUE
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TimerWidget", "❌ Supabase-Fehler: ${e.message}", e)
                    null
                }
            }

            if (timers != null) {
                Log.d("TimerWidget", "✅ ${timers.size} Timer von Supabase geladen")

                // 2. Cache aktualisieren
                WidgetDataCache.cacheTimers(context, timers)

                // 3. ALLE Widgets aktualisieren
                TimerWidget().updateAll(context)

                Log.d("TimerWidget", "✅ Widget aktualisiert!")
            } else {
                Log.w("TimerWidget", "⚠️ Keine Daten erhalten")
                // Trotzdem Widget aktualisieren mit Cache-Daten
                TimerWidget().updateAll(context)
            }
        } catch (e: Exception) {
            Log.e("TimerWidget", "❌ Refresh fehlgeschlagen: ${e.message}", e)
        }
    }
}

/**
 * ActionCallback: Timer direkt im Widget als erledigt markieren.
 */
class CompleteTimerAction : ActionCallback {

    companion object {
        val TIMER_ID_KEY = ActionParameters.Key<String>("timer_id")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val timerId = parameters[TIMER_ID_KEY] ?: return
        Log.d("TimerWidget", "✅ Timer erledigt markiert: $timerId")

        try {
            withContext(Dispatchers.IO) {
                SupabaseClient.client.from("timers")
                    .update({ set("is_completed", true) }) {
                        filter { eq("id", timerId) }
                    }
            }

            // Cache aktualisieren und Widget refreshen
            WidgetDataCache.refreshFromServer(context)
            TimerWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e("TimerWidget", "❌ Fehler beim Erledigen: ${e.message}", e)
            TimerWidget().updateAll(context)
        }
    }
}

// Dark Mode Farben
private val PrimaryColor = Color(0xFF64B5F6)
private val BackgroundColor = Color(0xFF1E1E1E)
private val CardColor = Color(0xFF2D2D2D)
private val TextColor = Color(0xFFE0E0E0)
private val SubtextColor = Color(0xFF9E9E9E)
private val DividerColor = Color(0xFF424242)
private val UrgentColor = Color(0xFFEF5350)
private val WarningColor = Color(0xFFFFB74D)
private val AccentColor = Color(0xFF81C784)
private val CompletedColor = Color(0xFF4CAF50)

@Composable
private fun TimerWidgetContent(timers: List<WidgetTimer>) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BackgroundColor))
            .cornerRadius(16.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header mit Aktualisieren-Button
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                // Titel - klickbar öffnet App
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    Text(
                        text = "⏰ Nächste Timer",
                        style = TextStyle(
                            color = ColorProvider(PrimaryColor),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Aktualisieren-Button
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .cornerRadius(18.dp)
                        .background(ColorProvider(CardColor))
                        .clickable(actionRunCallback<RefreshWidgetAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔄",
                        style = TextStyle(
                            fontSize = 18.sp
                        )
                    )
                }
            }

            if (timers.isEmpty()) {
                // Empty State
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Keine Timer",
                            style = TextStyle(
                                color = ColorProvider(SubtextColor),
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Tippe zum Erstellen",
                            style = TextStyle(
                                color = ColorProvider(PrimaryColor),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            } else {
                // Timer List
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    timers.forEach { timer ->
                        TimerListItem(timer = timer)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerListItem(timer: WidgetTimer) {
    val targetTime = DateTimeUtils.parseIsoDateTime(timer.target_time)
    val timeText = if (targetTime != null) {
        DateTimeUtils.getTimeUntilText(targetTime)
    } else {
        "Unbekannt"
    }

    // Urgency-basierte Farbe
    val urgencyColor = if (targetTime != null) {
        val minutesUntil = DateTimeUtils.getMinutesUntil(targetTime)
        when {
            minutesUntil < 0 -> UrgentColor
            minutesUntil < 60 -> WarningColor
            else -> AccentColor
        }
    } else {
        SubtextColor
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timer-Info (klickbar öffnet App)
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Text(
                text = timer.name,
                style = TextStyle(
                    color = ColorProvider(TextColor),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Row(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = timeText,
                    style = TextStyle(
                        color = ColorProvider(urgencyColor),
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = timer.category,
                    style = TextStyle(
                        color = ColorProvider(SubtextColor),
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Erledigt-Button
        Box(
            modifier = GlanceModifier
                .size(30.dp)
                .cornerRadius(15.dp)
                .background(ColorProvider(CardColor))
                .clickable(
                    actionRunCallback<CompleteTimerAction>(
                        actionParametersOf(CompleteTimerAction.TIMER_ID_KEY to timer.id)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                style = TextStyle(
                    color = ColorProvider(CompletedColor),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    // Trennlinie
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ColorProvider(DividerColor))
    ) {}
}

/**
 * Vereinfachtes Timer-Model für das Widget.
 */
@Serializable
data class WidgetTimer(
    val id: String = "",
    val name: String = "",
    val target_time: String = "",
    val category: String = "",
    val is_completed: Boolean = false
)
