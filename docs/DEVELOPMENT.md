# Development

This project assumes macOS with Android Studio and the Android SDK installed.

## One-time setup

Ensure `adb` is on your PATH, or the scripts will use the full SDK path:

```
~/Library/Android/sdk/platform-tools/adb
```

Install [ktlint](https://pinterest.github.io/ktlint/) for linting:

```
brew install ktlint
```

## Running tests

Tests are written with [Kotest](https://kotest.io/) in a `DescribeSpec`
(`describe`/`it`) format, living in `app/src/test/`.

```
./test.sh
```

This runs `./gradlew clean test`. Output shows each spec with a pass/fail mark.
63 tests cover `GoodTimes`, `CaltrainSchedule`, `CaltrainService`, and
`TripViewModel`.

The result is at the very end:

```
BUILD SUCCESSFUL
```

or

```
BUILD FAILED
```

### Running a single spec

```
./test.sh GoodTimesSpec
```

A bare class name with no leading dash is treated as a spec filter. A `.kt`
path also works, so shell tab-completion is fair game — including through a
personal `Tests/` symlink, if you've set one up (see `.gitignore`'s "Personal
convenience symlinks" — local-only, not portable, so not assumed below):

```
./test.sh Tests/ScheduleSpec.kt
./test.sh app/src/test/java/com/netpress/nextcaltrain/ScheduleSpec.kt
```

(the directory and `.kt` extension are stripped down to `ScheduleSpec`
either way — `test.sh` only looks at the basename, so it doesn't care
whether the path it's given resolves through a symlink). Fully-qualified
names work too
(`./test.sh com.netpress.nextcaltrain.GoodTimesSpec`), as do explicit
wildcards (`./test.sh "*.GoodTimesSpec.some it block"`, to match an
individual `it` rather than the whole spec). Anything dash-prefixed
(`./test.sh --rerun`) is forwarded straight to `gradlew` instead, unchanged.

Under the hood, `test.sh` translates the class name into Kotest's own
`kotest_filter_specs` environment variable rather than Gradle's `--tests`
flag. `--tests` doesn't work reliably against Kotest specs — `DescribeSpec`
classes carry no JUnit annotations for Gradle's bytecode-based pre-filter to
recognize as test classes, so it fails outright with `No tests found for
given includes`, even when targeted at a real `Test`-type task
(`testDebugUnitTest`) instead of the `test` lifecycle aggregate.
`kotest_filter_specs` is inherited by the forked test JVM automatically — no
Gradle wiring needed. To filter by individual test/`it` name directly
(bypassing `test.sh`'s class-name shorthand), set `kotest_filter_tests`
yourself:

```
kotest_filter_tests='*some it block*' ./test.sh GoodTimesSpec
```

See [Kotest's Gradle filtering
docs](https://kotest.io/docs/5.9.x/framework/conditional/conditional-tests-with-gradle.html)
(version-matched to this project's pinned `kotest = "5.9.1"`) for the full
mechanism.

### Test output formatting

`app/build.gradle.kts` used to register its own custom Gradle `TestListener`
directly (replacing `gradle-test-logger-plugin`), byte-for-byte copied into
humane-kotlin/huck too. That block is now `kotidy` (see
`~/workspace/kotidy`'s own `docs/COWORK.md`) -- one real plugin instead of
three hand-synced copies. Originally consumed as a composite build
(`pluginManagement { includeBuild("../kotidy") }`); now that
`com.netpress.kotidy` is approved and live on the Gradle Plugin Portal,
`settings.gradle.kts` just has `gradlePluginPortal()` in
`pluginManagement.repositories` like any other plugin, and `app/build.gradle.kts`
pins `id("com.netpress.kotidy") version "0.1.0"` + `kotidy { style = "fs" }`
-- no sibling checkout of `kotidy` needed on disk or in CI anymore. Still prints a real nested describe/context/it
tree straight from `TestDescriptor.parent`, still only blank-lines before
each top-level suite, still respects `NO_COLOR=1`. `style = "fs"` is the
closest match to the old look (green ✔ + dimmed name on pass) but isn't
byte-identical -- the old ad hoc block's fail/skip glyphs (solid red ✖, solid
cyan ○, no `(FAILED - N)`/`(SKIPPED)` suffix) never actually matched a real
named convention; `fs` now renders Mocha's real spec format instead (red
`✗ name (FAILED - N)`, cyan `- name (SKIPPED)`) -- see kotidy's README for
the full style table if a different look is ever wanted.

There's now a real "Test Succeeded"/"Test Failed" + counts summary line at
the end again, via `kotidy`'s shared `standardFooter`. There used to be one
here too, before it was dropped: it relied on a `val runStart =
System.currentTimeMillis()` set at Gradle *configuration* time, which went
stale (sometimes by hours) whenever `org.gradle.configuration-cache=true`
reused a cached configuration instead of re-running it, producing nonsense
elapsed times like `72 passing (113516.7s)`. `kotidy`'s footer doesn't have
the same bug: every count and duration it prints is reset in `doFirst`
(actual task-execution time, re-run on every invocation regardless of
config-cache state -- the same fix already applied to `lastPath`'s own
dedupe state) and each test's elapsed time comes from `TestResult`'s real
per-test `startTime`/`endTime`, never a value captured once at configuration
time. `BUILD SUCCESSFUL`/`BUILD FAILED` still follows right after, unchanged.

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

## Linting

```
ktlint
```

Add `-F` to auto-fix anything fixable in place:

```
ktlint -F
```

## Lint, test, check

`make lint` and `make test` wrap the two commands above/`./test.sh` — verbose,
same output either way. `make check` runs both back to back but tersely:
lint's usual output, then just `PASS` on a clean test run or the full log if
anything fails. Run `make check` before committing.

`.editorconfig` configures the rule set: IntelliJ's default code style
(rather than ktlint_official's stricter formatting), a 120-char line limit,
a naming exception for `@Composable` functions, and a few other
project-specific overrides. See the comments in `.editorconfig` itself for
the reasoning behind each one.

## Building and installing

```
./build.sh
```

Wraps `./gradlew installDebug`. Installs the debug build to the connected
emulator or device.

## Running on the emulator

`sim.sh` is the one entry point for everything emulator-related — no args
boots it, `run`/`snap`/`dark`/`light` are subcommands:

```
./sim.sh
```

Launches the Android emulator and just leaves it running. Which AVD boots by
default is set directly in `sim.sh` (the `SIM_DEVICE=` line near the top,
edit + commit to change it — no separate config file) — `-d/--device NAME`
overrides it for one invocation:

```
./sim.sh -d Pixel_Tablet
```

```
./build.sh && ./sim.sh run
```

`./sim.sh run` launches the app via `adb shell am start`. Flags:

| Flag | Effect |
| --- | --- |
| `--fresh` | Clears app data before launching (equivalent to uninstall/reinstall) |
| `--log` | Streams filtered logcat after launch (Ctrl-C to stop) |

Examples:

```
./sim.sh run                   # just launch
./sim.sh run --fresh           # clear cache, then launch
./sim.sh run --log             # launch + stream logs
./sim.sh run --fresh --log     # both
```

`--fresh` is useful to exercise the loading state machine (no-cache path).
If no emulator/device is attached, `./sim.sh run` fails fast with a pointer
back to `./sim.sh` rather than a raw `adb` error.

## Viewing logs

```
./sim.sh run --log
```

Streams `adb logcat` filtered to the app's PID. To add debug logs in Kotlin:

```kotlin
import android.util.Log

Log.d("NextCaltrain", "value=$someValue")
```

Use the tag `NextCaltrain` (or a subtag like `NextCaltrain.Schedule`) so logs
are captured by the PID filter. If the PID lookup fails, `./sim.sh run` falls
back to `-s NextCaltrain:V AndroidRuntime:E`.

**Important**: `println()` goes to stdout and may not appear in logcat on all
devices. Always use `Log.d/i/w/e` with a tag for logs you want to see in the
terminal.

## Working in Android Studio

Open the project root in Android Studio. The IDE has its own Logcat panel
(`View → Tool Windows → Logcat`) which filters by app automatically — no need
for `--log` when working there.

For the IDE, use the standard Run (▶) button instead of `./build.sh && ./sim.sh run`.

## Quick reference

| Task | Command |
| --- | --- |
| Run unit tests | `./test.sh` or `make test` |
| Run a single spec | `./test.sh GoodTimesSpec` |
| Lint | `ktlint` or `make lint` (or `ktlint -F` to auto-fix) |
| Lint + test, terse | `make check` (run before committing) |
| Build + install | `./build.sh` |
| Boot the emulator | `./sim.sh` |
| List available AVDs | `./sim.sh list` |
| Boot a specific AVD | `./sim.sh -d Pixel_Tablet` |
| Launch | `./sim.sh run` |
| Launch + clear cache | `./sim.sh run --fresh` |
| Stream debug logs | `./sim.sh run --log` |
| Both | `./sim.sh run --fresh --log` |
| Screenshot | `./sim.sh snap [filename]` |
| Dark / light mode | `./sim.sh dark` / `./sim.sh light` |
| Check crash logs | `adb logcat -d \| grep -A 20 "FATAL EXCEPTION"` |
