# 09 · Dependency verification

Checked on **2026-10-20** against official release pages, then proven by a green GitHub Actions
build (the toolchain probe commit `4af01ad` built, tested and rendered a screenshot on the first
run). Versions live in `android/gradle/libs.versions.toml`.

| Dependency | Version | Source checked | Compatibility notes |
|---|---|---|---|
| Android Gradle Plugin | 9.3.3 | developer.android.com · AGP 9.3.0 release notes (latest patch 9.3.3) | Requires Gradle ≥ 9.5.0, JDK 17, build tools 36.0.0; supports up to API 37. |
| Gradle | 9.5.0 | Same compatibility table | Wrapper committed; CI uses JDK 21. |
| Kotlin (KGP) | 2.4.20 | JetBrains blog "Kotlin 2.4.20 released" (Sept 2026) | Supports Gradle up to 9.7. Pinned on the root build classpath because AGP 9's built-in Kotlin otherwise uses its own older KGP. |
| Compose compiler plugin | 2.4.20 | Ships with Kotlin | Must equal the KGP version. |
| KSP | 2.3.12 | google/ksp releases | AGP 9 requires KSP ≥ 2.3.6. |
| Compose BOM | 2026.09.00 | developer.android.com · "Use a Bill of Materials" | Compose 1.12.x (stable channel lists Compose 1.12.1, Sep 2026). |
| Activity Compose | 1.13.0 | AndroidX stable channel | — |
| Core KTX | 1.19.1 | AndroidX stable channel | 1.18+ needs compileSdk ≥ 36.1; project compiles against 37. |
| Lifecycle | 2.11.0 | AndroidX stable channel | runtime-compose, viewmodel-compose, process. |
| Navigation Compose | 2.10.2 | AndroidX stable channel | Type-safe routes via kotlinx.serialization. |
| Room | 2.8.5 | AndroidX stable channel | KSP2; schema export via the `androidx.room` Gradle plugin. Room 3 (`androidx.room3`) is stable too but a new package; staying on 2.8 for now. |
| DataStore | 1.2.1 | AndroidX stable channel | Preferences. |
| kotlinx.coroutines | 1.11.0 | Maven listing (May 2026) | — |
| kotlinx.serialization | 1.12.0 | klibs.io (Oct 19, 2026) | — |
| Robolectric | 4.17 | robolectric-bazel releases (Sept 2026) | Tests run on SDK 35. |
| Roborazzi | 1.76.0 | klibs.io (Sept 29, 2026) | Chosen over Paparazzi because Paparazzi ≤ 2.0.0-alpha04 has known AGP 9 issues. |

## SDK levels

| Setting | Value | Why |
|---|---|---|
| `compileSdk` | 37 (Android 17) | Required by current AndroidX; newest APIs available. |
| `targetSdk` | 36 (Android 16) | Android 17 behavior changes (notably local-network access) are reviewed with the sync work before opting in. |
| `minSdk` | 26 (Android 8.0) | `java.time`, adaptive icons, `VibrationEffect`; covers any phone Earl is likely to use. |

## Not used, on purpose

| Library | Reason |
|---|---|
| Hilt / Koin | Manual DI is enough (ADR-003). |
| material-icons-extended | Unmaintained artifact; original icon set instead (ADR-009). |
| Any analytics, crash reporting, ads or cloud SDK | Private app, offline-first, student data stays on the phone. |
| Downloadable fonts (Google Fonts provider) | Needs Play services + network; fonts are bundled instead (OFL permits it). |

## Upgrade routine

1. Read the release notes of the dependency.
2. Bump it in `libs.versions.toml` on a branch.
3. Let CI build, test, lint and re-render screenshots; compare the images in `ci-reports`.
4. Note anything surprising here.
