package com.example.timerapp.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.timerapp.utils.DateTimeUtils
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Steuert minütliche Widget-Updates für den Echtzeit-Countdown.
 *
 * Strategie:
 * - Timer < 1 Stunde: Update jede Minute (Countdown tickt sichtbar)
 * - Timer < 24 Stunden: Update alle 15 Minuten
 * - Keine nahen Timer: Kein Auto-Update (System-Update alle 30 Min reicht)
 */
object WidgetAutoUpdater {

    private const val TAG = "WidgetAutoUpdater"
    private const val REQUEST_CODE = 9876

    /**
     * Plant das nächste Widget-Update basierend auf dem nächsten Timer.
     */
    fun scheduleNextUpdate(context: Context, timers: List<WidgetTimer>) {
        val now = ZonedDateTime.now()

        // Finde den nächsten Timer und berechne wie weit er entfernt ist
        val nearestMinutes = timers.mapNotNull { timer ->
            DateTimeUtils.parseIsoDateTime(timer.target_time)?.let { targetTime ->
                ChronoUnit.MINUTES.between(now, targetTime)
            }
        }.filter { it > 0 }.minOrNull()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createUpdatePendingIntent(context)

        if (nearestMinutes == null) {
            // Keine aktiven Timer → Auto-Update stoppen
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "⏹ Kein Auto-Update nötig (keine aktiven Timer)")
            return
        }

        val delayMillis = when {
            nearestMinutes <= 60 -> 60_000L     // Jede Minute
            nearestMinutes <= 1440 -> 900_000L  // Alle 15 Minuten
            else -> {
                alarmManager.cancel(pendingIntent)
                Log.d(TAG, "⏹ Timer > 24h, kein Auto-Update")
                return
            }
        }

        val triggerAtMillis = System.currentTimeMillis() + delayMillis

        try {
            alarmManager.set(
                AlarmManager.RTC,
                triggerAtMillis,
                pendingIntent
            )
            Log.d(TAG, "⏰ Nächstes Widget-Update in ${delayMillis / 1000}s (nächster Timer in ${nearestMinutes}min)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Auto-Update scheduling fehlgeschlagen: ${e.message}")
        }
    }

    /**
     * Stoppt alle geplanten Auto-Updates.
     */
    fun cancelAutoUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(createUpdatePendingIntent(context))
        Log.d(TAG, "⏹ Auto-Update gestoppt")
    }

    private fun createUpdatePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, TimerWidgetReceiver::class.java).apply {
            action = "com.example.timerapp.WIDGET_AUTO_UPDATE"
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
