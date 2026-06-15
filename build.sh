#!/bin/bash
set -e
ADB=~/Library/Android/sdk/platform-tools/adb
PKG=com.netpress.nextcaltrain

./gradlew installDebug
