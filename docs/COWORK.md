# Working with Next Caltrain Kotlin

Quick context for starting a new session on this repo.
Cross-project conventions (git locks, sandbox toolchain) are in `~/workspace/woodie/docs/COWORK.md`.

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
2. `config.properties` (committed, repo root) — the real production URL. If the
   schedule data ever moves to a new home, edit and commit this file directly.
   Named generically (not `schedule-endpoint.properties`) since it's the
   general committed-app-config file, not schedule-only — matches the Swift
   sibling's `config.properties`.
3. A hardcoded literal in `build.gradle.kts` — last-resort safety net if both files are
   missing.

Currently: `https://next-caltrain-app.appspot.com/feed/schedule.json`.

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
navigable, and tested on device. See `docs/COWORK_ADDITIONS.md` for per-screen status and
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
- `.editorconfig` — ktlint rule overrides (IntelliJ code style instead of
  ktlint_official, 120-char line limit, `@Composable` naming exception, and
  a few other project-specific disables). Repo linted/formatted against it.
  Run `ktlint` / `ktlint -F`; see `docs/DEVELOPMENT.md` "Linting" for setup
  and the file's own comments for the reasoning behind each override.

### Known gaps
- `ScheduleViewModel.ensureLoaded()` (the rotation-survival logic above) has no test
  coverage — it calls `Schedule.loadCached(context)`/`fetchFromNetwork(context)` directly
  with no DI seam, so unit-testing it would need either Robolectric/instrumented tests or a
  small refactor to inject those calls.
- Dark mode verification
- Physical devices so far are both Android 9 (Galaxy S8, Pixel XL); nothing newer yet

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

To run a single spec class:
```bash
./test.sh GoodTimesSpec
```
A bare class name (no leading dash) is translated under the hood into
Kotest's own `kotest_filter_specs` env var — Gradle's `--tests` filter
doesn't work reliably against Kotest specs, so `test.sh` bypasses it
entirely. See `docs/DEVELOPMENT.md` "Running a single spec" for
fully-qualified names, wildcards, and the `kotest_filter_tests` variant
(filters by individual `it` name).

Test output comes from the `kotidy` Gradle plugin; see `docs/DEVELOPMENT.md`
"Test output formatting" for how it looks and why.

## Testing approach

`justBeforeEach` (via [kwick](https://github.com/woodie/kwick)) hoists a
shared "act" step above sibling contexts that only vary the input. Not
automatically worth wiring in everywhere -- reviewed every spec file here
(and its Swift sibling) to separate real DRY wins from cases where it'd
cost more readability than it saves.

**Wired in:** `CaltrainScheduleSpec`'s `.optionIndexFor()`/`.forTomorrow()`,
`CaltrainServiceSpec`'s `.direction()` and `#routes()`, `TripViewModelSpec`'s
two "for a route with..." contexts, `GoodTimesSpec`'s `debugOverrideDotw`
context (the original motivating case) -- each had a real, multi-line act
duplicated across 2+ sibling contexts. `#routes()` is the biggest win: six
sibling contexts, several with multiple `it`s sharing one hoisted result --
the first real usage of several assertions reading one subject, closer to
RSpec's actual `subject` than any earlier case.

**Update:** `debugOverrideDotw`/`debugOverrideMinutes` were later renamed to
`dotwSeed`/`minutesSeed` (dropping "debug" -- this is a testing seam, not a
debugging tool -- and "override," which wrongly implied a live, ongoing
effect rather than a value read once at construction). `GoodTimesSpec` and
`CaltrainScheduleSpec` also switched to a `GoodTimes.seeded(dotw, mins)`
factory, since both construct `GoodTimes` directly and don't need the
statics at all; `TripViewModelSpec` keeps using the (renamed) statics, since
`TripViewModel` constructs `GoodTimes()` internally with no seam to pass a
seed through. See `docs/COMMENTS.md` for the full reasoning.

**Skipped on purpose:** `ScheduleSpec`'s three `fetchedToday()` contexts
(setup varies more than the one-line act), `GoodTimesSpec`'s
`.partTime()`/`.fullTime()` (one `it` per context, self-contained
one-liners), `CaltrainServiceSpec`'s `#nextIndex()` (what's under test
differs enough per context that sharing the `trips` fixture wouldn't
clarify anything). Hoisting any of these trades a readable one-liner for a
var + block + `beforeEach`, for no real duplication savings.

**Naming as `subject`:** kwick has no dedicated `subject {}` construct --
the convention is a plain, well-named shared var (`result`, `dir`,
`routes`) that every `it` in the context reads. That naming choice is the
`subject` equivalent; no extra feature needed.

**The `runCatching`-outcome convention doesn't apply to real throwing
calls.** kwick's own docs describe hoisting a possibly-throwing action
into `justBeforeEach`, capturing the outcome via `runCatching`. Checked
`zouk`'s `ScanClientSpec.swift` and `huck`'s `ScanClientSpec.kt` -- the two
real places in this account with actual network-failure coverage -- and
neither uses that pattern. Both inject a fake HTTP client
(`ScanHTTPClient`/`ScanHttpClient`) and call the throwing method directly
inside each `it`, asserting with `shouldThrow`/`throwError`. Hoisting a
throwing act into setup is exactly the trap that convention exists to
avoid, so testing a real exception just doesn't need `justBeforeEach` at
all.

`Schedule.fetchFromNetwork()` was the one real gap matching this shape --
untested, no DI seam, `HttpURLConnection` with nothing to fake. Fixed to
match huck's exact pattern: `ScheduleHttpClient` interface,
`JdkHttpScheduleHttpClient` (real implementation, `java.net.http.HttpClient`
now instead of `HttpURLConnection`), `FakeScheduleHttpClient` (tests),
`ScheduleError` replacing two generic `Exception(...)` throws.
`ScheduleSpec.kt`'s new `Schedule.fetchFromNetwork()` describe covers all
three outcomes (200+valid, non-2xx, 200+invalid) the same inline-`shouldThrow`
way as huck -- not `justBeforeEach`.

## Install and run on emulator or phone
```bash
./build.sh && ./sim.sh run
```

Add `--fresh` to clear app data first (shows the loading screen). Phone setup is in
`docs/DEVICE_TESTING.md`. `sim.sh` needs exactly one device attached; with more than one,
target a phone by serial with `ANDROID_SERIAL=<serial> ./gradlew installDebug`.

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

**Bugfixes and features tied to a GitHub issue go on a branch, not straight to
`main`.** Create the branch (see pattern below), make the fix there, and stop —
do not commit (even to the branch) until the user has discussed the fix and
confirmed it actually works (ran the tests, tried it on device/emulator).
Committing before that confirmation is exactly what we're avoiding; the user
pushes, reviews, and merges into `main` themselves once satisfied.
Direct-to-`main` commits are still fine for changes that don't need that
back-and-forth (docs, copy, config).

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
