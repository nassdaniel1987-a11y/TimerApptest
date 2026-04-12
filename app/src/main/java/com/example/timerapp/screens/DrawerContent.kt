package com.example.timerapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timerapp.AppDesignTheme
import com.example.timerapp.BuildConfig
import com.example.timerapp.SettingsManager
import com.example.timerapp.ui.theme.DesignTokens
import com.example.timerapp.ui.theme.GlassColors
import com.example.timerapp.ui.theme.GradientColors
import com.example.timerapp.ui.theme.LocalAppDesignTheme
import com.example.timerapp.ui.theme.ManropeFontFamily
import com.example.timerapp.ui.components.neumorphColorsLight
import com.example.timerapp.ui.components.neumorphColorsDark
import com.example.timerapp.ui.components.NeumorphColors
import com.example.timerapp.ui.theme.BrutalistColors

@Composable
fun DrawerContent(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToQRScanner: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToManageQRCodes: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val designTheme = LocalAppDesignTheme.current
    val isNeumorphism = designTheme == AppDesignTheme.NEUMORPHISM
    val isBrutalist = designTheme == AppDesignTheme.BRUTALIST
    val nmColors = if (isDark) neumorphColorsDark() else neumorphColorsLight()
    val drawerBg = when {
        isBrutalist   -> BrutalistColors.Background
        isNeumorphism -> nmColors.bg
        isDark        -> DesignTokens.SurfaceContainerLow
        else          -> Color(0xFFF8FAFC)
    }

    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var isAppPaused by remember { mutableStateOf(settingsManager.isAppPaused) }

    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        drawerContainerColor = drawerBg,
        drawerTonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 0.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            isBrutalist   -> Brush.linearGradient(
                                listOf(BrutalistColors.Surface, BrutalistColors.Background)
                            )
                            isNeumorphism -> Brush.linearGradient(listOf(nmColors.accent.copy(alpha = 0.10f), nmColors.bg))
                            else          -> Brush.linearGradient(
                                listOf(
                                    DesignTokens.IndigoAccent.copy(alpha = 0.15f),
                                    DesignTokens.VioletAccent.copy(alpha = 0.08f)
                                )
                            )
                        }
                    )
                    .then(
                        if (isBrutalist)
                            Modifier.border(
                                width = 0.dp, // no top/side border needed; bottom border shown via Box below
                                color = Color.Transparent
                            )
                        else Modifier
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // App icon — square for Brutalist, circle otherwise
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .then(
                                if (isBrutalist)
                                    Modifier
                                        .background(BrutalistColors.Cyan, RoundedCornerShape(4.dp))
                                        .border(1.dp, BrutalistColors.CyanDim, RoundedCornerShape(4.dp))
                                else
                                    Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isNeumorphism)
                                                Brush.linearGradient(listOf(nmColors.accent, nmColors.accentSuccess))
                                            else
                                                Brush.linearGradient(GradientColors.PrimaryButton)
                                        )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isBrutalist) BrutalistColors.Background else Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isBrutalist) "TIMERAPP" else "TimerApp",
                            fontFamily = if (isBrutalist) androidx.compose.ui.text.font.FontFamily.Monospace else ManropeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = if (isBrutalist) 3.sp else 0.sp,
                            color = if (isBrutalist) BrutalistColors.TextPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBrutalist) "ABHOLZEITEN_MGR" else "Abholzeiten Manager",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = if (isBrutalist) androidx.compose.ui.text.font.FontFamily.Monospace else null,
                            letterSpacing = if (isBrutalist) 1.sp else 0.sp,
                            color = if (isBrutalist) BrutalistColors.TextSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )

                        // Online status
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .then(
                                        if (isBrutalist) Modifier.background(BrutalistColors.Cyan)
                                        else Modifier.clip(CircleShape).background(DesignTokens.StatusOnline)
                                    )
                            )
                            Text(
                                text = if (isBrutalist) "SYNC_OK" else "Synchronisiert",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = if (isBrutalist) androidx.compose.ui.text.font.FontFamily.Monospace else null,
                                color = if (isBrutalist) BrutalistColors.Cyan else DesignTokens.StatusOnline,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = if (isBrutalist) 1.sp else 0.sp,
                            )
                        }
                    }
                }
            }

            // Bottom border line for Brutalist header
            if (isBrutalist) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BrutalistColors.Border)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Navigation Items ─────────────────────────────────────────────
            val nm = if (isNeumorphism) nmColors else null
            DrawerNavItem(
                icon = Icons.Default.Home,
                label = "Meine Timer",
                isSelected = currentRoute.contains("Home"),
                onClick = { onNavigateToHome(); onCloseDrawer() },
                nmColors = nm,
                isBrutalist = isBrutalist,
            )
            DrawerNavItem(
                icon = Icons.Default.Category,
                label = "Kategorien",
                isSelected = currentRoute.contains("Categories"),
                onClick = { onNavigateToCategories(); onCloseDrawer() },
                nmColors = nm,
                isBrutalist = isBrutalist,
            )
            DrawerNavItem(
                icon = Icons.Default.PlaylistAdd,
                label = "Vorlagen",
                isSelected = currentRoute.contains("ManageTemplates"),
                onClick = { onNavigateToTemplates(); onCloseDrawer() },
                nmColors = nm,
                isBrutalist = isBrutalist,
            )
            DrawerNavItem(
                icon = Icons.Default.QrCodeScanner,
                label = "QR-Code scannen",
                isSelected = currentRoute.contains("QRScanner"),
                onClick = { onNavigateToQRScanner(); onCloseDrawer() },
                nmColors = nm,
                isBrutalist = isBrutalist,
            )
            DrawerNavItem(
                icon = Icons.Default.QrCode,
                label = "QR-Codes verwalten",
                isSelected = currentRoute.contains("ManageQRCodes"),
                onClick = { onNavigateToManageQRCodes(); onCloseDrawer() },
                nmColors = nm,
                isBrutalist = isBrutalist,
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Pause toggle
            NavigationDrawerItem(
                icon = {
                    Icon(
                        if (isAppPaused) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (isAppPaused) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = if (isAppPaused) "Alarme pausiert" else "Alarme aktiv",
                        fontWeight = if (isAppPaused) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAppPaused) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                },
                badge = {
                    Switch(
                        checked = !isAppPaused,
                        onCheckedChange = {
                            isAppPaused = !it
                            settingsManager.isAppPaused = !it
                        },
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = DesignTokens.PrimaryDim
                        )
                    )
                },
                selected = false,
                onClick = {
                    isAppPaused = !isAppPaused
                    settingsManager.isAppPaused = isAppPaused
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            DrawerNavItem(
                icon = Icons.Default.Settings,
                label = "Einstellungen",
                isSelected = currentRoute.contains("SettingsRoute"),
                onClick = { onNavigateToSettings(); onCloseDrawer() },
                nmColors = nm,
                isBrutalist = isBrutalist,
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Footer ───────────────────────────────────────────────────────
            if (isBrutalist) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BrutalistColors.Border)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "TIMERAPP  ·  v${BuildConfig.VERSION_NAME}".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = if (isBrutalist) androidx.compose.ui.text.font.FontFamily.Monospace else null,
                        letterSpacing = 1.5.sp,
                        color = if (isBrutalist) BrutalistColors.TextSecondary.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                    Text(
                        text = java.time.ZoneId.systemDefault().id,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = if (isBrutalist) androidx.compose.ui.text.font.FontFamily.Monospace else null,
                        color = if (isBrutalist) BrutalistColors.TextSecondary.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    nmColors: NeumorphColors? = null,
    isBrutalist: Boolean = false,
) {
    val isNeumorphism = nmColors != null
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = {
            Text(
                text = if (isBrutalist) label.uppercase() else label,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = if (isBrutalist) androidx.compose.ui.text.font.FontFamily.Monospace else null,
                letterSpacing = if (isBrutalist) 1.sp else 0.sp,
                fontSize = if (isBrutalist) 11.sp else 14.sp,
            )
        },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = when {
                isBrutalist   -> BrutalistColors.Cyan.copy(alpha = 0.12f)
                isNeumorphism -> nmColors!!.accent.copy(alpha = 0.15f)
                else          -> DesignTokens.IndigoAccent.copy(alpha = 0.15f)
            },
            selectedIconColor = when {
                isBrutalist   -> BrutalistColors.Cyan
                isNeumorphism -> nmColors!!.accent
                else          -> DesignTokens.IndigoAccent
            },
            selectedTextColor = when {
                isBrutalist   -> BrutalistColors.Cyan
                isNeumorphism -> nmColors!!.accent
                else          -> DesignTokens.IndigoAccent
            },
            unselectedIconColor = when {
                isBrutalist   -> BrutalistColors.TextSecondary
                isNeumorphism -> nmColors!!.textSecondary
                else          -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            unselectedTextColor = when {
                isBrutalist   -> BrutalistColors.TextSecondary
                isNeumorphism -> nmColors!!.textPrimary
                else          -> MaterialTheme.colorScheme.onSurface
            },
        ),
        shape = if (isBrutalist) RoundedCornerShape(2.dp) else RoundedCornerShape(50)
    )
}
