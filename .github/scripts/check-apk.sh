#!/usr/bin/env bash
# Prints an APK's package, version and signing certificate, and fails if it isn't signed or
# isn't signed by the expected certificate. A build signed by any other key can't update the
# app already on the phone without deleting its data, so this runs before anything is shared.
#
#   bash .github/scripts/check-apk.sh <apk> [expected certificate SHA-256 | any]
set -euo pipefail
trap 'echo "::error::check-apk.sh stopped at line $LINENO: $BASH_COMMAND"' ERR
APK="$1"
EXPECTED="$(echo "${2:-any}" | tr -d ':' | tr 'A-F' 'a-f')"

[ -f "$APK" ] || { echo "::error::No APK at $APK"; exit 1; }
BT="$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)"

echo "Build tools: $(basename "$BT")"
# Package and version, for the log only. Captured whole and then trimmed: piping aapt2 into
# head can kill it with SIGPIPE, which pipefail turns into a failed step.
if BADGING="$("$BT/aapt2" dump badging "$APK" 2>&1)"; then
  head -n 1 <<< "$BADGING"
else
  echo "(aapt2 couldn't read the package details: $(head -n 1 <<< "$BADGING"))"
fi
if ! CERTS="$("$BT/apksigner" verify --print-certs "$APK" 2>&1)"; then
  echo "$CERTS"
  echo "::error::$(basename "$APK") isn't signed (or its signature is broken)."
  exit 1
fi
# apksigner's wording varies by version ("Signer #1 certificate ...", "V2 Signer: certificate
# ..."), so match the digest line itself. Every signature scheme must name the same certificate.
grep -E 'certificate (DN|SHA-256 digest)' <<< "$CERTS" || true
DIGESTS="$(awk '/certificate SHA-256 digest:/ {print tolower($NF)}' <<< "$CERTS" | sort -u)"
[ -n "$DIGESTS" ] || { echo "$CERTS"; echo "::error::No signing certificate found in $(basename "$APK")."; exit 1; }
[ "$(wc -l <<< "$DIGESTS")" -eq 1 ] || { echo "::error::$(basename "$APK") names more than one signing certificate."; exit 1; }
ACTUAL="$DIGESTS"

if [ "$EXPECTED" != "any" ] && [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "::error::$(basename "$APK") is signed by $ACTUAL, not the expected $EXPECTED. It would not update the installed app."
  exit 1
fi
echo "CERT_SHA256=$ACTUAL" >> "${GITHUB_OUTPUT:-/dev/null}"
