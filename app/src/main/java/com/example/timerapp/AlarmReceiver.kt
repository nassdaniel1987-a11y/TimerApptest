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
                // ✅ FIX: Wenn Sound bereits läuft, NICHT neu starten!
                // Verhindert Unterbrechungen bei gruppierten Timern
                if (mediaPlayer?.isPlaying == true) {
                    Log.d("AlarmReceiver", "🔊 Sound läuft bereits - überspringe Neustart")
                    return
                }
                stopAlarmSound() // Nur stoppen wenn nicht mehr aktiv (z.B. Fehler-Zustand)

                val settingsManager = SettingsManager.getInstance(context)

                // Custom-URI verwenden falls gesetzt, sonst System-Default
                val alarmUri = settingsManager.alarmSoundUri?.let { uriString ->
                    try {
                        android.net.Uri.parse(uriString)
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Ungültige Custom-Sound-URI, verwende Standard")
                        null
                    }
                } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
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
                // Fallback: System-Default versuchen falls Custom-Sound fehlschlägt
                try {
                    val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    mediaPlayer = MediaPlayer().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                        }
                        setDataSource(context, fallbackUri)
                        isLooping = true
                        prepare()
                        start()
                    }
                    Log.d("AlarmReceiver", "🔊 Fallback-Sound gestartet")
                } catch (fallbackError: Exception) {
                    Log.e("AlarmReceiver", "❌ Auch Fallback-Sound fehlgeschlagen: ${fallbackError.message}")
                }
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

            // ✅ FIX: Entferne nur die spezifische Notification, nicht ALLE
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val dismissTimerIds = intent.getStringArrayExtra("DISMISS_TIMER_IDS")
            if (dismissTimerIds != null) {
                val groupId = dismissTimerIds.joinToString("_")
                notificationManager.cancel(groupId.hashCode())
                Log.d("AlarmReceiver", "🔕 Notification gecancelt: ${groupId.hashCode()}")
            } else {
                // Fallback: Alle canceln wenn keine spezifische ID vorhanden
                notificationManager.cancelAll()
                Log.d("AlarmReceiver", "🔕 Alle Notifications gecancelt (Fallback)")
            }
            return
        }

        // ✅ Pause-Modus: Wenn App pausiert ist, Alarm ignorieren
        val settingsManager = SettingsManager.getInstance(context)
        if (settingsManager.isAppPaused) {
            Log.d("AlarmReceiver", "⏸️ App ist pausiert - Alarm wird ignoriert")
            return
        }

        // ✅ NEU: Unterstützt gruppierte Timer
        val timerIds = intent.getStringArrayExtra("TIMER_IDS")
        val timerNames = intent.getStringArrayExtra("TIMER_NAMES")
        val timerCategories = intent.getStringArrayExtra("TIMER_CATEGORIES")
        val isPreReminder = intent.getBooleanExtra("IS_PRE_REMINDER", false)

        if (timerIds != null && timerNames != null && timerCategories != null) {
            // ✅ KRITISCH: goAsync() hält den BroadcastReceiver am Leben
            // Ohne goAsync() killt Android den Prozess bevor die Coroutine fertig ist!
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Timer aus Room-Datenbank laden (offline-fähig)
                    val db = androidx.room.Room.databaseBuilder(
                        context,
                        com.example.timerapp.data.AppDatabase::class.java,
                        "timer_database"
                    ).addMigrations(com.example.timerapp.data.AppDatabase.MIGRATION_1_2).build()
                    val currentTimerIds = db.timerDao().getActiveTimersForWidget().map { it.id }.toSet()

                    // Filtere nur noch existierende Timer
                    val validIndices = timerIds.indices.filter { currentTimerIds.contains(timerIds[it]) }

                    if (validIndices.isEmpty()) {
                        Log.d("AlarmReceiver", "⏭️ Alle Timer wurden gelöscht - Alarm wird ignoriert")
                        pendingResult.finish()
                        return@launch
                    }

                    val validTimerIds = validIndices.map { timerIds[it] }
                    val validTimerNames = validIndices.map { timerNames[it] }
                    val validTimerCategories = validIndices.map { timerCategories[it] }

                    Log.d("AlarmReceiver", "🔔 Gruppen-Alarm ausgelöst für ${validTimerIds.size} Timer (${timerIds.size - validTimerIds.size} gelöscht)")

                    withContext(Dispatchers.Main) {
                        if (!isPreReminder) {
                            val settingsManager = SettingsManager.getInstance(context)

                            if (settingsManager.isSoundEnabled) {
                                playAlarmSound(context, escalate = settingsManager.isEscalatingAlarmEnabled)
                                Log.d("AlarmReceiver", "🔊 Alarm-Sound gestartet")
                            }

                            if (settingsManager.isVibrationEnabled) {
                                startVibration(context, intense = false)
                                Log.d("AlarmReceiver", "📳 Vibration gestartet")
                            }
                        }

                        NotificationHelper.showGroupedTimerNotification(
                            context = context,
                            timerIds = validTimerIds,
                            timerNames = validTimerNames,
                            timerCategories = validTimerCategories,
                            isPreReminder = isPreReminder
                        )
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "❌ KRITISCH: Fehler beim Validieren der Timer: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            // Fallback: Einzelner Timer (alte Implementierung)
            val timerId = intent.getStringExtra("TIMER_ID") ?: return
            val timerName = intent.getStringExtra("TIMER_NAME") ?: "Timer"
            val timerCategory = intent.getStringExtra("TIMER_CATEGORY") ?: "Allgemein"

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = androidx.room.Room.databaseBuilder(
                        context,
                        com.example.timerapp.data.AppDatabase::class.java,
                        "timer_database"
                    ).addMigrations(com.example.timerapp.data.AppDatabase.MIGRATION_1_2).build()
                    val timerExists = db.timerDao().getTimerById(timerId) != null

                    if (!timerExists) {
                        Log.d("AlarmReceiver", "⏭️ Timer wurde gelöscht - Alarm wird ignoriert: $timerId")
                        pendingResult.finish()
                        return@launch
                    }

                    Log.d("AlarmReceiver", "🔔 Einzelner Alarm ausgelöst: $timerName")

                    withContext(Dispatchers.Main) {
                        if (!isPreReminder) {
                            val settingsManager = SettingsManager.getInstance(context)

                            if (settingsManager.isSoundEnabled) {
                                playAlarmSound(context, escalate = settingsManager.isEscalatingAlarmEnabled)
                                Log.d("AlarmReceiver", "🔊 Alarm-Sound gestartet")
                            }

                            if (settingsManager.isVibrationEnabled) {
                                startVibration(context, intense = false)
                                Log.d("AlarmReceiver", "📳 Vibration gestartet")
                            }
                        }

                        NotificationHelper.showTimerNotification(
                            context = context,
                            timerId = timerId,
                            timerName = timerName,
                            timerCategory = timerCategory,
                            isPreReminder = isPreReminder
                        )
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "❌ KRITISCH: Fehler beim Validieren des Timers: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}