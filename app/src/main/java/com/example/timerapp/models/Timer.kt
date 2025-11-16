package com.example.timerapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Timer(
    val id: String = "",
    val name: String,
    val target_time: String, // ISO 8601 mit Offset (z.B. "2025-10-12T15:30:00+02:00")
    val category: String,
    val note: String? = null,
    val is_completed: Boolean = false,
    val created_at: String = "",
    val recurrence: String? = null, // "daily", "weekly", "weekdays", "weekends", null = einmalig
    val recurrence_end_date: String? = null // Enddatum für Wiederholungen
)
