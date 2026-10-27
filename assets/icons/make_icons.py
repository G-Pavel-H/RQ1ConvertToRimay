"""Generate the Rimay app icon family as SVG, render to 1024px PNG via Chromium."""
from pathlib import Path
from playwright.sync_api import sync_playwright

OUT = Path(__file__).parent
BG, INK, DIM = "#0A0A0C", "#F2F2EE", "#5A5A62"

# Squircle body per macOS grid: 824px square at 100px inset.
X0, Y0, W = 100, 100, 824
R = 186


def frame(accent, idx, code, word, glyph):
    grid = "".join(
        f'<line x1="{x}" y1="{Y0}" x2="{x}" y2="{Y0+W}"/>' for x in range(X0 + 64, X0 + W, 64)
    ) + "".join(
        f'<line x1="{X0}" y1="{y}" x2="{X0+W}" y2="{y}"/>' for y in range(Y0 + 64, Y0 + W, 64)
    )
    scan = "".join(f'<rect x="{X0}" y="{y}" width="{W}" height="2"/>' for y in range(Y0, Y0 + W, 8))
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
<defs>
  <clipPath id="body"><rect x="{X0}" y="{Y0}" width="{W}" height="{W}" rx="{R}"/></clipPath>
  <radialGradient id="glow" cx="50%" cy="54%" r="55%">
    <stop offset="0" stop-color="{accent}" stop-opacity="0.16"/>
    <stop offset="1" stop-color="{accent}" stop-opacity="0"/>
  </radialGradient>
  <filter id="shadow" x="-10%" y="-10%" width="120%" height="125%">
    <feDropShadow dx="0" dy="14" stdDeviation="14" flood-color="#000" flood-opacity="0.45"/>
  </filter>
</defs>
<g filter="url(#shadow)"><rect x="{X0}" y="{Y0}" width="{W}" height="{W}" rx="{R}" fill="{BG}"/></g>
<g clip-path="url(#body)">
  <rect x="{X0}" y="{Y0}" width="{W}" height="{W}" fill="url(#glow)"/>
  <g stroke="#FFFFFF" stroke-opacity="0.045" stroke-width="2">{grid}</g>
  <g fill="#000" fill-opacity="0.22">{scan}</g>
  <!-- top rule + labels -->
  <rect x="{X0}" y="{Y0+150}" width="{W}" height="4" fill="{INK}" fill-opacity="0.9"/>
  <text x="{X0+118}" y="{Y0+118}" font-family="DejaVu Sans Mono" font-weight="700" font-size="46"
        letter-spacing="10" fill="{INK}">RIMAY</text>
  <text x="{X0+W-118}" y="{Y0+118}" text-anchor="end" font-family="DejaVu Sans Mono" font-weight="700"
        font-size="46" fill="{accent}">{idx}</text>
  <!-- glyph -->
  {glyph}
  <!-- bottom band -->
  <rect x="{X0}" y="{Y0+W-156}" width="{W}" height="156" fill="{accent}"/>
  <text x="{X0+118}" y="{Y0+W-58}" font-family="DejaVu Sans Mono" font-weight="700" font-size="64"
        letter-spacing="4" fill="{BG}">{word}</text>
  <rect x="{X0+W-150}" y="{Y0+W-112}" width="32" height="56" fill="{BG}"/>
  <!-- glitch slice -->
  <rect x="{X0}" y="{Y0+W-170}" width="{W*0.38:.0f}" height="6" fill="{accent}"/>
  <rect x="{X0+W*0.52:.0f}" y="{Y0+W-164}" width="{W*0.12:.0f}" height="4" fill="{accent}" fill-opacity="0.6"/>
</g>
<rect x="{X0+2}" y="{Y0+2}" width="{W-4}" height="{W-4}" rx="{R-2}" fill="none" stroke="#FFFFFF" stroke-opacity="0.10" stroke-width="4"/>
</svg>'''


def chroma(shape, dx=7):
    """Cyan/magenta split behind a white shape — the cyberpunk sprinkle."""
    return (f'<g transform="translate({-dx},0)" fill="#00E5FF" opacity="0.55">{shape}</g>'
            f'<g transform="translate({dx},0)" fill="#FF2D6F" opacity="0.55">{shape}</g>'
            f'<g fill="{INK}">{shape}</g>')


# Content area: y ~ 300..700, x ~ 218..806
CX = 512

# 01 CURATION — records, one selected and accepted
def curation(a):
    rows = ""
    ys = [318, 428, 538]
    for i, y in enumerate(ys):
        if i == 1:
            continue
        rows += f'<rect x="300" y="{y}" width="72" height="72"/><rect x="400" y="{y+16}" width="{330 if i==0 else 260}" height="40"/>'
    sel_y = ys[1]
    sel = (f'<rect x="300" y="{sel_y}" width="72" height="72" fill="{a}"/>'
           f'<path d="M314 {sel_y+38} l18 18 l30 -34" stroke="{BG}" stroke-width="13" fill="none" stroke-linecap="square"/>'
           f'<rect x="400" y="{sel_y+16}" width="380" height="40" fill="{a}"/>'
           f'<path d="M232 {sel_y+12} l30 24 l-30 24 z" fill="{a}"/>')
    return chroma(rows) + sel


# 02 RESULTS — rising bars, last one accent
def results(a):
    base = 690
    hs = [120, 200, 280]
    bars = "".join(f'<rect x="{262 + i*128}" y="{base-h}" width="88" height="{h}"/>' for i, h in enumerate(hs))
    bars += f'<rect x="232" y="{base+18}" width="560" height="10"/>'
    last = f'<rect x="{262+3*128}" y="{base-380}" width="88" height="380" fill="{a}"/>'
    tick = f'<rect x="{262+3*128-14}" y="{base-380-28}" width="116" height="8" fill="{a}"/>'
    return chroma(bars) + last + tick


# 03 ANOMALIES — 4x4 block grid, one cell knocked out of place
def anomalies(a):
    s, g = 76, 22
    ox, oy = 262, 330
    miss = (2, 1)
    cells = ""
    for r in range(4):
        for c in range(4):
            if (c, r) == miss:
                continue
            cells += f'<rect x="{ox+c*(s+g)}" y="{oy+r*(s+g)}" width="{s}" height="{s}"/>'
    mx, my = ox + miss[0]*(s+g), oy + miss[1]*(s+g)
    hole = (f'<rect x="{mx+4}" y="{my+4}" width="{s-8}" height="{s-8}" fill="none" stroke="{a}" '
            f'stroke-width="6" stroke-dasharray="14 10"/>')
    out = f'<rect x="{ox+4*(s+g)+34}" y="{oy-14}" width="{s}" height="{s}" fill="{a}"/>'
    slice_ = f'<rect x="{ox+4*(s+g)+6}" y="{oy+18}" width="{s+56}" height="10" fill="{a}" fill-opacity="0.7"/>'
    return chroma(cells) + hole + out + slice_


# 04 PRAGYAN — two datasets compared: offset squares with overlap
def pragyan(a):
    s = 270
    x1, y1 = 292, 318
    x2, y2 = x1 + 150, y1 + 110
    left = f'<rect x="{x1}" y="{y1}" width="{s}" height="{s}" fill="none" stroke="{INK}" stroke-width="22"/>'
    right = f'<rect x="{x2}" y="{y2}" width="{s}" height="{s}" fill="{a}"/>'
    # overlap region knocked back to ink with hatch, showing agreement
    ox0, oy0, ox1, oy1 = x2, y2, x1 + s, y1 + s
    hatch = "".join(
        f'<rect x="{ox0}" y="{y}" width="{ox1-ox0}" height="8"/>' for y in range(oy0 + 14, oy1 - 4, 22)
    )
    over = f'<g fill="{BG}">{hatch}</g>'
    split = (f'<g transform="translate(-7,0)"><rect x="{x1}" y="{y1}" width="{s}" height="{s}" fill="none" stroke="#00E5FF" stroke-opacity="0.55" stroke-width="22"/></g>'
             f'<g transform="translate(7,0)"><rect x="{x1}" y="{y1}" width="{s}" height="{s}" fill="none" stroke="#FF2D6F" stroke-opacity="0.55" stroke-width="22"/></g>')
    return split + right + over + left


APPS = [
    ("curation", "Rimay Curation", "01", "CURATE", "#C8FF00", curation),
    ("results", "Rimay Results", "02", "RESULTS", "#FFB000", results),
    ("anomalies", "Rimay Anomalies", "03", "ANOMALY", "#FF2D6F", anomalies),
    ("pragyan", "Rimay Pragyan", "04", "PRAGYAN", "#00E5FF", pragyan),
]

if __name__ == "__main__":
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(viewport={"width": 1024, "height": 1024})
        for key, _, idx, word, acc, fn in APPS:
            svg = frame(acc, idx, key, word, fn(acc))
            (OUT / f"{key}.svg").write_text(svg)
            pg.set_content(f'<html><body style="margin:0;background:transparent">{svg}</body></html>')
            pg.screenshot(path=str(OUT / f"{key}.png"), omit_background=True,
                          clip={"x": 0, "y": 0, "width": 1024, "height": 1024})
        b.close()
    print("ok")
