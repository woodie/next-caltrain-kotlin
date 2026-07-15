# Comments

Rationale, history, and design notes that used to live as multi-line comments
in the source. Organized by file, then by the type, property, or function
each note is attached to. The source itself now carries at most one short
line at any given spot -- anything longer that would previously have been a
`/** ... */` KDoc block or a multi-line `//` note lives here instead. When a
code location kept its own one-line comment, it's noted below so this stays
a complete map of "why," not a duplicate of what's already readable in the
file.

## app/src/main/java/com/netpress/nextcaltrain/GoodTimes.kt

### `GoodTimes` (class)
Kept a one-line comment in place: "Central time utility: subtracts 2 hours
so trains past midnight stay on "today's" schedule."

Full history: subtracts 2 hours from the real clock so that trains running
past midnight (e.g. 24:05 = 1445 minutes) still appear on "today's" schedule
rather than rolling to tomorrow at midnight.

### `GoodTimes.debugOverrideMinutes` / `GoodTimes.debugOverrideDotw`
Kept a one-line comment on each: "DEBUG OVERRIDE — set to pin time-of-day
for testing (minutes since midnight, e.g. 330 = 5:30am)." and "0 = Sunday
... 6 = Saturday. Set to null for normal behavior." Both null by default;
setting either pins `GoodTimes()`'s output for deterministic manual testing
without touching the real clock or calendar.

### `GoodTimes.scheduleDateFor(epochMillis)`
Kept a one-line comment in place: "Schedule-day string for an instant, using
the same 2am-boundary rule as invoke()."

Full history: returns the yyyy-MM-dd schedule-day string for an arbitrary
instant, using the same "day starts at 2am" rule as `invoke()`: subtract 2
hours before formatting. Lets callers compare "today" against a stored
last-fetch timestamp for the once-per-day schedule fetch policy (see
`Schedule.fetchedToday`).

## app/src/main/java/com/netpress/nextcaltrain/CaltrainSchedule.kt

### `CaltrainSchedule.optionIndexFor(date, dotw, specialDates)`
Kept a one-line comment in place: "Factored out (rather than inlined into
forToday) so tomorrow's schedule type can be computed independently."

Full history: returns the schedule type for a given date string and
day-of-week, consulting `specialDates` first, then falling back to
dotw-based logic. Factored out of `forToday` specifically so `forTomorrow`
can call the exact same logic against tomorrow's date/dotw instead of
duplicating it.

## app/src/main/java/com/netpress/nextcaltrain/Schedule.kt

### `Schedule` (class)
Kept a one-line comment in place: "Holds the parsed schedule.json data;
uses Android's built-in JSONObject, no third-party JSON lib."

Full history: mirrors the iOS `Schedule` struct. Uses Android's built-in
`JSONObject` rather than a third-party library (Gson, Moshi, kotlinx.serialization)
specifically to keep dependencies minimal for what's a small, stable JSON shape.

### `Schedule.PREFS_NAME`, `Schedule.KEY_LAST_FETCH_MS`
Kept a one-line comment in place: "internal (not private) so tests can key
a fake SharedPreferences the same way as production."

Full history: `internal` rather than `private` specifically so unit tests
(see `ScheduleSpec`) can build a fake `SharedPreferences` keyed the same way
production code keys it, instead of hardcoding the same magic strings a
second time in the test file where they could drift out of sync.

### `Schedule.fetchedToday(context, nowMillis)`
Kept a one-line comment in place: "True if the last successful fetch landed
on today's schedule-day (2am boundary); skips redundant fetches."

Full history: true if the last successful network fetch landed on today's
schedule-day (2am boundary, see `GoodTimes.scheduleDateFor`). Used to skip
redundant network calls once the app already has today's data.
`nowMillis` defaults to the real clock; tests pass a fixed value so the
2am-boundary comparison is deterministic regardless of when the suite runs.

## app/src/main/java/com/netpress/nextcaltrain/CaltrainService.kt

### `Leg` (data class)
No comment kept in source; the class is judged self-explanatory now (name,
fields, and the `// minutes since midnight` inline note on `depart` already
say what's needed). History: models a single leg of a trip -- one train,
one boarding station, one depart time.

### `Trip` (data class)
No comment kept in source; the class is judged self-explanatory now.
History: a trip with one or two legs. Two legs means a transfer at San Jose
Diridon (see `CaltrainService.transferStation`).

### `CaltrainService` (class)
No comment kept in source; the class is judged self-explanatory now.
History: computes train routes, including transfers for South County
service. Mirrors iOS `CaltrainService.swift`.

## app/src/main/java/com/netpress/nextcaltrain/ScheduleViewModel.kt

### `LoadState` (sealed class)
Kept a one-line comment in place: "Schedule loading state machine; see
docs/COMMENTS.md for the three cases this walks through."

Full history: three cases the state machine walks through --
- Already fetched today (2am boundary) and cache exists: use the cache, no
  network call at all.
- No cache: block on a network fetch. Success -> `Ready`. Failure ->
  `Error` (retried the next time the app/process is opened).
- Cache exists but not fetched today: race a fetch against a 10s timeout.
  Winner -> `Ready`. Loser/failure -> fall back to the stale cache (retried
  the next time the app/process is opened, since `fetchedToday()` stays
  false until a fetch actually succeeds).

### `ScheduleViewModel` (class)
Kept a one-line comment in place: "Hosted in a ViewModel (not Compose
`remember`) so the load sequence survives Activity recreation on rotation."

Full history: without this, rotating the screen would tear down the whole
load sequence and re-flash "Loading schedule data" every time, even though
the app already had a perfectly good schedule loaded.

### `ScheduleViewModel.ensureLoaded(context)`
Kept a one-line comment in place: "Safe to call on every recomposition —
the load sequence only actually runs once per instance."

Full history: safe to call on every recomposition (e.g. after rotation
re-creates the Activity) because the `started` guard means the load
sequence only actually runs once per `ScheduleViewModel` instance.

## app/src/main/java/com/netpress/nextcaltrain/TripViewModel.kt

### `TripViewModel.markDragStart()`
Kept a one-line comment in place: "Called as soon as drag is detected —
prevents the 1-second timer from resetting offset mid-drag."

Full history: called as soon as a drag is detected (touchSlop exceeded) so
the 1-second timer (`updateNextIndex`, which runs via `startTimer()`) can't
reset `_offset` mid-drag -- that reset would shift the drag baseline
unexpectedly and cause a visible jump while the user's finger is still
moving. See also `docs/COWORK_ADDITIONS.md`'s "Drag Selection Persistence"
section, which covers the matching `rememberUpdatedState` fix in
`TripListScreen`/`HomeScreen` that this call pairs with.

## app/src/main/java/com/netpress/nextcaltrain/MainActivity.kt

### `NextCaltrainApp()`, `LaunchedEffect(Unit)` call to `ensureLoaded`
Kept a one-line comment in place: "applicationContext, not context: the
fetch coroutine can run up to 10s and must outlive the Activity."

Full history: `ensureLoaded()` no-ops after its first call, so this
`LaunchedEffect` is safe to re-run after rotation recreates the
Activity/composition -- it won't re-trigger the fetch or flash the loading
screen again. `context.applicationContext` (not the Activity `context`
itself) is passed specifically so the coroutine, which can run for up to
10s (see `ScheduleViewModel`'s race-against-timeout case), never holds a
reference to a destroyed Activity if rotation or navigation tears it down
mid-fetch.

## app/src/main/java/com/netpress/nextcaltrain/HomeScreen.kt

### `HomeScreen`, `latestOffset`/`latestTrips` (`rememberUpdatedState`)
Kept a one-line comment in place: "Live references so the pointerInput
lambda (key=Unit, never restarts) reads current offset/trips."

Full history: the drag gesture's `pointerInput(Unit)` lambda is created
once and never restarts, so without `rememberUpdatedState` it would keep
reading the `offset`/`trips` values captured at that first composition
instead of the current ones. This is the same stale-closure problem
`docs/COWORK_ADDITIONS.md`'s "Drag Selection Persistence" section
describes for `TripListScreen`.

### `HomeScreen`, gradient background `Box`
Kept a one-line comment in place: "Gradient background — height(100.dp)
matches iOS's actual fade extent; see docs/COMMENTS.md."

Full history: iOS draws this as `LinearGradient(startPoint: .top, endPoint:
.center)` inside a 200pt frame -- `.top`/`.center` are fractional anchors,
so the fade completes by the frame's halfway point (100pt); the rest is
flat `appBackground`. Compose's `verticalGradient` with no `startY`/`endY`
spans the box's full height, so matching `height(200.dp)` here made the
fade twice as tall as iOS. `height(100.dp)` reproduces the same pixels: the
visible fade is the only part of the box that ever mattered. `TripListScreen`
applies the identical fix for the same reason.

### `HomeScreen`, toolbar `Row`'s `.windowInsetsPadding` modifier
Kept a one-line comment in place: "Top + horizontal insets only: landscape
3-button nav can land on a side edge too."

Full history: the status bar covers the top edge, but the navigation bar
can also land on the left/right edge instead of the bottom (e.g. 3-button
nav in landscape), which would otherwise cover this row's trailing swap
icon. Padding for top + horizontal avoids adding an unnecessary bottom
inset in portrait, where only the top matters.

### `HomeScreen`, Refresh icon `Modifier.scale`
Kept a one-line comment in place: "Icon flips horizontally: clockwise art
reads as iOS's counterclockwise arrow."

Full history: the Material Refresh glyph is drawn clockwise; mirroring it
horizontally (`scaleX = -1f`) makes it read as counterclockwise, matching
iOS's `arrow.counterclockwise` SF Symbol used for the same reset action.
The identical fix and comment appear in `TripListScreen` and
`StationSelectionScreen` for their own Refresh/reset icons.

## app/src/main/java/com/netpress/nextcaltrain/StationSelectionScreen.kt

### `StationSelectionScreen`, restore+save button pill `Row`
Kept a one-line comment in place: "One pill grouping both buttons, matching
iOS's paired restore+save buttons."

Full history: the restore-defaults and save buttons are grouped in one
rounded pill (matching iOS's paired restore+save buttons) instead of two
separate circles.

### `StationSelectionScreen`, Refresh icon `Modifier.scale`
Kept a one-line comment in place: "Icon flips horizontally: clockwise art
reads as iOS's counterclockwise arrow." See `HomeScreen`'s note above for
the full rationale -- this is the same fix applied to the restore-defaults
button here.

### `StationList`, `LaunchedEffect(selected)` scroll-to-center logic
Kept a one-line comment in place: "Centers the selected row, matching iOS's
scrollTo(anchor: .center); see docs/COMMENTS.md."

Full history: centers the selected row in the viewport, matching iOS's
`proxy.scrollTo(station, anchor: .center)`. `LazyListState` has no anchor
concept, so this jumps to the item first if it's off-screen (to make it
measurable), then reads its real position/size from `layoutInfo` and
animates the precise correction needed to center it.

### `StationList`, `BoxWithConstraints` start-padding calc
Kept a one-line comment in place: "Start padding computed from panel width,
+12dp rightward bias matching iOS's fixed offset(x: 12)."

Full history: `BoxWithConstraints` lets the code compute start padding
relative to the actual panel width so portrait and landscape panels both
center the content block correctly, with a +12dp rightward bias matching
iOS's fixed `.offset(x: 12)`.

## app/src/main/java/com/netpress/nextcaltrain/TripDetailScreen.kt

### `TripDetailScreen`, stop list `Column`
Kept a one-line comment in place: "Stop list centered as a block, matching
iOS; ragged right edge is expected, left as-is."

Full history: centered as a block, matching iOS, with no rightward bias --
the ragged right edge produced by varying station-name lengths is expected
and intentionally left as-is (contrast with `StationSelectionScreen`'s
deliberate rightward bias, a different layout with a different visual
target).

### `TripDetailScreen`, `stopLineColor` inside `stops.forEachIndexed`
Kept a one-line comment in place: "Time-based, not role-based:
origin/destination dots stay white regardless."

Full history: line colors are time-based (not role-based) so
origin/destination dots show white while their connecting segment colors
still reflect past/future travel correctly.

## app/src/main/java/com/netpress/nextcaltrain/TripListScreen.kt

### `TripListScreen`, `measuredRowHeightPx`
Kept a one-line comment in place: "Real rendered TripRow height, used for
tap/drag slot math instead of the hand-tuned rowHeightPx estimate."

Full history: `rowHeightPx` (derived from the hardcoded `rowHeightDp = 42.dp`)
is a hand-tuned estimate, only used for the `rowCount` layout calc where no
row exists yet to measure. If it ever drifts from what `TripRow` actually
renders (font scaling, a padding tweak), tap routing computed from it would
desync from what's on screen, and the desync gets worse for higher slot
indices. Measuring the real thing (`measuredRowHeightPx`, fed back from the
first rendered `TripRow`'s `onGloballyPositioned`) removes that whole class
of bug. See also `docs/COWORK_ADDITIONS.md`'s "Dynamic Row Count to Fill
Available Height" section.

### `TripListScreen`, gradient background `Box`
Kept a one-line comment in place: "Gradient background — same iOS
fade-extent fix as HomeScreen; see docs/COMMENTS.md." Full rationale is
identical to `HomeScreen`'s gradient background note above: iOS's fade
completes by the frame's fractional halfway point, so `height(100.dp)`
(not 200.dp) is what reproduces the same visible pixels.

### `TripListScreen`, Refresh icon `Modifier.scale` (reset button)
Kept a one-line comment in place: "Icon flips horizontally: clockwise art
reads as iOS's counterclockwise arrow." Same fix and rationale as
`HomeScreen`'s note above, applied to the trip-list header's reset button.

### `TripListScreen`, `latestOffset`/`latestTrips` (`rememberUpdatedState`)
Kept a one-line comment in place: "Live reference so the gesture lambda
below sees current offset/trips without restarting."

Full history: `rememberUpdatedState` gives the drag/tap gesture lambda a
live reference to `offset`/`trips` without restarting the coroutine on
every recomposition -- the lambda is long-lived (keyed on `trips.size`, see
below) and would otherwise keep reading stale values captured at the point
the `pointerInput` block was first created.

### `TripListScreen`, `.pointerInput(trips.size)` key
Kept a one-line comment in place: "Key is trips.size only (not offset) —
prevents restarting mid-drag when offset updates."

Full history: keying on `trips.size` only, not `offset`, prevents the
gesture-handling coroutine from restarting mid-drag whenever the 1-second
timer or `vm.setOffset()` updates `offset` -- a restart mid-gesture would
drop the in-progress drag.

### same gesture block, touchSlop-exceeded branch
Kept a one-line comment in place: "Mark immediately so the 1-second timer
can't reset offset mid-drag." Same rationale as `TripViewModel.markDragStart()`'s
note above -- this is the call site.

### same gesture block, `dragShift` write guard
Kept a one-line comment in place: "Only write State on change — avoids a
recomposition per pointer event (60-120/sec)."

Full history: pointer-move events fire at 60-120/sec while dragging; only
writing the `dragShift` `State` when the computed shift actually changes
(rather than on every event) avoids triggering a recomposition that often
while the finger is still moving within the same row.

### same gesture block, drag-end `finalOffset`
Kept a one-line comment in place: "Use latestOffset.value (live), not a
stale captured Int, to avoid snapping back."

Full history: reads `latestOffset.value` (live, via `rememberUpdatedState`)
rather than a captured `effectiveOffset` (a stale `Int` from composition
time) specifically to avoid the selection snapping back to an earlier
value on release.

### `TripListScreen`, `TripRow`'s `modifier` for `slot == 0`
Kept a one-line comment in place: "Measures the first row's real height and
feeds it back into measuredRowHeightPx above." This is the write side of
the `measuredRowHeightPx` note above -- see that entry for the full
rationale.

### `TripRow`, root `Row`
Kept a one-line comment in place: "No fillMaxWidth: border hugs content;
fixed column widths keep times aligned regardless of digit count."

Full history: the row hugs its content (no `fillMaxWidth`) so the border
tightly wraps the columns instead of stretching edge to edge; fixed column
widths (train ID 56.dp, depart/arrive times 96.dp each) keep times aligned
down the list regardless of digit count, e.g. "9:55" vs "12:55".

## app/src/test/java/com/netpress/nextcaltrain/SpecFixtures.kt

### `SpecFixtures` (object)
Kept a one-line comment in place: "Factory for building Schedule fixtures
for specs; see docs/COMMENTS.md for the station layout."

Full history: mirrors iOS `SpecFixtures.swift` exactly. Fixture layout: 16
stations total, San Francisco at index 0, San Jose Diridon at index 7,
Morgan Hill at index 14, Gilroy at index 15. Electric trains run SF <-> SJD
only; diesel/South-County trains run SJD <-> Gilroy only -- matching the
real network's split between electrified Caltrain service and the diesel
South County extension.

## app/src/test/java/com/netpress/nextcaltrain/ProjectConfig.kt

### `ProjectConfig` (object)
Kept a one-line comment in place: "Pins spec/test execution order so
full-suite output is reproducible; see docs/COMMENTS.md."

Full history: tests *within* a spec already run sequentially (declaration
order) by Kotest's own default. But ordering *between* spec classes
(`CaltrainScheduleSpec`, `GoodTimesSpec`, `ScheduleSpec`, etc.) is undefined
by default -- Kotest's docs call it "essentially random, in whatever order
the discovery mechanism finds them"
(https://kotest.io/docs/5.9.x/framework/spec-ordering.html). That's what
made a full `./test.sh` run hard to diff section-by-section against the
Swift sibling's output: each spec's own test order was already stable, but
which spec printed first/second/etc. could shuffle between runs.

Pins both explicitly so output is reproducible run over run:
- `specExecutionOrder`: `Lexicographic` (alphabetical by class name),
  matching the Swift sibling (xcodebuild/XCTest there runs alphabetically
  by default; see that repo's `project.yml` for the analogous
  `randomExecutionOrder: false` pin).
- `testCaseOrder`: `Sequential` (declaration order) -- already Kotest's
  default, pinned here so it can't silently change on a future Kotest
  upgrade.

## app/src/test/java/com/netpress/nextcaltrain/ScheduleSpec.kt

### `ScheduleSpec` (class)
Kept a one-line comment in place: "Covers Schedule.fetchedToday(); the
2am-boundary math it delegates to is covered in GoodTimesSpec."

Full history: covers `Schedule.fetchedToday()` -- the once-per-day fetch
cap that lets `MainActivity`/`ScheduleViewModel` skip a redundant network
call once the app already has today's schedule. `fetchedToday()` itself
only reads from `SharedPreferences` and delegates to
`GoodTimes.scheduleDateFor()`; the 2am-boundary math for `scheduleDateFor()`
itself is covered separately in `GoodTimesSpec`.

### `ScheduleSpec.fixedNoon()`
Kept a one-line comment in place: "Fixed noon, well clear of the 2am
schedule-day boundary, so results don't depend on real run time."

Full history: fixed at noon on an arbitrary date, well clear of the 2am
schedule-day boundary in either direction. Tests pin "now" explicitly via
`fetchedToday`'s `nowMillis` param instead of `System.currentTimeMillis()`,
so results don't depend on what time the suite happens to run.

## app/src/test/java/com/netpress/nextcaltrain/TripViewModelSpec.kt

### `TripViewModelSpec.makeViewModel(schedule, origin, destination)`
Kept a one-line comment in place: "Relaxed mock: TripViewModel reads stop
prefs in init{}, but every test overwrites origin/destination anyway."

Full history: `TripViewModel` reads/writes stop preferences via
`Context.getSharedPreferences()` in `init {}`. A relaxed `mockk<Context>()`
answers every unstubbed call with the type's default (0 for the stored
AM/PM stop indices), which is fine here since every test immediately
overwrites `origin`/`destination` via `setOrigin`/`setDestination` right
after construction anyway.

### `describe("TripViewModel")` -> `context("manual selection via setOffset")`
Kept a one-line comment in place: "Regression coverage for the
reset-button-stuck-on bug; see docs/COMMENTS.md."

Full history: regression coverage for the reset-button-stuck-on bug:
dragging away from the next train sets `hasManualSelection`, but dragging
back to that same next-train offset should clear it again -- otherwise the
reset button stays visible even though the current selection is exactly
the auto-picked "next train."

## app/src/test/java/com/netpress/nextcaltrain/GoodTimesSpec.kt

### `describe(".scheduleDateFor()")`
Kept a one-line comment in place: "Both timestamps built from the same
Calendar so the comparison holds regardless of local timezone."

Full history: used by the once-per-day schedule fetch cap
(`Schedule.fetchedToday`) to decide whether a stored "last fetched at"
timestamp still counts as "today" under the same "day starts at 2am" rule
`GoodTimes()` itself uses. These tests build both timestamps being compared
from the same `Calendar` instance so the comparison holds regardless of the
machine's default timezone.

## app/src/androidTest/java/com/netpress/nextcaltrain/ExampleInstrumentedTest.kt

### `ExampleInstrumentedTest` (class)
Kept a one-line comment in place: "Instrumented test, executes on an
Android device; see http://d.android.com/tools/testing." Default Android
Studio template boilerplate, kept verbatim; no project-specific rationale
beyond the external doc link.
