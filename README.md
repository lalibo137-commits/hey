# Drunkard 1.2.0 patch

`dist/drunkard-forge-1.20.1-1.2.0.jar` replaces `drunkard-forge-1.20.1-1.6.0.jar` (Forge 1.20.1). As requested, the version number is now 1.2.0. That is lower than 1.6.x, so any update checker will see it as older.

## 1.2.0
- **Barkeeps restock glasses.** When no one is waiting, a barkeep takes mugs and other glassware returned to the register and carries them to a glass shelf with space. A new customer interrupts the trip.
- **Registers keep their pub when broken.** The dropped register item carries the pub profile: upgrades, reputation, regulars, stock list, capacity, customer style and the day's ledger. Its tooltip shows what is saved. Place it anywhere to reopen that pub; only the door and seats need setting again. Cash in the till and dishes still drop as items, so nothing is duplicated.
- **Safes stay with their own pub.** A safe remembers which register it belongs to, so a second pub nearby can't take it over. Upgrades always belonged to the register and a safe only shows its register's upgrades; they now survive breaking the register. Breaking a safe now also keeps its supplier deal (before, only the money and owner were kept).
- **Customer style: Modern / Medieval.** There's a new button on the register's Pub tab. It can only be changed while the store is closed and empty.
  - *Modern* is the existing customers.
  - *Medieval* dresses the Steves and Alexes in period clothes: tunics, leather jerkins over linen shirts, buttoned doublets, laced kirtles and bodices, aprons, cloaks, hoods and coifs, hose and leather boots. Barkeeps wear a linen shirt with rolled sleeves and a brown apron, and robbers wear dark hooded cloaks. Customers and barkeeps speak like medieval folk ("Good morrow!", "Wassail!", "Prithee", "thou art", coins instead of dollars), including in the conversation screen. See `medieval-preview.png`.

## 1.6.1 (still included)
- Barkeeps don't stand in the same block, and they walk more naturally.
- Door and seat linking no longer stops you placing blocks.
- Wort no longer needs glass bottles.

## NeoForge 1.21.1
`dist/drunkard-neoforge-1.21.1-1.2.0.jar` replaces `drunkard-neoforge-1.21.1-1.1.0.jar`. It adds everything above that the NeoForge build was missing:
- The whole pub: cash register (store, stock, seats, pub and staff tabs; customer style), customers (ordering, seating, drinking, groups, friends, conversations, brawls, robbers, regulars), barkeeps, the safe (money, upgrades, supplies, wages), the glass shelf, cash, door and seat linking, medieval customers and dialogue, the screens and renderers, recipes, loot tables and tags.
- The 1.6.1 and 1.2.0 changes: barkeep spacing and movement, linking that doesn't block block placement, wort without bottles (kettle, barrel and JEI), mugs restocked onto glass shelves, register pub profiles, safes that stay with their register.
- Forge-only seating: stairs, slabs and closed trapdoors work as seats, and sitting on a stool or chair needs an empty hand. Glasses and empty bottles can be set down like bottles, and pipes show when they're lit.
- The 52 pub game tests, in `PubGameTests`. Two NeoForge game tests now expect the kettle and barrel to stop using or returning bottles for wort.

On 1.21.1 the register's pub profile and the safe's money, owner and supplier deal travel on the dropped item as custom data, like the keg's beer. Seated customers sit at the same height as before.

`neoforge/` holds the port: `src/` (pub classes plus the changed NeoForge classes; the untouched NeoForge versions are in `original-src/`), `resources/` (overlay) and `tools/build.sh <original 1.1.0 jar>`, which rebuilds the jar and runs the bytecode, interface and link checks. `tools/verify/` holds the checks that also need the Minecraft mapping data (MCPConfig, Parchment, Yarn): API references, override signatures and renamed members.

## Layout
- `src/` holds the decompiled sources of every changed class, with the edits applied. `original-src/` holds the untouched decompiles for diffing.
- `resources/` holds the data files that override the jar's (loot tables).
- `tools/build.sh <original 1.6.0 jar>` rebuilds `dist/`. It makes compile-only stubs for the Minecraft/Forge classes the mod uses, recompiles `src/`, applies small bytecode patches (JEI display, one game test), checks every obfuscated name, overlays `resources/` and repacks the jar.
