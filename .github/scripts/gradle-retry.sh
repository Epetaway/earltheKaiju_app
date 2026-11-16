#!/usr/bin/env bash
# Runs ./gradlew with the given arguments, copying its output to a log, and retries (up to three
# tries) only when the failure was a dependency download. Maven Central sometimes answers
# "429 Too Many Requests" to CI runners. Any other failure fails at once.
#
#   bash ../.github/scripts/gradle-retry.sh <log file> <gradle arguments...>
set -uo pipefail
LOG="$1"; shift
for attempt in 1 2 3; do
  ./gradlew "$@" 2>&1 | tee "$LOG"
  status=${PIPESTATUS[0]}
  [ "$status" -eq 0 ] && exit 0
  if [ "$attempt" -lt 3 ] && grep -qE "status code (429|5[0-9][0-9])|Could not (GET|HEAD) 'https|Read timed out|Connection reset" "$LOG"; then
    echo "::warning::A dependency download failed (try $attempt of 3); trying again in $((attempt * 30)) s."
    sleep $((attempt * 30))
    continue
  fi
  exit "$status"
done
