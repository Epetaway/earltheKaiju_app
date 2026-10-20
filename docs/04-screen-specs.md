# 04 · Screen specifications

Status key: **Built** = working in Sprint 01 · **Concept** = navigable design preview, clearly
labeled, no functionality · **Planned** = documented only.

Screenshots for every screen are rendered from the real Compose code by CI
(`docs/screenshots/sprint-01/`). File names are given in each section.

---

## 01 · Today — Built
`01-today.png` · `01b-today-full.png` · `01c-today-live.png` · `01d-today-empty.png` · `01e-today-font-200.png`

**Job:** know the teaching day the moment the app opens, and start the next class in one tap.

| Area | Content |
|---|---|
| Header | Gold eyebrow `TUE · OCT 20`, settings control, **TODAY** at 96sp, keystone rule, one-line summary "3 classes today · next at 6:30 PM". |
| 01 Next class (or In progress) | Featured card (emerald or live accent edge, large chamfer): program eyebrow + status badge; lesson title at 40sp; data row *Starts 6:30 PM · Length 60 min · Expected 8* separated by hairlines; "Starts in 45 min" in gold when within 3 hours; objective; numbered segment preview; **Start class / Resume class** (hero); Attendance and Details. |
| 02 Also today | Class cards for the rest of the day (completed, canceled and later classes, each with its status). |
| Plan ahead | Schedule class · New lesson · Lesson library (n). |

**States.** Loading line. In progress: the live class is featured with its current segment marked.
Empty: "No class on the schedule" with *Schedule class* and *Saved lesson plans* (or *Build a lesson
plan* if none exist). Classes whose end time has passed are not featured as "next".

---

## 02 · Planner — Built
`02-planner-week.png` · `02b-planner-lessons.png` · `02c-session-editor.png` · `02d-session-detail.png`

**Job:** see and shape the week; keep the lesson library.

- **Schedule / Lessons** segmented control.
- Week navigator (previous / next week, *Today* shortcut) and a 7-day strip: weekday, large date,
  one diamond per class (accent = scheduled, spectral = live, muted = done); today's date is
  underlined in gold; selected day has a Facet fill and accent top bar.
- Selected day list of class cards; *Add* in the section header; *Schedule class* button.
- Status key: Scheduled / Live / Done, canceled struck through.
- **Lessons:** library cards (program, `planned / target MIN`, objective, segment count) and *New lesson plan*.

**Session editor** (new or edit): 01 When (date picker, time picker, length stepper in 5-min
steps) · 02 What (program sheet, lesson plan sheet, optional class name) · 03 Where (location) ·
04 Manage (cancel/restore, delete — both confirmed). Choosing a lesson borrows its program and length.

**Session detail:** program, status, title, date/time/length/location, Start or Resume (hero),
Attendance (present/expected), Add note, the lesson (template before the class, the snapshot after),
and notes.

Planned: month and year views, recurring classes, long-term curriculum.

---

## 03 · Lesson builder — Built
`03-lesson-builder.png` · `03b-lesson-builder-full.png`

**Job:** structure a class the way Earl teaches it, then reuse it.

| Section | Content |
|---|---|
| 01 Details | Title (required), program or audience, target length, teaching objective. |
| 02 Segments | Time budget: planned vs target as proportional blocks, a gold target marker and words ("15 MIN LEFT", "ON TARGET", "10 MIN OVER"). Numbered segment rows (gold 01, 02…), each with type, techniques and notes; tap to edit; move up/down buttons (48dp). *Add segment*. |
| 03 Notes | Instructor notes; *Keep in lesson library* switch. |
| 04 Use | Save and assign to a class · Duplicate · Delete (confirm says how many scheduled classes lose it). |

**Segment sheet:** title, minutes stepper + quick chips (5/10/15/20/30), type chips (a label only,
not a required structure), notes, technique chips from the library, *New technique* inline.

Unsaved changes are protected by a discard confirmation (back button and system back).

---

## 04 · Class Mode — Built
`04-class-mode.png` · `04b-class-mode-overtime.png` · `04c-class-mode-with-timer.png` · `04d-class-mode-landscape.png` · `04e-class-mode-font-150.png` · `04f-post-class.png`

**Job:** teach without fumbling: what's now, how long is left, what's next, and the controls a
thumb can reach.

- Distinct from planning screens: no bottom bar, Spectral live band across the top, LIVE eyebrow,
  screen kept on.
- Top bar: leave (the class keeps running), title, wall clock, class elapsed, *Finish*.
- Proportional segment progress bar.
- Hero: `02 / 04` (spectral index), segment type, title at 40–64sp, segment countdown at 104sp
  (turns gold with `+1:20 OVER` past its planned time), techniques, notes with a gold rule.
- Up next: the next segment in full, later ones compact.
- Thumb zone: Previous (64dp) and **Next segment** (hero; becomes **Finish class** on the last
  segment); Attendance (present/expected), Timer, Note as 64dp quick actions.
- When the round timer runs, a mini timer strip shows phase, round and time; tap to open.
- Landscape (≥600dp wide): segment hero on the left, up next + controls on the right.
- Restart-safe: status, current segment and segment start time are stored on the session row.

**Post-class:** CLASS COMPLETE, taught vs planned minutes, present vs expected, segments done;
notes for next time; review attendance; Done.

---

## 05 · Timer & clock — Built
`05-timer-work.png` · `05b-timer-rest.png` · `05c-timer-paused.png` · `05d-timer-ready.png` · `05e-timer-warning.png` · `05f-timer-finished.png` · `05g-timer-landscape.png`

**Job:** run rounds that everyone can read from across the mat.

| Phase | Field | Label | Numerals | Other cues |
|---|---|---|---|---|
| Ready | Shadow | READY (muted) | Ivory | "5 ROUNDS" |
| Get ready | Facet | GET READY | Ivory | 3-2-1 beeps |
| Train | Deep Emerald | TRAIN | Ivory | Double bell at start |
| Warning | Deep Emerald + gold border | TRAIN | Ivory | "Last 30 seconds", three clicks |
| Rest | Shadow + gold top band | REST (gold) | Ivory | Low bell; 3-2-1 before next round |
| Paused | Shadow + diagonal hatching | PAUSED (gold) | Muted | — |
| Finished | Shadow | TIME (gold) | Gold 0:00 | Triple bell |

Also: current wall clock, preset name, sound toggle, round `n OF m`, interval progress line,
"Next · Rest 1:00". Controls: Reset (64dp) and one hero button (Start / Pause / Resume / Start
again). Presets as chips (locked while running: "Reset to change"); *Custom…* opens the editor
(rounds, round length in 15 s steps, rest, get-ready, warning) with *Use once* or *Save preset*.

Engine and platform behavior: [07-timer-platform-behavior.md](07-timer-platform-behavior.md).

---

## 06 · Students — Built
`06-students.png` · `06b-student-detail.png` · `06c-student-editor.png` · `06d-students-empty.png`

- Header with active and program counts; Add student.
- Search by name; filter chips: Active, each program, Kids, Archived.
- Rows: name, KIDS marker for under-18s, belt graphic + words ("Blue · 2 stripes"), programs and
  classes in the last 30 days. **No contact or sensitive fields in the list.**
- **Detail:** belt at large size, programs, last 30 days / all time / last seen, teaching note,
  attendance history (present/absent with icon and word), each linking to its class.
- **Editor:** first/last name, Under 18 switch (switches to kids' belts; no birth date stored),
  belt chips + stripes/degrees stepper, program chips, teaching note with a privacy hint,
  Archive/Restore, Delete permanently (confirm explains what is removed).

---

## 07 · Attendance — Built
`07-attendance.png` · `07b-attendance-font-150.png`

- Reached from Class Mode, Today, class detail and post-class.
- Totals row: Present · Absent · Not marked (gold when anyone is unmarked), announced politely.
- *All present* (marks only the unmarked) and *Add drop-in* (search sheet of other active students).
- One row per expected student: name, belt, "Drop-in" note when not enrolled, two large toggles
  **✓ Present** (emerald fill) and **✕ Absent** (signal outline). Tapping the current mark clears it.
- Every change saves immediately (survives restart) and offers **Undo** in a snackbar; bulk marking
  undoes as one step.

---

## 08 · Library — Techniques Built · Media Concept
`08-library.png` · `08b-library-media-concept.png`

- **Techniques:** search, add, grouped by category with numbered section headers; cards show
  notes and how many lesson segments use each technique. Technique editor with category
  suggestions and delete.
- **Media (Concept):** a gold "Concept · planned for a later sprint" banner, placeholder tiles drawn
  as dashed frames (no fake thumbnails, no playback), and a plain list of what it will do: reference
  videos already on the phone, tag them, link them to lesson segments, play from Class Mode, later
  cast to a TV, never upload.

---

## 09 · Settings & sync — Built (sync: honest status only)
`09-settings.png` · `09b-settings-full.png` · `09c-programs.png`

| Section | Built in Sprint 01 |
|---|---|
| 01 Data & sync | Status panel: *Saved on this phone · OFFLINE-READY*, "Desktop sync isn't set up yet", paired computer *None*, last backup export (gold *Never* until one exists). **Export backup** (confirm warns the file contains student names; SAF file picker). |
| 02 Timer | Round sounds, vibration, keep screen on. |
| 03 Teaching | Programs (list, add/edit sheet with audience and planner accent, delete with consequences explained). |
| 04 About | Crest, version, privacy statement, font licenses, current theme. |
| 05 Debug build only | Load fictional sample data. |

Not shown because they don't exist yet: pairing, sync now, conflicts, restore, theme switch.
