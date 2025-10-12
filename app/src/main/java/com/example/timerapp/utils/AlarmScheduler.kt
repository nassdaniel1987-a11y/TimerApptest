package com.example.timerapp.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.timerapp.AlarmReceiver
import com.example.timerapp.SettingsManager
import com.example.timerapp.models.Timer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AlarmScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val settingsManager = SettingsManager.getInstance(context)
    
    fun scheduleAlarm(timer: Timer) {
        try {
            val targetTime = ZonedDateTime.parse(
                timer.target_time,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
            )
            
            // Hauptalarm setzen
            scheduleExactAlarm(
                timerId = timer.id,
                timerName = timer.name,
                triggerTime = targetTime.toInstant().toEpochMilli(),
                isPreReminder = false
            )
            
            // Vorab-Erinnerung falls aktiviert
            if (settingsManager.isPreReminderEnabled) {
                val preReminderTime = targetTime.minusMinutes(
                    settingsManager.preReminderMinutes.toLong()
                )
                
                if (preReminderTime.isAfter(ZonedDateTime.now())) {
                    scheduleExactAlarm(
                        timerId = "${timer.id}_pre",
                        timerName = timer.name,
                        triggerTime = preReminderTime.toInstant().toEpochMilli(),
                        isPreReminder = true
                    )
                }
            }
            
            Log.d("AlarmScheduler", "✅ Alarm gesetzt für: ${timer.name} um ${targetTime}")
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "❌ Fehler beim Alarm setzen: ${e.message}")
        }
    }
    
    private fun scheduleExactAlarm(
        timerId: String,
        timerName: String,
        triggerTime: Long,
        isPreReminder: Boolean
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TIMER_ID", timerId)
            putExtra("TIMER_NAME", timerName)
            putExtra("IS_PRE_REMINDER", isPreReminder)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timerId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                Log.w("AlarmScheduler", "⚠️ Keine Berechtigung für exakte Alarme")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
    
    fun cancelAlarm(timerId: String) {
        // Hauptalarm abbrechen
        cancelSpecificAlarm(timerId)
        
        // Vorab-Erinnerung abbrechen
        cancelSpecificAlarm("${timerId}_pre")
        
        Log.d("AlarmScheduler", "🔕 Alarm abgebrochen: $timerId")
    }
    
    private fun cancelSpecificAlarm(timerId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timerId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
