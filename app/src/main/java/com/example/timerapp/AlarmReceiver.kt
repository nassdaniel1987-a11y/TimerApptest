package com.example.timerapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.timerapp.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            // ✅ WICHTIG: Stoppe Eskalation und räume Handler auf
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

                mediaPlayer = MediaPlayer().apply {
                    // ✅ KRITISCH: Verwende ALARM-Stream statt MUSIC-Stream
                    // Dadurch funktioniert der Alarm auch wenn das Handy auf Stumm/Vibration ist!
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        setAudioStreamType(AudioManager.STREAM_ALARM)
                    }

                    setDataSource(context, alarmUri)
                    isLooping = true
                    setVolume(if (escalate) 0.5f else 1.0f, if (escalate) 0.5f else 1.0f)
                    prepare()
                    start()
                }

                Log.d("AlarmReceiver", "🔊 Sound gestartet mit ALARM-Stream (Lautstärke: ${if (escalate) "50%" else "100%"})")

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
        // ✅ KRITISCH: Behandle DISMISS_ALARM Action
        if (intent.action == "DISMISS_ALARM") {
            Log.d("AlarmReceiver", "🔇 Dismiss-Action empfangen - stoppe Alarm")
            stopAlarmSound()
            stopVibration()

            // Entferne Notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancelAll()
            return
        }

        // ✅ NEU: Unterstützt gruppierte Timer
        val timerIds = intent.getStringArrayExtra("TIMER_IDS")
        val timerNames = intent.getStringArrayExtra("TIMER_NAMES")
        val timerCategories = intent.getStringArrayExtra("TIMER_CATEGORIES")
        val isPreReminder = intent.getBooleanExtra("IS_PRE_REMINDER", false)

        if (timerIds != null && timerNames != null && timerCategories != null) {
            // ✅ WICHTIG: Prüfe ob Timer noch existieren
            // Verhindert Alarme von gelöschten Timern
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Timer aus Datenbank laden
                    val repository = com.example.timerapp.repository.TimerRepository()
                    repository.refreshTimers()

                    // Hole aktuelle Timer-IDs
                    val currentTimerIds = repository.timers.value.map { it.id }.toSet()

                    // Filtere nur noch existierende Timer
                    val validIndices = timerIds.indices.filter { currentTimerIds.contains(timerIds[it]) }

                    if (validIndices.isEmpty()) {
                        Log.d("AlarmReceiver", "⏭️ Alle Timer wurden gelöscht - Alarm wird ignoriert")
                        return@launch
                    }

                    val validTimerIds = validIndices.map { timerIds[it] }
                    val validTimerNames = validIndices.map { timerNames[it] }
                    val validTimerCategories = validIndices.map { timerCategories[it] }

                    Log.d("AlarmReceiver", "🔔 Gruppen-Alarm ausgelöst für ${validTimerIds.size} Timer (${timerIds.size - validTimerIds.size} gelöscht)")

                    // ✅ KRITISCH: Starte Sound & Vibration SOFORT (nicht erst in Activity!)
                    // Dadurch funktioniert der Alarm IMMER, auch wenn Fullscreen blockiert wird
                    withContext(Dispatchers.Main) {
                        if (!isPreReminder) {
                            // Hole Settings
                            val settingsManager = SettingsManager.getInstance(context)

                            // Starte Sound wenn aktiviert
                            if (settingsManager.isSoundEnabled) {
                                playAlarmSound(context, escalate = settingsManager.isEscalatingAlarmEnabled)
                                Log.d("AlarmReceiver", "🔊 Alarm-Sound gestartet")
                            }

                            // Starte Vibration wenn aktiviert
                            if (settingsManager.isVibrationEnabled) {
                                startVibration(context, intense = false)
                                Log.d("AlarmReceiver", "📳 Vibration gestartet")
                            }
                        }

                        // Zeige Benachrichtigung (mit Fullscreen-Intent als Bonus)
                        NotificationHelper.showGroupedTimerNotification(
                            context = context,
                            timerIds = validTimerIds,
                            timerNames = validTimerNames,
                            timerCategories = validTimerCategories,
                            isPreReminder = isPreReminder
                        )
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "❌ KRITISCH: Fehler beim Validieren der Timer: ${e.message} - Alarm wird NICHT angezeigt!")
                    // WICHTIG: Bei Fehler KEINEN Alarm anzeigen!
                    // Lieber einen Alarm verpassen als einen gelöschten Timer anzeigen
                }
            }
        } else {
            // Fallback: Einzelner Timer (alte Implementierung)
            val timerId = intent.getStringExtra("TIMER_ID") ?: return
            val timerName = intent.getStringExtra("TIMER_NAME") ?: "Timer"
            val timerCategory = intent.getStringExtra("TIMER_CATEGORY") ?: "Allgemein"

            // ✅ Prüfe ob Timer noch existiert
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = com.example.timerapp.repository.TimerRepository()
                    repository.refreshTimers()

                    val timerExists = repository.timers.value.any { it.id == timerId }

                    if (!timerExists) {
                        Log.d("AlarmReceiver", "⏭️ Timer wurde gelöscht - Alarm wird ignoriert: $timerId")
                        return@launch
                    }

                    Log.d("AlarmReceiver", "🔔 Einzelner Alarm ausgelöst: $timerName")

                    // ✅ KRITISCH: Starte Sound & Vibration SOFORT (nicht erst in Activity!)
                    withContext(Dispatchers.Main) {
                        if (!isPreReminder) {
                            // Hole Settings
                            val settingsManager = SettingsManager.getInstance(context)

                            // Starte Sound wenn aktiviert
                            if (settingsManager.isSoundEnabled) {
                                playAlarmSound(context, escalate = settingsManager.isEscalatingAlarmEnabled)
                                Log.d("AlarmReceiver", "🔊 Alarm-Sound gestartet")
                            }

                            // Starte Vibration wenn aktiviert
                            if (settingsManager.isVibrationEnabled) {
                                startVibration(context, intense = false)
                                Log.d("AlarmReceiver", "📳 Vibration gestartet")
                            }
                        }

                        // Zeige Benachrichtigung (mit Fullscreen-Intent als Bonus)
                        NotificationHelper.showTimerNotification(
                            context = context,
                            timerId = timerId,
                            timerName = timerName,
                            timerCategory = timerCategory,
                            isPreReminder = isPreReminder
                        )
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "❌ KRITISCH: Fehler beim Validieren des Timers: ${e.message} - Alarm wird NICHT angezeigt!")
                    // WICHTIG: Bei Fehler KEINEN Alarm anzeigen!
                    // Lieber einen Alarm verpassen als einen gelöschten Timer anzeigen
                }
            }
        }
    }
}