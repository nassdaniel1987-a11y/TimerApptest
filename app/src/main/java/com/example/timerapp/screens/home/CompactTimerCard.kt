package com.example.timerapp.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timerapp.SettingsManager
import com.example.timerapp.models.Timer
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.example.timerapp.utils.CategoryColors
import com.example.timerapp.ui.theme.GlassColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactTimerCard(
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
        try { ZonedDateTime.parse(timer.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .withZoneSameInstant(java.time.ZoneId.systemDefault()) }
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



    val (borderColor, borderWidth) = when (timerState) {
        TimerState.PENDING -> Color(0xFF2196F3) to 1.5.dp
        TimerState.RUNNING -> Color(0xFFFF9800) to 2.5.dp
        TimerState.COMPLETED -> Color(0xFF4CAF50) to 1.5.dp
        TimerState.ALARM -> Color(0xFFF44336) to 3.dp
    }

    val urgencyColor = getTimerUrgencyColor(targetTime)

    val timeText = when {
        timer.is_completed -> "Fertig"
        isPast -> "Abgelaufen"
        else -> {
            val h = hoursUntil(now, targetTime)
            val m = minutesUntil % 60
            when {
                minutesUntil < 60 -> "$minutesUntil Min"
                h < 24 -> "${h}h ${m}m"
                else -> "${h/24} Tage"
            }
        }
    }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()

    val cardElevation by animateDpAsState(
        targetValue = if (isCardPressed) 2.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardElevation"
    )

    Box(modifier = modifier) {

        val glassColor = if (isSystemInDarkTheme()) GlassColors.GlassSurfaceDark else GlassColors.GlassSurfaceLight
        val borderAlpha = if (isSystemInDarkTheme()) 0.2f else 0.8f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .border(
                    width = borderWidth,
                    color = borderColor.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .clickable(
                    interactionSource = cardInteractionSource,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = {
                        performHaptic(haptic, settingsManager)
                        showEditDialog = true
                    }
                ),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = glassColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular Progress
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!timer.is_completed && !isPast) {
                        val createdAt = try {
                            ZonedDateTime.parse(timer.created_at, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                .withZoneSameInstant(java.time.ZoneId.systemDefault())
                        } catch (e: Exception) { null }
                        
                        val totalMinutes = if (createdAt != null) ChronoUnit.MINUTES.between(createdAt, targetTime).toFloat() else 1f
                        val progress = if (totalMinutes > 0) {
                            1f - (minutesUntil.toFloat() / totalMinutes)
                        } else {
                            1f
                        }

                        CircularProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            color = urgencyColor,
                            strokeWidth = 6.dp,
                            trackColor = urgencyColor.copy(alpha = 0.15f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(6.dp, urgencyColor, CircleShape)
                        )
                    }

                    // Inside Circle
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor,
                        maxLines = 1,
                        fontSize = if (timeText.length > 6) 12.sp else 14.sp
                    )
                }

                // Title & Category
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    
                    val timeStr = targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    Text(
                        text = "Ziel: $timeStr Uhr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = CategoryColors.getColor(timer.category).copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = timer.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = CategoryColors.getColor(timer.category),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!timer.is_completed) {
                        IconButton(
                            onClick = {
                                performHaptic(haptic, settingsManager)
                                onComplete()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Fertig", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(
                        onClick = {
                            performHaptic(haptic, settingsManager)
                            showDeleteDialog = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val isRecurring = timer.recurrence != null
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Timer löschen?") },
            text = { Text("Möchtest du '${timer.name}' löschen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Löschen")
                }
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

private fun hoursUntil(now: ZonedDateTime, target: ZonedDateTime): Long {
    return ChronoUnit.HOURS.between(now, target)
}
