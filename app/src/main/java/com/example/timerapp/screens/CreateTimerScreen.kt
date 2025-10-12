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
import androidx.compose.ui.unit.dp
import com.example.timerapp.models.Category
import com.example.timerapp.models.Timer
import com.example.timerapp.viewmodel.TimerViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTimerScreen(
    viewModel: TimerViewModel,
    onNavigateBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()

    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    // HIER IST DIE KORREKTUR: .plusHours(1) wurde entfernt
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Wird abgeholt") }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val germanZone = ZoneId.of("Europe/Berlin")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timer erstellen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@IconButton
                            }

                            val targetDateTime = ZonedDateTime.of(
                                selectedDate,
                                selectedTime,
                                germanZone
                            )

                            val timer = Timer(
                                name = name,
                                target_time = targetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                category = selectedCategory,
                                note = note.ifBlank { null }
                            )

                            viewModel.createTimer(timer)
                            onNavigateBack()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Speichern")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Name") },
                placeholder = { Text("z.B. Max abholen") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Name ist erforderlich") }
                } else null,
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            )

            // Datum
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Datum",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Zeit
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Uhrzeit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    WheelTimePicker(
                        initialTime = selectedTime,
                        onTimeSelected = { selectedTime = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Kategorie
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showCategoryDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kategorie",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Notiz
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notiz (optional)") },
                placeholder = { Text("z.B. Sportplatz Eingang Nord") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                leadingIcon = {
                    Icon(Icons.Default.Notes, contentDescription = null)
                }
            )

            // Zusammenfassung
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Zusammenfassung",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Divider()

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Alarm wird ausgelöst am:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text(
                        text = "${selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} um ${selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))} Uhr",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(germanZone).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(germanZone)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Category Selection Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Kategorie wählen") },
            text = {
                Column {
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == category.name,
                                onClick = {
                                    selectedCategory = category.name
                                    showCategoryDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }
}