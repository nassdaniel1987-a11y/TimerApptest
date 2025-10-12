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

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ✅ NEU: Zeigt gruppierte Timer-Benachrichtigung
    fun showGroupedTimerNotification(
        context: Context,
        timerIds: List<String>,
        timerNames: List<String>,
        timerCategories: List<String>,
        isPreReminder: Boolean
    ) {
        if (timerIds.isEmpty()) return

        val groupId = timerIds.joinToString("_")

        // Intent für Vollbild-Alarm mit allen Timer-Daten
        val fullscreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TIMER_IDS", timerIds.toTypedArray())
            putExtra("TIMER_NAMES", timerNames.toTypedArray())
            putExtra("TIMER_CATEGORIES", timerCategories.toTypedArray())
        }
        val fullscreenPendingIntent = PendingIntent.getActivity(
            context,
            groupId.hashCode(),
            fullscreenIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Intent für Tap auf Benachrichtigung
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            groupId.hashCode() + 1,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Titel und Text basierend auf Anzahl der Timer
        val title = if (isPreReminder) {
            "⏰ ${timerNames.size} Timer in Kürze"
        } else {
            "🔔 ${timerNames.size} Timer abgelaufen!"
        }

        val text = when {
            timerNames.size == 1 -> timerNames.first()
            timerNames.size == 2 -> "${timerNames[0]} und ${timerNames[1]}"
            timerNames.size == 3 -> "${timerNames[0]}, ${timerNames[1]} und ${timerNames[2]}"
            else -> "${timerNames[0]}, ${timerNames[1]} und ${timerNames.size - 2} weitere"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        // Erweiterte Ansicht mit allen Timer-Namen
        if (timerNames.size > 1) {
            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)

            timerNames.forEach { name ->
                inboxStyle.addLine(name)
            }

            builder.setStyle(inboxStyle)
        }

        if (!isPreReminder) {
            // Vollbild-Intent nur für Haupt-Alarme
            builder.setFullScreenIntent(fullscreenPendingIntent, true)
            // Ton & Vibration werden manuell in AlarmActivity gesteuert
            builder.setSound(null)
            builder.setVibrate(longArrayOf(0L))
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(groupId.hashCode(), builder.build())
    }

    // ✅ ALTE METHODE: Für Rückwärtskompatibilität
    fun showTimerNotification(
        context: Context,
        timerId: String,
        timerName: String,
        timerCategory: String,
        isPreReminder: Boolean
    ) {
        showGroupedTimerNotification(
            context = context,
            timerIds = listOf(timerId),
            timerNames = listOf(timerName),
            timerCategories = listOf(timerCategory),
            isPreReminder = isPreReminder
        )
    }
}