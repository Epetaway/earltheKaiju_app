#!/usr/bin/env bash
# Prints an APK's package, version and signing certificate, and fails if it isn't signed or
# isn't signed by the expected certificate. A build signed by any other key can't update the
# app already on the phone without deleting its data, so this runs before anything is shared.
#
#   bash .github/scripts/check-apk.sh <apk> [expected certificate SHA-256 | any]
set -euo pipefail
APK="$1"
EXPECTED="$(echo "${2:-any}" | tr -d ':' | tr 'A-F' 'a-f')"

[ -f "$APK" ] || { echo "::error::No APK at $APK"; exit 1; }
BT="$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)"

"$BT/aapt2" dump badging "$APK" | head -1
if ! CERTS="$("$BT/apksigner" verify --print-certs "$APK" 2>&1)"; then
  echo "$CERTS"
  echo "::error::$(basename "$APK") isn't signed (or its signature is broken)."
  exit 1
fi
ACTUAL="$(echo "$CERTS" | awk -F': ' '/^Signer #1 certificate SHA-256 digest/ {print $2; exit}')"
echo "$CERTS" | grep -E '^Signer #1 certificate (DN|SHA-256 digest)'

if [ "$EXPECTED" != "any" ] && [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "::error::$(basename "$APK") is signed by $ACTUAL, not the expected $EXPECTED. It would not update the installed app."
  exit 1
fi
echo "CERT_SHA256=$ACTUAL" >> "${GITHUB_OUTPUT:-/dev/null}"
