package com.example.timerapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entity_type: String,    // "timer", "category", "template", "qr_code"
    val operation: String,      // "CREATE", "UPDATE", "DELETE"
    val entity_id: String,      // ID der betroffenen Entity
    val payload: String,        // Volle JSON-Serialisierung (für CREATE/UPDATE)
    val created_at: Long = System.currentTimeMillis()
)
