package com.example.timerapp.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "qr_codes")
data class QRCodeData(
    @PrimaryKey
    val id: String = "",
    val name: String,
    val time: String, // Format: "HH:mm"
    val category: String,
    val note: String? = null,

    @SerialName("isflexible")
    @ColumnInfo(name = "isFlexible")
    val isFlexible: Boolean = false,

    val created_at: String = ""
)

// Extension Functions für QR-Code Serialisierung
fun QRCodeData.toQRString(): String {
    return "TIMER:$name|$time|$category|${note ?: ""}|$isFlexible"
}

fun parseQRString(qrString: String): QRCodeData? {
    return try {
        if (!qrString.startsWith("TIMER:")) return null

        val parts = qrString.removePrefix("TIMER:").split("|")
        if (parts.size < 5) return null

        QRCodeData(
            name = parts[0],
            time = parts[1],
            category = parts[2],
            note = parts[3].ifBlank { null },
            isFlexible = parts[4].toBoolean()
        )
    } catch (e: Exception) {
        null
    }
}
