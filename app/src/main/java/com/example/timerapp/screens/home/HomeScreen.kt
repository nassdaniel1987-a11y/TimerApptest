package com.example.timerapp.screens.home

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.timerapp.SettingsManager
import com.example.timerapp.models.Timer
import com.example.timerapp.screens.EmptyStateView
import com.example.timerapp.ui.theme.GradientColors
import com.example.timerapp.viewmodel.TimerViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: TimerViewModel,
    onCreateTimer: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val allTimers by viewModel.timers.collectAsState()
    val pendingDeleteIds by viewModel.pendingDeleteTimerIds.collectAsState()
    val timers = allTimers.filter { it.id !in pendingDeleteIds }
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

    var isAppPaused by remember { mutableStateOf(settingsManager.isAppPaused) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var timeFilter by remember { mutableStateOf(TimeFilter.ALL) }

    var klasseFilter by remember { mutableStateOf(settingsManager.klasseFilter) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    val filteredTimers = remember(timers, filterCategory, sortBy, searchQuery, timeFilter, klasseFilter) {
        var filtered = timers.filter { !it.is_completed }

        if (klasseFilter != null) {
            filtered = filtered.filter { it.klasse == klasseFilter }
        }

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                (it.note?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

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

    val completedTimers = timers.filter { it.is_completed && (klasseFilter == null || it.klasse == klasseFilter) }

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
            containerColor = Color.Transparent,
            topBar = {
            LargeTopAppBar(
                title = { Text("Timer") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü öffnen")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            if (filterCategory != null) Icons.Default.FilterAltOff else Icons.Default.FilterAlt,
                            contentDescription = if (filterCategory != null) "Filter entfernen" else "Filter und Sortierung"
                        )
                    }
                    IconButton(onClick = { viewModel.sync() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Timer aktualisieren")
                    }
                    IconButton(onClick = {
                        performHaptic(haptic, settingsManager)
                        settingsManager.isAppPaused = !settingsManager.isAppPaused
                        isAppPaused = settingsManager.isAppPaused
                    }) {
                        Icon(
                            if (isAppPaused) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = if (isAppPaused) "Alarme aktivieren" else "Alarme pausieren",
                            tint = if (isAppPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
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
        Column(modifier = Modifier.padding(padding)) {
            // Pause-Banner
            if (isAppPaused) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            settingsManager.isAppPaused = false
                            isAppPaused = false
                        },
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Alarme sind pausiert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Tippe hier zum Reaktivieren",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Alarme wieder aktivieren",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            SwipeRefresh(
                state = rememberSwipeRefreshState(isLoading),
                onRefresh = { viewModel.sync() }
            ) {
            if (timers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Timer,
                    title = "Keine Timer vorhanden",
                    subtitle = "Erstelle deinen ersten Timer und werde rechtzeitig erinnert!",
                    ctaText = "Neuen Timer erstellen",
                    onCtaClick = onCreateTimer
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SearchBar
                    item {
                        androidx.compose.material3.SearchBar(
                            inputField = {
                                SearchBarDefaults.InputField(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    onSearch = { isSearchActive = false },
                                    expanded = isSearchActive,
                                    onExpandedChange = { isSearchActive = it },
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
                                )
                            },
                            expanded = isSearchActive,
                            onExpandedChange = { isSearchActive = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
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

                    // SegmentedButton für Zeitfilter
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

                    // Klassen-Filter Chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = klasseFilter == null,
                                onClick = {
                                    klasseFilter = null
                                    viewModel.updateKlasseFilter(null)
                                },
                                label = { Text("Alle") },
                                leadingIcon = if (klasseFilter == null) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            SettingsManager.KLASSE_OPTIONS.forEach { klasse ->
                                FilterChip(
                                    selected = klasseFilter == klasse,
                                    onClick = {
                                        klasseFilter = klasse
                                        viewModel.updateKlasseFilter(klasse)
                                    },
                                    label = { Text(klasse.removePrefix("Klasse ")) },
                                    leadingIcon = if (klasseFilter == klasse) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Carousel mit Timer-Vorlagen
                    item {
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
                                                category = "Schnell-Timer",
                                                klasse = settingsManager.myKlasse
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
                        item {
                            EmptyStateView(
                                icon = Icons.Default.SearchOff,
                                title = "Keine Timer gefunden",
                                subtitle = "Versuche einen anderen Filter oder erstelle einen neuen Timer",
                                ctaText = "Neuen Timer erstellen",
                                onCtaClick = onCreateTimer
                            )
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
                                        .animateItem(
                                            placementSpec = spring(
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
                                    viewModel.softDeleteTimer(timer.id)
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "'${timer.name}' gelöscht",
                                            actionLabel = "Rückgängig",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDeleteTimer(timer.id)
                                        }
                                    }
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
                                    modifier = Modifier.animateItem(),
                                    timer = timer,
                                    onComplete = { },
                                    onDelete = {
                                        performHaptic(haptic, settingsManager)
                                        viewModel.softDeleteTimer(timer.id)
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "'${timer.name}' gelöscht",
                                                actionLabel = "Rückgängig",
                                                duration = SnackbarDuration.Long
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDeleteTimer(timer.id)
                                            }
                                        }
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
        } // Column
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
