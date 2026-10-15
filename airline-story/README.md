# Airline Story: Flight 417 (Forge 1.20.1)

A single-player psychological horror story mod. You check in at Meridian International, clear
security, board Meridian Air Flight 417 to Port Ellery, and take seat 14A. Somewhere over the ocean,
things start to go wrong. Then you wake up on an island.

Everyone is rated. Everyone is watching.

## Install

1. Install **Minecraft Forge 1.20.1** (47.x).
2. Put `airlinestory-forge-1.20.1-1.0.0.jar` in your `mods` folder (CI builds it into `ci-report/`;
   or build it yourself, see below).
3. Start the game. The title screen's **Singleplayer** button now reads **Start Airline Story**.

The story only runs in its own single-player world. **Start Airline Story** lets you continue the
story, start over, or reach your other single-player worlds. On a server or in any other world the
mod's blocks and items exist, but the story does not run.

## Controls

| Key / action | What it does |
| --- | --- |
| **J** | Travel Journal: current chapter, objectives, *who to talk to*, *where to go* (distance and direction), *what to do*, the story so far, what people remember about you, and your SmileScore |
| **K** | Show/hide the on-screen objective tracker |
| Right-click a person | Talk. Answer with **1-4** or the mouse. Some answers are timed: wait too long and silence is your answer |
| Right-click a seat | Sit down. **Sneak** to stand up. Right-click the back of an airplane seat to watch its screen |
| Right-click story objects | Departure boards, the X-ray belt, the boarding podium, overhead bins, the call button, suitcases, the boat frame... |
| Hold **Space** in a cutscene | Skip it |

## What's in it

- **Twelve chapters, 33 quests**, from the check-in desk to the last night on the island. Several
  hours of play, most of it on the island.
- **A whole airport**: check-in hall with desks and queues, departures boards, a security hall with
  X-ray belts and body scanners, Cloud Nine Coffee, duty free, gates B5-B7, and a jet bridge to the
  plane. Travellers wander, queue, wait at gates and board the plane with you.
- **The flight**: 20 rows of seats you can sit in, overhead bins, galleys, a lavatory, seatbelt
  signs, seat-back screens, a cockpit door that shouldn't be locked, and turbulence you'll feel in
  the camera. Stand up during the storm and you'll be thrown around the cabin.
- **Conversations in the style of choice-driven adventure games**: letterboxed scenes, typed-out
  lines, four answers, timed choices, and "*X will remember that.*" Over 40 conversation trees.
- **SmileScore**: every answer is rated. Your score decides how you board, what HALO says to you,
  and which ending you get.
- **Cutscenes**: arrival, take-off, the stare, the storm, the crash, the lab and the escape.
- **The island**: generated around the wreck: beaches, palm groves, jungle, a freshwater spring,
  a mountain, a Tidefolk village with a dock, the Hollow Grove, and a grey building on the summit.
- **The Tidefolk**: Elder Ahu, Nalu, Mira, Teo, Pip and their people, each with their own quests.
- **The Unrated**: faceless figures that only move while nobody is looking at them.
- **Survival**: coconuts, taro, fish, fresh water (boil seawater, or fill a flask at the spring),
  plant fiber, rope, palm wood, salvaged hull panels, flares, a machete, ghost tonic.
- **Build your way out**: five stages of a boat at Teo's dock (keel, hull, mast, sail, provisions).
- **Three endings.**

Over 50 blocks, 34 items, over 30 characters with hand-made skins, all textures 16x16 in the vanilla style.

## Build

```
cd airline-story
./gradlew build
```

The jar is `build/libs/airlinestory-forge-1.20.1-1.0.0.jar`. Textures, models and data are generated
by `tools/gen_blocks.py`, `tools/gen_items.py`, `tools/gen_skins.py` and `tools/gen_data.py`
(Python 3 + Pillow); the generated files are committed, so you only need these to change the art.

`./gradlew runClient -Pautotest=true` plays an automated tour of the story (used by CI to take
screenshots into `run/screenshots`).

Cheat commands for testing (need cheats enabled, e.g. *Open to LAN* with cheats on):
`/story status`, `/story skip`, `/story goto <quest>`, `/story flag <flag>`.
