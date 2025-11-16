package com.example.timerapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.timerapp.utils.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
        var vibrator: Vibrator? = null
        private var escalationHandler: android.os.Handler? = null
        private var escalationRunnable: Runnable? = null

        fun stopAlarmSound() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                mediaPlayer = null
            }
            // Stoppe Eskalation
            escalationHandler?.removeCallbacksAndMessages(null)
            escalationHandler = null
            escalationRunnable = null
            Log.d("AlarmReceiver", "🔇 Sound gestoppt")
        }

        fun stopVibration() {
            vibrator?.cancel()
            vibrator = null
            Log.d("AlarmReceiver", "📴 Vibration gestoppt")
        }

        fun playAlarmSound(context: Context, escalate: Boolean = false) {
            try {
                stopAlarmSound() // Falls noch läuft

                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                mediaPlayer = MediaPlayer.create(context, alarmUri).apply {
                    isLooping = true
                    setVolume(if (escalate) 0.5f else 1.0f, if (escalate) 0.5f else 1.0f)
                    start()
                }

                Log.d("AlarmReceiver", "🔊 Sound gestartet (Lautstärke: ${if (escalate) "50%" else "100%"})")

                // ✅ Eskalierender Alarm: Nach 60 Sekunden lauter
                if (escalate) {
                    val settingsManager = SettingsManager.getInstance(context)
                    if (settingsManager.isEscalatingAlarmEnabled) {
                        escalationHandler = android.os.Handler(android.os.Looper.getMainLooper())
                        escalationRunnable = Runnable {
                            mediaPlayer?.setVolume(1.0f, 1.0f)
                            // Intensivere Vibration
                            startVibration(context, intense = true)
                            Log.d("AlarmReceiver", "📈 Alarm eskaliert (100% Lautstärke)")
                        }
                        escalationHandler?.postDelayed(escalationRunnable!!, 60000) // 60 Sekunden
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "❌ Fehler beim Sound: ${e.message}")
            }
        }

        fun startVibration(context: Context, intense: Boolean = false) {
            try {
                vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                val pattern = if (intense) {
                    longArrayOf(0, 500, 500) // Schnellere Vibration
                } else {
                    longArrayOf(0, 1000, 1000) // Normale Vibration
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(pattern, 0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }

                Log.d("AlarmReceiver", "📳 Vibration gestartet (${if (intense) "intensiv" else "normal"})")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "❌ Fehler bei Vibration: ${e.message}")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // ✅ NEU: Unterstützt gruppierte Timer
        val timerIds = intent.getStringArrayExtra("TIMER_IDS")
        val timerNames = intent.getStringArrayExtra("TIMER_NAMES")
        val timerCategories = intent.getStringArrayExtra("TIMER_CATEGORIES")
        val isPreReminder = intent.getBooleanExtra("IS_PRE_REMINDER", false)

        if (timerIds != null && timerNames != null && timerCategories != null) {
            // Gruppierter Alarm
            Log.d("AlarmReceiver", "🔔 Gruppen-Alarm ausgelöst für ${timerIds.size} Timer")

            NotificationHelper.showGroupedTimerNotification(
                context = context,
                timerIds = timerIds.toList(),
                timerNames = timerNames.toList(),
                timerCategories = timerCategories.toList(),
                isPreReminder = isPreReminder
            )
        } else {
            // Fallback: Einzelner Timer (alte Implementierung)
            val timerId = intent.getStringExtra("TIMER_ID") ?: return
            val timerName = intent.getStringExtra("TIMER_NAME") ?: "Timer"
            val timerCategory = intent.getStringExtra("TIMER_CATEGORY") ?: "Allgemein"

            Log.d("AlarmReceiver", "🔔 Einzelner Alarm ausgelöst: $timerName")

            NotificationHelper.showTimerNotification(
                context = context,
                timerId = timerId,
                timerName = timerName,
                timerCategory = timerCategory,
                isPreReminder = isPreReminder
            )
        }
    }
}