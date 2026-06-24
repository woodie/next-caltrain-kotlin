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

```
./sim.sh
```

`sim.sh` launches the Android simulator then just leave it running.

```
./build.sh && ./run.sh
```

`run.sh` launches the app via `adb shell am start`. Flags:

| Flag | Effect |
| --- | --- |
| `--fresh` | Clears app data before launching (equivalent to uninstall/reinstall) |
| `--log` | Streams filtered logcat after launch (Ctrl-C to stop) |

Examples:

```
./run.sh                   # just launch
./run.sh --fresh           # clear cache, then launch
./run.sh --log             # launch + stream logs
./run.sh --fresh --log     # both
```

`--fresh` is useful to exercise the loading state machine (no-cache path).

## Viewing logs

```
./run.sh --log
```

Streams `adb logcat` filtered to the app's PID. To add debug logs in Kotlin:

```kotlin
import android.util.Log

Log.d("NextCaltrain", "value=$someValue")
```

Use the tag `NextCaltrain` (or a subtag like `NextCaltrain.Schedule`) so logs
are captured by the PID filter. If the PID lookup fails, `run.sh` falls back
to `-s NextCaltrain:V AndroidRuntime:E`.

**Important**: `println()` goes to stdout and may not appear in logcat on all
devices. Always use `Log.d/i/w/e` with a tag for logs you want to see in the
terminal.

## Working in Android Studio

Open the project root in Android Studio. The IDE has its own Logcat panel
(`View → Tool Windows → Logcat`) which filters by app automatically — no need
for `--log` when working there.

For the IDE, use the standard Run (▶) button instead of `./build.sh && ./run.sh`.

## Quick reference

| Task | Command |
| --- | --- |
| Run unit tests | `./test.sh` |
| Run a single spec | `./test.sh GoodTimesSpec` |
| Lint | `ktlint` (or `ktlint -F` to auto-fix) |
| Build + install | `./build.sh` |
| Launch | `./run.sh` |
| Launch + clear cache | `./run.sh --fresh` |
| Stream debug logs | `./run.sh --log` |
| Both | `./run.sh --fresh --log` |
| Check crash logs | `adb logcat -d \| grep -A 20 "FATAL EXCEPTION"` |
