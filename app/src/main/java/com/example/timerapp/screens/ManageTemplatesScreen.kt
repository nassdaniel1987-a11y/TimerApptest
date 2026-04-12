package com.example.timerapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timerapp.models.TimerTemplate
import com.example.timerapp.ui.theme.GlassColors
import com.example.timerapp.ui.components.MeshGradientBackground
import com.example.timerapp.viewmodel.TimerViewModel
import com.example.timerapp.screens.WheelTimePicker
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
                        Text(
                            "Vorlagen",
                            fontFamily = com.example.timerapp.ui.theme.ManropeFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Zurück",
                                tint = com.example.timerapp.ui.theme.DesignTokens.IndigoAccent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            },
            floatingActionButton = {
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
        ) { padding ->
            if (templates.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PlaylistAdd,
                    title = "Keine Vorlagen vorhanden",
                    subtitle = "Erstelle Vorlagen für häufig verwendete Timer.\nDamit kannst du im CreateTimerScreen schnell einen Timer aus einer Vorlage erstellen.",
                    ctaText = "Vorlage erstellen",
                    onCtaClick = { showAddDialog = true },
                    modifier = Modifier.padding(padding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(templates) { template ->
                        TemplateItem(
                            template = template,
                            glassColor = glassColor,
                            categories = categories.map { it.name },
                            onDelete = { viewModel.deleteTemplate(template.id) },
                            onEdit = { editedTemplate -> viewModel.updateTemplate(editedTemplate) }
                        )
                    }
                }
            }
        }
    }

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
}

@Composable
private fun TemplateItem(
    template: TimerTemplate,
    glassColor: Color,
    categories: List<String>,
    onDelete: () -> Unit,
    onEdit: (TimerTemplate) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = glassColor
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
