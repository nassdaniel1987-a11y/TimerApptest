package com.example.timerapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val name: String,
    val color: String, // Hex format: #RRGGBB
    val created_at: String = ""
)
