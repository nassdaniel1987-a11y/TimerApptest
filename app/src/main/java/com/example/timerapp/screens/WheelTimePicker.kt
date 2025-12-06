package com.example.timerapp.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalTime
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelTimePicker(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }

    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = selectedHour)
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = selectedMinute)

    LaunchedEffect(selectedHour, selectedMinute) {
        onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
    }

    LaunchedEffect(hourState.isScrollInProgress) {
        if (!hourState.isScrollInProgress) {
            selectedHour = hourState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(minuteState.isScrollInProgress) {
        if (!minuteState.isScrollInProgress) {
            selectedMinute = minuteState.firstVisibleItemIndex
        }
    }

    Box(
        modifier = modifier.height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                items = (0..23).map { it.toString().padStart(2, '0') },
                state = hourState,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            WheelPicker(
                items = (0..59).map { it.toString().padStart(2, '0') },
                state = minuteState,
                modifier = Modifier.weight(1f)
            )
        }
        PickerOverlay(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PickerOverlay(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.surface

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val itemHeight = 50.dp
            HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp)
            Spacer(modifier = Modifier.height(itemHeight))
            HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to color,
                        0.3f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1.0f to color
                    )
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    items: List<String>,
    state: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val componentHeight = 200.dp
    val itemHeight = 50.dp
    val padding = (componentHeight - itemHeight) / 2

    val view = LocalView.current
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = padding)
        ) {
            items(items.size) { index ->
                // ----- HIER IST DIE KORREKTUR -----
                // Wir verwenden eine stabilere Methode zur Berechnung der Transformation,
                // die nicht auf potenziell unvollständige Layout-Informationen zugreift.
                val rotationX = remember(state.firstVisibleItemScrollOffset, state.firstVisibleItemIndex) {
                    val firstItemOffset = state.firstVisibleItemScrollOffset
                    val firstItemIndex = state.firstVisibleItemIndex
                    val itemOffset = (index - firstItemIndex) * itemHeight.value - (firstItemOffset / 20f)
                    -itemOffset
                }

                val alpha = remember(rotationX) {
                    1f - (abs(rotationX) * 0.01f).coerceIn(0f, 1f)
                }

                val scale = remember(rotationX) {
                    1f - (abs(rotationX) * 0.005f).coerceIn(0f, 0.5f)
                }

                Text(
                    text = items[index],
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .graphicsLayer {
                            this.rotationX = rotationX
                            this.alpha = alpha
                            this.scaleX = scale
                            this.scaleY = scale
                        },
                    style = LocalTextStyle.current
                )
            }
        }
    }
}