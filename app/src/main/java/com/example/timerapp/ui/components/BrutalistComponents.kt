package com.example.timerapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timerapp.ui.theme.BrutalistColors

// Monospace font alias — Brutalist uses monospaced layout feel
val BrutalistFontFamily: FontFamily get() = FontFamily.Monospace

// ─────────────────────────────────────────────────────────────────────────────
// Modifier: Scan-line overlay (horizontal repeating bands)
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.scanLines(
    lineSpacing: Float = 4f,
    alpha: Float = 0.06f,
): Modifier = this.drawWithContent {
    drawContent()
    var y = 0f
    while (y < size.height) {
        drawRect(
            color = Color.Black.copy(alpha = alpha),
            topLeft = Offset(0f, y),
            size = Size(size.width, lineSpacing / 2f),
        )
        y += lineSpacing
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modifier: Cyan left-border accent (card state indicator)
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.brutalistAccentBar(
    accentColor: Color = BrutalistColors.Cyan,
    barWidth: Float = 3f,
): Modifier = this.drawBehind {
    drawRect(
        color = accentColor,
        topLeft = Offset(0f, 0f),
        size = Size(barWidth, size.height)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// BrutalistBackground — raw #0D0D0D with subtle scan-line grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BrutalistBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrutalistColors.Background)
            .scanLines(lineSpacing = 5f, alpha = 0.055f),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// BrutalistCard — sharp-cornered, thin border, accent bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BrutalistCard(
    modifier: Modifier = Modifier,
    accentColor: Color = BrutalistColors.Border,
    showAccentBar: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BrutalistColors.Surface)
            .border(1.dp, BrutalistColors.Border, RoundedCornerShape(0.dp))
            .then(
                if (showAccentBar) Modifier.brutalistAccentBar(accentColor)
                else Modifier
            )
            .padding(
                start = if (showAccentBar) 20.dp else 16.dp,
                top = 16.dp, end = 16.dp, bottom = 16.dp
            ),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// BrutalistFAB — square with glowing cyan
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BrutalistFAB(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "brutalistFabScale"
    )

    // Pulsing cyan border when idle
    val infiniteTransition = rememberInfiniteTransition(label = "fabPulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabBorderAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .background(
                color = if (isPressed) BrutalistColors.CyanDim else BrutalistColors.Cyan,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isPressed) 0.dp else 1.dp,
                color = BrutalistColors.Cyan.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = BrutalistColors.Background,
            modifier = Modifier.size(26.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BrutalistQuickTimerButton — monospace pill with top/bottom border only
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BrutalistQuickTimerButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = BrutalistColors.Cyan,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "brutalistBtnScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(36.dp)
            .scale(scale)
            .background(
                color = if (isPressed) accentColor.copy(alpha = 0.1f) else BrutalistColors.Surface,
                shape = RoundedCornerShape(2.dp)
            )
            .border(1.dp, if (isPressed) accentColor else BrutalistColors.Border, RoundedCornerShape(2.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = label,
            fontFamily = BrutalistFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (isPressed) accentColor else BrutalistColors.TextSecondary,
            letterSpacing = 1.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BrutalistIconButton — minimal square icon button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BrutalistIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = BrutalistColors.TextSecondary,
    size: Dp = 40.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(
                color = if (isPressed) BrutalistColors.CyanGlow else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPressed) BrutalistColors.Cyan else iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BrutalistProgressBar — segmented block style instead of smooth bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BrutalistProgressBar(
    progress: Float,
    accentColor: Color = BrutalistColors.Cyan,
    modifier: Modifier = Modifier,
    segments: Int = 20,
) {
    val filledSegments = (progress * segments).toInt().coerceIn(0, segments)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(segments) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        color = if (i < filledSegments) accentColor else BrutalistColors.Border
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BrutalistStatusDot — square dot (no rounding)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BrutalistStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    pulseAlpha: Float = 1f,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = pulseAlpha))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// BrutalistTag — uppercase monospace label badge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BrutalistTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BrutalistColors.TextSecondary,
    borderColor: Color = BrutalistColors.Border,
    fontSize: TextUnit = 10.sp,
) {
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .border(1.dp, borderColor, RoundedCornerShape(0.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = BrutalistFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = color,
            letterSpacing = 1.2.sp,
            maxLines = 1,
        )
    }
}
