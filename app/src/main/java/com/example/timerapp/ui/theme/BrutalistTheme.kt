package com.example.timerapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brutalist / Industrial-Terminal Design-System Tokens
 *
 * Aesthetic: Monochrome darkness + single neon accent.
 * No rounded corners, no gradients, no shadows — only sharp edges and light.
 *
 * Wählbar in Einstellungen → App-Design → Brutalist
 */
object BrutalistColors {

    // ── Background ────────────────────────────────────────────────────────────
    val Background          = Color(0xFF0D0D0D)  // near-black
    val Surface             = Color(0xFF1A1A1A)  // card surface
    val SurfaceElevated     = Color(0xFF242424)  // subtle lift

    // ── Borders ───────────────────────────────────────────────────────────────
    val Border              = Color(0xFF2E2E2E)  // default border
    val BorderBright        = Color(0xFF444444)  // secondary/hover border

    // ── Accent — the ONLY colour besides black/white ──────────────────────────
    val Cyan                = Color(0xFF00FFD1)  // neon teal-cyan
    val CyanDim             = Color(0xFF00B894)  // less intense cyan
    val CyanGlow            = Color(0xFF00FFD1).copy(alpha = 0.15f) // subtle glow

    // ── Text ──────────────────────────────────────────────────────────────────
    val TextPrimary         = Color(0xFFEEEEEE)  // bright near-white
    val TextSecondary       = Color(0xFF888888)  // muted grey
    val TextAccent          = Cyan               // cyan labels
    val TextDisabled        = Color(0xFF444444)

    // ── Status Colours (desaturated, neon-ish) ────────────────────────────────
    val StatusPending       = Color(0xFF4488FF)  // electric blue
    val StatusRunning       = Color(0xFFFFAA00)  // amber
    val StatusCompleted     = Color(0xFF00FFD1)  // cyan = done
    val StatusAlarm         = Color(0xFFFF3355)  // hard red

    // ── Scan-line overlay tint ────────────────────────────────────────────────
    val ScanLine            = Color(0xFF000000).copy(alpha = 0.08f)
}

object BrutalistMetrics {
    const val CORNER_RADIUS = 0f      // no rounding — sharp everywhere
    const val BORDER_WIDTH  = 1.5f   // dp — thin but precise
    const val CARD_PADDING  = 16f    // dp
}
