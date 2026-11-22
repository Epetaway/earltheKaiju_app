# 2. Design

[← Overview](01-overview.md) · Next: [Architecture →](03-architecture.md)

I designed the Earl the Kaiju identity first: the crest, the palette and the type. Kaiju Coach is where that identity had to stop being decoration and become a tool you can read from across a mat. The full spec is [`03-design-system.md`](../03-design-system.md).

## Principles

| Principle | In practice |
|---|---|
| One dominant action per screen | Start Class, Next Segment, Start/Pause. Everything else is secondary. |
| Readable mid-class | Body text is 16 sp minimum; Class Mode titles are 40–64 sp; the timer's numbers fill the space available. |
| Few taps | Start a class from Today in one tap; one tap per student for attendance; undo is one tap. |
| Meaning never by colour alone | Every status has a word plus an icon or shape: LIVE with a diamond, DONE with a check, CANCELED with an X, Paused with diagonal hatching. |
| Composition over decoration | Personality comes from type contrast, numbered sections, hairlines and one angle borrowed from the crest, not from glow, gradients or mascots. |
| Honest states | Concepts are labelled as concepts. The sync screen says it isn't set up. |

## From brand to roles

The brand's colours are mapped to meaning, not used freely:
- **Shadow `#0B0F0E`** is the background, about 70% of every screen, as the brand guidelines specify.
- **Emerald** is for actions.
- **Spectral Emerald `#17D190`** marks what's live right now.
- **Gold `#D4AF37`** is for labels, rank and rest.
- **Ivory** is for text.

Every pairing was measured against WCAG 2.2. Body text is 17.09:1. The primary button label is 4.68:1, just over the 4.5:1 threshold. Emerald is never used as text on dark, because it measures 3.65:1.

**Type:** Barlow Condensed for display, labels and numbers, with tabular figures on every clock so digits don't jump. Manrope for reading.

**Icons:** I drew a set of 47 icons rather than using Material's rounded ones. Square caps and mitred corners match the crest's line-work ([ADR-009](../decisions.md)).

## Designed for the mat, not the desk

- **Dark theme only (for now).** The brand is mostly Shadow, and one theme proven on the mat beats two half-tested ones ([ADR-010](../decisions.md)). A bright gym may change that after real use.
- **Large text is a real case.** Every screen was rendered at 150% and 200% text. Labels that would break mid-word shrink to fit instead, and paired buttons stack.
- **Landscape and tablet.** A bottom bar under 600 dp, a navigation rail above it, and a separate landscape layout for Class Mode and the timer.
- **Reversible by default.** Attendance has undo, and editors ask before discarding changes.

The design was reviewed against the screens the code actually renders, not mockups. The rendered reference sheets are drawn by the real components ([`10-design-foundations.png`](../screenshots/sprint-01/10-design-foundations.png), [`11-design-components.png`](../screenshots/sprint-01/11-design-components.png)).
