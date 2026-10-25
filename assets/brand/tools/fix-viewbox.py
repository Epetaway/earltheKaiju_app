# svg2vectordrawable drops the SVG viewBox origin; this wraps the paths in a translate group and sets the dp size.
import re, sys
svg, vd, target_h = sys.argv[1], sys.argv[2], float(sys.argv[3])
vb = [float(x) for x in re.search(r'viewBox="([^"]+)"', open(svg).read()).group(1).split()]
minx, miny, w, h = vb
x = open(vd).read()
dpw = round(w * target_h / h, 2); dph = target_h
x = re.sub(r'android:width="[^"]+"', f'android:width="{dpw}dp"', x, count=1)
x = re.sub(r'android:height="[^"]+"', f'android:height="{dph}dp"', x, count=1)
if minx != 0 or miny != 0:
    i = x.index('<path'); j = x.rindex('</vector>')
    body = x[i:j].rstrip()
    body = '\n'.join('    ' + l for l in body.splitlines())
    x = x[:i] + f'<group\n        android:translateX="{-minx:g}"\n        android:translateY="{-miny:g}">\n    ' + body.lstrip() + '\n    </group>\n' + x[j:]
open(vd, 'w').write(x)
print(vd, vb, dpw, dph)
