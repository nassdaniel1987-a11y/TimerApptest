package com.example.timerapp.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timerapp.AppDesignTheme
import com.example.timerapp.SettingsManager
import com.example.timerapp.models.Timer
import com.example.timerapp.ui.theme.DesignTokens
import com.example.timerapp.ui.theme.GlassColors
import com.example.timerapp.ui.theme.LocalAppDesignTheme
import com.example.timerapp.ui.theme.ManropeFontFamily
import com.example.timerapp.ui.components.neumorphColorsLight
import com.example.timerapp.ui.components.neumorphColorsDark
import com.example.timerapp.ui.components.neumorphicRaised
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal enum class TimerState {
    PENDING,
    RUNNING,
    COMPLETED,
    ALARM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimerCard(
    timer: Timer,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (Timer) -> Unit,
    settingsManager: SettingsManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val targetTime = remember(timer.target_time) {
        try {
            ZonedDateTime.parse(timer.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withZoneSameInstant(java.time.ZoneId.systemDefault())
        } catch (e: Exception) { null }
    } ?: return

    val now = ZonedDateTime.now()
    val isPast = targetTime.isBefore(now)
    val minutesUntil = ChronoUnit.MINUTES.between(now, targetTime)

    val timerState = when {
        timer.is_completed -> TimerState.COMPLETED
        isPast -> TimerState.ALARM
        minutesUntil <= 60 -> TimerState.RUNNING
        else -> TimerState.PENDING
    }

    // Pulse animation for RUNNING / ALARM states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // State-based accent colour (left border + dot)
    val accentColor = when (timerState) {
        TimerState.PENDING   -> DesignTokens.BorderPending
        TimerState.RUNNING   -> DesignTokens.BorderRunning
        TimerState.COMPLETED -> DesignTokens.BorderCompleted
        TimerState.ALARM     -> DesignTokens.BorderAlarm
    }

    val timeText = when {
        timer.is_completed -> "Abgeschlossen"
        isPast -> "Abgelaufen"
        else -> {
            val h = ChronoUnit.HOURS.between(now, targetTime)
            val d = ChronoUnit.DAYS.between(now, targetTime)
            when {
                minutesUntil < 60 -> "Noch ${minutesUntil} Min"
                h < 24            -> "Noch ${h}h ${minutesUntil % 60}min"
                d == 0L           -> "Heute ${targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))} Uhr"
                d == 1L           -> "Morgen ${targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))} Uhr"
                else              -> targetTime.format(DateTimeFormatter.ofPattern("dd.MM. HH:mm")) + " Uhr"
            }
        }
    }

    // Progress
    val progress = remember(timer, now) {
        val createdAt = try {
            ZonedDateTime.parse(timer.created_at, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withZoneSameInstant(java.time.ZoneId.systemDefault())
        } catch (e: Exception) { null }
        if (createdAt != null) {
            val total = ChronoUnit.MINUTES.between(createdAt, targetTime).toFloat()
            val elapsed = ChronoUnit.MINUTES.between(createdAt, now).toFloat()
            if (total > 0) (elapsed / total).coerceIn(0f, 1f) else 0f
        } else 0f
    }

    // Swipe actions
    val deleteAction = SwipeAction(
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Löschen",
                tint = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        },
        background = DesignTokens.BorderAlarm,
        onSwipe = {
            performHaptic(haptic, settingsManager)
            showDeleteDialog = true
        }
    )
    val completeAction = SwipeAction(
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Abschließen",
                tint = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        },
        background = DesignTokens.BorderCompleted,
        onSwipe = {
            performHaptic(haptic, settingsManager)
            onComplete()
        }
    )

    val cardInteraction = remember { MutableInteractionSource() }
    val isPressed by cardInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else if (timerState == TimerState.ALARM || timerState == TimerState.RUNNING) pulseScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    val isDark = isSystemInDarkTheme()
    val glassBg = if (isDark) GlassColors.GlassSurfaceDark else GlassColors.GlassSurfaceLight
    val glassBorder = if (isDark) GlassColors.GlassBorderDark else GlassColors.GlassBorderLight
    val cardShape = RoundedCornerShape(24.dp)
    val designTheme = LocalAppDesignTheme.current
    val isNeumorphism = designTheme == AppDesignTheme.NEUMORPHISM
    val nmColors = if (isDark) neumorphColorsDark() else neumorphColorsLight()

    SwipeableActionsBox(
        startActions = if (!timer.is_completed) listOf(completeAction) else emptyList(),
        endActions = listOf(deleteAction),
        swipeThreshold = 180.dp
    ) {
        Box(modifier = modifier.fillMaxWidth()) {
            // Ambient glow for active states
            if (timerState == TimerState.RUNNING || timerState == TimerState.ALARM) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .blur(32.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.25f * pulseAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(cardScale)
                    .clip(cardShape)
                    .then(
                        if (isNeumorphism)
                            Modifier
                                .neumorphicRaised(
                                    bgColor     = nmColors.bg,
                                    lightShadow = nmColors.lightShadow,
                                    darkShadow  = nmColors.darkShadow,
                                    cornerRadius= 24f,
                                )
                                .background(nmColors.bg, cardShape)
                                .border(
                                    width = 2.dp,
                                    color = accentColor.copy(alpha = if (timerState == TimerState.RUNNING || timerState == TimerState.ALARM) pulseAlpha else 0.5f),
                                    shape = cardShape
                                )
                        else
                            Modifier
                                .background(glassBg)
                                .border(1.dp, glassBorder, cardShape)
                                .border(
                                    width = 3.dp,
                                    brush = if (timerState == TimerState.RUNNING || timerState == TimerState.ALARM) {
                                        Brush.linearGradient(listOf(accentColor.copy(alpha = pulseAlpha), accentColor.copy(alpha = pulseAlpha * 0.4f)))
                                    } else {
                                        Brush.linearGradient(listOf(accentColor.copy(alpha = 0.7f), accentColor.copy(alpha = 0.2f)))
                                    },
                                    shape = cardShape
                                )
                    )
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = { performHaptic(haptic, settingsManager) }
                    )
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(accentColor)
                        )

                        // Timer name
                        Text(
                            text = timer.name,
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isNeumorphism) nmColors.textPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        // Recurrence badge
                        if (timer.recurrence != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Repeat,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = when (timer.recurrence) {
                                            "daily" -> "Tägl."
                                            "weekly" -> "Wöch."
                                            "weekdays" -> "Werkt."
                                            "weekends" -> "WE"
                                            else -> "Wdh."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Action buttons
                        if (!timer.is_completed) {
                            AnimatedIconButton(
                                onClick = {
                                    performHaptic(haptic, settingsManager)
                                    showEditDialog = true
                                },
                                icon = Icons.Default.Edit,
                                contentDescription = "Bearbeiten"
                            )
                            AnimatedIconButton(
                                onClick = {
                                    performHaptic(haptic, settingsManager)
                                    onComplete()
                                },
                                icon = Icons.Default.CheckCircle,
                                contentDescription = "Abschließen"
                            )
                        }
                        AnimatedIconButton(
                            onClick = {
                                performHaptic(haptic, settingsManager)
                                showDeleteDialog = true
                            },
                            icon = Icons.Default.Delete,
                            contentDescription = "Löschen"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Countdown / time text (large, monospace feel)
                    Text(
                        text = timeText,
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = accentColor,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category + Klasse row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category pill
                        val catColor = com.example.timerapp.utils.CategoryColors.getColor(timer.category)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = catColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(catColor)
                                )
                                Text(
                                    text = timer.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = catColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Klasse pill (if set)
                        if (timer.klasse?.isNotBlank() == true) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = DesignTokens.IndigoAccent.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = timer.klasse ?: "",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DesignTokens.NavActiveColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Note (if set)
                        if (timer.note?.isNotBlank() == true) {
                            Text(
                                text = timer.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Progress bar
                    if (!timer.is_completed && !isPast) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(50)),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }

    // Delete dialog
    if (showDeleteDialog) {
        val isRecurring = timer.recurrence != null
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = if (isRecurring) {
                { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            } else null,
            title = { Text(if (isRecurring) "Wiederholenden Timer löschen?" else "Timer löschen?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Möchtest du '${timer.name}' wirklich löschen?")
                    if (isRecurring) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Dies ist ein wiederholender Timer",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(if (isRecurring) "Trotzdem löschen" else "Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    if (showEditDialog) {
        EditTimerDialog(
            timer = timer,
            onDismiss = { showEditDialog = false },
            onSave = { editedTimer ->
                onEdit(editedTimer)
                showEditDialog = false
            }
        )
    }
}
