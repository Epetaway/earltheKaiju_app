# Decisions (lightweight ADRs)

Each entry: the decision, why, and what it costs. Newest last.

### ADR-001 · Native Kotlin + Jetpack Compose
**Decision.** Native Android with Compose, not React Native/Flutter.
**Why.** The brief asks for it; the app leans on platform pieces (keep-screen-on, audio routing,
haptics, future foreground service and local-network discovery) that are first-class natively;
and a native case study complements the React/TypeScript website case study.
**Cost.** A second language in the portfolio stack; the desktop companion can't share UI code.

### ADR-002 · Room on the phone is the source of truth
**Decision.** All data lives in Room; there is no server.
**Why.** Classes happen without reliable signal; the phone must never wait on anything.
**Cost.** Sync has to be designed carefully (see 06-sync-architecture.md). Stable UUIDs,
`updatedAt` and tombstones were built into schema v1 so that work doesn't need a rewrite.

### ADR-003 · Manual dependency injection
**Decision.** One `AppContainer` object instead of Hilt/Koin.
**Why.** ~10 long-lived objects, one module, one developer. Manual wiring is readable, has no
annotation processing beyond Room, and avoided AGP 9 / Hilt compatibility risk at project start.
**Cost.** If the object graph grows a lot, Hilt becomes worth adding; screens already take plain
constructor parameters, so the switch is mechanical.

### ADR-004 · AGP 9 with built-in Kotlin, KGP pinned on the classpath
**Decision.** Use AGP 9.3.3's built-in Kotlin (no `kotlin-android` plugin) and pin KGP 2.4.20 via
the root `buildscript` classpath so the Compose compiler and serialization plugins match it.
**Why.** Current stable toolchain; AGP 10 removes the old path entirely.
**Cost.** Some older plugins (e.g. Paparazzi ≤ 2.0.0-alpha04) don't support AGP 9, which is one
reason screenshots use Roborazzi.

### ADR-005 · Timer derived from a monotonic clock
**Decision.** Store anchors, derive everything from `elapsedRealtime()`; redraw on second
boundaries; persist anchors; drop late cues.
**Why.** The brief requires accuracy independent of frame rate, and a timer that survives the
app being backgrounded or killed.
**Cost.** A reboot mid-round loses the run (detected and handled).

### ADR-006 · Lesson snapshot when a class starts
**Decision.** Copy the plan's segments (and technique names as text) into `session_segment` at
Start Class.
**Why.** Past classes must keep what was actually taught even when a template changes.
**Cost.** Some duplication of text; intended.

### ADR-007 · GitHub Actions as the build machine, reports on a branch
**Decision.** CI builds, tests, renders screenshots and lints every push, then force-pushes a
single-commit `ci-reports` branch with logs, test summary, lint, schemas and PNGs.
**Why.** The development environment can't download Android dependencies; the reports branch makes
results readable with plain git (Actions API downloads aren't available there), and keeps
screenshots out of `main`'s history.
**Cost.** One extra branch that must never be merged (its README says so). The debug APK is a
14-day Actions artifact.

### ADR-008 · English strings in code for Sprint 01
**Decision.** UI copy lives in the composables, not `strings.xml`.
**Why.** A private, English-only tool; faster iteration on wording during design review.
**Cost.** Extracting strings is required before any localization. Tracked as tech debt.

### ADR-009 · Original icon set
**Decision.** Draw a 24dp icon set (47 icons) instead of `material-icons-extended`.
**Why.** The extended icon artifact is no longer maintained, and square-capped, mitred icons match
the crest's line-work better than rounded Material symbols.
**Cost.** New icons are drawn by hand (SVG path strings in `KaijuIcons.kt`).

### ADR-010 · Shadow theme only in Sprint 01
**Decision.** Ship the dark Shadow theme only.
**Why.** The brand is Shadow-heavy (≈70%); a second theme doubles visual QA before the first is
proven on the mat. The brand's Ivory tokens exist and can become a light theme later.
**Cost.** No light mode for very bright rooms yet; listed in the QA review as a watch item.

### ADR-011 · Minimal student data
**Decision.** Store name, belt/stripes, under-18 flag, programs and one teaching note. No birth
dates, contacts, photos or medical data.
**Why.** Children's data; only store what serves teaching. Contact details stay in the website's
inquiry flow.
**Cost.** Parent contact lookup happens outside the app for now.

### ADR-012 · Sprint 01 sign-off
**Decision.** On 2026-10-24 Earl approved the Sprint 01 open items ("Yes to all"):
- The visual direction stands as built: Shadow-only theme, editorial condensed headings,
  chamfered shapes, emerald for action, spectral for live, gold for labels, rank and rest, and
  the crest on the launcher and in Settings only.
- The repository stays **public** for now. That makes it a readable portfolio piece and keeps CI
  minutes free. Real student data and private media never enter git by design: data lives on the
  phone, and backups and phone screenshots are git-ignored.
- The code stays **all rights reserved** (`NOTICE.md`) until a license is chosen on purpose.
- `main` is protected: changes arrive through a pull request with both CI checks passing.
- PR #1 merges into `main`.
**Cost.** A public repo makes the brand artwork easy to download; `NOTICE.md` states ownership.
Revisit visibility before any private assets are added.
