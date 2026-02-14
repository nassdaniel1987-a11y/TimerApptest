package com.example.timerapp.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.timerapp.BuildConfig
import com.example.timerapp.SettingsManager
import com.example.timerapp.ui.theme.GradientColors
import com.example.timerapp.utils.NotificationHelper
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var showSoundPickerDialog by remember { mutableStateOf(false) }
    var showAddTimeDialog by remember { mutableStateOf(false) }
    var showCleanupDaysDialog by remember { mutableStateOf(false) }
    var alarmSoundName by remember { mutableStateOf(settingsManager.alarmSoundName) }
    var pickupTimes by remember { mutableStateOf(settingsManager.getPickupTimeList()) }
    var isAutoCleanupEnabled by remember { mutableStateOf(settingsManager.isAutoCleanupEnabled) }
    var autoCleanupDays by remember { mutableStateOf(settingsManager.autoCleanupDays) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI
            )
            if (uri != null) {
                settingsManager.alarmSoundUri = uri.toString()
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val title = ringtone?.getTitle(context) ?: "Benutzerdefiniert"
                settingsManager.alarmSoundName = title
                alarmSoundName = title
                NotificationHelper.recreateNotificationChannel(context)
            }
        }
    }

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
                                contentDescription = if (isDarkModeEnabled) "Dark Mode aktiviert" else "Light Mode aktiviert",
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
                                contentDescription = if (isSoundEnabled) "Sound aktiviert" else "Sound deaktiviert",
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

                    if (isSoundEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Alarm-Sound", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text(alarmSoundName) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = "Alarm-Sound",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                TextButton(onClick = { showSoundPickerDialog = true }) {
                                    Text("Ändern")
                                }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Vibration", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Gerät vibrieren lassen") },
                        leadingContent = {
                            Icon(
                                if (isVibrationEnabled) Icons.Default.Vibration else Icons.Default.PhoneDisabled,
                                contentDescription = if (isVibrationEnabled) "Vibration aktiviert" else "Vibration deaktiviert",
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

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Eskalierender Alarm", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Alarm wird nach 1 Min lauter") },
                        leadingContent = {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = "Eskalierender Alarm",
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
                                contentDescription = "Vorab-Erinnerung",
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
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Vorab-Erinnerung Zeit", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("$preReminderMinutes Minuten vorher") },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = "Erinnerungszeit",
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

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Snooze-Zeit", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("$snoozeMinutes Minuten beim Schlummern") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Snooze,
                                contentDescription = "Snooze-Zeit",
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
                                contentDescription = "Haptisches Feedback",
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

                // ═══════════ ABHOLZEITEN ═══════════
                Text(
                    "Abholzeiten-Vorlagen",
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Diese Zeiten erscheinen als Schnellauswahl beim Erstellen eines Timers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Zeit-Chips mit Löschen-Option
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pickupTimes) { time ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text(time, fontWeight = FontWeight.Medium) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Entfernen",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    settingsManager.removePickupTime(time)
                                                    pickupTimes = settingsManager.getPickupTimeList()
                                                }
                                        )
                                    }
                                )
                            }
                        }

                        // Buttons: Hinzufügen + Zurücksetzen
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddTimeDialog = true }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Zeit hinzufügen")
                            }
                            if (pickupTimes.size != 7 || pickupTimes.joinToString(",") != "13:00,13:45,14:00,14:45,15:00,15:45,16:00") {
                                TextButton(
                                    onClick = {
                                        settingsManager.pickupTimePresets = "13:00,13:45,14:00,14:45,15:00,15:45,16:00"
                                        pickupTimes = settingsManager.getPickupTimeList()
                                    }
                                ) {
                                    Text("Zurücksetzen")
                                }
                            }
                        }
                    }
                }

                // ═══════════ AUFRÄUMEN ═══════════
                Text(
                    "Aufräumen",
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
                        headlineContent = { Text("Auto-Aufräumen", fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(
                                if (isAutoCleanupEnabled)
                                    "Abgeschlossene Timer nach $autoCleanupDays Tagen löschen"
                                else
                                    "Abgeschlossene Timer manuell löschen"
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.AutoDelete,
                                contentDescription = "Auto-Aufräumen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isAutoCleanupEnabled,
                                onCheckedChange = {
                                    isAutoCleanupEnabled = it
                                    settingsManager.isAutoCleanupEnabled = it
                                }
                            )
                        }
                    )

                    if (isAutoCleanupEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Aufräumen nach", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("$autoCleanupDays Tagen") },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = "Aufräumen-Zeitraum",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingContent = {
                                TextButton(onClick = { showCleanupDaysDialog = true }) {
                                    Text("Ändern")
                                }
                            }
                        )
                    }
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
                                contentDescription = "Zeitzone",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("App-Version", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(BuildConfig.VERSION_NAME) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "App-Version",
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
                            contentDescription = "Wichtiger Hinweis",
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

    // Sound Picker Dialog
    if (showSoundPickerDialog) {
        val soundOptions = listOf(
            "Standard-Alarm" to null,
            "Standard-Klingelton" to RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.toString(),
            "Standard-Benachrichtigung" to RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString()
        )

        AlertDialog(
            onDismissRequest = { showSoundPickerDialog = false },
            title = { Text("Alarm-Sound wählen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    soundOptions.forEach { (name, uriString) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsManager.alarmSoundUri = uriString
                                    settingsManager.alarmSoundName = name
                                    alarmSoundName = name
                                    NotificationHelper.recreateNotificationChannel(context)
                                    showSoundPickerDialog = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = alarmSoundName == name,
                                onClick = {
                                    settingsManager.alarmSoundUri = uriString
                                    settingsManager.alarmSoundName = name
                                    alarmSoundName = name
                                    NotificationHelper.recreateNotificationChannel(context)
                                    showSoundPickerDialog = false
                                }
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val previewUri = if (uriString != null) {
                                        Uri.parse(uriString)
                                    } else {
                                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                    }
                                    val ringtone = RingtoneManager.getRingtone(context, previewUri)
                                    ringtone?.play()
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(3000)
                                        ringtone?.stop()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Vorschau",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Custom picker option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSoundPickerDialog = false
                                val existingUri = settingsManager.alarmSoundUri?.let { Uri.parse(it) }
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_TYPE,
                                        RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE
                                    )
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm-Sound wählen")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    if (existingUri != null) {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                                    }
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = settingsManager.alarmSoundUri != null
                                    && soundOptions.none { it.second == settingsManager.alarmSoundUri },
                            onClick = null
                        )
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Aus Gerät wählen...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoundPickerDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }

    // Add Time Preset Dialog
    if (showAddTimeDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = 14,
            initialMinute = 0,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showAddTimeDialog = false },
            title = { Text("Abholzeit hinzufügen") },
            text = {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.padding(8.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        settingsManager.addPickupTime(newTime)
                        pickupTimes = settingsManager.getPickupTimeList()
                        showAddTimeDialog = false
                    }
                ) {
                    Text("Hinzufügen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTimeDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Cleanup Days Dialog
    if (showCleanupDaysDialog) {
        val options = listOf(3, 7, 14, 30)

        AlertDialog(
            onDismissRequest = { showCleanupDaysDialog = false },
            title = { Text("Nach wie vielen Tagen aufräumen?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { days ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    autoCleanupDays = days
                                    settingsManager.autoCleanupDays = days
                                    showCleanupDaysDialog = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = autoCleanupDays == days,
                                onClick = {
                                    autoCleanupDays = days
                                    settingsManager.autoCleanupDays = days
                                    showCleanupDaysDialog = false
                                }
                            )
                            Text(
                                text = "$days Tage",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCleanupDaysDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }
}
