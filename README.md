# Drunkard 1.6.1 patch

`dist/drunkard-forge-1.20.1-1.6.1.jar` is a drop-in replacement for `drunkard-forge-1.20.1-1.6.0.jar` (Forge 1.20.1).

## Changes
- **Bartenders no longer stand in the same block.** Each one reserves the spot it's standing on or walking to, so coworkers pick a different spot at taps, shelves, customers and the worker home spot. They also path around each other and get nudged apart if they ever touch.
- **More natural bartender walking.** They cut corners on open floor instead of following the block grid, turn gradually, slow down while turning, ease in and out of a stop, look where they're going and each walk at a slightly different pace.
- **Door / seat linking no longer blocks placing blocks.** Clicking something that isn't a door (or seat) now passes through normally, so you can place the door and then click it. Iron doors are now rejected (customers can't open them) instead of linked anyway.
- **Wort no longer needs glass bottles.** The brewing kettle outputs wort without bottles. A fermentation barrel doesn't hand back an empty bottle for wort, so you can't get free bottles. JEI is updated to match.

## Layout
- `src/` – the decompiled sources of the classes that changed, with the edits applied (`original-src/` is the untouched decompile, for diffing).
- `tools/` – rebuild toolchain: `tools/build.sh <original 1.6.0 jar>` makes compile-only stubs for the Minecraft/Forge classes the mod uses, recompiles `src/`, applies small bytecode patches (JEI display, one game test), and repacks the jar.
