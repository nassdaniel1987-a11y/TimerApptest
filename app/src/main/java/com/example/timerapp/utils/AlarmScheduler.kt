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

    // ✅ NEU: Gruppiert Timer nach ihrer Zielzeit (auf die Minute genau)
    fun scheduleAlarmsForTimers(timers: List<Timer>) {
        // Gruppiere Timer nach ihrer Zielzeit (HH:mm)
        val timersByTime = timers.groupBy { timer ->
            try {
                val targetTime = ZonedDateTime.parse(
                    timer.target_time,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                )
                // Gruppierungsschlüssel: Datum + Stunde + Minute
                "${targetTime.toLocalDate()}_${targetTime.hour}_${targetTime.minute}"
            } catch (e: Exception) {
                null
            }
        }.filterKeys { it != null }

        // Setze Alarme für jede Zeitgruppe
        timersByTime.forEach { (timeKey, groupedTimers) ->
            if (groupedTimers.isNotEmpty()) {
                scheduleGroupedAlarm(groupedTimers)
            }
        }
    }

    // ✅ NEU: Setzt einen Alarm für eine Gruppe von Timern
    private fun scheduleGroupedAlarm(timers: List<Timer>) {
        if (timers.isEmpty()) return

        try {
            val firstTimer = timers.first()
            val targetTime = ZonedDateTime.parse(
                firstTimer.target_time,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
            )

            // Erstelle eine eindeutige ID basierend auf der Zeit
            val groupId = "group_${targetTime.toLocalDate()}_${targetTime.hour}_${targetTime.minute}"

            // Sammle alle Timer-Daten
            val timerIds = timers.map { it.id }
            val timerNames = timers.map { it.name }
            val timerCategories = timers.map { it.category }

            // Hauptalarm setzen
            scheduleExactAlarm(
                groupId = groupId,
                timerIds = timerIds,
                timerNames = timerNames,
                timerCategories = timerCategories,
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
                        groupId = "${groupId}_pre",
                        timerIds = timerIds,
                        timerNames = timerNames,
                        timerCategories = timerCategories,
                        triggerTime = preReminderTime.toInstant().toEpochMilli(),
                        isPreReminder = true
                    )
                }
            }

            Log.d("AlarmScheduler", "✅ Gruppen-Alarm gesetzt für ${timers.size} Timer um ${targetTime}")
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "❌ Fehler beim Alarm setzen: ${e.message}")
        }
    }

    private fun scheduleExactAlarm(
        groupId: String,
        timerIds: List<String>,
        timerNames: List<String>,
        timerCategories: List<String>,
        triggerTime: Long,
        isPreReminder: Boolean
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("GROUP_ID", groupId)
            putExtra("TIMER_IDS", timerIds.toTypedArray())
            putExtra("TIMER_NAMES", timerNames.toTypedArray())
            putExtra("TIMER_CATEGORIES", timerCategories.toTypedArray())
            putExtra("IS_PRE_REMINDER", isPreReminder)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            groupId.hashCode(),
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

    // ✅ DEPRECATED: Alte Methode für Rückwärtskompatibilität
    @Deprecated("Verwende scheduleAlarmsForTimers() stattdessen")
    fun scheduleAlarm(timer: Timer) {
        scheduleAlarmsForTimers(listOf(timer))
    }

    fun cancelAlarm(timerId: String) {
        // Versuche sowohl einzelne als auch gruppierte Alarme abzubrechen
        cancelSpecificAlarm(timerId)
        cancelSpecificAlarm("${timerId}_pre")

        Log.d("AlarmScheduler", "🔕 Alarm abgebrochen: $timerId")
    }

    // ✅ NEU: Bricht Gruppen-Alarm ab
    fun cancelGroupAlarm(groupId: String) {
        cancelSpecificAlarm(groupId)
        cancelSpecificAlarm("${groupId}_pre")

        Log.d("AlarmScheduler", "🔕 Gruppen-Alarm abgebrochen: $groupId")
    }

    private fun cancelSpecificAlarm(alarmId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // ✅ NEU: Bricht alle Alarme ab und setzt sie neu (für vollständige Synchronisation)
    fun rescheduleAllAlarms(timers: List<Timer>) {
        // Alte Alarme abbrechen
        timers.forEach { timer ->
            cancelAlarm(timer.id)
        }

        // Neue gruppierte Alarme setzen
        scheduleAlarmsForTimers(timers)

        Log.d("AlarmScheduler", "🔄 Alle Alarme neu geplant für ${timers.size} Timer")
    }
}