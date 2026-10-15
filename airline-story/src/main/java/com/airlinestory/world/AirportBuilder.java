package com.airlinestory.world;

import com.airlinestory.block.StateInteractBlock;
import com.airlinestory.registry.ModBlocks;
import com.airlinestory.registry.ModItems;
import com.airlinestory.story.Locations;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.airlinestory.world.Placer.*;

/**
 * Meridian International, built from vanilla blocks wherever possible. West to east: the landside hall
 * (check-in, departures board, newsstand, waiting area), the security hall, the airside plaza (cafe,
 * duty free, lounge) and the concourse with gates B5-B7 on the south windows. Behind the staff door on the
 * north wall: the production backstage.
 */
public final class AirportBuilder {
    private AirportBuilder() {}

    public static final int F = Locations.FLOOR;      // floor blocks at y=63
    public static final int CEIL = 75;                // ceiling blocks
    public static final int X1 = 158, Z1 = 56;        // terminal extents (x 0..X1, z 0..Z1)

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public static void build(Placer p) {
        ground(p);
        shell(p);
        landside(p);
        security(p);
        plaza(p);
        concourse(p);
        jetBridge(p);
        backstage(p);
        tarmac(p);
        p.finish();
    }

    // =========================================================================================
    private static void ground(Placer p) {
        p.fill(-30, F, -40, 230, F, 160, Blocks.GRAY_CONCRETE);
        // kerb, road and car park in front of the terminal
        p.fill(-30, F, -26, 70, F, -12, Blocks.BLACK_CONCRETE);
        for (int x = -30; x <= 70; x += 5) p.fill(x, F, -19, x + 2, F, -19, Blocks.WHITE_CONCRETE);
        p.fill(-30, F, -11, 70, F, -1, Blocks.SMOOTH_STONE);
        p.fill(-30, F, -11, 70, F, -11, Blocks.POLISHED_ANDESITE);
        for (int x = 4; x <= 60; x += 14) {
            p.fill(x, F + 1, -6, x, F + 4, -6, Blocks.ANDESITE_WALL);
            p.set(x, F + 5, -6, Blocks.SEA_LANTERN);
            p.set(x - 1, F + 1, -7, Blocks.POTTED_AZALEA);
        }
        // taxi rank: a few blocky cars
        car(p, -6, -22, Blocks.YELLOW_CONCRETE);
        car(p, 8, -22, Blocks.YELLOW_CONCRETE);
        car(p, 40, -15, Blocks.WHITE_CONCRETE);
        p.hangingSign(Blocks.DARK_OAK_HANGING_SIGN, 26, F + 6, -2, Direction.NORTH, DyeColor.WHITE, "", "DEPARTURES", "Meridian Intl.", "");
        // the control tower, west of the terminal
        int tx = -18, tz = 66;
        p.fill(tx - 2, F + 1, tz - 2, tx + 2, F + 30, tz + 2, Blocks.LIGHT_GRAY_CONCRETE);
        for (int y = F + 2; y <= F + 28; y += 4) p.fill(tx - 2, y, tz, tx + 2, y, tz, Blocks.GLASS);
        p.fill(tx - 4, F + 31, tz - 4, tx + 4, F + 31, tz + 4, Blocks.WHITE_CONCRETE);
        p.walls(tx - 4, F + 32, tz - 4, tx + 4, F + 34, tz + 4, Blocks.GRAY_STAINED_GLASS_PANE);
        p.fill(tx - 5, F + 35, tz - 5, tx + 5, F + 35, tz + 5, Blocks.WHITE_CONCRETE);
        p.fill(tx, F + 36, tz, tx, F + 39, tz, Blocks.IRON_BARS);
        p.set(tx, F + 40, tz, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
    }

    private static void car(Placer p, int x, int z, Block body) {
        p.fill(x, F + 1, z, x + 4, F + 1, z + 2, body);
        p.fill(x + 1, F + 2, z, x + 3, F + 2, z + 2, Blocks.LIGHT_BLUE_STAINED_GLASS);
        for (int[] w : new int[][]{{0, -1}, {4, -1}, {0, 3}, {4, 3}}) p.set(x + w[0], F + 1, z + Math.max(0, Math.min(2, w[1])), Blocks.BLACK_CONCRETE);
    }

    // =========================================================================================
    private static void shell(Placer p) {
        BlockState floor = Blocks.CALCITE.defaultBlockState();
        BlockState band = Blocks.POLISHED_DIORITE.defaultBlockState();
        for (int x = 1; x < X1; x++)
            for (int z = 1; z < Z1; z++) p.set(x, F, z, (x % 12 == 0 || z % 12 == 0) ? band : floor);
        // ceiling with light grid and skylight strips
        for (int x = 0; x <= X1; x++) {
            for (int z = 0; z <= Z1; z++) {
                boolean sky = x % 16 >= 6 && x % 16 <= 9 && z >= 10 && z <= Z1 - 10;
                boolean lamp = x % 6 == 3 && z % 6 == 3 && !sky;
                p.set(x, CEIL, z, sky ? Blocks.GLASS.defaultBlockState() : lamp ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState());
                p.set(x, CEIL + 1, z, sky ? Blocks.AIR.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            }
        }
        for (int x = 0; x <= X1; x += 16) {
            p.fill(x, CEIL + 2, 0, x, CEIL + 2, Z1, Blocks.WHITE_CONCRETE);
            p.fill(x + 4, CEIL + 1, 0, x + 4, CEIL + 1, Z1, Blocks.SMOOTH_QUARTZ);
        }
        // walls: north (landside face) and south (gates face) are glass curtain walls with mullions
        for (int x = 0; x <= X1; x++) {
            for (int y = F + 1; y < CEIL; y++) {
                boolean mullion = x % 4 == 0 || y == F + 1 || y == CEIL - 1 || y == F + 5;
                p.set(x, y, 0, mullion ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.GLASS_PANE.defaultBlockState());
                p.set(x, y, Z1, mullion ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.GLASS_PANE.defaultBlockState());
            }
        }
        for (int z = 0; z <= Z1; z++)
            for (int y = F + 1; y < CEIL; y++) {
                boolean win = z % 6 != 0 && y >= F + 3 && y <= F + 7;
                p.set(0, y, z, win ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState());
                p.set(X1, y, z, win ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState());
            }
        // exterior canopy over the kerb
        p.fill(10, F + 7, -9, 44, F + 7, -1, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
        for (int x = 10; x <= 44; x += 8) p.fill(x, F + 1, -9, x, F + 6, -9, Blocks.QUARTZ_PILLAR);
        // entrance doors (two sets) and the logo above them
        for (int[] d : new int[][]{{18, 22}, {30, 34}}) p.clear(d[0], F + 1, 0, d[1], F + 4, 0);
        p.fill(17, F + 6, 0, 35, F + 9, 0, Blocks.WHITE_CONCRETE);
        for (int x = 23; x <= 29; x++) p.set(x, F + 8, -1, facing(ModBlocks.MERIDIAN_LOGO.get(), Direction.NORTH));
        p.sign(26, F + 7, -1, Direction.NORTH, DyeColor.LIGHT_BLUE, "MERIDIAN", "INTERNATIONAL", "", "");
        // interior structural columns
        for (int x = 16; x < X1; x += 16)
            for (int z : new int[]{12, 44}) {
                if (x == 48 || x == 80) continue;
                p.fill(x, F + 1, z, x, CEIL - 1, z, Blocks.QUARTZ_PILLAR);
                p.set(x, F + 1, z - 1, Blocks.SMOOTH_QUARTZ_SLAB);
            }
    }

    // =========================================================================================
    // Landside hall: x 1..51
    // =========================================================================================
    private static void landside(Placer p) {
        // departures board: a free-standing wall of flip signs
        p.fill(21, F + 1, 16, 31, F + 9, 16, Blocks.BLACK_CONCRETE);
        p.fill(21, F + 10, 16, 31, F + 10, 16, Blocks.WHITE_CONCRETE);
        String[][] flights = {
                {"MA112", "SOLACE BAY", "08:20  B5", "DEPARTED"},
                {"HJ240", "NEW HARROW", "08:35  B6", "BOARDING"},
                {"MA305", "KESTREL CITY", "08:55  B6", "GO TO GATE"},
                {"MA417", "PORT ELLERY", "09:10  B7", "ON TIME"},
                {"SA090", "LOW MARREN", "09:40  B5", "DELAYED"},
                {"MA417", "PORT ELLERY", "09:10  B7", "ON TIME"},
        };
        for (Direction face : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
            int z = face == Direction.NORTH ? 15 : 17;
            p.sign(face == Direction.NORTH ? 26 : 26, F + 9, z, face, DyeColor.WHITE, "", "DEPARTURES", "✈", "");
            for (int r = 0; r < flights.length; r++) {
                int y = F + 8 - r;
                int[] xs = face == Direction.NORTH ? new int[]{30, 28, 26, 24} : new int[]{22, 24, 26, 28};
                for (int c = 0; c < 4; c++) {
                    DyeColor col = c == 3 ? (flights[r][3].equals("DELAYED") ? DyeColor.RED : flights[r][3].equals("ON TIME") ? DyeColor.LIME : DyeColor.YELLOW) : DyeColor.YELLOW;
                    p.sign(xs[c], y, z, face, col, "", flights[r][c], "", "");
                }
            }
            p.frame(face == Direction.NORTH ? 22 : 30, F + 9, z, face, new ItemStack(Items.CLOCK), true);
        }
        // the departures board is also a story object
        p.set(26, F + 1, 15, facing(ModBlocks.DEPARTURE_BOARD.get(), Direction.NORTH));
        p.set(26, F + 1, 17, facing(ModBlocks.DEPARTURE_BOARD.get(), Direction.SOUTH));

        // info desk
        for (int[] c : new int[][]{{24, 7}, {25, 6}, {26, 6}, {27, 6}, {28, 7}, {24, 8}, {28, 8}}) {
            p.set(c[0], F + 1, c[1], Blocks.LIGHT_BLUE_CONCRETE);
            p.set(c[0], F + 2, c[1], slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
        }
        p.hangingSign(Blocks.DARK_OAK_HANGING_SIGN, 26, F + 5, 7, Direction.NORTH, DyeColor.WHITE, "", "INFORMATION", "", "");
        p.hangingLight(26, CEIL, 7, 4, Blocks.SEA_LANTERN);

        // check-in islands
        island(p, 4, 14, "SOLACE AIR", Blocks.LIME_CONCRETE, DyeColor.LIME);
        island(p, 19, 35, "MERIDIAN AIR", Blocks.BLUE_CONCRETE, DyeColor.LIGHT_BLUE);
        island(p, 40, 48, "HARROW JET", Blocks.RED_CONCRETE, DyeColor.RED);
        // Meridian queue lanes (stanchions)
        for (int x = 20; x <= 33; x++) {
            p.set(x, F + 1, 23, facing(ModBlocks.STANCHION.get(), Direction.NORTH));
            if (x >= 21) p.set(x, F + 1, 20, facing(ModBlocks.STANCHION.get(), Direction.NORTH));
        }
        for (int x = 20; x <= 34; x++) p.set(x, F, 21, Blocks.BLUE_CARPET.defaultBlockState().is(Blocks.BLUE_CARPET) ? Blocks.BLUE_CONCRETE.defaultBlockState() : Blocks.BLUE_CONCRETE.defaultBlockState());
        p.hangingSign(Blocks.DARK_OAK_HANGING_SIGN, 34, F + 4, 21, Direction.NORTH, DyeColor.LIGHT_BLUE, "MERIDIAN", "Check-in", "↓ queue here", "");

        // waiting area by the west windows
        for (int x = 2; x <= 12; x++) {
            if (x == 7) continue;
            p.set(x, F + 1, 40, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.SOUTH));
            p.set(x, F + 1, 41, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.NORTH));
            p.set(x, F + 1, 46, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.SOUTH));
        }
        planter(p, 7, 40);
        planter(p, 1, 50);
        planter(p, 14, 50);
        p.set(1, F + 1, 36, facing(ModBlocks.VENDING_MACHINE.get(), Direction.EAST));
        p.set(1, F + 1, 37, facing(ModBlocks.VENDING_MACHINE.get(), Direction.EAST));
        p.set(1, F + 1, 34, Blocks.CAULDRON);
        // restrooms on the south wall
        for (int[] r : new int[][]{{20, 0}, {28, 1}}) {
            p.door(r[0], F + 1, Z1 - 1, Blocks.IRON_DOOR, Direction.NORTH, false);
            p.sign(r[0] + 1, F + 3, Z1 - 1, Direction.NORTH, DyeColor.WHITE, "", r[1] == 0 ? "RESTROOMS" : "FAMILY ROOM", "←", "");
        }
        // newsstand
        newsstand(p, 39, 4);
        // janitor's closet (with a secret in the back wall)
        int cx = 44;
        p.walls(cx, F + 1, 47, cx + 6, F + 4, Z1 - 1, Blocks.WHITE_CONCRETE.defaultBlockState());
        p.fill(cx, F + 5, 47, cx + 6, F + 5, Z1 - 1, Blocks.WHITE_CONCRETE);
        p.door(cx + 3, F + 1, 47, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
        p.sign(cx + 4, F + 3, 46, Direction.NORTH, DyeColor.GRAY, "STAFF", "ONLY", "Janitor", "");
        p.fill(cx + 1, F + 1, Z1 - 2, cx + 5, F + 4, Z1 - 2, Blocks.STRIPPED_SPRUCE_WOOD);
        p.set(cx + 3, F + 2, Z1 - 2, facing(Blocks.OBSERVER, Direction.NORTH));
        p.set(cx + 1, F + 1, 48, Blocks.CAULDRON.defaultBlockState());
        p.set(cx + 5, F + 1, 48, Blocks.BARREL);
        p.set(cx + 5, F + 2, 48, Blocks.BARREL);
        p.clue(cx + 1, F + 1, 52, ModBlocks.CLUE_NOTE.get(), "closet_camera");
        p.set(cx + 4, F + 1, 52, facing(ModBlocks.WET_FLOOR_SIGN.get(), Direction.NORTH));
        p.set(cx + 3, F + 4, 51, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));

        // the partition to security, closed until you have checked in
        partition(p, 52, "SECURITY", "All passengers", "→");
        barrier(p, "to_security", true);
        // a lost toy for Leo, hidden under the benches
        p.set(11, F + 1, 47, ModBlocks.LOST_PLUSH.get());
    }

    private static void island(Placer p, int x1, int x2, String name, Block brand, DyeColor color) {
        for (int x = x1; x <= x2; x++) {
            boolean gap = (x - x1) % 5 == 4;
            p.set(x, F + 1, 28, gap ? Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState() : facing(ModBlocks.CHECKIN_COUNTER.get(), Direction.NORTH));
            p.set(x, F + 2, 28, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
            p.set(x, F + 1, 30, facing(ModBlocks.CONVEYOR_BELT.get(), Direction.EAST));
            p.fill(x, F + 1, 31, x, F + 3, 31, Blocks.WHITE_CONCRETE);
            p.fill(x, F + 4, 31, x, F + 5, 31, brand);
            p.set(x, F + 6, 31, Blocks.WHITE_CONCRETE);
        }
        p.fill(x1 - 1, F + 1, 28, x1 - 1, F + 6, 31, Blocks.LIGHT_GRAY_CONCRETE);
        p.fill(x2 + 1, F + 1, 28, x2 + 1, F + 6, 31, Blocks.LIGHT_GRAY_CONCRETE);
        int mid = (x1 + x2) / 2;
        p.sign(mid, F + 5, 30, Direction.NORTH, color, name, "Check-in", "", "");
        p.sign(mid - 3, F + 4, 30, Direction.NORTH, DyeColor.WHITE, "Bag drop", "↓", "", "");
        p.sign(mid + 3, F + 4, 30, Direction.NORTH, DyeColor.WHITE, "Desks", "1 - 4", "", "");
        // back of the island, facing south
        p.sign(mid, F + 4, 32, Direction.SOUTH, color, name, "", "", "");
    }

    private static void planter(Placer p, int x, int z) {
        p.set(x, F + 1, z, Blocks.MOSS_BLOCK);
        p.set(x, F + 2, z, Blocks.FLOWERING_AZALEA);
    }

    private static void newsstand(Placer p, int x, int z) {
        p.fill(x, F + 1, z, x + 10, F + 1, z, Blocks.SPRUCE_PLANKS);
        p.fill(x, F + 2, z, x + 10, F + 2, z, slab(Blocks.SPRUCE_SLAB, false));
        for (int i = 0; i <= 10; i += 2) {
            p.set(x + i, F + 1, z - 3, Blocks.BOOKSHELF);
            p.set(x + i + 1, F + 1, z - 3, Blocks.CHISELED_BOOKSHELF);
            p.set(x + i, F + 2, z - 3, Blocks.BOOKSHELF);
        }
        p.fill(x, F + 5, z - 3, x + 10, F + 5, z, Blocks.SPRUCE_PLANKS);
        p.hangingSign(Blocks.SPRUCE_HANGING_SIGN, x + 5, F + 4, z + 1, Direction.SOUTH, DyeColor.BLACK, "SKYLINE", "NEWS", "Papers • Snacks", "");
        for (int i = 1; i <= 9; i += 2) p.frame(x + i, F + 3, z, Direction.SOUTH, new ItemStack(i % 4 == 1 ? Items.WRITTEN_BOOK : Items.FILLED_MAP), false);
        p.frame(x + 2, F + 3, z - 2, Direction.SOUTH, new ItemStack(Items.COOKIE), false);
        p.frame(x + 8, F + 3, z - 2, Direction.SOUTH, new ItemStack(Items.SWEET_BERRIES), false);
    }

    private static void partition(Placer p, int x, String l1, String l2, String l3) {
        for (int z = 1; z < Z1; z++)
            for (int y = F + 1; y < CEIL; y++) {
                boolean opening = z >= 18 && z <= 34 && y <= F + 4;
                if (opening) continue;
                boolean glass = y >= F + 6 && y <= F + 9 && z % 4 != 0;
                p.set(x, y, z, glass ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState());
            }
        p.sign(x - 1, F + 6, 26, Direction.WEST, DyeColor.WHITE, l1, l2, l3, "");
        p.sign(x + 1, F + 6, 26, Direction.EAST, DyeColor.WHITE, l1, "", "", "");
    }

    // =========================================================================================
    // Security hall: x 53..75
    // =========================================================================================
    public static final int[] LANES = {16, 24, 32};

    private static void security(Placer p) {
        p.fill(53, F, 14, 75, F, 38, Blocks.GRAY_CONCRETE);
        // queue switchback
        for (int z = 21; z <= 31; z += 3)
            for (int x = (z / 3) % 2 == 0 ? 54 : 55; x <= ((z / 3) % 2 == 0 ? 59 : 60); x++)
                p.set(x, F + 1, z, facing(ModBlocks.STANCHION.get(), Direction.NORTH));
        for (int lz : LANES) {
            for (int x = 61; x <= 73; x++) p.set(x, F + 1, lz, facing(ModBlocks.CONVEYOR_BELT.get(), Direction.EAST));
            p.set(67, F + 1, lz, facing(ModBlocks.XRAY_MACHINE.get(), Direction.WEST));
            p.set(67, F + 2, lz, Blocks.LIGHT_GRAY_CONCRETE);
            for (int x = 61; x <= 64; x++) p.set(x, F + 2, lz, slab(Blocks.SMOOTH_STONE_SLAB, false));
            for (int y = F + 1; y <= F + 2; y++) {
                p.set(67, y, lz + 1, facing(ModBlocks.SCANNER_PILLAR.get(), Direction.EAST));
                p.set(67, y, lz + 3, facing(ModBlocks.SCANNER_PILLAR.get(), Direction.EAST));
            }
            for (int z = lz + 1; z <= lz + 3; z++) p.set(67, F + 3, z, facing(ModBlocks.SCANNER_TOP.get(), Direction.EAST));
            // low glass partitions so you have to walk through the arch
            for (int x = 61; x <= 73; x++) if (x != 67) p.set(x, F + 1, lz + 3, Blocks.GLASS_PANE);
            p.hangingSign(Blocks.DARK_OAK_HANGING_SIGN, 62, F + 4, lz + 2, Direction.WEST, DyeColor.WHITE, "LANE " + ((lz - 16) / 8 + 1), "Trays →", "", "");
        }
        // secondary screening room
        p.walls(62, F + 1, 42, 74, F + 5, Z1 - 1, Blocks.WHITE_CONCRETE.defaultBlockState());
        p.fill(62, F + 6, 42, 74, F + 6, Z1 - 1, Blocks.WHITE_CONCRETE);
        for (int x = 64; x <= 72; x++) p.set(x, F + 3, 42, Blocks.GRAY_STAINED_GLASS_PANE);
        p.door(68, F + 1, 42, Blocks.IRON_DOOR, Direction.NORTH, true);
        p.sign(69, F + 4, 41, Direction.NORTH, DyeColor.RED, "SECONDARY", "SCREENING", "Authorised", "only");
        p.set(68, F + 1, 49, Blocks.DARK_OAK_FENCE);
        p.set(68, F + 2, 49, slab(Blocks.POLISHED_ANDESITE_SLAB, false));
        p.set(67, F + 1, 49, slab(Blocks.POLISHED_ANDESITE_SLAB, false));
        p.set(69, F + 1, 49, slab(Blocks.POLISHED_ANDESITE_SLAB, false));
        p.set(68, F + 1, 47, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
        p.set(68, F + 1, 51, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
        p.set(68, F + 5, 49, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        p.set(73, F + 4, 54, facing(Blocks.OBSERVER, Direction.WEST));
        p.set(63, F + 1, 54, Blocks.BARREL);
        partition(p, 76, "AIRSIDE", "Gates B5 - B7", "Cloud Nine • Duty Free");
        barrier(p, "to_airside", true);
        p.sign(75, F + 3, 14, Direction.WEST, DyeColor.WHITE, "Please have", "your boarding", "pass ready", "");
    }

    // =========================================================================================
    // Airside plaza: x 77..104
    // =========================================================================================
    private static void plaza(Placer p) {
        // Cloud Nine Coffee
        int cx = 78;
        p.fill(cx, F + 1, 1, cx + 12, F + 1, 1, Blocks.STRIPPED_OAK_WOOD);
        p.fill(cx, F + 2, 1, cx + 12, F + 2, 1, slab(Blocks.SMOOTH_STONE_SLAB, false));
        for (int x = cx; x <= cx + 12; x += 3) {
            p.set(x, F + 2, 1, facing(Blocks.SMOKER, Direction.SOUTH));
            p.set(x + 1, F + 3, 1, Blocks.BREWING_STAND);
        }
        for (int x = cx + 1; x <= cx + 11; x++) {
            p.set(x, F + 1, 7, facing(Blocks.OAK_STAIRS, Direction.SOUTH).setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
        }
        p.fill(cx + 1, F + 2, 7, cx + 11, F + 2, 7, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
        p.fill(cx, F + 1, 2, cx, F + 1, 7, Blocks.STRIPPED_OAK_WOOD);
        p.fill(cx + 12, F + 1, 2, cx + 12, F + 1, 7, Blocks.STRIPPED_OAK_WOOD);
        p.set(cx + 10, F + 3, 7, Blocks.POTTED_RED_TULIP);
        p.set(cx + 2, F + 3, 7, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        p.frame(cx + 3, F + 3, 1, Direction.SOUTH, new ItemStack(ModItems.COFFEE.get()), false);
        p.frame(cx + 6, F + 3, 1, Direction.SOUTH, new ItemStack(Items.CAKE), false);
        p.frame(cx + 9, F + 3, 1, Direction.SOUTH, new ItemStack(Items.COOKIE), false);
        p.fill(cx, F + 6, 1, cx + 12, F + 6, 9, Blocks.SPRUCE_PLANKS);
        p.hangingSign(Blocks.SPRUCE_HANGING_SIGN, cx + 6, F + 5, 9, Direction.SOUTH, DyeColor.BROWN, "CLOUD NINE", "COFFEE", "~ always hot ~", "");
        for (int x = cx + 2; x <= cx + 10; x += 4) p.hangingLight(x, F + 6, 5, 1, Blocks.LANTERN);
        // seating
        for (int[] t : new int[][]{{80, 14}, {85, 14}, {90, 14}, {80, 19}, {85, 19}, {90, 19}}) table(p, t[0], t[1]);
        planter(p, 95, 18);
        planter(p, 78, 22);

        // Duty free
        int dx = 92;
        for (int x = dx; x <= dx + 12; x++)
            for (int y = F + 1; y <= F + 5; y++) {
                boolean door = x >= dx + 5 && x <= dx + 7 && y <= F + 3;
                if (!door) p.set(x, y, 13, y == F + 5 ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.GLASS_PANE.defaultBlockState());
            }
        p.fill(dx, F + 6, 1, dx + 12, F + 6, 13, Blocks.WHITE_CONCRETE);
        p.fill(dx, F + 1, 1, dx, F + 5, 13, Blocks.WHITE_CONCRETE);
        p.hangingSign(Blocks.BIRCH_HANGING_SIGN, dx + 6, F + 5, 14, Direction.SOUTH, DyeColor.BLACK, "DUTY FREE", "Sunscreen • Perfume", "SPF 50", "");
        for (int sx : new int[]{dx + 2, dx + 6, dx + 10}) {
            for (int z = 3; z <= 10; z++) {
                p.set(sx, F + 1, z, Blocks.SPRUCE_PLANKS);
                p.set(sx, F + 2, z, slab(Blocks.SPRUCE_SLAB, true));
                p.set(sx, F + 3, z, Blocks.SPRUCE_PLANKS);
            }
            for (int z = 3; z <= 10; z += 2) {
                ItemStack sun = PotionUtils.setPotion(new ItemStack(Items.POTION), z % 4 == 1 ? Potions.FIRE_RESISTANCE : Potions.WATER_BREATHING);
                p.frame(sx + 1, F + 2, z, Direction.EAST, sun, true);
                p.frame(sx - 1, F + 2, z, Direction.WEST, new ItemStack(z % 4 == 1 ? Items.HONEY_BOTTLE : Items.GLASS_BOTTLE), true);
                p.frame(sx + 1, F + 3, z, Direction.EAST, new ItemStack(Items.AMETHYST_SHARD), true);
            }
        }
        p.fill(dx + 9, F + 1, 11, dx + 11, F + 1, 11, Blocks.WHITE_CONCRETE);
        p.fill(dx + 9, F + 2, 11, dx + 11, F + 2, 11, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));

        // Meridian Prime lounge (south side)
        int lx = 78;
        for (int x = lx; x <= lx + 20; x++)
            for (int y = F + 1; y <= F + 5; y++) {
                boolean door = x >= lx + 9 && x <= lx + 10 && y <= F + 3;
                if (!door) p.set(x, y, 40, y == F + 5 ? Blocks.BLUE_CONCRETE.defaultBlockState() : Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.defaultBlockState());
            }
        p.fill(lx + 20, F + 1, 40, lx + 20, F + 5, Z1 - 1, Blocks.WHITE_CONCRETE);
        p.fill(lx, F + 6, 40, lx + 20, F + 6, Z1 - 1, Blocks.WHITE_CONCRETE);
        p.fill(lx, F, 41, lx + 19, F, Z1 - 1, Blocks.BLUE_WOOL);
        p.sign(lx + 11, F + 4, 39, Direction.NORTH, DyeColor.LIGHT_BLUE, "MERIDIAN", "PRIME", "LOUNGE", "4.5 & above");
        for (int x = lx + 2; x <= lx + 16; x += 4) {
            p.set(x, F + 1, 46, facing(Blocks.BLUE_WOOL, Direction.NORTH));
            p.set(x, F + 1, 47, stair(Blocks.QUARTZ_STAIRS, Direction.SOUTH, false));
            p.set(x + 1, F + 1, 47, stair(Blocks.QUARTZ_STAIRS, Direction.SOUTH, false));
            p.set(x, F + 1, 50, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.NORTH));
            p.set(x + 1, F + 1, 50, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.NORTH));
        }
        // the TV (news report) and the bar
        p.fill(lx + 6, F + 2, Z1 - 1, lx + 12, F + 4, Z1 - 1, Blocks.BLACK_CONCRETE);
        p.set(lx + 9, F + 3, Z1 - 2, facing(ModBlocks.LOUNGE_TV.get(), Direction.NORTH));
        p.fill(lx + 16, F + 1, 52, lx + 19, F + 1, 52, Blocks.DARK_OAK_PLANKS);
        p.fill(lx + 16, F + 2, 52, lx + 19, F + 2, 52, slab(Blocks.POLISHED_BLACKSTONE_SLAB, false));
        for (int x = lx + 16; x <= lx + 19; x++) p.frame(x, F + 3, Z1 - 1, Direction.NORTH, new ItemStack(x % 2 == 0 ? Items.GLASS_BOTTLE : Items.HONEY_BOTTLE), true);
        planter(p, lx + 1, 54);
        planter(p, lx + 18, 42);

        // the missing poster, on the pillar by the lounge
        p.fill(100, F + 1, 40, 100, F + 5, 40, Blocks.QUARTZ_PILLAR);
        p.set(100, F + 2, 39, facing(ModBlocks.MISSING_POSTER.get(), Direction.NORTH));
        // staff door to backstage (north wall)
        p.clear(105, F + 1, 0, 107, F + 4, 0);
        p.fill(105, F + 1, 0, 107, F + 4, 0, Blocks.WHITE_CONCRETE);
        p.door(106, F + 1, 0, Blocks.IRON_DOOR, Direction.SOUTH, false);
        p.sign(106, F + 4, 1, Direction.SOUTH, DyeColor.RED, "STAFF ONLY", "No entry", "beyond this", "point");
        p.set(107, F + 2, 1, facing(ModBlocks.KEYPAD.get(), Direction.SOUTH));
    }

    private static void table(Placer p, int x, int z) {
        p.set(x, F + 1, z, Blocks.DARK_OAK_FENCE);
        p.set(x, F + 2, z, Blocks.BIRCH_PRESSURE_PLATE);
        p.set(x - 1, F + 1, z, stair(Blocks.OAK_STAIRS, Direction.WEST, false));
        p.set(x + 1, F + 1, z, stair(Blocks.OAK_STAIRS, Direction.EAST, false));
    }

    // =========================================================================================
    // Concourse and gates: x 105..157
    // =========================================================================================
    private static void concourse(Placer p) {
        for (int x = 105; x < X1; x++)
            for (int z = 30; z < Z1; z++) p.set(x, F, z, (z == 30 || x % 16 == 0) ? Blocks.LIGHT_BLUE_WOOL.defaultBlockState() : Blocks.BLUE_WOOL.defaultBlockState());
        for (int x = 108; x <= 154; x++) {
            p.set(x, F, 24, Blocks.LIGHT_GRAY_CONCRETE);
            p.set(x, F + 1, 24, facing(ModBlocks.CONVEYOR_BELT.get(), Direction.EAST));
            p.set(x, F + 1, 23, Blocks.WHITE_STAINED_GLASS_PANE);
            p.set(x, F + 1, 25, Blocks.WHITE_STAINED_GLASS_PANE);
        }
        int[] gates = {Locations.GATE_B5, Locations.GATE_B6, Locations.GATE_B7};
        String[] names = {"B5", "B6", "B7"};
        String[][] info = {{"MA112", "SOLACE BAY", "DEPARTED"}, {"HJ240", "NEW HARROW", "BOARDING"}, {"MA417", "PORT ELLERY", "09:10"}};
        for (int i = 0; i < 3; i++) {
            int gx = gates[i];
            // podium and desk
            p.set(gx, F + 1, 52, facing(ModBlocks.BOARDING_SCANNER.get(), Direction.NORTH));
            p.fill(gx + 1, F + 1, 52, gx + 3, F + 1, 52, Blocks.LIGHT_GRAY_CONCRETE);
            p.fill(gx + 1, F + 2, 52, gx + 3, F + 2, 52, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
            p.set(gx + 2, F + 3, 52, Blocks.BLACK_STAINED_GLASS_PANE);
            // gate screen above the jet bridge door
            p.fill(gx - 3, F + 6, Z1, gx + 3, F + 8, Z1, Blocks.BLACK_CONCRETE);
            p.sign(gx - 2, F + 7, Z1 - 1, Direction.NORTH, DyeColor.WHITE, "GATE", names[i], "", "");
            p.sign(gx, F + 7, Z1 - 1, Direction.NORTH, DyeColor.YELLOW, info[i][0], info[i][1], "", "");
            p.sign(gx + 2, F + 7, Z1 - 1, Direction.NORTH, i == 2 ? DyeColor.LIME : DyeColor.YELLOW, "", info[i][2], "", "");
            p.set(gx - 3, F + 6, Z1 - 1, facing(ModBlocks.DEPARTURE_BOARD.get(), Direction.NORTH));
            // seating rows facing the windows
            for (int z : new int[]{36, 39, 44, 47}) {
                for (int dx = 1; dx <= 6; dx++) {
                    if (dx == 4) continue;
                    p.set(gx - dx, F + 1, z, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.SOUTH));
                    p.set(gx + dx, F + 1, z, facing(ModBlocks.AIRPORT_SEAT.get(), Direction.SOUTH));
                }
                p.set(gx - 4, F + 1, z, Blocks.SMOOTH_QUARTZ_SLAB);
                p.set(gx + 4, F + 1, z, Blocks.SMOOTH_QUARTZ_SLAB);
            }
            p.hangingSign(Blocks.DARK_OAK_HANGING_SIGN, gx, CEIL - 3, 33, Direction.NORTH, DyeColor.WHITE, "", "GATE " + names[i], "", "");
            p.hangingSign(Blocks.DARK_OAK_HANGING_SIGN, gx, CEIL - 3, 33, Direction.NORTH, DyeColor.WHITE, "", "GATE " + names[i], "", "");
            for (int y = CEIL - 1; y >= CEIL - 2; y--) p.set(gx, y, 33, Blocks.CHAIN);
            if (i < 2) {
                p.door(gx, F + 1, Z1, Blocks.IRON_DOOR, Direction.NORTH, false);
            }
        }
        // directions
        p.hangingSign(Blocks.DARK_OAK_HANGING_SIGN, 104, CEIL - 3, 27, Direction.WEST, DyeColor.WHITE, "GATES", "B5  B6  B7", "→", "");
        for (int y = CEIL - 1; y >= CEIL - 2; y--) p.set(104, y, 27, Blocks.CHAIN);
        planter(p, 106, 34);
        planter(p, 156, 34);
        planter(p, 140, 2);
        // charging stations and bins
        for (int x = 112; x <= 152; x += 10) {
            p.set(x, F + 1, 2, Blocks.CAULDRON);
            p.set(x + 2, F + 1, 2, facing(ModBlocks.SMILESCORE_KIOSK.get(), Direction.SOUTH));
        }
        p.set(124, F + 1, 2, facing(ModBlocks.VENDING_MACHINE.get(), Direction.SOUTH));
        // jet bridge door at B7
        p.clear(Locations.GATE_B7 - 1, F + 1, Z1, Locations.GATE_B7 + 1, F + 3, Z1);
        barrier(p, "jet_bridge", true);
    }

    // =========================================================================================
    private static void jetBridge(Placer p) {
        int x0 = Locations.GATE_B7 - 2, x1 = Locations.GATE_B7 + 2;
        int z0 = Z1 + 1, zEnd = Locations.GATE_PLANE.z() - 5;
        BlockState shell = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        for (int z = z0; z <= zEnd; z++) {
            int k = z - z0;
            int fy = F + Math.min(6, (k + 1) / 2);
            for (int x = x0; x <= x1; x++) {
                if (fy - 1 >= F + 1) p.fill(x, F + 1, z, x, fy - 1, z, shell);
                p.set(x, fy, z, x == x0 || x == x1 ? shell : Blocks.GRAY_WOOL.defaultBlockState());
            }
            for (int y = fy + 1; y <= F + 11; y++) {
                boolean win = z % 2 == 0 && y >= fy + 2 && y <= fy + 3;
                p.set(x0, y, z, win ? Blocks.GLASS_PANE.defaultBlockState() : shell);
                p.set(x1, y, z, win ? Blocks.GLASS_PANE.defaultBlockState() : shell);
                for (int x = x0 + 1; x < x1; x++) p.set(x, y, z, AIR);
            }
            for (int x = x0; x <= x1; x++) p.set(x, F + 12, z, shell);
            if (z % 4 == 0) p.set(Locations.GATE_B7, F + 12, z, Blocks.SEA_LANTERN);
            boolean stair = fy < F + 6 && (k + 2) / 2 > (k + 1) / 2;
            if (stair) for (int x = x0 + 1; x < x1; x++) p.set(x, fy + 1, z, stair(Blocks.POLISHED_ANDESITE_STAIRS, Direction.SOUTH, false));
        }
        for (int x = x0; x <= x1; x++) p.fill(x, F + 1, zEnd - 2, x, F + 1, zEnd - 2, Blocks.IRON_BLOCK.defaultBlockState());
        p.fill(Locations.GATE_B7, F + 1, zEnd - 2, Locations.GATE_B7, F + 3, zEnd - 2, Blocks.IRON_BLOCK);
    }

    // =========================================================================================
    // Backstage: behind the staff door, north of the airside plaza
    // =========================================================================================
    public static final int BX0 = 98, BX1 = 142, BZ0 = -36;

    private static void backstage(Placer p) {
        BlockState ply = Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState();
        BlockState beam = Blocks.SPRUCE_LOG.defaultBlockState();
        p.fill(BX0, F, BZ0, BX1, F, -1, Blocks.SMOOTH_STONE);
        for (int x = BX0; x <= BX1; x++)
            for (int z = BZ0; z <= -1; z++) {
                boolean edge = x == BX0 || x == BX1 || z == BZ0;
                for (int y = F + 1; y <= F + 12; y++) p.set(x, y, z, edge ? Blocks.GRAY_CONCRETE.defaultBlockState() : AIR);
                p.set(x, F + 13, z, Blocks.GRAY_CONCRETE);
            }
        for (int x = BX0 + 4; x < BX1; x += 8) for (int z = BZ0 + 4; z < -1; z += 8) p.hangingLight(x, F + 13, z, 3, Blocks.REDSTONE_LAMP);
        // corridor from the staff door
        for (int z = -8; z <= -1; z++) {
            p.set(104, F + 1, z, ply); p.set(104, F + 2, z, ply); p.set(104, F + 3, z, ply);
            p.set(108, F + 1, z, ply); p.set(108, F + 2, z, ply); p.set(108, F + 3, z, ply);
        }
        // the back of the terminal set: flats, braces and sandbags
        for (int x = BX0 + 2; x <= BX1 - 2; x += 3) {
            p.set(x, F + 1, -2, Blocks.SCAFFOLDING);
            p.set(x, F + 2, -2, Blocks.SCAFFOLDING);
        }
        // a spare departures board and a spare gate sign leaning on the wall
        p.fill(112, F + 1, -30, 118, F + 4, -30, Blocks.BLACK_CONCRETE);
        p.sign(115, F + 3, -29, Direction.SOUTH, DyeColor.YELLOW, "MA417", "PORT ELLERY", "ARRIVED", "(spare)");
        p.sign(113, F + 2, -29, Direction.SOUTH, DyeColor.WHITE, "GATE B7", "(TAKE 4)", "", "");
        // costume racks
        for (int i = 0; i < 4; i++) {
            p.mannequin(101.5 + i * 1.5, F + 1, -20.5, 90, i % 2 == 0 ? 0x1F3A7A : 0xB02E26, false);
        }
        p.sign(100, F + 3, -18, Direction.EAST, DyeColor.WHITE, "WARDROBE", "Sky Hosts", "Wigs: rotate!", "");
        // crates of props
        for (int[] c : new int[][]{{122, -10}, {123, -10}, {122, -11}, {126, -12}, {130, -8}, {131, -8}}) {
            p.set(c[0], F + 1, c[1], Blocks.BARREL);
            p.set(c[0], F + 2, c[1], c[0] % 2 == 0 ? Blocks.BARREL.defaultBlockState() : AIR);
        }
        p.sign(124, F + 2, -9, Direction.SOUTH, DyeColor.BLACK, "PROPS", "Suitcases x200", "Same tag", "");
        // lighting rigs
        for (int x = 110; x <= 138; x += 7) {
            p.fill(x, F + 1, -16, x, F + 6, -16, Blocks.IRON_BARS);
            p.set(x, F + 7, -16, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
            p.set(x, F + 7, -15, facing(Blocks.OBSERVER, Direction.SOUTH));
        }
        // control room
        int rx0 = 120, rx1 = 140, rz0 = -34, rz1 = -22;
        p.walls(rx0, F + 1, rz0, rx1, F + 5, rz1, ply);
        p.fill(rx0, F + 6, rz0, rx1, F + 6, rz1, beam);
        for (int x = rx0 + 2; x <= rx1 - 2; x++) {
            for (int y = F + 2; y <= F + 4; y++) {
                int mode = (x * 7 + y * 3) % 5 == 0 ? 1 : 0;
                p.set(x, y, rz0 + 1, facing(ModBlocks.MONITOR.get(), Direction.SOUTH).setValue(StateInteractBlock.MODE, mode));
            }
        }
        for (int x = rx0 + 2; x <= rx1 - 2; x++) {
            p.set(x, F + 1, rz0 + 3, Blocks.DARK_OAK_PLANKS);
            p.set(x, F + 2, rz0 + 3, slab(Blocks.POLISHED_BLACKSTONE_SLAB, false));
            if (x % 3 == 0) p.set(x, F + 1, rz0 + 5, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
        }
        p.door(130, F + 1, rz1, Blocks.SPRUCE_DOOR, Direction.SOUTH, false);
        p.sign(131, F + 3, rz1 + 1, Direction.SOUTH, DyeColor.RED, "CONTROL", "ON AIR", "● REC", "");
        p.clue(124, F + 3, rz0 + 3, ModBlocks.CLUE_NOTE.get(), "control_log");
        p.clue(136, F + 3, rz0 + 3, ModBlocks.CLUE_NOTE.get(), "crew_memo");
        p.clue(110, F + 1, -26, ModBlocks.CLUE_NOTE.get(), "lost_found");
        p.set(rx0 + 10, F + 2, rz0 + 3, Blocks.LECTERN);
        // a little office with the call sheet
        p.walls(100, F + 1, -34, 108, F + 4, -26, ply);
        p.door(104, F + 1, -26, Blocks.SPRUCE_DOOR, Direction.SOUTH, false);
        p.set(104, F + 1, -31, Blocks.DARK_OAK_PLANKS);
        p.clue(104, F + 2, -31, ModBlocks.CLUE_NOTE.get(), "call_sheet");
        p.set(106, F + 1, -33, Blocks.BOOKSHELF);
        p.set(107, F + 1, -33, Blocks.BOOKSHELF);
    }

    // =========================================================================================
    private static void tarmac(Placer p) {
        for (int x = -30; x <= 230; x++) {
            if (x % 6 < 3) p.set(x, F, 105, Blocks.YELLOW_CONCRETE);
            for (int z = 128; z <= 142; z++) p.set(x, F, z, Blocks.BLACK_CONCRETE);
            if (x % 10 < 5) p.set(x, F, 135, Blocks.WHITE_CONCRETE);
            if (x % 12 == 0) { p.set(x, F, 127, Blocks.SEA_LANTERN); p.set(x, F, 143, Blocks.SEA_LANTERN); }
        }
        for (int z = 57; z <= 104; z++) if (z % 4 < 2) p.set(124, F, z, Blocks.YELLOW_CONCRETE);
        // baggage carts and cones near B7
        for (int i = 0; i < 3; i++) {
            int x = 118 + i * 4;
            p.fill(x, F + 1, 64, x + 2, F + 1, 65, Blocks.GRAY_CONCRETE);
            p.set(x, F + 2, 64, ModBlocks.SUITCASE.get().defaultBlockState());
            p.set(x + 2, F + 2, 65, ModBlocks.SUITCASE.get().defaultBlockState());
            p.set(x + 1, F + 2, 64, ModBlocks.SUITCASE.get().defaultBlockState());
        }
        p.fill(112, F + 1, 64, 114, F + 2, 65, Blocks.YELLOW_CONCRETE);
        p.set(113, F + 3, 64, Blocks.GRAY_STAINED_GLASS);
        for (int[] c : new int[][]{{132, 60}, {134, 66}, {130, 72}, {205, 75}, {204, 80}}) p.set(c[0], F + 1, c[1], Blocks.ORANGE_CONCRETE_POWDER);
        // fuel truck
        p.fill(196, F + 1, 92, 206, F + 3, 95, Blocks.WHITE_CONCRETE);
        p.fill(193, F + 1, 92, 195, F + 3, 95, Blocks.RED_CONCRETE);
        p.fill(193, F + 3, 93, 194, F + 3, 94, Blocks.GLASS);
        // another Meridian jet waiting at the far end of the taxiway
        PlaneBuilder.exterior(p, new Locations.Plane(10, F + 6, 115));
    }

    /** Glass barriers the story opens as you progress. */
    public static void barrier(Placer p, String name, boolean closed) {
        BlockState s = closed ? ModBlocks.STORY_BARRIER.get().defaultBlockState() : AIR;
        switch (name) {
            case "to_security" -> p.fill(52, F + 1, 18, 52, F + 4, 34, s);
            case "to_airside" -> p.fill(76, F + 1, 18, 76, F + 4, 34, s);
            case "jet_bridge" -> p.fill(Locations.GATE_B7 - 1, F + 1, Z1, Locations.GATE_B7 + 1, F + 3, Z1, s);
            default -> {}
        }
    }

    public static void staffDoor(Placer p, boolean open) {
        for (int y = F + 1; y <= F + 2; y++) {
            BlockState s = p.get(106, y, 0);
            if (s.hasProperty(net.minecraft.world.level.block.DoorBlock.OPEN)) p.set(106, y, 0, s.setValue(net.minecraft.world.level.block.DoorBlock.OPEN, open));
        }
    }

    /** Switch every departures-board story block between normal (0) and glitched (1). */
    public static void boards(Placer p, int mode) {
        Block b = ModBlocks.DEPARTURE_BOARD.get();
        for (int x = 0; x <= X1; x++)
            for (int z : new int[]{15, 17, Z1 - 1})
                for (int y = F + 1; y <= F + 7; y++) {
                    BlockState s = p.get(x, y, z);
                    if (s.is(b)) p.set(x, y, z, s.setValue(StateInteractBlock.MODE, mode));
                }
        if (mode == 1) {
            for (int face = 0; face < 2; face++) {
                Direction d = face == 0 ? Direction.NORTH : Direction.SOUTH;
                int z = face == 0 ? 15 : 17;
                int[] xs = face == 0 ? new int[]{30, 28, 26, 24} : new int[]{22, 24, 26, 28};
                for (int r = 0; r < 6; r++) {
                    int y = F + 8 - r;
                    p.sign(xs[0], y, z, d, DyeColor.RED, "", "MA417", "", "");
                    p.sign(xs[1], y, z, d, DyeColor.RED, "", "PORT ELLERY", "", "");
                    p.sign(xs[2], y, z, d, DyeColor.RED, "", "09:10  B7", "", "");
                    p.sign(xs[3], y, z, d, DyeColor.RED, "", r == 5 ? "LAST CALL" : "ARRIVED", "", "");
                }
            }
        }
    }
}
