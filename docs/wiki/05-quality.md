# 5. Quality

[← Key decisions](04-decisions.md) · Next: [Challenges and lessons →](06-challenges-and-lessons.md)

The full review is [`08-qa-review.md`](../08-qa-review.md). Design and engineering were reviewed together, against screens the real code renders.

## The testing ladder

Each step up found problems the step below couldn't see.

| Step | What it checks | What it caught |
|---|---|---|
| **Unit and Robolectric tests** | 75 tests: the timer engine (pure JVM), repositories against a real Room database file, timer restore after process death, schema rules | Logic and persistence bugs |
| **Rendered screens** (Roborazzi) | 45 screen states: phone, landscape, tablet, 150% and 200% text, empty, live, paused, overtime, finished | Hidden actions, truncated labels, text-size problems |
| **Emulator device run** | A full class from an empty app on a Pixel 7 (Android 15) and a Pixel 9 (Android 17), with accessibility checks on every tap and text entry | The keyboard covering forms, inset problems, a mislabelled drag handle, a crash in the Android 17 test tooling |
| **My real phone** | The same flow on a Galaxy Note10+ (Android 12) with 3-button navigation, over USB and over Wi-Fi | Content sitting under the system navigation buttons |
| **On the mat** | Sound in a loud room, glare, sweaty hands, TalkBack by ear | Not done yet. This is the [hand checklist](../07-timer-platform-behavior.md). |

The full class flow passed on the real phone in 42 seconds on Oct 24, and again in 44.8 seconds after the fixes on Nov 16.

## Findings, by severity

33 findings were recorded, each with a fix and a commit. A sample:

| # | Finding | Severity | Fix |
|---|---|---|---|
| 1 | "Start class" sat below the fold on a standard phone | High | Moved directly under the class facts |
| 4 | Warning text in gold on deep emerald measured 3.81:1 | Medium | Warning text is Ivory (7.11:1); gold stays a large accent |
| 10 | Two fast taps could interleave database writes when changing segments | Medium | One Room transaction per segment change, with a test |
| 20 | With the keyboard open, editor action bars hid behind it (found only on the device run) | High | `imePadding` on the whole screen |
| 25 | On my phone, the timer's preset summary sat under the 3-button navigation bar | High | The timer pads for the navigation bar |
| 31 | No safe way to update the app without uninstalling and losing data | High | Signed personal builds ([decision 6](04-decisions.md#6-ship-updates-without-risking-real-data-adr-013)) |

## Accessibility

- Every colour pairing was measured against WCAG 2.2 ([table](../03-design-system.md#contrast-wcag-22-computed-from-the-hex-values)).
- Touch targets are at least 48 dp, and hero actions are 64 dp.
- Every icon button has a label, and headings are marked.
- Rows are merged for screen readers.
- State is never shown by colour alone.
- The system "remove animations" setting is respected.

The device run applies Android's Accessibility Test Framework on every interaction. Automated checks can't judge TalkBack by ear, so that's on the hand checklist.

## Open items

From the QA review's watch list:
- Restore from a backup isn't built yet. Export works.
- UI strings live in Kotlin, not `strings.xml`; they need extracting before any translation.
- Dark theme only. A bright gym may need a higher-contrast mode.
- One physical phone tested so far.
