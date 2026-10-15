#!/usr/bin/env python3
"""Block textures, 16x16, in the vanilla style (noisy shading, bevelled panels, limited palettes)."""
import os, json, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from texlib import *

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'airlinestory', 'textures', 'block')
os.makedirs(OUT, exist_ok=True)

def save(name, img, frametime=None, interpolate=False):
    img.save(os.path.join(OUT, name + '.png'))
    if frametime:
        meta = {'animation': {'frametime': frametime}}
        if interpolate:
            meta['animation']['interpolate'] = True
        with open(os.path.join(OUT, name + '.png.mcmeta'), 'w') as f:
            json.dump(meta, f)

# ------------------------------------------------------------------ palettes
WHITE = rgb('e9ecec'); LGRAY = rgb('a0a7a7'); MGRAY = rgb('6b7272'); DGRAY = rgb('3e4444'); BLACK = rgb('1b1d1f')
NAVY = rgb('1f3a7a'); BLUE = rgb('2f63c4'); SKY = rgb('6fb2ea'); RED = rgb('b02e26'); YELLOW = rgb('f9c62d')
CREAM = rgb('d9d0b4'); SAND = rgb('dbcfa3'); WOOD = rgb('a07b4f'); DWOOD = rgb('5c3d22'); GREEN = rgb('3f8a2e')

def metal(base, seed, amount=0.05):
    img = noisy(base, seed, amount)
    frame(img, 0, 0, 15, 15, mul(base, 1.12), mul(base, 0.78))
    return img

# ------------------------------------------------------------------ airport
def terminal_tile():
    img = new()
    r = random.Random(1)
    for ty in range(2):
        for tx in range(2):
            c = rgb('e6e3dc') if (tx + ty) % 2 == 0 else rgb('d4d0c6')
            for y in range(8):
                for x in range(8):
                    put(img, tx * 8 + x, ty * 8 + y, mul(c, 1 + (r.random() - 0.5) * 0.06))
            frame(img, tx * 8, ty * 8, tx * 8 + 7, ty * 8 + 7, mul(c, 1.05), mul(c, 0.86))
    save('terminal_tile', img)

def terminal_carpet():
    img = noisy(rgb('2c4a8a'), 2, 0.07)
    r = random.Random(3)
    for y in range(16):
        for x in range(16):
            if (x + y * 3) % 8 == 0 or (x * 5 + y) % 11 == 0:
                put(img, x, y, mul(rgb('3d63b0'), 1 + (r.random() - 0.5) * 0.1))
            if (x == 3 and y % 4 == 1) or (x == 11 and y % 4 == 3):
                put(img, x, y, rgb('d9b13b'))
    save('terminal_carpet', img)

def terminal_wall():
    img = noisy(rgb('e4e6e2'), 4, 0.03)
    for y in (7, 15):
        rect(img, 0, y, 15, y, rgb('c4c7c2'))
    for x in (0,):
        rect(img, x, 0, x, 15, rgb('cfd2cd'))
    rect(img, 8, 0, 8, 7, rgb('c9ccc7'))
    save('terminal_wall', img)

def ceiling_panel():
    img = noisy(rgb('ecebe6'), 5, 0.03)
    r = random.Random(6)
    for _ in range(14):
        put(img, r.randrange(1, 15), r.randrange(1, 15), rgb('bdbbb5'))
    frame(img, 0, 0, 15, 15, rgb('f5f5f1'), rgb('b8b7b0'))
    save('ceiling_panel', img)

def ceiling_light():
    img = noisy(rgb('ecebe6'), 7, 0.02)
    frame(img, 0, 0, 15, 15, rgb('f5f5f1'), rgb('b8b7b0'))
    rect(img, 2, 2, 13, 13, rgb('fffbe7'))
    rect(img, 3, 3, 12, 12, rgb('ffffff'))
    frame(img, 2, 2, 13, 13, rgb('d8d3c0'), rgb('d8d3c0'))
    save('ceiling_light', img)

def meridian_logo():
    img = noisy(NAVY, 8, 0.05)
    sun = rgb('ffd34e'); sun2 = rgb('f4a62a'); w = rgb('ffffff')
    rows = [
        "................",
        "................",
        "......y..y......",
        "...y..yyyy..y...",
        "....yYYYYYYy....",
        "...yYYYYYYYYy...",
        "..yYYYwYYwYYYy..",
        "...YYYYYYYYYY...",
        "..yYYwYYYYwYYy..",
        "...YYYwwwwYYY...",
        "....yYYYYYYy....",
        "...y..yyyy..y...",
        "......y..y......",
        "................",
        "wwwwwwwwwwwwwwww",
        "................"]
    img = art(rows, {'y': sun2, 'Y': sun, 'w': w}, base=img)
    save('meridian_logo', img)

def checkin():
    side = metal(rgb('c9cdd0'), 9)
    save('checkin_counter_side', side)
    front = noisy(rgb('d8dbdd'), 10, 0.03)
    rect(front, 0, 0, 15, 2, rgb('8f969a'))
    rect(front, 0, 6, 15, 7, BLUE)
    rect(front, 0, 8, 15, 8, NAVY)
    frame(front, 0, 3, 15, 15, rgb('e8eaec'), rgb('9aa1a5'))
    save('checkin_counter_front', front)
    top = noisy(rgb('5a5f63'), 11, 0.05)
    rect(top, 0, 0, 15, 1, rgb('8f969a'))
    rect(top, 3, 5, 9, 10, rgb('1d2022'))
    rect(top, 4, 6, 8, 9, rgb('3bb3d1'))
    put(top, 5, 7, rgb('a8ecfa')); put(top, 6, 7, rgb('a8ecfa'))
    rect(top, 11, 6, 13, 9, rgb('d6d6d6'))
    save('checkin_counter_top', top)
    save('metal_dark', metal(rgb('45494c'), 12))
    save('metal_light', metal(rgb('b9bec1'), 13))

def conveyor():
    frames = []
    for f in range(8):
        img = new()
        for y in range(16):
            for x in range(16):
                c = rgb('2a2c2e') if (y + f * 2) % 4 < 2 else rgb('363a3d')
                put(img, x, y, c)
        rect(img, 0, 0, 0, 15, rgb('8a9094')); rect(img, 15, 0, 15, 15, rgb('5b6064'))
        frames.append(img)
    save('conveyor_belt_top', stack(frames), frametime=2)
    side = metal(rgb('8a9094'), 14)
    rect(side, 0, 0, 15, 2, rgb('2a2c2e'))
    for x in range(1, 16, 4):
        put(side, x, 4, rgb('4b5054')); put(side, x, 5, rgb('5b6064'))
    rect(side, 0, 9, 15, 15, (0, 0, 0, 0))
    save('conveyor_belt_side', side)

def xray():
    side = metal(rgb('9aa1a5'), 15)
    rect(side, 1, 1, 14, 3, rgb('2f63c4'))
    save('xray_side', side)
    save('xray_top', metal(rgb('8a9094'), 16))
    front = metal(rgb('9aa1a5'), 17)
    rect(front, 2, 4, 13, 15, rgb('151617'))
    for x in range(2, 14):
        if x % 2 == 0:
            rect(front, x, 4, x, 11, rgb('3a3d40'))
    rect(front, 1, 1, 6, 2, RED)
    put(front, 12, 2, rgb('5eff6a'))
    save('xray_front', front)

def scanner():
    img = metal(rgb('b7bcbf'), 18)
    rect(img, 4, 2, 11, 3, rgb('5eff6a'))
    for y in range(5, 15, 3):
        rect(img, 5, y, 10, y, rgb('8a9094'))
    save('scanner_pillar', img)
    top = metal(rgb('b7bcbf'), 19)
    rect(top, 1, 6, 14, 9, rgb('2b2d2f'))
    rect(top, 3, 7, 5, 8, rgb('5eff6a'))
    rect(top, 10, 7, 12, 8, rgb('ff4040'))
    save('scanner_top', top)

def stanchion():
    post = noisy(rgb('c9ced2'), 20, 0.08)
    for x in range(16):
        put(post, x, 0, rgb('f2f4f5'))
    save('stanchion_post', post)
    belt = noisy(rgb('2c4a8a'), 21, 0.05)
    for y in range(16):
        for x in range(16):
            if x % 6 == 0:
                put(belt, x, y, rgb('d9b13b'))
    save('stanchion_belt', belt)

def barrier():
    img = new()
    r = random.Random(22)
    for y in range(16):
        for x in range(16):
            a = 70 + int(r.random() * 25)
            put(img, x, y, (220, 236, 245, a))
    for x in range(16):
        put(img, x, 0, (255, 255, 255, 200)); put(img, x, 15, (160, 180, 190, 200))
        put(img, x, 10, (47, 99, 196, 220)); put(img, x, 11, (47, 99, 196, 220))
    for y in range(16):
        put(img, 0, y, (255, 255, 255, 200)); put(img, 15, y, (160, 180, 190, 200))
    for i in range(3):
        put(img, 3 + i, 3 + i, (255, 255, 255, 170))
    save('story_barrier', img)

GLYPHS = {
    'A': ["010", "101", "111", "101", "101"], 'B': ["110", "101", "110", "101", "110"], 'C': ["011", "100", "100", "100", "011"],
    'D': ["110", "101", "101", "101", "110"], 'E': ["111", "100", "110", "100", "111"], 'I': ["111", "010", "010", "010", "111"],
    'L': ["100", "100", "100", "100", "111"], 'M': ["101", "111", "111", "101", "101"], 'N': ["110", "101", "101", "101", "101"],
    'O': ["010", "101", "101", "101", "010"], 'P': ["110", "101", "110", "100", "100"], 'R': ["110", "101", "110", "101", "101"],
    'T': ["111", "010", "010", "010", "010"], 'V': ["101", "101", "101", "101", "010"], 'Y': ["101", "101", "010", "010", "010"],
    '4': ["101", "101", "111", "001", "001"], '1': ["010", "110", "010", "010", "111"], '7': ["111", "001", "010", "010", "010"],
    '0': ["111", "101", "101", "101", "111"], '9': ["111", "101", "111", "001", "111"], '?': ["110", "001", "010", "000", "010"],
    ' ': ["000", "000", "000", "000", "000"], '-': ["000", "000", "111", "000", "000"], 'S': ["011", "100", "010", "001", "110"],
    'H': ["101", "101", "111", "101", "101"], 'K': ["101", "110", "100", "110", "101"], 'G': ["011", "100", "101", "101", "011"],
}

def text(img, s, x, y, c):
    for ch in s:
        g = GLYPHS.get(ch, GLYPHS[' '])
        for gy, row in enumerate(g):
            for gx, v in enumerate(row):
                if v == '1':
                    put(img, x + gx, y + gy, c)
        x += 4

def departure_board():
    frames = []
    amber = rgb('ffb12b'); green = rgb('5eff6a'); dim = rgb('2a2410')
    for f in range(4):
        img = new(16, 16, rgb('101214'))
        frame(img, 0, 0, 15, 15, rgb('3e4444'), rgb('1b1d1f'))
        rows = [('MA4', 'B7'), ('1 7', '  '), ('ON ', 'TI')]
        text(img, 'MA4', 1, 2, amber)
        text(img, '17', 13 - 7, 2, amber) if False else None
        for i in range(3):
            y = 2 + i * 5 if i < 2 else 12
        # three rows of "flights"
        for i, (a, b) in enumerate([('MA', 'B5'), ('MA', 'B6'), ('MA', 'B7')]):
            yy = 1 + i * 5
            text(img, a, 1, yy, amber)
            text(img, b, 9, yy, green if (i == 2 and f % 2 == 0) or i < 2 else dim)
        frames.append(img)
    save('departure_board', stack(frames), frametime=10)
    gframes = []
    r = random.Random(23)
    for f in range(6):
        img = new(16, 16, rgb('101214'))
        frame(img, 0, 0, 15, 15, rgb('3e4444'), rgb('1b1d1f'))
        for i in range(3):
            yy = 1 + i * 5
            text(img, 'MA', 1, yy, rgb('ff4040') if f % 3 else amber)
            text(img, '??', 9, yy, rgb('ff4040'))
        for _ in range(4 + f):
            y = r.randrange(16)
            rect(img, 1, y, 14, y, (255, 255, 255, 255) if r.random() < 0.3 else rgb('5a1010'))
        gframes.append(img)
    save('departure_board_glitch', stack(gframes), frametime=3)

def boarding_scanner():
    front = metal(rgb('c9ced2'), 24)
    rect(front, 2, 2, 13, 4, NAVY)
    rect(front, 3, 3, 12, 3, rgb('ffd34e'))
    rect(front, 0, 13, 15, 15, rgb('6b7272'))
    save('boarding_scanner_front', front)
    top = metal(rgb('5a5f63'), 25)
    rect(top, 3, 3, 12, 9, rgb('151617'))
    rect(top, 4, 4, 11, 8, rgb('1d6a2a'))
    rect(top, 5, 6, 10, 6, rgb('5eff6a'))
    rect(top, 6, 11, 9, 12, RED)
    save('boarding_scanner_top', top)

def kiosk():
    img = metal(rgb('d8dbdd'), 26)
    rect(img, 2, 2, 13, 10, rgb('151617'))
    rect(img, 3, 3, 12, 9, rgb('1e3c78'))
    for i in range(4):
        put(img, 4 + i * 2, 6, rgb('ffd34e'))
        put(img, 4 + i * 2, 5, rgb('ffd34e'))
    put(img, 12, 6, rgb('6b7272'))
    rect(img, 5, 12, 10, 13, rgb('2b2d2f'))
    save('kiosk_front', img)

def vending():
    side = metal(rgb('b02e26'), 27)
    save('vending_side', side)
    img = metal(rgb('b02e26'), 28)
    rect(img, 2, 2, 10, 13, rgb('1b2a34'))
    r = random.Random(29)
    cols = [rgb('ffd34e'), rgb('5eff6a'), rgb('6fb2ea'), rgb('f9f9f9'), rgb('ff8a3d')]
    for y in (3, 6, 9, 12):
        for x in range(3, 10, 2):
            put(img, x, y, r.choice(cols)); put(img, x, y - 1, mul(r.choice(cols), 0.8))
    rect(img, 12, 3, 13, 8, rgb('2b2d2f'))
    for y in range(4, 8, 2):
        put(img, 12, y, rgb('ffd34e'))
    rect(img, 11, 11, 14, 13, rgb('151617'))
    save('vending_front', img)

def suitcase():
    leather = rgb('5a3a24')
    side = noisy(leather, 30, 0.07)
    frame(side, 0, 0, 15, 15, mul(leather, 1.2), mul(leather, 0.7))
    rect(side, 0, 5, 15, 5, rgb('3a2516'))
    save('suitcase_side', side)
    front = side.copy()
    rect(front, 3, 2, 3, 13, rgb('c9a53c')); rect(front, 12, 2, 12, 13, rgb('c9a53c'))
    rect(front, 6, 4, 9, 6, rgb('d8d0b8'))
    save('suitcase_front', front)
    top = noisy(leather, 31, 0.07)
    rect(top, 0, 7, 15, 8, rgb('3a2516'))
    rect(top, 6, 6, 9, 9, rgb('c9a53c'))
    save('suitcase_top', top)
    open_ = noisy(rgb('2a1a10'), 32, 0.08)
    r = random.Random(33)
    for _ in range(18):
        put(open_, r.randrange(1, 15), r.randrange(1, 15), rgb('dbcfa3'))
    rect(open_, 2, 3, 8, 6, rgb('6fb2ea'))
    frame(open_, 0, 0, 15, 15, mul(leather, 1.2), mul(leather, 0.7))
    save('suitcase_open', open_)

# ------------------------------------------------------------------ airplane
def fuselage():
    img = noisy(rgb('eef0f1'), 34, 0.025)
    for x in range(16):
        put(img, x, 15, rgb('c7cbcd'))
    for y in range(16):
        put(img, 15, y, rgb('d3d6d8'))
    for p in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        put(img, *p, rgb('b5b9bb'))
    save('fuselage', img)
    s = noisy(rgb('eef0f1'), 35, 0.025)
    rect(s, 0, 4, 15, 9, NAVY)
    rect(s, 0, 10, 15, 11, rgb('ffd34e'))
    for x in range(16):
        put(s, x, 4, rgb('2a4b96'))
    save('fuselage_stripe', s)

def window():
    img = noisy(rgb('eef0f1'), 36, 0.025)
    for y in range(3, 13):
        for x in range(4, 12):
            dx = (x - 7.5) / 3.6; dy = (y - 7.5) / 4.9
            if dx * dx + dy * dy <= 1.0:
                put(img, x, y, (0, 0, 0, 0))
            elif dx * dx + dy * dy <= 1.5:
                put(img, x, y, rgb('9aa1a5'))
    save('airplane_window', img)

def airplane_seat():
    fab = rgb('25407e')
    img = noisy(fab, 37, 0.07)
    r = random.Random(38)
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0:
                put(img, x, y, mul(fab, 1.15))
    save('airplane_seat', img)
    head = noisy(fab, 39, 0.06)
    rect(head, 2, 2, 13, 13, rgb('e8e6df'))
    rect(head, 3, 3, 12, 12, rgb('f4f2ec'))
    rect(head, 4, 6, 11, 7, rgb('2f63c4'))
    save('seat_headrest', head)

def seat_backs():
    def screen_base():
        img = noisy(rgb('25407e'), 40, 0.07)
        rect(img, 2, 1, 13, 9, rgb('151617'))
        rect(img, 5, 12, 10, 13, rgb('2b2d2f'))
        return img
    logo = screen_base()
    rect(logo, 3, 2, 12, 8, rgb('16295a'))
    for p in [(7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6)]:
        put(logo, *p, rgb('ffd34e'))
    save('seat_back_logo', logo)
    frames = []
    for f in range(4):
        img = screen_base()
        rect(img, 3, 2, 12, 8, rgb('3a3a46'))
        rect(img, 3, 7, 12, 8, rgb('25407e'))
        # a figure from behind: you
        rect(img, 7, 3, 8, 4, rgb('4a3020'))
        rect(img, 6, 5, 9, 7, rgb('5b8f4a'))
        if f == 3:
            rect(img, 7, 3, 8, 4, rgb('d8b58f'))
            put(img, 7, 3, rgb('000000')); put(img, 8, 3, rgb('000000'))
        rect(img, 10, 2, 12, 2, rgb('ff4040'))
        frames.append(img)
    save('seat_back_live', stack(frames), frametime=12)
    sframes = []
    r = random.Random(41)
    for f in range(6):
        img = screen_base()
        for y in range(2, 9):
            for x in range(3, 13):
                v = r.randint(40, 230)
                put(img, x, y, (v, v, v, 255))
        sframes.append(img)
    save('seat_back_static', stack(sframes), frametime=1)

def cabin():
    img = noisy(rgb('3b3f63'), 42, 0.07)
    r = random.Random(43)
    for y in range(16):
        for x in range(16):
            if (x * 3 + y * 7) % 13 == 0:
                put(img, x, y, rgb('5a5f8a'))
    save('cabin_floor', img)
    panel = noisy(rgb('ddd5c1'), 44, 0.025)
    frame(panel, 0, 0, 15, 15, rgb('ebe4d2'), rgb('b8b09c'))
    save('cabin_panel', panel)
    binf = noisy(rgb('ddd5c1'), 45, 0.025)
    frame(binf, 0, 0, 15, 15, rgb('ebe4d2'), rgb('a39b88'))
    rect(binf, 0, 12, 15, 15, rgb('c9c1ad'))
    rect(binf, 6, 12, 9, 13, rgb('6b7272'))
    rect(binf, 7, 12, 8, 12, rgb('9aa1a5'))
    save('overhead_bin_front', binf)
    on = noisy(rgb('ddd5c1'), 46, 0.02)
    rect(on, 1, 5, 14, 10, rgb('fff6d6')); rect(on, 2, 6, 13, 9, rgb('ffffff'))
    save('cabin_light', on)
    off = noisy(rgb('ddd5c1'), 46, 0.02)
    rect(off, 1, 5, 14, 10, rgb('8c8676')); rect(off, 2, 6, 13, 9, rgb('9d9786'))
    save('cabin_light_off', off)

def seatbelt():
    for name, lit in [('seatbelt_sign_on', True), ('seatbelt_sign_off', False)]:
        img = new(16, 16, rgb('1b1d1f'))
        c = rgb('ffffff') if lit else rgb('3e4444')
        a = rgb('ffb12b') if lit else rgb('3e4444')
        rows = ["................", "................", "................", "................",
                "................", "......aa........", "......aa..cccc..", ".....aaaa.c..c..",
                ".....aaaa.cccc..", "......aa..c..c..", ".....a..a.cccc..", "................",
                "................", "................", "................", "................"]
        img = art(rows, {'a': a, 'c': c}, base=img)
        save(name, img)

def galley():
    img = metal(rgb('b9bec1'), 47)
    for y in (1, 8):
        frame(img, 1, y, 14, y + 6, rgb('d8dbdd'), rgb('7d8387'))
        rect(img, 6, y + 3, 9, y + 3, rgb('45494c'))
    save('galley_front', img)

def mirror():
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            t = (x + y) / 30
            put(img, x, y, mix(rgb('a6c4cf'), rgb('e6f2f5'), t))
    for i in range(5):
        put(img, 3 + i, 9 - i, rgb('ffffff'))
    frame(img, 0, 0, 15, 15, rgb('d8dbdd'), rgb('7d8387'))
    save('mirror', img)

def call_button():
    img = metal(rgb('d8dbdd'), 48)
    rect(img, 4, 4, 11, 11, rgb('9aa1a5'))
    rect(img, 5, 5, 10, 10, rgb('f2f4f5'))
    rows = ["......", "..o...", ".ooo..", "..o...", ".o.o..", "......"]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == 'o':
                put(img, 5 + x, 5 + y, rgb('2f63c4'))
    save('call_button', img)

def oxygen_mask():
    rows = [
        ".......t........", ".......t........", ".......t........", ".......t........",
        "......t.........", "......t.........", ".....ttt........", "....yyyyy.......",
        "...yYYYYYy......", "...yYwwwYy......", "...yYwwwYy......", "...yYYYYYy......",
        "....yyyyy.......", ".....tt.........", "................", "................"]
    img = art(rows, {'t': rgb('dcdcdc'), 'y': rgb('d9a514'), 'Y': rgb('f9c62d'), 'w': rgb('fff6d0')})
    save('oxygen_mask', img)

# ------------------------------------------------------------------ island
def palm():
    side = new()
    r = random.Random(49)
    base = rgb('8a6a45')
    for y in range(16):
        for x in range(16):
            band = (y + (x // 4) * 2) % 4
            c = mul(base, [1.0, 0.85, 0.72, 0.92][band])
            put(side, x, y, mul(c, 1 + (r.random() - 0.5) * 0.1))
    save('palm_log', side)
    top = noisy(rgb('c9a879'), 50, 0.06)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 6.8:
                put(top, x, y, mul(base, 0.85))
            elif int(d) % 3 == 0:
                put(top, x, y, rgb('b08f61'))
    save('palm_log_top', top)
    planks = new()
    for y in range(16):
        row = y // 4
        for x in range(16):
            c = rgb('d2b07a') if row % 2 == 0 else rgb('c7a46e')
            put(planks, x, y, mul(c, 1 + (r.random() - 0.5) * 0.08))
        if y % 4 == 3:
            rect(planks, 0, y, 15, y, rgb('9b7a4b'))
    for (x, y) in [(5, 0), (5, 1), (5, 2), (12, 4), (12, 5), (12, 6), (2, 8), (2, 9), (2, 10), (9, 12), (9, 13), (9, 14)]:
        put(planks, x, y, rgb('9b7a4b'))
    save('palm_planks', planks)
    leaves = new()
    for y in range(16):
        for x in range(16):
            if r.random() < 0.22:
                continue
            c = rgb('4c9a2a') if (x + y) % 3 else rgb('3a7d20')
            if r.random() < 0.2:
                c = rgb('68b83d')
            put(leaves, x, y, mul(c, 1 + (r.random() - 0.5) * 0.12))
    for x in range(16):
        put(leaves, x, (x // 2 + 5) % 16, rgb('2c5f18'))
    save('palm_leaves', leaves)

def coconut():
    img = noisy(rgb('6b4a2b'), 51, 0.12)
    r = random.Random(52)
    for _ in range(20):
        put(img, r.randrange(16), r.randrange(16), rgb('4a311b'))
    for _ in range(10):
        put(img, r.randrange(16), r.randrange(16), rgb('8c6a45'))
    save('coconut', img)

def thatch():
    img = new()
    r = random.Random(53)
    for x in range(16):
        h = r.randint(0, 3)
        for y in range(16):
            c = rgb('c9a84a') if (x + h) % 3 else rgb('a98a35')
            if (y + h) % 5 == 0:
                c = rgb('e0c46a')
            put(img, x, y, mul(c, 1 + (r.random() - 0.5) * 0.12))
    for y in (5, 11):
        rect(img, 0, y, 15, y, rgb('7d6526'))
    save('thatch', img)

def totem():
    wood = rgb('7a5532')
    side = noisy(wood, 54, 0.08)
    for y in range(0, 16, 5):
        rect(side, 0, y, 15, y, mul(wood, 0.7))
    save('totem_side', side)
    top = noisy(rgb('9a7448'), 55, 0.06)
    frame(top, 0, 0, 15, 15, mul(wood, 1.1), mul(wood, 0.7))
    save('totem_top', top)
    rows = [
        "wwwwwwwwwwwwwwww", "wRRwwwwwwwwwwRRw", "wwRRwwwwwwwwRRww", "wwwwwwwwwwwwwwww",
        "wwwBBBwwwwBBBwww", "wwBwwwBwwBwwwBww", "wwwBBBwwwwBBBwww", "wwwwwwwddwwwwwww",
        "wwwwwwwddwwwwwww", "wwwwwwddddwwwwww", "wwwwwwwwwwwwwwww", "wwKKKKKKKKKKKKww",
        "wwKwwKwwKwwKwKww", "wwKKKKKKKKKKKKww", "wwwwwwwwwwwwwwww", "dddddddddddddddd"]
    img = art(rows, {'w': wood, 'R': rgb('b02e26'), 'B': rgb('f0f0f0'), 'd': mul(wood, 0.6), 'K': rgb('1b1d1f')})
    save('totem_front', jitter(img, 56, 0.08))

def plants():
    bitter = ["................", "................", ".......g........", "......gGg.......",
              ".....gGGGg..g...", ".g...gGdGg.gGg..", ".gGg..gdg.gGGg..", ".gGGg.gdg.gdg...",
              "..gdGgdddgdg....", "...gddddddg.....", "....gdddddg.....", "......ddd.......",
              "......ddd.......", "......ddd.......", ".......d........", "................"]
    save('bitterleaf', art(bitter, {'g': rgb('1f4a1c'), 'G': rgb('2f6b27'), 'd': rgb('1a3a16')}))
    taro = ["................", "...gggg.........", "..gGGGGg...ggg..", "..gGGLGGg.gGGGg.",
            "...gGGGGggGLGGg.", "....gggs.gGGGg..", "......s..sggg...", "......s.s.......",
            "......ss........", "......s.........", "..g...s....g....", "..gg..s...gg....",
            "...gg.s..gg.....", "....ggsggg......", "......bb........", ".....bBBb......."]
    save('taro_plant', art(taro, {'g': rgb('3c7a2a'), 'G': rgb('59a03a'), 'L': rgb('8ad25f'), 's': rgb('6b8f3a'),
                                  'b': rgb('6b4a3a'), 'B': rgb('8c6a5a')}))
    orchid = ["................", "....w.....w.....", "...wWw...wWw....", "....wPw.wPw.....",
              ".....wWwWw......", "......wPw.......", ".......s........", "...w...s...w....",
              "..wWw..s..wWw...", "...wPw.s.wPw....", "....ws.s.sw.....", ".......s........",
              "......gsg.......", ".....g.s.g......", ".......s........", "................"]
    save('ghost_orchid', art(orchid, {'w': rgb('bfe9ff'), 'W': rgb('ffffff'), 'P': rgb('9fd7ff'), 's': rgb('5a7a8a'), 'g': rgb('46705a')}))

def wreckage():
    img = noisy(rgb('5e6265'), 57, 0.12)
    r = random.Random(58)
    for _ in range(26):
        put(img, r.randrange(16), r.randrange(16), rgb('2b2d2f'))
    for _ in range(10):
        put(img, r.randrange(16), r.randrange(16), rgb('8a5a2a'))
    rect(img, 0, 6, 15, 8, rgb('c4c8ca'))
    rect(img, 0, 7, 15, 7, rgb('1f3a7a'))
    for x in range(0, 16, 3):
        put(img, x, r.randrange(9, 16), rgb('1b1d1f'))
    save('wreckage', img)

def boat():
    top = noisy(rgb('a07b4f'), 59, 0.07)
    for x in (2, 7, 13):
        rect(top, x, 0, x + 1, 15, rgb('6b4a2b'))
    rect(top, 0, 7, 15, 8, rgb('c9a84a'))
    save('boat_frame_top', top)
    side = noisy(rgb('a07b4f'), 60, 0.07)
    for y in (3, 9):
        rect(side, 0, y, 15, y + 1, rgb('6b4a2b'))
    rect(side, 0, 13, 15, 15, rgb('7a5532'))
    save('boat_frame_side', side)
    helm = noisy(rgb('8a5e34'), 61, 0.08)
    for y in range(0, 16, 4):
        rect(helm, 0, y, 15, y, rgb('5c3d22'))
    save('boat_helm', helm)

def dark():
    img = noisy(rgb('2e2a26'), 62, 0.08)
    for y in range(3, 16, 4):
        rect(img, 0, y, 15, y, rgb('1e1b18'))
    save('dark_planks', img)
    pw = noisy(rgb('2e2a26'), 63, 0.08)
    r = random.Random(64)
    for (x, y) in [(1, 1), (8, 0), (4, 6), (11, 5), (0, 10), (7, 11), (12, 10)]:
        w_, h_ = 4, 5
        rect(pw, x, y, x + w_ - 1, y + h_ - 1, rgb('f2efe6'))
        rect(pw, x + 1, y + 1, x + w_ - 2, y + 3, r.choice([rgb('6b8aa0'), rgb('a08060'), rgb('4a5a3a'), rgb('202020')]))
        put(pw, x + 1, y + 1, rgb('d8b58f'))
    put(pw, 5, 7, rgb('b02e26'))
    save('polaroid_wall', pw)
    eff = noisy(rgb('3a3a46'), 65, 0.1)
    rect(eff, 4, 2, 11, 6, rgb('d8cfc4'))
    rect(eff, 2, 9, 13, 10, rgb('25407e'))
    rect(eff, 7, 9, 8, 15, rgb('c9a53c'))
    save('hollow_effigy', eff)

# ------------------------------------------------------------------ station
def station():
    panel = noisy(rgb('5f6668'), 66, 0.05)
    frame(panel, 0, 0, 15, 15, rgb('7a8284'), rgb('3e4444'))
    for p in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        put(panel, *p, rgb('9aa1a5'))
    rect(panel, 0, 7, 15, 8, rgb('c9a53c'))
    for x in range(0, 16, 4):
        put(panel, x, 7, rgb('1b1d1f')); put(panel, x + 1, 8, rgb('1b1d1f'))
    save('bunker_panel', panel)
    l = noisy(rgb('5f6668'), 67, 0.05)
    rect(l, 2, 2, 13, 13, rgb('e8f6ff')); rect(l, 3, 3, 12, 12, rgb('ffffff'))
    save('bunker_light', l)
    lo = noisy(rgb('5f6668'), 67, 0.05)
    rect(lo, 2, 2, 13, 13, rgb('4a5052')); rect(lo, 3, 3, 12, 12, rgb('555c5e'))
    save('bunker_light_off', lo)
    r = random.Random(68)
    def screen(draw):
        img = new(16, 16, rgb('1b1d1f'))
        frame(img, 0, 0, 15, 15, rgb('3e4444'), rgb('111213'))
        draw(img)
        return img
    st = []
    for f in range(6):
        def d(img):
            for y in range(2, 14):
                for x in range(2, 14):
                    v = r.randint(30, 200)
                    put(img, x, y, (v, v, int(v * 1.05) if v < 240 else v, 255))
        st.append(screen(d))
    save('monitor_static', stack(st), frametime=1)
    eye = []
    for f in range(8):
        def d(img, f=f):
            rect(img, 2, 2, 13, 13, rgb('050508'))
            open_ = [2, 3, 3, 3, 3, 2, 1, 0][f]
            if open_ > 0:
                for x in range(3, 13):
                    hgt = int(open_ * (1 - abs(x - 7.5) / 6))
                    rect(img, x, 8 - hgt, x, 7 + hgt, rgb('e8e6df'))
                rect(img, 6, 6, 9, 9, rgb('b02e26'))
                rect(img, 7, 7, 8, 8, rgb('000000'))
        eye.append(screen(d))
    save('monitor_eye', stack(eye), frametime=6)
    save('monitor_off', screen(lambda img: rect(img, 2, 2, 13, 13, rgb('0b0c0d'))))
    rack = []
    for f in range(4):
        img = metal(rgb('2b2d2f'), 69)
        for y in range(2, 15, 3):
            rect(img, 2, y, 13, y + 1, rgb('151617'))
            put(img, 11, y, rgb('5eff6a') if (y + f) % 3 else rgb('1d6a2a'))
            put(img, 12, y, rgb('ff4040') if (y * f) % 5 == 1 else rgb('5a1010'))
        rack.append(img)
    save('server_rack_front', stack(rack), frametime=8)
    term = []
    for f in range(4):
        img = metal(rgb('45494c'), 70)
        rect(img, 2, 2, 13, 11, rgb('120a1a'))
        for y in range(3, 11, 2):
            ln = r.randint(4, 10)
            rect(img, 3, y, 3 + ln, y, rgb('c06cff'))
        if f % 2 == 0:
            rect(img, 3 + r.randint(2, 8), 9, 4 + r.randint(2, 8), 9, rgb('ffffff'))
        rect(img, 5, 13, 10, 13, rgb('c06cff'))
        term.append(img)
    save('halo_terminal_front', stack(term), frametime=10)
    top = metal(rgb('45494c'), 71)
    for y in range(4, 14, 2):
        for x in range(2, 14, 2):
            put(top, x, y, rgb('2b2d2f'))
    save('halo_terminal_top', top)
    kp = metal(rgb('3e4444'), 72)
    for y in range(3):
        for x in range(3):
            rect(kp, 3 + x * 4, 3 + y * 4, 4 + x * 4, 4 + y * 4, rgb('c9ced2'))
    rect(kp, 6, 14, 9, 14, rgb('ff4040'))
    save('keypad', kp)
    tx = []
    for f in range(4):
        img = metal(rgb('2b2d2f'), 73)
        for y in range(2, 14, 2):
            for x in range(2, 14, 2):
                on = ((x * 7 + y * 3 + f * 5) % 9) < 5
                put(img, x, y, rgb('ff3030') if on else rgb('4a0e0e'))
        tx.append(img)
    save('transmitter_front', stack(tx), frametime=5)
    off = metal(rgb('2b2d2f'), 73)
    for y in range(2, 14, 2):
        for x in range(2, 14, 2):
            put(off, x, y, rgb('240808'))
    save('transmitter_off', off)

def airport_seat():
    img = noisy(rgb('2b4f9a'), 74, 0.06)
    frame(img, 0, 0, 15, 15, rgb('3d63b0'), rgb('1f3a7a'))
    for y in range(3, 16, 4):
        rect(img, 1, y, 14, y, rgb('25478c'))
    save('airport_seat', img)

for fn in [terminal_tile, terminal_carpet, terminal_wall, ceiling_panel, ceiling_light, meridian_logo, checkin, conveyor, xray, scanner,
           stanchion, barrier, departure_board, boarding_scanner, kiosk, vending, suitcase, fuselage, window, airplane_seat, seat_backs,
           cabin, seatbelt, galley, mirror, call_button, oxygen_mask, palm, coconut, thatch, totem, plants, wreckage, boat, dark, station,
           airport_seat]:
    fn()
print('blocks:', len([f for f in os.listdir(OUT) if f.endswith('.png')]))
