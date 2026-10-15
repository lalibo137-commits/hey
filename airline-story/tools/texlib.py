"""Tiny helpers for drawing 16x16 Minecraft-style textures."""
import random
from PIL import Image

def rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

def mul(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)

def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)

def new(w=16, h=16, c=(0, 0, 0, 0)):
    return Image.new('RGBA', (w, h), c)

def noisy(base, seed, amount=0.08, w=16, h=16, clumps=True):
    """Base colour with per-pixel brightness noise, the way vanilla stone/dirt look."""
    r = random.Random(seed)
    img = new(w, h)
    px = img.load()
    for y in range(h):
        for x in range(w):
            f = 1 + (r.random() * 2 - 1) * amount
            if clumps and r.random() < 0.12:
                f *= 1 - amount * 1.2
            px[x, y] = mul(base, f)
    return img

def ramp_noise(palette, seed, w=16, h=16, weights=None):
    """Pick a shade from a palette per pixel (dark..light)."""
    r = random.Random(seed)
    img = new(w, h)
    px = img.load()
    weights = weights or [1] * len(palette)
    for y in range(h):
        for x in range(w):
            px[x, y] = r.choices(palette, weights)[0]
    return img

def rect(img, x1, y1, x2, y2, c):
    px = img.load()
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            if 0 <= x < img.width and 0 <= y < img.height:
                px[x, y] = c

def frame(img, x1, y1, x2, y2, light, dark):
    """Bevel: light top/left, dark bottom/right."""
    rect(img, x1, y1, x2, y1, light)
    rect(img, x1, y1, x1, y2, light)
    rect(img, x1, y2, x2, y2, dark)
    rect(img, x2, y1, x2, y2, dark)

def put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.load()[x, y] = c

def art(rows, colors, w=16, h=16, base=None):
    """Draw from ASCII rows. '.' or ' ' = transparent (or base)."""
    img = base.copy() if base is not None else new(w, h)
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in '. ':
                continue
            px[x, y] = colors[ch]
    return img

def jitter(img, seed, amount=0.06, only_opaque=True):
    r = random.Random(seed)
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            c = px[x, y]
            if only_opaque and c[3] == 0:
                continue
            px[x, y] = mul(c, 1 + (r.random() * 2 - 1) * amount)
    return out

def outline(img, factor=0.45):
    """Add a darker outline around opaque pixels (item sprites)."""
    out = img.copy()
    src = img.load()
    px = out.load()
    for y in range(img.height):
        for x in range(img.width):
            if src[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < img.width and 0 <= ny < img.height and src[nx, ny][3] != 0:
                    px[x, y] = mul(src[nx, ny], factor)
                    break
    return out

def stack(frames):
    """Vertical strip for animated textures."""
    w, h = frames[0].size
    out = new(w, h * len(frames))
    for i, f in enumerate(frames):
        out.paste(f, (0, i * h))
    return out
