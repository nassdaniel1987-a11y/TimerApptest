package com.example.timerapp.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import com.example.timerapp.ui.theme.GradientColors
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.timerapp.SettingsManager
import com.example.timerapp.models.Timer
import com.example.timerapp.viewmodel.TimerViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: TimerViewModel,
    onCreateTimer: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val timers by viewModel.timers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var hasExactAlarmPermission by remember { mutableStateOf(true) }
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf(SortType.DATE) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // ✨ SearchBar State
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // ✨ SegmentedButton State für Zeitfilter
    var timeFilter by remember { mutableStateOf(TimeFilter.ALL) }

    // ✅ Zeige Error als Snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    // ✅ Filtern, Suchen und Sortieren
    val filteredTimers = remember(timers, filterCategory, sortBy, searchQuery, timeFilter) {
        var filtered = timers.filter { !it.is_completed }

        // ✨ Suchfilter
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                (it.note?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        // ✨ Zeitfilter
        when (timeFilter) {
            TimeFilter.TODAY -> {
                val today = LocalDate.now()
                filtered = filtered.filter {
                    try {
                        val targetDate = ZonedDateTime.parse(it.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate()
                        targetDate == today
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            TimeFilter.TOMORROW -> {
                val tomorrow = LocalDate.now().plusDays(1)
                filtered = filtered.filter {
                    try {
                        val targetDate = ZonedDateTime.parse(it.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate()
                        targetDate == tomorrow
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            TimeFilter.THIS_WEEK -> {
                val today = LocalDate.now()
                val endOfWeek = today.plusDays(7)
                filtered = filtered.filter {
                    try {
                        val targetDate = ZonedDateTime.parse(it.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate()
                        targetDate >= today && targetDate <= endOfWeek
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            TimeFilter.ALL -> { /* Keine Filterung */ }
        }

        if (filterCategory != null) {
            filtered = filtered.filter { it.category == filterCategory }
        }

        when (sortBy) {
            SortType.DATE -> filtered.sortedBy {
                try {
                    ZonedDateTime.parse(it.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                } catch (e: Exception) {
                    ZonedDateTime.now().plusYears(100)
                }
            }
            SortType.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortType.CATEGORY -> filtered.sortedBy { it.category }
        }
    }

    val completedTimers = timers.filter { it.is_completed }

    fun checkPermission() {
        hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else { true }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!hasExactAlarmPermission) {
        ExactAlarmPermissionRationaleDialog(onGoToSettings = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also { context.startActivity(it) }
            }
        })
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    // ✨ Gradient Background
    val backgroundGradient = Brush.verticalGradient(
        colors = if (isSystemInDarkTheme()) {
            GradientColors.BackgroundDark
        } else {
            GradientColors.BackgroundLight
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent, // Transparent für Gradient
            topBar = {
            LargeTopAppBar(
                title = { Text("Timer") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü öffnen")
                    }
                },
                actions = {
                    // Sortier-Button
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            if (filterCategory != null) Icons.Default.FilterAltOff else Icons.Default.FilterAlt,
                            contentDescription = if (filterCategory != null) "Filter entfernen" else "Filter und Sortierung"
                        )
                    }
                    IconButton(onClick = { viewModel.sync() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Timer aktualisieren")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            // ✨ Animierte FAB mit Gradient
            val fabScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "fabScale"
            )

            FloatingActionButton(
                onClick = onCreateTimer,
                modifier = Modifier.scale(fabScale),
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Timer erstellen",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isLoading),
            onRefresh = { viewModel.sync() },
            modifier = Modifier.padding(padding)
        ) {
            if (timers.isEmpty()) {
                EmptyState(onCreateTimer = onCreateTimer)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ✨ SearchBar
                    item {
                        androidx.compose.material3.SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { isSearchActive = false },
                            active = isSearchActive,
                            onActiveChange = { isSearchActive = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Timer durchsuchen...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Suchen"
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Löschen"
                                        )
                                    }
                                }
                            }
                        ) {
                            // Suchvorschläge
                            if (searchQuery.isNotBlank()) {
                                val suggestions = timers
                                    .filter { !it.is_completed }
                                    .filter {
                                        it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.category.contains(searchQuery, ignoreCase = true)
                                    }
                                    .take(5)

                                Column(modifier = Modifier.padding(16.dp)) {
                                    if (suggestions.isEmpty()) {
                                        Text(
                                            "Keine Timer gefunden",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        suggestions.forEach { timer ->
                                            androidx.compose.material3.ListItem(
                                                headlineContent = { Text(timer.name) },
                                                supportingContent = { Text(timer.category) },
                                                leadingContent = {
                                                    Icon(
                                                        Icons.Default.Timer,
                                                        contentDescription = null
                                                    )
                                                },
                                                modifier = Modifier.clickable {
                                                    searchQuery = timer.name
                                                    isSearchActive = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ✨ SegmentedButton für Zeitfilter
                    item {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TimeFilter.entries.forEachIndexed { index, filter ->
                                SegmentedButton(
                                    selected = timeFilter == filter,
                                    onClick = { timeFilter = filter },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = TimeFilter.entries.size
                                    ),
                                    icon = {
                                        SegmentedButtonDefaults.Icon(active = timeFilter == filter)
                                    }
                                ) {
                                    Text(filter.label)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ✨ Carousel mit Timer-Vorlagen
                    item {
                        // Hole Farben im @Composable Kontext
                        val colorPrimary = MaterialTheme.colorScheme.primary
                        val colorSecondary = MaterialTheme.colorScheme.secondary
                        val colorTertiary = MaterialTheme.colorScheme.tertiary
                        val colorError = MaterialTheme.colorScheme.error

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Schnell-Timer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                // Timer-Vorlagen
                                val templates = listOf(
                                    TemplateData("5 Min", 5, Icons.Default.Coffee, colorTertiary),
                                    TemplateData("10 Min", 10, Icons.Default.Accessibility, colorPrimary),
                                    TemplateData("15 Min", 15, Icons.Default.DirectionsWalk, colorSecondary),
                                    TemplateData("25 Min\nPomodoro", 25, Icons.Default.Psychology, colorError),
                                    TemplateData("30 Min", 30, Icons.Default.FitnessCenter, colorTertiary),
                                    TemplateData("1 Std", 60, Icons.Default.Schedule, colorPrimary),
                                    TemplateData("2 Std", 120, Icons.Default.Timelapse, colorSecondary)
                                )

                                items(templates.size) { index ->
                                    val template = templates[index]
                                    QuickTimerCard(
                                        title = template.title,
                                        minutes = template.minutes,
                                        icon = template.icon,
                                        color = template.color,
                                        onClick = {
                                            performHaptic(haptic, settingsManager)
                                            val targetTime = ZonedDateTime.now().plusMinutes(template.minutes.toLong())
                                            val timer = Timer(
                                                name = "${template.minutes} Min Timer",
                                                target_time = targetTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                                category = "Schnell-Timer"
                                            )
                                            viewModel.createTimer(timer)
                                            showSnackbar(snackbarHostState, "Timer für ${template.minutes} Min erstellt")
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Aktive Timer
                    if (filteredTimers.isEmpty() && completedTimers.isEmpty()) {
                        // ✅ Empty State anzeigen
                        item {
                            AnimatedEmptyState()
                        }
                    }

                    if (filteredTimers.isNotEmpty()) {
                        item {
                            ListHeader(
                                title = "Aktive Timer${if (filterCategory != null) " · $filterCategory" else ""}",
                                count = filteredTimers.size
                            )
                        }
                        items(filteredTimers, key = { it.id }) { timer ->
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(
                                    animationSpec = tween(300)
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { -it / 2 },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) + fadeOut(
                                    animationSpec = tween(200)
                                )
                            ) {
                                TimerCard(
                                    modifier = Modifier
                                        .animateItemPlacement(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                        .animateContentSize(),
                                    timer = timer,
                                onComplete = {
                                    performHaptic(haptic, settingsManager)
                                    viewModel.markTimerCompleted(timer.id)
                                    showSnackbar(snackbarHostState, "Timer abgeschlossen")
                                },
                                onDelete = {
                                    performHaptic(haptic, settingsManager)
                                    viewModel.deleteTimer(timer.id)
                                    showSnackbar(snackbarHostState, "Timer gelöscht")
                                },
                                onEdit = { editedTimer ->
                                    performHaptic(haptic, settingsManager)
                                    viewModel.updateTimer(timer.id, editedTimer)
                                    showSnackbar(snackbarHostState, "Timer aktualisiert")
                                },
                                settingsManager = settingsManager,
                                haptic = haptic
                            )
                            }
                        }
                    }

                    // Abgeschlossene Timer
                    if (completedTimers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            ListHeader("Abgeschlossen", completedTimers.size)
                        }
                        items(completedTimers, key = { it.id }) { timer ->
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(
                                    animationSpec = tween(300)
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { -it / 2 },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) + fadeOut(
                                    animationSpec = tween(200)
                                )
                            ) {
                                TimerCard(
                                    modifier = Modifier.animateItemPlacement(),
                                    timer = timer,
                                    onComplete = { },
                                    onDelete = {
                                        performHaptic(haptic, settingsManager)
                                        viewModel.deleteTimer(timer.id)
                                        showSnackbar(snackbarHostState, "Timer gelöscht")
                                    },
                                    onEdit = { },
                                    settingsManager = settingsManager,
                                    haptic = haptic
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Filter/Sort Dialog
    if (showFilterDialog) {
        FilterSortDialog(
            currentSort = sortBy,
            currentFilter = filterCategory,
            categories = timers.map { it.category }.distinct().sorted(),
            onSortChange = { sortBy = it },
            onFilterChange = { filterCategory = it },
            onDismiss = { showFilterDialog = false }
        )
    }
}

// ✅ Quick-Timer-Buttons
@Composable
fun QuickTimerButtons(
    viewModel: TimerViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    settingsManager: SettingsManager,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val quickTimers = listOf(
        QuickTimerOption(5, Icons.Default.LunchDining, "5 Min"),
        QuickTimerOption(15, Icons.Default.Coffee, "15 Min"),
        QuickTimerOption(30, Icons.Default.LocalPizza, "30 Min"),
        QuickTimerOption(60, Icons.Default.Restaurant, "1 Std")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ),
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Schnell-Timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // ✅ Verbessert: Row statt LazyRow für bessere Performance & Flexibilität
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickTimers.forEach { option ->
                    QuickTimerButton(
                        modifier = Modifier.weight(1f), // Gleichmäßige Verteilung
                        option = option,
                        onClick = {
                            performHaptic(haptic, settingsManager)
                            val userZone = ZoneId.systemDefault()
                            val targetTime = ZonedDateTime.now(userZone).plusMinutes(option.minutes.toLong())
                            val timer = Timer(
                                name = "${option.label} Timer",
                                target_time = targetTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                category = "Schnell-Timer"
                            )
                            viewModel.createTimer(timer)
                            showSnackbar(snackbarHostState, "${option.label} Timer erstellt")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickTimerButton(
    modifier: Modifier = Modifier,
    option: QuickTimerOption,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "quickTimerScale"
    )

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .height(72.dp),
        interactionSource = interactionSource,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                option.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                option.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

data class QuickTimerOption(val minutes: Int, val icon: ImageVector, val label: String)

@Composable
fun ListHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (count != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        // Dekorative Linie
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// ✅ Farbcodierung nach Zeit
// ✅ Verbesserte Farbkontraste für bessere Lesbarkeit
fun getTimerUrgencyColor(targetTime: ZonedDateTime): Color {
    val now = ZonedDateTime.now()
    val minutesUntil = ChronoUnit.MINUTES.between(now, targetTime)

    return when {
        minutesUntil < 0 -> Color(0xFFD32F2F) // Dunkleres Rot: Abgelaufen (war #B00020)
        minutesUntil < 60 -> Color(0xFFE65100) // Dunkleres Orange: < 1 Stunde (war #FF6B35)
        targetTime.toLocalDate() == now.toLocalDate() -> Color(0xFFF57C00) // Dunkles Orange: Heute (war #FFA500)
        targetTime.toLocalDate() == now.toLocalDate().plusDays(1) -> Color(0xFF388E3C) // Dunkleres Grün: Morgen (war #4CAF50)
        else -> Color(0xFF757575) // Dunkleres Grau: Später (war Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerCard(
    timer: Timer,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (Timer) -> Unit,
    settingsManager: SettingsManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val targetTime = remember(timer.target_time) {
        try { ZonedDateTime.parse(timer.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
        catch (e: Exception) { null }
    }

    if (targetTime == null) {
        return
    }

    val now = ZonedDateTime.now()
    val isPast = targetTime.isBefore(now)
    val minutesUntil = ChronoUnit.MINUTES.between(now, targetTime)

    // ✅ Timer Status bestimmen
    val timerState = when {
        timer.is_completed -> TimerState.COMPLETED
        isPast -> TimerState.ALARM
        minutesUntil <= 60 -> TimerState.RUNNING
        else -> TimerState.PENDING
    }

    // ✅ Pulsing Animation für RUNNING und ALARM States
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // ✅ State-basierte Farben
    val (borderColor, borderWidth) = when (timerState) {
        TimerState.PENDING -> Color(0xFF2196F3) to 2.dp // Blau
        TimerState.RUNNING -> Color(0xFFFF9800) to 3.dp // Orange
        TimerState.COMPLETED -> Color(0xFF4CAF50) to 2.dp // Grün
        TimerState.ALARM -> Color(0xFFF44336) to 3.dp // Rot
    }

    // ✅ Farbcodierung
    val urgencyColor = getTimerUrgencyColor(targetTime)

    val timeText = when {
        timer.is_completed -> "Abgeschlossen"
        isPast -> "Abgelaufen"
        else -> {
            val minutesUntil = ChronoUnit.MINUTES.between(now, targetTime)
            val hoursUntil = ChronoUnit.HOURS.between(now, targetTime)
            val daysUntil = ChronoUnit.DAYS.between(now, targetTime)

            when {
                minutesUntil < 60 -> "Noch $minutesUntil Min"
                hoursUntil < 24 -> "Noch ${hoursUntil}h ${minutesUntil % 60}min"
                daysUntil == 0L -> "Heute ${targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))} Uhr"
                daysUntil == 1L -> "Morgen ${targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))} Uhr"
                else -> targetTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " Uhr"
            }
        }
    }

    // ✅ Swipe-Aktionen
    val deleteAction = SwipeAction(
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Löschen",
                tint = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        },
        background = Color(0xFFB00020),
        onSwipe = {
            performHaptic(haptic, settingsManager)
            showDeleteDialog = true
        }
    )

    val completeAction = SwipeAction(
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Abschließen",
                tint = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        },
        background = Color(0xFF4CAF50),
        onSwipe = {
            performHaptic(haptic, settingsManager)
            onComplete()
        }
    )

    // ✅ Card Tap Interaction
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()

    val cardElevation by animateDpAsState(
        targetValue = if (isCardPressed) 2.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardElevation"
    )

    SwipeableActionsBox(
        startActions = if (!timer.is_completed) listOf(completeAction) else emptyList(),
        endActions = listOf(deleteAction),
        swipeThreshold = 100.dp
    ) {
        // ✅ Glasmorphism Box mit animierter Border + Glow
        Box(
            modifier = modifier
                .fillMaxWidth()
        ) {
            // Glow Effect Layer (hinter der Card)
            if (timerState == TimerState.RUNNING || timerState == TimerState.ALARM) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .scale(pulseScale * 1.05f)
                        .blur(24.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    borderColor.copy(alpha = 0.4f * pulseAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                )
            }

            // Card mit Glasmorphism
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .then(
                        if (timerState == TimerState.RUNNING || timerState == TimerState.ALARM) {
                            Modifier
                                .scale(pulseScale)
                                .border(
                                    width = borderWidth,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            borderColor.copy(alpha = pulseAlpha),
                                            borderColor.copy(alpha = pulseAlpha * 0.5f)
                                        )
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                )
                        } else {
                            Modifier.border(
                                width = borderWidth,
                                color = borderColor.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    )
                    .clickable(
                        onClick = {
                            performHaptic(haptic, settingsManager)
                        }
                    ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = cardElevation),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) // Glasmorphism!
                )
            ) {
            Column {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ Circular Progress Indicator - Countdown bis Timer-Start
                    if (!timer.is_completed && !isPast) {
                        val totalMinutes = ChronoUnit.MINUTES.between(
                            ZonedDateTime.parse(timer.created_at, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            targetTime
                        ).toFloat()
                        val remainingMinutes = minutesUntil.toFloat()
                        val progress = if (totalMinutes > 0) {
                            1f - (remainingMinutes / totalMinutes)
                        } else {
                            1f
                        }

                        Box(
                            modifier = Modifier.size(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = progress.coerceIn(0f, 1f),
                                modifier = Modifier.size(60.dp),
                                color = urgencyColor,
                                strokeWidth = 4.dp,
                                trackColor = urgencyColor.copy(alpha = 0.2f)
                            )
                            // Icon in der Mitte
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = urgencyColor
                            )
                        }
                    } else {
                        // Farbindikator für abgeschlossene/abgelaufene Timer
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(60.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(urgencyColor)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // ✅ Timer-Name mit Wiederholungs-Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = timer.name, style = MaterialTheme.typography.titleMedium)

                        // ✅ Wiederholungs-Badge
                        if (timer.recurrence != null) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = "Wiederholend",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = when (timer.recurrence) {
                                            "daily" -> "Tägl."
                                            "weekly" -> "Wöch."
                                            "weekdays" -> "Werkt."
                                            "weekends" -> "WE"
                                            "custom" -> {
                                                // Parse weekdays and show abbreviated form
                                                if (!timer.recurrence_weekdays.isNullOrBlank()) {
                                                    val weekdayNames = mapOf(
                                                        1 to "Mo", 2 to "Di", 3 to "Mi", 4 to "Do",
                                                        5 to "Fr", 6 to "Sa", 7 to "So"
                                                    )
                                                    val days = timer.recurrence_weekdays.split(",")
                                                        .mapNotNull { it.trim().toIntOrNull() }
                                                        .sorted()
                                                        .mapNotNull { weekdayNames[it] }
                                                        .joinToString(",")
                                                    days
                                                } else {
                                                    "Custom"
                                                }
                                            }
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = urgencyColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = urgencyColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // ✅ Farbiges Kategorie-Badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = com.example.timerapp.utils.CategoryColors.getColor(timer.category).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Kategorie",
                                modifier = Modifier.size(14.dp),
                                tint = com.example.timerapp.utils.CategoryColors.getColor(timer.category)
                            )
                            Text(
                                text = timer.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = com.example.timerapp.utils.CategoryColors.getColor(timer.category),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (timer.note?.isNotBlank() == true) {
                        Text(
                            text = timer.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (!timer.is_completed) {
                        AnimatedIconButton(
                            onClick = {
                                performHaptic(haptic, settingsManager)
                                showEditDialog = true
                            },
                            icon = Icons.Default.Edit,
                            contentDescription = "Bearbeiten"
                        )
                    }
                    if (!timer.is_completed) {
                        AnimatedIconButton(
                            onClick = {
                                performHaptic(haptic, settingsManager)
                                onComplete()
                            },
                            icon = Icons.Default.CheckCircle,
                            contentDescription = "Abschließen"
                        )
                    }
                    AnimatedIconButton(
                        onClick = {
                            performHaptic(haptic, settingsManager)
                            showDeleteDialog = true
                        },
                        icon = Icons.Default.Delete,
                        contentDescription = "Löschen"
                    )
                }
                }

                // ✅ Linear Progress Indicator - Zeit seit Timer erstellt
                if (!timer.is_completed) {
                    val createdAt = try {
                        ZonedDateTime.parse(timer.created_at, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    } catch (e: Exception) {
                        null
                    }

                    if (createdAt != null) {
                        val totalDuration = ChronoUnit.MINUTES.between(createdAt, targetTime).toFloat()
                        val elapsed = ChronoUnit.MINUTES.between(createdAt, now).toFloat()
                        val linearProgress = if (totalDuration > 0) {
                            (elapsed / totalDuration).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = linearProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(MaterialTheme.shapes.small),
                                color = urgencyColor,
                                trackColor = urgencyColor.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        val isRecurring = timer.recurrence != null
        val recurrenceDescription = when (timer.recurrence) {
            "daily" -> "täglich"
            "weekly" -> "wöchentlich"
            "weekdays" -> "werktags (Mo-Fr)"
            "weekends" -> "an Wochenenden (Sa-So)"
            "custom" -> {
                if (!timer.recurrence_weekdays.isNullOrBlank()) {
                    val weekdayNames = mapOf(
                        1 to "Montag", 2 to "Dienstag", 3 to "Mittwoch", 4 to "Donnerstag",
                        5 to "Freitag", 6 to "Samstag", 7 to "Sonntag"
                    )
                    val days = timer.recurrence_weekdays.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .sorted()
                        .mapNotNull { weekdayNames[it] }
                    "jeden ${days.joinToString(", ")}"
                } else {
                    "benutzerdefiniert"
                }
            }
            else -> null
        }

        val recurrenceEndDateText = timer.recurrence_end_date?.let {
            try {
                val endDate = ZonedDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                "Endet am: ${endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
            } catch (e: Exception) {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = if (isRecurring) {
                { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            } else null,
            title = { Text(if (isRecurring) "Wiederholenden Timer löschen?" else "Timer löschen?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Möchtest du '${timer.name}' wirklich löschen?")

                    if (isRecurring) {
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "⚠️ Dies ist ein wiederholender Timer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Wiederholt sich: $recurrenceDescription",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (recurrenceEndDateText != null) {
                                    Text(
                                        text = recurrenceEndDateText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = if (isRecurring) {
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.textButtonColors()
                    }
                ) {
                    Text(if (isRecurring) "Trotzdem löschen" else "Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    // Edit Dialog
    if (showEditDialog) {
        EditTimerDialog(
            timer = timer,
            onDismiss = { showEditDialog = false },
            onSave = { editedTimer ->
                onEdit(editedTimer)
                showEditDialog = false
            }
        )
    }
}

// ✅ Timer State Enum
private enum class TimerState {
    PENDING,    // ⏰ Ausstehend
    RUNNING,    // ⏳ Läuft
    COMPLETED,  // ✅ Abgeschlossen
    ALARM       // 🔔 Alarm
}

// ✅ Animated Icon Button with Scale Effect
@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        interactionSource = interactionSource
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

// ✅ Timer Complete Animation
@Composable
private fun TimerCompleteAnimation(
    visible: Boolean,
    onAnimationEnd: () -> Unit = {}
) {
    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(1500)
            onAnimationEnd()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(animationSpec = tween(300)),
        exit = scaleOut(
            targetScale = 1.2f,
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            // Grüner Kreis mit Checkmark
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = Color(0xFF4CAF50),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Abgeschlossen",
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }
}

// ✅ Animated Empty State
@Composable
private fun AnimatedEmptyState() {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyState")

    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    val iconRotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animiertes Icon
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier
                .size(120.dp)
                .offset(y = floatingOffset.dp)
                .rotate(iconRotation)
        )

        Text(
            text = "Keine Timer vorhanden",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Text(
            text = "Erstelle deinen ersten Timer mit dem + Button",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ✅ Filter & Sort Dialog
@Composable
fun FilterSortDialog(
    currentSort: SortType,
    currentFilter: String?,
    categories: List<String>,
    onSortChange: (SortType) -> Unit,
    onFilterChange: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sortieren & Filtern") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Sortieren nach:", style = MaterialTheme.typography.titleSmall)
                SortType.values().forEach { sortType ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == sortType,
                            onClick = { onSortChange(sortType) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(sortType.label)
                    }
                }

                Divider()

                Text("Filtern nach Kategorie:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentFilter == null,
                        onClick = { onFilterChange(null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Alle Kategorien")
                }
                categories.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFilter == category,
                            onClick = { onFilterChange(category) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}

enum class SortType(val label: String) {
    DATE("Datum"),
    NAME("Name"),
    CATEGORY("Kategorie")
}

enum class TimeFilter(val label: String) {
    ALL("Alle"),
    TODAY("Heute"),
    TOMORROW("Morgen"),
    THIS_WEEK("Diese Woche")
}

// ✨ Template Data für Carousel
data class TemplateData(
    val title: String,
    val minutes: Int,
    val icon: ImageVector,
    val color: Color
)

// ✨ Quick Timer Card für Carousel
@Composable
private fun QuickTimerCard(
    title: String,
    minutes: Int,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "cardScale"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(140.dp)
            .scale(scale),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.3f),
                                    color.copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = color.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = color
                    )
                }

                // Text
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Edit Timer Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTimerDialog(
    timer: Timer,
    onDismiss: () -> Unit,
    onSave: (Timer) -> Unit
) {
    var name by remember { mutableStateOf(timer.name) }
    var note by remember { mutableStateOf(timer.note ?: "") }

    val targetTime = remember {
        try {
            ZonedDateTime.parse(timer.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (e: Exception) {
            ZonedDateTime.now()
        }
    }

    var selectedDate by remember { mutableStateOf(targetTime.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(targetTime.toLocalTime()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val userZone = ZoneId.systemDefault()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timer bearbeiten") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Datum",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Uhrzeit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        WheelTimePicker(
                            initialTime = selectedTime,
                            onTimeSelected = { selectedTime = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notiz (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newTargetDateTime = ZonedDateTime.of(
                        selectedDate,
                        selectedTime,
                        userZone
                    )

                    val editedTimer = timer.copy(
                        name = name,
                        target_time = newTargetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        note = note.ifBlank { null }
                    )
                    onSave(editedTimer)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(userZone).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(userZone)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ExactAlarmPermissionRationaleDialog(onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        icon = { Icon(Icons.Default.Alarm, contentDescription = null) },
        title = { Text("Berechtigung erforderlich") },
        text = {
            Text(
                "Damit die Timer zuverlässig funktionieren, auch wenn die App geschlossen ist, " +
                        "benötigt die App die Berechtigung 'Alarme & Erinnerungen'."
            )
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("Zu den Einstellungen")
            }
        }
    )
}

@Composable
private fun EmptyState(onCreateTimer: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Keine Timer vorhanden",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCreateTimer) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Neuen Timer erstellen")
            }
        }
    }
}

// ✅ Haptic Feedback Helper
fun performHaptic(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback, settingsManager: SettingsManager) {
    if (settingsManager.isHapticFeedbackEnabled) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

// ✅ SnackBar Helper
fun showSnackbar(snackbarHostState: SnackbarHostState, message: String) {
    CoroutineScope(Dispatchers.Main).launch {
        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
    }
}
