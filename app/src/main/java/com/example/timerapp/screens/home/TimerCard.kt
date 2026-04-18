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
import com.example.timerapp.ui.components.BrutalistProgressBar
import com.example.timerapp.ui.components.BrutalistStatusDot
import com.example.timerapp.ui.components.BrutalistTag
import com.example.timerapp.ui.components.brutalistAccentBar
import com.example.timerapp.ui.theme.BrutalistColors
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
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    val isDark = isSystemInDarkTheme()
    val glassBg = if (isDark) GlassColors.GlassSurfaceDark else GlassColors.GlassSurfaceLight
    val glassBorder = if (isDark) GlassColors.GlassBorderDark else GlassColors.GlassBorderLight
    val cardShape = RoundedCornerShape(24.dp)
    val designTheme = LocalAppDesignTheme.current
    val isNeumorphism = designTheme == AppDesignTheme.NEUMORPHISM
    val isBrutalist = designTheme == AppDesignTheme.BRUTALIST
    val nmColors = if (isDark) neumorphColorsDark() else neumorphColorsLight()

    // Brutalist accent colour maps to timer state colours
    val brutalistAccent = when (timerState) {
        TimerState.PENDING   -> BrutalistColors.StatusPending
        TimerState.RUNNING   -> BrutalistColors.StatusRunning
        TimerState.COMPLETED -> BrutalistColors.StatusCompleted
        TimerState.ALARM     -> BrutalistColors.StatusAlarm
    }

    SwipeableActionsBox(
        startActions = if (!timer.is_completed) listOf(completeAction) else emptyList(),
        endActions = listOf(deleteAction),
        swipeThreshold = 180.dp
    ) {
        Box(modifier = modifier.fillMaxWidth()) {
            // Ambient glow for active states


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(cardScale)
                    .then(
                        when {
                            isBrutalist -> Modifier
                                .clip(RoundedCornerShape(0.dp))
                                .background(BrutalistColors.Surface)
                                .border(1.dp, BrutalistColors.Border, RoundedCornerShape(0.dp))
                                .brutalistAccentBar(
                                    accentColor = brutalistAccent,
                                    barWidth = 3f
                                )
                            isNeumorphism -> Modifier
                                .clip(cardShape)
                                .neumorphicRaised(
                                    bgColor     = nmColors.bg,
                                    lightShadow = nmColors.lightShadow,
                                    darkShadow  = nmColors.darkShadow,
                                    cornerRadius= 24f,
                                )
                                .background(nmColors.bg, cardShape)
                                .border(
                                    width = 2.dp,
                                    color = accentColor.copy(alpha = 0.7f),
                                    shape = cardShape
                                )
                            else -> Modifier
                                .clip(cardShape)
                                .background(glassBg)
                                .border(1.dp, glassBorder, cardShape)
                                .border(
                                    width = 3.dp,
                                    brush = Brush.linearGradient(listOf(accentColor.copy(alpha = 0.7f), accentColor.copy(alpha = 0.2f))),
                                    shape = cardShape
                                )
                        }
                    )
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = { performHaptic(haptic, settingsManager) }
                    )
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = if (isBrutalist) 20.dp else 16.dp,
                        top = 16.dp, end = 16.dp, bottom = 16.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status dot — square for Brutalist
                        if (isBrutalist) {
                            BrutalistStatusDot(
                                color = brutalistAccent,
                                pulseAlpha = 1f,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(accentColor)
                            )
                        }

                        // Timer name
                        Text(
                            text = if (isBrutalist) timer.name.uppercase() else timer.name,
                            fontFamily = if (isBrutalist) com.example.timerapp.ui.components.BrutalistFontFamily else ManropeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isBrutalist) 13.sp else 16.sp,
                            color = when {
                                isBrutalist   -> BrutalistColors.TextPrimary
                                isNeumorphism -> nmColors.textPrimary
                                else          -> MaterialTheme.colorScheme.onSurface
                            },
                            letterSpacing = if (isBrutalist) 1.sp else 0.sp,
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

                    // Countdown / time text
                    Text(
                        text = timeText,
                        fontFamily = if (isBrutalist) com.example.timerapp.ui.components.BrutalistFontFamily else ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isBrutalist) 18.sp else 22.sp,
                        color = if (isBrutalist) brutalistAccent else accentColor,
                        letterSpacing = if (isBrutalist) 0.sp else (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category + Klasse row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val catColor = com.example.timerapp.utils.CategoryColors.getColor(timer.category)

                        if (isBrutalist) {
                            // Brutalist: square tags, no pills
                            BrutalistTag(
                                text = timer.category,
                                color = BrutalistColors.TextSecondary,
                                borderColor = BrutalistColors.Border,
                            )
                            if (timer.klasse?.isNotBlank() == true) {
                                BrutalistTag(
                                    text = timer.klasse ?: "",
                                    color = BrutalistColors.Cyan,
                                    borderColor = BrutalistColors.CyanDim,
                                )
                            }
                        } else {
                            // Classic / Neumorphism: pill badges
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
                        }

                        // Note (if set)
                        if (timer.note?.isNotBlank() == true) {
                            Text(
                                text = timer.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBrutalist) BrutalistColors.TextSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Progress bar
                    if (!timer.is_completed && !isPast) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (isBrutalist) {
                            BrutalistProgressBar(
                                progress = progress,
                                accentColor = brutalistAccent,
                            )
                        } else {
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
