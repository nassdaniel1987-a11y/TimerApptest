package com.example.timerapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.timerapp.screens.AlarmFullscreenScreen
import com.example.timerapp.ui.theme.TimerAppTheme
import java.time.ZonedDateTime

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ KRITISCH: Fullscreen aktivieren (auch bei gesperrtem Bildschirm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // ✅ WICHTIG: Stelle sicher dass Bildschirm an bleibt
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ✅ NEU: Unterstützt mehrere Timer
        val timerIds = intent.getStringArrayExtra("TIMER_IDS")
        val timerNames = intent.getStringArrayExtra("TIMER_NAMES")
        val timerCategories = intent.getStringArrayExtra("TIMER_CATEGORIES")

        val ids = timerIds?.toList() ?: listOf()
        val names = timerNames?.toList() ?: listOf(intent.getStringExtra("TIMER_NAME") ?: "Timer")
        val categories = timerCategories?.toList() ?: listOf(intent.getStringExtra("TIMER_CATEGORY") ?: "")

        // Die Activity startet jetzt selbst Ton und Vibration.
        val settingsManager = SettingsManager.getInstance(this)
        if (settingsManager.isSoundEnabled) {
            AlarmReceiver.playAlarmSound(this, escalate = settingsManager.isEscalatingAlarmEnabled)
        }
        if (settingsManager.isVibrationEnabled) {
            AlarmReceiver.startVibration(this, intense = false)
        }

        setContent {
            TimerAppTheme(darkTheme = settingsManager.isDarkModeEnabled) {
                AlarmFullscreenScreen(
                    timerNames = names,
                    timerCategories = categories,
                    onDismiss = {
                        AlarmReceiver.stopAlarmSound()
                        AlarmReceiver.stopVibration()
                        finish()
                    },
                    onSnooze = {
                        AlarmReceiver.stopAlarmSound()
                        AlarmReceiver.stopVibration()

                        // ✅ Snooze implementiert
                        scheduleSnooze(ids, names, categories, settingsManager.snoozeMinutes)
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stellt sicher, dass alles gestoppt wird, wenn die Activity zerstört wird.
        AlarmReceiver.stopAlarmSound()
        AlarmReceiver.stopVibration()
    }

    // ✅ Snooze-Funktion: Plant Alarm in X Minuten neu
    private fun scheduleSnooze(
        timerIds: List<String>,
        timerNames: List<String>,
        timerCategories: List<String>,
        snoozeMinutes: Int
    ) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeTime = ZonedDateTime.now().plusMinutes(snoozeMinutes.toLong())

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("TIMER_IDS", timerIds.toTypedArray())
            putExtra("TIMER_NAMES", timerNames.toTypedArray())
            putExtra("TIMER_CATEGORIES", timerCategories.toTypedArray())
            putExtra("IS_PRE_REMINDER", false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            "snooze_${System.currentTimeMillis()}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime.toInstant().toEpochMilli(),
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeTime.toInstant().toEpochMilli(),
                pendingIntent
            )
        }
    }
}