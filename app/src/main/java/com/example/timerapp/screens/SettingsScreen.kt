package com.example.timerapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timerapp.SettingsManager
import com.example.timerapp.ui.theme.GradientColors

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

    // 🎨 Gradient Background
    val backgroundGradient = Brush.verticalGradient(
        colors = if (isSystemInDarkTheme()) {
            GradientColors.BackgroundDark
        } else {
            GradientColors.BackgroundLight
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Einstellungen",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ═══════════ DARSTELLUNG ═══════════
                Text(
                    "Darstellung",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    ListItem(
                        headlineContent = { Text("Dark Mode", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Dunkles Farbschema aktivieren") },
                        leadingContent = {
                            Icon(
                                if (isDarkModeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
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
                }

                // ═══════════ ALARM-EINSTELLUNGEN ═══════════
                Text(
                    "Alarm-Einstellungen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    ListItem(
                        headlineContent = { Text("Sound", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Alarm-Ton abspielen") },
                        leadingContent = {
                            Icon(
                                if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
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

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Vibration", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Gerät vibrieren lassen") },
                        leadingContent = {
                            Icon(
                                if (isVibrationEnabled) Icons.Default.Vibration else Icons.Default.PhoneDisabled,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
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

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Eskalierender Alarm", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Alarm wird nach 1 Min lauter") },
                        leadingContent = {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
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
                }

                // ═══════════ ERINNERUNGEN ═══════════
                Text(
                    "Erinnerungen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    ListItem(
                        headlineContent = { Text("Vorab-Erinnerung", fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(
                                if (isPreReminderEnabled)
                                    "$preReminderMinutes Minuten vor dem Timer"
                                else
                                    "Keine Vorab-Erinnerung"
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
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
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Vorab-Erinnerung Zeit", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("$preReminderMinutes Minuten vorher") },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingContent = {
                                TextButton(onClick = { showPreReminderDialog = true }) {
                                    Text("Ändern")
                                }
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Snooze-Zeit", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("$snoozeMinutes Minuten beim Schlummern") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Snooze,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = { showSnoozeDialog = true }) {
                                Text("Ändern")
                            }
                        }
                    )
                }

                // ═══════════ BEDIENUNG ═══════════
                Text(
                    "Bedienung",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    ListItem(
                        headlineContent = { Text("Haptisches Feedback", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Vibration bei Aktionen") },
                        leadingContent = {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
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
                }

                // ═══════════ INFO ═══════════
                Text(
                    "Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    ListItem(
                        headlineContent = { Text("Zeitzone", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("${java.time.ZoneId.systemDefault().id} (Automatisch erkannt)") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("App-Version", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("1.0.0") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }

                // 🎨 Hinweis-Card mit Glasmorphism
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Wichtiger Hinweis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Für zuverlässige Alarme stelle sicher, dass:\n" +
                                        "• Benachrichtigungen erlaubt sind\n" +
                                        "• 'Alarme & Erinnerungen' aktiviert ist\n" +
                                        "• Batterie-Optimierung deaktiviert ist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = preReminderMinutes == minutes,
                                onClick = {
                                    preReminderMinutes = minutes
                                    settingsManager.preReminderMinutes = minutes
                                    showPreReminderDialog = false
                                }
                            )
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

    // Snooze Time Picker Dialog
    if (showSnoozeDialog) {
        val options = listOf(5, 10, 15, 20, 30)

        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text("Snooze-Zeit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = snoozeMinutes == minutes,
                                onClick = {
                                    snoozeMinutes = minutes
                                    settingsManager.snoozeMinutes = minutes
                                    showSnoozeDialog = false
                                }
                            )
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
