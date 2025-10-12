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
    
    companion object {
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_PRE_REMINDER_ENABLED = "pre_reminder_enabled"
        private const val KEY_PRE_REMINDER_MINUTES = "pre_reminder_minutes"
        
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
