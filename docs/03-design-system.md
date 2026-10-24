# 03 · Design system

**Kaiju Coach** is a martial-arts command center designed like an editorial brand system:
Shadow-heavy, strict grid, big condensed numerals against small tracked labels, one controlled
angle borrowed from the crest, and color used for meaning rather than decoration.

Source of truth: Earl the Kaiju brand package **v1.5** (identity approved Oct 18, 2026; tokens in
`06-design-tokens/tokens.json`). The app maps those values to semantic roles in
`designsystem/theme/KaijuColors.kt`. Rendered reference sheets: `docs/screenshots/sprint-01/10-design-foundations.png`
and `11-design-components.png` (drawn by the real components, not mockups).

## Principles

| Principle | In practice |
|---|---|
| Hierarchy | One dominant action per screen (Start Class, Next Segment, Start/Pause). Everything else is secondary or ghost. |
| Readable mid-class | Body 16sp minimum; Class Mode titles 40–64sp; timer numerals sized to the available space. |
| Few taps | Start a class from Today in one tap; mark attendance in one tap per student; undo is one tap. |
| Meaning never by color alone | Every status has a word and an icon or shape: LIVE + diamond, DONE + check, CANCELED + X + strike-through, Paused = gold label + diagonal hatching. |
| Composition over decoration | Personality comes from type scale contrast, numbered sections, hairlines and the chamfer — not glow, gradients, glass or mascots. |
| Honest states | Concepts are labeled as concepts; sync says it isn't set up; empty states say what to do next. |

## Color roles

Brand proportion (guidelines p.15): about 70% Shadow, gold and emerald sharing most of the rest,
Ivory for text, **Spectral Emerald in small doses**.

| Role | Value | Use |
|---|---|---|
| `background` | Shadow `#0B0F0E` | Every screen |
| `surface` | Raised Shadow `#17211D` | Cards, sheets, inputs |
| `surfaceInset` | Facet `#1E2523` | Selected chips/segments, steppers, prep phase |
| `line` / `lineStrong` | `#26332D` / `#5F6F67` | Decorative hairlines / meaningful borders |
| `ink` / `inkMuted` | Ivory `#F4F1E9` / `#A3B0A9` | Text |
| `action` / `actionPressed` | Emerald `#0B7A62` / Deep Emerald `#0F5B4B` | Primary fills (never text on dark) |
| `accent` | `#27A58B` (UI-only) | Links, active labels, selected icons, Material "primary" |
| `live` | Spectral Emerald `#17D190` | LIVE, focus ring, current segment index |
| `gold` | `#D4AF37` | Eyebrows, section indexes, rank, rest phase, warnings |
| `signal` | `#F0775A` (from site tokens) | Errors, Absent, destructive actions |
| `phaseWork` / `phaseRest` | Deep Emerald / Shadow | Timer fields |

### Contrast (WCAG 2.2, computed from the hex values)

| Foreground | Background | Ratio | Needs | Result | Used for |
|---|---|---|---|---|---|
| Ivory `#F4F1E9` | Shadow `#0B0F0E` | 17.09:1 | 4.5:1 | Pass | Body and display text |
| Ivory `#F4F1E9` | Raised `#17211D` | 14.63:1 | 4.5:1 | Pass | Text on cards and sheets |
| Ivory `#F4F1E9` | Facet `#1E2523` | 13.84:1 | 4.5:1 | Pass | Selected chips, segments, inset fills |
| Muted `#A3B0A9` | Shadow `#0B0F0E` | 8.58:1 | 4.5:1 | Pass | Secondary text |
| Muted `#A3B0A9` | Raised `#17211D` | 7.34:1 | 4.5:1 | Pass | Secondary text on cards |
| Muted `#A3B0A9` | Facet `#1E2523` | 6.95:1 | 4.5:1 | Pass | Secondary text on inset fills |
| Gold `#D4AF37` | Shadow `#0B0F0E` | 9.17:1 | 4.5:1 | Pass | Eyebrows, section indexes, rank labels |
| Gold `#D4AF37` | Raised `#17211D` | 7.85:1 | 4.5:1 | Pass | Eyebrows on cards |
| Accent `#27A58B` | Shadow `#0B0F0E` | 6.28:1 | 4.5:1 | Pass | Links, active labels, selected icons |
| Accent `#27A58B` | Raised `#17211D` | 5.38:1 | 4.5:1 | Pass | Links and ghost buttons on cards |
| Spectral `#17D190` | Shadow `#0B0F0E` | 9.70:1 | 3:1 | Pass | Live state, focus ring, current index |
| Shadow `#0B0F0E` | Spectral `#17D190` | 9.70:1 | 4.5:1 | Pass | "LIVE" badge text |
| Ivory `#F4F1E9` | Emerald `#0B7A62` | 4.68:1 | 4.5:1 | Pass | Primary button label |
| Ivory `#F4F1E9` | DeepEmerald `#0F5B4B` | 7.11:1 | 4.5:1 | Pass | Pressed button label; timer numerals in work phase |
| Gold `#D4AF37` | DeepEmerald `#0F5B4B` | 3.81:1 | 3:1 | Pass | (not used for small text; large only) |
| Signal `#F0775A` | Shadow `#0B0F0E` | 6.88:1 | 4.5:1 | Pass | Errors, Absent, destructive buttons |
| Signal `#F0775A` | Raised `#17211D` | 5.89:1 | 4.5:1 | Pass | Errors on cards |
| LineStrong `#5F6F67` | Shadow `#0B0F0E` | 3.63:1 | 3:1 | Pass | Input / toggle borders (non-text) |
| LineStrong `#5F6F67` | Raised `#17211D` | 3.11:1 | 3:1 | Pass | Input borders on sheets (non-text) |
| Emerald `#0B7A62` | Shadow `#0B0F0E` | 3.65:1 | 3:1 | Pass | Emerald graphics only (accent edges, keystone) |
| Line `#26332D` | Shadow `#0B0F0E` | 1.46:1 | decorative | — | Decorative hairlines only |

Emerald `#0B7A62` is never used as text on dark (3.65:1). Gold is never used as small text on
Deep Emerald; the timer's warning text stays Ivory and the gold border carries the cue.

## Typography

Barlow Condensed (display, numerals, labels) and Manrope (text), both SIL OFL 1.1, bundled as
static TTFs in `res/font` (Manrope instanced from the variable font with fontTools). Every size is
in `sp` and follows the system font scale; screens were rendered at 150% and 200%.

| Style | Font | Size / line | Weight | Use |
|---|---|---|---|---|
| displayXL | Barlow Condensed | 96 / 88 | 800, caps | TODAY |
| displayL | Barlow Condensed | 64 / 61 | 800, caps | Screen titles, Class Mode index |
| displayM | Barlow Condensed | 40 / 40 | 700 | Featured lesson, timer phase |
| heading | Barlow Condensed | 28 / 31 | 700 | Sheet and empty-state titles |
| title | Barlow Condensed | 22 / 26 | 700 | Top bars, card titles |
| eyebrow | Barlow Condensed | 13 / 16, +0.22em | 600, caps | Section labels, metadata |
| index | Barlow Condensed | 13 / 16, tabular | 600 | `01` section numbers |
| bodyL / body / bodyS | Manrope | 18/28 · 16/24 · 14/20 | 500 · 400 · 400 | Text |
| label / labelL | Manrope | 14/20 · 16/22 | 600 · 700 | Form labels, buttons |
| caption | Manrope | 12 / 16 | 500 | Supporting lines |
| numeralL / numeralM | Barlow Condensed, `tnum` | 48 · 28 | 700 | Times, counts |
| numeralHero | Barlow Condensed, `tnum` | sized to space | 800 | Timer, segment countdown |

Tabular figures (`tnum`, 0.52em per digit in Barlow Condensed) keep countdowns from jittering.

## Spacing and grid

Brand scale: 4 · 8 · 12 · 16 · 24 · 32 · 48 · 64 · 96 dp. Phone gutter 20dp. Reading columns cap
at 640dp on tablets and landscape. Minimum touch target 48dp; mat-side controls 64dp.

## Shape

Square by default (brand radius 0–2px). One controlled angle — a 45° **chamfer** — does the
emphasis work:

| Shape | Chamfer | Use |
|---|---|---|
| Card | 14dp top-end | Cards |
| CardFeature | 28dp top-end | The featured next class, empty states |
| Action / ActionHero | 10 / 16dp bottom-end | Buttons, attendance toggles |
| Sheet | 20dp top-start | Bottom sheets |
| Dialog | 18dp top-end | Dialogs |
| Small | 2dp radius | Chips, inputs, badges |

## Editorial devices

- **Section header:** gold two-digit index, tracked label, hairline to the edge, optional action.
- **Keystone rule:** gold rule with an emerald keystone, echoing the rule under KAIJU in the lockup.
- **Data rows:** facts separated by vertical hairlines instead of rounded stat cards.
- **Diamonds:** the crest's geometry at bullet size, for live/status dots and technique tags.
- **Vertical labels:** available (`VerticalLabel`) for margin labels on wide layouts.

## Brand marks in the app

| Mark | Where | Rule followed |
|---|---|---|
| Adaptive launcher icon (3 layers from 05-app-icon-favicon) | Launcher, themed icons | Master crest inside the 66dp safe circle |
| Master crest | Settings › About, design sheet | Full crest at 64dp and up |
| Simplified crest | Design sheet | 48–63dp |
| Eyes mark | Navigation rail header | Under 48dp; one Spectral element per view |
| Frame (graphic kit) | Empty states | Kit piece used alone, never as a new badge |

"KAIJU COACH" is set in Barlow Condensed as a product label. It is not a logo and the Earl the
Kaiju wordmarks are never retyped. A designed Kaiju Coach lockup is a decision for Earl.

## Iconography

47 original icons on a 24dp grid: 1.75dp strokes, square caps, mitred joins, straight geometry.
Filled shapes only for transport (play, pause, skip) so they read at a glance. Icons are always
paired with a label or a content description.

## Components

| Group | Components (`designsystem/component`) |
|---|---|
| Structure | `KaijuTopBar`, `KaijuBottomBar`, `KaijuNavRail`, `ScreenHeader`, `SectionHeader`, `Eyebrow`, `HairlineDivider`, `DiamondDivider`, `KeystoneRule`, `VerticalLabel` |
| Actions | `KaijuButton` (Primary, Secondary, Ghost, Danger × Regular, Compact, Hero), `KaijuIconButton` |
| Inputs | `KaijuTextField`, `NumberStepper`, `PickerField`, `KaijuDatePickerDialog`, `KaijuTimePickerDialog`, `OptionSheet`, `KaijuSwitchRow`, `KaijuChip`, `ChipGroup`, `SegmentedControl` |
| Domain | `ClassCard`, `LessonSegmentRow`, `TechniqueCard`, `TechniqueTag`, `StudentRow`, `AttendanceRow`, `BeltBar`, `SessionStatusBadge`, `SegmentProgressBar`, `LiveDot` |
| Timer | `TimerControls`, phase field, `MiniTimer` |
| Feedback | `EmptyState`, `LoadingState`, `ErrorState`, `ConceptBanner`, `SyncStatusPanel`, `ConfirmDialog`, `KaijuBottomSheet`, `KaijuSnackbar` |

## States

| State | Treatment |
|---|---|
| Pressed | Primary → Deep Emerald; others → Facet fill; ripple in content color |
| Focused (keyboard, switch access) | 2dp Spectral Emerald ring (brand rule) |
| Disabled | 38% content alpha on Facet; never the only signal |
| Selected | Facet fill + accent bar/border + Ivory text (+ check icon on chips) |
| Error | Signal border + warning icon + message; announced to TalkBack |
| Loading | Thin accent progress line + eyebrow label, polite live region |
| Empty | Gold frame, title, one line, next action |
| Concept | Gold-bordered banner "Concept · planned" |

## Motion

Restrained: 120–180ms fades between screens, 350ms color change between timer phases, a slow
(900ms) pulse on the LIVE diamond. All motion is skipped when the system "Remove animations"
setting is on (`LocalReduceMotion`). No bounce, no parallax, no glow.

## Accessibility checklist

- Text scales with the system; screens verified at 150% and 200%.
- All interactive elements ≥ 48dp; Class Mode and timer controls 64dp.
- Every icon action has a content description; decorative graphics are cleared from semantics.
- Headings are marked for screen-reader navigation; segmented controls are tabs; toggles are
  switches; attendance toggles are radio buttons.
- Live regions: timer phase changes, attendance totals, segment countdown (polite).
- Status never by color alone (see Principles).
