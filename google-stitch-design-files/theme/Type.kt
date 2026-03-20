package com.example.timerapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.unit.sp
import com.example.timerapp.R

// Google Fonts Provider
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Inter Font Family
val interFontName = GoogleFont("Inter")
val InterFontFamily = FontFamily(
    Font(googleFont = interFontName, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = interFontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = interFontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = interFontName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = interFontName, fontProvider = provider, weight = FontWeight.Bold)
)

// Manrope Font Family (for accents/headings)
val manropeFontName = GoogleFont("Manrope")
val ManropeFontFamily = FontFamily(
    Font(googleFont = manropeFontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = manropeFontName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = manropeFontName, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = manropeFontName, fontProvider = provider, weight = FontWeight.ExtraBold)
)

val Typography = Typography(
    // Display styles with Manrope for impact
    displayLarge = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),

    // Headlines with Manrope
    headlineLarge = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),

    // Titles with Inter
    titleLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),

    // Body text with Inter for readability
    bodyLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),

    // Labels with Inter
    labelLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)