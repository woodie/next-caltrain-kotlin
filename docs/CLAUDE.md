# CLAUDE.md — Next Caltrain Kotlin

Quick context for starting a new session on this repo.

## What this is

Android/Kotlin port of Next Caltrain — a live countdown app for Caltrain
commuters. Two sibling repos:

- `next-caltrain-pwa` — KaiOS PWA (JavaScript), hosts the published `schedule.json`
- `next-caltrain-ios` — SwiftUI iOS app, reference implementation

This Kotlin app should match the iOS feature set. Read `docs/DESIGN.md` for
full architecture details.

## Schedule data

Fetched at runtime from:
```
https://next-caltrain-pwa.appspot.com/schedule.json
```

Times are minutes since midnight. Missing stops are null. See `docs/DESIGN.md`
for full format.

## Current status

Core data layer and HomeScreen are working and tested on the emulator.

### Done
- `GoodTimes.kt` — 2-hour rollback, tomorrowDate/tomorrowDotw, debug overrides
- `CaltrainSchedule.kt` — weekday/weekend/holiday detection, special dates, forTomorrow
- `CaltrainService.kt` — direct routes, transfer routes, isSouthCounty, Leg/Trip models
- `Schedule.kt` — JSON parsing, fetch/cache pipeline (HttpURLConnection, no third-party lib)
- `TripViewModel.kt` — tomorrow appending, isFuture, offset/nextIndex, StateFlow
- `TripViewModelFactory.kt` — factory for injecting Schedule into ViewModel
- `MainActivity.kt` — loading state machine (no cache → block; cache → race 10s timeout)
- `AboutScreen.kt` — loading/about screen with train icon, matches iOS AboutView
- `HomeScreen.kt` — big circle with countdown, toolbar, swap, drag to scroll trips
- Tests — Kotest DescribeSpec for all data layer classes, 63 tests passing

### Still needed
- Navigation: tap circle → TripListScreen, tap station names → StationSelectionScreen,
  tap "Next Caltrain" → AboutScreen (with schedule date)
- `TripListScreen.kt` — scrollable list of trip rows, mirroring iOS TripListView
- `TripDetailScreen.kt` — stop-by-stop detail, mirroring iOS TripDetailView
- `StationSelectionScreen.kt` — station picker, mirroring iOS StationSelectionView
- Station persistence (SharedPreferences, mirroring iOS UserDefaults stopAM/stopPM)
- Dark mode verification
- Real device testing

## Key files

```
app/src/main/java/com/netpress/nextcaltrain/
  GoodTimes.kt         CaltrainSchedule.kt   CaltrainService.kt
  Schedule.kt          TripViewModel.kt      TripViewModelFactory.kt
  MainActivity.kt      HomeScreen.kt         AboutScreen.kt
  LoadingScreen.kt     ui/theme/Color.kt     ui/theme/Theme.kt

app/src/test/java/com/netpress/nextcaltrain/
  GoodTimesSpec.kt     CaltrainScheduleSpec.kt  CaltrainServiceSpec.kt
  TripViewModelSpec.kt SpecFixtures.kt
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

## Navigation (not yet wired up)

`HomeScreen` accepts lambdas:
- `onNavigateToTripList` — tap circle
- `onNavigateToAbout` — tap "Next Caltrain" title
- `onNavigateToStationSelection` — tap station names

These need to be connected via `NavHost` in `MainActivity.kt`.

## Git workflow

Branches tied to GitHub issues:
```
feature/29-append-tomorrow-trips
bugfix/12-remove-unused-rowwidthkey
```

Pattern: `{type}/{issue-number}-{short-description}`

Always `mv` files from `~/Downloads/`, never `cp`.
Always include the copy-paste command with every file change.

## Sibling repo paths

```
~/workspace/next-caltrain-pwa
~/workspace/next-caltrain-ios
~/workspace/next-caltrain-kotlin
```
