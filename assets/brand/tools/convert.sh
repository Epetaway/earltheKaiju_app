#!/usr/bin/env bash
# Regenerates the app's VectorDrawables from the approved brand SVGs.
# Needs Node 18+ and Python 3. Run from this folder: ./tools/convert.sh
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=../../android/app/src/main/res/drawable
npm ls svg2vectordrawable >/dev/null 2>&1 || npm i --no-save svg2vectordrawable@2
convert_one() { # <svg name without extension> <drawable name> <height dp>
  node tools/svg-to-vd.js "source-svg/$1.svg" "$OUT/$2.xml" >/dev/null
  python3 tools/fix-viewbox.py "source-svg/$1.svg" "$OUT/$2.xml" "$3"
}
convert_one earl-the-kaiju-crest-full-color brand_crest 96
convert_one earl-the-kaiju-crest-simplified-full-color brand_crest_simplified 48
convert_one earl-the-kaiju-eyes-mark-full-color brand_eyes_mark 24
convert_one earl-the-kaiju-app-icon-foreground ic_launcher_foreground 108
convert_one earl-the-kaiju-app-icon-background ic_launcher_background 108
convert_one earl-the-kaiju-app-icon-monochrome ic_launcher_monochrome 108
convert_one earl-the-kaiju-frame-triangle-gold brand_frame_triangle 48
echo "Done. Review the diff in android/app/src/main/res/drawable."
