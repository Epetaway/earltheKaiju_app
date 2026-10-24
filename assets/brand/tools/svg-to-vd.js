// Converts one SVG to an Android VectorDrawable (svg2vectordrawable). Usage: node svg-to-vd.js in.svg out.xml
const s2v = require('svg2vectordrawable');
const fs = require('fs');
const [,, inp, out] = process.argv;
s2v(fs.readFileSync(inp, 'utf8'), { floatPrecision: 3, xmlTag: true, fillBlack: false }).then(xml => { fs.writeFileSync(out, xml); console.log(out, xml.length); }).catch(e => { console.error('ERR', inp, e.message); process.exit(1); });
