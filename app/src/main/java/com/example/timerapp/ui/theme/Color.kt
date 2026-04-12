package com.example.timerapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Light Theme ───────────────────────────────────────────────────────────────
val md_theme_light_primary = Color(0xFF6366F1)          // Indigo
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFE0E7FF)
val md_theme_light_onPrimaryContainer = Color(0xFF1E1B4B)
val md_theme_light_secondary = Color(0xFF8B5CF6)        // Purple
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFF3E8FF)
val md_theme_light_onSecondaryContainer = Color(0xFF2E1065)
val md_theme_light_tertiary = Color(0xFFEC4899)         // Pink
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFCE7F3)
val md_theme_light_onTertiaryContainer = Color(0xFF831843)
val md_theme_light_error = Color(0xFFEF4444)
val md_theme_light_errorContainer = Color(0xFFFEE2E2)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF7F1D1D)
val md_theme_light_background = Color(0xFFF8FAFC)
val md_theme_light_onBackground = Color(0xFF0F172A)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF0F172A)
val md_theme_light_surfaceVariant = Color(0xFFF1F5F9)
val md_theme_light_onSurfaceVariant = Color(0xFF475569)
val md_theme_light_outline = Color(0xFFCBD5E1)
val md_theme_light_inverseOnSurface = Color(0xFFF1F5F9)
val md_theme_light_inverseSurface = Color(0xFF1E293B)
val md_theme_light_inversePrimary = Color(0xFFA5B4FC)

// ── Dark Theme (exakt aus Stitch-HTML extrahiert) ────────────────────────────
// Basis: #060E20 (Settings) / #0B1326 (CreateTimer) → wir nutzen Settings-Basis
val md_theme_dark_primary = Color(0xFFA3A6FF)           // periwinkle
val md_theme_dark_onPrimary = Color(0xFF0F00A4)
val md_theme_dark_primaryContainer = Color(0xFF9396FF)
val md_theme_dark_onPrimaryContainer = Color(0xFF0A0081)
val md_theme_dark_secondary = Color(0xFFA28EFC)         // lavender
val md_theme_dark_onSecondary = Color(0xFF21006D)
val md_theme_dark_secondaryContainer = Color(0xFF49339D)
val md_theme_dark_onSecondaryContainer = Color(0xFFD4C9FF)
val md_theme_dark_tertiary = Color(0xFFEF81C4)          // pink
val md_theme_dark_onTertiary = Color(0xFF701455)
val md_theme_dark_tertiaryContainer = Color(0xFFFF8ED2)
val md_theme_dark_onTertiaryContainer = Color(0xFF63054A)
val md_theme_dark_error = Color(0xFFFF6E84)
val md_theme_dark_errorContainer = Color(0xFFA70138)
val md_theme_dark_onError = Color(0xFF490013)
val md_theme_dark_onErrorContainer = Color(0xFFFFB2B9)
val md_theme_dark_background = Color(0xFF060E20)        // deep navy
val md_theme_dark_onBackground = Color(0xFFDEE5FF)
val md_theme_dark_surface = Color(0xFF0F1930)           // surface-container
val md_theme_dark_onSurface = Color(0xFFDEE5FF)
val md_theme_dark_surfaceVariant = Color(0xFF192540)    // surface-variant
val md_theme_dark_onSurfaceVariant = Color(0xFFA3AAC4)
val md_theme_dark_outline = Color(0xFF6D758C)
val md_theme_dark_inverseOnSurface = Color(0xFF4D556B)
val md_theme_dark_inverseSurface = Color(0xFFFAF8FF)
val md_theme_dark_inversePrimary = Color(0xFF494BD7)

// ── Extended Design Tokens ────────────────────────────────────────────────────
object DesignTokens {
    // Hardcoded accent colours (from Stitch)
    val IndigoAccent     = Color(0xFF6366F1)
    val VioletAccent     = Color(0xFF7C3AED)
    val PrimaryDim       = Color(0xFF6063EE)   // toggle ON, sync button

    // Surface layers (dark)
    val SurfaceContainerLowest = Color(0xFF000000)
    val SurfaceContainerLow    = Color(0xFF091328)
    val SurfaceContainer       = Color(0xFF0F1930)
    val SurfaceContainerHigh   = Color(0xFF141F38)
    val SurfaceContainerHighest= Color(0xFF192540)
    val SurfaceBright          = Color(0xFF1F2B49)

    // Glass
    val GlassDark        = Color(0xFF0F1930).copy(alpha = 0.6f)
    val GlassBorder      = Color.White.copy(alpha = 0.05f)
    val GlassDarkCreate  = Color(0xFF2D3449).copy(alpha = 0.4f)  // create-timer variant

    // Status
    val StatusOnline     = Color(0xFF10B981)   // emerald
    val StatusSyncing    = Color(0xFF818CF8)   // indigo-400

    // Nav
    val NavBarBg         = Color(0xFF091328).copy(alpha = 0.8f)
    val NavActiveColor   = Color(0xFF818CF8)   // indigo-400

    // Timer state border colours (kept from existing design)
    val BorderPending    = Color(0xFF3B82F6)   // blue-500
    val BorderRunning    = Color(0xFFF59E0B)   // amber-500
    val BorderCompleted  = Color(0xFF10B981)   // emerald-500
    val BorderAlarm      = Color(0xFFEF4444)   // red-500
}

// ── Gradients ─────────────────────────────────────────────────────────────────
object GradientColors {
    val PendingGradient   = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))
    val RunningGradient   = listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
    val CompletedGradient = listOf(Color(0xFF10B981), Color(0xFF059669))
    val AlarmGradient     = listOf(Color(0xFFDC2626), Color(0xFFB91C1C))

    val BackgroundLight   = listOf(Color(0xFFFAFAFA), Color(0xFFF0F9FF))
    val BackgroundDark    = listOf(Color(0xFF060E20), Color(0xFF0F1930))

    // Primary action gradient (buttons, FAB)
    val PrimaryButton     = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))  // indigo→violet
    val SyncButton        = listOf(Color(0xFFA3A6FF), Color(0xFF6063EE))  // primary→primary-dim

    val HeroGradient      = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899))

    val CardGlow          = listOf(
        Color(0xFF8B5CF6).copy(alpha = 0.3f),
        Color(0xFFEC4899).copy(alpha = 0.3f),
        Color(0xFFF59E0B).copy(alpha = 0.3f)
    )
}

// ── Glassmorphism ─────────────────────────────────────────────────────────────
object GlassColors {
    val GlassSurfaceLight = Color(0xFFFFFFFF).copy(alpha = 0.6f)
    val GlassSurfaceDark  = Color(0xFF0F1930).copy(alpha = 0.6f)   // updated to Stitch value
    val GlassBorderLight  = Color(0xFFFFFFFF).copy(alpha = 0.8f)
    val GlassBorderDark   = Color(0xFFFFFFFF).copy(alpha = 0.05f)  // updated to Stitch value

    val MeshBlue    = Color(0xFF3B82F6)
    val MeshPurple  = Color(0xFF8B5CF6)
    val MeshPink    = Color(0xFFEC4899)
    val MeshAmber   = Color(0xFFF59E0B)
    val MeshCyan    = Color(0xFF06B6D4)
    val MeshGreen   = Color(0xFF10B981)
}
