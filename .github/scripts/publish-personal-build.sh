#!/usr/bin/env bash
# Publishes the signed personal build as a GitHub Release, so the phone can install or update it
# from one fixed link:
#   https://github.com/<repo>/releases/latest/download/kaiju-coach.apk
# Runs only for main, after every check has passed, with an APK check-apk.sh has already
# confirmed is signed by the personal key. Keeps the 10 newest builds.
set -euo pipefail

: "${KAIJU_BUILD_NUMBER:?}" "${CERT_SHA256:?}" "${GH_TOKEN:?}"
SRC=android/app/build/outputs/apk/release/app-release.apk
APK="$RUNNER_TEMP/kaiju-coach.apk"
cp "$SRC" "$APK"

BT="$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)"
VERSION="$("$BT/aapt2" dump badging "$APK" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -1)"
TAG="build-${KAIJU_BUILD_NUMBER}"
SUBJECT="$(git log -1 --format=%s "$GITHUB_SHA")"
LINK="https://github.com/${GITHUB_REPOSITORY}/releases/latest/download/kaiju-coach.apk"

cat > "$RUNNER_TEMP/notes.md" <<EOF
Personal build of Kaiju Coach ${VERSION}, build ${KAIJU_BUILD_NUMBER}, from [\`${GITHUB_SHA:0:7}\`](https://github.com/${GITHUB_REPOSITORY}/commit/${GITHUB_SHA}): ${SUBJECT}

Built after this commit passed the build, unit and screenshot tests, lint, and the device runs.

**Install or update on your phone:** open ${LINK} in Chrome on the phone. An update installs over the app you have and keeps your classes and students. Steps: [docs/10-personal-builds.md](https://github.com/${GITHUB_REPOSITORY}/blob/main/docs/10-personal-builds.md).

| | |
|---|---|
| Package | \`com.earlthekaiju.coach\` |
| Version | ${VERSION} (build ${KAIJU_BUILD_NUMBER}) |
| Signing certificate SHA-256 | \`${CERT_SHA256}\` |
| APK SHA-256 | \`$(sha256sum "$APK" | cut -d' ' -f1)\` |
EOF

gh release create "$TAG" "$APK" \
  --repo "$GITHUB_REPOSITORY" \
  --target "$GITHUB_SHA" \
  --title "Kaiju Coach ${VERSION} · build ${KAIJU_BUILD_NUMBER}" \
  --notes-file "$RUNNER_TEMP/notes.md" \
  --latest
echo "Published $TAG: $LINK"

# Keep the 10 newest personal builds (older ones can't be installed over newer ones anyway).
gh release list --repo "$GITHUB_REPOSITORY" --limit 100 --json tagName,createdAt \
  --jq 'map(select(.tagName | startswith("build-"))) | sort_by(.createdAt) | reverse | .[10:] | .[].tagName' |
while read -r old; do
  gh release delete "$old" --repo "$GITHUB_REPOSITORY" --yes --cleanup-tag && echo "Removed old release $old"
done
