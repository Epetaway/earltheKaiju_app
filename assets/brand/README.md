# Brand assets used by the app

Source: **Earl the Kaiju Brand Package v1.5** (`~/Dev/Design/EarltheKaiju/Earl-the-Kaiju-Brand-Package-v1.5`),
identity approved by Earl Hickson Jr. on October 18, 2026; horizontal logos updated October 19, 2026.
Only the files the app needs are copied here, unmodified, with checksums in `SHA256SUMS.txt`.

| File (in `source-svg/`) | Becomes (in `android/app/src/main/res/`) | Used for |
|---|---|---|
| `earl-the-kaiju-app-icon-{background,foreground,monochrome}.svg` | `drawable/ic_launcher_*.xml` + `mipmap-anydpi/ic_launcher.xml` | Adaptive launcher icon, themed icon, splash |
| `earl-the-kaiju-crest-full-color.svg` | `drawable/brand_crest.xml` | Crest at 64dp and up (Settings › About, design sheet) |
| `earl-the-kaiju-crest-simplified-full-color.svg` | `drawable/brand_crest_simplified.xml` | 48–63dp |
| `earl-the-kaiju-eyes-mark-full-color.svg` | `drawable/brand_eyes_mark.xml` | Under 48dp (navigation rail) |
| `earl-the-kaiju-frame-triangle-gold.svg` | `drawable/brand_frame_triangle.xml` | Empty states |
| Other graphic-kit pieces (corner, divider, bullet, stripes, chevron) | not converted yet | Kept as sources; the in-app dividers and diamonds are drawn natively from the same geometry |

`tools/convert.sh` regenerates the VectorDrawables (svg2vectordrawable + a viewBox fix). The
conversion was verified by rendering the converted paths back to SVG and comparing them with the
originals.

## Rules followed (from the brand guidelines)

- The crest is never redrawn, recolored, rotated or placed on Emerald or Gold.
- Small sizes use the deliberate small-size cuts (simplified crest, eyes mark).
- The Earl the Kaiju wordmarks are artwork and are never retyped. "KAIJU COACH" in the app is a
  product label set in Barlow Condensed, not a logo.
- Graphic-kit pieces are used alone, never combined into a new badge.
- Raster art in `00-source-reference` is reference only and is not used.

## Fonts

Barlow Condensed (Medium, SemiBold, Bold, ExtraBold) and Manrope (static instances at 400–800,
made from the official variable font with fontTools) are bundled in `res/font`. Both are licensed
under the SIL Open Font License 1.1, which allows embedding in apps; license texts are in
`fonts/`. Source: github.com/google/fonts (`ofl/barlowcondensed`, `ofl/manrope`).

## Ownership

The crest, wordmarks and graphic kit are © Earl Hickson Jr. They are not covered by any license
that may later apply to this repository's code. See `NOTICE.md` at the repository root.
