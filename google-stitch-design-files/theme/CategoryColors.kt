package com.example.timerapp.utils

import androidx.compose.ui.graphics.Color

// ✅ Kategorie-Farben für visuelle Unterscheidung
object CategoryColors {
    private val colorMap = mapOf(
        "Arbeit" to Color(0xFF1976D2),           // Blau
        "Privat" to Color(0xFF388E3C),           // Grün
        "Sport" to Color(0xFFD32F2F),            // Rot
        "Familie" to Color(0xFFE64A19),          // Orange
        "Gesundheit" to Color(0xFF00796B),       // Türkis
        "Termine" to Color(0xFF7B1FA2),          // Lila
        "Einkaufen" to Color(0xFFF57C00),        // Dunkelorange
        "Freizeit" to Color(0xFF0097A7),         // Cyan
        "Sonstiges" to Color(0xFF616161),        // Grau
        "Schnell-Timer" to Color(0xFF512DA8),    // Dunkellila
        "Wird abgeholt" to Color(0xFF1976D2)     // Blau (default)
    )

    private val defaultColor = Color(0xFF757575) // Grau

    fun getColor(category: String): Color {
        return colorMap[category] ?: defaultColor
    }

    fun getColors(): Map<String, Color> = colorMap
}
