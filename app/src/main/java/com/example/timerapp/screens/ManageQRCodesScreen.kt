package com.example.timerapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.models.toQRString
import com.example.timerapp.utils.QRCodeGenerator
import com.example.timerapp.utils.ShareHelper
import com.example.timerapp.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageQRCodesScreen(
    viewModel: TimerViewModel,
    onNavigateBack: () -> Unit
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
                items(qrCodes) { qrCode ->
                    QRCodeItem(
                        qrCodeData = qrCode,
                        onDelete = { viewModel.deleteQRCode(qrCode.id) }
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

@Composable
private fun QRCodeItem(
    qrCodeData: QRCodeData,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(qrCodeData.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Uhrzeit: ${qrCodeData.time}", style = MaterialTheme.typography.bodySmall)
            Text("Kategorie: ${qrCodeData.category}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val qrBitmap = QRCodeGenerator.generateQRCode(qrCodeData.toQRString())
                    if (qrBitmap != null) {
                        val shareText = "Timer QR-Code für: ${qrCodeData.name} (${qrCodeData.time})"
                        ShareHelper.shareQRCodeImage(context, qrBitmap, shareText)
                    }
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Als Bild teilen")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                }
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