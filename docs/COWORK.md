# Working with Next Caltrain Kotlin

Quick context for starting a new session on this repo.

## What this is

Android/Kotlin port of Next Caltrain — a live countdown app for Caltrain
commuters. Two sibling repos:

- `next-caltrain-pwa` — KaiOS PWA (JavaScript), hosts the published `schedule.json`
- `next-caltrain-swift` — SwiftUI iOS app, reference implementation

This Kotlin app should match the iOS feature set. Read `docs/DESIGN.md` for
full architecture details.

## Schedule data

Fetched at runtime from the URL in `BuildConfig.SCHEDULE_URL` (set in `app/build.gradle.kts`),
resolved with this precedence:
1. `local.properties` (gitignored) — per-developer override, e.g. to point at
   `next-caltrain-swift/tools/hang_server.py` for instant-fail / 10s-timeout-race testing.
   Never committed, no source edit/revert needed.
2. `schedule-endpoint.properties` (committed, repo root) — the real production URL.
   If the schedule data ever moves to a new home, edit and commit this file directly.
3. A hardcoded literal in `build.gradle.kts` — last-resort safety net if both files are
   missing.

Currently: `https://next-caltrain-pwa.appspot.com/schedule.json`.

Times are minutes since midnight. Missing stops are null. See `docs/DESIGN.md`
for full format.

## Release signing

Play Console requires every uploaded build to be signed with an upload key. The
keystore and its passwords are per-developer secrets — they live only in
`local.properties` (gitignored), never in source or committed config:

```
RELEASE_STORE_FILE=/absolute/path/to/next-caltrain-release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=next-caltrain
RELEASE_KEY_PASSWORD=...
```

If any of the four are missing, `app/build.gradle.kts` leaves the `release` build
type unsigned — it still compiles fine for local testing, it just can't be uploaded
to Play Console.

To generate a keystore (one-time, run yourself — `keytool` prompts interactively
for passwords so they never pass through a script or chat):
```bash
keytool -genkeypair -v -keystore ~/keystores/next-caltrain-release.jks \
  -alias next-caltrain -keyalg RSA -keysize 2048 -validity 10000
```

Store the resulting `.jks` file and its passwords somewhere durable (password
manager + backed-up file) outside the repo. **If you lose it, you cannot publish
updates to the existing Play Store listing ever again** — Play App Signing re-signs
with Google's key, but it still requires your upload key to authenticate each
upload.

## Current status

All five screens (Home, TripList, TripDetail, StationSelection, About) are implemented,
navigable, and tested on device. See `docs/CLAUDE_ADDITIONS.md` for per-screen status and
UI architecture decisions. Outstanding work is UI polish, not missing features.

### Done
- `GoodTimes.kt` — 2-hour rollback, tomorrowDate/tomorrowDotw, debug overrides,
  `scheduleDateFor(epochMillis)` (2am day-boundary rule, used by the fetch cap below)
- `CaltrainSchedule.kt` — weekday/weekend/holiday detection, special dates, forTomorrow
- `CaltrainService.kt` — direct routes, transfer routes, isSouthCounty, Leg/Trip models
- `Schedule.kt` — JSON parsing, fetch/cache pipeline (HttpURLConnection, no third-party lib);
  `fetchedToday(context)` caps network fetches to once per schedule-day
- `TripViewModel.kt` — tomorrow appending, isFuture, offset/nextIndex, StateFlow
- `TripViewModelFactory.kt` — factory for injecting Schedule into ViewModel
- `ScheduleViewModel.kt` — hosts the loading state machine in a ViewModel (survives Activity
  recreation on rotation, so rotating never re-flashes the loading screen). Three cases:
  cache exists + already fetched today → use cache, no network call; no cache → block on
  fetch; cache exists but not fetched today → race fetch against a 10s timeout, fall back to
  stale cache on timeout/failure.
- `MainActivity.kt` — `NavHost` + screen wiring; delegates loading to `ScheduleViewModel`
- `AboutScreen.kt` — loading/about screen with train icon, matches iOS AboutView
- `HomeScreen.kt`, `TripListScreen.kt`, `TripDetailScreen.kt`, `StationSelectionScreen.kt`
- Tests — Kotest DescribeSpec for all data layer classes + ViewModel/fetch-cap logic
  (MockK for mocking `Context`/`SharedPreferences`)

### Known gaps
- `ScheduleViewModel.ensureLoaded()` (the rotation-survival logic above) has no test
  coverage — it calls `Schedule.loadCached(context)`/`fetchFromNetwork(context)` directly
  with no DI seam, so unit-testing it would need either Robolectric/instrumented tests or a
  small refactor to inject those calls.
- Dark mode verification, real device testing pass

## Key files

```
app/src/main/java/com/netpress/nextcaltrain/
  GoodTimes.kt          CaltrainSchedule.kt    CaltrainService.kt
  Schedule.kt           TripViewModel.kt       TripViewModelFactory.kt
  ScheduleViewModel.kt  MainActivity.kt        HomeScreen.kt
  TripListScreen.kt     TripDetailScreen.kt    StationSelectionScreen.kt
  AboutScreen.kt
  ui/theme/Color.kt     ui/theme/Theme.kt

app/src/test/java/com/netpress/nextcaltrain/
  GoodTimesSpec.kt      CaltrainScheduleSpec.kt  CaltrainServiceSpec.kt
  TripViewModelSpec.kt  ScheduleSpec.kt          SpecFixtures.kt
```

## Run tests
```bash
./gradlew clean test
```

## Install and run on emulator
```bash
./gradlew installDebug && ~/Library/Android/sdk/platform-tools/adb shell am start -n com.netpress.nextcaltrain/.MainActivity
```

## Launch with clear cache (to see loading screen)
```bash
~/Library/Android/sdk/platform-tools/adb shell pm clear com.netpress.nextcaltrain
~/Library/Android/sdk/platform-tools/adb shell am start -n com.netpress.nextcaltrain/.MainActivity
```

## Check crash logs
```bash
~/Library/Android/sdk/platform-tools/adb logcat -d | grep -A 20 "FATAL EXCEPTION"
```

## Core conventions

- **2-hour rollback**: `now - 2hrs` is "current time" for schedule purposes.
- **Tomorrow trips**: appended shifted by +1440 minutes, marked `isFuture`.
- **Transfers**: needed when exactly one endpoint is South County + Weekday schedule.
  Transfer station is San Jose Diridon.
- **South County stations**: Gilroy, San Martin, Morgan Hill, Blossom Hill, Capitol.
- **Train IDs**: 101–400 Local, 401–500 Limited, 501–800 Express, 801–900 South County.
- **No text wrapping**: always use `softWrap = false` or `maxLines = 1` — text should
  overflow, never wrap. The circle is decorative; content floats independently.
- **Floating overlay pattern**: ring (`Canvas`) and content (`Column`) are siblings in
  a `Box`, both centered. Content is not constrained by ring size.

## Navigation

Wired up via `NavHost` in `MainActivity.kt`'s `NextCaltrainApp()`. Routes: `home`,
`tripList`, `tripDetail`, `stationSelection`, `about`. `HomeScreen`/`TripListScreen` take
`onNavigateTo*` lambdas that call `navController.navigate(...)`.

## Editing workflow

Claude has direct read/write access to this repo's working copy — it edits files in place
with its file tools (no download/move step), then runs `./gradlew clean test` to verify and
commits directly with git. Commits go on `main` and are left unpushed unless asked to push.

Branches tied to GitHub issues, when used:
```
feature/29-append-tomorrow-trips
bugfix/12-remove-unused-rowwidthkey
```

Pattern: `{type}/{issue-number}-{short-description}`

## Sibling repo paths

```
~/workspace/next-caltrain-pwa
~/workspace/next-caltrain-swift
~/workspace/next-caltrain-kotlin
```
