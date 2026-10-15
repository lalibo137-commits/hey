#!/usr/bin/env python3
"""v2 textures: new story items and props, and palm wood redrawn on vanilla's plank/log structure."""
import os, sys, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from texlib import *

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'airlinestory', 'textures')
ITEM = os.path.join(BASE, 'item')
BLOCK = os.path.join(BASE, 'block')


def save(folder, name, rows, colors, jit=0.03, seed=1):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), (name, [len(r) for r in rows])
    img = art(rows, {k: rgb(v) for k, v in colors.items()})
    if jit:
        img = jitter(img, seed, jit)
    img.save(os.path.join(folder, name + '.png'))
    return img


# =========================================================================================== items
save(ITEM, 'smartphone', [
    "................",
    ".....oooooo.....",
    "....okkkkkko....",
    "....okbbbbko....",
    "....okbyybko....",
    "....okbyybko....",
    "....okbbbbko....",
    "....okbwwbko....",
    "....okbbbbko....",
    "....okbwbwko....",
    "....okbbbbko....",
    "....okkggkko....",
    "....okkkkkko....",
    ".....oooooo.....",
    "................",
    "................"], {'o': '101010', 'k': '2b2b2e', 'b': '3a6fd0', 'y': 'ffd34e', 'w': 'e8eef8', 'g': '6b6b70'})

save(ITEM, 'plush_rabbit', [
    "................",
    "......oo........",
    ".....ogGo.......",
    ".....ogGo.......",
    ".....ogGo..oo...",
    "......ogo.oo....",
    ".....ooggoo.....",
    "....ogggggo.....",
    "....ogkgkgo.....",
    "....oggwwgo.....",
    ".....ogggo......",
    "....ogggggo.....",
    "...oggggggGo....",
    "...ogGggggGo....",
    "....oooooooo....",
    "................"], {'o': '3f3f44', 'g': 'a9a9ad', 'G': 'c8c8cc', 'k': '141414', 'w': 'f0eeea'})

save(ITEM, 'wet_floor_sign', [
    "................",
    ".......oo.......",
    "......oyyo......",
    "......oyyo......",
    ".....oyyyyo.....",
    ".....oykkyo.....",
    "....oyykkyyo....",
    "....oyyyyyyo....",
    "...oyykyykyyo...",
    "...oyyyyyyyyo...",
    "...oykkkkkkyo...",
    "..oyyyyyyyyyyo..",
    "..oyyyyyyyyyyo..",
    "..oDDDDDDDDDDo..",
    "..oo........oo..",
    "................"], {'o': '6b5200', 'y': 'f2c81f', 'k': '2a2a2a', 'D': 'b88f00'})

save(ITEM, 'lost_wallet', [
    "................",
    "................",
    "................",
    "...oooooooooo...",
    "..obbbbbbbbbbo..",
    "..obBBBBBBBBbo..",
    "..obBBBBBBBBbo..",
    "..obbbbbbbbbbo..",
    "..obBBBBBBBByo..",
    "..obBBBBBBBByo..",
    "..obBBBBBBBBbo..",
    "..obbbbbbbbbbo..",
    "...oooooooooo...",
    "................",
    "................",
    "................"], {'o': '24150a', 'b': '5c3a1e', 'B': '74492a', 'y': 'c9a53c'})

save(ITEM, 'staff_lanyard', [
    "....r......r....",
    "....r......r....",
    ".....r....r.....",
    ".....r....r.....",
    "......r..r......",
    "......r..r......",
    ".......rr.......",
    ".......gg.......",
    ".....oooooo.....",
    ".....owwwwo.....",
    ".....orrrro.....",
    ".....owkkwo.....",
    ".....owwwwo.....",
    ".....owkkwo.....",
    ".....oooooo.....",
    "................"], {'r': 'b02e26', 'g': 'a0a0a0', 'o': '4a4a4a', 'w': 'f2f2f2', 'k': '303030'})

save(ITEM, 'flight_tape', [
    "................",
    "................",
    "................",
    "..oooooooooooo..",
    "..oKKKKKKKKKKo..",
    "..oKwwwwwwwwKo..",
    "..oKwrrrrrrwKo..",
    "..oKwwwwwwwwKo..",
    "..oKKkkKKkkKKo..",
    "..oKkggkkggkKo..",
    "..oKKkkKKkkKKo..",
    "..oKKKKKKKKKKo..",
    "..oKKoooooKKKo..",
    "..oooooooooooo..",
    "................",
    "................"], {'o': '0e0e0e', 'K': '2f2f33', 'w': 'e6e2d6', 'r': 'c23a2a', 'k': '141414', 'g': '8c8c8c'})

save(ITEM, 'engine_coil', [
    "................",
    "......iiii......",
    ".....iIIIIi.....",
    "....cccccccc....",
    "....CCCCCCCC....",
    "....cccccccc....",
    "....CCCCCCCC....",
    "....ccclLccc....",
    "....CCClLCCC....",
    "....cccccccc....",
    "....CCCCCCCC....",
    "....cccccccc....",
    ".....iIIIIi.....",
    "......iiii......",
    "................",
    "................"], {'i': '6b6b6b', 'I': 'b8b8b8', 'c': 'a0573a', 'C': 'c87a52', 'l': '7fe0d0', 'L': 'd4fff6'})

save(ITEM, 'island_map', [
    "................",
    "..pppppppppppp..",
    "..pPPPPPPPPPPp..",
    "..pPPPggPPPPPp..",
    "..pPPggggPPPPp..",
    "..pPgggkggPPPp..",
    "..pPggggggbbPp..",
    "..pPggxgggbPPp..",
    "..pPPggggggPPp..",
    "..pPPPgggggPPp..",
    "..pPPPPggrgPPp..",
    "..pPPPPPggPPPp..",
    "..pPPPPPPPPPPp..",
    "..pppppppppppp..",
    "................",
    "................"], {'p': '8a6a3a', 'P': 'd9c48f', 'g': '5f8a3c', 'k': '6b6b6b', 'b': '3f76c4', 'x': 'b02e26', 'r': 'c23a2a'})

save(ITEM, 'boar_tusk', [
    "................",
    "................",
    "..........ww....",
    ".........wWw....",
    "........wWw.....",
    ".......wWw......",
    "......wWw.......",
    ".....wWw........",
    "....wWw.........",
    "...wWw..........",
    "...ww...........",
    "..ss............",
    ".ss.............",
    "................",
    "................",
    "................"], {'w': 'd8cfb4', 'W': 'f2ecd8', 's': '7a5a2e'})

# =========================================================================================== props
save(BLOCK, 'wet_floor_sign', [
    "......oooo......",
    ".....oyyyyo.....",
    "....oyyyyyyo....",
    "....oyykkyyo....",
    "...oyyykkyyyo...",
    "...oyyykkyyyo...",
    "..oyyyyyyyyyyo..",
    "..oyyyykkyyyyo..",
    ".oyyyyyyyyyyyyo.",
    ".oykykykkykkkyo.",
    ".oykykykyykkyyo.",
    ".oyykykkkykkkyo.",
    ".oyyyyyyyyyyyyo.",
    ".oDDDDDDDDDDDDo.",
    ".oo..........oo.",
    ".oo..........oo."], {'o': '6b5200', 'y': 'f2c81f', 'k': '2a2a2a', 'D': 'b88f00'})

save(BLOCK, 'clue_tape', [
    "KKKKKKKKKKKKKKKK",
    "KwwwwwwwwwwwwwwK",
    "KwrrrrrrrrrrrrwK",
    "KwkkkkwwwwkkkkwK",
    "KwwwwwwwwwwwwwwK",
    "KKKkkKKKKKKkkKKK",
    "KKkggkKKKKkggkKK",
    "KKkggkKKKKkggkKK",
    "KKKkkKKKKKKkkKKK",
    "KKKKKKKKKKKKKKKK",
    "KKKKoooooooKKKKK",
    "KKKKKKKKKKKKKKKK",
    "................",
    "................",
    "................",
    "................"], {'K': '2f2f33', 'w': 'e6e2d6', 'r': 'c23a2a', 'k': '141414', 'g': '8c8c8c', 'o': '0e0e0e'})

save(BLOCK, 'missing_poster', [
    "..wwwwwwwwwwww..",
    "..wRRRRRRRRRRw..",
    "..wwwwwwwwwwww..",
    "..wwwhhhhhhwww..",
    "..wwhhhhhhhhww..",
    "..wwhsssssshww..",
    "..wwsskssksww...",
    "..wwsssssssww...",
    "..wwwsssssswww..",
    "..wwwwbbbbwwww..",
    "..wwwbbbbbbwww..",
    "..wwwwwwwwwwww..",
    "..wkkwkkkwkkww..",
    "..wwwwwwwwwwww..",
    "..wkkkwkkwkkkw..",
    "..wwwwwwwwwwww.."], {'w': 'ece6d6', 'R': 'b02e26', 'h': '4a3220', 's': 'c99a74', 'k': '3a3a3a', 'b': '3f5a8a'}, seed=4)

# lounge TV: an animated news broadcast (3 frames)
def tv(frame):
    rows = [
        "BBBBBBBBBBBBBBBB",
        "BnnnnnnnnnnnnnnB",
        "BnnnnnnnnnnnnnnB",
        "BnsssnnnnnnnnnnB",
        "BnssssnnnnnppnnB",
        "BnshhsnnnnnpPpnB",
        "BnsffsnnnnnpppnB",
        "BnsffsnnnnnnnnnB",
        "BnssssnnnnnnnnnB",
        "BnnnnnnnnnnnnnnB",
        "BrrrrrrrrrrrrrrB",
        "BwwkwwkkwkwwkwwB",
        "BwwwwwwwwwwwwwwB",
        "BBBBBBBBBBBBBBBB",
        "BBBBBBBBBBBBBBBB",
        "BBBBBBBBBBBBBBBB"]
    # ticker scroll
    t = rows[11]
    sh = frame * 3
    rows[11] = 'B' + (t[1:15][sh:] + t[1:15][:sh]) + 'B'
    img = art(rows, {k: rgb(v) for k, v in {'B': '111114', 'n': '2c3f6b', 's': '4a5a7a', 'h': '3a2a1c', 'f': 'c99a74',
                                              'p': 'd0d8e8', 'P': 'b02e26', 'r': 'b02e26', 'w': 'e8e8e8', 'k': '303030'}.items()})
    return jitter(img, 9 + frame, 0.02)

stack([tv(0), tv(1), tv(2), tv(3)]).save(os.path.join(BLOCK, 'lounge_tv.png'))
with open(os.path.join(BLOCK, 'lounge_tv.png.mcmeta'), 'w') as f:
    f.write('{"animation": {"frametime": 12}}\n')


# =========================================================================================== palm wood
def planks(name, shades, seed):
    """Vanilla plank layout: four boards of 4px, dark seam on the bottom row, staggered butt joints, grain streaks."""
    r = random.Random(seed)
    dark, mid, base, light = [rgb(c) for c in shades]
    img = new()
    joints = [11, 4, 13, 7]
    for b in range(4):
        for y in range(b * 4, b * 4 + 4):
            for x in range(16):
                c = base
                if y == b * 4 + 3:
                    c = dark
                put(img, x, y, c)
        # grain: horizontal streaks
        for _ in range(4):
            y = b * 4 + r.randint(0, 2)
            x0 = r.randint(0, 15)
            ln = r.randint(2, 6)
            col = mid if r.random() < 0.6 else light
            for x in range(x0, min(16, x0 + ln)):
                put(img, x, y, col)
        jx = joints[b]
        for y in range(b * 4, b * 4 + 3):
            put(img, jx, y, dark)
    img.save(os.path.join(BLOCK, name + '.png'))

planks('palm_planks', ['8a6a3e', 'b08c56', 'c4a066', 'd6b67e'], 7)


def log_side(name, shades, seed):
    """Palm trunk: vertical fibres crossed by the ring scars palms are known for."""
    r = random.Random(seed)
    dark, mid, base, light = [rgb(c) for c in shades]
    img = new()
    for x in range(16):
        col = [base, mid, base, light][r.randint(0, 3)] if x % 3 else mid
        for y in range(16):
            put(img, x, y, col)
    for y0 in (2, 7, 12):
        off = r.randint(0, 1)
        for x in range(16):
            put(img, x, (y0 + (1 if (x // 4) % 2 == off else 0)) % 16, dark)
    for _ in range(10):
        put(img, r.randint(0, 15), r.randint(0, 15), mul(base, 0.85))
    img.save(os.path.join(BLOCK, name + '.png'))

log_side('palm_log', ['4e3b26', '6e5638', '7f6542', '957a52'], 3)


def log_top(name, bark, wood, seed):
    r = random.Random(seed)
    img = new()
    bd, bm = rgb(bark[0]), rgb(bark[1])
    w = [rgb(c) for c in wood]
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                c = bm if (x + y) % 3 else bd
            else:
                ring = int(d) % 3
                c = [w[0], w[1], w[2]][ring]
            put(img, x, y, c)
    for _ in range(6):
        put(img, r.randint(3, 12), r.randint(3, 12), w[0])
    img.save(os.path.join(BLOCK, name + '.png'))

log_top('palm_log_top', ['4e3b26', '6e5638'], ['b08c56', 'c4a066', 'd6b67e'], 5)


# fuselage stripe: smooth quartz-like white with a crisp Meridian blue cheatline
def stripe():
    img = new()
    white, white2, blue, blue2, gold = rgb('ece9e4'), rgb('e0ddd6'), rgb('1f3a7a'), rgb('2a4b96'), rgb('c9a53c')
    for y in range(16):
        for x in range(16):
            c = white if (x * 7 + y * 3) % 11 else white2
            if 6 <= y <= 9:
                c = blue if y in (6, 9) else blue2
            if y == 11:
                c = gold
            put(img, x, y, c)
    for x in range(16):
        put(img, x, 0, white2)
        put(img, x, 15, mul(white2, 0.95))
    img.save(os.path.join(BLOCK, 'fuselage_stripe.png'))

stripe()
print('v2 textures written')
