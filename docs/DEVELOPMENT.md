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
