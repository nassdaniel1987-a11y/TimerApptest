package com.example.timerapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timerapp.ui.theme.ManropeFontFamily
import com.example.timerapp.ui.theme.NeumorphismColors
import com.example.timerapp.ui.theme.NeumorphismShadow

// ─────────────────────────────────────────────────────────────────────────────
// Modifier Extensions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Neumorphic Raised-Shadow (Buttons, Cards, FAB).
 * Zeichnet zwei Schatten: einen hellen oben-links, einen dunklen unten-rechts.
 */
fun Modifier.neumorphicRaised(
    bgColor: Color,
    lightShadow: Color,
    darkShadow: Color,
    cornerRadius: Float = NeumorphismShadow.RAISED_RADIUS,
    offset: Float = NeumorphismShadow.RAISED_OFFSET,
    blur: Float = NeumorphismShadow.RAISED_BLUR,
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blur, -offset, -offset, lightShadow.copy(alpha = 0.7f).toArgb())
            }
        }
        canvas.drawRoundRect(
            left = 0f, top = 0f,
            right = size.width, bottom = size.height,
            radiusX = cornerRadius, radiusY = cornerRadius,
            paint = paint
        )
        val paintDark = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blur, offset, offset, darkShadow.copy(alpha = 0.7f).toArgb())
            }
        }
        canvas.drawRoundRect(
            left = 0f, top = 0f,
            right = size.width, bottom = size.height,
            radiusX = cornerRadius, radiusY = cornerRadius,
            paint = paintDark
        )
    }
}

/**
 * Neumorphic Inset-Shadow (aktive States, gedrückte Buttons).
 * Simuliert einen eingelassenen Effekt durch invertierte Schatten.
 */
fun Modifier.neumorphicPressed(
    bgColor: Color,
    lightShadow: Color,
    darkShadow: Color,
    cornerRadius: Float = NeumorphismShadow.PRESSED_RADIUS,
    offset: Float = NeumorphismShadow.PRESSED_OFFSET,
    blur: Float = NeumorphismShadow.PRESSED_BLUR,
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        // Dunkler Schatten oben-links (inset simulation)
        val paintDark = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blur, -offset, -offset, darkShadow.copy(alpha = 0.7f).toArgb())
            }
        }
        canvas.drawRoundRect(
            left = offset, top = offset,
            right = size.width - offset, bottom = size.height - offset,
            radiusX = cornerRadius, radiusY = cornerRadius,
            paint = paintDark
        )
        // Heller Schatten unten-rechts (inset simulation)
        val paintLight = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blur, offset, offset, lightShadow.copy(alpha = 0.7f).toArgb())
            }
        }
        canvas.drawRoundRect(
            left = offset, top = offset,
            right = size.width - offset, bottom = size.height - offset,
            radiusX = cornerRadius, radiusY = cornerRadius,
            paint = paintLight
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Gibt die Neumorphism-Farben für Light oder Dark Mode zurück.
 */
data class NeumorphColors(
    val bg: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentSuccess: Color,
    val accentWarning: Color,
    val accentError: Color,
)

fun neumorphColorsLight() = NeumorphColors(
    bg           = NeumorphismColors.BackgroundLight,
    lightShadow  = NeumorphismColors.ShadowLightLight,
    darkShadow   = NeumorphismColors.ShadowDarkLight,
    textPrimary  = NeumorphismColors.TextPrimaryLight,
    textSecondary= NeumorphismColors.TextSecondaryLight,
    accent       = NeumorphismColors.AccentTeal,
    accentSuccess= NeumorphismColors.AccentSuccess,
    accentWarning= NeumorphismColors.AccentWarning,
    accentError  = NeumorphismColors.AccentError,
)

fun neumorphColorsDark() = NeumorphColors(
    bg           = NeumorphismColors.BackgroundDark,
    lightShadow  = NeumorphismColors.ShadowLightDark,
    darkShadow   = NeumorphismColors.ShadowDarkDark,
    textPrimary  = NeumorphismColors.TextPrimaryDark,
    textSecondary= NeumorphismColors.TextSecondaryDark,
    accent       = NeumorphismColors.AccentTealDark,
    accentSuccess= NeumorphismColors.AccentSuccessDark,
    accentWarning= NeumorphismColors.AccentWarningDark,
    accentError  = NeumorphismColors.AccentErrorDark,
)

// ─────────────────────────────────────────────────────────────────────────────
// NeumorphicCard
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Raised Neumorphic Card — ersetzt GlassmorphCard im Neumorphism-Modus.
 */
@Composable
fun NeumorphicCard(
    colors: NeumorphColors,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .neumorphicRaised(
                bgColor      = colors.bg,
                lightShadow  = colors.lightShadow,
                darkShadow   = colors.darkShadow,
                cornerRadius = cornerRadius.value,
            )
            .background(color = colors.bg, shape = RoundedCornerShape(cornerRadius))
            .padding(16.dp),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// NeumorphicFAB
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Neumorphic Floating Action Button — runde Form, Teal-Akzent.
 */
@Composable
fun NeumorphicFAB(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    colors: NeumorphColors,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fabScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(
                if (isPressed)
                    Modifier.neumorphicPressed(
                        bgColor     = colors.bg,
                        lightShadow = colors.lightShadow,
                        darkShadow  = colors.darkShadow,
                        cornerRadius= size.value / 2,
                    )
                else
                    Modifier.neumorphicRaised(
                        bgColor     = colors.bg,
                        lightShadow = colors.lightShadow,
                        darkShadow  = colors.darkShadow,
                        cornerRadius= size.value / 2,
                    )
            )
            .background(color = colors.accent, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector          = icon,
            contentDescription   = contentDescription,
            tint                 = Color.White,
            modifier             = Modifier.size(24.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NeumorphicQuickTimerButton
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pill-förmiger Quick-Timer-Button im Neumorphism-Stil.
 */
@Composable
fun NeumorphicQuickTimerButton(
    label: String,
    onClick: () -> Unit,
    colors: NeumorphColors,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .scale(scale)
            .then(
                if (isPressed)
                    Modifier.neumorphicPressed(
                        bgColor     = colors.bg,
                        lightShadow = colors.lightShadow,
                        darkShadow  = colors.darkShadow,
                        cornerRadius= NeumorphismShadow.PILL_RADIUS,
                        offset      = NeumorphismShadow.PILL_OFFSET,
                        blur        = NeumorphismShadow.PILL_BLUR,
                    )
                else
                    Modifier.neumorphicRaised(
                        bgColor     = colors.bg,
                        lightShadow = colors.lightShadow,
                        darkShadow  = colors.darkShadow,
                        cornerRadius= NeumorphismShadow.PILL_RADIUS,
                        offset      = NeumorphismShadow.PILL_OFFSET,
                        blur        = NeumorphismShadow.PILL_BLUR,
                    )
            )
            .background(color = colors.bg, shape = RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp)
    ) {
        Text(
            text       = label,
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize   = fontSize,
            color      = colors.accent,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NeumorphicIconButton
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Quadratischer Icon-Button im Neumorphism-Stil (z.B. für TopBar, Drawer).
 */
@Composable
fun NeumorphicIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    colors: NeumorphColors,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconTint: Color? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconBtnScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(
                if (isPressed)
                    Modifier.neumorphicPressed(
                        bgColor     = colors.bg,
                        lightShadow = colors.lightShadow,
                        darkShadow  = colors.darkShadow,
                        cornerRadius= 12f,
                    )
                else
                    Modifier.neumorphicRaised(
                        bgColor     = colors.bg,
                        lightShadow = colors.lightShadow,
                        darkShadow  = colors.darkShadow,
                        cornerRadius= 12f,
                    )
            )
            .background(color = colors.bg, shape = RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = contentDescription,
            tint               = iconTint ?: colors.accent,
            modifier           = Modifier.size(22.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NeumorphicToggle (Switch-Ersatz)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Neumorphism-Schalter als Ersatz für den Standard-Switch in Einstellungen.
 */
@Composable
fun NeumorphicSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: NeumorphColors,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .neumorphicPressed(
                bgColor     = colors.bg,
                lightShadow = colors.lightShadow,
                darkShadow  = colors.darkShadow,
                cornerRadius= 14f,
            )
            .background(color = colors.bg, shape = RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset, top = 2.dp)
                .size(24.dp)
                .neumorphicRaised(
                    bgColor     = colors.bg,
                    lightShadow = colors.lightShadow,
                    darkShadow  = colors.darkShadow,
                    cornerRadius= 12f,
                    offset      = 2f,
                    blur        = 4f,
                )
                .background(
                    color = if (checked) colors.accent else colors.bg,
                    shape = CircleShape
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NeumorphicBackground
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Füllt den Hintergrund mit der Neumorphism-Hintergrundfarbe.
 * Ersetzt MeshGradientBackground im Neumorphism-Modus.
 */
@Composable
fun NeumorphicBackground(
    colors: NeumorphColors,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
        content = content
    )
}
