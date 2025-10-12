package com.example.timerapp.models

import kotlinx.serialization.Serializable

@Serializable
data class TimerTemplate(
    val id: String = "",
    val name: String,
    val defaultTime: String, // Format: "HH:mm"
    val category: String,
    val note: String? = null,
    val created_at: String = ""
)
