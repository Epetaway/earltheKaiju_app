# CI reports (generated)

Written by `.github/scripts/publish-ci-report.sh` on every push, one folder per branch:
build logs, test summary, lint report, Room schemas and Roborazzi screenshots; in
`<branch>-device/` the Pixel 9 (Android 17) emulator run's test summary, device screenshots,
logcat and APKs; and in `<branch>-device-api35/` the same for the Pixel 7 (Android 15) run.
This branch is replaced on every run. Do not merge it.
