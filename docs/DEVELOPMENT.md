# Development

This project assumes macOS with Android Studio and the Android SDK installed.

## One-time setup

Ensure `adb` is on your PATH, or the scripts will use the full SDK path:

```
~/Library/Android/sdk/platform-tools/adb
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
| Build + install | `./build.sh` |
| Launch | `./run.sh` |
| Launch + clear cache | `./run.sh --fresh` |
| Stream debug logs | `./run.sh --log` |
| Both | `./run.sh --fresh --log` |
| Check crash logs | `adb logcat -d \| grep -A 20 "FATAL EXCEPTION"` |
