package com.example.timerapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.timerapp.screens.*
import com.example.timerapp.ui.theme.TimerAppTheme
import com.example.timerapp.utils.NotificationHelper
import com.example.timerapp.viewmodel.TimerViewModel
import com.example.timerapp.workers.DailyReminderWorker
import androidx.work.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notification Channel erstellen
        NotificationHelper.createNotificationChannel(this)

        // Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // ✅ Starte tägliche Erinnerungen (WorkManager)
        scheduleDailyReminder()

        setContent {
            val settingsManager = remember { SettingsManager.getInstance(this) }
            var isDarkMode by remember { mutableStateOf(settingsManager.isDarkModeEnabled) }

            TimerAppTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        onDarkModeChange = { enabled ->
                            isDarkMode = enabled
                        }
                    )
                }
            }
        }
    }

    // ✅ Plant tägliche Erinnerung um 20:00 Uhr
    private fun scheduleDailyReminder() {
        val currentTime = LocalDateTime.now()
        val targetTime = LocalDateTime.of(currentTime.toLocalDate(), LocalTime.of(20, 0))

        // Berechne Verzögerung bis 20:00 Uhr
        val initialDelay = if (currentTime.isAfter(targetTime)) {
            // Wenn es nach 20:00 Uhr ist, plane für morgen
            Duration.between(currentTime, targetTime.plusDays(1))
        } else {
            // Sonst für heute
            Duration.between(currentTime, targetTime)
        }

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Behalte existierende Arbeit
            dailyWorkRequest
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: TimerViewModel = viewModel(),
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentRoute = currentRoute,
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToQRScanner = {
                    navController.navigate("qr_scanner")
                },
                onNavigateToCategories = {
                    navController.navigate("categories")
                },
                onNavigateToManageQRCodes = {
                    navController.navigate("manage_qr_codes")
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onCreateTimer = {
                        navController.navigate("create_timer")
                    },
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    }
                )
            }

            composable("create_timer") {
                CreateTimerScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onDarkModeChange = onDarkModeChange
                )
            }

            composable("qr_scanner") {
                QRScannerScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("categories") {
                ManageCategoriesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("manage_qr_codes") {
                ManageQRCodesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToDetail = { qrCodeId ->
                        navController.navigate("qr_code_detail/$qrCodeId")
                    },
                    onNavigateToCreate = {
                        navController.navigate("create_qr_code")
                    }
                )
            }

            composable("create_qr_code") {
                CreateQRCodeScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "qr_code_detail/{qrCodeId}",
                arguments = listOf(navArgument("qrCodeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val qrCodeId = backStackEntry.arguments?.getString("qrCodeId")
                if (qrCodeId != null) {
                    QRCodeDetailScreen(
                        qrCodeId = qrCodeId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}