package com.example.timerapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DrawerContent(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToQRScanner: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToManageQRCodes: () -> Unit, // <-- HIER IST DER FEHLENDE PARAMETER
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Timer-App",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Abholzeiten Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Navigation Items
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Home") },
                selected = currentRoute == "home",
                onClick = {
                    onNavigateToHome()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                label = { Text("QR-Code scannen") },
                selected = currentRoute == "qr_scanner",
                onClick = {
                    onNavigateToQRScanner()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Category, contentDescription = null) },
                label = { Text("Kategorien") },
                selected = currentRoute == "categories",
                onClick = {
                    onNavigateToCategories()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                label = { Text("QR-Codes verwalten") },
                selected = currentRoute == "manage_qr_codes",
                onClick = {
                    onNavigateToManageQRCodes() // Und hier wird er verwendet
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Einstellungen") },
                selected = currentRoute == "settings",
                onClick = {
                    onNavigateToSettings()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Help, contentDescription = null) },
                label = { Text("Hilfe") },
                selected = false,
                onClick = { onCloseDrawer() },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Zeitzone: ${java.time.ZoneId.systemDefault().id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}