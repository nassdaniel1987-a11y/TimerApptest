package com.example.timerapp.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.example.timerapp.AppDesignTheme

/**
 * CompositionLocal, das das aktive Design-Theme (Klassisch / Neumorphism) App-weit bereitstellt.
 * Wird in MainActivity gesetzt und in allen Composables via LocalAppDesignTheme.current gelesen.
 */
val LocalAppDesignTheme = compositionLocalOf<AppDesignTheme> { AppDesignTheme.CLASSIC }
