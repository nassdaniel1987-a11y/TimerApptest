package com.example.timerapp.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timerapp.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Composable
fun ListHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (count != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

fun getTimerUrgencyColor(targetTime: ZonedDateTime): Color {
    val now = ZonedDateTime.now()
    val minutesUntil = ChronoUnit.MINUTES.between(now, targetTime)

    return when {
        minutesUntil < 0 -> Color(0xFFD32F2F)
        minutesUntil < 60 -> Color(0xFFE65100)
        targetTime.toLocalDate() == now.toLocalDate() -> Color(0xFFF57C00)
        targetTime.toLocalDate() == now.toLocalDate().plusDays(1) -> Color(0xFF388E3C)
        else -> Color(0xFF757575)
    }
}

@Composable
internal fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        interactionSource = interactionSource
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
internal fun TimerCompleteAnimation(
    visible: Boolean,
    onAnimationEnd: () -> Unit = {}
) {
    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(1500)
            onAnimationEnd()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(animationSpec = tween(300)),
        exit = scaleOut(
            targetScale = 1.2f,
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = Color(0xFF4CAF50),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Abgeschlossen",
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }
}

@Composable
internal fun ExactAlarmPermissionRationaleDialog(onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        icon = { Icon(Icons.Default.Alarm, contentDescription = "Alarm-Berechtigung") },
        title = { Text("Berechtigung erforderlich") },
        text = {
            Text(
                "Damit die Timer zuverlässig funktionieren, auch wenn die App geschlossen ist, " +
                        "benötigt die App die Berechtigung 'Alarme & Erinnerungen'."
            )
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("Zu den Einstellungen")
            }
        }
    )
}

fun performHaptic(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback, settingsManager: SettingsManager) {
    if (settingsManager.isHapticFeedbackEnabled) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

fun showSnackbar(snackbarHostState: SnackbarHostState, message: String) {
    CoroutineScope(Dispatchers.Main).launch {
        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
    }
}
