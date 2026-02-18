package com.example.timerapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "timers")
data class Timer(
    @PrimaryKey
    val id: String = "",
    val name: String,
    val target_time: String, // ISO 8601 mit Offset (z.B. "2025-10-12T15:30:00+02:00")
    val category: String,
    val note: String? = null,
    val is_completed: Boolean = false,
    val created_at: String = "",
    val recurrence: String? = null, // "daily", "weekly", "weekdays", "weekends", "custom", null = einmalig
    val recurrence_end_date: String? = null, // Enddatum für Wiederholungen
    val recurrence_weekdays: String? = null, // Komma-separierte Wochentage (ISO 8601: 1=Mo, 7=So), z.B. "1,3,5" für Mo,Mi,Fr
    val klasse: String? = null // Klasse 1-4, null = keine Zuordnung (Kompatibilität mit bestehenden Timern)
)
