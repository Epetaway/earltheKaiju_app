# Case study log — Kaiju Coach (Android)

Running record for the portfolio case study. Facts only: what was decided, built and tested,
and what is still unproven. No invented usage numbers or testimonials.

## 2026-10-20 · Sprint 01 kickoff to first full build

**Starting point.** Empty GitHub repository (`Epetaway/earltheKaiju_app`, public). Approved brand
package v1.5. The website project already exists separately.

**Constraints discovered.** The development environment could not download Android
dependencies (Google Maven and Gradle were unreachable). Rather than build blind, CI became the
build machine: every push builds, tests, renders every screen with Roborazzi and lints, then
publishes results to a `ci-reports` branch. This turned out to be a strength for the case study:
every screenshot in the docs is produced by the real code, reproducibly.

**Key decisions** (details in `decisions.md`):
- Native Kotlin + Compose; Room as the on-device source of truth; manual DI.
- Timer derived from a monotonic clock, restored after process death, late cues dropped.
- Lesson snapshot at class start so history never changes when templates do.
- Sync designed now (UUIDs, timestamps, tombstones in schema v1), built later.
- Original icon set and a chamfer-based shape system drawn from the crest.
- Minimal student data; under-18 flag instead of birth dates; cloud backup off.

**Design direction.** "Command center designed by an editorial studio": TODAY at 96sp over a gold
eyebrow and keystone rule; numbered section headers with hairlines; data rows separated by rules
instead of rounded stat cards; color reserved for meaning (emerald action, spectral live, gold
rank/rest, signal error) and always paired with words or shapes.

**Built.** Today, Planner (week + day), session editor/detail, lesson builder, Class Mode with
post-class notes, timer with presets and synthesized cues, students, attendance with undo,
technique library, media concept, settings with backup export and programs.

**Tested.** Timer engine (12 JVM tests), repositories against a real Room database (persistence
across reopen, snapshot rule, deletion rules, roster logic), timer restore after process death and
after reboot, ~40 rendered screen states including font scale 150%/200% and landscape.

**Not yet proven.** Use during a real class. Locked-screen timer cues (deliberately deferred).
Sync (designed, not built).

## 2026-10-20 → 10-24 · Design and engineering QA

**Method.** Reviewed every CI-rendered screen at phone, landscape, tablet and 150%/200% text, as
a designer and as an engineer in the same pass. Full table in `08-qa-review.md`.

**What the review caught.** The most important action on the most important screen ("Start
class" on Today) was below the fold. Buttons truncated their labels. Landscape Class Mode clipped
its own countdown. One color pair (gold on deep emerald) fell under 4.5:1. Large text broke the
nav bar, the class facts and attendance rows.

**What changed.** Start class moved up under the facts. Labels wrap; paired buttons stack at large
text; single-line labels auto-size. Landscape gets its own hero scale. The failing pair became
Ivory. An end-to-end test now drives the real activity from empty app to finished class.

**What the end-to-end test caught.** Its first run stalled while stepping quickly through
segments: taps landed before the screen had caught up with the previous one. Investigating it
turned up a real weakness: a segment change was several separate database writes that two fast
taps could interleave. The test now waits for each segment, and segment changes are a single
transaction with a regression test.
Its screenshots also exposed two small things the static renders hid: a class name breaking
mid-word at large text, and struck-through "done" segments that read as cancelled.

**Takeaway for the case study.** Rendering every state in CI made the review concrete: each
fix was confirmed by re-rendering the same state from the same code. For the case study, keep
future before/after pairs by saving the screenshot folder before each QA pass (the CI report
branch only holds the latest run).

## 2026-10-24 · Full slice and a real Android system

**Why.** The full kickoff brief asked for the whole vertical slice to be tested through the UI
and for visual checks on an emulator. Until then every screen had been rendered under
Robolectric only. Robolectric has no real window, system bars, keyboard or accessibility service.

**What was added.** One test drives the slice from an empty app through the real screens:
program → student → lesson (rename, add, reorder) → schedule → start → timer → attendance with
undo → finish with a note → restart. CI runs it twice: under Robolectric for speed, and on an
Android 15 Pixel 7 emulator with Google's Accessibility Test Framework checking every tap. The
emulator run takes full-screen screenshots, rotation and keyboard included.

**What the emulator caught that the renders couldn't.**
- The bottom-sheet drag handle was unlabeled and too small. The accessibility checks failed the
  run on it.
- With the keyboard open, a focused field and the Save bar were hidden behind it.
- In landscape, real status and navigation bars pushed Class Mode's quick actions off screen.

All three are fixed. Separately, the consistency review found that the class editor dropped
unsaved changes without asking, unlike the other editors.

**Takeaway.** Fast renders are good for breadth: every state at every text size. A real system
is good for depth: insets, input methods and accessibility services. Both now run on every push.

## Next entries

- First class taught with the app: what helped, what got in the way, what changed.
- Sprint 02 decisions (timer foreground service, restore, schema v2 for sync).
