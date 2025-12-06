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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Wird abgeholt") }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showRecurrenceDialog by remember { mutableStateOf(false) }
    var showRecurrenceEndDatePicker by remember { mutableStateOf(false) }

    // ✅ Validierungs-State
    var nameError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var weekdayError by remember { mutableStateOf<String?>(null) }

    // ✅ Wiederholungs-State
    var recurrence by remember { mutableStateOf<String?>(null) }
    var recurrenceEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedWeekdays by remember { mutableStateOf(setOf<Int>()) } // ISO 8601: 1=Mo, 7=So

    val userZone = ZoneId.systemDefault() // Nutzt Handy-Timezone

    // ✅ Konstanten für Validierung
    val MAX_NAME_LENGTH = 50
    val MAX_NOTE_LENGTH = 200

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neuer Timer") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Abbrechen")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // ✅ Validierung: Name
                            nameError = when {
                                name.isBlank() -> "Name darf nicht leer sein"
                                name.length > MAX_NAME_LENGTH -> "Max. $MAX_NAME_LENGTH Zeichen"
                                else -> null
                            }

                            val targetDateTime = ZonedDateTime.of(
                                selectedDate,
                                selectedTime,
                                userZone
                            )

                            // ✅ Validierung: Datum in Vergangenheit
                            dateError = if (targetDateTime.isBefore(ZonedDateTime.now(userZone))) {
                                "Datum muss in der Zukunft liegen"
                            } else null

                            // ✅ Validierung: Custom Weekdays
                            weekdayError = if (recurrence == "custom" && selectedWeekdays.isEmpty()) {
                                "Bitte mindestens einen Wochentag auswählen"
                            } else null

                            // Wenn Fehler existieren, nicht erstellen
                            if (nameError != null || dateError != null || weekdayError != null) {
                                return@TextButton
                            }

                            val timer = Timer(
                                name = name.trim(),
                                target_time = targetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                category = selectedCategory,
                                note = note.trim().ifBlank { null },
                                recurrence = recurrence,
                                recurrence_end_date = recurrenceEndDate?.atStartOfDay(userZone)?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                recurrence_weekdays = if (recurrence == "custom" && selectedWeekdays.isNotEmpty()) {
                                    selectedWeekdays.sorted().joinToString(",")
                                } else null
                            )

                            viewModel.createTimer(timer)
                            onNavigateBack()
                        },
                        enabled = name.isNotBlank() && name.length <= MAX_NAME_LENGTH
                    ) {
                        Text(
                            "Erstellen",
                            style = MaterialTheme.typography.labelLarge
                        )
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
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ✅ Name Input - Minimalistisch
            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.length <= MAX_NAME_LENGTH) {
                        name = it
                        nameError = null
                    }
                },
                label = { Text("Wofür?") },
                placeholder = { Text("z.B. Max abholen") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = {
                    if (nameError != null) {
                        Text(nameError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${name.length}/$MAX_NAME_LENGTH Zeichen")
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            // ✅ Datum & Zeit - Kompakt nebeneinander
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Datum
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    onClick = { showDatePicker = true },
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Datum",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yy")),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Uhrzeit - nur Anzeige
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Uhrzeit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ✅ Zeit-Picker - Kompakter
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                WheelTimePicker(
                    initialTime = selectedTime,
                    onTimeSelected = { selectedTime = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            // ✅ Kategorie - Kompakt
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showCategoryDialog = true },
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Kategorie",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedCategory,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ✅ Wiederholung
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showRecurrenceDialog = true },
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            tint = if (recurrence != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Wiederholung",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when (recurrence) {
                                    "daily" -> "Täglich"
                                    "weekly" -> "Wöchentlich"
                                    "weekdays" -> "Werktags (Mo-Fr)"
                                    "weekends" -> "Wochenende (Sa-So)"
                                    "custom" -> {
                                        if (selectedWeekdays.isEmpty()) {
                                            "Benutzerdefiniert"
                                        } else {
                                            val weekdayNames = mapOf(
                                                1 to "Mo", 2 to "Di", 3 to "Mi", 4 to "Do",
                                                5 to "Fr", 6 to "Sa", 7 to "So"
                                            )
                                            selectedWeekdays.sorted().joinToString(", ") { weekdayNames[it] ?: "" }
                                        }
                                    }
                                    else -> "Einmalig"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (recurrence != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ✅ Wochentags-Auswahl (nur bei custom recurrence)
            if (recurrence == "custom") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Wochentage auswählen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Wochentags-Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val weekdays = listOf(
                                1 to "Mo",
                                2 to "Di",
                                3 to "Mi",
                                4 to "Do",
                                5 to "Fr",
                                6 to "Sa",
                                7 to "So"
                            )

                            weekdays.forEach { (dayNumber, dayName) ->
                                val isSelected = selectedWeekdays.contains(dayNumber)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedWeekdays = if (isSelected) {
                                            selectedWeekdays - dayNumber
                                        } else {
                                            selectedWeekdays + dayNumber
                                        }
                                    },
                                    label = { Text(dayName) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                                    )
                                )
                            }
                        }

                        if (selectedWeekdays.isEmpty()) {
                            Text(
                                text = "Bitte mindestens einen Wochentag auswählen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // ✅ Wiederholungs-Enddatum (nur wenn Wiederholung aktiv)
            if (recurrence != null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showRecurrenceEndDatePicker = true },
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column {
                                Text(
                                    text = "Wiederholung endet am",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = recurrenceEndDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Nie",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // ✅ Notiz - Optional & minimalistisch
            OutlinedTextField(
                value = note,
                onValueChange = {
                    if (it.length <= MAX_NOTE_LENGTH) {
                        note = it
                    }
                },
                label = { Text("Notiz (optional)") },
                placeholder = { Text("z.B. Sportplatz Eingang Nord") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("${note.length}/$MAX_NOTE_LENGTH Zeichen")
                },
                minLines = 2,
                maxLines = 4,
                shape = MaterialTheme.shapes.large
            )
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(userZone).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(userZone)
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { category ->
                        Surface(
                            onClick = {
                                selectedCategory = category.name
                                showCategoryDialog = false
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = selectedCategory == category.name,
                                    onClick = {
                                        selectedCategory = category.name
                                        showCategoryDialog = false
                                    }
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
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

    // ✅ Recurrence Selection Dialog
    if (showRecurrenceDialog) {
        val recurrenceOptions = listOf(
            null to "Einmalig",
            "daily" to "Täglich",
            "weekly" to "Wöchentlich",
            "weekdays" to "Werktags (Mo-Fr)",
            "weekends" to "Wochenende (Sa-So)",
            "custom" to "Benutzerdefiniert..."
        )

        AlertDialog(
            onDismissRequest = { showRecurrenceDialog = false },
            icon = { Icon(Icons.Default.Repeat, contentDescription = null) },
            title = { Text("Wiederholung wählen") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recurrenceOptions.forEach { (value, label) ->
                        Surface(
                            onClick = {
                                recurrence = value
                                if (value == null) {
                                    recurrenceEndDate = null
                                }
                                showRecurrenceDialog = false
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = recurrence == value,
                                    onClick = {
                                        recurrence = value
                                        if (value == null) {
                                            recurrenceEndDate = null
                                        }
                                        showRecurrenceDialog = false
                                    }
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecurrenceDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }

    // ✅ Recurrence End Date Picker
    if (showRecurrenceEndDatePicker) {
        val initialDate = recurrenceEndDate ?: selectedDate.plusMonths(1)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(userZone).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showRecurrenceEndDatePicker = false },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        recurrenceEndDate = null
                        showRecurrenceEndDatePicker = false
                    }) {
                        Text("Nie")
                    }
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                recurrenceEndDate = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(userZone)
                                    .toLocalDate()
                            }
                            showRecurrenceEndDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecurrenceEndDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
