# 08 · QA review — Sprint 01

Design and engineering were reviewed together, against the screens the code actually renders.
Every screenshot comes from CI with fictional data, so a finding and its fix can both be
checked against the same source. There are two kinds: Roborazzi renders under Robolectric (every
state, fast), and full-screen captures from an Android emulator running the real app (system
bars, keyboard, rotation, accessibility services).

## How the review was run

| Pass | What was checked | Evidence |
|---|---|---|
| Rendered states | 45 screen states: phone portrait (412×915 dp), tall scrolls, landscape (915×412), tablet (900×1280 with nav rail), font scale 150% and 200%, empty, live, paused, overtime, finished | `docs/screenshots/sprint-01/` |
| Hierarchy | One dominant action per screen, above the fold; section order matches the order of use on the mat | Screens 01, 04, 05, 07 |
| Typography | Barlow Condensed for display, labels and numerals (tabular figures on every clock); Manrope for reading; no truncated labels at 100–200% | Screens 01e, 02e, 04e, 06e, 07b |
| Consistency | Same chamfer shapes, section headers, button sizes, status badges and spacing tokens on every screen; colors only from semantic roles | `designsystem/`, sheets 10 and 11 |
| Accessibility | Contrast (see design system table), 48 dp minimum targets, 64 dp hero actions, labels on every icon button, headings marked, rows merged for screen readers, state never shown by color alone, system "remove animations" respected | Code review + screenshots |
| Responsive | Bottom bar under 600 dp, nav rail at 600 dp+, content capped at 640 dp, Class Mode and Timer re-laid out for landscape | Screens 04d, 05g, 12 |
| Real-world use | Readable from across the mat, operable with one hand, nothing needs the network, screen stays on in Class Mode, no data loss on rotate, process death or accidental back | Tests + screens 04, 05 |
| Device run | The whole vertical slice on an Android 15 Pixel 7 emulator: Accessibility Test Framework checks (labels, touch targets, contrast, traversal) on every tap and text entry; landscape rotation mid-class; keyboard over a form; activity restart | `docs/screenshots/sprint-01/device/` |
| Engineering | Build, 67 unit/Robolectric tests plus the instrumented device flow, lint clean, Room schema exported | CI report on the `ci-reports` branch |

## Findings and fixes

Severity: **High** blocks real use in class · **Medium** degrades it · **Low** polish.

| # | Finding | Severity | Fix | Commit |
|---|---|---|---|---|
| 1 | Today: "Start class" sat below the fold on a standard phone. | High | Hero action moved directly under the class facts; objective and segments follow it. | `659c138` |
| 2 | Button labels truncated ("Attendan…", "Schedule cl…"). | High | Labels wrap to two lines; side padding tightened; paired buttons stack at large text (`ButtonPair`). | `659c138` |
| 3 | Class Mode landscape clipped the countdown. | High | Landscape hero uses a smaller index and title and a 96 sp countdown. | `659c138` |
| 4 | Warning text in gold on deep emerald measured 3.81:1. | Medium | Warning text is Ivory (7.11:1); gold stays a large-graphic accent there. | `659c138` |
| 5 | At 200% text, nav labels and the class facts truncated. | Medium | Single-line labels auto-size (`FitText`); the facts row becomes stacked rows (`FactRow`). | `659c138` |
| 6 | Class Mode quick-action labels broke mid-word. | Medium | Auto-sized single-line labels. | `659c138` |
| 7 | Attendance rows cramped at 150% text. | Medium | Rows stack name above the Present / Absent pair at large text. | `659c138` |
| 8 | Class Mode top bar truncated its eyebrow and class name at 150%. | Medium | Eyebrow shortens to "Live"; the elapsed caption drops its label; the class name shrinks to fit one line instead of breaking mid-word. | `94d066e`, `312f025`, `a2139e0` |
| 9 | No test exercised the app as a whole. | Medium | `AppFlowTest`: launches `MainActivity`, loads sample data, starts tonight's class, steps through every segment, finishes, runs the timer, opens a student. | `94d066e` |
| 10 | Class Mode moved between segments with several separate database writes; two fast taps could interleave them. | Medium | One Room transaction per segment change; new test covers stepping back and out-of-range taps. | `9f94801` |
| 11 | Student detail "Last seen" showed a bare date. | Low | Shows "Today" or "N days ago". | `659c138` |
| 12 | Empty Today offered the same action twice. | Low | "Plan ahead" only appears when there are classes. | `659c138` |
| 13 | Screen eyebrows truncated beside header actions at 200%. | Low | Eyebrows wrap to two lines. | `94d066e` |
| 14 | Finished segments were struck through, so the post-class "Taught" list read like a list of cancelled items. | Low | Finished segments show a muted title with a check; strikethrough is reserved for cancelled classes. | `312f025` |
| 15 | Lint: unused drawables; brand vectors over the path-length guideline; one modifier-order warning. | Low | Unused drawables removed. Crest and launcher vectors keep their full geometry, marked `tools:ignore="VectorPath"` with a comment. Modifier order fixed. | `659c138`, `a2139e0` |
| 16 | `LiveDot` drew with the wrong size (a parameter shadowed the canvas size). | Build | Uses the canvas size explicitly. | `169cbca` |
| 17 | Bottom-sheet drag handle had no label and was under 48 dp. It carries expand and dismiss actions for screen readers. Found by the device run's accessibility checks. | Medium | The gold bar sits in a labeled 64×48 dp target. | `a0a2830` |
| 18 | The class editor discarded unsaved changes silently, while the lesson and student editors asked first. | Low | Same "Discard changes?" confirmation on back. | `a09cbf0` |
| 19 | The core flow was only tested in pieces through the UI. | Medium | `AppFlowTest.buildScheduleAndTeachFromAnEmptyApp` and `DeviceFlowTest` drive Today → program → student → lesson → schedule → start → timer → attendance (with undo) → finish → restart from an empty app. | `f9316bf` |

## Open watch items

These are known and accepted for Sprint 01. None blocks teaching a class.

- **Nav labels at 200% text** fit by shrinking one at a time, so "Today" renders larger than
  "Students". Readable, but uneven. A shared size across the bar would look calmer.
- **Class Mode landscape** shows the segment's coaching cue just below the fold on a 412 dp-tall
  phone; it scrolls. Moving the cue into the right column in landscape would remove the scroll.
- **Long class names in Class Mode at 150%+ text** shrink to fit and then end in an ellipsis.
  The full name is on the screen you came from; keep class titles short.
- **Locked-screen timer cues** are not built. The timer keeps correct time and catches up on
  return, but it does not sound while the phone is locked. Plan and limits in
  [07-timer-platform-behavior.md](07-timer-platform-behavior.md).
- **Dark theme only.** Chosen for the mat. A very bright gym may call for a higher-contrast
  mode; decide after real use.
- **Strings live in Kotlin**, not `strings.xml`. Fine for a single-language private app; move
  them before any translation.
- **Not yet run on a physical phone.** It runs on an emulator in CI, with automated
  accessibility checks, but nothing automated can judge TalkBack flow by ear, speaker volume in
  a loud room, glare at a distance, or touch accuracy with sweaty hands. The on-device checklist
  is in [07-timer-platform-behavior.md](07-timer-platform-behavior.md#device-checklist-for-earl).

## Acceptance criteria (kickoff brief §14)

| Criterion | Status | Evidence |
|---|---|---|
| Initialized in the existing GitHub repository | Met | `Epetaway/earltheKaiju_app`, branch `sprint/01-foundation`, PR #1; `main` untouched |
| Coherent Earl the Kaiju design system | Met | [03-design-system.md](03-design-system.md), `designsystem/`, sheets 10–11 |
| Core screens have professional, consistent UI | Met, pending Earl's design review | Screenshots and this review |
| Navigation functions | Met | Both flow tests; back, restart and rotation covered |
| Create and edit a lesson plan | Met | Flow tests (create, rename, add, reorder); repository test (save, reorder, reload) |
| Schedule a class | Met | Flow tests |
| Assign a lesson to a class | Met | Flow tests (*Save and assign to a class*) |
| Start and finish that class | Met | Flow tests; lesson snapshot test |
| Operate the training timer | Met | Engine (12) and controller (3) tests; flow tests start it from Class Mode |
| Create student records with test data | Met | Flow tests; debug sample data |
| Mark attendance | Met | Flow tests, including undo; repository restore-after-restart test |
| The local database preserves the information | Met | Repository tests reopen the database; flow tests restart the activity |
| Works without internet | Met by construction | No `INTERNET` permission in the manifest, so the app cannot use the network |
| Meaningful automated tests | Met | 68 tests (below) |
| Completed features clearly documented | Met | Status per screen in [04-screen-specs.md](04-screen-specs.md), README, PR |
| UI reviewable through real previews or screenshots | Met | `docs/screenshots/sprint-01/` (Robolectric) and `.../device/` (emulator) |
| Architecture ready for desktop sync | Met (designed) | [06-sync-architecture.md](06-sync-architecture.md); schema v1 has UUIDs, timestamps and tombstones; `contracts/` |

## Verification at the end of the sprint

- CI on `sprint/01-foundation`: assemble, unit and screenshot tests, lint, and the emulator device
  run all pass.
- Tests (68): timer engine (12), repositories against a real Room database (10), timer restore
  after process death and reboot (3), rendered screens (40), end-to-end flows under Robolectric
  (2), and the device flow on an emulator with accessibility checks (1).
- Lint: no issues.
