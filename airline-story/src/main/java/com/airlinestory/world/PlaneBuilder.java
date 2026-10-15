package com.airlinestory.world;

import com.airlinestory.block.LightBlock;
import com.airlinestory.block.SeatBlock;
import com.airlinestory.block.StateInteractBlock;
import com.airlinestory.registry.ModBlocks;
import com.airlinestory.story.Locations.Plane;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import static com.airlinestory.world.Placer.*;

/**
 * Meridian Flight 417: a narrow-body jet, nose to the west. Cargo hold under the cabin floor, 20 rows of
 * 3-3 seating, galleys fore and aft, cockpit and lavatory. Built three times: parked at gate B7, cruising
 * above the clouds, and (exterior only) taxiing at the far end of the airfield.
 */
public final class PlaneBuilder {
    private PlaneBuilder() {}

    public static final int LENGTH = 58;

    public static void exterior(Placer p, Plane pl) {
        hull(p, pl, false, true);
        wingsAndTail(p, pl, true);
    }

    public static void build(Placer p, Plane pl, boolean atGate) {
        hull(p, pl, atGate, false);
        wingsAndTail(p, pl, atGate);
        cabin(p, pl, atGate);
        hold(p, pl, !atGate);
        p.finish();
    }

    private static void hull(Placer p, Plane pl, boolean atGate, boolean shellOnly) {
        int X = pl.x(), Y = pl.y(), Z = pl.z();
        BlockState fus = ModBlocks.FUSELAGE.get().defaultBlockState();
        BlockState stripe = ModBlocks.FUSELAGE_STRIPE.get().defaultBlockState();
        BlockState window = ModBlocks.AIRPLANE_WINDOW.get().defaultBlockState();
        BlockState panel = ModBlocks.CABIN_PANEL.get().defaultBlockState();
        BlockState floor = ModBlocks.CABIN_FLOOR.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = 2; dx <= LENGTH; dx++) {
            int x = X + dx;
            p.fill(x, Y - 4, Z - 2, x, Y - 4, Z + 2, fus);
            for (int y = Y - 3; y <= Y - 1; y++) {
                p.set(x, y, Z - 3, fus);
                p.set(x, y, Z + 3, fus);
                for (int z = Z - 2; z <= Z + 2; z++) p.set(x, y, z, shellOnly ? fus : air);
            }
            p.set(x, Y - 1, Z - 4, fus);
            p.set(x, Y - 1, Z + 4, fus);
            p.fill(x, Y, Z - 3, x, Y, Z + 3, shellOnly ? fus : floor);
            p.set(x, Y, Z - 4, fus);
            p.set(x, Y, Z + 4, fus);
            for (int y = Y + 1; y <= Y + 3; y++) {
                BlockState w = y == Y + 1 ? stripe : (y == Y + 2 && dx >= 11 && dx <= 51 && dx % 2 == 0) ? window : fus;
                p.set(x, y, Z - 4, w);
                p.set(x, y, Z + 4, w);
                for (int z = Z - 3; z <= Z + 3; z++) p.set(x, y, z, shellOnly ? fus : air);
            }
            p.set(x, Y + 4, Z - 4, fus);
            p.set(x, Y + 4, Z + 4, fus);
            p.fill(x, Y + 4, Z - 3, x, Y + 4, Z + 3, shellOnly ? fus : panel);
            p.fill(x, Y + 5, Z - 3, x, Y + 5, Z + 3, fus);
        }
        // nose cone
        p.fill(X + 1, Y - 3, Z - 3, X + 1, Y + 4, Z + 3, fus);
        p.fill(X, Y - 2, Z - 2, X, Y + 3, Z + 2, fus);
        p.fill(X - 1, Y - 1, Z - 1, X - 1, Y + 2, Z + 1, fus);
        p.fill(X - 2, Y, Z, X - 2, Y + 1, Z, fus);
        p.fill(X, Y + 2, Z - 2, X + 1, Y + 3, Z + 2, Blocks.BLACK_STAINED_GLASS.defaultBlockState());
        p.fill(X - 1, Y + 2, Z - 1, X - 1, Y + 2, Z + 1, Blocks.BLACK_STAINED_GLASS.defaultBlockState());
        // tail cone
        p.fill(X + LENGTH, Y - 3, Z - 3, X + LENGTH, Y + 4, Z + 3, fus);
        p.fill(X + LENGTH + 1, Y - 1, Z - 2, X + LENGTH + 1, Y + 4, Z + 2, fus);
        p.fill(X + LENGTH + 2, Y + 1, Z - 1, X + LENGTH + 2, Y + 4, Z + 1, fus);
        p.fill(X + LENGTH + 3, Y + 3, Z, X + LENGTH + 3, Y + 4, Z, fus);
        // doors outlined on the outside
        for (int y = Y + 1; y <= Y + 2; y++) {
            if (!atGate || shellOnly) p.set(X + 9, y, Z - 4, panel);
            p.set(X + 9, y, Z + 4, panel);
        }
        if (atGate && !shellOnly) {
            p.set(X + 9, Y + 1, Z - 4, air);
            p.set(X + 9, Y + 2, Z - 4, air);
        }
        p.sign(X + 9, Y + 3, Z - 5, Direction.NORTH, DyeColor.LIGHT_BLUE, "MERIDIAN", "", "", "");
        for (int dx = 14; dx <= 30; dx += 4) p.set(X + dx, Y + 3, Z - 4, stripe);
    }

    private static void wingsAndTail(Placer p, Plane pl, boolean gear) {
        int X = pl.x(), Y = pl.y(), Z = pl.z();
        BlockState fus = ModBlocks.FUSELAGE.get().defaultBlockState();
        BlockState stripe = ModBlocks.FUSELAGE_STRIPE.get().defaultBlockState();
        // fin
        for (int h = 0; h <= 10; h++) {
            for (int dx = 48 + h; dx <= LENGTH + 2; dx++) p.set(X + dx, Y + 6 + h, Z, h % 4 == 2 ? fus : stripe);
        }
        p.set(X + LENGTH + 1, Y + 14, Z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
        // stabilisers
        for (int s = 5; s <= 12; s++) {
            int from = 50 + (s - 5) / 2;
            for (int dx = from; dx <= LENGTH; dx++) {
                p.set(X + dx, Y + 3, Z - s, fus);
                p.set(X + dx, Y + 3, Z + s, fus);
            }
        }
        // wings, swept back, slightly anhedral at the root
        for (int s = 5; s <= 26; s++) {
            int from = 20 + (int) ((s - 5) * 0.55);
            int len = Math.max(3, 13 - (int) ((s - 5) * 0.48));
            int y = Y - 1 + (s > 15 ? 1 : 0);
            for (int dx = from; dx < from + len; dx++) {
                BlockState b = dx == from ? stripe : fus;
                p.set(X + dx, y, Z - s, b);
                p.set(X + dx, y, Z + s, b);
            }
        }
        for (int side : new int[]{-1, 1}) {
            p.set(X + 32, Y, Z + side * 26, stripe);
            p.set(X + 32, Y + 1, Z + side * 26, stripe);
            p.set(X + 31, Y, Z + side * 26, Blocks.RED_CONCRETE);
            // engines
            int ez = Z + side * 11;
            for (int dx = 17; dx <= 25; dx++)
                for (int y = Y - 4; y <= Y - 2; y++)
                    for (int z = ez - 1; z <= ez + 1; z++) {
                        boolean core = y == Y - 3 && z == ez;
                        BlockState b = dx == 17 ? (core ? Blocks.GRAY_CONCRETE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState())
                                : dx == 25 && core ? Blocks.BLACK_CONCRETE.defaultBlockState()
                                : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                        p.set(X + dx, y, z, b);
                    }
            p.set(X + 21, Y - 1, ez, Blocks.LIGHT_GRAY_CONCRETE);
        }
        if (gear) {
            for (int[] g : new int[][]{{8, 0}, {28, -2}, {28, 2}, {29, -2}, {29, 2}}) {
                p.set(X + g[0], Y - 5, Z + g[1], Blocks.GRAY_CONCRETE);
                p.set(X + g[0], Y - 6, Z + g[1], Blocks.BLACK_WOOL);
            }
        }
    }

    private static void cabin(Placer p, Plane pl, boolean atGate) {
        int X = pl.x(), Y = pl.y(), Z = pl.z();
        BlockState panel = ModBlocks.CABIN_PANEL.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // cockpit
        p.fill(X + 2, Y + 1, Z - 3, X + 3, Y + 3, Z + 3, air);
        p.fill(X + 2, Y, Z - 2, X + 3, Y, Z + 2, Blocks.GRAY_CONCRETE);
        p.fill(X + 1, Y + 1, Z - 2, X + 1, Y + 1, Z + 2, Blocks.POLISHED_BLACKSTONE);
        for (int z = Z - 2; z <= Z + 2; z++) {
            p.set(X + 1, Y + 2, z, facing(ModBlocks.MONITOR.get(), Direction.EAST).setValue(StateInteractBlock.MODE, z == Z ? 2 : 0));
        }
        p.set(X + 2, Y + 1, Z - 1, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.WEST));
        p.set(X + 2, Y + 1, Z + 1, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.WEST));
        p.set(X + 2, Y + 1, Z, Blocks.POLISHED_BLACKSTONE);
        p.set(X + 2, Y + 2, Z, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.WEST));
        p.set(X + 3, Y + 3, Z - 3, Blocks.REDSTONE_LAMP);
        p.set(X + 3, Y + 4, Z, Blocks.SEA_LANTERN);
        // cockpit bulkhead and its locked door
        p.fill(X + 4, Y + 1, Z - 3, X + 4, Y + 3, Z + 3, panel);
        p.door(X + 4, Y + 1, Z, Blocks.IRON_DOOR, Direction.EAST, false);
        p.sign(X + 5, Y + 3, Z - 1, Direction.EAST, DyeColor.WHITE, "FLIGHT DECK", "Crew only", "", "");
        // forward galley
        for (int dx = 5; dx <= 8; dx++) {
            p.set(X + dx, Y + 1, Z + 3, facing(ModBlocks.GALLEY_COUNTER.get(), Direction.NORTH));
            p.set(X + dx, Y + 3, Z + 3, facing(ModBlocks.OVERHEAD_BIN.get(), Direction.NORTH));
            p.set(X + dx, Y + 3, Z - 3, facing(ModBlocks.OVERHEAD_BIN.get(), Direction.SOUTH));
        }
        p.set(X + 5, Y + 2, Z + 3, facing(Blocks.SMOKER, Direction.NORTH));
        p.set(X + 6, Y + 2, Z + 3, Blocks.BREWING_STAND);
        p.set(X + 7, Y + 2, Z + 3, facing(Blocks.FURNACE, Direction.NORTH));
        p.set(X + 5, Y + 1, Z - 3, Blocks.IRON_BLOCK);
        p.set(X + 6, Y + 1, Z - 3, trapdoor(Blocks.IRON_TRAPDOOR, Direction.SOUTH, true, true));
        p.set(X + 7, Y + 1, Z - 3, facing(ModBlocks.GALLEY_COUNTER.get(), Direction.SOUTH));
        p.set(X + 8, Y + 1, Z - 3, facing(ModBlocks.GALLEY_COUNTER.get(), Direction.SOUTH));
        p.sign(X + 10, Y + 3, Z - 3, Direction.EAST, DyeColor.LIME, "EXIT", "←", "", "");
        p.sign(X + 10, Y + 3, Z + 3, Direction.EAST, DyeColor.LIME, "EXIT", "←", "", "");
        p.sign(X + 10, Y + 3, Z - 1, Direction.EAST, DyeColor.LIGHT_BLUE, "Welcome", "aboard", "MERIDIAN", "");
        // rows
        for (int row = 1; row <= Plane.ROWS; row++) {
            int dx = 12 + 2 * (row - 1);
            for (char c : Plane.LETTERS.toCharArray()) p.set(pl.seat(row, c), facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.WEST));
            for (int ddx = dx - 1; ddx <= dx; ddx++) {
                for (int dz : new int[]{-3, -2}) p.set(X + ddx, Y + 3, Z + dz, facing(ModBlocks.OVERHEAD_BIN.get(), Direction.SOUTH));
                for (int dz : new int[]{2, 3}) p.set(X + ddx, Y + 3, Z + dz, facing(ModBlocks.OVERHEAD_BIN.get(), Direction.NORTH));
            }
            p.sign(X + dx, Y + 3, Z - 1, Direction.SOUTH, DyeColor.WHITE, "ROW " + row, "A  B  C", "", "");
            p.sign(X + dx, Y + 3, Z + 1, Direction.NORTH, DyeColor.WHITE, "ROW " + row, "D  E  F", "", "");
        }
        for (int dx = 11; dx <= 55; dx += 2) p.set(X + dx, Y + 4, Z, ModBlocks.CABIN_LIGHT.get().defaultBlockState());
        for (int dx = 11; dx <= 51; dx += 2) {
            p.set(X + dx, Y + 4, Z - 1, stair(Blocks.QUARTZ_STAIRS, Direction.NORTH, true));
            p.set(X + dx, Y + 4, Z + 1, stair(Blocks.QUARTZ_STAIRS, Direction.SOUTH, true));
        }
        for (int dx : new int[]{13, 23, 33, 43}) p.set(X + dx, Y + 3, Z, facing(ModBlocks.SEATBELT_SIGN.get(), Direction.EAST));
        p.set(callButton(pl), facing(ModBlocks.CALL_BUTTON.get(), Direction.SOUTH));
        // rear: lavatories, galley and the hatch to the hold
        for (int side : new int[]{-1, 1}) {
            for (int y = Y + 1; y <= Y + 3; y++) {
                p.set(X + 52, y, Z + side * 3, panel);
                p.set(X + 52, y, Z + side * 2, panel);
                p.set(X + 55, y, Z + side * 3, panel);
                p.set(X + 55, y, Z + side * 2, panel);
                for (int dx = 52; dx <= 55; dx++) p.set(X + dx, y, Z + side, panel);
            }
        }
        p.door(X + 53, Y + 1, Z - 1, Blocks.BIRCH_DOOR, Direction.SOUTH, false);
        p.set(X + 54, Y + 1, Z - 2, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
        p.set(X + 53, Y + 1, Z - 3, Blocks.CAULDRON);
        p.set(X + 54, Y + 2, Z - 3, facing(ModBlocks.LAVATORY_MIRROR.get(), Direction.SOUTH));
        p.set(X + 53, Y + 3, Z - 2, ModBlocks.CABIN_LIGHT.get().defaultBlockState());
        p.sign(X + 54, Y + 3, Z, Direction.WEST, DyeColor.WHITE, "LAVATORY", "○ vacant", "", "");
        for (int dx = 56; dx <= 57; dx++) {
            p.set(X + dx, Y + 1, Z - 3, facing(ModBlocks.GALLEY_COUNTER.get(), Direction.SOUTH));
            p.set(X + dx, Y + 1, Z + 3, facing(ModBlocks.GALLEY_COUNTER.get(), Direction.NORTH));
        }
        p.set(X + 56, Y + 2, Z + 3, Blocks.BREWING_STAND);
        BlockPos hatch = hatch(pl);
        p.set(hatch, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH, true, false));
        p.sign(X + 57, Y + 2, Z - 2, Direction.EAST, DyeColor.RED, "CREW ONLY", "hold access", "↓", "");
    }

    /** The trapdoor in the rear galley floor that leads down to the cargo hold. */
    public static BlockPos hatch(Plane pl) {
        return new BlockPos(pl.x() + 57, pl.y(), pl.z() + 2);
    }

    private static void hold(Placer p, Plane pl, boolean clues) {
        int X = pl.x(), Y = pl.y(), Z = pl.z();
        BlockPos h = hatch(pl);
        for (int y = Y - 3; y <= Y - 1; y++) p.set(h.getX(), y, h.getZ(), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        // walls of suitcases, all with the same tag
        for (int dx = 4; dx <= 54; dx++) {
            if (dx % 8 == 0) continue;
            p.set(X + dx, Y - 3, Z - 2, facing(ModBlocks.SUITCASE.get(), Direction.SOUTH));
            if (dx % 3 != 0) p.set(X + dx, Y - 2, Z - 2, facing(ModBlocks.SUITCASE.get(), Direction.SOUTH));
            if (dx < 40 && dx % 5 != 0) p.set(X + dx, Y - 3, Z + 2, facing(ModBlocks.SUITCASE.get(), Direction.NORTH));
        }
        for (int dx = 8; dx <= 54; dx += 8) p.set(X + dx, Y - 1, Z, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        // crates
        for (int dx = 42; dx <= 52; dx += 2) {
            p.set(X + dx, Y - 3, Z + 2, Blocks.BARREL);
            p.set(X + dx, Y - 2, Z + 2, Blocks.BARREL);
        }
        p.sign(X + 46, Y - 2, Z + 1, Direction.NORTH, DyeColor.BLACK, "ISLAND SET", "MODULAR", "FRAGILE", "");
        p.sign(X + 26, Y - 2, Z + 1, Direction.NORTH, DyeColor.BLACK, "WEATHER RIG", "STORM", "x4", "");
        p.fill(X + 24, Y - 3, Z + 2, X + 28, Y - 2, Z + 2, Blocks.IRON_BLOCK);
        p.set(X + 25, Y - 3, Z + 1, facing(Blocks.DISPENSER, Direction.NORTH));
        p.set(X + 27, Y - 3, Z + 1, facing(Blocks.DISPENSER, Direction.NORTH));
        // the stand-in, strapped into a seat
        p.set(X + 14, Y - 3, Z, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.WEST));
        p.mannequin(X + 14.5, Y - 2.6, Z + 0.5, 90, 0x4A6A3A, true);
        if (clues) {
            p.clue(X + 13, Y - 3, Z + 1, ModBlocks.CLUE_PHOTO.get(), "seat_14a_dummy");
            p.clue(X + 47, Y - 3, Z, ModBlocks.CLUE_NOTE.get(), "cargo_manifest");
            p.clue(X + 8, Y + 2, Z - 3, ModBlocks.CLUE_PHOTO.get(), "june_badge");
        }
    }

    public static BlockPos callButton(Plane pl) {
        BlockPos s = pl.seat(14, 'A');
        return new BlockPos(s.getX() - 1, pl.y() + 2, s.getZ());
    }

    public static void lights(Placer p, Plane pl, boolean on) {
        for (int dx = 11; dx <= 55; dx += 2) setLit(p, pl.x() + dx, pl.y() + 4, pl.z(), on);
        setLit(p, pl.x() + 53, pl.y() + 3, pl.z() - 2, on);
    }

    private static void setLit(Placer p, int x, int y, int z, boolean on) {
        BlockState s = p.get(x, y, z);
        if (s.hasProperty(LightBlock.LIT) && s.getValue(LightBlock.LIT) != on) p.set(x, y, z, s.setValue(LightBlock.LIT, on));
    }

    public static void seatbelt(Placer p, Plane pl, boolean on) {
        for (int dx : new int[]{13, 23, 33, 43}) {
            BlockState s = p.get(pl.x() + dx, pl.y() + 3, pl.z());
            if (s.hasProperty(StateInteractBlock.LIT)) p.set(pl.x() + dx, pl.y() + 3, pl.z(), s.setValue(StateInteractBlock.LIT, on));
        }
    }

    public static void screens(Placer p, Plane pl, int mode) {
        for (int row = 1; row <= Plane.ROWS; row++)
            for (char c : Plane.LETTERS.toCharArray()) {
                BlockPos s = pl.seat(row, c);
                BlockState st = p.get(s.getX(), s.getY(), s.getZ());
                if (st.hasProperty(SeatBlock.SCREEN)) p.set(s, st.setValue(SeatBlock.SCREEN, mode));
            }
    }

    public static void masks(Placer p, Plane pl) {
        for (int row = 1; row <= Plane.ROWS; row++)
            for (char c : Plane.LETTERS.toCharArray()) {
                BlockPos s = pl.seat(row, c);
                BlockPos m = new BlockPos(s.getX() - 1, pl.y() + 2, s.getZ());
                if (p.get(m.getX(), m.getY(), m.getZ()).isAir()) p.set(m, facing(ModBlocks.OXYGEN_MASK.get(), Direction.WEST));
            }
    }
}
