package com.example.timerapp.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.timerapp.MainActivity
import com.example.timerapp.utils.DateTimeUtils
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Timer Widget - Zeigt die nächsten anstehenden Timer als Liste an.
 * Liest Daten aus dem lokalen Cache (WidgetDataCache).
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

        provideContent {
            TimerWidgetContent(timers = upcomingTimers)
        }
    }
}

/**
 * ActionCallback für den Aktualisieren-Button.
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("TimerWidget", "🔄 Manuelles Widget-Update angefordert")
        TimerWidget().update(context, glanceId)
    }
}

// Farben als Konstanten
private val PrimaryColorDay = Color(0xFF1976D2)
private val BackgroundColorDay = Color(0xFFF5F5F5)
private val TextColorDay = Color(0xFF212121)
private val SubtextColorDay = Color(0xFF757575)
private val DividerColorDay = Color(0xFFE0E0E0)
private val UrgentColorDay = Color(0xFFD32F2F)
private val WarningColorDay = Color(0xFFE65100)

@Composable
private fun TimerWidgetContent(timers: List<WidgetTimer>) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BackgroundColorDay))
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
                            color = ColorProvider(PrimaryColorDay),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Aktualisieren-Button
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .cornerRadius(16.dp)
                        .background(ColorProvider(PrimaryColorDay.copy(alpha = 0.1f)))
                        .clickable(actionRunCallback<RefreshWidgetAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔄",
                        style = TextStyle(
                            fontSize = 16.sp
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
                                color = ColorProvider(SubtextColorDay),
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Tippe zum Erstellen",
                            style = TextStyle(
                                color = ColorProvider(PrimaryColorDay),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            } else {
                // Timer List
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity<MainActivity>())
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
            minutesUntil < 0 -> UrgentColorDay
            minutesUntil < 60 -> WarningColorDay
            else -> PrimaryColorDay
        }
    } else {
        SubtextColorDay
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Timer Name
        Text(
            text = timer.name,
            style = TextStyle(
                color = ColorProvider(TextColorDay),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )

        // Zeit + Kategorie
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = timeText,
                style = TextStyle(
                    color = ColorProvider(urgencyColor),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = timer.category,
                style = TextStyle(
                    color = ColorProvider(SubtextColorDay),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        // Trennlinie
        Spacer(modifier = GlanceModifier.height(4.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ColorProvider(DividerColorDay))
        ) {}
    }
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
