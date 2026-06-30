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
2. `schedule-endpoint.properties` (committed, repo root) — the real production URL.
   If the schedule data ever moves to a new home, edit and commit this file directly.
3. A hardcoded literal in `build.gradle.kts` — last-resort safety net if both files are
   missing.

Currently: `https://next-caltrain-pwa.appspot.com/feed/schedule.json`.

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

### Test output formatting

`app/build.gradle.kts` registers a custom Gradle `TestListener` (replacing
`gradle-test-logger-plugin`) that prints a real nested describe/context/it tree
straight from `TestDescriptor.parent`, with terminal colors matching Mocha's
spec reporter — green ✔ + dimmed name on pass, solid red ✖ + red exception
detail on failure, solid cyan ○ on skip. Unlike the old plugin's mocha theme,
which hardcoded a blank line between every nested describe/context group,
this one only blank-lines before each top-level suite (see below) — nested
groups within a suite still print with no padding. Set `NO_COLOR=1` to
disable the ANSI codes (e.g. piping to a file). See the comment block above
the listener in `build.gradle.kts` for the "dedupe shared prefix" trick that
turns Gradle's flat `afterTest` callbacks into a dense tree.

There's no custom "N passing (Xs)" summary line at the end — there used to
be, but it relied on a `val runStart = System.currentTimeMillis()` set at
Gradle configuration time, which goes stale (sometimes by hours) whenever
`org.gradle.configuration-cache=true` reuses a cached configuration instead
of re-running it, producing nonsense elapsed times like `72 passing
(113516.7s)`. Rather than chase that down further, the summary line was
dropped — Gradle's own `BUILD SUCCESSFUL`/`BUILD FAILED` is the whole footer
now.

That footer still includes a `Configuration cache entry stored/reused.`
line. `test.sh` deliberately does NOT pipe `./gradlew clean test` through
`grep`/`sed` to filter it out: piping makes Gradle's stdout a non-tty, which
drops it out of rich console mode and into plain mode, printing a
`> Task :app:xxx` line for every one of the ~27 tasks instead of collapsing
the silent ones — far noisier than the one line it would remove. Gradle also
has no dedicated flag to silence just that notice without affecting
`BUILD SUCCESSFUL`/`actionable tasks` too (open issue:
[gradle/gradle#24435](https://github.com/gradle/gradle/issues/24435)), so it
stays.

A blank line is also printed before every top-level suite line (the
`com.netpress.nextcaltrain.*` class name), unconditionally — including the
very first one in the run — so each suite's block visually stands apart from
whatever preceded it.

The Swift sibling (`next-caltrain-swift`) does the same dedupe-and-render
trick, plus the same unconditional blank-line-before-suite rule, from the
opposite starting point — see its `docs/COWORK.md` "Test output formatting"
section.

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

### Stale-looking git lock errors

If `git add`/`git commit` fails with `Unable to create '.git/index.lock'`
(or `HEAD.lock`) `: File exists`, it's almost never a real stale lock from a
crashed git process — `ps aux` will show nothing running, and `rm -f` on the
lock file fails with `Operation not permitted` even though ownership and
permissions look completely normal. This repo's working copy is a folder
mounted into Cowork's sandbox from the user's actual machine, and deleting
files there requires explicit one-time permission per session. Call the
`allow_cowork_file_delete` tool with the lock file's path; once granted, `rm
-f .git/*.lock` succeeds immediately and the commit can be retried. The
permission covers the whole mounted folder, so if a second lock file shows up
(e.g. `HEAD.lock` after clearing `index.lock`) it can just be removed too, no
need to ask again.

## Sibling repo paths

```
~/workspace/next-caltrain-pwa
~/workspace/next-caltrain-swift
~/workspace/next-caltrain-kotlin
```
