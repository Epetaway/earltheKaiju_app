#!/usr/bin/env bash
# Runs Kaiju Coach's device flow test on an Android phone plugged into this Mac over USB, then
# collects full-screen screenshots and logs. Leaves the debug app installed for hands-on use.
#
#   bash android/scripts/run-on-phone.sh [folder with the two APKs]
#
# The APKs default to device-test/apk/ at the repo root (CI's "Device run" publishes them; a
# local `./gradlew assembleDebug assembleDebugAndroidTest` produces them too). Results land in
# device-test/<timestamp>/. device-test/ is git-ignored: phone screenshots show your status bar
# and must not be committed.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APK_DIR="${1:-$ROOT/device-test/apk}"
OUT="$ROOT/device-test/$(date +%Y%m%d-%H%M%S)"
APP_ID="com.earlthekaiju.coach.debug"
RUNNER="$APP_ID.test/com.earlthekaiju.coach.device.KaijuTestRunner"

say() { printf '\n== %s\n' "$*"; }

ADB="$(command -v adb || true)"
[ -z "$ADB" ] && [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ] && ADB="$HOME/Library/Android/sdk/platform-tools/adb"
if [ -z "$ADB" ]; then
  echo "adb is not installed. Install it with Homebrew, then run this again:"
  echo "  brew install --cask android-platform-tools"
  exit 1
fi

APP_APK="$APK_DIR/app-debug.apk"
TEST_APK="$APK_DIR/app-debug-androidTest.apk"
for f in "$APP_APK" "$TEST_APK"; do
  [ -f "$f" ] || { echo "Missing $f"; exit 1; }
done

say "Phones connected"
"$ADB" devices -l
STATE="$("$ADB" get-state 2>&1 || true)"
if [ "$STATE" != "device" ]; then
  echo
  echo "No authorized phone yet ($STATE). On the phone:"
  echo "  1. Settings > About phone > Software information > tap Build number 7 times"
  echo "  2. Settings > Developer options > turn on USB debugging"
  echo "  3. Unplug and replug the cable, then tap Allow on the 'Allow USB debugging?' prompt"
  echo "  (Samsung: if installs are blocked, turn off Settings > Security and privacy > Auto Blocker)"
  exit 1
fi

mkdir -p "$OUT/shots"
{
  echo "Model:   $("$ADB" shell getprop ro.product.model | tr -d '\r')"
  echo "Android: $("$ADB" shell getprop ro.build.version.release | tr -d '\r') (API $("$ADB" shell getprop ro.build.version.sdk | tr -d '\r'))"
  echo "One UI:  $("$ADB" shell getprop ro.build.version.oneui | tr -d '\r')"
  "$ADB" shell wm size | tr -d '\r'
  "$ADB" shell wm density | tr -d '\r'
  echo "Font scale: $("$ADB" shell settings get system font_scale | tr -d '\r')"
} | tee "$OUT/device.txt"

say "Installing the debug app and its test"
"$ADB" install -r -t "$APP_APK" || { echo "Install failed (see above)."; exit 1; }
"$ADB" install -r -t "$TEST_APK" || { echo "Test install failed (see above)."; exit 1; }

say "Running the class flow on the phone (about 2 minutes; keep the phone unlocked and untouched)"
"$ADB" shell rm -rf /data/local/tmp/kaiju
"$ADB" shell mkdir -p /data/local/tmp/kaiju
"$ADB" logcat -c || true
"$ADB" shell am instrument -w -e class com.earlthekaiju.coach.device.DeviceFlowTest "$RUNNER" | tee "$OUT/instrument.txt"

"$ADB" pull /data/local/tmp/kaiju/. "$OUT/shots/" > /dev/null 2>&1 || true
"$ADB" logcat -d -v time AndroidRuntime:E TestRunner:I '*:S' > "$OUT/logcat.txt" 2>&1 || true
"$ADB" shell rm -rf /data/local/tmp/kaiju

say "Result"
if grep -q "OK (1 test)" "$OUT/instrument.txt"; then echo "PASSED"; else echo "FAILED (details in $OUT/instrument.txt)"; fi
echo "Screenshots: $(ls "$OUT/shots" | wc -l | tr -d ' ') in $OUT/shots"
echo "The debug app (Kaiju Coach, with a .debug suffix) stays installed for you to try."
