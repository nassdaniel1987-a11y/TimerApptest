package com.example.timerapp.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.timerapp.MainActivity
import com.example.timerapp.R
import com.example.timerapp.repository.TimerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * ✅ Intelligente Erinnerungen Worker
 * Läuft täglich um 20:00 Uhr und zeigt eine Zusammenfassung der Timer für morgen
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "daily_reminder_channel"
        private const val NOTIFICATION_ID = 1000
        const val WORK_NAME = "daily_reminder_work"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("DailyReminderWorker", "🔔 Starte tägliche Erinnerung...")

            // Hole alle aktiven Timer
            val repository = TimerRepository()
            repository.refreshTimers()
            val allTimers = repository.timers.value.filter { !it.is_completed }

            // Filtere Timer für morgen (nutzt System-Timezone)
            val userZone = ZoneId.systemDefault()
            val tomorrow = LocalDate.now(userZone).plusDays(1)

            val tomorrowTimers = allTimers.filter { timer ->
                try {
                    val targetTime = ZonedDateTime.parse(
                        timer.target_time,
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    )
                    targetTime.toLocalDate() == tomorrow
                } catch (e: Exception) {
                    false
                }
            }

            // Zeige Benachrichtigung nur wenn Timer für morgen vorhanden sind
            if (tomorrowTimers.isNotEmpty()) {
                showDailySummaryNotification(tomorrowTimers.size, tomorrowTimers)
                Log.d("DailyReminderWorker", "✅ Erinnerung gesendet: ${tomorrowTimers.size} Timer für morgen")
            } else {
                Log.d("DailyReminderWorker", "ℹ️ Keine Timer für morgen")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("DailyReminderWorker", "❌ Fehler bei täglicher Erinnerung: ${e.message}")
            Result.retry()
        }
    }

    private fun showDailySummaryNotification(timerCount: Int, timers: List<com.example.timerapp.models.Timer>) {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Erstelle Benachrichtigungstext
        val title = if (timerCount == 1) {
            "1 Timer für morgen"
        } else {
            "$timerCount Timer für morgen"
        }

        val text = if (timerCount <= 3) {
            timers.joinToString("\n") { "• ${it.name}" }
        } else {
            "${timers.take(2).joinToString("\n") { "• ${it.name}" }}\n• und ${timerCount - 2} weitere..."
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Vergiss nicht deine Timer für morgen!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tägliche Erinnerungen",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Zeigt eine Zusammenfassung deiner Timer für den nächsten Tag"
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
