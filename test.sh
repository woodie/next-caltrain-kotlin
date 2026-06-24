#!/bin/bash
set -e

# Deliberately NOT piping this through grep/sed to filter Gradle's
# "Configuration cache entry stored/reused." notice -- piping makes
# Gradle's stdout a non-tty, which drops it out of rich console mode and
# into plain mode, printing a "> Task :app:xxx" line for every one of the
# ~27 tasks instead of collapsing the silent ones. That trade was worse
# than the one line it removed. Gradle also has no dedicated flag to
# silence just that notice without affecting BUILD SUCCESSFUL/actionable
# tasks too (see gradle/gradle#24435) -- so it stays.
./gradlew clean test
