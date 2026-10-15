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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import static com.airlinestory.world.Placer.facing;

/**
 * Meridian Flight 417: a narrow-body jet, nose to the west. 20 rows of 3-3 seating, forward galley and
 * cockpit, rear lavatories and galley. Built twice: parked at gate B7, and cruising above the clouds.
 */
public final class PlaneBuilder {
    private PlaneBuilder() {}

    public static final int LENGTH = 58;

    public static void build(Placer p, Plane pl, boolean atGate) {
        int X = pl.x(), Y = pl.y(), Z = pl.z();
        BlockState fus = ModBlocks.FUSELAGE.get().defaultBlockState();
        BlockState stripe = ModBlocks.FUSELAGE_STRIPE.get().defaultBlockState();
        BlockState window = ModBlocks.AIRPLANE_WINDOW.get().defaultBlockState();
        BlockState panel = ModBlocks.CABIN_PANEL.get().defaultBlockState();
        BlockState floor = ModBlocks.CABIN_FLOOR.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // ---------------- hull ----------------
        for (int dx = 2; dx <= LENGTH; dx++) {
            int x = X + dx;
            p.fill(x, Y - 1, Z - 3, x, Y - 1, Z + 3, fus);
            p.fill(x, Y, Z - 3, x, Y, Z + 3, floor);
            p.set(x, Y, Z - 4, fus);
            p.set(x, Y, Z + 4, fus);
            for (int y = Y + 1; y <= Y + 3; y++) {
                BlockState w = y == Y + 1 ? stripe : (y == Y + 2 && dx >= 11 && dx <= 51 && dx % 2 == 0) ? window : fus;
                p.set(x, y, Z - 4, w);
                p.set(x, y, Z + 4, w);
                for (int z = Z - 3; z <= Z + 3; z++) p.set(x, y, z, air);
            }
            p.set(x, Y + 4, Z - 4, fus);
            p.set(x, Y + 4, Z + 4, fus);
            p.fill(x, Y + 4, Z - 3, x, Y + 4, Z + 3, panel);
            p.fill(x, Y + 5, Z - 3, x, Y + 5, Z + 3, fus);
        }
        // nose
        p.fill(X + 1, Y - 1, Z - 3, X + 1, Y + 4, Z + 3, fus);
        p.fill(X, Y, Z - 2, X, Y + 3, Z + 2, fus);
        p.fill(X - 1, Y + 1, Z - 1, X - 1, Y + 2, Z + 1, fus);
        p.fill(X, Y + 2, Z - 2, X + 1, Y + 3, Z + 2, Blocks.BLACK_STAINED_GLASS.defaultBlockState());
        p.fill(X + 2, Y + 1, Z - 3, X + 3, Y + 3, Z + 3, air);
        p.fill(X + 1, Y + 1, Z - 2, X + 1, Y + 1, Z + 2, facing(ModBlocks.MONITOR.get(), Direction.EAST));
        p.set(X + 2, Y + 1, Z - 1, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.WEST));
        p.set(X + 2, Y + 1, Z + 1, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.WEST));
        // cockpit bulkhead and locked door
        p.fill(X + 4, Y + 1, Z - 3, X + 4, Y + 3, Z + 3, panel);
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST);
        p.set(X + 4, Y + 1, Z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        p.set(X + 4, Y + 2, Z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        p.sign(X + 5, Y + 3, Z - 1, Direction.EAST, DyeColor.WHITE, "FLIGHT DECK", "Crew only", "", "");
        // forward galley
        for (int dx = 5; dx <= 8; dx++) {
            p.set(X + dx, Y + 1, Z - 3, facing(ModBlocks.GALLEY_COUNTER.get(), Direction.SOUTH));
            p.set(X + dx, Y + 1, Z + 3, facing(ModBlocks.GALLEY_COUNTER.get(), Direction.NORTH));
            p.set(X + dx, Y + 3, Z - 3, facing(ModBlocks.OVERHEAD_BIN.get(), Direction.SOUTH));
            p.set(X + dx, Y + 3, Z + 3, facing(ModBlocks.OVERHEAD_BIN.get(), Direction.NORTH));
        }
        // boarding door (open at the gate, sealed in the air)
        if (atGate) {
            p.set(X + 9, Y + 1, Z - 4, air);
            p.set(X + 9, Y + 2, Z - 4, air);
        } else {
            p.set(X + 9, Y + 2, Z - 4, panel);
        }
        p.set(X + 9, Y + 2, Z + 4, panel);

        // ---------------- rows ----------------
        for (int row = 1; row <= Plane.ROWS; row++) {
            int dx = 12 + 2 * (row - 1);
            for (char c : Plane.LETTERS.toCharArray()) {
                BlockPos s = pl.seat(row, c);
                p.set(s, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.WEST));
            }
            for (int ddx = dx - 1; ddx <= dx; ddx++) {
                for (int dz : new int[]{-3, -2}) p.set(X + ddx, Y + 3, Z + dz, facing(ModBlocks.OVERHEAD_BIN.get(), Direction.SOUTH));
                for (int dz : new int[]{2, 3}) p.set(X + ddx, Y + 3, Z + dz, facing(ModBlocks.OVERHEAD_BIN.get(), Direction.NORTH));
            }
            p.sign(X + dx, Y + 3, Z - 1, Direction.SOUTH, DyeColor.WHITE, "ROW " + row, "A  B  C", "", "");
            p.sign(X + dx, Y + 3, Z + 1, Direction.NORTH, DyeColor.WHITE, "ROW " + row, "D  E  F", "", "");
        }
        p.sign(X + 10, Y + 3, Z - 1, Direction.SOUTH, DyeColor.LIGHT_BLUE, "Welcome", "aboard", "MERIDIAN", "");
        for (int dx = 11; dx <= 55; dx += 2) p.set(X + dx, Y + 4, Z, ModBlocks.CABIN_LIGHT.get().defaultBlockState());
        for (int dx : new int[]{13, 23, 33, 43}) p.set(X + dx, Y + 3, Z, facing(ModBlocks.SEATBELT_SIGN.get(), Direction.EAST));
        BlockPos call = callButton(pl);
        p.set(call, facing(ModBlocks.CALL_BUTTON.get(), Direction.SOUTH));

        // ---------------- rear: lavatories and galley ----------------
        for (int side : new int[]{-1, 1}) {
            int zIn1 = Z + side * 3, zIn2 = Z + side * 2, zWall = Z + side;
            for (int y = Y + 1; y <= Y + 3; y++) {
                p.set(X + 52, y, zIn1, panel);
                p.set(X + 52, y, zIn2, panel);
                p.set(X + 55, y, zIn1, panel);
                p.set(X + 55, y, zIn2, panel);
                for (int dx = 52; dx <= 55; dx++) p.set(X + dx, y, zWall, panel);
            }
        }
        p.set(X + 53, Y + 1, Z - 1, air);
        p.set(X + 53, Y + 2, Z - 1, air);
        p.set(X + 54, Y + 1, Z - 2, Blocks.SMOOTH_QUARTZ_SLAB);
        p.set(X + 54, Y + 2, Z - 3, facing(ModBlocks.LAVATORY_MIRROR.get(), Direction.SOUTH));
        p.set(X + 53, Y + 3, Z - 2, ModBlocks.CABIN_LIGHT.get().defaultBlockState());
        p.sign(X + 53, Y + 3, Z, Direction.WEST, DyeColor.WHITE, "LAVATORY", "", "", "");
        for (int dx = 56; dx <= 57; dx++) {
            p.set(X + dx, Y + 1, Z - 3, facing(ModBlocks.GALLEY_COUNTER.get(), Direction.SOUTH));
            p.set(X + dx, Y + 1, Z + 3, facing(ModBlocks.GALLEY_COUNTER.get(), Direction.NORTH));
        }
        // tail cone
        p.fill(X + LENGTH, Y, Z - 3, X + LENGTH, Y + 4, Z + 3, fus);
        p.fill(X + LENGTH + 1, Y + 1, Z - 2, X + LENGTH + 1, Y + 4, Z + 2, fus);
        p.fill(X + LENGTH + 2, Y + 2, Z - 1, X + LENGTH + 2, Y + 4, Z + 1, fus);
        for (int h = 0; h <= 9; h++) {
            for (int dx = 50 + h; dx <= LENGTH + 2; dx++) p.set(X + dx, Y + 6 + h, Z, stripe);
        }
        for (int s = 5; s <= 11; s++) {
            int from = 52 + (s - 5) / 2;
            for (int dx = from; dx <= LENGTH; dx++) {
                p.set(X + dx, Y + 3, Z - s, fus);
                p.set(X + dx, Y + 3, Z + s, fus);
            }
        }
        // wings and engines
        for (int s = 5; s <= 24; s++) {
            int from = 22 + (int) ((s - 5) * 0.5);
            int len = Math.max(3, 12 - (int) ((s - 5) * 0.45));
            for (int dx = from; dx < from + len; dx++) {
                p.set(X + dx, Y, Z - s, fus);
                p.set(X + dx, Y, Z + s, fus);
            }
        }
        for (int side : new int[]{-1, 1}) {
            int ez = Z + side * 11;
            p.set(X + 33, Y + 1, Z + side * 24, stripe);
            p.set(X + 34, Y + 1, Z + side * 24, stripe);
            p.set(X + 34, Y + 2, Z + side * 24, stripe);
            for (int dx = 19; dx <= 26; dx++) {
                for (int y = Y - 3; y <= Y - 1; y++) {
                    for (int z = ez - 1; z <= ez + 1; z++) {
                        boolean core = y == Y - 2 && z == ez;
                        BlockState b = dx == 19 ? (core ? Blocks.GRAY_CONCRETE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState())
                                : core ? Blocks.GRAY_CONCRETE.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                        p.set(X + dx, y, z, b);
                    }
                }
            }
        }
        if (atGate) {
            for (int[] g : new int[][]{{6, 0}, {28, -2}, {28, 2}, {30, -2}, {30, 2}}) {
                p.set(X + g[0], Y - 2, Z + g[1], Blocks.GRAY_CONCRETE);
                p.set(X + g[0], Y - 3, Z + g[1], Blocks.BLACK_WOOL);
            }
        }
    }

    public static BlockPos callButton(Plane pl) {
        BlockPos s = pl.seat(14, 'A');
        return new BlockPos(s.getX() - 1, pl.y() + 2, s.getZ());
    }

    /** 0 = off, 1 = on. */
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

    /** Seat-back screens: 0 = logo, 1 = live feed of you, 2 = static. */
    public static void screens(Placer p, Plane pl, int mode) {
        for (int row = 1; row <= Plane.ROWS; row++) {
            for (char c : Plane.LETTERS.toCharArray()) {
                BlockPos s = pl.seat(row, c);
                BlockState st = p.get(s.getX(), s.getY(), s.getZ());
                if (st.hasProperty(SeatBlock.SCREEN)) p.set(s, st.setValue(SeatBlock.SCREEN, mode));
            }
        }
    }

    public static void masks(Placer p, Plane pl) {
        for (int row = 1; row <= Plane.ROWS; row++) {
            for (char c : Plane.LETTERS.toCharArray()) {
                BlockPos s = pl.seat(row, c);
                BlockPos m = new BlockPos(s.getX() - 1, pl.y() + 2, s.getZ());
                if (p.get(m.getX(), m.getY(), m.getZ()).isAir()) p.set(m, facing(ModBlocks.OXYGEN_MASK.get(), Direction.WEST));
            }
        }
    }
}
