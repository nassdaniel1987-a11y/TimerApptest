package com.example.timerapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "timer_templates")
data class TimerTemplate(
    @PrimaryKey
    val id: String = "",
    val name: String,
    val defaultTime: String, // Format: "HH:mm"
    val category: String,
    val note: String? = null,
    val created_at: String = ""
)
