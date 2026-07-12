#!/bin/bash
# Usage: ./sim.sh                       boot the emulator and leave it running
#        ./sim.sh run [--fresh] [--log]
#          --fresh  clear app data before launching
#          --log    stream filtered logcat after launch (Ctrl-C to stop)
#        ./sim.sh snap [filename]
#          filename   optional output name, saved under ~/Downloads
#                      (default: snap-<timestamp>.png)
#        ./sim.sh dark | light
#          switch device to dark/light mode

ADB=~/Library/Android/sdk/platform-tools/adb
PKG=com.netpress.nextcaltrain
ACTIVITY=$PKG/.MainActivity
DEST_DIR=~/Downloads

# Shared by snap/dark/light -- exactly one device/emulator must be attached,
# since none of these commands have a way to target a specific one.
require_single_device() {
  DEVICE_COUNT=$($ADB devices | grep -w "device" | wc -l | tr -d ' ')

  if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "No device/emulator attached — boot one first: ./sim.sh"
    exit 1
  elif [ "$DEVICE_COUNT" -gt 1 ]; then
    echo "More than one device/emulator attached — kill one or target a specific one:"
    $ADB devices
    exit 1
  fi
}

cmd_boot() {
  ~/Library/Android/sdk/emulator/emulator -avd Pixel_8 -no-snapshot-load > /dev/null 2>&1 &
}

cmd_run() {
  local FRESH=false
  local LOG=false

  for arg in "$@"; do
    case $arg in
      --fresh) FRESH=true ;;
      --log)   LOG=true ;;
    esac
  done

  require_single_device

  if $FRESH; then
    echo "Clearing app data..."
    $ADB shell pm clear $PKG
  fi

  $ADB shell am force-stop $PKG
  $ADB shell am start -n $ACTIVITY

  if $LOG; then
    echo "Streaming logs for $PKG (Ctrl-C to stop)..."
    # Clear stale log buffer, then wait for the process to start
    $ADB logcat -c
    sleep 1
    PID=$($ADB shell pidof $PKG 2>/dev/null | tr -d '\r')
    if [ -n "$PID" ]; then
      $ADB logcat --pid=$PID
    else
      echo "Warning: could not find PID for $PKG, falling back to tag filter"
      $ADB logcat -s NextCaltrain:V AndroidRuntime:E
    fi
  fi
}

cmd_snap() {
  local NAME="$1"
  require_single_device

  if [ -z "$NAME" ]; then
    NAME="snap-$(date +%Y%m%d-%H%M%S).png"
  fi
  local OUT="$DEST_DIR/$NAME"

  $ADB exec-out screencap -p > "$OUT"

  if [ -s "$OUT" ]; then
    echo "Saved screenshot to $OUT"
  else
    echo "Screenshot failed (empty file) — is the screen on and unlocked?"
    rm -f "$OUT"
    exit 1
  fi
}

cmd_mode() {
  local MODE="$1"
  require_single_device

  if [ "$MODE" = "dark" ]; then
    $ADB shell "cmd uimode night yes"
  else
    $ADB shell "cmd uimode night no"
  fi
  echo "Switched to $MODE mode"
}

case "${1:-}" in
  run)
    shift
    cmd_run "$@"
    ;;
  snap)
    cmd_snap "$2"
    ;;
  dark|light)
    cmd_mode "$1"
    ;;
  "")
    cmd_boot
    ;;
  *)
    echo "Usage: $0 [run [--fresh] [--log] | snap [filename] | dark | light]" >&2
    exit 1
    ;;
esac
