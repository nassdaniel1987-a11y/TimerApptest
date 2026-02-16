package com.example.timerapp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.timerapp.navigation.*
import com.example.timerapp.screens.*
import com.example.timerapp.screens.home.HomeScreen
import androidx.navigation.toRoute
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
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.example.timerapp.repository.TimerRepository
import com.example.timerapp.utils.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notification Channel erstellen
        NotificationHelper.createNotificationChannel(this)

        // ✅ Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // ✅ KRITISCH: Request Exact Alarm Permission (Android 12+)
        requestExactAlarmPermission()

        // ✅ KRITISCH: Request Fullscreen Intent Permission (Android 14+)
        requestFullscreenIntentPermission()

        // ✅ WICHTIG: Prüfe und deaktiviere Batterie-Optimierung
        checkBatteryOptimization()

        // ✅ Starte tägliche Erinnerungen (WorkManager)
        scheduleDailyReminder()

        // ✅ KRITISCH: Synchronisiere alle Alarme beim App-Start
        // Verhindert dass alte/gelöschte Alarme ausgelöst werden
        synchronizeAlarmsOnStartup()

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

    // ✅ KRITISCH: Fordere Exact Alarm Permission an (Android 12+)
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("MainActivity", "⚠️ Exact Alarm Permission fehlt - fordere an")
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ Fehler beim Anfordern der Exact Alarm Permission: ${e.message}")
                }
            } else {
                Log.d("MainActivity", "✅ Exact Alarm Permission vorhanden")
            }
        }
    }

    // ✅ KRITISCH: Fordere Fullscreen Intent Permission an (Android 14+)
    private fun requestFullscreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34 (Android 14)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.canUseFullScreenIntent()) {
                Log.w("MainActivity", "⚠️ Fullscreen Intent Permission fehlt - fordere an")
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ Fehler beim Anfordern der Fullscreen Intent Permission: ${e.message}")
                }
            } else {
                Log.d("MainActivity", "✅ Fullscreen Intent Permission vorhanden")
            }
        }
    }

    // ✅ WICHTIG: Prüfe Batterie-Optimierung und bitte Benutzer sie zu deaktivieren
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.w("MainActivity", "⚠️ Batterie-Optimierung ist aktiv - kann Alarme blockieren")
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ Fehler beim Anfordern der Batterie-Optimierungs-Ausnahme: ${e.message}")
                }
            } else {
                Log.d("MainActivity", "✅ Batterie-Optimierung deaktiviert")
            }
        }
    }

    // ✅ KRITISCH: Synchronisiere alle Alarme beim App-Start
    // Verhindert dass gelöschte Timer Alarme auslösen
    private fun synchronizeAlarmsOnStartup() {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "🔄 Starte Alarm-Synchronisierung...")

                timerRepository.refreshTimers()

                val activeTimers = timerRepository.timers.value.filter { !it.is_completed }

                alarmScheduler.rescheduleAllAlarms(activeTimers)

                Log.d("MainActivity", "✅ Alarm-Synchronisierung abgeschlossen: ${activeTimers.size} aktive Timer")
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Fehler bei Alarm-Synchronisierung: ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: TimerViewModel = hiltViewModel(),
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentRoute = currentRoute,
                onNavigateToHome = {
                    navController.navigate(Home) {
                        popUpTo<Home> { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute)
                },
                onNavigateToQRScanner = {
                    navController.navigate(QRScanner)
                },
                onNavigateToCategories = {
                    navController.navigate(Categories)
                },
                onNavigateToManageQRCodes = {
                    navController.navigate(ManageQRCodes)
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Home
        ) {
            composable<Home> {
                HomeScreen(
                    viewModel = viewModel,
                    onCreateTimer = {
                        navController.navigate(CreateTimer)
                    },
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    }
                )
            }

            composable<CreateTimer> {
                CreateTimerScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onDarkModeChange = onDarkModeChange
                )
            }

            composable<QRScanner> {
                QRScannerScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<Categories> {
                ManageCategoriesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<ManageQRCodes> {
                ManageQRCodesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToDetail = { qrCodeId ->
                        navController.navigate(QRCodeDetail(qrCodeId))
                    },
                    onNavigateToCreate = {
                        navController.navigate(CreateQRCode)
                    }
                )
            }

            composable<CreateQRCode> {
                CreateQRCodeScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<QRCodeDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<QRCodeDetail>()
                QRCodeDetailScreen(
                    qrCodeId = route.qrCodeId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}