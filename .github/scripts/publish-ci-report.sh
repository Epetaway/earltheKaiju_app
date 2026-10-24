#!/usr/bin/env bash
# Publishes this run's results to the `ci-reports` branch, one folder per source branch:
#   REPORT_KIND=build  (default) build logs, test summary, lint, Room schemas, Roborazzi screenshots
#   REPORT_KIND=device emulator run: instrumented test summary, device screenshots, logcat
# The branch is force-pushed as a single commit so screenshots never bloat history; the two
# jobs use separate folders and a lease so neither overwrites the other's latest report.
# Reports contain build output only: no secrets, no user data.
set -uo pipefail

KIND="${REPORT_KIND:-build}"
BRANCH="${GITHUB_HEAD_REF:-${GITHUB_REF_NAME}}"
SLUG="$(echo "$BRANCH" | tr '/' '-')"
[ "$KIND" = "device" ] && SLUG="${SLUG}-device"
WORK="$RUNNER_TEMP/ci-report-$KIND"
OUT="$WORK/out/$SLUG"
rm -rf "$WORK" && mkdir -p "$OUT/screenshots" "$OUT/logs"

# Logs (tails only) and an error digest that also becomes GitHub annotations.
for f in ci-*.log; do
  [ -f "$f" ] && tail -n 3000 "$f" > "$OUT/logs/$f"
done
grep -hE '^e: |^w: .*(deprecated|unused)|error:|FAILED|What went wrong|Could not|Caused by|Exception' ci-*.log 2>/dev/null | head -400 > "$OUT/errors.txt" || true
grep -hE '^e: ' ci-*.log 2>/dev/null | head -50 | while IFS= read -r line; do
  echo "::error::${line:0:900}"
done

summarize_tests() { # $1 = glob of JUnit XML files
python3 - "$OUT/tests.txt" "$1" <<'PY' || true
import glob, sys, xml.etree.ElementTree as ET
lines, total, fails = [], 0, 0
for f in sorted(glob.glob(sys.argv[2], recursive=True)):
    r = ET.parse(f).getroot()
    for tc in r.iter('testcase'):
        total += 1
        bad = tc.find('failure') if tc.find('failure') is not None else tc.find('error')
        name = f"{tc.get('classname')}.{tc.get('name')}"
        if bad is not None:
            fails += 1
            lines.append(f"FAIL {name}\n    {(bad.get('message') or '')[:600]}\n" + (bad.text or '')[:15000])
        else:
            lines.append(f"ok   {name} ({tc.get('time')}s)")
open(sys.argv[1], 'w').write(f"{total} tests, {fails} failed\n\n" + "\n".join(lines) + "\n")
PY
}

if [ "$KIND" = "device" ]; then
  cp device-out/shots/*.png "$OUT/screenshots/" 2>/dev/null || true
  cp device-out/device.txt device-out/logcat.txt "$OUT/" 2>/dev/null || true
  # The debug app and its instrumented test, for android/scripts/run-on-phone.sh.
  mkdir -p "$OUT/apk" && cp device-out/apk/*.apk "$OUT/apk/" 2>/dev/null || true
  summarize_tests 'android/app/build/outputs/androidTest-results/**/*.xml'
  STATUS_FIELDS="\"build\": \"${BUILD:-}\", \"device\": \"${DEVICE:-}\","
else
  cp android/app/build/outputs/roborazzi/*.png "$OUT/screenshots/" 2>/dev/null || true
  summarize_tests 'android/app/build/test-results/**/*.xml'
  cp android/app/build/reports/lint-results-debug.txt "$OUT/lint.txt" 2>/dev/null || true
  [ -d android/app/schemas ] && cp -r android/app/schemas "$OUT/schemas"
  STATUS_FIELDS="\"assemble\": \"${ASSEMBLE:-}\", \"tests\": \"${TESTS:-}\", \"lint\": \"${LINT:-}\","
fi

cat > "$OUT/status.json" <<JSON
{
  "sha": "${GITHUB_SHA}",
  "run_id": "${GITHUB_RUN_ID}",
  "branch": "${BRANCH}",
  "kind": "${KIND}",
  ${STATUS_FIELDS}
  "finished_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
JSON

REPO_URL="https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
cd "$WORK"
for attempt in 1 2 3 4 5; do
  rm -rf wt
  if git ls-remote --exit-code --heads "$REPO_URL" ci-reports >/dev/null 2>&1; then
    git clone -q --depth 1 --branch ci-reports "$REPO_URL" wt || { sleep 5; continue; }
    BASE="$(git -C wt rev-parse HEAD)"
  else
    git init -q wt && git -C wt remote add origin "$REPO_URL"
    BASE=""
  fi
  rm -rf "wt/$SLUG" && cp -r "out/$SLUG" "wt/$SLUG"
  cat > wt/README.md <<'MD'
# CI reports (generated)

Written by `.github/scripts/publish-ci-report.sh` on every push, one folder per branch:
build logs, test summary, lint report, Room schemas and Roborazzi screenshots; and in
`<branch>-device/` the emulator run's test summary, device screenshots and logcat.
This branch is replaced on every run. Do not merge it.
MD
  (
    cd wt
    git config user.name "kaiju-coach-ci"
    git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
    git checkout -q --orphan publish
    git add -A
    git commit -q -m "CI report: ${BRANCH} @ ${GITHUB_SHA:0:7} (run ${GITHUB_RUN_ID})"
    # Fails (and retries from a fresh clone) if the other job published in the meantime.
    git push -q --force-with-lease="ci-reports:${BASE}" origin publish:ci-reports
  ) && { echo "Published CI report for ${BRANCH}"; exit 0; }
  sleep 5
done
echo "::warning::Could not publish CI report"
exit 0
