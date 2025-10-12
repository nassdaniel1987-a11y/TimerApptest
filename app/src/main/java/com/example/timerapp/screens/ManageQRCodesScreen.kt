package com.example.timerapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageQRCodesScreen(
    viewModel: TimerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit // Parameter für die Navigation zur Detailseite
) {
    val qrCodes by viewModel.qrCodes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR-Codes verwalten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "QR-Code erstellen")
            }
        }
    ) { padding ->
        if (qrCodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Noch keine QR-Codes erstellt.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(qrCodes, key = { it.id }) { qrCode ->
                    QRCodeItem(
                        qrCodeData = qrCode,
                        onDelete = { viewModel.deleteQRCode(qrCode.id) },
                        onClick = { onNavigateToDetail(qrCode.id) } // Navigation hier aufrufen
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddQRCodeDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { qrCodeData ->
                viewModel.createQRCode(qrCodeData)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QRCodeItem(
    qrCodeData: QRCodeData,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick, // Die ganze Karte ist klickbar
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(qrCodeData.name, style = MaterialTheme.typography.titleMedium)
                Text("Uhrzeit: ${qrCodeData.time}", style = MaterialTheme.typography.bodySmall)
            }
            // IconButton bleibt klickbar, um das Löschen nicht zu blockieren
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddQRCodeDialog(
    onDismiss: () -> Unit,
    onAdd: (QRCodeData) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("12:00") }
    var category by remember { mutableStateOf("Allgemein") }
    var note by remember { mutableStateOf("") }
    var isFlexible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuen QR-Code erstellen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Uhrzeit (HH:mm)") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Kategorie") })
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Notiz (optional)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFlexible, onCheckedChange = { isFlexible = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Flexibel")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qrCodeData = QRCodeData(
                        name = name,
                        time = time,
                        category = category,
                        note = note.ifBlank { null },
                        isFlexible = isFlexible
                    )
                    onAdd(qrCodeData)
                },
                enabled = name.isNotBlank() && time.matches(Regex("\\d{2}:\\d{2}"))
            ) {
                Text("Erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}