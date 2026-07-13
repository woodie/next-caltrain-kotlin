#!/bin/bash
# Usage: ./sim.sh [-d DEVICE]            boot the emulator and leave it running
#          -d, --device DEVICE   boot an AVD matching DEVICE (substring match).
#                                 Overrides SIM_DEVICE (see sim-device.env/local.env)
#                                 for this invocation only.
#        ./sim.sh run [--fresh] [--log]
#          --fresh  clear app data before launching
#          --log    stream filtered logcat after launch (Ctrl-C to stop)
#        ./sim.sh snap [filename]
#          filename   optional output name, saved under ~/Downloads
#                      (default: snap-<timestamp>.png)
#        ./sim.sh dark | light
#          switch device to dark/light mode
#        ./sim.sh list
#          list installed AVDs, for picking a -d/--device value

ADB=~/Library/Android/sdk/platform-tools/adb
EMULATOR=~/Library/Android/sdk/emulator/emulator
PKG=com.netpress.nextcaltrain
ACTIVITY=$PKG/.MainActivity
DEST_DIR=~/Downloads

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Which AVD sim.sh boots by default when no -d/--device is given, highest
# priority first:
#   1. SIM_DEVICE already set in the calling environment (e.g.
#      `SIM_DEVICE=Pixel_Tablet ./sim.sh`) -- captured before the files below
#      are sourced, and restored after, so it can't be clobbered by them.
#   2. local.env (gitignored) -- per-developer override.
#   3. sim-device.env (committed) -- the real default. Edit + commit to
#      change it for everyone.
_SIM_DEVICE_FROM_ENV="${SIM_DEVICE:-}"
[ -f "$SCRIPT_DIR/sim-device.env" ] && source "$SCRIPT_DIR/sim-device.env"
[ -f "$SCRIPT_DIR/local.env" ] && source "$SCRIPT_DIR/local.env"
[ -n "$_SIM_DEVICE_FROM_ENV" ] && SIM_DEVICE="$_SIM_DEVICE_FROM_ENV"

# Resolves an AVD NAME against installed AVDs: an exact name always wins if
# one exists, otherwise falls back to a substring match (case-insensitive
# either way) -- same reasoning as sim.sh's swift sibling's resolve_device,
# so e.g. "Pixel_8" can't accidentally resolve to a hypothetical "Pixel_8_Pro"
# instead. Kept as a resolve step (rather than passing $SIM_DEVICE to
# `emulator -avd` unchecked) so a typo'd/removed AVD fails fast with a clear
# message instead of emulator's own less obvious startup error.
resolve_avd() {
  local name="$1"
  local list
  list=$("$EMULATOR" -list-avds)

  local avd
  avd=$(echo "$list" | grep -ix "$name" | head -1)
  if [ -z "$avd" ]; then
    avd=$(echo "$list" | grep -i "$name" | head -1)
  fi
  if [ -z "$avd" ]; then
    echo "No AVD found matching '$name' -- installed AVDs:" >&2
    echo "$list" >&2
    return 1
  fi
  echo "$avd"
}

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
  local device="$SIM_DEVICE"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      -d|--device)
        device="$2"
        shift 2
        ;;
      *)
        shift
        ;;
    esac
  done

  local avd
  avd=$(resolve_avd "$device") || exit 1

  # Kill any already-running emulator first. Android emulators are heavy
  # (memory/CPU) -- letting them pile up across AVD switches wastes
  # resources for no benefit, since only one is ever actually being tested
  # against at a time.
  $ADB devices | grep "^emulator-" | cut -f1 | while read -r serial; do
    $ADB -s "$serial" emu kill 2>/dev/null
  done

  "$EMULATOR" -avd "$avd" -no-snapshot-load > /dev/null 2>&1 &
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

cmd_list() {
  "$EMULATOR" -list-avds
  echo
  echo "Boot one of these: ./sim.sh -d <name or unique substring>"
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
  list)
    cmd_list
    ;;
  ""|-d|--device)
    cmd_boot "$@"
    ;;
  *)
    echo "Usage: $0 [[-d DEVICE] | run [--fresh] [--log] | snap [filename] | dark | light | list]" >&2
    exit 1
    ;;
esac
