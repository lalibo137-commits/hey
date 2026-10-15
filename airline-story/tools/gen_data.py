#!/usr/bin/env python3
"""Generates blockstates, models, loot tables, recipes, tags, lang and the world preset."""
import json, os, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
NS = 'airlinestory'
A = os.path.join(ROOT, 'assets', NS)
D = os.path.join(ROOT, 'data', NS)

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')

def bs(name, variants):
    w(f'{A}/blockstates/{name}.json', {'variants': variants})

def model(name, obj):
    w(f'{A}/models/block/{name}.json', obj)

def item_model(name, obj):
    w(f'{A}/models/item/{name}.json', obj)

ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}

# Where vanilla already has the right texture, use it: it is the vanilla art style by definition.
VANILLA = {
    'metal_light': 'minecraft:block/iron_block',
    'metal_dark': 'minecraft:block/gray_concrete',
    'dark_planks': 'minecraft:block/dark_oak_planks',
    'airplane_seat': 'minecraft:block/blue_wool',
    'seat_headrest': 'minecraft:block/white_wool',
    'airport_seat': 'minecraft:block/light_blue_wool',
    'cabin_floor': 'minecraft:block/gray_wool',
    'cabin_panel': 'minecraft:block/quartz_block_bottom',
    'ceiling_panel': 'minecraft:block/quartz_block_bottom',
    'fuselage': 'minecraft:block/quartz_block_bottom',
    'terminal_tile': 'minecraft:block/calcite',
    'terminal_carpet': 'minecraft:block/blue_wool',
    'terminal_wall': 'minecraft:block/white_concrete',
    'story_barrier': 'minecraft:block/glass',
    'boat_frame_top': 'minecraft:block/stripped_jungle_log_top',
    'stanchion_post': 'minecraft:block/iron_block',
}

def t(n):
    return VANILLA.get(n, f'{NS}:block/{n}')

def facing_variants(model_for, extra_props=None):
    """extra_props: list of (name, values). model_for(dict props) -> model name."""
    combos = [{}]
    for name, vals in (extra_props or []):
        combos = [dict(c, **{name: v}) for c in combos for v in vals]
    out = {}
    for f, r in ROT.items():
        for c in combos:
            props = dict(c, facing=f)
            key = ','.join(f'{k}={props[k]}' for k in sorted(props))
            v = {'model': f'{NS}:block/{model_for(props)}'}
            if r:
                v['y'] = r
            out[key] = v
    return out

BLOCK_ITEMS = {}

def cube(name, tex=None, render=None, block_item=True):
    m = {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(tex or name)}}
    if render:
        m['render_type'] = render
    model(name, m)
    bs(name, {'': {'model': f'{NS}:block/{name}'}})
    BLOCK_ITEMS[name] = f'{NS}:block/{name}'

def orientable(name, front, side, top, bottom=None, extra=None, model_name=None, render=None):
    m = {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {'front': t(front), 'side': t(side), 'top': t(top), 'bottom': t(bottom or top)}}
    if render:
        m['render_type'] = render
    model(model_name or name, m)

def simple_orientable(name, front, side, top, bottom=None, render=None):
    orientable(name, front, side, top, bottom, render=render)
    bs(name, facing_variants(lambda p: name))
    BLOCK_ITEMS[name] = f'{NS}:block/{name}'

def elements_model(name, textures, elements, render='minecraft:cutout', particle=None):
    tx = dict(textures)
    tx['particle'] = particle or list(textures.values())[0]
    m = {'parent': 'minecraft:block/block', 'textures': tx, 'elements': elements, 'render_type': render}
    model(name, m)

def el(frm, to, faces, rot=None):
    e = {'from': frm, 'to': to, 'faces': {}}
    for k, v in faces.items():
        tex, uv = (v if isinstance(v, tuple) else (v, None))
        f = {'texture': '#' + tex}
        if uv:
            f['uv'] = uv
        e['faces'][k] = f
    if rot:
        e['rotation'] = rot
    return e

def allf(tex, skip=()):
    return {k: tex for k in ('north', 'south', 'east', 'west', 'up', 'down') if k not in skip}

def box(frm, to, tex, **over):
    faces = allf(tex)
    for k, v in over.items():
        faces[k] = v
    # auto UV: map from coordinates so textures aren't stretched
    out = {}
    x1, y1, z1 = frm; x2, y2, z2 = to
    uvs = {
        'north': [16 - x2, 16 - y2, 16 - x1, 16 - y1], 'south': [x1, 16 - y2, x2, 16 - y1],
        'east': [16 - z2, 16 - y2, 16 - z1, 16 - y1], 'west': [z1, 16 - y2, z2, 16 - y1],
        'up': [x1, z1, x2, z2], 'down': [x1, 16 - z2, x2, 16 - z1]}
    for k, v in faces.items():
        if v is None:
            continue
        if isinstance(v, tuple):
            out[k] = v
        else:
            uv = [max(0, min(16, c)) for c in uvs[k]]
            out[k] = (v, uv)
    return el(frm, to, out)

# ---------------------------------------------------------------------------------------- blocks
for n in ['terminal_tile', 'terminal_carpet', 'terminal_wall', 'ceiling_panel', 'ceiling_light', 'fuselage', 'fuselage_stripe',
          'cabin_floor', 'cabin_panel', 'palm_planks', 'wreckage', 'bunker_panel']:
    cube(n)
model('thatch', {'parent': 'minecraft:block/cube_column', 'textures': {'end': 'minecraft:block/hay_block_top', 'side': 'minecraft:block/hay_block_side'}})
bs('thatch', {'': {'model': f'{NS}:block/thatch'}})
BLOCK_ITEMS['thatch'] = f'{NS}:block/thatch'

cube('story_barrier', render='minecraft:translucent')
cube('airplane_window', render='minecraft:cutout')

for n in ['cabin_light', 'bunker_light']:
    model(n, {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(n)}})
    model(n + '_off', {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(n + '_off')}})
    bs(n, {'lit=true': {'model': f'{NS}:block/{n}'}, 'lit=false': {'model': f'{NS}:block/{n}_off'}})
    BLOCK_ITEMS[n] = f'{NS}:block/{n}'

simple_orientable('meridian_logo', 'meridian_logo', 'terminal_wall', 'terminal_wall')
simple_orientable('checkin_counter', 'checkin_counter_front', 'checkin_counter_side', 'checkin_counter_top', 'metal_dark')
simple_orientable('xray_machine', 'xray_front', 'xray_side', 'xray_top', 'metal_dark')
simple_orientable('scanner_top', 'scanner_top', 'scanner_top', 'metal_light', 'metal_light')
simple_orientable('smilescore_kiosk', 'kiosk_front', 'metal_light', 'metal_light', 'metal_dark')
simple_orientable('vending_machine', 'vending_front', 'vending_side', 'vending_side', 'metal_dark')
simple_orientable('overhead_bin', 'overhead_bin_front', 'cabin_panel', 'cabin_panel', 'cabin_panel')
simple_orientable('galley_counter', 'galley_front', 'metal_light', 'metal_light', 'metal_dark')
simple_orientable('totem', 'totem_front', 'totem_side', 'totem_top')
simple_orientable('boat_frame', 'boat_frame_side', 'boat_frame_side', 'boat_frame_top', 'palm_planks')
simple_orientable('polaroid_wall', 'polaroid_wall', 'dark_planks', 'dark_planks')
simple_orientable('server_rack', 'server_rack_front', 'metal_dark', 'metal_dark')
simple_orientable('halo_terminal', 'halo_terminal_front', 'metal_dark', 'halo_terminal_top', 'metal_dark')

# State blocks (facing x lit x mode)
SI = [('lit', ['true', 'false']), ('mode', ['0', '1', '2'])]
orientable('departure_board', 'departure_board', 'metal_dark', 'metal_dark')
orientable('departure_board_glitch', 'departure_board_glitch', 'metal_dark', 'metal_dark')
bs('departure_board', facing_variants(lambda p: 'departure_board' if p['mode'] == '0' else 'departure_board_glitch', SI))
BLOCK_ITEMS['departure_board'] = f'{NS}:block/departure_board'

orientable('monitor', 'monitor_static', 'metal_dark', 'metal_dark')
orientable('monitor_eye', 'monitor_eye', 'metal_dark', 'metal_dark')
orientable('monitor_off', 'monitor_off', 'metal_dark', 'metal_dark')
bs('monitor', facing_variants(lambda p: {'0': 'monitor', '1': 'monitor_eye', '2': 'monitor_off'}[p['mode']], SI))
BLOCK_ITEMS['monitor'] = f'{NS}:block/monitor'

orientable('transmitter', 'transmitter_front', 'metal_dark', 'metal_dark')
orientable('transmitter_off', 'transmitter_off', 'metal_dark', 'metal_dark')
bs('transmitter', facing_variants(lambda p: 'transmitter' if p['lit'] == 'true' else 'transmitter_off', SI))
BLOCK_ITEMS['transmitter'] = f'{NS}:block/transmitter'

# suitcase: mode 0 closed, 1/2 open
for n, top in [('suitcase', 'suitcase_top'), ('suitcase_open', 'suitcase_open')]:
    elements_model(n, {'side': t('suitcase_side'), 'top': t(top), 'front': t('suitcase_front')}, [
        box([1, 0, 4], [15, 12, 12], 'side', north='front', up='top'),
        box([6, 12, 7], [10, 13.5, 9], 'side'),
    ])
bs('suitcase', facing_variants(lambda p: 'suitcase' if p['mode'] == '0' else 'suitcase_open', SI))
BLOCK_ITEMS['suitcase'] = f'{NS}:block/suitcase'

# seatbelt sign
for n, tex in [('seatbelt_sign', 'seatbelt_sign_on'), ('seatbelt_sign_off', 'seatbelt_sign_off')]:
    elements_model(n, {'face': t(tex), 'metal': t('metal_light')}, [
        box([3, 10, 5], [13, 15, 11], 'metal', north='face', south='face'),
        box([7, 15, 7], [9, 16, 9], 'metal'),
    ])
bs('seatbelt_sign', facing_variants(lambda p: 'seatbelt_sign' if p['lit'] == 'true' else 'seatbelt_sign_off', SI))
BLOCK_ITEMS['seatbelt_sign'] = f'{NS}:block/seatbelt_sign'

# seats
elements_model('airport_seat', {'cushion': t('airport_seat'), 'metal': t('metal_light')}, [
    box([1, 0, 2], [3, 7, 14], 'metal'), box([13, 0, 2], [15, 7, 14], 'metal'),
    box([1, 7, 1], [15, 9, 13], 'cushion'), box([1, 7, 13], [15, 18, 15], 'cushion'),
], render='minecraft:solid')
bs('airport_seat', facing_variants(lambda p: 'airport_seat'))
BLOCK_ITEMS['airport_seat'] = f'{NS}:block/airport_seat'

for i, back in enumerate(['seat_back_logo', 'seat_back_live', 'seat_back_static']):
    elements_model('airplane_seat' + ('' if i == 0 else f'_screen{i}'), {'fabric': t('airplane_seat'), 'metal': t('metal_dark'), 'back': t(back), 'head': t('seat_headrest')}, [
        box([1, 0, 3], [15, 3, 13], 'metal'),
        box([1, 3, 1], [15, 7, 13], 'fabric'),
        box([1, 7, 12], [15, 16, 16], 'fabric', south='back'),
        box([1, 16, 11.5], [15, 20, 16], 'head'),
        box([0, 3, 2], [1, 10, 13], 'metal'), box([15, 3, 2], [16, 10, 13], 'metal'),
    ], render='minecraft:solid', particle=t('airplane_seat'))
bs('airplane_seat', facing_variants(lambda p: 'airplane_seat' + ('' if p['screen'] == '0' else f"_screen{p['screen']}"), [('screen', ['0', '1', '2'])]))
BLOCK_ITEMS['airplane_seat'] = f'{NS}:block/airplane_seat'

elements_model('conveyor_belt', {'top': t('conveyor_belt_top'), 'side': t('conveyor_belt_side'), 'metal': t('metal_dark')}, [
    box([0, 0, 0], [16, 9, 16], 'side', up='top', down='metal')], render='minecraft:solid')
bs('conveyor_belt', facing_variants(lambda p: 'conveyor_belt'))
BLOCK_ITEMS['conveyor_belt'] = f'{NS}:block/conveyor_belt'

elements_model('scanner_pillar', {'side': t('scanner_pillar')}, [box([4, 0, 4], [12, 16, 12], 'side')], render='minecraft:solid')
bs('scanner_pillar', facing_variants(lambda p: 'scanner_pillar'))
BLOCK_ITEMS['scanner_pillar'] = f'{NS}:block/scanner_pillar'

elements_model('stanchion', {'post': t('stanchion_post'), 'belt': t('stanchion_belt')}, [
    box([6, 0, 6], [10, 1, 10], 'post'), box([7, 1, 7], [9, 14, 9], 'post'), box([6.5, 14, 6.5], [9.5, 15, 9.5], 'post'),
    box([0, 11, 7.5], [16, 13, 8.5], 'belt')])
bs('stanchion', facing_variants(lambda p: 'stanchion'))
BLOCK_ITEMS['stanchion'] = f'{NS}:block/stanchion'

elements_model('boarding_scanner', {'front': t('boarding_scanner_front'), 'side': t('metal_light'), 'top': t('boarding_scanner_top')}, [
    box([1, 0, 3], [15, 15, 13], 'side', north='front'), box([0, 15, 2], [16, 16, 14], 'side', up='top')], render='minecraft:solid')
bs('boarding_scanner', facing_variants(lambda p: 'boarding_scanner'))
BLOCK_ITEMS['boarding_scanner'] = f'{NS}:block/boarding_scanner'

elements_model('lavatory_mirror', {'glass': t('mirror'), 'metal': t('metal_light')}, [box([1, 1, 14], [15, 15, 16], 'metal', north='glass')])
bs('lavatory_mirror', facing_variants(lambda p: 'lavatory_mirror'))
BLOCK_ITEMS['lavatory_mirror'] = f'{NS}:block/lavatory_mirror'

elements_model('call_button', {'face': t('call_button'), 'metal': t('metal_light')}, [box([4, 4, 14], [12, 12, 16], 'metal', north='face')])
bs('call_button', facing_variants(lambda p: 'call_button'))
BLOCK_ITEMS['call_button'] = f'{NS}:block/call_button'

elements_model('keypad', {'face': t('keypad'), 'metal': t('metal_dark')}, [box([4, 3, 14], [12, 13, 16], 'metal', north='face')])
bs('keypad', facing_variants(lambda p: 'keypad'))
BLOCK_ITEMS['keypad'] = f'{NS}:block/keypad'

def cross_model(name, tex, render='minecraft:cutout'):
    model(name, {'parent': 'minecraft:block/cross', 'textures': {'cross': t(tex)}, 'render_type': render})

cross_model('oxygen_mask', 'oxygen_mask')
bs('oxygen_mask', facing_variants(lambda p: 'oxygen_mask'))
BLOCK_ITEMS['oxygen_mask'] = 'item'

elements_model('coconut', {'nut': t('coconut')}, [box([4, 3, 4], [12, 11, 12], 'nut'), box([7, 11, 7], [9, 16, 9], 'nut')])
bs('coconut', facing_variants(lambda p: 'coconut'))
BLOCK_ITEMS['coconut'] = f'{NS}:block/coconut'

elements_model('boat_helm', {'wood': t('boat_helm'), 'post': t('palm_planks')}, [
    box([6, 0, 6], [10, 10, 10], 'post'),
    box([1, 6, 6.5], [15, 8, 8.5], 'wood'), box([1, 18, 6.5], [15, 20, 8.5], 'wood'),
    box([1, 8, 6.5], [3, 18, 8.5], 'wood'), box([13, 8, 6.5], [15, 18, 8.5], 'wood'),
    box([7, 8, 6], [9, 18, 9], 'wood'), box([3, 12, 6], [13, 14, 9], 'wood'),
    box([7.5, 20, 7], [8.5, 22, 8], 'wood'), box([7.5, 4, 7], [8.5, 6, 8], 'wood')])
bs('boat_helm', facing_variants(lambda p: 'boat_helm'))
BLOCK_ITEMS['boat_helm'] = f'{NS}:block/boat_helm'

elements_model('hollow_effigy', {'cloth': t('hollow_effigy'), 'stick': t('dark_planks')}, [
    box([7, 0, 7], [9, 16, 9], 'stick'), box([3, 9, 6], [13, 11, 10], 'stick'), box([4, 4, 5], [12, 14, 11], 'cloth')])
bs('hollow_effigy', facing_variants(lambda p: 'hollow_effigy'))
BLOCK_ITEMS['hollow_effigy'] = f'{NS}:block/hollow_effigy'

# palm log / leaves / plants
model('palm_log', {'parent': 'minecraft:block/cube_column', 'textures': {'end': t('palm_log_top'), 'side': t('palm_log')}})
model('palm_log_horizontal', {'parent': 'minecraft:block/cube_column_horizontal', 'textures': {'end': t('palm_log_top'), 'side': t('palm_log')}})
bs('palm_log', {'axis=y': {'model': f'{NS}:block/palm_log'}, 'axis=z': {'model': f'{NS}:block/palm_log_horizontal', 'x': 90},
                'axis=x': {'model': f'{NS}:block/palm_log_horizontal', 'x': 90, 'y': 90}})
BLOCK_ITEMS['palm_log'] = f'{NS}:block/palm_log'
model('palm_leaves', {'parent': 'minecraft:block/leaves', 'textures': {'all': 'minecraft:block/jungle_leaves'}, 'render_type': 'minecraft:cutout_mipped'})
bs('palm_leaves', {'': {'model': f'{NS}:block/palm_leaves'}})
BLOCK_ITEMS['palm_leaves'] = f'{NS}:block/palm_leaves'
for n in ['bitterleaf', 'taro_plant', 'ghost_orchid']:
    cross_model(n, n)
    bs(n, {'': {'model': f'{NS}:block/{n}'}})
    BLOCK_ITEMS[n] = 'item'

# ---------------------------------------------------------------------------------------- v2 story props
SI2 = [('lit', ['true', 'false']), ('mode', ['0', '1', '2'])]
def flat(name, tex, x1, z1, x2, z2):
    elements_model(name, {'t': tex}, [el([x1, 0, z1], [x2, 0.2, z2], {'up': ('t', [0, 0, 16, 16])})])
    bs(name, facing_variants(lambda p: name))
flat('clue_note', 'minecraft:item/paper', 3, 3, 13, 13)
BLOCK_ITEMS['clue_note'] = 'item_tex:minecraft:item/paper'
flat('clue_photo', f'{NS}:item/polaroid_seat', 4, 4, 12, 12)
BLOCK_ITEMS['clue_photo'] = f'item_tex:{NS}:item/polaroid_seat'

elements_model('clue_tape', {'case': t('clue_tape'), 'side': 'minecraft:block/black_concrete'}, [
    el([4, 0, 5], [12, 1.5, 11], {'up': ('case', [0, 0, 16, 12]), 'north': ('side', [4, 14, 12, 16]), 'south': ('side', [4, 14, 12, 16]),
                                  'east': ('side', [5, 14, 11, 16]), 'west': ('side', [5, 14, 11, 16])})])
bs('clue_tape', facing_variants(lambda p: 'clue_tape'))
BLOCK_ITEMS['clue_tape'] = f'item_tex:{NS}:item/flight_tape'

elements_model('wet_floor_sign_block', {'sign': t('wet_floor_sign')}, [
    el([3, 0, 7.5], [13, 15, 8.5], {'north': ('sign', [0, 0, 16, 16]), 'south': ('sign', [0, 0, 16, 16])}, {'origin': [8, 0, 8], 'axis': 'x', 'angle': 22.5}),
    el([3, 0, 7.5], [13, 15, 8.5], {'north': ('sign', [0, 0, 16, 16]), 'south': ('sign', [0, 0, 16, 16])}, {'origin': [8, 0, 8], 'axis': 'x', 'angle': -22.5}),
])
bs('wet_floor_sign_block', facing_variants(lambda p: 'wet_floor_sign_block'))
BLOCK_ITEMS['wet_floor_sign_block'] = f'item_tex:{NS}:item/wet_floor_sign'

elements_model('lost_plush', {'fur': 'minecraft:block/light_gray_wool', 'pale': 'minecraft:block/white_wool', 'eye': 'minecraft:block/black_wool'}, [
    box([5, 0, 6], [11, 5, 11], 'fur'), box([5.5, 5, 6.5], [10.5, 9, 10.5], 'fur'),
    box([6.5, 9, 8], [8, 13, 9], 'fur'), box([7, 6.5, 6.3], [9, 7.5, 6.5], 'pale'),
    box([6.2, 7.5, 6.3], [7, 8.3, 6.5], 'eye'), box([9, 7.5, 6.3], [9.8, 8.3, 6.5], 'eye')])
bs('lost_plush', facing_variants(lambda p: 'lost_plush'))
BLOCK_ITEMS['lost_plush'] = f'item_tex:{NS}:item/plush_rabbit'

elements_model('engine_part', {'copper': 'minecraft:block/exposed_copper', 'iron': 'minecraft:block/iron_block', 'core': 'minecraft:block/sea_lantern'}, [
    box([3, 0, 3], [13, 2, 13], 'iron'), box([4, 2, 4], [12, 10, 12], 'copper'), box([6, 10, 6], [10, 12, 10], 'core'),
    box([3, 4, 3], [13, 5, 13], 'iron'), box([3, 7, 3], [13, 8, 13], 'iron')], render='minecraft:solid')
bs('engine_part', facing_variants(lambda p: 'engine_part'))
BLOCK_ITEMS['engine_part'] = f'item_tex:{NS}:item/engine_coil'

for n, tex in [('lounge_tv', 'lounge_tv'), ('lounge_tv_off', 'monitor_off')]:
    elements_model(n, {'screen': t(tex), 'frame': 'minecraft:block/black_concrete'}, [
        el([-7, -3, 14.5], [23, 13, 16], {'north': ('screen', [0, 0, 16, 16]), 'south': ('frame', [0, 0, 16, 16]), 'up': ('frame', [0, 0, 16, 1.5]),
                                         'down': ('frame', [0, 0, 16, 1.5]), 'east': ('frame', [0, 0, 1.5, 16]), 'west': ('frame', [0, 0, 1.5, 16])})],
        render='minecraft:solid')
bs('lounge_tv', facing_variants(lambda p: 'lounge_tv' if p['mode'] != '2' else 'lounge_tv_off', SI2))
BLOCK_ITEMS['lounge_tv'] = f'{NS}:block/lounge_tv'

elements_model('missing_poster', {'poster': t('missing_poster')}, [
    el([2, -1, 15.6], [14, 15, 16], {'north': ('poster', [2, 0, 14, 16])})])
bs('missing_poster', facing_variants(lambda p: 'missing_poster'))
BLOCK_ITEMS['missing_poster'] = f'{NS}:block/missing_poster'

# item models for blocks
for n, m in BLOCK_ITEMS.items():
    if m == 'item':
        item_model(n, {'parent': 'minecraft:item/generated', 'textures': {'layer0': t(n)}})
    elif m.startswith('item_tex:'):
        item_model(n, {'parent': 'minecraft:item/generated', 'textures': {'layer0': m[9:]}})
    else:
        item_model(n, {'parent': m})

ITEMS = ['travel_journal', 'passport', 'boarding_pass', 'carry_on_bag', 'polaroid_seat', 'polaroid_beach', 'polaroid_grove', 'crumpled_note',
         'coffee', 'airline_peanuts', 'airline_meal', 'plant_fiber', 'palm_frond', 'rope', 'airline_blanket', 'sail_cloth', 'hull_panel',
         'mast', 'empty_flask', 'water_flask', 'sea_water_flask', 'ghost_tonic', 'coconut_half', 'taro_root', 'roasted_taro', 'fish_skewer',
         'tribal_stew', 'flare', 'tidefolk_charm', 'boat_blueprint', 'navigation_chart', 'station_keycard',
         'smartphone', 'plush_rabbit', 'wet_floor_sign', 'lost_wallet', 'staff_lanyard', 'flight_tape', 'engine_coil', 'island_map', 'boar_tusk']
TOOLS = ['stone_knife', 'machete']
for n in ITEMS:
    item_model(n, {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{n}'}})
for n in TOOLS:
    item_model(n, {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'{NS}:item/{n}'}})

# ---------------------------------------------------------------------------------------- loot
def loot_self(name):
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}],
                                                  'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}

ALL_BLOCKS = list(BLOCK_ITEMS.keys())
for n in ALL_BLOCKS:
    lt = loot_self(n)
    if n == 'palm_leaves':
        lt = {'type': 'minecraft:block', 'pools': [
            {'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
                {'type': 'minecraft:item', 'name': f'{NS}:palm_leaves', 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {'items': ['minecraft:shears']}}]},
                {'type': 'minecraft:item', 'name': f'{NS}:palm_frond', 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.35}]}]}]},
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:plant_fiber', 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.4}]}]},
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:stick', 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.15}]}]}]}
    elif n == 'coconut':
        lt = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:coconut_half',
              'functions': [{'function': 'minecraft:set_count', 'count': 2}]}]}]}
    elif n == 'wreckage':
        lt = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:hull_panel'}]}]}
    elif n == 'taro_plant':
        lt = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:taro_root',
              'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]}]},
              {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:plant_fiber'}]}]}
    elif n == 'thatch':
        lt = loot_self(n)
    elif n in ('story_barrier', 'boat_frame', 'boat_helm', 'oxygen_mask', 'clue_note', 'clue_photo', 'clue_tape', 'wet_floor_sign_block',
               'lost_plush', 'engine_part', 'lounge_tv', 'missing_poster'):
        lt = {'type': 'minecraft:block', 'pools': []}
    w(f'{D}/loot_tables/blocks/{n}.json', lt)

# ---------------------------------------------------------------------------------------- tags
def tag(path, values, ns='minecraft'):
    w(os.path.join(ROOT, 'data', ns, 'tags', path + '.json'), {'replace': False, 'values': values})

tag('blocks/logs', [f'{NS}:palm_log'])
tag('blocks/logs_that_burn', [f'{NS}:palm_log'])
tag('items/logs', [f'{NS}:palm_log'])
tag('items/logs_that_burn', [f'{NS}:palm_log'])
tag('blocks/planks', [f'{NS}:palm_planks'])
tag('items/planks', [f'{NS}:palm_planks'])
tag('blocks/leaves', [f'{NS}:palm_leaves'])
tag('items/leaves', [f'{NS}:palm_leaves'])
tag('blocks/mineable/axe', [f'{NS}:palm_log', f'{NS}:palm_planks', f'{NS}:totem', f'{NS}:coconut', f'{NS}:polaroid_wall', f'{NS}:hollow_effigy'])
tag('blocks/mineable/hoe', [f'{NS}:palm_leaves', f'{NS}:thatch'])
tag('blocks/mineable/pickaxe', [f'{NS}:wreckage'])
tag('items/fishes', [])

# ---------------------------------------------------------------------------------------- recipes
R = f'{D}/recipes'
def shapeless(name, ingredients, result, count=1):
    w(f'{R}/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                           'ingredients': [i if isinstance(i, dict) else {'item': i} for i in ingredients],
                           'result': {'item': result, 'count': count}})
def shaped(name, pattern, key, result, count=1):
    w(f'{R}/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
                           'key': {k: (v if isinstance(v, dict) else {'item': v}) for k, v in key.items()},
                           'result': {'item': result, 'count': count}})
def cook(name, ingredient, result, xp=0.35):
    for kind, time in [('smelting', 200), ('campfire_cooking', 600), ('smoking', 100)]:
        if kind == 'smoking' and 'flask' in ingredient:
            continue
        w(f'{R}/{name}_{kind}.json', {'type': f'minecraft:{kind}', 'category': 'food', 'ingredient': {'item': ingredient},
                                      'result': result, 'experience': xp, 'cookingtime': time})

shapeless('palm_planks', [f'{NS}:palm_log'], f'{NS}:palm_planks', 4)
shapeless('rope', [f'{NS}:plant_fiber'] * 3, f'{NS}:rope')
shapeless('string_from_fiber', [f'{NS}:plant_fiber'] * 2, 'minecraft:string')
shaped('sail_cloth', ['BBB', 'BRB', 'BBB'], {'B': f'{NS}:airline_blanket', 'R': f'{NS}:rope'}, f'{NS}:sail_cloth')
shaped('mast', ['LR', 'LR', 'L '], {'L': f'{NS}:palm_log', 'R': f'{NS}:rope'}, f'{NS}:mast')
shaped('stone_knife', ['S', 'F', 'T'], {'S': {'tag': 'minecraft:stone_tool_materials'}, 'F': f'{NS}:plant_fiber', 'T': 'minecraft:stick'}, f'{NS}:stone_knife')
shaped('stone_knife_flint', ['S', 'F', 'T'], {'S': 'minecraft:flint', 'F': f'{NS}:plant_fiber', 'T': 'minecraft:stick'}, f'{NS}:stone_knife')
shaped('machete', [' H', 'R ', 'T '], {'H': f'{NS}:hull_panel', 'R': f'{NS}:rope', 'T': 'minecraft:stick'}, f'{NS}:machete')
shaped('empty_flask', [' R ', 'P P', ' P '], {'R': f'{NS}:rope', 'P': f'{NS}:palm_planks'}, f'{NS}:empty_flask')
shaped('campfire_island', [' S ', 'SFS', 'LLL'], {'S': 'minecraft:stick', 'F': f'{NS}:plant_fiber', 'L': {'tag': 'minecraft:logs'}}, 'minecraft:campfire')
shaped('thatch', ['FF', 'FF'], {'F': f'{NS}:palm_frond'}, f'{NS}:thatch', 2)
shaped('torch_island', ['F', 'S'], {'F': f'{NS}:palm_frond', 'S': 'minecraft:stick'}, 'minecraft:torch', 2)
shapeless('fish_skewer_cod', ['minecraft:stick', 'minecraft:cooked_cod', f'{NS}:roasted_taro'], f'{NS}:fish_skewer')
shapeless('fish_skewer_salmon', ['minecraft:stick', 'minecraft:cooked_salmon', f'{NS}:roasted_taro'], f'{NS}:fish_skewer')
shapeless('tribal_stew', ['minecraft:bowl', f'{NS}:roasted_taro', f'{NS}:bitterleaf', {'tag': 'minecraft:fishes'}], f'{NS}:tribal_stew')
shapeless('ghost_tonic', [f'{NS}:water_flask', f'{NS}:ghost_orchid', f'{NS}:bitterleaf'], f'{NS}:ghost_tonic')
shapeless('white_wool_from_blanket', [f'{NS}:airline_blanket'], 'minecraft:white_wool')
shaped('bed_island', ['BBB', 'PPP'], {'B': f'{NS}:airline_blanket', 'P': {'tag': 'minecraft:planks'}}, 'minecraft:white_bed')
cook('roasted_taro', f'{NS}:taro_root', f'{NS}:roasted_taro')
cook('water_flask', f'{NS}:sea_water_flask', f'{NS}:water_flask', 0.1)

# ---------------------------------------------------------------------------------------- world preset
flat = {'type': 'minecraft:flat', 'settings': {
    'biome': 'minecraft:warm_ocean', 'features': False, 'lakes': False, 'structure_overrides': [],
    'layers': [{'block': 'minecraft:bedrock', 'height': 1}, {'block': 'minecraft:stone', 'height': 110},
               {'block': 'minecraft:sand', 'height': 6}, {'block': 'minecraft:water', 'height': 10}]}}
w(f'{D}/worldgen/world_preset/story.json', {'dimensions': {'minecraft:overworld': {'type': 'minecraft:overworld', 'generator': flat}}})
print('data written', len(ALL_BLOCKS), 'blocks')
