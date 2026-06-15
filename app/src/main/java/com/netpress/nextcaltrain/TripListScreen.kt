package com.netpress.nextcaltrain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netpress.nextcaltrain.ui.theme.AppStyle
import com.netpress.nextcaltrain.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun TripListScreen(
    vm: TripViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTripDetail: () -> Unit,
    onNavigateToStationSelection: () -> Unit,
) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    val offset by vm.offset.collectAsStateWithLifecycle()
    val goodTimes by vm.goodTimes.collectAsStateWithLifecycle()
    val scheduleType by vm.scheduleType.collectAsStateWithLifecycle()
    val origin by vm.origin.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    val density = LocalDensity.current

    var blinkOn by remember { mutableStateOf(true) }
    var dragShift by remember { mutableIntStateOf(0) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val rowHeightDp = 48.dp  // 44dp content + 2dp top/bottom padding
    val rowHeightPx = with(density) { rowHeightDp.toPx() }

    val effectiveOffset = (offset + dragShift).coerceIn(0, maxOf(trips.size - 1, 0))
    val selectedTrip = trips.getOrNull(effectiveOffset)
    val noTrains = trips.isEmpty()
    val isSelectedPast = selectedTrip?.let { goodTimes.inThePast(it.depart) } ?: false
    val isSelectedFuture = selectedTrip?.isFuture ?: false
    val isSelectedDeparting = selectedTrip?.let { goodTimes.departing(it.depart) } ?: false

    val statusColor = when {
        noTrains || vm.swapped || isSelectedPast || isSelectedFuture -> colors.calPast
        isSelectedDeparting -> colors.calDepart
        else -> colors.calArrive
    }
    val statusText = when {
        noTrains -> "NO TRAINS"
        isSelectedFuture -> "${vm.tomorrowScheduleType.label} Schedule"
        vm.swapped || isSelectedPast -> "${scheduleType.label} Schedule"
        isSelectedDeparting -> "DEPARTING"
        selectedTrip != null -> goodTimes.countdown(selectedTrip.depart)
        else -> ""
    }
    val serviceTypeLabel = trips.firstOrNull()?.legs?.firstOrNull()?.let {
        "${CaltrainService.trainType(it.trainId)} Service"
    } ?: ""
    val line1 = if (origin.length + 3 >= destination.length) origin else "$origin to"
    val line2 = if (origin.length + 3 >= destination.length) "to $destination" else destination

    LaunchedEffect(isSelectedDeparting, noTrains) {
        while (true) {
            delay(500L)
            blinkOn = if (isSelectedDeparting || noTrains) !blinkOn else true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val rowCount = if (headerHeightPx > 0) {
            val available = screenHeightPx - headerHeightPx
            minOf(maxOf(1, (available / rowHeightPx).toInt()), maxOf(trips.size, 1))
        } else 6

        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFF808080), colors.appBackground)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { headerHeightPx = it.size.height.toFloat() }
            ) {
                // Row 1: service type + swap/reset buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = serviceTypeLabel,
                        fontSize = AppStyle.fontStatusBar,
                        color = colors.appText,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (vm.hasManualSelection) {
                        IconButton(
                            onClick = { vm.resetToNext() },
                            modifier = Modifier
                                .size(AppStyle.iconButtonSizeDp.dp)
                                .clip(CircleShape)
                                .background(colors.iconCircleBackground),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reset", tint = colors.appText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = { vm.swapStations() },
                        modifier = Modifier
                            .size(AppStyle.iconButtonSizeDp.dp)
                            .clip(CircleShape)
                            .background(colors.iconCircleBackground),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Swap", tint = colors.appText)
                    }
                }

                // Row 2: back button + station names (centered) + spacer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(AppStyle.iconButtonSizeDp.dp)
                            .clip(CircleShape)
                            .background(colors.iconCircleBackground),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.appText)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures { onNavigateToStationSelection() }
                        },
                    ) {
                        Text(line1, fontSize = AppStyle.fontOrigin, color = colors.appText, maxLines = 1, softWrap = false)
                        Text(line2, fontSize = AppStyle.fontOrigin, color = colors.appText, maxLines = 1, softWrap = false)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Balance the back button
                    Spacer(modifier = Modifier.size(AppStyle.iconButtonSizeDp.dp))
                }

                // Status blurb — tappable to cycle schedule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeightDp)
                        .pointerInput(Unit) { detectTapGestures { vm.cycleSchedule() } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = statusText,
                        fontSize = AppStyle.fontBlurb,
                        color = statusColor,
                        softWrap = false,
                        modifier = Modifier.alpha(
                            if ((isSelectedDeparting || noTrains) && !blinkOn) 0f else 1f
                        ),
                    )
                }
            }

            // ── Trip rows ────────────────────────────────────────────────────
            // rememberUpdatedState gives the lambda a live reference to offset/trips
            // without restarting the coroutine on every recomposition.
            val latestOffset = rememberUpdatedState(offset)
            val latestTrips = rememberUpdatedState(trips)

            // Single pointerInput handles both drag and tap to avoid child gesture conflict.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // Key is trips.size only (not offset) — prevents restarting mid-drag
                    // when the timer or vm.setOffset() updates offset.
                    .pointerInput(trips.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDragY = 0f
                            var isDragging = false

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                val dy = change.position.y - change.previousPosition.y
                                totalDragY += dy

                                if (!isDragging && abs(totalDragY) > viewConfiguration.touchSlop) {
                                    isDragging = true
                                    // Mark immediately so the 1-second timer can't reset
                                    // offset mid-drag (which would shift the drag baseline).
                                    vm.markDragStart()
                                }
                                if (isDragging) {
                                    change.consume()
                                    accumulatedDrag = totalDragY
                                    val newShift = -(accumulatedDrag / rowHeightPx).toInt()
                                    val proposed = latestOffset.value + newShift
                                    if (proposed >= 0 && proposed < latestTrips.value.size) {
                                        dragShift = newShift
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            if (isDragging) {
                                // Use latestOffset.value (live) not captured `effectiveOffset`
                                // (stale Int from composition time) to avoid snapping back.
                                val finalOffset = (latestOffset.value + dragShift)
                                    .coerceIn(0, maxOf(latestTrips.value.size - 1, 0))
                                vm.setOffset(finalOffset)
                                dragShift = 0
                                accumulatedDrag = 0f
                            } else {
                                // Tap — find which row and navigate
                                val slot = (down.position.y / rowHeightPx).toInt()
                                val tapped = latestTrips.value.getOrNull(latestOffset.value + slot)
                                if (tapped != null) {
                                    vm.selectTripForDetail(tapped)
                                    onNavigateToTripDetail()
                                }
                            }
                        }
                    },
            ) {
                repeat(rowCount) { slot ->
                    val trip = trips.getOrNull(effectiveOffset + slot)
                    if (trip != null) {
                        val isNext = slot == 0 && !vm.swapped
                        val isInactive = when {
                            vm.swapped -> false
                            trip.isFuture -> true
                            else -> goodTimes.inThePast(trip.depart)
                        }
                        TripRow(
                            trip = trip,
                            isNext = isNext,
                            isInactive = isInactive,
                            isFuture = trip.isFuture,
                            isDeparting = slot == 0 && isSelectedDeparting,
                            swapped = vm.swapped,
                            colors = colors,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TripRow(
    trip: Trip,
    isNext: Boolean,
    isInactive: Boolean,
    isFuture: Boolean,
    isDeparting: Boolean,
    swapped: Boolean,
    colors: com.netpress.nextcaltrain.ui.theme.AppColors,
) {
    val textColor = when {
        isInactive || swapped -> colors.calPast
        else -> colors.appText
    }
    val borderColor = when {
        isNext && isDeparting -> colors.calDepart
        isNext && (isFuture || isInactive || swapped) -> colors.calSwapped
        isNext -> colors.calArrive
        else -> Color.Transparent
    }

    val (departTime, departMer) = GoodTimes.partTime(trip.depart)
    val (arriveTime, arriveMer) = GoodTimes.partTime(trip.arrive)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            "#${trip.legs.first().trainId}",
            fontSize = AppStyle.fontTrain,
            color = textColor,
            softWrap = false,
        )
        TripTimeDisplay(departTime, departMer, textColor)
        TripTimeDisplay(arriveTime, arriveMer, textColor)
    }
}

@Composable
private fun TripTimeDisplay(time: String, mer: String, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(time, fontSize = AppStyle.fontBlurb, color = color, softWrap = false)
        Text(mer, fontSize = AppStyle.fontTrain, color = color, softWrap = false)
    }
}
