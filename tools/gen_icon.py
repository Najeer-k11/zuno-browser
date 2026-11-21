import os
"""Zuno icon generator: one artwork -> SVG previews + Android adaptive-icon vector drawables."""
import sys, math

def lin(x1,y1,x2,y2,*stops): return ('lin',(x1,y1,x2,y2),stops)
def rad(cx,cy,r,*stops): return ('rad',(cx,cy,r),stops)
def circle(cx,cy,r): return f"M{cx-r},{cy} a{r},{r} 0 1,0 {2*r},0 a{r},{r} 0 1,0 {-2*r},0 Z"
def ellipse(cx,cy,rx,ry): return f"M{cx-rx},{cy} a{rx},{ry} 0 1,0 {2*rx},0 a{rx},{ry} 0 1,0 {-2*rx},0 Z"

# --- artwork (108x108 adaptive canvas; keep key content inside the 66dp safe circle 21..87) ---
# Material Expressive mark: flat tonal colour, MaterialShapes, depth from layered flat shapes.
def star(cx,cy,R,n,inner,f,rot=-90):
    """Rounded star polygon, the construction behind MaterialShapes (RoundedPolygon.star):
    2n vertices alternating R / R*inner, each corner rounded by cutting back fraction f of
    both adjacent edges and joining with a quadratic curve through the vertex."""
    v=[]
    for i in range(2*n):
        t=math.radians(rot)+math.pi*i/n
        r=R if i%2==0 else R*inner
        v.append((cx+r*math.cos(t), cy+r*math.sin(t)))
    m=len(v); seg=[]
    for i in range(m):
        p,c,q=v[i-1],v[i],v[(i+1)%m]
        a=(c[0]+(p[0]-c[0])*f, c[1]+(p[1]-c[1])*f)
        b=(c[0]+(q[0]-c[0])*f, c[1]+(q[1]-c[1])*f)
        seg.append((a,c,b))
    d=f"M{seg[0][0][0]:.2f},{seg[0][0][1]:.2f}"
    for a,c,b in seg:
        d+=f" L{a[0]:.2f},{a[1]:.2f} Q{c[0]:.2f},{c[1]:.2f} {b[0]:.2f},{b[1]:.2f}"
    return d+" Z"

def stroke_outline(pts, w, steps=24):
    """Closed outline of a polyline stroked with width w, round caps and round joins.
    Used to punch the Z out of the sunny for alpha-only themed icons."""
    r = w / 2
    def unit(a, b):
        dx, dy = b[0]-a[0], b[1]-a[1]; L = math.hypot(dx, dy); return (dx/L, dy/L)
    def arc(c, a0, delta):
        return [(c[0]+r*math.cos(a0+delta*i/steps), c[1]+r*math.sin(a0+delta*i/steps)) for i in range(steps+1)]
    def side(P):
        # walk P keeping the stroke on the left-hand offset (normal = direction rotated -90deg)
        out = []
        segs = list(zip(P, P[1:]))
        for i, (a, b) in enumerate(segs):
            d = unit(a, b); n = (d[1], -d[0])
            pa = (a[0]+n[0]*r, a[1]+n[1]*r); pb = (b[0]+n[0]*r, b[1]+n[1]*r)
            if i == 0: out.append(pa)
            if i == len(segs)-1:
                out.append(pb); break
            c = segs[i+1][1]; d2 = unit(b, c); n2 = (d2[1], -d2[0])
            cross = d[0]*d2[1] - d[1]*d2[0]
            if cross < 0:   # turning towards the offset side: offset lines intersect (inner corner)
                p3 = (b[0]+n2[0]*r, b[1]+n2[1]*r)
                den = d[0]*d2[1] - d[1]*d2[0]
                t = ((p3[0]-pa[0])*d2[1] - (p3[1]-pa[1])*d2[0]) / den
                out.append((pa[0]+t*d[0], pa[1]+t*d[1]))
            else:           # outer corner: round join, shortest arc between the two normals
                a0 = math.atan2(n[1], n[0]); a1 = math.atan2(n2[1], n2[0])
                delta = (a1 - a0 + math.pi) % (2*math.pi) - math.pi
                out += arc(b, a0, delta)
        return out
    def cap(p_end, d):
        # half circle from the left offset to the right offset, bulging forward along d
        n = (d[1], -d[0]); a0 = math.atan2(n[1], n[0])
        mid = a0 + math.pi/2; fwd = math.atan2(d[1], d[0])
        delta = math.pi if abs(((mid - fwd + math.pi) % (2*math.pi)) - math.pi) < 1e-6 else -math.pi
        return arc(p_end, a0, delta)
    left = side(pts)
    right = side(pts[::-1])
    poly = left + cap(pts[-1], unit(pts[-2], pts[-1])) + right + cap(pts[0], unit(pts[1], pts[0]))
    return "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in poly) + " Z"

# Zuno mark: a pale Sunny (MaterialShapes) on a saturated indigo tile, with an indigo Z.
TILE    = "#FF3A44B8"   # indigo tile
SUNNY_C = "#FFFFD8E4"   # tertiary container
Z_C     = "#FF3A44B8"   # Z (matches the tile)

SUN   = dict(cx=54, cy=54, R=29, n=8, inner=.80, f=.36, rot=-90)
Z_PTS = [(45,46), (63,46), (45,64), (63,64)]
ZP    = "M45,46 L63,46 L45,64 L63,64"
ZW    = 7.5

BG = [dict(d="M0,0h108v108h-108z", fill=TILE)]
FG_SCALE = 1.0
FG = [
    dict(d=star(**SUN), fill=SUNNY_C),
    dict(d=ZP, stroke=Z_C, sw=ZW, cap="round", join="round"),
]
# Themed icons (Android 13+) tint this layer with wallpaper colours and use alpha only,
# so the Z is a true hole punched through the sunny (even-odd fill).
MONO = [
    dict(d=star(**SUN) + " " + stroke_outline(Z_PTS, ZW), fill="#FFFFFFFF", evenodd=True),
]

def argb_to_svg(c):  # "#AARRGGBB" -> (rgb, opacity)
    return "#"+c[3:], int(c[1:3],16)/255

# ---------------- SVG ----------------
def svg(layers, mask, size=512):
    defs=[]; body=[]; n=[0]
    def paint(p):
        if isinstance(p,str):
            rgb,a=argb_to_svg(p); return rgb, a
        n[0]+=1; gid=f"g{n[0]}"; kind,geo,stops=p
        st="".join(f'<stop offset="{o}" stop-color="{argb_to_svg(c)[0]}" stop-opacity="{argb_to_svg(c)[1]:.3f}"/>' for o,c in stops)
        if kind=='lin': defs.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{geo[0]}" y1="{geo[1]}" x2="{geo[2]}" y2="{geo[3]}">{st}</linearGradient>')
        else: defs.append(f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{geo[0]}" cy="{geo[1]}" r="{geo[2]}">{st}</radialGradient>')
        return f"url(#{gid})", 1
    for L in layers:
        scale = 1.0
        if isinstance(L, tuple): L, scale = L
        if scale != 1.0: body.append(f'<g transform="translate(54 54) scale({scale}) translate(-54 -54)">')
        for e in L:
            attrs=[f'd="{e["d"]}"']
            if 'fill' in e: f,a=paint(e['fill']); attrs+= [f'fill="{f}"', f'fill-opacity="{a:.3f}"'] + (['fill-rule="evenodd"'] if e.get('evenodd') else [])
            else: attrs.append('fill="none"')
            if 'stroke' in e:
                s,a=paint(e['stroke']); attrs+=[f'stroke="{s}"', f'stroke-opacity="{a:.3f}"', f'stroke-width="{e["sw"]}"',
                    f'stroke-linecap="{e.get("cap","round")}"', f'stroke-linejoin="{e.get("join","round")}"']
            p=f'<path {" ".join(attrs)}/>'
            if 'rot' in e:
                px,py=e.get('pivot',(54,54)); p=f'<g transform="rotate({e["rot"]} {px} {py})">{p}</g>'
            if 'ty' in e or 'tx' in e: p=f'<g transform="translate({e.get("tx",0)} {e.get("ty",0)})">{p}</g>'
            body.append(p)
        if scale != 1.0: body.append('</g>')
    clip = {'circle':'<circle cx="54" cy="54" r="54"/>',
            'squircle':'<path d="M54,0 C96,0 108,12 108,54 C108,96 96,108 54,108 C12,108 0,96 0,54 C0,12 12,0 54,0 Z"/>',
            'full':'<rect width="108" height="108"/>'}[mask]
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 108 108">'
            f'<defs>{"".join(defs)}<clipPath id="m">{clip}</clipPath></defs><g clip-path="url(#m)">{"".join(body)}</g></svg>')

# ---------------- VectorDrawable ----------------
def vd(layer, scale=1.0):
    out=['<?xml version="1.0" encoding="utf-8"?>',
         '<!-- Generated by the Zuno icon generator. Edit the generator, not this file. -->',
         '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
         '    xmlns:aapt="http://schemas.android.com/aapt"',
         '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">']
    def grad(attr,p):
        kind,geo,stops=p
        items="".join(f'\n                <item android:offset="{o}" android:color="{c}"/>' for o,c in stops)
        if kind=='lin': g=f'<gradient android:type="linear" android:startX="{geo[0]}" android:startY="{geo[1]}" android:endX="{geo[2]}" android:endY="{geo[3]}">'
        else: g=f'<gradient android:type="radial" android:centerX="{geo[0]}" android:centerY="{geo[1]}" android:gradientRadius="{geo[2]}">'
        return f'\n        <aapt:attr name="android:{attr}">\n            {g}{items}\n            </gradient>\n        </aapt:attr>'
    for e in layer:
        attrs=[f'android:pathData="{e["d"]}"']; kids=""
        if e.get('evenodd'): attrs.append('android:fillType="evenOdd"')
        if 'fill' in e:
            if isinstance(e['fill'],str): attrs.append(f'android:fillColor="{e["fill"]}"')
            else: kids+=grad('fillColor',e['fill'])
        if 'stroke' in e:
            attrs+=[f'android:strokeWidth="{e["sw"]}"', f'android:strokeLineCap="{e.get("cap","round")}"', f'android:strokeLineJoin="{e.get("join","round")}"']
            if isinstance(e['stroke'],str): attrs.append(f'android:strokeColor="{e["stroke"]}"')
            else: kids+=grad('strokeColor',e['stroke'])
        p=f'    <path {" ".join(attrs)}>{kids}\n    </path>' if kids else f'    <path {" ".join(attrs)}/>'
        if 'rot' in e:
            px,py=e.get('pivot',(54,54))
            p=f'    <group android:rotation="{e["rot"]}" android:pivotX="{px}" android:pivotY="{py}">\n{p}\n    </group>'
        if 'ty' in e or 'tx' in e:
            p=f'    <group android:translateX="{e.get("tx",0)}" android:translateY="{e.get("ty",0)}">\n{p}\n    </group>'
        out.append(p)
    if scale != 1.0:
        out.insert(5, f'    <group android:scaleX="{scale}" android:scaleY="{scale}" android:pivotX="54" android:pivotY="54">')
        out.append('    </group>')
    out.append('</vector>\n'); return "\n".join(out)

if __name__=="__main__":
    mode=sys.argv[1]
    if mode=="preview":
        for m in ("squircle","circle"):
            os.makedirs("/tmp/icon", exist_ok=True); open(f"/tmp/icon/preview_{m}.svg","w").write(svg([BG,(FG,FG_SCALE)],m))
        open("/tmp/icon/preview_mono.svg","w").write(svg([[dict(d="M0,0h108v108h-108z",fill="#FF2B2D3A")],
            [dict(e, **({'fill':'#FFD8DCF5'} if e.get('fill')=='#FFFFFFFF' else {}), **({'stroke':'#FFD8DCF5'} if e.get('stroke')=='#FFFFFFFF' else {}), **({'stroke':'#FF2B2D3A'} if e.get('stroke')=='#FF000000' else {})) for e in MONO]],"circle"))
    else:
        res=sys.argv[2]
        open(f"{res}/drawable/ic_launcher_background.xml","w").write(vd(BG))
        open(f"{res}/drawable/ic_launcher_foreground.xml","w").write(vd(FG, FG_SCALE))
        open(f"{res}/drawable/ic_launcher_monochrome.xml","w").write(vd(MONO, FG_SCALE))
