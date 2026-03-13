package com.example.timerapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

import android.os.Build
import android.util.Log
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
                NotificationManager.IMPORTANCE_HIGH // HIGH ist korrekt für Fullscreen
            ).apply {
                description = "Benachrichtigungen für Timer-Alarme (Fullscreen-Alarme)"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true) // Bypass "Nicht stören" Modus
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                // ✅ FIX: Kein Sound auf dem Channel!
                // Sound wird über AlarmReceiver.playAlarmSound() (MediaPlayer) gesteuert.
                // Channel-Sound würde zu doppeltem Audio und Interferenz führen.
                setSound(null, null)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            android.util.Log.d("NotificationHelper", "✅ Notification Channel erstellt (Importance: HIGH, Bypass DND: true, ALARM-Stream)")
        }
    }

    // ✅ Channel neu erstellen wenn sich der Sound ändert
    // Android erlaubt keine nachträgliche Änderung des Channel-Sounds
    fun recreateNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
            createNotificationChannel(context)
            Log.d("NotificationHelper", "🔄 Notification Channel neu erstellt (Sound geändert)")
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

        // ✅ Intent für Vollbild-Alarm mit allen Timer-Daten
        val fullscreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("TIMER_IDS", timerIds.toTypedArray())
            putExtra("TIMER_NAMES", timerNames.toTypedArray())
            putExtra("TIMER_CATEGORIES", timerCategories.toTypedArray())
        }
        val fullscreenPendingIntent = PendingIntent.getActivity(
            context,
            groupId.hashCode(),
            fullscreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        Log.d("NotificationHelper", "📱 Erstelle Benachrichtigung für ${timerIds.size} Timer (Pre-Reminder: $isPreReminder)")

        // Intent für Tap auf Benachrichtigung
        // Hauptalarme: AlarmActivity öffnen (damit User Dismiss/Snooze drücken kann)
        // Pre-Reminder: MainActivity öffnen
        val contentIntent = if (!isPreReminder) {
            Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
                putExtra("TIMER_IDS", timerIds.toTypedArray())
                putExtra("TIMER_NAMES", timerNames.toTypedArray())
                putExtra("TIMER_CATEGORIES", timerCategories.toTypedArray())
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            groupId.hashCode() + 1,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Ongoing: Notification kann nicht weggewischt werden
            // User muss "Beenden" drücken oder in AlarmActivity dismissen
            builder.setOngoing(true)
            builder.setAutoCancel(false)

            // Sound wird über AlarmReceiver.playAlarmSound() (MediaPlayer) gesteuert.
            // Notification-Sound ist deaktiviert (Channel hat setSound(null)),
            // aber setSilent(true) darf NICHT verwendet werden - das blockiert den Fullscreen-Intent!
            builder.setSound(null)
            builder.setVibrate(longArrayOf(0L))

            // Dismiss-Action zum Stoppen des Alarms
            val dismissIntent = Intent(context, com.example.timerapp.AlarmReceiver::class.java).apply {
                action = "DISMISS_ALARM"
                putExtra("DISMISS_TIMER_IDS", timerIds.toTypedArray())
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                groupId.hashCode() + 2,
                dismissIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Beenden",
                dismissPendingIntent
            )

            Log.d("NotificationHelper", "🚨 Fullscreen-Intent gesetzt + Notification Sound/Vibration als Fallback")
        } else {
            // Pre-Reminder: Nur sanfte Benachrichtigung, normal wegwischbar
            builder.setAutoCancel(true)
            builder.setSound(null)
            builder.setVibrate(longArrayOf(0L))
            Log.d("NotificationHelper", "Pre-Reminder Benachrichtigung (kein Fullscreen)")
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Prüfe ob Fullscreen-Intent Permission vorhanden ist (Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!notificationManager.canUseFullScreenIntent()) {
                Log.e("NotificationHelper", "❌ KRITISCH: USE_FULL_SCREEN_INTENT Permission fehlt!")
                Log.e("NotificationHelper", "   → Gehe zu Einstellungen → Apps → Timer App → Fullscreen-Benachrichtigungen und aktiviere sie!")
            } else {
                Log.d("NotificationHelper", "✅ Fullscreen-Intent Permission vorhanden")
            }
        }

        // ✅ Prüfe "Nicht stören" Modus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val currentInterruptionFilter = notificationManager.currentInterruptionFilter
            if (currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                Log.w("NotificationHelper", "⚠️ 'Nicht stören' Modus ist aktiv - Fullscreen könnte blockiert sein")
                Log.w("NotificationHelper", "   → Interruption Filter: $currentInterruptionFilter")
            }
        }

        notificationManager.notify(groupId.hashCode(), builder.build())
        Log.d("NotificationHelper", "✅ Benachrichtigung angezeigt (ID: ${groupId.hashCode()})")
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