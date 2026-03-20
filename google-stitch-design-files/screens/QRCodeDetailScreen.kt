package com.example.timerapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.timerapp.models.toQRString
import com.example.timerapp.utils.QRCodeGenerator
import com.example.timerapp.utils.ShareHelper // <-- IMPORT HINZUGEFÜGT
import com.example.timerapp.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeDetailScreen(
    qrCodeId: String,
    viewModel: TimerViewModel,
    onNavigateBack: () -> Unit
) {
    val qrCodes by viewModel.qrCodes.collectAsState()
    val qrCodeData = remember(qrCodes, qrCodeId) {
        qrCodes.find { it.id == qrCodeId }
    }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(qrCodeData?.name ?: "QR-Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        if (qrCodeData == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("QR-Code nicht gefunden.")
            }
        } else {
            val qrBitmap = remember(qrCodeData) {
                QRCodeGenerator.generateQRCode(qrCodeData.toQRString(), size = 1024)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Scannen, um Timer zu erstellen:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "${qrCodeData.name} | ${qrCodeData.time} | ${qrCodeData.category}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR-Code",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                    )
                } else {
                    Text("Fehler bei der QR-Code-Generierung.")
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        if (qrBitmap != null) {
                            val shareText = "Timer QR-Code für: ${qrCodeData.name} (${qrCodeData.time})"
                            ShareHelper.shareQRCodeImage(context, qrBitmap, shareText)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Teilen")
                    }
                    OutlinedButton(onClick = {
                        if (qrBitmap != null) {
                            ShareHelper.saveImageToGallery(context, qrBitmap, "timer_qr_${qrCodeData.name}")
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download")
                    }
                }
            }
        }
    }
}