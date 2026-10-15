package com.airlinestory.world;

import com.airlinestory.block.StateInteractBlock;
import com.airlinestory.registry.ModBlocks;
import com.airlinestory.story.Locations;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import static com.airlinestory.world.Placer.facing;

/**
 * Meridian International: a long terminal running west to east. Landside hall with check-in, then the
 * security hall, then airside (cafe, duty free) and the concourse with gates B5, B6 and B7 on the south
 * windows. Flight 417 is parked at B7 with a jet bridge.
 */
public final class AirportBuilder {
    private AirportBuilder() {}

    public static final int F = Locations.FLOOR; // 63
    public static final int CEIL = 73;

    public static void build(Placer p) {
        BlockState concrete = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState lgray = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        BlockState wall = ModBlocks.TERMINAL_WALL.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // ---------------- ground ----------------
        p.fill(-24, F, -24, 214, F, 124, concrete);
        p.fill(-24, F, -18, 214, F, -6, Blocks.BLACK_CONCRETE.defaultBlockState());
        for (int x = -24; x <= 214; x += 4) p.fill(x, F, -12, x + 1, F, -12, white);
        p.fill(-24, F, -5, 140, F, -1, Blocks.SMOOTH_STONE.defaultBlockState());
        for (int x = -10; x <= 130; x += 14) lamp(p, x, -3);
        for (int x = 70; x <= 200; x += 2) p.set(x, F, 82, Blocks.YELLOW_CONCRETE);
        for (int z = 48; z <= 80; z += 2) p.set(160, F, z, Blocks.YELLOW_CONCRETE);
        controlTower(p);

        // ---------------- shell ----------------
        p.fill(1, F, 1, 126, F, 46, ModBlocks.TERMINAL_TILE.get());
        p.fill(81, F, 20, 126, F, 46, ModBlocks.TERMINAL_CARPET.get());
        p.fill(41, F, 17, 55, F, 30, Blocks.GRAY_WOOL);
        for (int x = 1; x <= 126; x++) {
            for (int z = 1; z <= 46; z++) {
                boolean light = x % 5 == 2 && z % 5 == 2;
                p.set(x, CEIL, z, light ? ModBlocks.CEILING_LIGHT.get().defaultBlockState() : ModBlocks.CEILING_PANEL.get().defaultBlockState());
            }
        }
        p.fill(0, CEIL + 1, 0, 127, CEIL + 1, 47, lgray);
        for (int x = 0; x <= 127; x++) {
            for (int y = F + 1; y <= CEIL; y++) {
                boolean pillar = x % 6 == 0 || y >= CEIL - 1;
                p.set(x, y, 0, pillar ? white : glass);
                p.set(x, y, 47, pillar ? white : glass);
            }
        }
        for (int z = 0; z <= 47; z++) {
            for (int y = F + 1; y <= CEIL; y++) {
                boolean window = z % 6 != 0 && y >= F + 3 && y <= F + 6;
                p.set(0, y, z, window ? glass : wall);
                p.set(127, y, z, window ? glass : wall);
            }
        }
        // entrance
        p.clear(16, F + 1, 0, 23, F + 4, 0);
        p.fill(14, F + 5, 0, 25, F + 7, 0, white);
        for (int x = 17; x <= 22; x++) p.set(x, F + 8, 0, facing(ModBlocks.MERIDIAN_LOGO.get(), Direction.NORTH));
        p.fill(17, F + 8, 1, 22, F + 8, 1, white);
        p.sign(19, F + 6, -1, Direction.NORTH, DyeColor.WHITE, "", "DEPARTURES", "Meridian Intl.", "");
        p.sign(20, F + 6, -1, Direction.NORTH, DyeColor.LIGHT_BLUE, "", "Have a nice", "flight!", "");
        p.fill(15, F, -1, 24, F, -1, Blocks.SMOOTH_STONE.defaultBlockState());

        landside(p, wall, white);
        security(p, wall, white);
        airside(p, white);
        jetBridge(p, lgray, glass, air);
    }

    private static void lamp(Placer p, int x, int z) {
        p.fill(x, F + 1, z, x, F + 4, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
        p.set(x, F + 5, z, Blocks.SEA_LANTERN);
    }

    private static void controlTower(Placer p) {
        BlockState c = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        p.fill(-18, F + 1, 52, -14, F + 26, 56, c);
        p.fill(-20, F + 27, 50, -12, F + 27, 58, c);
        p.walls(-20, F + 28, 50, -12, F + 30, 58, Blocks.GRAY_STAINED_GLASS.defaultBlockState());
        p.fill(-21, F + 31, 49, -11, F + 31, 59, Blocks.WHITE_CONCRETE.defaultBlockState());
        p.set(-16, F + 32, 54, Blocks.IRON_BARS);
        p.set(-16, F + 33, 54, Blocks.IRON_BARS);
        p.set(-16, F + 34, 54, Blocks.REDSTONE_BLOCK);
    }

    private static void landside(Placer p, BlockState wall, BlockState white) {
        // Check-in counters and back wall
        p.fill(1, F + 1, 35, 39, CEIL - 1, 35, wall);
        for (int x = 13; x <= 19; x++)
            for (int y = F + 4; y <= F + 5; y++) p.set(x, y, 35, facing(ModBlocks.MERIDIAN_LOGO.get(), Direction.NORTH));
        for (int x = 6; x <= 34; x++) {
            if (x == 11 || x == 22 || x == 28) continue;
            p.set(x, F + 1, 30, facing(ModBlocks.CHECKIN_COUNTER.get(), Direction.NORTH));
            p.set(x, F + 1, 33, facing(ModBlocks.CONVEYOR_BELT.get(), Direction.EAST));
        }
        p.sign(16, F + 7, 34, Direction.NORTH, DyeColor.LIGHT_BLUE, "MERIDIAN AIR", "Check-in", "Smile! You're", "on camera");
        p.sign(8, F + 7, 34, Direction.NORTH, DyeColor.WHITE, "SOLACE AIR", "Check-in", "", "");
        p.sign(31, F + 7, 34, Direction.NORTH, DyeColor.WHITE, "HARROW JET", "Check-in", "", "");
        // Queue stanchions for the Meridian desk
        for (int x = 13; x <= 20; x++) p.set(x, F + 1, 26, facing(ModBlocks.STANCHION.get(), Direction.NORTH));
        for (int x = 12; x <= 19; x++) p.set(x, F + 1, 23, facing(ModBlocks.STANCHION.get(), Direction.NORTH));
        // Benches by the west windows
        for (int x = 2; x <= 10; x++) {
            p.set(x, F + 1, 7, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.SOUTH));
            p.set(x, F + 1, 12, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.NORTH));
        }
        p.set(1, F + 1, 1, Blocks.POTTED_AZALEA_BUSH);
        p.set(1, F + 1, 15, Blocks.POTTED_AZALEA_BUSH);
        p.set(38, F + 1, 15, Blocks.POTTED_FLOWERING_AZALEA_BUSH);
        // Departures board on a pillar
        p.fill(26, F + 1, 10, 26, F + 3, 11, wall);
        for (int x = 24; x <= 28; x++) {
            for (int y = F + 4; y <= F + 5; y++) {
                p.set(x, y, 10, facing(ModBlocks.DEPARTURE_BOARD.get(), Direction.NORTH));
                p.set(x, y, 11, facing(ModBlocks.DEPARTURE_BOARD.get(), Direction.SOUTH));
            }
        }
        p.set(34, F + 1, 12, facing(ModBlocks.SMILESCORE_KIOSK.get(), Direction.NORTH));
        p.set(38, F + 1, 3, facing(ModBlocks.VENDING_MACHINE.get(), Direction.WEST));
        p.set(38, F + 1, 4, facing(ModBlocks.VENDING_MACHINE.get(), Direction.WEST));
        // Partition to security, closed until you have checked in
        p.fill(40, F + 1, 1, 40, CEIL - 1, 46, wall);
        p.clear(40, F + 1, 18, 40, F + 4, 29);
        barrier(p, "to_security", true);
        p.sign(39, F + 6, 23, Direction.WEST, DyeColor.WHITE, "SECURITY", "All passengers", "-->", "");
    }

    private static void security(Placer p, BlockState wall, BlockState white) {
        for (int x = 41; x <= 45; x++) p.set(x, F + 1, 21, facing(ModBlocks.STANCHION.get(), Direction.NORTH));
        for (int x = 42; x <= 46; x++) p.set(x, F + 1, 26, facing(ModBlocks.STANCHION.get(), Direction.NORTH));
        for (int lane = 0; lane < 2; lane++) {
            int bz = 19 + lane * 6;
            for (int x = 46; x <= 54; x++) p.set(x, F + 1, bz, facing(ModBlocks.CONVEYOR_BELT.get(), Direction.EAST));
            p.set(50, F + 1, bz, facing(ModBlocks.XRAY_MACHINE.get(), Direction.WEST));
            p.set(45, F + 1, bz, Blocks.SMOOTH_STONE_SLAB);
            for (int y = F + 1; y <= F + 2; y++) {
                p.set(50, y, bz + 1, facing(ModBlocks.SCANNER_PILLAR.get(), Direction.EAST));
                p.set(50, y, bz + 3, facing(ModBlocks.SCANNER_PILLAR.get(), Direction.EAST));
            }
            for (int z = bz + 1; z <= bz + 3; z++) p.set(50, F + 3, z, facing(ModBlocks.SCANNER_TOP.get(), Direction.EAST));
        }
        p.fill(56, F + 1, 1, 56, CEIL - 1, 46, wall);
        p.clear(56, F + 1, 18, 56, F + 4, 29);
        barrier(p, "to_airside", true);
        p.sign(55, F + 6, 23, Direction.WEST, DyeColor.WHITE, "AIRSIDE", "Gates B5 - B7", "Cloud Nine Coffee", "");
        p.sign(57, F + 6, 23, Direction.EAST, DyeColor.LIGHT_BLUE, "GATES", "B5  B6  B7", "-->", "");
    }

    private static void airside(Placer p, BlockState white) {
        // Cloud Nine Coffee
        p.fill(58, F + 1, 0, 68, CEIL - 1, 0, white);
        for (int x = 60; x <= 66; x++) {
            p.set(x, F + 1, 6, facing(ModBlocks.CHECKIN_COUNTER.get(), Direction.SOUTH));
            p.set(x, F + 1, 1, x % 2 == 0 ? facing(Blocks.SMOKER, Direction.SOUTH) : facing(Blocks.BARREL, Direction.UP));
        }
        p.sign(63, F + 6, 1, Direction.SOUTH, DyeColor.WHITE, "CLOUD NINE", "COFFEE", "~ always hot ~", "");
        int[][] tables = {{61, 11}, {65, 11}, {61, 15}, {65, 15}};
        for (int[] t : tables) {
            p.set(t[0], F + 1, t[1], Blocks.DARK_OAK_FENCE);
            p.set(t[0], F + 2, t[1], Blocks.BIRCH_PRESSURE_PLATE);
            p.set(t[0] - 1, F + 1, t[1], facing(ModBlocks.AIRPORT_SEAT.get(), Direction.EAST));
            p.set(t[0] + 1, F + 1, t[1], facing(ModBlocks.AIRPORT_SEAT.get(), Direction.WEST));
        }
        // Duty free
        for (int x = 71; x <= 78; x++) {
            p.set(x, F + 1, 4, facing(Blocks.BARREL, Direction.UP));
            p.set(x, F + 1, 8, facing(Blocks.BARREL, Direction.UP));
            if (x % 2 == 0) p.set(x, F + 2, 4, facing(ModBlocks.SUITCASE.get(), Direction.SOUTH));
        }
        p.sign(75, F + 6, 1, Direction.SOUTH, DyeColor.YELLOW, "DUTY FREE", "Sunscreen", "SPF 50", "");
        p.fill(70, F + 1, 0, 80, CEIL - 1, 0, white);
        p.set(68, F + 1, 22, facing(ModBlocks.SMILESCORE_KIOSK.get(), Direction.SOUTH));
        p.set(80, F + 1, 2, facing(ModBlocks.VENDING_MACHINE.get(), Direction.SOUTH));
        p.set(57, F + 1, 46, Blocks.POTTED_AZALEA_BUSH);
        p.set(126, F + 1, 20, Blocks.POTTED_AZALEA_BUSH);

        int[] gates = {Locations.GATE_B5, Locations.GATE_B6, Locations.GATE_B7};
        String[] names = {"B5", "B6", "B7"};
        String[] flights = {"MA112 SOLACE BAY", "MA305 NEW HARROW", "MA417 PORT ELLERY"};
        for (int i = 0; i < 3; i++) {
            int gx = gates[i];
            p.set(gx, F + 1, 43, facing(ModBlocks.BOARDING_SCANNER.get(), Direction.NORTH));
            for (int z : new int[]{28, 31, 34, 37}) {
                for (int dx = 1; dx <= 5; dx++) {
                    p.set(gx - dx, F + 1, z, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.SOUTH));
                    p.set(gx + dx, F + 1, z, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.SOUTH));
                }
            }
            p.fill(gx - 2, F + 6, 47, gx + 2, CEIL, 47, white);
            p.sign(gx, F + 7, 46, Direction.NORTH, DyeColor.WHITE, "GATE " + names[i], flights[i].substring(0, 5), flights[i].substring(6), i == 2 ? "09:10" : "");
            for (int x = gx + 3; x <= gx + 4; x++)
                for (int y = F + 5; y <= F + 6; y++) p.set(x, y, 46, facing(ModBlocks.DEPARTURE_BOARD.get(), Direction.NORTH));
        }
        p.sign(81, F + 6, 1, Direction.SOUTH, DyeColor.LIGHT_BLUE, "CONCOURSE B", "B5  B6  B7", "-->", "");
        // Gate B7 door to the jet bridge
        p.clear(Locations.GATE_B7 - 1, F + 1, 47, Locations.GATE_B7 + 1, F + 3, 47);
        barrier(p, "jet_bridge", true);
    }

    private static void jetBridge(Placer p, BlockState lgray, BlockState glass, BlockState air) {
        int x0 = Locations.GATE_B7 - 2, x1 = Locations.GATE_B7 + 2;
        BlockState floor = Blocks.GRAY_WOOL.defaultBlockState();
        for (int z = 48; z <= 61; z++) {
            int fy = floorY(z);
            for (int x = x0; x <= x1; x++) {
                if (fy - 1 >= F + 1) p.fill(x, F + 1, z, x, fy - 1, z, lgray);
                p.set(x, fy, z, x == x0 || x == x1 ? lgray : floor);
            }
            for (int y = fy + 1; y <= 72; y++) {
                boolean win = (z % 2 == 0) && y >= fy + 2 && y <= fy + 3;
                p.set(x0, y, z, win ? glass : lgray);
                p.set(x1, y, z, win ? glass : lgray);
                for (int x = x0 + 1; x < x1; x++) p.set(x, y, z, air);
            }
            for (int x = x0; x <= x1; x++) p.set(x, 73, z, lgray);
            if (z % 4 == 2) p.set(Locations.GATE_B7, 73, z, Blocks.SEA_LANTERN);
            if (isStair(z)) {
                for (int x = x0 + 1; x < x1; x++)
                    p.set(x, fy, z, Blocks.SMOOTH_STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
            }
        }
        for (int x = x0; x <= x1; x++) p.fill(x, F + 1, 58, x, F + 1, 58, Blocks.IRON_BLOCK.defaultBlockState());
    }

    private static boolean isStair(int z) {
        return z == 50 || z == 52 || z == 54 || z == 56;
    }

    /** Floor block height inside the jet bridge, rising four blocks to the aircraft floor at y=67. */
    private static int floorY(int z) {
        if (z <= 49) return F;
        if (z <= 51) return F + 1;
        if (z <= 53) return F + 2;
        if (z <= 55) return F + 3;
        return F + 4;
    }

    /** Glass barriers the story opens as you progress. */
    public static void barrier(Placer p, String name, boolean closed) {
        BlockState s = closed ? ModBlocks.STORY_BARRIER.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
        switch (name) {
            case "to_security" -> p.fill(40, F + 1, 18, 40, F + 4, 29, s);
            case "to_airside" -> p.fill(56, F + 1, 18, 56, F + 4, 29, s);
            case "jet_bridge" -> p.fill(Locations.GATE_B7 - 1, F + 1, 47, Locations.GATE_B7 + 1, F + 3, 47, s);
            default -> {}
        }
    }

    /** Switch every departures board between normal (0) and glitched (1). */
    public static void boards(Placer p, int mode) {
        Block b = ModBlocks.DEPARTURE_BOARD.get();
        for (int x = 24; x <= 28; x++)
            for (int y = F + 4; y <= F + 5; y++)
                for (int z = 10; z <= 11; z++) setMode(p, x, y, z, b, mode);
        for (int gx : new int[]{Locations.GATE_B5, Locations.GATE_B6, Locations.GATE_B7})
            for (int x = gx + 3; x <= gx + 4; x++)
                for (int y = F + 5; y <= F + 6; y++) setMode(p, x, y, 46, b, mode);
    }

    private static void setMode(Placer p, int x, int y, int z, Block b, int mode) {
        BlockState s = p.get(x, y, z);
        if (s.is(b)) p.set(x, y, z, s.setValue(StateInteractBlock.MODE, mode));
    }
}
