# Android Device Testing Guide

This guide walks through setting up a physical Android device for testing
Next Caltrain, from enabling developer mode to running the app.

## Step 1 — Enable Developer Mode on your Android phone

1. Open the **Settings** app on your phone.
2. Scroll down and tap **About phone** (sometimes inside a "General management" section).
3. Find **Build number** — it may be under "Software information".
4. Tap **Build number seven times** in a row. You'll feel a haptic tap each time
   and see a countdown ("You are 3 steps away from being a developer…").
5. When you see "You are now a developer!" you're done.

> On Samsung devices the path is: Settings → About phone → Software information → Build number.
> On Pixel devices: Settings → About phone → Build number.

## Step 2 — Enable USB Debugging

1. Go back to **Settings** and find the new **Developer options** entry
   (usually near the bottom of the Settings list, or under "System").
2. Tap **Developer options** and make sure the toggle at the top is **On**.
3. Scroll down and enable **USB debugging**.
4. Confirm the prompt that warns about USB debugging.

## Step 3 — Connect your phone to your Mac

1. Use a USB cable (USB-C to USB-C, or USB-C to USB-A with an adapter).
2. On your phone, a dialog will appear: **"Allow USB debugging?"** — tap **Allow**.
   Check "Always allow from this computer" so you don't see it every time.
3. Your phone's screen may ask you to select a USB mode — choose **File Transfer**
   (also called MTP) to keep the connection stable.

## Step 4 — Verify the connection

Open a terminal on your Mac and run:

```bash
~/Library/Android/sdk/platform-tools/adb devices
```

You should see your device listed, like:

```
List of devices attached
XXXXXXXXXXXXXXXX    device
```

If it shows `unauthorized`, check your phone screen for a new "Allow USB debugging?" prompt.

## Step 5 — Build and run

From the project root:

```bash
cd ~/workspace/next-caltrain-kotlin
./build.sh && ./run.sh
```

The app will compile, install, and launch on your phone automatically.

### Useful run.sh flags

```bash
./build.sh && ./run.sh --log      # stream logcat output to terminal (Ctrl-C to stop)
./build.sh && ./run.sh --fresh    # clear app data before launch (simulates first install)
./build.sh && ./run.sh --fresh --log   # both
```

### Check for crashes

If the app crashes on launch:

```bash
~/Library/Android/sdk/platform-tools/adb logcat -d | grep -A 20 "FATAL EXCEPTION"
```

## Android Studio (optional)

You don't need Android Studio to build or run — the terminal workflow above is
sufficient. But if you want to use it:

1. Open Android Studio and choose **Open** → select the `next-caltrain-kotlin` folder.
2. Wait for Gradle sync to complete (progress bar at the bottom).
3. Your connected device should appear in the toolbar dropdown next to the green ▶ Run button.
4. Press ▶ to build and install on the device.
5. The **Logcat** panel (bottom of the screen, or View → Tool Windows → Logcat) shows
   live logs — filter by package name `com.netpress.nextcaltrain` to reduce noise.

### Layout Inspector (Android Studio)

If you want to inspect the UI live on device:

- Run → **Attach Debugger to Android Process** (or just run in debug mode with the 🐛 button).
- Go to **Tools → Layout Inspector**.
- Select your running process from the dropdown.
- You can click any element on the mirrored screen and see its Compose tree, modifiers,
  and exact sizes in dp.

## Troubleshooting

**Device not detected**
- Try a different USB cable (many cables are charge-only and don't carry data).
- Try a different USB port on your Mac.
- Run `adb kill-server && adb start-server` then reconnect.

**App installs but immediately crashes**
- Run `./run.sh --log` to see the stack trace, or check with the `adb logcat` command above.

**"INSTALL_FAILED_UPDATE_INCOMPATIBLE"**
- Uninstall the existing app from your phone first, then re-run `./build.sh && ./run.sh`.

**Wireless debugging (no cable)**
Android 11+ supports debugging over Wi-Fi:
1. In **Developer options**, enable **Wireless debugging**.
2. Tap **Pair device with pairing code**.
3. Run `adb pair <ip>:<port>` with the code shown on your phone.
4. Then `adb connect <ip>:<port>` (the port shown on the Wireless debugging main screen).
5. Verify with `adb devices`, then use `./build.sh && ./run.sh` as normal.
