package com.example.timerapp.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val context = LocalContext.current
    var hasExactAlarmPermission by remember { mutableStateOf(true) }

    fun checkPermission() {
        hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else { true }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!hasExactAlarmPermission) {
        ExactAlarmPermissionRationaleDialog(onGoToSettings = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also { context.startActivity(it) }
            }
        })
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
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
                },
                scrollBehavior = scrollBehavior
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
            if (timers.isEmpty()) {
                EmptyState(onCreateTimer = onCreateTimer)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeTimers.isNotEmpty()) {
                        item {
                            ListHeader("Aktive Timer")
                        }
                        items(activeTimers, key = { it.id }) { timer ->
                            TimerCard(
                                modifier = Modifier.animateItemPlacement(),
                                timer = timer,
                                onComplete = { viewModel.markTimerCompleted(timer.id) },
                                onDelete = { viewModel.deleteTimer(timer.id) }
                            )
                        }
                    }

                    if (completedTimers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            ListHeader("Abgeschlossen")
                        }
                        items(completedTimers, key = { it.id }) { timer ->
                            TimerCard(
                                modifier = Modifier.animateItemPlacement(),
                                timer = timer,
                                onComplete = { /* Nichts tun */ },
                                onDelete = { viewModel.deleteTimer(timer.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun TimerCard(
    timer: Timer,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val targetTime = remember(timer.target_time) {
        try { ZonedDateTime.parse(timer.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
        catch (e: Exception) { null }
    }

    if (targetTime == null) {
        return
    }

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

    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = timer.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = timeText, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = timer.category, style = MaterialTheme.typography.bodySmall)
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
                    IconButton(onClick = onComplete) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Abschließen")
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen")
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
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}

// HIER SIND DIE FEHLENDEN FUNKTIONEN
@Composable
private fun ExactAlarmPermissionRationaleDialog(onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Nicht schließbar */ },
        icon = { Icon(Icons.Default.Alarm, contentDescription = null) },
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

@Composable
private fun EmptyState(onCreateTimer: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Keine Timer vorhanden",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCreateTimer) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Neuen Timer erstellen")
            }
        }
    }
}