package com.example.timerapp.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Zentrale Utility-Klasse für Date/Time-Parsing und -Formatierung.
 * Vermeidet duplizierten Code und bietet konsistente Fehlerbehandlung.
 */
object DateTimeUtils {

    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

    /**
     * Parst einen ISO-8601 DateTime-String zu ZonedDateTime.
     * @return ZonedDateTime oder null bei Parsing-Fehler
     */
    fun parseIsoDateTime(isoString: String?): ZonedDateTime? {
        if (isoString.isNullOrBlank()) return null
        return try {
            ZonedDateTime.parse(isoString, isoFormatter)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parst einen ISO-8601 DateTime-String mit Fallback-Wert.
     * @return ZonedDateTime oder der Fallback-Wert bei Parsing-Fehler
     */
    fun parseIsoDateTimeOrDefault(
        isoString: String?,
        default: ZonedDateTime = ZonedDateTime.now()
    ): ZonedDateTime {
        return parseIsoDateTime(isoString) ?: default
    }

    /**
     * Formatiert ZonedDateTime zu ISO-8601 String.
     */
    fun formatToIso(dateTime: ZonedDateTime): String {
        return dateTime.format(isoFormatter)
    }

    /**
     * Formatiert ZonedDateTime zu lesbarem Datum-String (dd.MM.yyyy).
     */
    fun formatDate(dateTime: ZonedDateTime): String {
        return dateTime.format(dateFormatter)
    }

    /**
     * Formatiert ZonedDateTime zu Uhrzeit-String (HH:mm).
     */
    fun formatTime(dateTime: ZonedDateTime): String {
        return dateTime.format(timeFormatter)
    }

    /**
     * Formatiert ZonedDateTime zu vollständigem DateTime-String (dd.MM.yyyy HH:mm).
     */
    fun formatDateTime(dateTime: ZonedDateTime): String {
        return dateTime.format(dateTimeFormatter)
    }

    /**
     * Formatiert ZonedDateTime zu kurzem Datum-String (dd.MM.yy).
     */
    fun formatShortDate(dateTime: ZonedDateTime): String {
        return dateTime.format(shortDateFormatter)
    }

    /**
     * Berechnet die verbleibende Zeit bis zum Ziel als lesbaren String.
     * z.B. "Noch 5 Min", "Noch 2h 30min", "Morgen 14:00 Uhr"
     */
    fun getTimeUntilText(targetTime: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): String {
        val isPast = targetTime.isBefore(now)

        if (isPast) return "Abgelaufen"

        val minutesUntil = ChronoUnit.MINUTES.between(now, targetTime)
        val hoursUntil = ChronoUnit.HOURS.between(now, targetTime)
        val daysUntil = ChronoUnit.DAYS.between(now.toLocalDate(), targetTime.toLocalDate())

        return when {
            minutesUntil < 60 -> "Noch $minutesUntil Min"
            hoursUntil < 24 -> "Noch ${hoursUntil}h ${minutesUntil % 60}min"
            daysUntil == 0L -> "Heute ${formatTime(targetTime)} Uhr"
            daysUntil == 1L -> "Morgen ${formatTime(targetTime)} Uhr"
            else -> "${formatDateTime(targetTime)} Uhr"
        }
    }

    /**
     * Berechnet Minuten bis zum Ziel.
     * @return Negative Werte bedeuten, dass das Ziel in der Vergangenheit liegt.
     */
    fun getMinutesUntil(targetTime: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): Long {
        return ChronoUnit.MINUTES.between(now, targetTime)
    }

    /**
     * Prüft, ob das Ziel heute ist.
     */
    fun isToday(targetTime: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        return targetTime.toLocalDate() == now.toLocalDate()
    }

    /**
     * Prüft, ob das Ziel morgen ist.
     */
    fun isTomorrow(targetTime: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        return targetTime.toLocalDate() == now.toLocalDate().plusDays(1)
    }

    /**
     * Prüft, ob das Ziel innerhalb der nächsten X Tage liegt.
     */
    fun isWithinDays(targetTime: ZonedDateTime, days: Long, now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        val endDate = now.toLocalDate().plusDays(days)
        return targetTime.toLocalDate() >= now.toLocalDate() && targetTime.toLocalDate() <= endDate
    }

    /**
     * Erstellt einen ZonedDateTime aus LocalDate und LocalTime mit der System-Zeitzone.
     */
    fun createDateTime(date: LocalDate, time: LocalTime, zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
        return ZonedDateTime.of(date, time, zone)
    }

    /**
     * Berechnet den Fortschritt zwischen Erstellung und Ziel (0.0 - 1.0).
     */
    fun calculateProgress(
        createdAt: ZonedDateTime,
        targetTime: ZonedDateTime,
        now: ZonedDateTime = ZonedDateTime.now()
    ): Float {
        val totalMinutes = ChronoUnit.MINUTES.between(createdAt, targetTime).toFloat()
        val elapsedMinutes = ChronoUnit.MINUTES.between(createdAt, now).toFloat()

        return if (totalMinutes > 0) {
            (elapsedMinutes / totalMinutes).coerceIn(0f, 1f)
        } else {
            1f
        }
    }
}
