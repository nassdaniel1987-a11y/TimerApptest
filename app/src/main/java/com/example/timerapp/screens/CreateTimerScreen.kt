package com.example.timerapp.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timerapp.SettingsManager
import com.example.timerapp.models.Timer
import com.example.timerapp.ui.theme.GradientColors
import com.example.timerapp.viewmodel.TimerViewModel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// 🎨 Weekday Button Composable
@Composable
private fun WeekdayButton(
    dayName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "weekdayScale"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            interactionSource = interactionSource,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        ) {
            Text(
                text = dayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// 🎨 Category Button Composable
@Composable
private fun CategoryButton(
    category: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "categoryScale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTimerScreen(
    viewModel: TimerViewModel,
    onNavigateBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Wird abgeholt") }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
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

    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val pickupTimes = remember { settingsManager.getPickupTimeList() }

    val userZone = ZoneId.systemDefault() // Nutzt Handy-Timezone

    // ✅ Konstanten für Validierung
    val MAX_NAME_LENGTH = 50
    val MAX_NOTE_LENGTH = 200

    // ✅ Error Handling - zeige Fehler als Snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    // ✨ Gradient Background
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Neuer Timer",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
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
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 🎨 Hero Section - Timer Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Großes Timer Icon
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 3.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Preview Text
                        val targetDateTime = ZonedDateTime.of(selectedDate, selectedTime, userZone)
                        val minutesUntil = ChronoUnit.MINUTES.between(ZonedDateTime.now(userZone), targetDateTime)

                        Text(
                            text = if (name.isBlank()) "Timer Preview" else name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (minutesUntil > 0) {
                            Text(
                                text = when {
                                    minutesUntil < 60 -> "in $minutesUntil Minuten"
                                    minutesUntil < 1440 -> "in ${minutesUntil / 60}h ${minutesUntil % 60}min"
                                    else -> "in ${minutesUntil / 1440} Tagen"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ✅ Name Input - Modernisiert mit Icon
            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.length <= MAX_NAME_LENGTH) {
                        name = it
                        nameError = null
                    }
                },
                label = { Text("Wofür?", style = MaterialTheme.typography.titleSmall) },
                placeholder = { Text("z.B. Max abholen") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = {
                    if (nameError != null) {
                        Text(nameError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(
                            "${name.length}/$MAX_NAME_LENGTH Zeichen",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Datum Schnell-Auswahl
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
                        text = "Wann?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Datum-Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val today = LocalDate.now()
                        val dateOptions = listOf(
                            "Heute" to today,
                            "Morgen" to today.plusDays(1)
                        )
                        dateOptions.forEach { (label, date) ->
                            FilterChip(
                                selected = selectedDate == date,
                                onClick = {
                                    selectedDate = date
                                    dateError = null
                                },
                                label = {
                                    Text(
                                        label,
                                        fontWeight = if (selectedDate == date) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (selectedDate == date) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                        FilterChip(
                            selected = selectedDate != LocalDate.now() && selectedDate != LocalDate.now().plusDays(1),
                            onClick = { showDatePicker = true },
                            label = { Text("Anderes Datum...") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    // Uhrzeiten-Chips
                    if (pickupTimes.isNotEmpty()) {
                        Text(
                            text = "Abholzeit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pickupTimes) { timeStr ->
                                val parts = timeStr.split(":")
                                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                val chipTime = LocalTime.of(hour, minute)
                                val isSelected = selectedTime.hour == hour && selectedTime.minute == minute

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTime = chipTime
                                    },
                                    label = {
                                        Text(
                                            timeStr,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ✅ Datum & Zeit - Glasmorphism Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Datum
                Card(
                    modifier = Modifier.weight(1f),
                    onClick = { showDatePicker = true },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Datum",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yy")),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Uhrzeit
                Card(
                    modifier = Modifier.weight(1f),
                    onClick = { showTimePicker = true },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Uhrzeit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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

            // 🎨 Kategorie - Visual Icon Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Kategorie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Visual Category Grid
                    val categoryIcons = mapOf(
                        "Wird abgeholt" to Icons.Default.DirectionsCar,
                        "Arzt" to Icons.Default.LocalHospital,
                        "Einkaufen" to Icons.Default.ShoppingCart,
                        "Meeting" to Icons.Default.Groups,
                        "Sport" to Icons.Default.FitnessCenter,
                        "Kochen" to Icons.Default.Restaurant
                    )

                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(180.dp)
                    ) {
                        categories.forEach { category ->
                            item {
                                CategoryButton(
                                    category = category.name,
                                    icon = categoryIcons[category.name] ?: Icons.Default.Category,
                                    isSelected = selectedCategory == category.name,
                                    onClick = { selectedCategory = category.name }
                                )
                            }
                        }
                    }
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

                        // Wochentags-Buttons - Circular Design
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
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
                                WeekdayButton(
                                    dayName = dayName,
                                    isSelected = selectedWeekdays.contains(dayNumber),
                                    onClick = {
                                        selectedWeekdays = if (selectedWeekdays.contains(dayNumber)) {
                                            selectedWeekdays - dayNumber
                                        } else {
                                            selectedWeekdays + dayNumber
                                        }
                                    }
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

            // ✅ Notiz - Mit Icon und modernem Styling
            OutlinedTextField(
                value = note,
                onValueChange = {
                    if (it.length <= MAX_NOTE_LENGTH) {
                        note = it
                    }
                },
                label = { Text("Notiz (optional)", style = MaterialTheme.typography.titleSmall) },
                placeholder = { Text("z.B. Sportplatz Eingang Nord") },
                leadingIcon = {
                    Icon(
                        Icons.Default.StickyNote2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        "${note.length}/$MAX_NOTE_LENGTH Zeichen",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                minLines = 2,
                maxLines = 4,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }
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
            icon = { Icon(Icons.Default.Repeat, contentDescription = "Wiederholung") },
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

    // ⏰ Material 3 TimePicker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.padding(16.dp)
                )
            }
        )
    }
}
