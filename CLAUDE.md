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

## Core classes to implement

| Class | Responsibility |
|---|---|
| `GoodTimes` | Clock with 2-hour rollback, tomorrowDate/tomorrowDotw |
| `CaltrainSchedule` | Weekday/Weekend/Holiday detection, special dates |
| `CaltrainService` | Routes, transfers, isSouthCounty |
| `TripViewModel` | Trip list, tomorrow appending, offset management |

Reference implementations in `next-caltrain-ios/Sources/`.

## Key conventions

- **2-hour rollback**: `now - 2hrs` is "current time" for schedule purposes.
- **Tomorrow trips**: appended shifted by +1440 minutes, marked `isFuture`.
- **Transfers**: needed when exactly one endpoint is South County + Weekday schedule.
  Transfer station is San Jose Diridon.
- **South County stations**: Gilroy, San Martin, Morgan Hill, Blossom Hill, Capitol.
- **Train IDs**: 101–400 Local, 401–500 Limited, 501–800 Express, 801–900 South County.

## Git workflow

Branches tied to GitHub issues:
```
feature/29-append-tomorrow-trips
bugfix/12-remove-unused-rowwidthkey
```

Pattern: `{type}/{issue-number}-{short-description}`

Always `mv` files from `~/Downloads/`, never `cp`.
Always include the copy-paste command with every file change.

## Testing

Mirror the iOS fixture builder pattern — minimal schedule with just enough
stations to test the logic. Use debug overrides for time-sensitive tests:
- `debugOverrideMinutes` — pin current time
- `debugOverrideDotw` — pin day of week (0=Sunday … 6=Saturday)

## Sibling repo paths

```
~/workspace/next-caltrain-pwa
~/workspace/next-caltrain-ios
~/workspace/next-caltrain-kotlin
```
