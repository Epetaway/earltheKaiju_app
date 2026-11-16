#!/usr/bin/env bash
# Runs Kaiju Coach's device flow test on an Android phone connected to this Mac (USB or wireless
# debugging), then collects full-screen screenshots and logs. Leaves the debug app ("Kaiju Test")
# installed for hands-on use. It never touches the personal build ("Kaiju Coach"): that's a
# different app with its own data.
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

# Pick one phone. With wireless debugging adb can list the same phone twice (ip:port and an
# mDNS name); either entry works. ANDROID_SERIAL, if already set, wins.
# Sets ANDROID_SERIAL and STATE in this shell (not in a $(...) subshell, which would lose them).
pick_device() {
  if [ -z "${ANDROID_SERIAL:-}" ]; then
    local first
    first="$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
    [ -n "$first" ] && export ANDROID_SERIAL="$first"
  fi
  STATE="$("$ADB" get-state 2>&1 || true)"
}

say "Phones connected"
"$ADB" devices -l
pick_device
if [ "$STATE" != "device" ]; then
  # A stale adb server is a common cause; restart it once before giving up.
  "$ADB" kill-server > /dev/null 2>&1 || true
  "$ADB" start-server > /dev/null 2>&1 || true
  sleep 2
  "$ADB" devices -l
  pick_device
fi
if [ "$STATE" != "device" ]; then
  say "Android phones on this Mac's USB bus (serial numbers left out)"
  USB="$(system_profiler SPUSBHostDataType SPUSBDataType 2>/dev/null | grep -iE -B3 -A10 'samsung|0x04e8|google|pixel|0x18d1' | grep -viE 'serial')"
  if [ -n "$USB" ]; then
    echo "$USB"
    echo
    echo "The Mac sees the phone over USB, but adb can't use it yet: USB debugging is off,"
    echo "or the 'Allow USB debugging?' prompt on the phone hasn't been accepted."
  else
    echo "None. The Mac doesn't see an Android phone over USB at all: try another cable"
    echo "(some only charge) or another port, and unlock the phone. Or use wireless debugging."
  fi
  echo
  echo "No authorized phone yet ($STATE). On the phone:"
  echo "  1. Settings > About phone (Samsung: then Software information) > tap Build number 7 times"
  echo "  2. Settings > Developer options > turn on USB debugging"
  echo "  3. Unplug and replug the cable, then tap Allow on the 'Allow USB debugging?' prompt"
  echo "  (Samsung: if installs are blocked, turn off Settings > Security and privacy > Auto Blocker)"
  exit 1
fi

mkdir -p "$OUT/shots"
{
  echo "Model:   $("$ADB" shell getprop ro.product.model | tr -d '\r')"
  echo "Android: $("$ADB" shell getprop ro.build.version.release | tr -d '\r') (API $("$ADB" shell getprop ro.build.version.sdk | tr -d '\r'))"
  ONEUI="$("$ADB" shell getprop ro.build.version.oneui | tr -d '\r')"
  [ -n "$ONEUI" ] && echo "One UI:  $ONEUI"
  "$ADB" shell wm size | tr -d '\r'
  "$ADB" shell wm density | tr -d '\r'
  echo "Font scale: $("$ADB" shell settings get system font_scale | tr -d '\r')"
} | tee "$OUT/device.txt"

# CI debug builds share one key (docs/10-personal-builds.md), so each installs over the last.
# A build signed by another key (a local Android Studio build, or a CI build from before the
# shared key) can't update in place (INSTALL_FAILED_UPDATE_INCOMPATIBLE). The test package holds
# no data and is simply replaced. The debug app may hold classes and students you entered, so it
# is never removed here. -d allows installing an older build over a newer one (debug builds only).
install_apk() { # $1 apk, $2 package, $3 disposable|keep
  local out
  out="$("$ADB" install -r -t -d "$1" 2>&1)"; echo "$out"
  echo "$out" | grep -q "^Success" && return 0
  if echo "$out" | grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE"; then
    if [ "$3" = "disposable" ]; then
      echo "Replacing the earlier $2 (signed by another build; it holds no data)."
      "$ADB" uninstall "$2" > /dev/null 2>&1 || true
      "$ADB" install -r -t -d "$1" && return 0
    else
      echo
      echo "The Kaiju Test app on the phone was signed by another key, so Android won't update it"
      echo "in place. Removing it deletes the classes and students saved in Kaiju Test (your"
      echo "personal Kaiju Coach app and its data are separate and stay as they are)."
      echo "If that's fine, uninstall Kaiju Test on the phone, then run this again."
    fi
  fi
  return 1
}

say "Installing the debug app and its test"
echo "Phone: ${ANDROID_SERIAL:-}"
install_apk "$APP_APK" "$APP_ID" keep || { echo "Install failed (see above)."; exit 1; }
install_apk "$TEST_APK" "$APP_ID.test" disposable || { echo "Test install failed (see above)."; exit 1; }

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
echo "The debug app (Kaiju Test) stays installed for you to try. Your personal build was not touched."
