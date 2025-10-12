package com.example.timerapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.timerapp.AlarmActivity
import com.example.timerapp.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "timer_alarms"
    private const val CHANNEL_NAME = "Timer Alarme"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für Timer-Alarme"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTimerNotification(
        context: Context,
        timerId: String,
        timerName: String,
        isPreReminder: Boolean
    ) {
        // Intent, der beim Tippen auf die Benachrichtigung ausgeführt wird (öffnet die Haupt-App)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            timerId.hashCode() + 1, // Eindeutige Request-Codes verwenden
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Intent, der für den Vollbild-Alarm ausgeführt wird (öffnet die Alarm-Activity)
        val fullscreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TIMER_ID", timerId)
            putExtra("TIMER_NAME", timerName)
        }
        val fullscreenPendingIntent = PendingIntent.getActivity(
            context,
            timerId.hashCode() + 2, // Eindeutige Request-Codes verwenden
            fullscreenIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (isPreReminder) "⏰ Erinnerung" else "🔔 Timer abgelaufen!")
            .setContentText(if (isPreReminder) "$timerName in Kürze" else timerName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        // ----- HIER IST DIE KORREKTUR -----
        if (!isPreReminder) {
            // Dies ist ein Haupt-Alarm.
            // 1. Wir starten die Vollbild-Aktivität.
            builder.setFullScreenIntent(fullscreenPendingIntent, true)

            // 2. Wir schalten den Ton & die Vibration der Benachrichtigung aus,
            // da wir dies manuell im AlarmReceiver steuern.
            builder.setSound(null)
            builder.setVibrate(longArrayOf(0L))
        }
        // Bei einer Vorab-Erinnerung (im 'else'-Fall) verwenden wir
        // einfach den Standard-Ton und die Vibration des Kanals.

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(timerId.hashCode(), builder.build())
    }
}