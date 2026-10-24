#!/usr/bin/env bash
# Runs inside android-emulator-runner (working directory: android/) once the emulator has
# booted: instrumented tests, then device screenshots and logs into ../device-out/.
set -uo pipefail
OUT=../device-out
mkdir -p "$OUT/shots"

adb shell rm -rf /data/local/tmp/kaiju
adb shell mkdir -p /data/local/tmp/kaiju
# Show the on-screen keyboard even though the emulator reports a hardware keyboard.
adb shell settings put secure show_ime_with_hard_keyboard 1 || true
adb logcat -c || true
{
  echo "Android $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"
  adb shell getprop ro.product.model | tr -d '\r'
  adb shell wm size | tr -d '\r'
  adb shell wm density | tr -d '\r'
} > "$OUT/device.txt" 2>&1

./gradlew connectedDebugAndroidTest --stacktrace 2>&1 | tee ../ci-device.log
STATUS=${PIPESTATUS[0]}

adb pull /data/local/tmp/kaiju/. "$OUT/shots/" > /dev/null 2>&1 || true
adb logcat -d -v time AndroidRuntime:E TestRunner:I AccessibilityValidator:V '*:S' > "$OUT/logcat.txt" 2>&1 || true
ls "$OUT/shots" | wc -l | xargs echo "device screenshots:"
exit "$STATUS"
