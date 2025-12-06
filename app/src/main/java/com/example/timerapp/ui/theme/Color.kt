package com.example.timerapp.ui.theme

import androidx.compose.ui.graphics.Color

// ✨ Modern Vibrant Colors
// Light Theme Colors - Lebendiger und moderner
val md_theme_light_primary = Color(0xFF6366F1) // Indigo - mutiger!
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFE0E7FF)
val md_theme_light_onPrimaryContainer = Color(0xFF1E1B4B)
val md_theme_light_secondary = Color(0xFF8B5CF6) // Purple
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFF3E8FF)
val md_theme_light_onSecondaryContainer = Color(0xFF2E1065)
val md_theme_light_tertiary = Color(0xFFEC4899) // Pink
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFCE7F3)
val md_theme_light_onTertiaryContainer = Color(0xFF831843)
val md_theme_light_error = Color(0xFFEF4444)
val md_theme_light_errorContainer = Color(0xFFFEE2E2)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF7F1D1D)
val md_theme_light_background = Color(0xFFF8FAFC) // Helleres, saubereres Grau
val md_theme_light_onBackground = Color(0xFF0F172A)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF0F172A)
val md_theme_light_surfaceVariant = Color(0xFFF1F5F9)
val md_theme_light_onSurfaceVariant = Color(0xFF475569)
val md_theme_light_outline = Color(0xFFCBD5E1)
val md_theme_light_inverseOnSurface = Color(0xFFF1F5F9)
val md_theme_light_inverseSurface = Color(0xFF1E293B)
val md_theme_light_inversePrimary = Color(0xFFA5B4FC)

// Dark Theme Colors - Tiefere, sattere Farben
val md_theme_dark_primary = Color(0xFF818CF8) // Helleres Indigo
val md_theme_dark_onPrimary = Color(0xFF1E1B4B)
val md_theme_dark_primaryContainer = Color(0xFF4338CA)
val md_theme_dark_onPrimaryContainer = Color(0xFFE0E7FF)
val md_theme_dark_secondary = Color(0xFFA78BFA) // Helleres Purple
val md_theme_dark_onSecondary = Color(0xFF2E1065)
val md_theme_dark_secondaryContainer = Color(0xFF6D28D9)
val md_theme_dark_onSecondaryContainer = Color(0xFFF3E8FF)
val md_theme_dark_tertiary = Color(0xFFF472B6) // Helleres Pink
val md_theme_dark_onTertiary = Color(0xFF831843)
val md_theme_dark_tertiaryContainer = Color(0xFFBE185D)
val md_theme_dark_onTertiaryContainer = Color(0xFFFCE7F3)
val md_theme_dark_error = Color(0xFFF87171)
val md_theme_dark_errorContainer = Color(0xFF991B1B)
val md_theme_dark_onError = Color(0xFF7F1D1D)
val md_theme_dark_onErrorContainer = Color(0xFFFEE2E2)
val md_theme_dark_background = Color(0xFF0F172A) // Tiefer, satter
val md_theme_dark_onBackground = Color(0xFFF1F5F9)
val md_theme_dark_surface = Color(0xFF1E293B)
val md_theme_dark_onSurface = Color(0xFFF1F5F9)
val md_theme_dark_surfaceVariant = Color(0xFF334155)
val md_theme_dark_onSurfaceVariant = Color(0xFFCBD5E1)
val md_theme_dark_outline = Color(0xFF475569)
val md_theme_dark_inverseOnSurface = Color(0xFF0F172A)
val md_theme_dark_inverseSurface = Color(0xFFF1F5F9)
val md_theme_dark_inversePrimary = Color(0xFF6366F1)

// 🌈 Gradient Colors für moderne Effekte
object GradientColors {
    // Timer States Gradients
    val PendingGradient = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF06B6D4)  // Cyan
    )

    val RunningGradient = listOf(
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444)  // Red
    )

    val CompletedGradient = listOf(
        Color(0xFF10B981), // Emerald
        Color(0xFF059669)  // Green
    )

    val AlarmGradient = listOf(
        Color(0xFFDC2626), // Red
        Color(0xFFB91C1C)  // Dark Red
    )

    // Background Gradients
    val BackgroundLight = listOf(
        Color(0xFFFAFAFA),
        Color(0xFFF0F9FF)  // Leichter Blau-Tint
    )

    val BackgroundDark = listOf(
        Color(0xFF0F172A),
        Color(0xFF1E1B4B)  // Indigo-Tint
    )

    // Card Gradients
    val CardGlow = listOf(
        Color(0xFF8B5CF6).copy(alpha = 0.3f),
        Color(0xFFEC4899).copy(alpha = 0.3f),
        Color(0xFFF59E0B).copy(alpha = 0.3f)
    )

    // Hero Section
    val HeroGradient = listOf(
        Color(0xFF6366F1),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899)
    )
}