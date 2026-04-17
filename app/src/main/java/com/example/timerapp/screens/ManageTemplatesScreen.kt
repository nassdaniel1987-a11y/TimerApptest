package com.example.timerapp.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.timerapp.models.TimerTemplate
import com.example.timerapp.ui.theme.GlassColors
import com.example.timerapp.ui.components.MeshGradientBackground
import com.example.timerapp.viewmodel.TimerViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTemplatesScreen(
    viewModel: TimerViewModel,
    onNavigateBack: () -> Unit
) {
    val templates by viewModel.templates.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }

    // ── Auswahl-Modus ──────────────────────────────────────────────────────
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showStartDialog by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    val glassColor = if (isSystemInDarkTheme()) GlassColors.GlassSurfaceDark else GlassColors.GlassSurfaceLight

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        MeshGradientBackground()
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        if (selectedIds.isEmpty()) {
                            Text(
                                "Vorlagen",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "${selectedIds.size} ausgewählt",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        if (selectedIds.isNotEmpty()) {
                            // Im Auswahl-Modus: Auswahl aufheben
                            IconButton(onClick = { selectedIds = emptySet() }) {
                                Icon(Icons.Default.Close, contentDescription = "Auswahl aufheben")
                            }
                        } else {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                            }
                        }
                    },
                    actions = {
                        // "Alle auswählen" nur wenn Vorlagen vorhanden und noch nicht alle selektiert
                        if (templates.isNotEmpty() && selectedIds.size != templates.size) {
                            TextButton(onClick = { selectedIds = templates.map { it.id }.toSet() }) {
                                Text("Alle")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            },
            floatingActionButton = {
                // FAB nur anzeigen wenn kein Timer-Start-Button sichtbar
                AnimatedVisibility(
                    visible = selectedIds.isEmpty(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.5f),
                                shape = CircleShape
                            ),
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Vorlage hinzufügen",
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (templates.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.PlaylistAdd,
                        title = "Keine Vorlagen vorhanden",
                        subtitle = "Erstelle Vorlagen für häufig verwendete Timer.\nDamit kannst du schnell mehrere Timer auf einmal starten.",
                        ctaText = "Vorlage erstellen",
                        onCtaClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            // Extra Platz unten, damit der Bottom-Button nichts verdeckt
                            bottom = if (selectedIds.isNotEmpty()) 100.dp else 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hinweis-Banner wenn keine Auswahl
                        if (selectedIds.isEmpty() && templates.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.TouchApp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Tippe auf Vorlagen, um sie zu markieren – dann alle auf einmal starten.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        items(templates, key = { it.id }) { template ->
                            val isSelected = template.id in selectedIds
                            TemplateItem(
                                template = template,
                                glassColor = glassColor,
                                categories = categories.map { it.name },
                                isSelected = isSelected,
                                onToggleSelect = {
                                    selectedIds = if (isSelected) {
                                        selectedIds - template.id
                                    } else {
                                        selectedIds + template.id
                                    }
                                },
                                onDelete = { viewModel.deleteTemplate(template.id) },
                                onEdit = { editedTemplate -> viewModel.updateTemplate(editedTemplate) }
                            )
                        }
                    }
                }

                // ── Animierter "Timer starten"-Button am unteren Rand ──────
                AnimatedVisibility(
                    visible = selectedIds.isNotEmpty(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ) {
                        Button(
                            onClick = { showStartDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    ),
                                    shape = MaterialTheme.shapes.extraLarge
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (selectedIds.size == 1) "1 Timer starten"
                                       else "${selectedIds.size} Timer starten",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Neue Vorlage ───────────────────────────────────────────────
    if (showAddDialog) {
        TemplateDialog(
            title = "Neue Vorlage",
            confirmText = "Hinzufügen",
            categories = categories.map { it.name },
            onDismiss = { showAddDialog = false },
            onConfirm = { name, time, category, note ->
                val template = TimerTemplate(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    default_time = time,
                    category = category,
                    note = note.ifBlank { null }
                )
                viewModel.createTemplate(template)
                showAddDialog = false
            }
        )
    }

    // ── Dialog: Timer starten (Datum wählen) ──────────────────────────────
    if (showStartDialog) {
        val selectedTemplates = templates.filter { it.id in selectedIds }
        StartTimersDialog(
            templateCount = selectedTemplates.size,
            templateNames = selectedTemplates.map { it.name },
            glassColor = glassColor,
            onDismiss = { showStartDialog = false },
            onConfirm = { targetDate ->
                viewModel.createTimersFromTemplates(
                    templates = selectedTemplates,
                    targetDate = targetDate
                )
                showStartDialog = false
                selectedIds = emptySet()
            }
        )
    }
}

// ── TemplateItem ──────────────────────────────────────────────────────────────

@Composable
private fun TemplateItem(
    template: TimerTemplate,
    glassColor: Color,
    categories: List<String>,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (TimerTemplate) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "templateBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.extraLarge
            )
            .clickable(onClick = onToggleSelect),
        colors = CardDefaults.cardColors(
            containerColor = glassColor
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Auswahl-Indikator (Checkbox-Kreis) links
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Uhrzeit-Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = template.default_time,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = template.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (template.note != null) {
                        Text(
                            text = template.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Bearbeiten",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Vorlage löschen?") },
            text = { Text("Möchtest du die Vorlage '${template.name}' wirklich löschen?") },
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

    if (showEditDialog) {
        TemplateDialog(
            title = "Vorlage bearbeiten",
            confirmText = "Speichern",
            categories = categories,
            initialName = template.name,
            initialTime = template.default_time,
            initialCategory = template.category,
            initialNote = template.note ?: "",
            onDismiss = { showEditDialog = false },
            onConfirm = { name, time, category, note ->
                onEdit(template.copy(
                    name = name,
                    default_time = time,
                    category = category,
                    note = note.ifBlank { null }
                ))
                showEditDialog = false
            }
        )
    }
}

// ── StartTimersDialog ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartTimersDialog(
    templateCount: Int,
    templateNames: List<String>,
    glassColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    if (templateCount == 1) "1 Timer starten"
                    else "$templateCount Timer starten"
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Vorschau der ausgewählten Vorlagen
                Card(
                    colors = CardDefaults.cardColors(containerColor = glassColor),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Ausgewählte Vorlagen:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        templateNames.take(5).forEach { name ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(6.dp)
                                )
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (templateNames.size > 5) {
                            Text(
                                "… und ${templateNames.size - 5} weitere",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Datum-Auswahl
                Text(
                    "Für welchen Tag?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDate == today,
                        onClick = { selectedDate = today },
                        label = {
                            Text(
                                "Heute",
                                fontWeight = if (selectedDate == today) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selectedDate == today) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedDate == tomorrow,
                        onClick = { selectedDate = tomorrow },
                        label = {
                            Text(
                                "Morgen",
                                fontWeight = if (selectedDate == tomorrow) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selectedDate == tomorrow) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Anderes Datum
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (selectedDate != today && selectedDate != tomorrow) {
                            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy").format(selectedDate)
                        } else {
                            "Anderes Datum…"
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedDate) }) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Starten")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )

    // DatePicker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ── TemplateDialog ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDialog(
    title: String,
    confirmText: String,
    categories: List<String>,
    initialName: String = "",
    initialTime: String = "14:00",
    initialCategory: String = categories.firstOrNull() ?: "Wird abgeholt",
    initialNote: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, time: String, category: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedTime by remember {
        mutableStateOf(
            try {
                val parts = initialTime.split(":")
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            } catch (_: Exception) { LocalTime.of(14, 0) }
        )
    }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var note by remember { mutableStateOf(initialNote) }
    var expandedCategory by remember { mutableStateOf(false) }

    val glassColor = if (isSystemInDarkTheme()) GlassColors.GlassSurfaceDark else GlassColors.GlassSurfaceLight

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    if (initialName.isEmpty()) Icons.Default.PlaylistAdd else Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(title)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z.B. Max Müller") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                // Uhrzeit
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = glassColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Standardzeit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        WheelTimePicker(
                            initialTime = selectedTime,
                            onTimeSelected = { selectedTime = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Kategorie-Auswahl
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategorie") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = MaterialTheme.shapes.large
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 200) note = it },
                    label = { Text("Notiz (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = MaterialTheme.shapes.large
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val timeStr = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute)
                    onConfirm(name.trim(), timeStr, selectedCategory, note.trim())
                },
                enabled = name.isNotBlank()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}


