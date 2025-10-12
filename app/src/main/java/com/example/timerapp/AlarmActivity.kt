package com.example.timerapp

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.timerapp.screens.AlarmFullscreenScreen
import com.example.timerapp.ui.theme.TimerAppTheme

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen aktivieren (auch bei gesperrtem Bildschirm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // ✅ NEU: Unterstützt mehrere Timer
        val timerNames = intent.getStringArrayExtra("TIMER_NAMES")
        val timerCategories = intent.getStringArrayExtra("TIMER_CATEGORIES")

        val names = timerNames?.toList() ?: listOf(intent.getStringExtra("TIMER_NAME") ?: "Timer")
        val categories = timerCategories?.toList() ?: listOf(intent.getStringExtra("TIMER_CATEGORY") ?: "")

        // Die Activity startet jetzt selbst Ton und Vibration.
        val settingsManager = SettingsManager.getInstance(this)
        if (settingsManager.isSoundEnabled) {
            AlarmReceiver.playAlarmSound(this)
        }
        if (settingsManager.isVibrationEnabled) {
            AlarmReceiver.startVibration(this)
        }

        setContent {
            TimerAppTheme {
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
                        // Optional: Snooze implementieren
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
}