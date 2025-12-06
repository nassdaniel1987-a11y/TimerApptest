package com.example.timerapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.timerapp.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }

    var isSoundEnabled by remember { mutableStateOf(settingsManager.isSoundEnabled) }
    var isVibrationEnabled by remember { mutableStateOf(settingsManager.isVibrationEnabled) }
    var isPreReminderEnabled by remember { mutableStateOf(settingsManager.isPreReminderEnabled) }
    var preReminderMinutes by remember { mutableStateOf(settingsManager.preReminderMinutes) }
    var snoozeMinutes by remember { mutableStateOf(settingsManager.snoozeMinutes) }
    var isDarkModeEnabled by remember { mutableStateOf(settingsManager.isDarkModeEnabled) }
    var isHapticFeedbackEnabled by remember { mutableStateOf(settingsManager.isHapticFeedbackEnabled) }
    var isEscalatingAlarmEnabled by remember { mutableStateOf(settingsManager.isEscalatingAlarmEnabled) }

    var showPreReminderDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ═══════════ DARSTELLUNG ═══════════
            ListItem(
                headlineContent = {
                    Text(
                        "Darstellung",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )

            Divider()

            // Dark Mode
            ListItem(
                headlineContent = { Text("Dark Mode") },
                supportingContent = { Text("Dunkles Farbschema aktivieren") },
                leadingContent = {
                    Icon(
                        if (isDarkModeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isDarkModeEnabled,
                        onCheckedChange = {
                            isDarkModeEnabled = it
                            settingsManager.isDarkModeEnabled = it
                            onDarkModeChange(it)
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════ ALARM-EINSTELLUNGEN ═══════════
            ListItem(
                headlineContent = {
                    Text(
                        "Alarm-Einstellungen",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )

            Divider()

            // Sound
            ListItem(
                headlineContent = { Text("Sound") },
                supportingContent = { Text("Alarm-Ton abspielen") },
                leadingContent = {
                    Icon(
                        if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isSoundEnabled,
                        onCheckedChange = {
                            isSoundEnabled = it
                            settingsManager.isSoundEnabled = it
                        }
                    )
                }
            )

            // Vibration
            ListItem(
                headlineContent = { Text("Vibration") },
                supportingContent = { Text("Gerät vibrieren lassen") },
                leadingContent = {
                    Icon(
                        if (isVibrationEnabled) Icons.Default.Vibration else Icons.Default.PhoneDisabled,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isVibrationEnabled,
                        onCheckedChange = {
                            isVibrationEnabled = it
                            settingsManager.isVibrationEnabled = it
                        }
                    )
                }
            )

            // ✅ Eskalierender Alarm
            ListItem(
                headlineContent = { Text("Eskalierender Alarm") },
                supportingContent = { Text("Alarm wird nach 1 Min lauter") },
                leadingContent = {
                    Icon(Icons.Default.TrendingUp, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = isEscalatingAlarmEnabled,
                        onCheckedChange = {
                            isEscalatingAlarmEnabled = it
                            settingsManager.isEscalatingAlarmEnabled = it
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════ ERINNERUNGEN ═══════════
            ListItem(
                headlineContent = {
                    Text(
                        "Erinnerungen",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )

            Divider()

            // Vorab-Erinnerung
            ListItem(
                headlineContent = { Text("Vorab-Erinnerung") },
                supportingContent = {
                    Text(
                        if (isPreReminderEnabled)
                            "$preReminderMinutes Minuten vor dem Timer"
                        else
                            "Keine Vorab-Erinnerung"
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = isPreReminderEnabled,
                        onCheckedChange = {
                            isPreReminderEnabled = it
                            settingsManager.isPreReminderEnabled = it
                        }
                    )
                }
            )

            if (isPreReminderEnabled) {
                ListItem(
                    headlineContent = { Text("Vorab-Erinnerung Zeit") },
                    supportingContent = { Text("$preReminderMinutes Minuten vorher") },
                    leadingContent = {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                    },
                    trailingContent = {
                        TextButton(onClick = { showPreReminderDialog = true }) {
                            Text("Ändern")
                        }
                    }
                )
            }

            // ✅ Snooze-Zeit
            ListItem(
                headlineContent = { Text("Snooze-Zeit") },
                supportingContent = { Text("$snoozeMinutes Minuten beim Schlummern") },
                leadingContent = {
                    Icon(Icons.Default.Snooze, contentDescription = null)
                },
                trailingContent = {
                    TextButton(onClick = { showSnoozeDialog = true }) {
                        Text("Ändern")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════ BEDIENUNG ═══════════
            ListItem(
                headlineContent = {
                    Text(
                        "Bedienung",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )

            Divider()

            // ✅ Haptisches Feedback
            ListItem(
                headlineContent = { Text("Haptisches Feedback") },
                supportingContent = { Text("Vibration bei Aktionen") },
                leadingContent = {
                    Icon(Icons.Default.TouchApp, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = isHapticFeedbackEnabled,
                        onCheckedChange = {
                            isHapticFeedbackEnabled = it
                            settingsManager.isHapticFeedbackEnabled = it
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════ INFO ═══════════
            ListItem(
                headlineContent = {
                    Text(
                        "Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )

            Divider()

            ListItem(
                headlineContent = { Text("Zeitzone") },
                supportingContent = { Text("${java.time.ZoneId.systemDefault().id} (Automatisch erkannt)") },
                leadingContent = {
                    Icon(Icons.Default.Public, contentDescription = null)
                }
            )

            ListItem(
                headlineContent = { Text("App-Version") },
                supportingContent = { Text("1.0.0") },
                leadingContent = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hinweis-Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Wichtiger Hinweis",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Für zuverlässige Alarme stelle sicher, dass:\n" +
                                    "• Benachrichtigungen erlaubt sind\n" +
                                    "• 'Alarme & Erinnerungen' aktiviert ist\n" +
                                    "• Batterie-Optimierung deaktiviert ist",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    // Pre-Reminder Time Picker Dialog
    if (showPreReminderDialog) {
        val options = listOf(5, 10, 15, 20, 30, 45, 60)

        AlertDialog(
            onDismissRequest = { showPreReminderDialog = false },
            title = { Text("Vorab-Erinnerung Zeit") },
            text = {
                Column {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preReminderMinutes == minutes,
                                onClick = {
                                    preReminderMinutes = minutes
                                    settingsManager.preReminderMinutes = minutes
                                    showPreReminderDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$minutes Minuten",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPreReminderDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }

    // ✅ Snooze Time Picker Dialog
    if (showSnoozeDialog) {
        val options = listOf(5, 10, 15, 20, 30)

        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text("Snooze-Zeit") },
            text = {
                Column {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = snoozeMinutes == minutes,
                                onClick = {
                                    snoozeMinutes = minutes
                                    settingsManager.snoozeMinutes = minutes
                                    showSnoozeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$minutes Minuten",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnoozeDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }
}
