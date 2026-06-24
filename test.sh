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
#
# A bare word with no leading dash (e.g. `./test.sh GoodTimesSpec`) is
# treated as a Kotest spec filter and translated into the kotest_filter_specs
# env var -- Gradle's own --tests class filter doesn't reliably work against
# Kotest specs (DescribeSpec classes carry no JUnit annotations for Gradle's
# bytecode-based pre-filter to recognize as test classes), so it fails
# outright with "No tests found for given includes". kotest_filter_specs is
# inherited by the forked test JVM automatically, no Gradle wiring needed. A
# ".kt" path (e.g. shell-completed from app/src/test/java/...) has its
# directory and extension stripped down to the bare class name first, so
# tab-completion works. After that: a name containing a "." is used as-is
# (already fully-qualified, or already a glob); a bare name is wrapped as
# "*.<name>" so it matches regardless of package. Everything else (any
# dash-prefixed flag) is forwarded straight to gradlew unchanged -- mirrors
# the Swift sibling's test.sh, which forwards args to xcodebuild test the
# same way (see its docs/COWORK.md).
#
#   ./test.sh GoodTimesSpec
#   ./test.sh com.netpress.nextcaltrain.GoodTimesSpec
#   ./test.sh app/src/test/java/com/netpress/nextcaltrain/ScheduleSpec.kt
#
# See docs/DEVELOPMENT.md "Running a single spec" for more, including the
# kotest_filter_tests variant (filters by individual `it` name).
if [ "$#" -gt 0 ] && [[ "$1" != -* ]]; then
  ARG="$1"
  [[ "$ARG" == *.kt ]] && ARG="$(basename "$ARG" .kt)"
  case "$ARG" in
    *.*) export kotest_filter_specs="$ARG" ;;
    *)   export kotest_filter_specs="*.$ARG" ;;
  esac
  shift
fi

./gradlew clean test "$@"
