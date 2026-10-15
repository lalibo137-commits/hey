#!/usr/bin/env python3
"""64x64 player-format skins for every character, plus 'smile' variants and the faceless Unrated."""
import os, sys, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from texlib import *

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'airlinestory', 'textures', 'entity')
os.makedirs(os.path.join(BASE, 'npc'), exist_ok=True)
os.makedirs(os.path.join(BASE, 'hollow'), exist_ok=True)

def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + d + w + d, v + d, w, h)}

HEAD = faces(0, 0, 8, 8, 8)
HAT = faces(32, 0, 8, 8, 8)
BODY = faces(16, 16, 8, 12, 4)
JACKET = faces(16, 32, 8, 12, 4)
RLEG = faces(0, 16, 4, 12, 4)
LLEG = faces(16, 48, 4, 12, 4)

def arms(slim):
    w = 3 if slim else 4
    return faces(40, 16, w, 12, 4), faces(32, 48, w, 12, 4)

class Skin:
    def __init__(self, seed):
        self.img = new(64, 64)
        self.px = self.img.load()
        self.r = random.Random(seed)

    def fill(self, rect, color, amount=0.05):
        x, y, w, h = rect
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.px[xx, yy] = mul(color, 1 + (self.r.random() * 2 - 1) * amount)

    def part(self, f, color, amount=0.05):
        for k in f.values():
            self.fill(k, color, amount)

    def row(self, f, sides, y0, y1, color, amount=0.04):
        """Paint rows y0..y1 (relative to the face's top) on the given side faces."""
        for s in sides:
            x, y, w, h = f[s]
            for yy in range(y + y0, min(y + h, y + y1 + 1)):
                for xx in range(x, x + w):
                    self.px[xx, yy] = mul(color, 1 + (self.r.random() * 2 - 1) * amount)

    def dot(self, f, side, dx, dy, color):
        x, y, w, h = f[side]
        if 0 <= dx < w and 0 <= dy < h:
            self.px[x + dx, y + dy] = color

    def save(self, path):
        self.img.save(path)

SIDES = ['front', 'back', 'left', 'right']

TONES = {'pale': rgb('f0c8a8'), 'light': rgb('e0b08a'), 'tan': rgb('c08a5e'), 'brown': rgb('9a6440'), 'dark': rgb('6a4026'), 'grey': rgb('cfc6bc')}

def paint(spec, smile=False, faceless=False):
    s = Skin(sum(ord(c) * (i + 1) for i, c in enumerate(spec['name'])))
    skin = TONES[spec.get('tone', 'light')]
    hair = rgb(spec.get('hair', '4a3020'))
    slim = spec.get('slim', False)
    RARM, LARM = arms(slim)
    shirt = rgb(spec.get('shirt', '3d63b0'))
    pants = rgb(spec.get('pants', '2b3a55'))
    shoes = rgb(spec.get('shoes', '2a2a2a'))
    sleeve = spec.get('sleeve', 'long')
    # base
    s.part(HEAD, skin, 0.03)
    s.part(BODY, shirt)
    s.part(RLEG, pants); s.part(LLEG, pants)
    s.part(RARM, shirt); s.part(LARM, shirt)
    for leg in (RLEG, LLEG):
        s.row(leg, SIDES, 10, 11, shoes)
        s.fill(leg['bottom'], shoes)
    for arm in (RARM, LARM):
        if sleeve == 'short':
            s.row(arm, SIDES, 4, 11, skin, 0.03)
        elif sleeve == 'none':
            s.row(arm, SIDES, 0, 11, skin, 0.03)
            s.fill(arm['top'], skin, 0.03)
        else:
            s.row(arm, SIDES, 10, 11, skin, 0.03)
        s.fill(arm['bottom'], skin, 0.03)
        if spec.get('cuff'):
            s.row(arm, SIDES, 9, 9, rgb(spec['cuff']))
    # hair
    style = spec.get('style', 'short')
    if style != 'bald':
        s.fill(HEAD['top'], hair)
        s.row(HEAD, ['back'], 0, 7 if style in ('long', 'bun', 'braid') else 4, hair)
        s.row(HEAD, ['left', 'right'], 0, 6 if style == 'long' else 2, hair)
        s.row(HEAD, ['front'], 0, 1 if style != 'fringe' else 2, hair)
        if style in ('long',):
            s.row(BODY, ['back'], 0, 4, hair)
        if style == 'bun':
            s.row(HAT, ['back'], 0, 3, hair)
            s.fill(HAT['top'], (0, 0, 0, 0))
            for dx in range(2, 6):
                for dy in range(1, 3):
                    s.dot(HAT, 'back', dx, dy, mul(hair, 1.1))
        if style == 'braid':
            for dy in range(0, 10):
                s.dot(BODY, 'back', 3, dy, mul(hair, 0.85)); s.dot(BODY, 'back', 4, dy, hair)
        if style == 'curly':
            for dx in range(8):
                s.dot(HEAD, 'front', dx, 2 if dx % 3 == 0 else 1, mul(hair, 1.15))
        if style == 'fringe':
            s.dot(HEAD, 'front', 7, 3, hair); s.dot(HEAD, 'front', 0, 3, hair)
        if style == 'messy':
            for dx in (1, 3, 6):
                s.dot(HEAD, 'front', dx, 2, hair)
    # face
    if not faceless:
        eye = rgb(spec.get('eyes', '3a5a8a'))
        white = rgb('f4f2ec')
        brow = mul(hair, 0.8)
        s.dot(HEAD, 'front', 1, 4, white); s.dot(HEAD, 'front', 2, 4, eye)
        s.dot(HEAD, 'front', 5, 4, eye); s.dot(HEAD, 'front', 6, 4, white)
        if style != 'bald' or spec.get('brows'):
            s.dot(HEAD, 'front', 1, 3, brow); s.dot(HEAD, 'front', 2, 3, brow)
            s.dot(HEAD, 'front', 5, 3, brow); s.dot(HEAD, 'front', 6, 3, brow)
        s.dot(HEAD, 'front', 3, 5, mul(skin, 0.85)); s.dot(HEAD, 'front', 4, 5, mul(skin, 0.85))
        mouth = mul(skin, 0.6)
        if smile:
            for dx in range(1, 7):
                s.dot(HEAD, 'front', dx, 6, rgb('1a0a0a'))
            for dx in range(2, 6):
                s.dot(HEAD, 'front', dx, 6, rgb('f4f2ec'))
            s.dot(HEAD, 'front', 1, 5, rgb('1a0a0a')); s.dot(HEAD, 'front', 6, 5, rgb('1a0a0a'))
            for dx in (1, 2, 5, 6):
                s.dot(HEAD, 'front', dx, 4, rgb('050505'))
            s.dot(HEAD, 'front', 2, 4, rgb('ffffff')); s.dot(HEAD, 'front', 5, 4, rgb('ffffff'))
        else:
            s.dot(HEAD, 'front', 3, 6, mouth); s.dot(HEAD, 'front', 4, 6, mouth)
        if spec.get('beard'):
            b = rgb(spec['beard'])
            for dx in range(8):
                s.dot(HEAD, 'front', dx, 7, b)
            for dx in (0, 1, 6, 7):
                s.dot(HEAD, 'front', dx, 6, b)
                s.dot(HEAD, 'front', dx, 5, b)
            s.row(HEAD, ['left', 'right'], 5, 7, b)
            if spec.get('longbeard'):
                s.row(BODY, ['front'], 0, 4, b)
                for dx in (1, 2, 5, 6):
                    s.dot(HEAD, 'front', dx, 6, b)
        if spec.get('stubble'):
            for dx in range(1, 7):
                s.dot(HEAD, 'front', dx, 7, mul(skin, 0.8))
        if spec.get('mustache'):
            for dx in range(2, 6):
                s.dot(HEAD, 'front', dx, 5, rgb(spec['mustache']))
        if spec.get('glasses'):
            g = rgb('1b1d1f')
            for dx in (1, 2, 5, 6):
                s.dot(HAT, 'front', dx, 3, g)
            s.dot(HAT, 'front', 0, 4, g); s.dot(HAT, 'front', 3, 4, g); s.dot(HAT, 'front', 4, 4, g); s.dot(HAT, 'front', 7, 4, g)
        if spec.get('paint'):
            pc = rgb(spec['paint'])
            for dx in (0, 1, 6, 7):
                s.dot(HEAD, 'front', dx, 5, pc)
            s.dot(HEAD, 'front', 3, 2, pc); s.dot(HEAD, 'front', 4, 2, pc)
    else:
        # the Unrated: a smooth blank face, a faint seam where a mouth was
        s.fill(HEAD['front'], mul(skin, 0.98), 0.02)
        if style != 'bald':
            s.row(HEAD, ['front'], 0, 1, hair)
        s.dot(HEAD, 'front', 3, 6, mul(skin, 0.9)); s.dot(HEAD, 'front', 4, 6, mul(skin, 0.9))
    # outfit details
    for d in spec.get('details', []):
        kind = d[0]
        if kind == 'blazer':
            c = rgb(d[1]); inner = rgb(d[2])
            s.row(BODY, SIDES, 0, 11, c)
            for dy in range(0, 6):
                for dx in (3, 4):
                    s.dot(BODY, 'front', dx, dy, inner)
            for dy in range(0, 4):
                s.dot(BODY, 'front', 2, dy + 1, inner); s.dot(BODY, 'front', 5, dy + 1, inner)
            for arm in (RARM, LARM):
                s.row(arm, SIDES, 0, 9, c)
                s.fill(arm['top'], c)
        elif kind == 'tie':
            c = rgb(d[1])
            for dy in range(1, 8):
                s.dot(BODY, 'front', 3 + (dy % 2 == 0 and dy > 5), dy, c)
                s.dot(BODY, 'front', 4, dy, c)
        elif kind == 'scarf':
            c = rgb(d[1])
            s.row(BODY, SIDES, 0, 1, c)
            s.dot(BODY, 'front', 2, 2, c); s.dot(BODY, 'front', 2, 3, c); s.dot(BODY, 'front', 3, 2, c)
        elif kind == 'badge':
            s.dot(BODY, 'front', 1, 3, rgb(d[1])); s.dot(BODY, 'front', 2, 3, rgb(d[1])); s.dot(BODY, 'front', 1, 4, rgb(d[1]))
        elif kind == 'wings':
            c = rgb('d9b13b')
            for dx in range(0, 4):
                s.dot(BODY, 'front', dx + 1, 2, c)
            s.dot(BODY, 'front', 2, 3, c)
        elif kind == 'apron':
            c = rgb(d[1])
            s.row(BODY, ['front'], 3, 11, c)
            s.row(RLEG, ['front'], 0, 3, c); s.row(LLEG, ['front'], 0, 3, c)
            s.dot(BODY, 'front', 0, 2, c); s.dot(BODY, 'front', 7, 2, c)
        elif kind == 'stripe':
            c = rgb(d[1])
            s.row(BODY, SIDES, d[2], d[2], c)
        elif kind == 'hood':
            c = rgb(d[1])
            s.row(HAT, ['back', 'left', 'right'], 4, 7, c)
            s.row(BODY, ['front'], 0, 0, mul(c, 0.85))
            s.dot(BODY, 'front', 3, 1, rgb('dddddd')); s.dot(BODY, 'front', 4, 1, rgb('dddddd'))
            s.row(BODY, ['front'], 7, 9, mul(c, 0.9))
        elif kind == 'cap':
            c = rgb(d[1])
            s.fill(HAT['top'], c)
            s.row(HAT, SIDES, 0, 1, c)
            s.row(HAT, ['front'], 2, 2, rgb('d9b13b'))
        elif kind == 'beret':
            c = rgb(d[1])
            s.fill(HAT['top'], c)
            s.row(HAT, ['front', 'left', 'right', 'back'], 0, 0, c)
            s.dot(HAT, 'left', 0, 1, c); s.dot(HAT, 'left', 1, 1, c)
        elif kind == 'cardigan':
            c = rgb(d[1])
            s.row(BODY, SIDES, 0, 11, c)
            for dy in range(0, 12):
                s.dot(BODY, 'front', 3, dy, shirt); s.dot(BODY, 'front', 4, dy, shirt)
            for dy in (3, 6, 9):
                s.dot(BODY, 'front', 2, dy, rgb('e8e0c8'))
            for arm in (RARM, LARM):
                s.row(arm, SIDES, 0, 9, c)
        elif kind == 'leafskirt':
            g1, g2 = rgb('3a7d20'), rgb('68b83d')
            for leg in (RLEG, LLEG):
                for side in SIDES:
                    x, y, w, h = leg[side]
                    for yy in range(0, 6):
                        for xx in range(w):
                            s.px[x + xx, y + yy] = g1 if (xx + yy) % 2 else g2
            s.row(BODY, SIDES, 10, 11, rgb('8a6a45'))
        elif kind == 'necklace':
            c = rgb(d[1])
            for dx in range(1, 7):
                s.dot(BODY, 'front', dx, 1 if dx in (1, 6) else 2, c)
            s.dot(BODY, 'front', 3, 3, rgb('efe8d8')); s.dot(BODY, 'front', 4, 3, rgb('efe8d8'))
        elif kind == 'bodypaint':
            c = rgb(d[1])
            for dy in (2, 5, 8):
                for dx in range(8):
                    if (dx + dy) % 3:
                        s.dot(BODY, 'front', dx, dy, c)
            for arm in (RARM, LARM):
                s.row(arm, ['front', 'right', 'left'], 2, 2, c)
                s.row(arm, ['front', 'right', 'left'], 6, 6, c)
        elif kind == 'patchwork':
            cols = [rgb(x) for x in d[1]]
            for f in (BODY,):
                for side in SIDES:
                    x, y, w, h = f[side]
                    for yy in range(h):
                        for xx in range(w):
                            s.px[x + xx, y + yy] = mul(cols[((xx // 3) + (yy // 3)) % len(cols)], 1 + (s.r.random() - 0.5) * 0.08)
            for arm in (RARM, LARM):
                for side in SIDES:
                    x, y, w, h = arm[side]
                    for yy in range(0, 9):
                        for xx in range(w):
                            s.px[x + xx, y + yy] = cols[(yy // 3) % len(cols)]
        elif kind == 'headband':
            c = rgb(d[1])
            s.row(HAT, SIDES, 2, 2, c)
        elif kind == 'rags':
            for side in SIDES:
                x, y, w, h = BODY[side]
                for xx in range(w):
                    if s.r.random() < 0.5:
                        s.px[x + xx, y + h - 1] = (0, 0, 0, 0) if False else mul(shirt, 0.7)
        elif kind == 'overalls':
            c = rgb(d[1])
            s.row(BODY, SIDES, 4, 11, c)
            s.dot(BODY, 'front', 1, 0, c); s.dot(BODY, 'front', 1, 1, c); s.dot(BODY, 'front', 1, 2, c); s.dot(BODY, 'front', 1, 3, c)
            s.dot(BODY, 'front', 6, 0, c); s.dot(BODY, 'front', 6, 1, c); s.dot(BODY, 'front', 6, 2, c); s.dot(BODY, 'front', 6, 3, c)
        elif kind == 'faded':
            # Unrated clothes: washed out, sea-stained
            for y in range(16, 64):
                for x in range(0, 64):
                    c = s.px[x, y]
                    if c[3] and not (x < 32 and y < 16):
                        g = (c[0] + c[1] + c[2]) // 3
                        s.px[x, y] = mix(c, (g, g, g, 255), 0.6)
    return s

TRAVELERS = [
    dict(shirt='b02e26', pants='2b3a55', hair='2a1a10', tone='light'),
    dict(shirt='e8e6df', pants='3a3a3a', hair='c9a54a', tone='pale', slim=True, style='long', sleeve='short'),
    dict(shirt='3f8a2e', pants='6b5a3a', hair='1a1a1a', tone='brown', style='curly', sleeve='short'),
    dict(shirt='6fb2ea', pants='e8e6df', hair='8a4a20', tone='light', slim=True, style='bun'),
    dict(shirt='4a4a52', pants='2a2a30', hair='5a5a5a', tone='tan', details=[('tie', '1f3a7a')], beard='4a4a4a'),
    dict(shirt='f0a030', pants='3a5a8a', hair='3a2010', tone='dark', slim=True, style='braid', sleeve='short'),
    dict(shirt='7a3a8a', pants='2b2b2b', hair='d8d0b8', tone='pale', glasses=True),
    dict(shirt='2a8a8a', pants='5a4a3a', hair='1a1a1a', tone='tan', slim=True, style='long'),
    dict(shirt='8a6a45', pants='3a3a46', hair='6a3a18', tone='light', details=[('hood', '8a6a45')]),
    dict(shirt='e04a6a', pants='1b1d1f', hair='101010', tone='brown', slim=True, style='fringe', sleeve='short'),
]

SPECS = {
    'dana': dict(slim=True, tone='pale', hair='e0c070', style='bun', shirt='f4f2ec', pants='1f3a7a', eyes='3a7a5a',
                 details=[('blazer', '1f3a7a', 'f4f2ec'), ('scarf', 'b02e26'), ('badge', 'ffd34e')]),
    'agent': dict(tone='tan', hair='2a1a10', shirt='f4f2ec', pants='1f3a7a', details=[('blazer', '1f3a7a', 'f4f2ec'), ('badge', 'ffd34e')]),
    'reyes': dict(tone='tan', hair='15100c', shirt='22262e', pants='1b1d22', mustache='15100c', details=[('badge', 'd9b13b'), ('stripe', '3e4a5e', 9)]),
    'officer': dict(slim=True, tone='brown', hair='15100c', style='bun', shirt='22262e', pants='1b1d22', details=[('badge', 'd9b13b')]),
    'tomas': dict(tone='light', hair='5a3418', shirt='f4f2ec', pants='3a3a3a', stubble=True, sleeve='short', details=[('apron', '2f6b3a')]),
    'clerk': dict(slim=True, tone='light', hair='b06a2a', style='long', shirt='f9c62d', pants='2a2a30', sleeve='short'),
    'corinne': dict(slim=True, tone='light', hair='6a3a18', style='long', shirt='f4f2ec', pants='1f3a7a',
                    details=[('blazer', '1f3a7a', 'f4f2ec'), ('scarf', 'b02e26'), ('badge', 'ffd34e')]),
    'janitor': dict(tone='dark', hair='3a3a3a', shirt='9aa1a5', pants='5a6a7a', beard='3a3a3a', details=[('overalls', '5a6a7a')]),
    'marcus': dict(tone='brown', hair='15100c', style='curly', shirt='6b7272', pants='3a5a8a', glasses=True, details=[('hood', '6b7272')]),
    'priya': dict(slim=True, tone='brown', hair='101010', style='long', shirt='1f9a9a', pants='2a2a46', eyes='3a2010',
                  details=[('stripe', 'd9b13b', 11), ('necklace', 'd9b13b')]),
    'leo': dict(tone='brown', hair='101010', style='messy', shirt='f08a2a', pants='3a5a8a', sleeve='short', details=[('stripe', 'ffffff', 4)]),
    'halvorsen': dict(tone='pale', hair='c8c0a8', shirt='f4f2ec', pants='5a5f63', details=[('blazer', '5a5f63', 'f4f2ec'), ('tie', 'b02e26')]),
    'ines': dict(slim=True, tone='light', hair='101010', style='fringe', shirt='1b1d1f', pants='2a2a30', details=[('beret', 'b02e26')]),
    'edith': dict(slim=True, tone='pale', hair='d8d8d8', style='bun', shirt='e8e0c8', pants='4a3a5a', glasses=True, details=[('cardigan', '6a4a8a')]),
    'june': dict(slim=True, tone='light', hair='2a1a10', style='bun', shirt='b02e26', pants='1f3a7a',
                 details=[('blazer', 'b02e26', 'f4f2ec'), ('scarf', '1f3a7a'), ('cap', '1f3a7a'), ('badge', 'ffd34e')]),
    'attendant': dict(tone='tan', hair='2a1a10', shirt='f4f2ec', pants='1f3a7a', details=[('stripe', '1f3a7a', 6), ('tie', '1f3a7a'), ('badge', 'ffd34e')]),
    'ahu': dict(tone='tan', hair='efefef', beard='efefef', longbeard=True, shirt='3a4a6a', pants='6b5a3a', shoes='5a4220',
                details=[('patchwork', ['1f3a7a', '2f4f9a', '6b7272', 'c9a53c']), ('wings', None)]),
    'nalu': dict(tone='brown', hair='15100c', style='long', shirt='9a6440', pants='3a7d20', shoes='9a6440', sleeve='none', paint='f4f2ec',
                 details=[('bodypaint', 'f4f2ec'), ('leafskirt', None), ('necklace', 'efe8d8')]),
    'mira': dict(slim=True, tone='dark', hair='15100c', style='braid', shirt='c9b48a', pants='8a6a45', shoes='6a4026', sleeve='short',
                 details=[('scarf', 'a03a2e'), ('rags', None)]),
    'teo': dict(tone='tan', hair='3a2010', shirt='a08048', pants='6b5a3a', shoes='8a6a45', sleeve='none', stubble=True, details=[('headband', 'b02e26')]),
    'pip': dict(slim=True, tone='tan', hair='5a3418', style='braid', shirt='d9c08a', pants='8a6a45', shoes='c08a5e', sleeve='short',
                details=[('necklace', '4a9ac4')]),
    'tribe_0': dict(tone='brown', hair='15100c', shirt='9a6440', pants='3a7d20', shoes='9a6440', sleeve='none', details=[('leafskirt', None), ('necklace', 'efe8d8')]),
    'tribe_1': dict(slim=True, tone='tan', hair='2a1a10', style='long', shirt='c9b48a', pants='8a6a45', shoes='c08a5e', sleeve='short'),
    'tribe_2': dict(tone='dark', hair='101010', style='bald', shirt='6a4026', pants='5a8a3a', shoes='6a4026', sleeve='none', paint='b02e26',
                    details=[('bodypaint', 'b02e26'), ('leafskirt', None)]),
    'tribe_3': dict(slim=True, tone='brown', hair='3a2010', style='braid', shirt='2f4f9a', pants='8a6a45', shoes='9a6440', sleeve='short',
                    details=[('stripe', 'c9a53c', 3)]),
}
for i, t in enumerate(TRAVELERS):
    SPECS[f'traveler_{i}'] = t

for name, spec in SPECS.items():
    spec = dict(spec, name=name)
    paint(spec).save(os.path.join(BASE, 'npc', name + '.png'))
    paint(spec, smile=True).save(os.path.join(BASE, 'npc', name + '_smile.png'))

for i in range(4):
    base = dict(TRAVELERS[(i * 3) % len(TRAVELERS)])
    base['tone'] = 'grey'
    base['details'] = list(base.get('details', [])) + [('faded', None)]
    base['name'] = f'hollow_{i}'
    paint(base, faceless=True).save(os.path.join(BASE, 'hollow', f'hollow_{i}.png'))
print('skins:', len(os.listdir(os.path.join(BASE, 'npc'))), len(os.listdir(os.path.join(BASE, 'hollow'))))
