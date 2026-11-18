#!/usr/bin/env bash
# Runs ./gradlew with the given arguments, copying its output to a log, and retries (up to three
# tries) only when the failure was a dependency download. Maven Central sometimes answers
# "429 Too Many Requests" to CI runners, and now and then reports a well-known artifact as not
# found (Guava, 2026-11-18). That case retries with --refresh-dependencies so Gradle doesn't reuse
# the cached miss; a version that truly doesn't exist still fails after the third try.
# Any other failure fails at once.
#
#   bash ../.github/scripts/gradle-retry.sh <log file> <gradle arguments...>
set -uo pipefail
LOG="$1"; shift
REFRESH=()
for attempt in 1 2 3; do
  ./gradlew "$@" "${REFRESH[@]}" 2>&1 | tee "$LOG"
  status=${PIPESTATUS[0]}
  [ "$status" -eq 0 ] && exit 0
  if [ "$attempt" -lt 3 ] && grep -qE "status code (429|5[0-9][0-9])|Could not (GET|HEAD) 'https|Read timed out|Connection reset" "$LOG"; then
    echo "::warning::A dependency download failed (try $attempt of 3); trying again in $((attempt * 30)) s."
    sleep $((attempt * 30))
    continue
  fi
  if [ "$attempt" -lt 3 ] && grep -qE "Could not find [^ ]+:[^ ]+:[^ ]+\." "$LOG"; then
    echo "::warning::A dependency was reported missing (try $attempt of 3); refreshing and trying again in $((attempt * 30)) s."
    REFRESH=(--refresh-dependencies)
    sleep $((attempt * 30))
    continue
  fi
  exit "$status"
done
