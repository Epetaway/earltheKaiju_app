# 01 · Product brief

## The problem

Teaching a Brazilian Jiu-Jitsu class means juggling a lesson plan in one place, a round timer in
another app, attendance on paper or in memory, and technique notes somewhere else — while
standing on a mat, often with one free hand, with students waiting. Context switching costs
class time and makes it hard to see what was taught last week and what comes next.

## The user

**Earl Hickson Jr.** — BJJ black belt and instructor (kids' and adult classes, private lessons),
teaching from an Android phone. He plans on his computer, teaches from the phone, and wants the
tool to feel like his brand rather than a generic fitness app.

Situations the app must handle:

| Moment | What he needs |
|---|---|
| Before class (home or gym) | See today's classes; adjust the plan; know who to expect. |
| Walking onto the mat | One tap to start the class. |
| Mid-class, standing | Current segment, time left, what's next, techniques — readable at arm's length. |
| Live rounds | A timer everyone can read and hear; start/pause without looking twice. |
| Students arriving | Mark attendance in one tap each; fix mistakes instantly. |
| After class | Record what worked; see attendance totals. |
| Weekly | Plan the week; reuse lesson plans. |

## Sprint 01 scope

| In | Out (later) |
|---|---|
| Today dashboard | Month / year curriculum views, recurring classes |
| Day/week planner, create/edit/cancel classes | Desktop companion and sync engine (architecture only) |
| Lesson builder with reusable templates | Media library functionality (concept only) |
| Class Mode with segment countdown, post-class notes | Website inquiry integration |
| Interval timer with presets and cues | Background/locked-screen timer service |
| Students, programs, attendance with undo | Restore from backup |
| Technique library | Light theme, localization |
| Settings: timer options, backup export, honest sync status | Student photos, face recognition (not planned without consent design) |

## Success criteria for Sprint 01

- Runs fully offline; nothing breaks without a network.
- Every screen above works end to end on real data and survives an app restart.
- One clear primary action per screen; every status readable without color.
- Builds, tests and renders screenshots in CI on every push; lint clean of errors.
- **The real test:** Earl teaches at least one class with it and writes down what got in the way.
  (Not done yet — see the device checklist in `07-timer-platform-behavior.md`.)

## Privacy stance

Store only what serves teaching: names, belts, programs, attendance, a short teaching note.
No birth dates, contacts, photos or medical information; an under-18 flag instead of a birth date.
Android cloud backup is off; backups are explicit exports. Sample data is fictional.
