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

        fun stopAlarmSound() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                mediaPlayer = null
            }
            Log.d("AlarmReceiver", "🔇 Sound gestoppt")
        }

        fun stopVibration() {
            vibrator?.cancel()
            vibrator = null
            Log.d("AlarmReceiver", "📴 Vibration gestoppt")
        }

        // Die Logik zum Starten wurde hierher verschoben, damit die Activity sie aufrufen kann
        fun playAlarmSound(context: Context) {
            try {
                stopAlarmSound() // Falls noch läuft

                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                mediaPlayer = MediaPlayer.create(context, alarmUri).apply {
                    isLooping = true
                    start()
                }

                Log.d("AlarmReceiver", "🔊 Sound gestartet")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "❌ Fehler beim Sound: ${e.message}")
            }
        }

        fun startVibration(context: Context) {
            try {
                vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                val pattern = longArrayOf(0, 1000, 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(pattern, 0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }

                Log.d("AlarmReceiver", "📳 Vibration gestartet")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "❌ Fehler bei Vibration: ${e.message}")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra("TIMER_ID") ?: return
        val timerName = intent.getStringExtra("TIMER_NAME") ?: "Timer"
        val isPreReminder = intent.getBooleanExtra("IS_PRE_REMINDER", false)

        Log.d("AlarmReceiver", "🔔 Alarm ausgelöst: $timerName (Pre: $isPreReminder)")

        // Die Benachrichtigung wird immer noch angezeigt.
        // Sie ist der Auslöser für den Fullscreen-Intent.
        NotificationHelper.showTimerNotification(
            context = context,
            timerId = timerId,
            timerName = timerName,
            isPreReminder = isPreReminder
        )

        // Die manuelle Steuerung von Ton und Vibration wird hier entfernt,
        // da die AlarmActivity dies nun übernimmt.
    }
}