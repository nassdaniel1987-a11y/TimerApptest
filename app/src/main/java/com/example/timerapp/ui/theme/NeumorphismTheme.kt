package com.example.timerapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Neumorphism Design-System Tokens
 * Laut figma-design-prompt.md (Light #E0E5EC / Dark #2D2D2D)
 * Wählbar in Einstellungen → Design → Neumorphism
 */
object NeumorphismColors {

    // ── Light Mode ────────────────────────────────────────────────────────────
    val BackgroundLight     = Color(0xFFE0E5EC)
    val ShadowLightLight    = Color(0xFFFFFFFF)          // helle Seite
    val ShadowDarkLight     = Color(0xFFA3B1C6)          // dunkle Seite
    val TextPrimaryLight    = Color(0xFF333333)
    val TextSecondaryLight  = Color(0xFF666666)
    val AccentTeal          = Color(0xFF5BA4B5)
    val AccentSuccess       = Color(0xFF5BAA6A)
    val AccentWarning       = Color(0xFFD4A052)
    val AccentError         = Color(0xFFD45252)

    // ── Dark Mode ─────────────────────────────────────────────────────────────
    val BackgroundDark      = Color(0xFF2D2D2D)
    val ShadowLightDark     = Color(0xFF3D3D3D)          // helle Seite (dark)
    val ShadowDarkDark      = Color(0xFF1A1A1A)          // dunkle Seite (dark)
    val TextPrimaryDark     = Color(0xFFE0E0E0)
    val TextSecondaryDark   = Color(0xFFA0A0A0)
    val AccentTealDark      = Color(0xFF6DCAD8)
    val AccentSuccessDark   = Color(0xFF6DD87A)
    val AccentWarningDark   = Color(0xFFE8B865)
    val AccentErrorDark     = Color(0xFFE86565)

    // ── Kategorie-Farben (Neumorphism-angepasst) ──────────────────────────────
    val CategoryArbeit      = Color(0xFF5B8FBA)
    val CategoryPrivat      = Color(0xFF5BAA6A)
    val CategorySport       = Color(0xFFD45252)
    val CategoryFamilie     = Color(0xFFD4A052)
    val CategoryGesundheit  = Color(0xFF5BA4B5)
    val CategoryTermine     = Color(0xFF8B6AAE)
    val CategoryEinkaufen   = Color(0xFFC47D3E)
    val CategoryFreizeit    = Color(0xFF5BB5B0)
    val CategorySonstiges   = Color(0xFF8E8E8E)
}

/**
 * Shadow-Stärken für verschiedene Neumorphism-Zustände.
 * Android Compose unterstützt keine direkten Doppelschatten via Modifier.shadow(),
 * daher werden diese Werte für Canvas-basiertes Zeichnen genutzt.
 */
object NeumorphismShadow {
    // Raised: Buttons, Cards
    const val RAISED_OFFSET  = 6f
    const val RAISED_BLUR    = 12f
    const val RAISED_RADIUS  = 16f

    // Pressed / Inset: Active States, Input-Felder
    const val PRESSED_OFFSET = 4f
    const val PRESSED_BLUR   = 8f
    const val PRESSED_RADIUS = 12f

    // Flat: Hover
    const val FLAT_OFFSET    = 3f
    const val FLAT_BLUR      = 6f

    // Pill-Buttons (Quick-Timer)
    const val PILL_OFFSET    = 4f
    const val PILL_BLUR      = 8f
    const val PILL_RADIUS    = 50f
}
