package com.netpress.nextcaltrain

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netpress.nextcaltrain.ui.theme.AppStyle
import com.netpress.nextcaltrain.ui.theme.LocalAppColors

@Composable
fun StationSelectionScreen(
    vm: TripViewModel,
    onNavigateBack: () -> Unit,
) {
    val origin by vm.origin.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    val stations = vm.schedule.southStops
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    var showSaveConfirm by remember { mutableStateOf(false) }

    // morningStation / eveningStation derived from isFlipped
    val morningStation = if (vm.isFlipped) destination else origin
    val eveningStation = if (vm.isFlipped) origin else destination
    val isAlreadyDefault = vm.isAlreadyDefaultStops()

    if (showSaveConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveConfirm = false },
            title = { Text("Station Defaults", color = colors.appText) },
            text = {
                Text(
                    "Save $morningStation as morning and $eveningStation as evening default stations?",
                    color = colors.appText,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.saveStops()
                    showSaveConfirm = false
                    onNavigateBack()
                }) {
                    Text("Save", color = colors.calArrive)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveConfirm = false
                    onNavigateBack()
                }) {
                    Text("Cancel", color = colors.appText)
                }
            },
            containerColor = colors.appBackground,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
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
            if (!isAlreadyDefault) {
                // Grouped in one pill (matching iOS's paired restore+save buttons)
                // instead of two separate circles.
                Row(
                    modifier = Modifier
                        .height(AppStyle.iconButtonSizeDp.dp)
                        .clip(RoundedCornerShape(AppStyle.iconButtonSizeDp.dp / 2))
                        .background(colors.iconCircleBackground),
                ) {
                    IconButton(
                        onClick = { vm.restoreDefaultStops() },
                        modifier = Modifier.size(AppStyle.iconButtonSizeDp.dp),
                    ) {
                        // Refresh is drawn clockwise; mirror it horizontally so it
                        // reads as counterclockwise, matching iOS's arrow.counterclockwise.
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Restore defaults",
                            tint = colors.appText,
                            modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
                        )
                    }
                    IconButton(
                        onClick = { showSaveConfirm = true },
                        modifier = Modifier.size(AppStyle.iconButtonSizeDp.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save", tint = colors.calArrive)
                    }
                }
            }
        }

        // Station lists — portrait: stacked; landscape: side by side
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                StationList(
                    title = "Morning Station",
                    stations = stations,
                    selected = morningStation,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onSelect = { vm.setMorningStation(it) },
                )
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = colors.calSwapped,
                )
                StationList(
                    title = "Evening Station",
                    stations = stations,
                    selected = eveningStation,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onSelect = { vm.setEveningStation(it) },
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                StationList(
                    title = "Morning Station",
                    stations = stations,
                    selected = morningStation,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onSelect = { vm.setMorningStation(it) },
                )
                Divider(color = colors.calSwapped, modifier = Modifier.padding(vertical = 4.dp))
                StationList(
                    title = "Evening Station",
                    stations = stations,
                    selected = eveningStation,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onSelect = { vm.setEveningStation(it) },
                )
            }
        }
    }
}

@Composable
private fun StationList(
    title: String,
    stations: List<String>,
    selected: String,
    colors: com.netpress.nextcaltrain.ui.theme.AppColors,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val selectedIndex = stations.indexOf(selected).coerceAtLeast(0)

    // Center the selected row in the viewport, matching iOS's
    // `proxy.scrollTo(station, anchor: .center)`. LazyListState has no anchor concept,
    // so we jump to the item if it's off-screen (to make it measurable), then read its
    // real position/size from layoutInfo and animate the precise correction to center it.
    LaunchedEffect(selected) {
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == selectedIndex }) {
            listState.scrollToItem(selectedIndex)
        }
        val info = listState.layoutInfo
        val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        if (itemInfo != null) {
            val viewportCenter = (info.viewportEndOffset - info.viewportStartOffset) / 2
            val itemCenter = itemInfo.offset + itemInfo.size / 2
            listState.animateScrollBy((itemCenter - viewportCenter).toFloat())
        }
    }

    Column(modifier = modifier.fillMaxHeight()) {
        // Section header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.appBackground)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                fontSize = AppStyle.fontOrigin,
                color = colors.calArrive,
            )
        }

        // BoxWithConstraints lets us compute start padding relative to the panel width
        // so portrait and landscape panels both center the content block correctly,
        // with a +12dp rightward bias matching iOS's fixed `.offset(x: 12)`.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val startPad = ((maxWidth - 200.dp) / 2 + 12.dp).coerceAtLeast(12.dp)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(stations) { index, station ->
                    if (index > 0) {
                        Divider(color = colors.calSwapped.copy(alpha = 0.4f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(station) { detectTapGestures { onSelect(station) } }
                            .padding(start = startPad, end = 16.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                            if (station == selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = colors.calArrive,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Text(
                            text = station,
                            fontSize = AppStyle.fontOrigin,
                            color = colors.appText,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}
