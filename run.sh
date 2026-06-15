#!/bin/bash
# Usage: ./run.sh [--fresh] [--log]
#   --fresh  clear app data before launching
#   --log    stream filtered logcat after launch (Ctrl-C to stop)

ADB=~/Library/Android/sdk/platform-tools/adb
PKG=com.netpress.nextcaltrain
ACTIVITY=$PKG/.MainActivity

FRESH=false
LOG=false

for arg in "$@"; do
  case $arg in
    --fresh) FRESH=true ;;
    --log)   LOG=true ;;
  esac
done

if $FRESH; then
  echo "Clearing app data..."
  $ADB shell pm clear $PKG
fi

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
