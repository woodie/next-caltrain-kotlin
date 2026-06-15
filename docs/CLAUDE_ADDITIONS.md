# Claude Session Handoff Notes

This file captures project context for AI-assisted sessions, supplementing DEVELOPMENT.md.

## Current Status (June 2026)

All five screens are implemented and navigable. The app builds and runs. Outstanding
issues are UI polish — tracked below screen by screen.

## Architecture Decisions

### Theme
`Theme.AppCompat.DayNight.NoActionBar` in `themes.xml` (not Material3 XML — those
aren't available without a separate XML theme dep). AppCompat is declared explicitly
in `gradle/libs.versions.toml` (`appcompat = "1.7.0"`). True black in dark mode via
`<item name="android:windowBackground">@android:color/black</item>`.

### ViewModel
`TripViewModel(schedule, context)` is constructed once at the activity level via
`TripViewModelFactory` and passed to all NavHost composables. Do NOT call `viewModel()`
inside individual screens — use the `vm` parameter.

### SharedPreferences
Keys: `stopAM` (Int, default 15 = Palo Alto), `stopPM` (Int, default 0 = San Francisco).
`isFlipped` = hour >= 12. Morning station = origin when not flipped, destination when flipped.
Matches iOS UserDefaults pattern exactly.

### Navigation
`rememberNavController()` in `MainActivity`. Routes: `home`, `tripList`, `tripDetail`,
`stationSelection`, `about`. `tripDetail` is driven by `vm.tripDetailState` (set by
`vm.selectTripForDetail(trip)` before navigating).

### Drag Gesture Pattern
`detectDragGestures` on a parent container conflicts with `detectTapGestures` on child
rows — the child consumes pointer events first and the parent never sees a drag start.
**Fix**: use `awaitEachGesture` + `awaitFirstDown` in a single `pointerInput` on the
parent; accumulate `positionChange().y`; distinguish tap vs drag by comparing total
displacement to `viewConfiguration.touchSlop`. Commits offset on drag end, routes tap
to the tapped row by computing `slot = (down.position.y / rowHeightPx).toInt()`.

### Drag Selection Persistence (`rememberUpdatedState`)
`pointerInput` lambdas capture their closure at creation time. `offset` and
`effectiveOffset` are plain `Int` values — they go stale inside the lambda even when the
underlying StateFlow updates. Calling `vm.setOffset(effectiveOffset)` at drag end was
sending the original next-train index, causing snap-back on release.

**Fix**: `rememberUpdatedState(offset)` / `rememberUpdatedState(trips)` gives the lambda
a `State<T>` holder that always reflects the latest composable value without restarting
the coroutine. At drag end, read `latestOffset.value + dragShift` (live) instead of the
captured stale `effectiveOffset`.

Additionally: `vm.markDragStart()` (sets `userSelected = true`) is called the moment
`touchSlop` is exceeded. This prevents the 1-second timer (`updateNextIndex`) from
resetting `_offset` mid-drag, which would have shifted the drag baseline unexpectedly.
The `pointerInput` key is `(trips.size)` only — not `offset` — so the coroutine is never
cancelled mid-drag by an offset change.

### Track Line in TripDetailScreen
`fillMaxHeight()` inside a Row with wrap-content height does nothing. Fix: give the Row
a fixed `.height(rowHeightDp)` and use `Canvas(Modifier.width(dotSize).fillMaxHeight())`
— Canvas then gets an explicit height and the line fills correctly.

### Ragged-Right Centering Pattern
Several screens display a content block where the left side is uniform (times, checkmarks)
and the right side is ragged (station names of varying length). The goal is to center the
block visually, with a slight rightward bias so the ragged right edge sits close to the
screen edge rather than leaving dead space.

**For fixed-layout screens (TripDetailScreen):**
1. Outer `Column` uses `horizontalAlignment = Alignment.CenterHorizontally`.
2. Inner `Column` wraps all rows with `Modifier.padding(start = N.dp)` — this biases the
   centered block rightward. A positive start pad shifts the block right of true center.
3. Each `StopRow` is naturally sized (no `fillMaxWidth`), so the inner Column shrinks to
   the widest row. All rows start from the same left edge → dots/checkmarks stay aligned.
4. Station name `Text` uses `softWrap = false, overflow = TextOverflow.Visible` — do NOT
   use `overflow = Ellipsis` here, as that causes `IntrinsicSize.Min` to collapse to zero.

**For `LazyColumn` screens (StationSelectionScreen):**
`LazyColumn` doesn't support `horizontalAlignment`, so use `BoxWithConstraints` to read
the actual panel width, then compute a dynamic start padding:
```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val startPad = ((maxWidth - 200.dp) / 2 + 8.dp).coerceAtLeast(8.dp)
    LazyColumn(...) { ... }
}
```
`200.dp` is the nominal content block width (checkmark box + longest station name).
This self-adjusts for portrait vs landscape panels without any hardcoded breakpoints.

## Screen Status

### HomeScreen (`HomeScreen.kt`)
- **Status**: Working, tested on device. Drag selection working and persistent.
- **Circle size**: 250.dp
- **Gradient**: `Color(0xFF808080)` → `appBackground`, 200.dp height at top of screen
- **Drag**: `rememberUpdatedState` fix applied; `onDragStart = markDragStart()`
- **Known issues**: none

### TripListScreen (`TripListScreen.kt`)
- **Status**: Working, tested on device. Drag selection working and persistent.
- **Drag fix**: single `awaitEachGesture` + `rememberUpdatedState`; `markDragStart()`
  on touchSlop; `pointerInput(trips.size)` key prevents mid-drag restart
- **Alignment**: `TripRow` `fillMaxWidth()`, `Arrangement.SpaceBetween`,
  `padding(horizontal = 16.dp)` on Column
- **To verify**: tapping a row navigates to TripDetail (tap routing exists, unconfirmed)

### TripDetailScreen (`TripDetailScreen.kt`)
- **Status**: Working, tested on device. Layout matches iOS.
- **Title**: centered (back button + weight spacer + title + weight spacer + balance spacer)
- **Track line**: Canvas with fixed `rowHeightDp = 30.dp`; `dotSizeDp = 16.dp`;
  `dotOffsetYDp = 7.dp` ((30-16)/2); draws line above/below dot using `isFirst`/`isLast` flags
- **Alignment**: ragged-right centering pattern (see Architecture Decisions above);
  `padding(start = 16.dp)` on inner Column biases block rightward
- **Text**: always `colors.appText` (white); station names use `TextOverflow.Visible`
- **Dot colors**: ORIGIN/DESTINATION/TRANSFER = white; PAST = calPast; FUTURE = calArrive
- **Known issues**: none

### StationSelectionScreen (`StationSelectionScreen.kt`)
- **Status**: Working, tested on device.
- **Layout**: portrait = stacked Column; landscape = side-by-side Row (via `LocalConfiguration`)
- **Scroll**: `LazyColumn` with `rememberLazyListState` + `animateScrollToItem` to selected
- **Save**: `AlertDialog` confirmation; checkmark button only shown when `!isAlreadyDefault`
- **Item layout**: `BoxWithConstraints` dynamic start padding `(maxWidth - 200.dp) / 2 + 8.dp`;
  checkmark in a `32.dp` Box to the left of the station name; self-adjusts portrait vs landscape
- **Known issues**: none

### AboutScreen (`AboutScreen.kt`)
- **Status**: Working, tested on device.
- **Back button**: visible when `!isLoading && onBack != null`; hidden on LoadingScreen (onBack=null)
- **Background**: Box has `background(colors.appBackground)`; button has `background(colors.iconCircleBackground)`
- **Known issues**: none

## Files of Interest

```
app/src/main/java/com/netpress/nextcaltrain/
  HomeScreen.kt              ← circle drag, gradient
  TripListScreen.kt          ← row list, drag+tap gesture (awaitEachGesture)
  TripDetailScreen.kt        ← Canvas track line, stop roles
  StationSelectionScreen.kt  ← LazyColumn pickers, save/restore logic
  AboutScreen.kt             ← schedule date display
  MainActivity.kt            ← NavHost, shared vm construction
  TripViewModel.kt           ← SharedPrefs, TripDetailState, swapStations, setOffset
  TripViewModelFactory.kt    ← takes (schedule, context)

app/src/main/res/values/themes.xml   ← DayNight theme, black windowBackground
gradle/libs.versions.toml           ← appcompat = "1.7.0"
app/build.gradle.kts                ← implementation(libs.androidx.appcompat)

docs/DEVELOPMENT.md       ← build/run/test instructions
docs/PLAY_STORE_LISTING.md
build.sh / run.sh / test.sh
```

## Testing Order for Next Session

1. Light mode pass on all screens
2. Play Store screenshots

## Play Store

Screenshots pending — waiting until all screens are polished.
iOS app releasing separately; both share the same schedule JSON at
`https://next-caltrain-pwa.appspot.com/schedule.json`.

## iOS Reference

Source browsable via GitHub API:
`https://api.github.com/repos/woodie/next-caltrain-ios/contents/Sources/NextCaltrain`
