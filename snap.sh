#!/bin/bash
# Usage: ./snap.sh [filename]
#   filename   optional output name, saved under ~/Downloads (default: snap-<timestamp>.png)
# Usage: ./snap.sh -dark | -light
#   -dark      switch device to dark mode and exit (no screenshot)
#   -light     switch device to light mode and exit (no screenshot)

ADB=~/Library/Android/sdk/platform-tools/adb
DEST_DIR=~/Downloads

NAME="$1"

DEVICE_COUNT=$($ADB devices | grep -w "device" | wc -l | tr -d ' ')

if [ "$DEVICE_COUNT" -eq 0 ]; then
  echo "No device/emulator attached."
  exit 1
elif [ "$DEVICE_COUNT" -gt 1 ]; then
  echo "More than one device/emulator attached — kill one or target a specific one:"
  $ADB devices
  exit 1
fi

if [ "$NAME" = "-dark" ]; then
  $ADB shell "cmd uimode night yes"
  echo "Switched to dark mode"
  exit 0
elif [ "$NAME" = "-light" ]; then
  $ADB shell "cmd uimode night no"
  echo "Switched to light mode"
  exit 0
fi

if [ -z "$NAME" ]; then
  NAME="snap-$(date +%Y%m%d-%H%M%S).png"
fi
OUT="$DEST_DIR/$NAME"

$ADB exec-out screencap -p > "$OUT"

if [ -s "$OUT" ]; then
  echo "Saved screenshot to $OUT"
else
  echo "Screenshot failed (empty file) — is the screen on and unlocked?"
  rm -f "$OUT"
  exit 1
fi
