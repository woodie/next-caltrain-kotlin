package com.netpress.nextcaltrain

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netpress.nextcaltrain.ui.theme.AppStyle
import com.netpress.nextcaltrain.ui.theme.LocalAppColors

enum class StopRole { PAST, ORIGIN, DESTINATION, TRANSFER, FUTURE }

data class TripStop(
    val time: Int,
    val station: String,
    val role: StopRole,
    val transferLabel: String? = null,
)

fun computeStops(
    trip: Trip,
    schedule: Schedule,
    origin: String,
    destination: String,
    scheduleType: ScheduleType,
    goodTimes: GoodTimes,
): List<TripStop> {
    val result = mutableListOf<TripStop>()
    for ((legIndex, leg) in trip.legs.withIndex()) {
        val direction = CaltrainService.direction(leg.station, destination, schedule.southStops)
        val stopList = if (direction == "North") schedule.northStops else schedule.southStops
        val source = when (scheduleType) {
            ScheduleType.WEEKDAY -> if (direction == "North") schedule.northWeekday else schedule.southWeekday
            ScheduleType.WEEKEND -> if (direction == "North") schedule.northWeekend else schedule.southWeekend
            ScheduleType.HOLIDAY -> if (direction == "North") schedule.northHoliday else schedule.southHoliday
        }
        val times = source[leg.trainId.toString()] ?: continue
        val isSecondLeg = legIndex > 0
        for ((i, time) in times.withIndex()) {
            val t = time ?: continue
            if (i >= stopList.size) continue
            val sta = stopList[i]
            if (isSecondLeg && sta == CaltrainService.transferStation) continue
            val isTransfer = trip.isTransfer && legIndex == 0 && sta == CaltrainService.transferStation
            val role = when {
                sta == origin && legIndex == 0 -> StopRole.ORIGIN
                sta == destination -> StopRole.DESTINATION
                isTransfer -> StopRole.TRANSFER
                goodTimes.inThePast(t) -> StopRole.PAST
                else -> StopRole.FUTURE
            }
            result.add(TripStop(time = t, station = sta, role = role))
        }
    }
    return result
}

@Composable
fun TripDetailScreen(
    vm: TripViewModel,
    onNavigateBack: () -> Unit,
) {
    val detailState by vm.tripDetailState.collectAsStateWithLifecycle()
    val goodTimes by vm.goodTimes.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    val state = detailState ?: return

    val stops = remember(state.trip, state.scheduleType) {
        computeStops(state.trip, vm.schedule, state.origin, state.destination, state.scheduleType, goodTimes)
    }

    val currentLeg = if (state.trip.legs.size > 1 && goodTimes.inThePast(state.trip.legs[1].depart)) {
        state.trip.legs[1]
    } else {
        state.trip.legs.first()
    }
    val direction = if (currentLeg.trainId % 2 == 0) "Southbound" else "Northbound"
    val title = "$direction #${currentLeg.trainId} ${CaltrainService.trainType(currentLeg.trainId)}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
    ) {
        // Toolbar — back button left, title centered
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
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
            Text(
                text = title,
                fontSize = AppStyle.fontStatusBar,
                color = colors.appText,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))
            // Balance the back button so title stays centered
            Spacer(modifier = Modifier.size(AppStyle.iconButtonSizeDp.dp))
        }

        // Stop list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            stops.forEachIndexed { index, stop ->
                // Segment below this dot takes the next stop's track color
                val nextTrackColor = stops.getOrNull(index + 1)?.let { next ->
                    if (next.role == StopRole.PAST) colors.calPast else colors.calArrive
                } ?: colors.calArrive
                StopRow(
                    stop = stop,
                    isFirst = index == 0,
                    isLast = index == stops.size - 1,
                    nextTrackColor = nextTrackColor,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun StopRow(
    stop: TripStop,
    isFirst: Boolean,
    isLast: Boolean,
    nextTrackColor: androidx.compose.ui.graphics.Color,
    colors: com.netpress.nextcaltrain.ui.theme.AppColors,
) {
    val trackColor = if (stop.role == StopRole.PAST) colors.calPast else colors.calArrive
    val dotColor = when (stop.role) {
        StopRole.ORIGIN, StopRole.DESTINATION, StopRole.TRANSFER -> colors.appText
        else -> trackColor
    }
    val dotSizeDp = 14.dp
    val dotOffsetYDp = 4.dp
    val rowHeightDp = 48.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeightDp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Time — right-aligned in fixed-width column
        Text(
            text = GoodTimes.fullTime(stop.time),
            fontSize = AppStyle.fontTrain,
            color = colors.appText,
            softWrap = false,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .padding(top = dotOffsetYDp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Track: Canvas draws the dot and the connecting line above/below it
        Canvas(
            modifier = Modifier
                .width(dotSizeDp)
                .fillMaxHeight()
        ) {
            val cx = size.width / 2f
            val dotR = dotSizeDp.toPx() / 2f
            val dotCY = dotOffsetYDp.toPx() + dotR
            val lineW = 2.dp.toPx()

            // Line from top of row to dot edge (all rows except first)
            if (!isFirst) {
                drawLine(
                    color = trackColor,
                    start = Offset(cx, 0f),
                    end = Offset(cx, dotCY - dotR),
                    strokeWidth = lineW,
                )
            }
            // Line from dot edge to bottom of row — uses NEXT stop's color
            if (!isLast) {
                drawLine(
                    color = nextTrackColor,
                    start = Offset(cx, dotCY + dotR),
                    end = Offset(cx, size.height),
                    strokeWidth = lineW,
                )
            }
            // Dot
            drawCircle(
                color = dotColor,
                radius = dotR,
                center = Offset(cx, dotCY),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Station name (+ optional transfer label)
        Column(modifier = Modifier.padding(top = dotOffsetYDp)) {
            Text(
                text = stop.station,
                fontSize = AppStyle.fontTrain,
                color = colors.appText,
                softWrap = false,
            )
            if (stop.transferLabel != null) {
                Text(
                    text = stop.transferLabel,
                    fontSize = (AppStyle.fontOrigin.value - 2).sp,
                    color = colors.calSwapped,
                    softWrap = false,
                )
            }
        }
    }
}
