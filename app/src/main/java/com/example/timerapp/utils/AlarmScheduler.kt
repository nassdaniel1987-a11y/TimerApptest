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
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val settingsManager = SettingsManager.getInstance(context)

    // ✅ NEU: Gruppiert Timer nach ihrer Zielzeit (auf die Minute genau)
    fun scheduleAlarmsForTimers(timers: List<Timer>) {
        // ✅ WICHTIG: Filtere Timer die bereits abgelaufen sind
        val now = System.currentTimeMillis()
        val validTimers = timers.filter { timer ->
            try {
                val targetTime = ZonedDateTime.parse(
                    timer.target_time,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                )
                val isInFuture = targetTime.toInstant().toEpochMilli() > now

                if (!isInFuture) {
                    Log.w("AlarmScheduler", "⏭️ Timer übersprungen (bereits abgelaufen): ${timer.name} (${timer.target_time})")
                }

                isInFuture
            } catch (e: Exception) {
                Log.e("AlarmScheduler", "❌ Fehler beim Parsen von Timer ${timer.id}: ${e.message}")
                false
            }
        }

        if (validTimers.isEmpty()) {
            Log.d("AlarmScheduler", "ℹ️ Keine zukünftigen Timer zum Planen")
            return
        }

        // Gruppiere Timer nach ihrer Zielzeit (HH:mm)
        val timersByTime = validTimers.groupBy { timer ->
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

        Log.d("AlarmScheduler", "✅ ${validTimers.size} zukünftige Timer geplant (${timers.size - validTimers.size} abgelaufen übersprungen)")
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

        // ✅ Berechne Zeit bis Alarm
        val timeUntilAlarm = triggerTime - System.currentTimeMillis()
        val minutesUntilAlarm = timeUntilAlarm / 1000 / 60
        val alarmType = if (isPreReminder) "Pre-Reminder" else "Haupt-Alarm"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "⏰ $alarmType geplant für $groupId in $minutesUntilAlarm Minuten (${timerNames.joinToString(", ")})")
            } else {
                Log.e("AlarmScheduler", "❌ Keine Berechtigung für exakte Alarme! Alarm wird NICHT gesetzt!")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d("AlarmScheduler", "⏰ $alarmType geplant für $groupId in $minutesUntilAlarm Minuten (${timerNames.joinToString(", ")})")
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
        // WICHTIG: Alle möglichen Gruppen-Alarme abbrechen
        // Gruppiere nach Zeit, um alle Gruppen-IDs zu finden
        val groupIds = mutableSetOf<String>()

        timers.forEach { timer ->
            try {
                val targetTime = ZonedDateTime.parse(
                    timer.target_time,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                )
                val groupId = "group_${targetTime.toLocalDate()}_${targetTime.hour}_${targetTime.minute}"
                groupIds.add(groupId)

                // Auch einzelne Timer-ID-basierte Alarme abbrechen (falls noch vorhanden)
                cancelAlarm(timer.id)
            } catch (e: Exception) {
                Log.e("AlarmScheduler", "Fehler beim Parsen von Timer ${timer.id}: ${e.message}")
            }
        }

        // Alle Gruppen-Alarme abbrechen
        groupIds.forEach { groupId ->
            cancelGroupAlarm(groupId)
        }

        // Neue gruppierte Alarme setzen
        scheduleAlarmsForTimers(timers)

        Log.d("AlarmScheduler", "🔄 Alle Alarme neu geplant für ${timers.size} Timer (${groupIds.size} Gruppen)")
    }

    // ✅ WIEDERHOLUNGS-LOGIK: Berechnet das nächste Vorkommen eines wiederholenden Timers
    fun calculateNextOccurrence(timer: Timer): Timer? {
        if (timer.recurrence == null) return null

        try {
            val currentTargetTime = ZonedDateTime.parse(
                timer.target_time,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
            )

            // Prüfe ob Enddatum erreicht ist
            if (timer.recurrence_end_date != null) {
                val endDate = ZonedDateTime.parse(
                    timer.recurrence_end_date,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                )
                if (currentTargetTime.isAfter(endDate) || currentTargetTime.isEqual(endDate)) {
                    Log.d("AlarmScheduler", "🔚 Wiederholung beendet für: ${timer.name}")
                    return null
                }
            }

            val nextTime = when (timer.recurrence) {
                "daily" -> calculateNextDaily(currentTargetTime)
                "weekly" -> calculateNextWeekly(currentTargetTime)
                "weekdays" -> calculateNextWeekday(currentTargetTime)
                "weekends" -> calculateNextWeekend(currentTargetTime)
                else -> null
            }

            if (nextTime == null) return null

            // Erstelle neuen Timer mit gleichem Namen, Kategorie, etc. aber neuem Datum
            val newTimer = timer.copy(
                id = "", // Neue ID wird vom Repository vergeben
                target_time = nextTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                is_completed = false,
                created_at = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            )

            Log.d("AlarmScheduler", "🔁 Nächste Wiederholung berechnet: ${timer.name} → ${nextTime}")
            return newTimer

        } catch (e: Exception) {
            Log.e("AlarmScheduler", "❌ Fehler bei Wiederholungsberechnung: ${e.message}")
            return null
        }
    }

    private fun calculateNextDaily(currentTime: ZonedDateTime): ZonedDateTime {
        return currentTime.plusDays(1)
    }

    private fun calculateNextWeekly(currentTime: ZonedDateTime): ZonedDateTime {
        return currentTime.plusWeeks(1)
    }

    private fun calculateNextWeekday(currentTime: ZonedDateTime): ZonedDateTime {
        var nextTime = currentTime.plusDays(1)

        // Überspringe Wochenende
        while (nextTime.dayOfWeek == DayOfWeek.SATURDAY || nextTime.dayOfWeek == DayOfWeek.SUNDAY) {
            nextTime = nextTime.plusDays(1)
        }

        return nextTime
    }

    private fun calculateNextWeekend(currentTime: ZonedDateTime): ZonedDateTime {
        var nextTime = currentTime.plusDays(1)

        // Finde nächsten Samstag oder Sonntag
        while (nextTime.dayOfWeek != DayOfWeek.SATURDAY && nextTime.dayOfWeek != DayOfWeek.SUNDAY) {
            nextTime = nextTime.plusDays(1)
        }

        return nextTime
    }
}