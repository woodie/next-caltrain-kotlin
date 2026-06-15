package com.netpress.nextcaltrain

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netpress.nextcaltrain.ui.theme.AppStyle
import com.netpress.nextcaltrain.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    schedule: Schedule,
    onNavigateToTripList: (() -> Unit)? = null,
    onNavigateToAbout: (() -> Unit)? = null,
    onNavigateToStationSelection: (() -> Unit)? = null,
) {
    val vm: TripViewModel = viewModel(factory = TripViewModelFactory(schedule))
    val trips by vm.trips.collectAsStateWithLifecycle()
    val offset by vm.offset.collectAsStateWithLifecycle()
    val goodTimes by vm.goodTimes.collectAsStateWithLifecycle()
    val scheduleType by vm.scheduleType.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current

    var blinkOn by remember { mutableStateOf(true) }
    var dragShift by remember { mutableIntStateOf(0) }
    val rowHeight = 44f

    val effectiveOffset = (offset + dragShift).coerceIn(0, maxOf(trips.size - 1, 0))
    val selectedTrip = trips.getOrNull(effectiveOffset)
    val noTrains = trips.isEmpty()
    val isSelectedPast = selectedTrip?.let { goodTimes.inThePast(it.depart) } ?: false
    val isSelectedFuture = selectedTrip?.isFuture ?: false
    val isSelectedDeparting = selectedTrip?.let { goodTimes.departing(it.depart) } ?: false

    val ringColor = when {
        noTrains || isSelectedFuture || vm.swapped || isSelectedPast -> colors.calSwapped
        isSelectedDeparting -> colors.calDepart
        else -> colors.calArrive
    }

    val infoColor = if (vm.swapped || isSelectedPast || isSelectedFuture) colors.calPast else colors.appText

    val line1 = with(vm) {
        val o = origin.value; val d = destination.value
        if (o.length + 3 >= d.length) o else "$o to"
    }
    val line2 = with(vm) {
        val o = origin.value; val d = destination.value
        if (o.length + 3 >= d.length) "to $d" else d
    }

    LaunchedEffect(isSelectedDeparting, noTrains) {
        while (true) {
            delay(500L)
            blinkOn = if (isSelectedDeparting || noTrains) !blinkOn else true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onNavigateToTripList?.invoke() }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        val newShift = -(dragAmount.y / rowHeight).toInt()
                        val proposed = offset + newShift
                        if (proposed >= 0 && proposed < trips.size) dragShift = newShift
                    },
                    onDragEnd = {
                        vm.setOffset(effectiveOffset)
                        dragShift = 0
                    }
                )
            }
    ) {
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

        // Toolbar — fixed at top, own layer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
                .statusBarsPadding()
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Next Caltrain",
                fontSize = AppStyle.fontStatusBar,
                color = colors.appText,
                maxLines = 1,
                modifier = Modifier
                    .pointerInput(Unit) { detectTapGestures { onNavigateToAbout?.invoke() } }
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

        // Ring — decorative only, centered on full screen
        Canvas(
            modifier = Modifier
                .size(230.dp)
                .align(Alignment.Center)
        ) {
            drawCircle(
                color = ringColor,
                radius = size.minDimension / 2 - 5.dp.toPx(),
                style = Stroke(width = 5.dp.toPx())
            )
        }

        // Content — floats independently, centered on full screen, not constrained by ring
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 8.dp),
        ) {
            // Station names
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures { onNavigateToStationSelection?.invoke() }
                }
            ) {
                Text(
                    line1,
                    fontSize = AppStyle.fontOrigin,
                    color = colors.appText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    line2,
                    fontSize = AppStyle.fontOrigin,
                    color = colors.appText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Status blurb — no constraints, overflows ring naturally
            when {
                noTrains -> Text(
                    "NO TRAINS",
                    fontSize = AppStyle.fontBlurb,
                    color = colors.calPast,
                    softWrap = false,
                    modifier = Modifier.alpha(if (blinkOn) 1f else 0f),
                )
                isSelectedFuture -> Text(
                    vm.tomorrowScheduleType.label,
                    fontSize = AppStyle.fontBlurb,
                    color = colors.calPast,
                    softWrap = false,
                )
                isSelectedDeparting -> Text(
                    "DEPARTING",
                    fontSize = AppStyle.fontBlurb,
                    color = colors.calDepart,
                    softWrap = false,
                    modifier = Modifier.alpha(if (blinkOn) 1f else 0f),
                )
                vm.swapped || isSelectedPast -> Text(
                    scheduleType.label,
                    fontSize = AppStyle.fontBlurb,
                    color = colors.calPast,
                    softWrap = false,
                )
                selectedTrip != null -> {
                    val c = goodTimes.countdown(selectedTrip.depart)
                    if (c.isNotEmpty()) {
                        Text(
                            c,
                            fontSize = AppStyle.fontBlurb,
                            color = colors.calArrive,
                            softWrap = false,
                        )
                    }
                }
            }

            // Train number + time
            selectedTrip?.let { trip ->
                val (timeStr, merStr) = GoodTimes.partTime(trip.depart)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "#${trip.legs.first().trainId}",
                            fontSize = AppStyle.fontTrain,
                            color = infoColor,
                            softWrap = false,
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            timeStr,
                            fontSize = AppStyle.fontBlurb,
                            color = infoColor,
                            softWrap = false,
                        )
                        Text(
                            merStr,
                            fontSize = AppStyle.fontTrain,
                            color = infoColor,
                            softWrap = false,
                        )
                    }
                    Text(
                        CaltrainService.trainType(trip.legs.first().trainId),
                        fontSize = AppStyle.fontTrain,
                        color = colors.appText,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
