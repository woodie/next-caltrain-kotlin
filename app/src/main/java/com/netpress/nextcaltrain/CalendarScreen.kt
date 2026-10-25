package com.netpress.nextcaltrain

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.netpress.nextcaltrain.ui.CircleButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netpress.nextcaltrain.ui.theme.AppStyle
import com.netpress.nextcaltrain.ui.theme.LocalAppColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Before 2:00 AM is still the previous schedule day, as in GoodTimes.
private const val SCHEDULE_DAY_START = 120

// The calendar planner: when to travel, then the event to add. Mirrors iOS CalendarView.swift.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: TripViewModel, onNavigateBack: () -> Unit, onNavigateToStationSelection: () -> Unit) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val origin by vm.origin.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(PlanMode.LEAVE) }
    var time by remember { mutableStateOf(initialTime(vm)) }
    var date by remember { mutableStateOf(initialDate(vm)) }
    var showModeMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val isoDate = date.toString()
    val scheduleType = CaltrainSchedule.optionIndexFor(isoDate, CalendarTrip.dotw(isoDate), vm.schedule.specialDates)
    val trips = remember(origin, destination, scheduleType) {
        CaltrainService(vm.schedule).routes(origin, destination, scheduleType)
    }
    var minutes = time.hour * 60 + time.minute
    if (minutes < SCHEDULE_DAY_START) minutes += TripViewModel.dayMinutes
    val trains = CalendarTrip.find(trips, mode, minutes)?.let { CalendarTrip.neighbors(trips, it) } ?: emptyList()

    // The planned train as a new event in the phone's calendar app, on Pacific time.
    fun addToCalendar() {
        val planned = trains.firstOrNull { it.selected } ?: return
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, CalendarTrip.title(destination))
            .putExtra(CalendarContract.Events.EVENT_LOCATION, CalendarTrip.location(origin))
            .putExtra(CalendarContract.Events.DESCRIPTION, CalendarTrip.notes(origin, destination, trains))
            .putExtra(CalendarContract.Events.EVENT_TIMEZONE, CalendarTrip.zone.id)
            .putExtra(
                CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                CalendarTrip.date(isoDate, planned.depart).toInstant().toEpochMilli(),
            )
            .putExtra(
                CalendarContract.EXTRA_EVENT_END_TIME,
                CalendarTrip.date(isoDate, planned.arrive).toInstant().toEpochMilli(),
            )
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No calendar app installed; nothing to hand the event to.
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(AppStyle.iconButtonSizeDp.dp)
                    .clip(CircleShape)
                    .background(colors.iconCircleBackground),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.appText)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("Create Calendar Event", fontSize = AppStyle.fontStatusBar, color = colors.appText)
            Spacer(modifier = Modifier.weight(1f))
            // Adds the event, the same green check as the station screen's Save.
            if (trains.isEmpty()) {
                Spacer(modifier = Modifier.size(AppStyle.iconButtonSizeDp.dp))
            } else {
                CircleButton(
                    onClick = { addToCalendar() },
                    modifier = Modifier
                        .size(AppStyle.iconButtonSizeDp.dp)
                        .clip(CircleShape)
                        .background(colors.iconCircleBackground),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Add to Calendar", tint = colors.calArrive)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Pill("$origin to $destination", onClick = onNavigateToStationSelection)

            // The mode menu; only the layout on screen carries its dropdown.
            @Composable
            fun modePill(withMenu: Boolean) {
                Box {
                    Pill(mode.label, trailing = Icons.Filled.UnfoldMore, onClick = { showModeMenu = true })
                    if (withMenu) {
                        DropdownMenu(expanded = showModeMenu, onDismissRequest = { showModeMenu = false }) {
                            PlanMode.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    leadingIcon = {
                                        if (option == mode) Icon(Icons.Filled.Check, contentDescription = null)
                                    },
                                    onClick = {
                                        mode = option
                                        showModeMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            @Composable
            fun dateTimePills(dateFormat: String) {
                Pill(date.format(DateTimeFormatter.ofPattern(dateFormat, Locale.US)), onClick = { showDatePicker = true })
                Pill(time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)), onClick = { showTimePicker = true })
            }

            // One row with the full date, then with a numeric date (10/21/26), then two lines.
            FirstThatFits(
                { withMenu ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modePill(withMenu)
                        dateTimePills("MMM d, yyyy")
                    }
                },
                { withMenu ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modePill(withMenu)
                        dateTimePills("M/d/yy")
                    }
                },
                { withMenu ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        modePill(withMenu)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { dateTimePills("M/d/yy") }
                    }
                },
            )

            // The event as it lands in the calendar: the title, then the notes.
            val dark = isSystemInDarkTheme()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (trains.isEmpty()) {
                    EventSection(dark) { Text("No train fits that time.", color = colors.appText) }
                } else {
                    EventSection(dark) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(CalendarTrip.title(destination), color = colors.appText, fontSize = 17.sp)
                            HorizontalDivider()
                            Text(CalendarTrip.location(origin), color = colors.appText, fontSize = 17.sp)
                        }
                    }
                    EventSection(dark) {
                        Text(CalendarTrip.notes(origin, destination, trains), color = colors.appText, fontSize = 17.sp)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

// A gray capsule, like iOS's compact date and time pickers.
@Composable
private fun Pill(text: String, trailing: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.iconCircleBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = colors.appText, fontSize = 17.sp, maxLines = 1, softWrap = false)
        if (trailing != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(trailing, contentDescription = null, tint = colors.appText, modifier = Modifier.size(16.dp))
        }
    }
}

// One rounded group of the event preview.
@Composable
private fun EventSection(dark: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (dark) Color(0xFF2C2C2E) else Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) { content() }
}

// Shows the first layout that fits the width, or the last one; measured without menus, shown with.
@Composable
private fun FirstThatFits(vararg layouts: @Composable (withMenu: Boolean) -> Unit) {
    SubcomposeLayout { constraints ->
        val chosen = layouts.indices.firstOrNull { i ->
            val widths = subcompose("measure$i") { layouts[i](false) }.map { it.measure(Constraints()).width }
            (widths.maxOrNull() ?: 0) <= constraints.maxWidth
        } ?: layouts.lastIndex
        val placeables = subcompose("show") { layouts[chosen](true) }.map { it.measure(constraints.copy(minWidth = 0)) }
        layout(placeables.maxOfOrNull { it.width } ?: 0, placeables.maxOfOrNull { it.height } ?: 0) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}

// The selected train's departure, or now if there's no train.
private fun initialTime(vm: TripViewModel): LocalTime {
    val trip = vm.trips.value.getOrNull(vm.offset.value)
    val minutes = (trip?.depart ?: vm.goodTimes.value.minutes) % TripViewModel.dayMinutes
    return LocalTime.of(minutes / 60, minutes % 60)
}

// The next date whose schedule matches the one on screen.
private fun initialDate(vm: TripViewModel): LocalDate {
    val trip = vm.trips.value.getOrNull(vm.offset.value)
    val isFuture = trip?.isFuture ?: false
    val wanted = if (isFuture) vm.tomorrowScheduleType else vm.scheduleType.value
    // Before 2:00 AM it's still the previous schedule day.
    val today = java.time.LocalDateTime.now().minusMinutes(SCHEDULE_DAY_START.toLong()).toLocalDate()
    for (days in (if (isFuture) 1 else 0) until 14) {
        val day = today.plusDays(days.toLong())
        val type = CaltrainSchedule.optionIndexFor(
            day.toString(),
            CalendarTrip.dotw(day.toString()),
            vm.schedule.specialDates,
        )
        if (type == wanted) return day
    }
    return today.plusDays(if (isFuture) 1 else 0)
}
