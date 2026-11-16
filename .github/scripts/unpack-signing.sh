#!/usr/bin/env bash
# Unpacks signing keys from the KAIJU_SIGNING_BUNDLE secret (base64 of a .tar.gz) into a private
# temporary folder and tells Gradle where they are (KAIJU_SIGNING_DIR):
#
#   bash .github/scripts/unpack-signing.sh ci-debug    # debug jobs
#   bash .github/scripts/unpack-signing.sh release     # the personal build
#
# Only the keys named are unpacked, so debug jobs never see the release key. Without the secret
# it does nothing: debug builds keep the runner's throwaway key and no personal build is
# published. Nothing here prints a password. See docs/10-personal-builds.md.
set -euo pipefail

if [ -z "${KAIJU_SIGNING_BUNDLE:-}" ]; then
  echo "::notice::The KAIJU_SIGNING_BUNDLE secret isn't set: using a throwaway debug key and skipping the personal build."
  echo "signed=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

DIR="$RUNNER_TEMP/kaiju-signing"
mkdir -p "$DIR" && chmod 700 "$DIR"
members=()
for name in "$@"; do members+=("$name.p12" "$name.password"); done

# Pasting into the GitHub form can add spaces or line breaks; strip them before decoding.
if ! printf '%s' "$KAIJU_SIGNING_BUNDLE" | tr -d ' \r\n\t' | base64 -d | tar -xz -C "$DIR" "${members[@]}"; then
  echo "::error::KAIJU_SIGNING_BUNDLE couldn't be unpacked. Paste the whole bundle file again (docs/10-personal-builds.md)."
  exit 1
fi
for name in "$@"; do
  # The bundle itself is masked as a secret; the passwords inside it are not, so mask them too.
  echo "::add-mask::$(tr -d '\r\n' < "$DIR/$name.password")"
  chmod 600 "$DIR/$name.p12" "$DIR/$name.password"
done

echo "KAIJU_SIGNING_DIR=$DIR" >> "$GITHUB_ENV"
echo "signed=true" >> "$GITHUB_OUTPUT"
echo "Unpacked signing keys: $*"
