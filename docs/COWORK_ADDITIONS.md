# Cowork Session Handoff Notes

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

**Landmine**: `windowBackground` is hardcoded black for ALL modes (no `values-night`
split). Any screen that forgets `Modifier.background(colors.appBackground)` on its root
container will silently show a black background in light mode (text drawn in light-mode
colors over black look like a rendering bug, not a missing-modifier bug). HomeScreen hit
this exact issue — fixed by adding `.background(colors.appBackground)` to its root `Box`.
When adding a new screen, always set this explicitly; don't rely on the window theme.

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

### Dynamic Row Count to Fill Available Height (TripListScreen)
iOS shows as many trip rows as fit below the header — more in portrait, fewer in
landscape. Pattern used to match this:
1. Wrap the screen in `BoxWithConstraints` to get `constraints.maxHeight` (px).
2. Measure the header's actual rendered height via
   `Modifier.onGloballyPositioned { headerHeightPx = it.size.height.toFloat() }`
   on the header `Column` (don't hardcode header height — it differs by content/orientation).
3. `rowCount = ((screenHeightPx - headerHeightPx) / rowHeightPx).toInt()`, clamped to
   `[1, trips.size]`. Fall back to a fixed default (6) for the first frame before
   `headerHeightPx` is measured (it's 0 initially).
4. `rowHeightDp` is a single source of truth shared by three places that must stay in
   sync: the `rowCount` math above, `TripRow`'s actual rendered height (padding values),
   and the touch-dispatch math (`slot = (down.position.y / rowHeightPx).toInt()`) used to
   route taps to the right row. Changing `TripRow`'s padding without updating
   `rowHeightDp` desyncs tap routing from what's drawn on screen.

**Gotcha**: this is integer floor division — a near-miss ratio (e.g. 4.8 rows of space)
truncates to 4, leaving up to ~1 row of visible dead space below the last row. If you see
dead space that looks like "almost another row," shave a few dp off the header padding
or `rowHeightDp` rather than assuming the available-height calculation is broken.
Small, conservative trims (2–4dp at a time) compound fast in floor-division — verify on
device after each change rather than stacking guesses.

## Screen Status

### HomeScreen (`HomeScreen.kt`)
- **Status**: Working, tested on device. Drag selection working and persistent.
  Light mode background bug fixed (see Theme landmine note above).
- **Circle size**: 250.dp
- **Gradient**: `Color(0xFF808080)` → `appBackground`, 200.dp height at top of screen
- **Drag**: `rememberUpdatedState` fix applied; `onDragStart = markDragStart()`
- **Known issues**: none

### TripListScreen (`TripListScreen.kt`)
- **Status**: Working, tested on device. Drag selection working and persistent.
  Row layout tightened and row count tuned to match iOS density (see below).
- **Drag fix**: single `awaitEachGesture` + `rememberUpdatedState`; `markDragStart()`
  on touchSlop; `pointerInput(trips.size)` key prevents mid-drag restart
- **Row layout**: `TripRow`'s `Row` has no `fillMaxWidth()` — uses
  `Arrangement.spacedBy(16.dp)` with fixed-width columns (train ID 56.dp, depart/arrive
  times 96.dp each) so the green border hugs the content instead of stretching edge to
  edge, and times stay aligned regardless of digit count. The rows' parent `Column` has
  `horizontalAlignment = Alignment.CenterHorizontally` to center the now content-sized
  block on screen.
- **Row density**: `rowHeightDp = 42.dp` (see "Dynamic Row Count" pattern above); header
  rows use `vertical = 2.dp` padding. Currently renders ~16 rows in portrait, 5 in
  landscape — matches iOS density. If iOS gains/loses a row at some breakpoint, tune
  `rowHeightDp` and header padding together, in small increments, and retest on device.
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
- **Back button**: visible when `!isLoading && onBack != null`; hidden when loading (onBack=null). Also doubles as the loading/error screen — `MainActivity` calls it directly with `isLoading = true` for both `LoadState.Loading` and `LoadState.Error` (no separate LoadingScreen.kt)
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
  MainActivity.kt            ← NavHost, shared vm construction, delegates loading to below
  ScheduleViewModel.kt       ← rotation-surviving load state machine, once-per-day fetch cap
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

1. Light mode pass on remaining screens (HomeScreen confirmed fixed; spot-check the rest
   for the same missing-`.background(colors.appBackground)` landmine)
2. TripListScreen row tap routing — confirm tapping a row (not just drag) navigates to
   TripDetail correctly now that row width/centering changed
3. Play Store screenshots — crop workaround verified and good to upload; native
   Pixel_8 1080x1920 boot fix still open but low priority (see "Play Store" section)

## Play Store

iOS app releasing separately; both share the same schedule JSON at
`https://next-caltrain-pwa.appspot.com/schedule.json`.

### Screenshots — WORKAROUND IN USE (native emulator resolution still unresolved)

Captured screenshots via `./snap.sh` were originally rejected by Play Store
requirements on two counts:

1. **Aspect ratio out of range.** The `Pixel_8` AVD's default resolution is
   1080x2400 (20:9 ≈ 2.22 height/width ratio). Play's cap is 16:9, which for a
   1080px-wide screen means a max height of **1920** (1080 × 16/9 = 1920 exactly
   — matches `docs/SCREENSHOTS.md`'s "Recommended phone size: 1080 x 1920").
2. **Alpha channel present.** `adb exec-out screencap -p` outputs RGBA PNGs even
   though the content is fully opaque. Play Console rejects screenshots that
   carry an alpha channel.

**Current workaround**: capture at the AVD's native 1080x2400 via `./snap.sh`,
then manually crop each image down to 1080x1920. Verified on `pics/1920_HomeScreen.png`,
`pics/1920_TripListScreen.png`, `pics/1920_TripDetailScreen.png` — all exactly
1080x1920, RGB (no alpha channel), status bar intact at the top of the crop. This
satisfies Play Store's aspect-ratio and alpha-channel rules and covers the
≥2-screenshot minimum for the listing, so the cropped images are good to upload
as-is.

**Still open**: get the Pixel_8 AVD itself booting at 1080x1920 natively, so
`snap.sh` doesn't need a manual crop step. Not urgent (the workaround unblocks the
listing) but worth finishing so future screenshots don't need hand-cropping.
Landmines hit so far attempting the native fix:

- Editing `hw.lcd.height` in `~/.android/avd/Pixel_8.avd/config.ini` silently
  does nothing if its `key=value` formatting doesn't match the rest of the
  file. The emulator's ini parser appears to do an exact string match on the
  key with no whitespace trimming — `hw.lcd.height = 1920` (spaces around `=`)
  does not match `hw.lcd.height` and falls back to the device default (2400),
  while `hw.lcd.density=420` and `hw.lcd.width=1080` (no spaces, untouched)
  work fine. **Always write `hw.lcd.height=1920` with no spaces**, matching the
  other lines exactly.
- `config.ini` being correct doesn't guarantee the runtime picked it up —
  `hardware-qemu.ini` (auto-generated fresh on each boot, not meant to be
  hand-edited) is what's actually used. Cross-check the boot log's
  `Setting display: 0 configuration to: ...` line, not just `config.ini`.
- The `-no-skin` launch flag is obsolete/ignored in current emulator versions
  (prints a warning, does nothing) — don't rely on it.
- Must fully quit the running emulator (Cmd+Q the `qemu-system-aarch64`
  process, or `adb -s emulator-5554 emu kill`) before relaunching — bringing an
  already-running window back to front just shows the stale resolution, and
  launching a second instance of the same AVD fails outright ("Running
  multiple emulators with the same AVD is an experimental feature").

**Status as of last session:** `config.ini` now correctly reads
`hw.lcd.height=1920` (verified via `grep -n "lcd" ~/.android/avd/Pixel_8.avd/config.ini`,
no stray spaces). Cold-booted with:
```bash
~/Library/Android/sdk/emulator/emulator -avd Pixel_8 -no-snapshot-load
```
**Not yet confirmed**: whether the boot log's `Setting display: 0 configuration
to:` line now reads `1080x1920` (still waiting on that output).

**Next steps (low priority — listing is already unblocked by the crop workaround):**
1. Confirm the boot log shows `1080x1920`, not `1080x2400`.
2. Recapture with `./snap.sh` and confirm dimensions on the output PNG
   (`sips -g pixelWidth -g pixelHeight <file>` or similar) — if native capture
   already comes out 1080x1920, the manual crop step can be dropped.
3. Check whether the alpha channel issue still applies to a fresh native capture;
   if so, add a strip-alpha step to `snap.sh` and re-verify.

## iOS Reference

Source browsable via GitHub API:
`https://api.github.com/repos/woodie/next-caltrain-swift/contents/Sources/NextCaltrain`
