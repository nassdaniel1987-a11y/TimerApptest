package com.example.timerapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timerapp.BuildConfig
import com.example.timerapp.SettingsManager

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
                        contentDescription = "Timer-App Logo",
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Navigation Items
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") },
                selected = currentRoute.contains("Home"),
                onClick = {
                    onNavigateToHome()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "QR-Code scannen") },
                label = { Text("QR-Code scannen") },
                selected = currentRoute.contains("QRScanner"),
                onClick = {
                    onNavigateToQRScanner()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Category, contentDescription = "Kategorien") },
                label = { Text("Kategorien") },
                selected = currentRoute.contains("Categories"),
                onClick = {
                    onNavigateToCategories()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.QrCode, contentDescription = "QR-Codes verwalten") },
                label = { Text("QR-Codes verwalten") },
                selected = currentRoute.contains("ManageQRCodes"),
                onClick = {
                    onNavigateToManageQRCodes() // Und hier wird er verwendet
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Pause-Modus Toggle
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager.getInstance(context) }
            var isAppPaused by remember { mutableStateOf(settingsManager.isAppPaused) }

            NavigationDrawerItem(
                icon = {
                    Icon(
                        if (isAppPaused) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                        contentDescription = "Alarme pausieren",
                        tint = if (isAppPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        if (isAppPaused) "Alarme pausiert" else "Alarme aktiv",
                        fontWeight = if (isAppPaused) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAppPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                },
                badge = {
                    Switch(
                        checked = !isAppPaused,
                        onCheckedChange = {
                            isAppPaused = !it
                            settingsManager.isAppPaused = !it
                        },
                        modifier = Modifier.height(24.dp)
                    )
                },
                selected = false,
                onClick = {
                    isAppPaused = !isAppPaused
                    settingsManager.isAppPaused = isAppPaused
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Einstellungen") },
                label = { Text("Einstellungen") },
                selected = currentRoute.contains("SettingsRoute"),
                onClick = {
                    onNavigateToSettings()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Help, contentDescription = "Hilfe") },
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
                        text = "Version ${BuildConfig.VERSION_NAME}",
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