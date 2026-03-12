package com.example.timerapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.timerapp.MainActivity
import com.example.timerapp.R
import com.example.timerapp.SettingsManager
import com.example.timerapp.SupabaseClient
import com.example.timerapp.repository.TimerRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FcmServiceEntryPoint {
        fun timerRepository(): TimerRepository
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val repository: TimerRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            FcmServiceEntryPoint::class.java
        ).timerRepository()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Neuer FCM Token: $token")

        // Token sofort in Supabase aktualisieren
        val tokenManager = FcmTokenManager(applicationContext, SupabaseClient.client)
        serviceScope.launch {
            tokenManager.registerToken()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM Message from: ${message.from}")

        val settingsManager = SettingsManager.getInstance(applicationContext)

        // Notification-Payload (wenn App im Vordergrund)
        if (!settingsManager.isAppPaused) {
            message.notification?.let { notification ->
                showNotification(
                    title = notification.title ?: "TimerApp",
                    body = notification.body ?: ""
                )
            }
        } else {
            Log.d(TAG, "Push-Benachrichtigung unterdrückt (Alarme pausiert)")
        }

        // Data-Payload: Hintergrund-Sync immer ausführen (auch wenn pausiert)
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "FCM Data: ${message.data}")
            handleDataMessage(message.data, settingsManager)
        }
    }

    private fun handleDataMessage(data: Map<String, String>, settingsManager: SettingsManager) {
        val type = data["type"] ?: return

        // Hintergrund-Sync: Lokale Daten aktualisieren bei jeder Änderung
        serviceScope.launch {
            try {
                repository.refreshTimers()
                repository.refreshCategories()
                Log.d(TAG, "Hintergrund-Sync nach FCM abgeschlossen")
            } catch (e: Exception) {
                Log.e(TAG, "Hintergrund-Sync fehlgeschlagen: ${e.message}")
            }
        }

        // Keine Push-Benachrichtigungen anzeigen wenn Alarme pausiert sind
        if (settingsManager.isAppPaused) {
            Log.d(TAG, "Push-Benachrichtigung unterdrückt (Alarme pausiert): $type")
            return
        }

        when (type) {
            "timer_created" -> {
                showNotification(
                    title = data["title"] ?: "Neuer Timer",
                    body = data["body"] ?: "Ein neuer Timer wurde erstellt"
                )
            }
            "timer_deleted" -> {
                showNotification(
                    title = data["title"] ?: "Timer gelöscht",
                    body = data["body"] ?: "Ein Timer wurde gelöscht"
                )
            }
            "timer_expired" -> {
                showNotification(
                    title = data["title"] ?: "Timer abgelaufen",
                    body = data["body"] ?: "Ein Timer ist abgelaufen!"
                )
            }
            "sync" -> {
                Log.d(TAG, "Sync-Nachricht empfangen")
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "fcm_default"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Push-Benachrichtigungen",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Benachrichtigungen von anderen Geräten"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val TAG = "FCMService"
    }
}
