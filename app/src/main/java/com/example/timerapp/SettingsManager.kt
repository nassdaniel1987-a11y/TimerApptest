package com.example.timerapp

import android.content.Context
import android.content.SharedPreferences

class SettingsManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "timer_app_settings",
        Context.MODE_PRIVATE
    )
    
    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
    
    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()
    
    var isPreReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_PRE_REMINDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PRE_REMINDER_ENABLED, value).apply()
    
    var preReminderMinutes: Int
        get() = prefs.getInt(KEY_PRE_REMINDER_MINUTES, 10)
        set(value) = prefs.edit().putInt(KEY_PRE_REMINDER_MINUTES, value).apply()

    var snoozeMinutes: Int
        get() = prefs.getInt(KEY_SNOOZE_MINUTES, 10)
        set(value) = prefs.edit().putInt(KEY_SNOOZE_MINUTES, value).apply()

    var isDarkModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE_ENABLED, value).apply()

    var isHapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, value).apply()

    var isEscalatingAlarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_ESCALATING_ALARM_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ESCALATING_ALARM_ENABLED, value).apply()

    var isAppPaused: Boolean
        get() = prefs.getBoolean(KEY_APP_PAUSED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_PAUSED, value).apply()

    var alarmSoundUri: String?
        get() = prefs.getString(KEY_ALARM_SOUND_URI, null)
        set(value) = prefs.edit().putString(KEY_ALARM_SOUND_URI, value).apply()

    var alarmSoundName: String
        get() = prefs.getString(KEY_ALARM_SOUND_NAME, "Standard-Alarm") ?: "Standard-Alarm"
        set(value) = prefs.edit().putString(KEY_ALARM_SOUND_NAME, value).apply()

    companion object {
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_PRE_REMINDER_ENABLED = "pre_reminder_enabled"
        private const val KEY_PRE_REMINDER_MINUTES = "pre_reminder_minutes"
        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
        private const val KEY_DARK_MODE_ENABLED = "dark_mode_enabled"
        private const val KEY_HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"
        private const val KEY_ESCALATING_ALARM_ENABLED = "escalating_alarm_enabled"
        private const val KEY_APP_PAUSED = "app_paused"
        private const val KEY_ALARM_SOUND_URI = "alarm_sound_uri"
        private const val KEY_ALARM_SOUND_NAME = "alarm_sound_name"
        
        @Volatile
        private var instance: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
