package com.example.timerapp.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.timerapp.models.Timer
import com.example.timerapp.viewmodel.TimerViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TimerViewModel,
    onCreateTimer: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val timers by viewModel.timers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val activeTimers = timers.filter { !it.is_completed }
    val completedTimers = timers.filter { it.is_completed }

    // Code für die Berechtigungsprüfung
    val context = LocalContext.current
    var hasExactAlarmPermission by remember { mutableStateOf(true) }

    fun checkPermission() {
        hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasExactAlarmPermission) {
        ExactAlarmPermissionRationaleDialog(
            onGoToSettings = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                        context.startActivity(it)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timer") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.sync() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTimer) {
                Icon(Icons.Default.Add, contentDescription = "Timer erstellen")
            }
        }
    ) { padding ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isLoading),
            onRefresh = { viewModel.sync() },
            modifier = Modifier.padding(padding)
        ) {
            // ----- WIEDERHERGESTELLTER CODE -----
            if (timers.isEmpty()) {
                EmptyState(onCreateTimer = onCreateTimer)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeTimers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Aktive Timer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(activeTimers) { timer ->
                            TimerCard(
                                timer = timer,
                                onComplete = { viewModel.markTimerCompleted(timer.id) },
                                onDelete = { viewModel.deleteTimer(timer.id) }
                            )
                        }
                    }

                    if (completedTimers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Abgeschlossen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(completedTimers) { timer ->
                            TimerCard(
                                timer = timer,
                                onComplete = { /* Nichts tun bei abgeschlossenen */ },
                                onDelete = { viewModel.deleteTimer(timer.id) }
                            )
                        }
                    }
                }
            }
            // ----- ENDE WIEDERHERGESTELLTER CODE -----
        }
    }
}

@Composable
private fun ExactAlarmPermissionRationaleDialog(
    onGoToSettings: () -> Unit
) {
    // ... (unverändert)
}

@Composable
private fun EmptyState(onCreateTimer: () -> Unit) {
    // ... (unverändert)
}


// ----- START DER REPARIERTEN TIMERCARD -----
@Composable
private fun TimerCard(
    timer: Timer,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Wir versuchen, das Datum zu verarbeiten.
    val targetTime: ZonedDateTime? = try {
        ZonedDateTime.parse(timer.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (e: Exception) {
        // Wenn es fehlschlägt, loggen wir den Fehler und geben null zurück.
        Log.e("TimerCard", "Konnte Datum nicht verarbeiten: ${timer.target_time}", e)
        null
    }

    // Wenn die Verarbeitung fehlschlägt, zeigen wir eine Fehler-Karte an.
    if (targetTime == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                text = "Fehlerhafte Timer-Daten für '${timer.name}'. Zeit-String: ${timer.target_time}",
                modifier = Modifier.padding(16.dp)
            )
        }
        return // Beenden die Funktion für diese Karte
    }

    // Wenn alles gut geht, wird der normale Code ausgeführt.
    val now = ZonedDateTime.now()
    val minutesUntil = ChronoUnit.MINUTES.between(now, targetTime)
    val hoursUntil = ChronoUnit.HOURS.between(now, targetTime)
    val isPast = targetTime.isBefore(now)

    val timeText = when {
        timer.is_completed -> "Abgeschlossen"
        isPast -> "Abgelaufen"
        minutesUntil < 60 -> "in $minutesUntil Min"
        hoursUntil < 24 -> "in $hoursUntil Std"
        else -> targetTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    }

    val cardColor = when {
        timer.is_completed -> MaterialTheme.colorScheme.surfaceVariant
        isPast -> MaterialTheme.colorScheme.errorContainer
        minutesUntil < 30 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timer.category,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (timer.note?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timer.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!timer.is_completed) {
                    IconButton(onClick = onComplete) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Abschließen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Timer löschen?") },
            text = { Text("Möchtest du '${timer.name}' wirklich löschen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
// ----- ENDE DER REPARIERTEN TIMERCARD -----