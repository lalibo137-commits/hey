#!/usr/bin/env python3
"""Item sprites, hand-drawn on 16x16 grids in the vanilla style."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from texlib import *

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'airlinestory', 'textures', 'item')
os.makedirs(OUT, exist_ok=True)

def save(name, rows, colors, jit=0.04, seed=1):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), (name, [len(r) for r in rows])
    img = art(rows, {k: rgb(v) for k, v in colors.items()})
    if jit:
        img = jitter(img, seed, jit)
    img.save(os.path.join(OUT, name + '.png'))

save('travel_journal', [
    "................",
    "...oooooooooo...",
    "..obbbbbbbbbbo..",
    "..obBBBBBBBBpo..",
    "..obBBBBBBBBpo..",
    "..obBBggggBBpo..",
    "..obBBgyygBBpo..",
    "..obBBggggBBpo..",
    "..obBBBBBBBBpo..",
    "..obBBBBBBBBpo..",
    "..obBBBBBBBBpo..",
    "..obBBBBBBBBpo..",
    "..obddddddddpo..",
    "...oooooooooo...",
    "................",
    "................"], {'o': '2a1a0e', 'b': '6b3f1f', 'B': '8c5a2e', 'd': '5a3418', 'p': 'e8e0c8', 'g': 'c9a53c', 'y': 'ffd34e'})

save('passport', [
    "................",
    "....oooooooo....",
    "...onnnnnnnno...",
    "...onNNNNNNno...",
    "...onNNyyNNno...",
    "...onNyYYyNno...",
    "...onNyYYyNno...",
    "...onNNyyNNno...",
    "...onNNNNNNno...",
    "...onNyyyyNno...",
    "...onNNNNNNno...",
    "...onNyyyyNno...",
    "...onnnnnnnno...",
    "....oooooooo....",
    "................",
    "................"], {'o': '0e1a33', 'n': '1f3a7a', 'N': '2a4b96', 'y': 'c9a53c', 'Y': 'ffd34e'})

save('boarding_pass', [
    "................",
    "................",
    "................",
    ".oooooooooooooo.",
    ".owwwwwwwwkwwwo.",
    ".obbbbbbbbkbbbo.",
    ".owddwwddwkwdwo.",
    ".owwwwwwwwkwwwo.",
    ".owddddwwwkwddo.",
    ".owwwwwwwwkwwwo.",
    ".owlwlllwlklwlo.",
    ".owlwlllwlklwlo.",
    ".oooooooooooooo.",
    "................",
    "................",
    "................"], {'o': '6b6b6b', 'w': 'f4f2ec', 'b': '2f63c4', 'd': '6b7272', 'k': 'c4c0b6', 'l': '1b1d1f'})

save('carry_on_bag', [
    "................",
    "......oooo......",
    ".....o....o.....",
    ".....o....o.....",
    "..oooooooooooo..",
    "..onnnnnnnnnno..",
    "..onNNNNNNNNNo..",
    "..onNNNNNNNNNo..",
    "..oyyyyyyyyyyo..",
    "..onNNNNNNNNNo..",
    "..onNNNNNNNNNo..",
    "..onNNNNNNNNNo..",
    "..onnnnnnnnnno..",
    "..oooooooooooo..",
    "...k........k...",
    "................"], {'o': '0e1a33', 'n': '1f3a7a', 'N': '2f4f9a', 'y': 'c9a53c', 'k': '1b1d1f'})

def polaroid(name, pic, seed):
    rows = [
        "................",
        "..oooooooooooo..",
        "..owwwwwwwwwwo..",
        "..owppppppppwo..",
        "..owppppppppwo..",
        "..owppppppppwo..",
        "..owppppppppwo..",
        "..owppppppppwo..",
        "..owppppppppwo..",
        "..owppppppppwo..",
        "..owwwwwwwwwwo..",
        "..owwwwwwwwwwo..",
        "..owwwwwwwwwwo..",
        "..oooooooooooo..",
        "................",
        "................"]
    img = art(rows, {'o': rgb('9a9a90'), 'w': rgb('f2efe6'), 'p': rgb('202020')})
    for (x, y), c in pic.items():
        put(img, x, y, rgb(c))
    jitter(img, seed, 0.03).save(os.path.join(OUT, name + '.png'))

seat = {}
for y in range(3, 10):
    for x in range(4, 12):
        seat[(x, y)] = '3a3a46'
for x in range(5, 11):
    for y in range(4, 10):
        seat[(x, y)] = '25407e'
seat.update({(7, 5): 'd8b58f', (8, 5): 'd8b58f', (7, 6): 'd8b58f', (8, 6): 'd8b58f', (6, 7): '5b8f4a', (7, 7): '5b8f4a', (8, 7): '5b8f4a', (9, 7): '5b8f4a',
             (6, 8): '5b8f4a', (9, 8): '5b8f4a', (7, 4): '4a3020', (8, 4): '4a3020', (10, 4): '9fc8e8', (10, 5): '9fc8e8'})
polaroid('polaroid_seat', seat, 2)
beach = {}
for y in range(3, 10):
    for x in range(4, 12):
        beach[(x, y)] = '43a5d6' if y < 6 else 'dbcfa3'
beach.update({(6, 7): '5b8f4a', (7, 7): '5b8f4a', (8, 7): '5b8f4a', (9, 7): 'd8b58f', (5, 7): '4a3020', (6, 8): '3a5a8a', (7, 8): '3a5a8a', (8, 8): '3a5a8a'})
polaroid('polaroid_beach', beach, 3)
grove = {}
for y in range(3, 10):
    for x in range(4, 12):
        grove[(x, y)] = '1a1a1f'
grove.update({(7, 4): 'd8cfc4', (8, 4): 'd8cfc4', (7, 5): 'd8cfc4', (8, 5): 'd8cfc4', (6, 6): '3a3a46', (7, 6): '3a3a46', (8, 6): '3a3a46', (9, 6): '3a3a46',
              (6, 7): '3a3a46', (9, 7): '3a3a46', (5, 4): '2a2a2a', (10, 3): '2a2a2a', (10, 4): '2a2a2a', (5, 8): '46705a'})
polaroid('polaroid_grove', grove, 4)

save('crumpled_note', [
    "................",
    "................",
    "....oo.oooo.....",
    "...owwowwwwoo...",
    "..owwlwwwlwwwo..",
    "..owwwwwwwwwwo..",
    "...owllwlllwwo..",
    "..owwwwwwwwwo...",
    "..owlllwllwwwo..",
    "...owwwwwwwwwo..",
    "..owwllwlllwo...",
    "..owwwwwwwwwwo..",
    "...oowwoowwoo...",
    ".....oo..oo.....",
    "................",
    "................"], {'o': '9a9a90', 'w': 'f2efe6', 'l': '3a3a3a'})

save('coffee', [
    "................",
    "......s..s......",
    ".......s..s.....",
    "......s..s......",
    "....oooooooo....",
    "....owwwwwwo....",
    "....obbbbbbo....",
    "....ownnnnwooo..",
    "....owNyyNwo.o..",
    "....ownnnnwooo..",
    "....owwwwwwo....",
    ".....owwwwo.....",
    ".....oooooo.....",
    "................",
    "................",
    "................"], {'o': '6b6b6b', 'w': 'f4f2ec', 'b': '5a3418', 's': 'c8c8c8', 'n': '1f3a7a', 'N': '2f4f9a', 'y': 'ffd34e'})

save('airline_peanuts', [
    "................",
    "................",
    "....oooooooo....",
    "...onnnnnnnno...",
    "...onyyyyyynno..",
    "...onyNNNNynno..",
    "...onyyyyyynno..",
    "...onnnnnnnnno..",
    "...ontttttttno..",
    "...onttTTttnno..",
    "...ontTttTtnno..",
    "...onttttttnno..",
    "...onnnnnnnnno..",
    "....ooooooooo...",
    "................",
    "................"], {'o': '0e1a33', 'n': '2f4f9a', 'y': 'ffd34e', 'N': 'b02e26', 't': 'c9a26a', 'T': 'a07b4f'})

save('airline_meal', [
    "................",
    "................",
    "................",
    ".oooooooooooooo.",
    ".owwwwwwwwwwwwo.",
    ".owYYYYwwggggwo.",
    ".owYYYYwwgGGgwo.",
    ".owYYYYwwggggwo.",
    ".owwwwwwwwwwwwo.",
    ".owbbbbbbbwrrwo.",
    ".owbBBBBbbwrrwo.",
    ".owbbbbbbbwwwwo.",
    ".owwwwwwwwwwwwo.",
    ".oooooooooooooo.",
    "................",
    "................"], {'o': '6b6b6b', 'w': 'd8d8d0', 'Y': 'e8c86a', 'g': '4c9a2a', 'G': '68b83d', 'b': 'a0612e', 'B': 'c47a3a', 'r': 'b02e26'})

save('plant_fiber', [
    "................",
    "...........g....",
    "..........gG....",
    ".........gG.....",
    "....g...gG......",
    ".....g.gG...g...",
    "......gG...gG...",
    ".....gGg..gG....",
    "....gG.g.gG.....",
    "...gG...gG......",
    "..gG...gGg......",
    "..g...gG.g......",
    ".....gG...g.....",
    "....gG..........",
    "...g............",
    "................"], {'g': '5a7a2a', 'G': '8aa84a'})

save('palm_frond', [
    "................",
    "..............s.",
    ".........g...s..",
    "........gG..s...",
    "..g....gG..s....",
    "...gg.gG..sgg...",
    "....gGG..sgGG...",
    ".gg..g..sgGg....",
    "..gGG..s.gg.....",
    "...gg.s..gGg....",
    "...g.sgg..g.....",
    "....s.gGg.......",
    "...s...gg.......",
    "..s.............",
    ".s..............",
    "................"], {'g': '3a7d20', 'G': '68b83d', 's': '6b8f3a'})

save('rope', [
    "................",
    "....oooooo......",
    "...orRrRrRo.....",
    "..orRo...orR....",
    "..oRo.....oRo...",
    "..orRo...oRro...",
    "...orRrRrRro....",
    "....ooooooo.....",
    "....orRrRrRo....",
    "...orRo...oRro..",
    "...oRo.....oRo..",
    "...orRo...oRro..",
    "....orRrRrRro...",
    ".....oooooooR...",
    "............rR..",
    ".............r.."], {'o': '5a4220', 'r': 'a08048', 'R': 'c9a86a'})

save('airline_blanket', [
    "................",
    "................",
    "...ooooooooooo..",
    "..onnnnnnnnnnno.",
    "..onNNNNNNNNNno.",
    "..oyyyyyyyyyyyo.",
    "..onNNNNNNNNNno.",
    "..onNNNNNNNNNno.",
    "..onNNNNNNNNNno.",
    "..oyyyyyyyyyyyo.",
    "..onNNNNNNNNNno.",
    "..onnnnnnnnnnno.",
    "...ooooooooooo..",
    "...t.t.t.t.t.t..",
    "................",
    "................"], {'o': '0e1a33', 'n': '1f3a7a', 'N': '2f4f9a', 'y': 'ffd34e', 't': '2f4f9a'})

save('sail_cloth', [
    "................",
    ".ro.............",
    ".roooooooooooo..",
    ".rowwwwwwwwwwwo.",
    ".rowWWWWWWWWWwo.",
    ".rowWWWWWWWWWwo.",
    ".robbbbbbbbbbbo.",
    ".rowWWWWWWWWWwo.",
    ".rowWWWWWWWWWwo.",
    ".rowWWWWWWWWwo..",
    ".rowwwwwwwwwo...",
    ".roooooooooo....",
    ".r..............",
    ".r..............",
    "................",
    "................"], {'o': '9a9a90', 'w': 'e8e6df', 'W': 'f8f6f0', 'b': '2f63c4', 'r': 'a08048'})

save('hull_panel', [
    "................",
    "................",
    "..oooooooooo....",
    "..owwwwwwwwwoo..",
    "..owWWWWWWWWwo..",
    "..owWkWWWWkWwo..",
    "..onnnnnnnnnnoo.",
    "..oyyyyyyyyyyo..",
    "..owWWWWWWWWwo..",
    "..owWkWWWWkWwo..",
    "..owWWWWWWWwo...",
    "..owwwwwwwwo....",
    "..ooo.oooo......",
    "................",
    "................",
    "................"], {'o': '6b7272', 'w': 'c4c8ca', 'W': 'e8ebec', 'k': '6b7272', 'n': '1f3a7a', 'y': 'ffd34e'})

save('mast', [
    "............oo..",
    "...........olo..",
    "..........olLo..",
    ".........olLo...",
    "........olLo....",
    ".......olLo.....",
    "......orrr......",
    ".....olLo.......",
    "....olLo........",
    "...olLo.........",
    "..olLo..........",
    ".orrr...........",
    ".lLo............",
    "olo.............",
    "oo..............",
    "................"], {'o': '3a2516', 'l': '8a6a45', 'L': 'b08f61', 'r': 'c9a86a'})

def flask(name, liquid, liquid2, cork='8a6a45'):
    rows = [
        "................",
        "......occo......",
        "......oCCo......",
        ".......oo.......",
        "......owwo......",
        ".....owwwwo.....",
        "....owLLLLwo....",
        "...owLllllLwo...",
        "...owLllllLwo...",
        "...owLllllLwo...",
        "...owLllllLwo...",
        "....owLLLLwo....",
        ".....owwwwo.....",
        "......oooo......",
        "................",
        "................"]
    colors = {'o': '5a4220', 'w': 'a07b4f', 'c': cork, 'C': 'b08f61', 'L': liquid2, 'l': liquid}
    save(name, rows, colors)

flask('empty_flask', 'c4a27a', 'a8875e')
flask('water_flask', '4f9be0', '3a7dc4')
flask('sea_water_flask', '4fa89a', '357a6e')
flask('ghost_tonic', 'bfe9ff', '7fc8ff')

save('coconut_half', [
    "................",
    "................",
    "................",
    "................",
    "...oooooooooo...",
    "..owwwwwwwwwwo..",
    "..owWWWWWWWWwo..",
    "..obwwwwwwwwbo..",
    "..obbwwwwwwbbo..",
    "...obbbbbbbbo...",
    "...obBbbBbbbo...",
    "....obbbBbbo....",
    ".....oooooo.....",
    "................",
    "................",
    "................"], {'o': '3a2516', 'w': 'efe8d8', 'W': 'ffffff', 'b': '6b4a2b', 'B': '8c6a45'})

save('taro_root', [
    "................",
    "........g.......",
    ".......gG.......",
    "......g.g.......",
    "......oooo......",
    ".....obbbbo.....",
    "....obBbbbbo....",
    "...obBbbdbbbo...",
    "...obbbbbbdbo...",
    "...obdbbbbbbo...",
    "...obbbbBbbbo...",
    "....obbbbbbo....",
    ".....obbbbo.....",
    "......oooo......",
    "................",
    "................"], {'o': '3a2a20', 'b': '8c6a5a', 'B': 'a8877a', 'd': '5a4236', 'g': '5a8a3a', 'G': '7ab04a'})

save('roasted_taro', [
    "................",
    "................",
    "................",
    "......oooo......",
    ".....obbbbo.....",
    "....obByybbo....",
    "...obByYYybbo...",
    "...obyYYYYybo...",
    "...obyYYYYybo...",
    "...obbyYYybbo...",
    "...obbbyybbbo...",
    "....obbbbbbo....",
    ".....obbbbo.....",
    "......oooo......",
    "................",
    "................"], {'o': '2a1a10', 'b': '5a3418', 'B': '7a4a22', 'y': 'd8b878', 'Y': 'f0d8a0'})

save('fish_skewer', [
    "................",
    "...............s",
    "..............s.",
    ".........oooos..",
    "........obbbsbo.",
    ".......obBBsBbo.",
    "......obBBsBbo..",
    ".....obbBsbbo...",
    "....ooobsbbo....",
    "...oyyysobo.....",
    "..oyYYsyoo......",
    "..oyYsYyo.......",
    "..ooso.oo.......",
    "...s............",
    "..s.............",
    ".s.............."], {'o': '3a2516', 'b': 'c47a3a', 'B': 'e09a5a', 's': 'b08f61', 'y': 'a0612e', 'Y': 'c88a4a'})

save('tribal_stew', [
    "................",
    "................",
    "................",
    "................",
    "..oooooooooooo..",
    ".ossssggssbssso.",
    ".osSsgGgssbBsso.",
    ".obsssgssbbssbo.",
    "..owwwwwwwwwwo..",
    "..owWWWWWWWWwo..",
    "...owwwwwwwwo...",
    "....owwwwwwo....",
    ".....oooooo.....",
    "................",
    "................",
    "................"], {'o': '3a2516', 's': 'b06a3a', 'S': 'd08a5a', 'g': '3a7d20', 'G': '68b83d', 'b': 'e0c46a', 'B': 'f8e090', 'w': '7a5532', 'W': '9a7448'})

save('stone_knife', [
    "................",
    "............oo..",
    "...........osso.",
    "..........osSso.",
    ".........osSso..",
    "........osSso...",
    ".......osSso....",
    "......oosso.....",
    ".....ofoo.......",
    "....ofFo........",
    "...owfo.........",
    "..owwo..........",
    ".owwo...........",
    ".oo.............",
    "................",
    "................"], {'o': '2a2a2a', 's': '7a7a7a', 'S': 'a8a8a8', 'f': '8aa84a', 'F': 'b0c86a', 'w': '6b4a2b'})

save('machete', [
    "...........oo...",
    "..........owwo..",
    ".........owWwo..",
    "........owWwo...",
    ".......owWwo....",
    "......owWwo.....",
    ".....owWwo......",
    "....owWwo.......",
    "...ownno........",
    "..oyrno.........",
    ".orRro..........",
    ".orro...........",
    "orro............",
    "oro.............",
    "oo..............",
    "................"], {'o': '2a2a2a', 'w': 'b5b9bb', 'W': 'e8ebec', 'n': '1f3a7a', 'y': 'ffd34e', 'r': 'a08048', 'R': 'c9a86a'})

save('flare', [
    "................",
    "........y.......",
    ".......yRy......",
    "......yRRRy.....",
    ".......oRo......",
    "......orrro.....",
    "......orRro.....",
    "......orrro.....",
    "......owwwo.....",
    "......orrro.....",
    "......orRro.....",
    "......orrro.....",
    "......okkko.....",
    "......okKko.....",
    ".......ooo......",
    "................"], {'o': '3a0e0e', 'r': 'b02e26', 'R': 'e04a3a', 'y': 'ffd34e', 'w': 'f4f2ec', 'k': '2a2a2a', 'K': '4a4a4a'})

save('tidefolk_charm', [
    "................",
    "....s......s....",
    "....s......s....",
    ".....s....s.....",
    "......s..s......",
    ".......ss.......",
    "......oooo......",
    ".....obbbbo.....",
    "....obBwwBbo....",
    "....obwRRwbo....",
    "....obwRRwbo....",
    "....obBwwBbo....",
    ".....obbbbo.....",
    "......oooo......",
    ".......tt.......",
    "................"], {'o': '3a2516', 'b': '8a6a45', 'B': 'b08f61', 'w': 'efe8d8', 'R': '2f63c4', 's': 'a08048', 't': '4a9ac4'})

save('boat_blueprint', [
    "................",
    "................",
    ".oooooooooooooo.",
    ".obbbbbbbbbbbbo.",
    ".obBBBBBlBBBBbo.",
    ".obBBBBBlwBBBbo.",
    ".obBBBBBlwwBBbo.",
    ".obBBBBBlwwwBbo.",
    ".obBBBBBlBBBBbo.",
    ".obBlllllllllbo.",
    ".obBBlBBBBBlBbo.",
    ".obBBBlllllBBbo.",
    ".obbbbbbbbbbbbo.",
    ".oooooooooooooo.",
    "................",
    "................"], {'o': '3a2516', 'b': '8a6a45', 'B': 'b08f61', 'l': '3a2516', 'w': 'efe8d8'})

save('navigation_chart', [
    "................",
    "................",
    ".oooooooooooooo.",
    ".opppppppppppps.",
    ".opbbbbbbbbbbps.",
    ".opbbgGbbbbbbps.",
    ".opbgGGgbbrbbps.",
    ".opbbggbbrbbbps.",
    ".opbbbbbrbbbbps.",
    ".opbbbbrbbbbbps.",
    ".opbbbrbbbbXbps.",
    ".opbbbbbbbbbbps.",
    ".opppppppppppps.",
    ".ossssssssssss..",
    "................",
    "................"], {'o': '6b5a3a', 'p': 'e8dcb0', 's': 'a8987a', 'b': '7fb8d8', 'g': 'c9a86a', 'G': '5a8a3a', 'r': 'b02e26', 'X': '1b1d1f'})

save('station_keycard', [
    "................",
    "................",
    "................",
    "..oooooooooooo..",
    "..owwwwwwwwwwo..",
    "..orrrrrrrrrro..",
    "..owwwwwwwwwwo..",
    "..owffwwlllwwo..",
    "..owffwwwwwwwo..",
    "..owwwwwllwwwo..",
    "..okkkkkkkkkko..",
    "..oooooooooooo..",
    "................",
    "................",
    "................",
    "................"], {'o': '3e4444', 'w': 'f4f2ec', 'r': 'b02e26', 'f': 'd8b58f', 'l': '6b7272', 'k': '1b1d1f'})
print('items:', len(os.listdir(OUT)))
