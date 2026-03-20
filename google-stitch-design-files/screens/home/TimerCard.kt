package com.example.timerapp.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import com.example.timerapp.SettingsManager
import com.example.timerapp.models.Timer
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
        try { ZonedDateTime.parse(timer.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
        catch (e: Exception) { null }
    }

    if (targetTime == null) {
        return
    }

    val now = ZonedDateTime.now()
    val isPast = targetTime.isBefore(now)
    val minutesUntil = ChronoUnit.MINUTES.between(now, targetTime)

    val timerState = when {
        timer.is_completed -> TimerState.COMPLETED
        isPast -> TimerState.ALARM
        minutesUntil <= 60 -> TimerState.RUNNING
        else -> TimerState.PENDING
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (borderColor, borderWidth) = when (timerState) {
        TimerState.PENDING -> Color(0xFF2196F3) to 2.dp
        TimerState.RUNNING -> Color(0xFFFF9800) to 3.dp
        TimerState.COMPLETED -> Color(0xFF4CAF50) to 2.dp
        TimerState.ALARM -> Color(0xFFF44336) to 3.dp
    }

    val urgencyColor = getTimerUrgencyColor(targetTime)

    val timeText = when {
        timer.is_completed -> "Abgeschlossen"
        isPast -> "Abgelaufen"
        else -> {
            val minutesUntil = ChronoUnit.MINUTES.between(now, targetTime)
            val hoursUntil = ChronoUnit.HOURS.between(now, targetTime)
            val daysUntil = ChronoUnit.DAYS.between(now, targetTime)

            when {
                minutesUntil < 60 -> "Noch $minutesUntil Min"
                hoursUntil < 24 -> "Noch ${hoursUntil}h ${minutesUntil % 60}min"
                daysUntil == 0L -> "Heute ${targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))} Uhr"
                daysUntil == 1L -> "Morgen ${targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))} Uhr"
                else -> targetTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " Uhr"
            }
        }
    }

    val deleteAction = SwipeAction(
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Löschen",
                tint = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        },
        background = Color(0xFFB00020),
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
        background = Color(0xFF4CAF50),
        onSwipe = {
            performHaptic(haptic, settingsManager)
            onComplete()
        }
    )

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()

    val cardElevation by animateDpAsState(
        targetValue = if (isCardPressed) 2.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardElevation"
    )

    SwipeableActionsBox(
        startActions = if (!timer.is_completed) listOf(completeAction) else emptyList(),
        endActions = listOf(deleteAction),
        swipeThreshold = 180.dp
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
        ) {
            if (timerState == TimerState.RUNNING || timerState == TimerState.ALARM) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .scale(pulseScale * 1.05f)
                        .blur(24.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    borderColor.copy(alpha = 0.4f * pulseAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                )
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .then(
                        if (timerState == TimerState.RUNNING || timerState == TimerState.ALARM) {
                            Modifier
                                .scale(pulseScale)
                                .border(
                                    width = borderWidth,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            borderColor.copy(alpha = pulseAlpha),
                                            borderColor.copy(alpha = pulseAlpha * 0.5f)
                                        )
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                )
                        } else {
                            Modifier.border(
                                width = borderWidth,
                                color = borderColor.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    )
                    .clickable(
                        onClick = {
                            performHaptic(haptic, settingsManager)
                        }
                    ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = cardElevation),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
            Column {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!timer.is_completed && !isPast) {
                        val totalMinutes = ChronoUnit.MINUTES.between(
                            ZonedDateTime.parse(timer.created_at, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            targetTime
                        ).toFloat()
                        val remainingMinutes = minutesUntil.toFloat()
                        val progress = if (totalMinutes > 0) {
                            1f - (remainingMinutes / totalMinutes)
                        } else {
                            1f
                        }

                        Box(
                            modifier = Modifier.size(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.size(60.dp),
                                color = urgencyColor,
                                strokeWidth = 4.dp,
                                trackColor = urgencyColor.copy(alpha = 0.2f)
                            )
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = urgencyColor
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(60.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(urgencyColor)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = timer.name, style = MaterialTheme.typography.titleMedium)

                        if (timer.recurrence != null) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = "Wiederholend",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = when (timer.recurrence) {
                                            "daily" -> "Tägl."
                                            "weekly" -> "Wöch."
                                            "weekdays" -> "Werkt."
                                            "weekends" -> "WE"
                                            "custom" -> {
                                                if (!timer.recurrence_weekdays.isNullOrBlank()) {
                                                    val weekdayNames = mapOf(
                                                        1 to "Mo", 2 to "Di", 3 to "Mi", 4 to "Do",
                                                        5 to "Fr", 6 to "Sa", 7 to "So"
                                                    )
                                                    val days = timer.recurrence_weekdays.split(",")
                                                        .mapNotNull { it.trim().toIntOrNull() }
                                                        .sorted()
                                                        .mapNotNull { weekdayNames[it] }
                                                        .joinToString(",")
                                                    days
                                                } else {
                                                    "Custom"
                                                }
                                            }
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = urgencyColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = urgencyColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = com.example.timerapp.utils.CategoryColors.getColor(timer.category).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Kategorie",
                                modifier = Modifier.size(14.dp),
                                tint = com.example.timerapp.utils.CategoryColors.getColor(timer.category)
                            )
                            Text(
                                text = timer.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = com.example.timerapp.utils.CategoryColors.getColor(timer.category),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (timer.note?.isNotBlank() == true) {
                        Text(
                            text = timer.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (!timer.is_completed) {
                        AnimatedIconButton(
                            onClick = {
                                performHaptic(haptic, settingsManager)
                                showEditDialog = true
                            },
                            icon = Icons.Default.Edit,
                            contentDescription = "Bearbeiten"
                        )
                    }
                    if (!timer.is_completed) {
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
                }

                if (!timer.is_completed) {
                    val createdAt = try {
                        ZonedDateTime.parse(timer.created_at, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    } catch (e: Exception) {
                        null
                    }

                    if (createdAt != null) {
                        val totalDuration = ChronoUnit.MINUTES.between(createdAt, targetTime).toFloat()
                        val elapsed = ChronoUnit.MINUTES.between(createdAt, now).toFloat()
                        val linearProgress = if (totalDuration > 0) {
                            (elapsed / totalDuration).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { linearProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(MaterialTheme.shapes.small),
                                color = urgencyColor,
                                trackColor = urgencyColor.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        val isRecurring = timer.recurrence != null
        val recurrenceDescription = when (timer.recurrence) {
            "daily" -> "täglich"
            "weekly" -> "wöchentlich"
            "weekdays" -> "werktags (Mo-Fr)"
            "weekends" -> "an Wochenenden (Sa-So)"
            "custom" -> {
                if (!timer.recurrence_weekdays.isNullOrBlank()) {
                    val weekdayNames = mapOf(
                        1 to "Montag", 2 to "Dienstag", 3 to "Mittwoch", 4 to "Donnerstag",
                        5 to "Freitag", 6 to "Samstag", 7 to "Sonntag"
                    )
                    val days = timer.recurrence_weekdays.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .sorted()
                        .mapNotNull { weekdayNames[it] }
                    "jeden ${days.joinToString(", ")}"
                } else {
                    "benutzerdefiniert"
                }
            }
            else -> null
        }

        val recurrenceEndDateText = timer.recurrence_end_date?.let {
            try {
                val endDate = ZonedDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                "Endet am: ${endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
            } catch (e: Exception) {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = if (isRecurring) {
                { Icon(Icons.Default.Warning, contentDescription = "Warnung", tint = MaterialTheme.colorScheme.error) }
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
                                    shape = MaterialTheme.shapes.medium
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
                            Column {
                                Text(
                                    text = "Dies ist ein wiederholender Timer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Wiederholt sich: $recurrenceDescription",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (recurrenceEndDateText != null) {
                                    Text(
                                        text = recurrenceEndDateText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = if (isRecurring) {
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.textButtonColors()
                    }
                ) {
                    Text(if (isRecurring) "Trotzdem löschen" else "Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    // Edit Dialog
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
