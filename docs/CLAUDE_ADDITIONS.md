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

### Track Line in TripDetailScreen
`fillMaxHeight()` inside a Row with wrap-content height does nothing. Fix: give the Row
a fixed `.height(rowHeightDp)` and use `Canvas(Modifier.width(dotSize).fillMaxHeight())`
— Canvas then gets an explicit height and the line fills correctly.

## Screen Status

### HomeScreen (`HomeScreen.kt`)
- **Status**: Working, tested on device.
- **Circle size**: 250.dp
- **Gradient**: `Color(0xFF808080)` → `appBackground`, 200.dp height at top of screen
- **Drag**: accumulation fix applied (`accumulatedDrag` state, reset on drag end)
- **Known issues**: none outstanding after last fix

### TripListScreen (`TripListScreen.kt`)
- **Status**: Built; column alignment and drag fix applied in latest session (untested).
- **Drag fix**: single `awaitEachGesture` in Column pointerInput handles both tap routing
  and drag; removed `detectTapGestures` from `TripRow`
- **Alignment fix**: `TripRow` now `fillMaxWidth()` with `Arrangement.SpaceBetween`;
  rows span full width with `padding(horizontal = 16.dp)` on the Column
- **To verify**: drag changes selected trip; tapping a row navigates to TripDetail

### TripDetailScreen (`TripDetailScreen.kt`)
- **Status**: Built; connecting line and title centering applied in latest session (untested).
- **Title**: centered (back button + weight spacer + title + weight spacer + balance spacer)
- **Track line**: Canvas with fixed `rowHeightDp = 48.dp`; draws line above/below dot
  using `isFirst`/`isLast` flags; dot drawn at `dotOffsetYDp + dotR` from row top
- **Text**: always `colors.appText` (white) — iOS screenshot showing blue text is wrong
- **Dot colors**: ORIGIN/DESTINATION/TRANSFER = white; PAST = calPast; FUTURE = calArrive
- **To verify**: line is continuous between all stops; title is centered; correct dot colors

### StationSelectionScreen (`StationSelectionScreen.kt`)
- **Status**: Built; NOT yet tested on device.
- **Layout**: portrait = stacked Column; landscape = side-by-side Row (via `LocalConfiguration`)
- **Scroll**: `LazyColumn` with `rememberLazyListState` + `animateScrollToItem` to selected
- **Save**: `AlertDialog` confirmation; checkmark button only shown when `!isAlreadyDefault`
- **To verify**: morning/evening lists scroll to selected station; save persists across app restart

### AboutScreen (`AboutScreen.kt`)
- **Status**: Working, tested on device.
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

1. `TripListScreen` — drag to scroll trips, tap to open detail
2. `TripDetailScreen` — connecting line, centered title, correct dot colors
3. `StationSelectionScreen` — select stations, save, confirm persistence after restart
4. Light mode pass on all screens

## Play Store

Screenshots pending — waiting until all screens are polished.
iOS app releasing separately; both share the same schedule JSON at
`https://next-caltrain-pwa.appspot.com/schedule.json`.

## iOS Reference

Source browsable via GitHub API:
`https://api.github.com/repos/woodie/next-caltrain-ios/contents/Sources/NextCaltrain`
