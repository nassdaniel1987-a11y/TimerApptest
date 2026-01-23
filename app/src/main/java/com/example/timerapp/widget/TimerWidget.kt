package com.example.timerapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.timerapp.MainActivity
import com.example.timerapp.utils.DateTimeUtils
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Timer Widget - Zeigt die nächsten anstehenden Timer als Liste an.
 */
class TimerWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val timers = fetchUpcomingTimers(context)

        provideContent {
            TimerWidgetContent(timers = timers)
        }
    }

    private suspend fun fetchUpcomingTimers(context: Context): List<WidgetTimer> {
        return withContext(Dispatchers.IO) {
            try {
                val now = ZonedDateTime.now()
                val result = com.example.timerapp.SupabaseClient.client
                    .from("timers")
                    .select {
                        filter {
                            eq("is_completed", false)
                        }
                        order("target_time", Order.ASCENDING)
                        limit(5)
                    }
                    .decodeList<WidgetTimer>()

                // Filtere nur zukünftige Timer
                result.filter { timer ->
                    val targetTime = DateTimeUtils.parseIsoDateTime(timer.target_time)
                    targetTime != null && targetTime.isAfter(now)
                }.take(5)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

@Composable
private fun TimerWidgetContent(timers: List<WidgetTimer>) {
    val primaryColor = ColorProvider(
        day = androidx.compose.ui.graphics.Color(0xFF1976D2),
        night = androidx.compose.ui.graphics.Color(0xFF90CAF9)
    )
    val backgroundColor = ColorProvider(
        day = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
        night = androidx.compose.ui.graphics.Color(0xFF1E1E1E)
    )
    val textColor = ColorProvider(
        day = androidx.compose.ui.graphics.Color(0xFF212121),
        night = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
    )
    val subtextColor = ColorProvider(
        day = androidx.compose.ui.graphics.Color(0xFF757575),
        night = androidx.compose.ui.graphics.Color(0xFFBDBDBD)
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nächste Timer",
                    style = TextStyle(
                        color = primaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (timers.isEmpty()) {
                // Empty State
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine Timer",
                        style = TextStyle(
                            color = subtextColor,
                            fontSize = 14.sp
                        )
                    )
                }
            } else {
                // Timer List
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    items(timers) { timer ->
                        TimerListItem(
                            timer = timer,
                            textColor = textColor,
                            subtextColor = subtextColor,
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerListItem(
    timer: WidgetTimer,
    textColor: ColorProvider,
    subtextColor: ColorProvider,
    primaryColor: ColorProvider
) {
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
            minutesUntil < 0 -> ColorProvider(
                day = androidx.compose.ui.graphics.Color(0xFFD32F2F),
                night = androidx.compose.ui.graphics.Color(0xFFEF5350)
            )
            minutesUntil < 60 -> ColorProvider(
                day = androidx.compose.ui.graphics.Color(0xFFE65100),
                night = androidx.compose.ui.graphics.Color(0xFFFF9800)
            )
            else -> primaryColor
        }
    } else {
        subtextColor
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        // Timer Name
        Text(
            text = timer.name,
            style = TextStyle(
                color = textColor,
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
                    color = urgencyColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = timer.category,
                style = TextStyle(
                    color = subtextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        // Trennlinie
        Spacer(modifier = GlanceModifier.height(6.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ColorProvider(
                    day = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
                    night = androidx.compose.ui.graphics.Color(0xFF424242)
                ))
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
