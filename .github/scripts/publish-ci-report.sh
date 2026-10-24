#!/usr/bin/env bash
# Publishes this run's build logs, test summary, lint report, Room schemas and
# Roborazzi screenshots to the `ci-reports` branch (one folder per source branch).
# The branch is force-pushed as a single commit so screenshots never bloat history.
# Reports contain build output only: no secrets, no user data.
set -uo pipefail

BRANCH="${GITHUB_HEAD_REF:-${GITHUB_REF_NAME}}"
SLUG="$(echo "$BRANCH" | tr '/' '-')"
WORK="$RUNNER_TEMP/ci-report"
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

# Screenshots, test results, lint, schemas.
cp android/app/build/outputs/roborazzi/*.png "$OUT/screenshots/" 2>/dev/null || true
python3 - "$OUT/tests.txt" <<'PY' || true
import glob, sys, xml.etree.ElementTree as ET
lines, total, fails = [], 0, 0
for f in sorted(glob.glob('android/app/build/test-results/**/*.xml', recursive=True)):
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
cp android/app/build/reports/lint-results-debug.txt "$OUT/lint.txt" 2>/dev/null || true
[ -d android/app/schemas ] && cp -r android/app/schemas "$OUT/schemas"

cat > "$OUT/status.json" <<JSON
{
  "sha": "${GITHUB_SHA}",
  "run_id": "${GITHUB_RUN_ID}",
  "branch": "${BRANCH}",
  "assemble": "${ASSEMBLE:-}",
  "tests": "${TESTS:-}",
  "lint": "${LINT:-}",
  "finished_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
JSON

REPO_URL="https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
cd "$WORK"
for attempt in 1 2 3; do
  rm -rf wt
  if git ls-remote --exit-code --heads "$REPO_URL" ci-reports >/dev/null 2>&1; then
    git clone -q --depth 1 --branch ci-reports "$REPO_URL" wt || { sleep 5; continue; }
  else
    git init -q wt && git -C wt remote add origin "$REPO_URL"
  fi
  rm -rf "wt/$SLUG" && cp -r "out/$SLUG" "wt/$SLUG"
  cat > wt/README.md <<'MD'
# CI reports (generated)

Written by `.github/scripts/publish-ci-report.sh` on every push: build logs, test
summary, lint report, Room schemas and Roborazzi screenshots, one folder per branch.
This branch is replaced on every run. Do not merge it.
MD
  (
    cd wt
    git config user.name "kaiju-coach-ci"
    git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
    git checkout -q --orphan publish
    git add -A
    git commit -q -m "CI report: ${BRANCH} @ ${GITHUB_SHA:0:7} (run ${GITHUB_RUN_ID})"
    git push -q -f origin publish:ci-reports
  ) && { echo "Published CI report for ${BRANCH}"; exit 0; }
  sleep 5
done
echo "::warning::Could not publish CI report"
exit 0
