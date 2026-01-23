package com.example.timerapp.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

    companion object {
        private const val TAG = "TimerWidget"
    }

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "🔄 provideGlance aufgerufen")
        val result = fetchUpcomingTimers(context)

        provideContent {
            TimerWidgetContent(
                timers = result.timers,
                errorMessage = result.errorMessage
            )
        }
    }

    private suspend fun fetchUpcomingTimers(context: Context): WidgetResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📡 Lade Timer aus Supabase...")
                val now = ZonedDateTime.now()
                val result = com.example.timerapp.SupabaseClient.client
                    .from("timers")
                    .select {
                        filter {
                            eq("is_completed", false)
                        }
                        order("target_time", Order.ASCENDING)
                        limit(10)
                    }
                    .decodeList<WidgetTimer>()

                Log.d(TAG, "📥 ${result.size} Timer aus DB geladen")

                // Filtere nur zukünftige Timer
                val filteredTimers = result.filter { timer ->
                    val targetTime = DateTimeUtils.parseIsoDateTime(timer.target_time)
                    val isFuture = targetTime != null && targetTime.isAfter(now)
                    if (!isFuture) {
                        Log.d(TAG, "⏭️ Timer übersprungen (vergangen): ${timer.name}")
                    }
                    isFuture
                }.take(5)

                Log.d(TAG, "✅ ${filteredTimers.size} zukünftige Timer gefunden")
                WidgetResult(timers = filteredTimers, errorMessage = null)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Fehler beim Laden: ${e.message}", e)
                WidgetResult(timers = emptyList(), errorMessage = e.message ?: "Unbekannter Fehler")
            }
        }
    }
}

data class WidgetResult(
    val timers: List<WidgetTimer>,
    val errorMessage: String?
)

// Farben als Konstanten
private val PrimaryColorDay = Color(0xFF1976D2)
private val BackgroundColorDay = Color(0xFFF5F5F5)
private val TextColorDay = Color(0xFF212121)
private val SubtextColorDay = Color(0xFF757575)
private val DividerColorDay = Color(0xFFE0E0E0)
private val UrgentColorDay = Color(0xFFD32F2F)
private val WarningColorDay = Color(0xFFE65100)
private val ErrorColorDay = Color(0xFFB71C1C)

@Composable
private fun TimerWidgetContent(
    timers: List<WidgetTimer>,
    errorMessage: String?
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BackgroundColorDay))
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
                    text = "⏰ Nächste Timer",
                    style = TextStyle(
                        color = ColorProvider(PrimaryColorDay),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            when {
                errorMessage != null -> {
                    // Error State
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚠️ Fehler",
                                style = TextStyle(
                                    color = ColorProvider(ErrorColorDay),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = "Tippe zum Öffnen",
                                style = TextStyle(
                                    color = ColorProvider(SubtextColorDay),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
                timers.isEmpty() -> {
                    // Empty State
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✓ Keine Timer",
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
                }
                else -> {
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
