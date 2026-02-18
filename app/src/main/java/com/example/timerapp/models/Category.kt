package com.example.timerapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String = "",
    val name: String,
    val color: String, // Hex format: #RRGGBB
    val created_at: String = ""
)
