package com.airlinestory.world;

import com.airlinestory.block.StateInteractBlock;
import com.airlinestory.registry.ModBlocks;
import com.airlinestory.story.Locations;
import com.airlinestory.story.StoryState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.airlinestory.world.IslandTerrain.*;
import static com.airlinestory.world.Placer.facing;
import static com.airlinestory.world.Placer.leaves;

/**
 * Generates the island a little at a time (it is built in the background while you are still at the
 * airport). Phases: 0 terrain, 1 plants and trees, 2 structures, 3 done. Progress is saved.
 */
public final class IslandBuilder {
    public static final int EXTENT = 150;
    private final List<ChunkPos> chunks = new ArrayList<>();
    private final StoryState state;

    public IslandBuilder(StoryState state) {
        this.state = state;
        int cx0 = (Locations.ISLAND_X - EXTENT) >> 4, cx1 = (Locations.ISLAND_X + EXTENT) >> 4;
        int cz0 = (Locations.ISLAND_Z - EXTENT) >> 4, cz1 = (Locations.ISLAND_Z + EXTENT) >> 4;
        for (int cx = cx0; cx <= cx1; cx++)
            for (int cz = cz0; cz <= cz1; cz++) chunks.add(new ChunkPos(cx, cz));
    }

    public boolean done() {
        return state.counter("island_phase") >= 3;
    }

    public int percent() {
        int phase = state.counter("island_phase");
        if (phase >= 3) return 100;
        int idx = state.counter("island_idx");
        return (int) ((phase * chunks.size() + idx) * 100L / (2L * chunks.size() + 1));
    }

    /** Works until the deadline. Returns true when finished. */
    public boolean step(ServerLevel level, long deadline) {
        Placer p = new Placer(level);
        while (System.nanoTime() < deadline) {
            int phase = state.counter("island_phase");
            int idx = state.counter("island_idx");
            if (phase >= 3) return true;
            if (phase < 2) {
                if (idx >= chunks.size()) {
                    state.setCounter("island_phase", phase + 1);
                    state.setCounter("island_idx", 0);
                    continue;
                }
                ChunkPos c = chunks.get(idx);
                if (phase == 0) terrainChunk(p, c);
                else decorChunk(p, c);
                state.setCounter("island_idx", idx + 1);
            } else {
                structures(p);
                state.setCounter("island_phase", 3);
                return true;
            }
        }
        return done();
    }

    // =========================================================================================
    // Phase 0: terrain
    // =========================================================================================
    private void terrainChunk(Placer p, ChunkPos c) {
        int poolWater = poolSurface() - 1;
        for (int x = c.getMinBlockX(); x <= c.getMaxBlockX(); x++) {
            for (int z = c.getMinBlockZ(); z <= c.getMaxBlockZ(); z++) {
                int dx = x - Locations.ISLAND_X, dz = z - Locations.ISLAND_Z;
                double r = radial(dx, dz);
                if (r > 1.35) continue;
                int h = height(dx, dz);
                if (h <= SEA) {
                    for (int y = SEA_FLOOR + 1; y <= h; y++) p.set(x, y, z, Blocks.SAND);
                    continue;
                }
                boolean pool = isPool(dx, dz);
                if (pool) {
                    double pd = dist(dx, dz, Locations.POOL);
                    int floor = poolWater - 3 + (int) (pd * 0.4);
                    for (int y = SEA_FLOOR + 1; y <= floor; y++) p.set(x, y, z, y >= floor - 1 ? Blocks.CLAY : Blocks.STONE);
                    for (int y = floor + 1; y <= poolWater; y++) p.set(x, y, z, Blocks.WATER);
                    continue;
                }
                boolean beach = beach(dx, dz, h);
                boolean grove = dist(dx, dz, Locations.GROVE) < 20;
                boolean high = h > 96;
                for (int y = SEA_FLOOR + 1; y <= h; y++) {
                    Block b;
                    if (y == h) {
                        if (beach) b = Blocks.SAND;
                        else if (high) b = hash(x, z, 5) < 0.3 ? Blocks.GRAVEL : Blocks.STONE;
                        else if (h > 88) b = hash(x, z, 6) < 0.5 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK;
                        else if (grove) b = hash(x, z, 7) < 0.55 ? Blocks.PODZOL : Blocks.COARSE_DIRT;
                        else b = Blocks.GRASS_BLOCK;
                    } else if (y >= h - 3) {
                        b = beach ? Blocks.SAND : high ? Blocks.STONE : Blocks.DIRT;
                    } else if (beach && y >= h - 5) {
                        b = Blocks.SANDSTONE;
                    } else {
                        b = hash(x, y * 31 + z, 9) < 0.04 ? Blocks.COAL_ORE : hash(x, y * 17 + z, 10) < 0.015 ? Blocks.IRON_ORE : Blocks.STONE;
                    }
                    p.set(x, y, z, b);
                }
            }
        }
    }

    // =========================================================================================
    // Phase 1: vegetation
    // =========================================================================================
    private boolean reserved(int dx, int dz) {
        return dist(dx, dz, Locations.VILLAGE) < 24 || dist(dx, dz, Locations.CRASH) < 22 || dist(dx, dz, Locations.PEAK) < 12
                || dist(dx, dz, Locations.POOL) < 9 || dist(dx, dz, Locations.LONE_SEAT) < 5 || (dx > 70 && Math.abs(dz - Locations.DOCK[1]) < 9);
    }

    private void decorChunk(Placer p, ChunkPos c) {
        for (int x = c.getMinBlockX(); x <= c.getMaxBlockX(); x++) {
            for (int z = c.getMinBlockZ(); z <= c.getMaxBlockZ(); z++) {
                int dx = x - Locations.ISLAND_X, dz = z - Locations.ISLAND_Z;
                if (radial(dx, dz) >= 1) continue;
                int h = height(dx, dz);
                if (h <= SEA || isPool(dx, dz)) continue;
                double roll = hash(x, z, 101);
                double roll2 = hash(x, z, 202);
                boolean grove = dist(dx, dz, Locations.GROVE) < 20;
                if (grove) {
                    if (dist(dx, dz, Locations.GROVE) < 9) continue;
                    if (roll < 1 / 40.0) deadTree(p, x, h + 1, z, roll2);
                    else if (roll < 1 / 14.0) p.set(x, h + 1, z, ModBlocks.GHOST_ORCHID.get());
                    else if (roll < 1 / 7.0) p.set(x, h + 1, z, Blocks.DEAD_BUSH);
                    continue;
                }
                if (reserved(dx, dz)) {
                    if (roll < 0.12 && !beach(dx, dz, h) && dist(dx, dz, Locations.PEAK) > 12) p.set(x, h + 1, z, Blocks.GRASS);
                    continue;
                }
                boolean beach = beach(dx, dz, h);
                if (h > 96) continue;
                if (beach) {
                    if (roll < 1 / 45.0) palm(p, x, h + 1, z, roll2);
                    else if (roll < 1 / 30.0 && radial(dx, dz) < 0.97) p.set(x, h + 1, z, Blocks.DEAD_BUSH);
                    continue;
                }
                if (roll < 1 / 65.0) jungleTree(p, x, h + 1, z, roll2);
                else if (roll < 1 / 50.0) palm(p, x, h + 1, z, roll2);
                else if (roll < 1 / 22.0) bush(p, x, h + 1, z, roll2);
                else if (roll < 1 / 17.0) p.set(x, h + 1, z, ModBlocks.BITTERLEAF.get());
                else if (roll < 1 / 14.0 && h < 72) p.set(x, h + 1, z, ModBlocks.TARO_PLANT.get());
                else if (roll < 0.30) p.set(x, h + 1, z, Blocks.GRASS);
                else if (roll < 0.40) p.set(x, h + 1, z, Blocks.FERN);
                else if (roll < 0.405) p.set(x, h + 1, z, Blocks.POPPY);
                else if (roll < 0.41) p.set(x, h + 1, z, Blocks.AZURE_BLUET);
            }
        }
    }

    public static void palm(Placer p, int x, int y, int z, double seed) {
        Random r = new Random((long) (seed * 1e9));
        int height = 5 + r.nextInt(4);
        int lx = 0, lz = 0;
        Direction lean = Direction.from2DDataValue(r.nextInt(4));
        BlockState log = ModBlocks.PALM_LOG.get().defaultBlockState();
        for (int i = 0; i < height; i++) {
            if (i == height / 2 + 1) { lx += lean.getStepX(); lz += lean.getStepZ(); }
            p.set(x + lx, y + i, z + lz, log);
        }
        int tx = x + lx, ty = y + height, tz = z + lz;
        BlockState leaf = leaves(ModBlocks.PALM_LEAVES.get());
        p.set(tx, ty, tz, leaf);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int k = 1; k <= 3; k++) p.set(tx + d.getStepX() * k, ty - (k == 3 ? 1 : 0), tz + d.getStepZ() * k, leaf);
            p.set(tx + d.getStepX() * 3, ty - 2, tz + d.getStepZ() * 3, leaf);
        }
        for (int[] dg : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            p.set(tx + dg[0], ty, tz + dg[1], leaf);
            p.set(tx + dg[0] * 2, ty - 1, tz + dg[1] * 2, leaf);
        }
        int nuts = r.nextInt(3);
        for (int i = 0; i < nuts; i++) {
            Direction d = Direction.from2DDataValue(r.nextInt(4));
            p.set(tx + d.getStepX(), ty - 1, tz + d.getStepZ(), facing(ModBlocks.COCONUT.get(), d));
        }
    }

    private static void jungleTree(Placer p, int x, int y, int z, double seed) {
        Random r = new Random((long) (seed * 1e9));
        int height = 6 + r.nextInt(5);
        BlockState log = Blocks.JUNGLE_LOG.defaultBlockState();
        for (int i = 0; i < height; i++) p.set(x, y + i, z, log);
        BlockState leaf = leaves(Blocks.JUNGLE_LEAVES);
        int top = y + height;
        for (int ox = -3; ox <= 3; ox++)
            for (int oy = -2; oy <= 1; oy++)
                for (int oz = -3; oz <= 3; oz++) {
                    double d = ox * ox + oz * oz + oy * oy * 2.5;
                    if (d <= 10 + r.nextInt(3) && !(ox == 0 && oz == 0 && oy < 0)) {
                        if (p.get(x + ox, top + oy, z + oz).isAir()) p.set(x + ox, top + oy, z + oz, leaf);
                    }
                }
        for (int i = 0; i < 3; i++) {
            Direction d = Direction.from2DDataValue(r.nextInt(4));
            int vy = top - 2 - r.nextInt(3);
            int vx = x + d.getStepX() * 3, vz = z + d.getStepZ() * 3;
            for (int k = 0; k < 3 && p.get(vx, vy - k, vz).isAir(); k++) p.set(vx, vy - k, vz, Blocks.VINE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(d.getOpposite()), true));
        }
    }

    private static void bush(Placer p, int x, int y, int z, double seed) {
        BlockState leaf = leaves(seed < 0.5 ? Blocks.JUNGLE_LEAVES : Blocks.OAK_LEAVES);
        p.set(x, y, z, Blocks.JUNGLE_LOG.defaultBlockState());
        for (Direction d : Direction.Plane.HORIZONTAL) p.set(x + d.getStepX(), y, z + d.getStepZ(), leaf);
        p.set(x, y + 1, z, leaf);
    }

    private static void deadTree(Placer p, int x, int y, int z, double seed) {
        Random r = new Random((long) (seed * 1e9));
        int height = 4 + r.nextInt(4);
        BlockState log = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
        for (int i = 0; i < height; i++) p.set(x, y + i, z, log);
        for (int i = 0; i < 2; i++) {
            Direction d = Direction.from2DDataValue(r.nextInt(4));
            int by = y + height - 1 - r.nextInt(2);
            BlockState branch = log.setValue(RotatedPillarBlock.AXIS, d.getAxis());
            p.set(x + d.getStepX(), by, z + d.getStepZ(), branch);
            p.set(x + d.getStepX() * 2, by + 1, z + d.getStepZ() * 2, branch);
        }
    }

    // =========================================================================================
    // Phase 2: structures
    // =========================================================================================
    private static int ground(int dx, int dz) {
        return height(dx, dz);
    }

    private static int wx(int dx) { return Locations.ISLAND_X + dx; }
    private static int wz(int dz) { return Locations.ISLAND_Z + dz; }

    private void structures(Placer p) {
        crashSite(p);
        loneSeat(p);
        village(p);
        dock(p);
        station(p);
        grove(p);
        poolRim(p);
        state.setCounter("y:crash", ground(Locations.CRASH[0], Locations.CRASH[1]) + 1);
        state.setCounter("y:pool", poolSurface());
        state.setCounter("y:village", ground(Locations.VILLAGE[0], Locations.VILLAGE[1]) + 1);
        state.setCounter("y:grove", ground(Locations.GROVE[0], Locations.GROVE[1]) + 1);
        state.setCounter("y:peak", (int) SUMMIT + 1);
        state.setCounter("y:lab", (int) SUMMIT + 1);
        state.setCounter("y:dock", SEA + 2);
        state.setCounter("x:dock", wx(shoreX() + 8));
        state.setCounter("y:summit", (int) SUMMIT);
    }

    // ---------- crash site ----------
    private void crashSite(Placer p) {
        int cx = Locations.CRASH[0], cz = Locations.CRASH[1];
        Random r = new Random(417);
        BlockState fus = ModBlocks.FUSELAGE.get().defaultBlockState();
        BlockState stripe = ModBlocks.FUSELAGE_STRIPE.get().defaultBlockState();
        BlockState window = ModBlocks.AIRPLANE_WINDOW.get().defaultBlockState();
        BlockState wreck = ModBlocks.WRECKAGE.get().defaultBlockState();
        // forward section, lying on the sand
        int g = ground(cx - 10, cz - 3);
        for (int dx = cx - 18; dx <= cx - 5; dx++) {
            int x = wx(dx);
            boolean broken = dx >= cx - 7;
            for (int dz = cz - 6; dz <= cz; dz++) {
                int z = wz(dz);
                int rel = dz - (cz - 3);
                for (int y = g; y <= g + 5; y++) {
                    int ry = y - g;
                    boolean shell = Math.abs(rel) == 3 || ry == 0 || ry == 5;
                    if (Math.abs(rel) == 3 && (ry == 0 || ry == 5)) continue;
                    if (shell) {
                        if (broken && r.nextFloat() < 0.45f) { p.set(x, y, z, r.nextBoolean() ? wreck : Blocks.AIR.defaultBlockState()); continue; }
                        BlockState s = ry == 1 ? stripe : (ry == 2 && dx % 2 == 0) ? window : fus;
                        if (ry == 0) s = ModBlocks.CABIN_FLOOR.get().defaultBlockState();
                        p.set(x, y, z, s);
                    } else {
                        p.set(x, y, z, Blocks.AIR.defaultBlockState());
                    }
                }
            }
            if (dx % 2 == 0 && dx > cx - 17 && !broken) {
                for (int dz : new int[]{cz - 5, cz - 4, cz - 2, cz - 1}) p.set(wx(dx), g + 1, wz(dz), facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.WEST));
            }
        }
        p.fill(wx(cx - 19), g, wz(cz - 5), wx(cx - 19), g + 4, wz(cz - 1), fus);
        p.fill(wx(cx - 19), g + 2, wz(cz - 4), wx(cx - 19), g + 3, wz(cz - 2), Blocks.BLACK_STAINED_GLASS.defaultBlockState());
        // tail section further east, fin up
        int g2 = ground(cx + 12, cz + 2);
        for (int dx = cx + 6; dx <= cx + 16; dx++) {
            for (int dz = cz; dz <= cz + 4; dz++) {
                int rel = dz - (cz + 2);
                for (int y = g2; y <= g2 + 4; y++) {
                    int ry = y - g2;
                    boolean shell = Math.abs(rel) == 2 || ry == 0 || ry == 4;
                    if (dx == cx + 6 && r.nextFloat() < 0.5f) { p.set(wx(dx), y, wz(dz), wreck); continue; }
                    p.set(wx(dx), y, wz(dz), shell ? (ry == 1 ? stripe : fus) : Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int h = 0; h < 8; h++)
            for (int dx = cx + 10 + h / 2; dx <= cx + 16; dx++) p.set(wx(dx), g2 + 5 + h, wz(cz + 2), stripe);
        // a wing in the shallows
        for (int dx = cx - 6; dx <= cx + 4; dx++)
            for (int dz = cz + 13; dz <= cz + 16 - Math.max(0, (dx - cx)) / 3; dz++) {
                int gy = Math.max(ground(dx, dz), SEA - 1);
                p.set(wx(dx), gy + (gy < SEA ? 1 : 1), wz(dz), fus);
            }
        // an engine on the sand
        int ge = ground(cx - 2, cz + 7);
        for (int dx = cx - 4; dx <= cx + 1; dx++)
            for (int y = ge + 1; y <= ge + 3; y++)
                for (int dz = cz + 6; dz <= cz + 8; dz++)
                    p.set(wx(dx), y, wz(dz), (y == ge + 2 && dz == cz + 7) ? Blocks.BLACK_CONCRETE.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        // debris, luggage, seats, fires
        scatter(p, r, cx, cz, 18, 46, s -> wreck);
        int luggage = 0;
        for (int i = 0; i < 60 && luggage < 16; i++) {
            int dx = cx + r.nextInt(33) - 16, dz = cz + r.nextInt(21) - 10;
            int h = ground(dx, dz);
            if (h <= SEA || !p.get(wx(dx), h + 1, wz(dz)).isAir()) continue;
            p.set(wx(dx), h + 1, wz(dz), facing(ModBlocks.SUITCASE.get(), Direction.from2DDataValue(r.nextInt(4))).setValue(StateInteractBlock.MODE, 0));
            luggage++;
        }
        for (int i = 0; i < 12; i++) {
            int dx = cx + r.nextInt(31) - 15, dz = cz + r.nextInt(17) - 8;
            int h = ground(dx, dz);
            if (h <= SEA || !p.get(wx(dx), h + 1, wz(dz)).isAir()) continue;
            p.set(wx(dx), h + 1, wz(dz), facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.from2DDataValue(r.nextInt(4))));
        }
        for (int[] f : new int[][]{{cx - 3, cz - 8}, {cx + 4, cz + 3}, {cx + 18, cz - 2}}) {
            int h = ground(f[0], f[1]);
            p.set(wx(f[0]), h, wz(f[1]), Blocks.HAY_BLOCK);
            p.set(wx(f[0]), h + 1, wz(f[1]), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true).setValue(CampfireBlock.LIT, true));
        }
    }

    private interface StateFn { BlockState get(Random r); }

    private void scatter(Placer p, Random r, int cx, int cz, int radius, int count, StateFn fn) {
        for (int i = 0; i < count; i++) {
            int dx = cx + r.nextInt(radius * 2 + 1) - radius, dz = cz + r.nextInt(radius + 1) - radius / 2;
            int h = ground(dx, dz);
            if (h <= SEA - 1) continue;
            if (!p.get(wx(dx), h + 1, wz(dz)).isAir()) continue;
            p.set(wx(dx), h + 1, wz(dz), fn.get(r));
            if (r.nextFloat() < 0.3f && p.get(wx(dx), h + 2, wz(dz)).isAir()) p.set(wx(dx), h + 2, wz(dz), fn.get(r));
        }
    }

    public static BlockPos loneSeatPos() {
        int dx = Locations.LONE_SEAT[0], dz = Locations.LONE_SEAT[1];
        return new BlockPos(wx(dx), Math.max(SEA, ground(dx, dz)) + 1, wz(dz));
    }

    private void loneSeat(Placer p) {
        BlockPos s = loneSeatPos();
        p.set(s.getX(), s.getY() - 1, s.getZ(), Blocks.SAND);
        p.set(s, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.SOUTH));
        state.setCounter("y:lone_seat", s.getY());
    }

    public static BlockPos wakePos() {
        int dx = Locations.CRASH[0] + 6, dz = Locations.CRASH[1] + 6;
        return new BlockPos(wx(dx), Math.max(SEA, ground(dx, dz)) + 1, wz(dz));
    }

    // ---------- village ----------
    public static int villageY() {
        return ground(Locations.VILLAGE[0], Locations.VILLAGE[1]);
    }

    public static BlockPos village(int ox, int oz) {
        return new BlockPos(wx(Locations.VILLAGE[0] + ox), villageY() + 1, wz(Locations.VILLAGE[1] + oz));
    }

    private void village(Placer p) {
        int vx = wx(Locations.VILLAGE[0]), vz = wz(Locations.VILLAGE[1]);
        int vy = villageY();
        // bonfire
        p.set(vx, vy, vz, Blocks.HAY_BLOCK);
        p.set(vx, vy + 1, vz, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true).setValue(CampfireBlock.LIT, true));
        for (int[] s : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
            Direction.Axis ax = s[0] == 0 ? Direction.Axis.X : Direction.Axis.Z;
            p.set(vx + s[0], vy + 1, vz + s[1], ModBlocks.PALM_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, ax));
        }
        // paths
        for (int k = 4; k <= 12; k++) {
            p.set(vx + k, vy, vz, Blocks.DIRT_PATH);
            p.set(vx - k, vy, vz, Blocks.DIRT_PATH);
            p.set(vx, vy, vz + k, Blocks.DIRT_PATH);
            p.set(vx, vy, vz - k, Blocks.DIRT_PATH);
        }
        hut(p, vx - 10, vy, vz - 8, Direction.SOUTH);
        hut(p, vx + 10, vy, vz - 9, Direction.SOUTH);
        hut(p, vx - 12, vy, vz + 6, Direction.EAST); // Mira
        hut(p, vx + 11, vy, vz + 8, Direction.NORTH);
        hut(p, vx - 1, vy, vz + 14, Direction.NORTH);
        longhouse(p, vx, vy, vz - 15);
        // totems
        for (int[] t : new int[][]{{7, 7}, {-7, 7}, {7, -7}, {-7, -7}}) {
            Direction face = Math.abs(t[0]) > 0 ? (t[1] > 0 ? Direction.NORTH : Direction.SOUTH) : Direction.NORTH;
            for (int y = 1; y <= 3; y++) p.set(vx + t[0], vy + y, vz + t[1], facing(ModBlocks.TOTEM.get(), face));
        }
        // Mira's drying racks and herbs
        for (int k = 0; k < 3; k++) {
            p.set(vx - 8, vy + 1, vz + 9 + k, Blocks.BAMBOO_FENCE);
            p.set(vx - 8, vy + 2, vz + 9 + k, ModBlocks.THATCH.get().defaultBlockState());
        }
        p.set(vx - 9, vy + 1, vz + 4, Blocks.BREWING_STAND);
        // shrine of seats facing the fire, oxygen masks hanging like charms
        for (int k = -1; k <= 1; k++) {
            p.set(vx + k, vy + 1, vz - 6, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.SOUTH));
            p.set(vx + k, vy + 3, vz - 6, Blocks.BAMBOO_FENCE);
            p.set(vx + k, vy + 2, vz - 7, Blocks.BAMBOO_FENCE);
        }
        p.set(vx - 2, vy + 1, vz - 6, Blocks.BAMBOO_FENCE);
        p.set(vx - 2, vy + 2, vz - 6, Blocks.BAMBOO_FENCE);
        p.set(vx + 2, vy + 1, vz - 6, Blocks.BAMBOO_FENCE);
        p.set(vx + 2, vy + 2, vz - 6, Blocks.BAMBOO_FENCE);
        p.fill(vx - 2, vy + 3, vz - 6, vx + 2, vy + 3, vz - 6, ModBlocks.THATCH.get().defaultBlockState());
        for (int[] l : new int[][]{{5, 5}, {-5, 5}, {5, -5}, {-5, -5}}) {
            p.set(vx + l[0], vy + 1, vz + l[1], Blocks.BAMBOO_FENCE);
            p.set(vx + l[0], vy + 2, vz + l[1], Blocks.LANTERN);
        }
    }

    private void hut(Placer p, int cx, int y, int cz, Direction door) {
        BlockState wall = Blocks.BAMBOO_PLANKS.defaultBlockState();
        BlockState post = Blocks.BAMBOO_BLOCK.defaultBlockState();
        BlockState thatch = ModBlocks.THATCH.get().defaultBlockState();
        p.fill(cx - 2, y, cz - 2, cx + 2, y, cz + 2, ModBlocks.PALM_PLANKS.get().defaultBlockState());
        for (int yy = y + 1; yy <= y + 3; yy++) {
            for (int x = cx - 2; x <= cx + 2; x++)
                for (int z = cz - 2; z <= cz + 2; z++) {
                    boolean edge = x == cx - 2 || x == cx + 2 || z == cz - 2 || z == cz + 2;
                    boolean corner = (x == cx - 2 || x == cx + 2) && (z == cz - 2 || z == cz + 2);
                    p.set(x, yy, z, edge ? (corner ? post : wall) : Blocks.AIR.defaultBlockState());
                }
        }
        p.set(cx + door.getStepX() * 2, y + 1, cz + door.getStepZ() * 2, Blocks.AIR.defaultBlockState());
        p.set(cx + door.getStepX() * 2, y + 2, cz + door.getStepZ() * 2, Blocks.AIR.defaultBlockState());
        p.set(cx - door.getStepX() * 2, y + 2, cz - door.getStepZ() * 2, Blocks.AIR.defaultBlockState());
        for (int k = 0; k <= 3; k++) p.fill(cx - 3 + k, y + 4 + k, cz - 3 + k, cx + 3 - k, y + 4 + k, cz + 3 - k, thatch);
        p.set(cx - door.getStepX(), y + 1, cz - door.getStepZ(), Blocks.WHITE_CARPET);
        p.set(cx + door.getStepZ(), y + 1, cz + door.getStepX(), Blocks.BARREL);
    }

    private void longhouse(Placer p, int cx, int y, int cz) {
        BlockState wall = ModBlocks.PALM_PLANKS.get().defaultBlockState();
        BlockState post = ModBlocks.PALM_LOG.get().defaultBlockState();
        BlockState thatch = ModBlocks.THATCH.get().defaultBlockState();
        p.fill(cx - 6, y, cz - 3, cx + 6, y, cz + 3, wall);
        for (int yy = y + 1; yy <= y + 4; yy++)
            for (int x = cx - 6; x <= cx + 6; x++)
                for (int z = cz - 3; z <= cz + 3; z++) {
                    boolean edge = x == cx - 6 || x == cx + 6 || z == cz - 3 || z == cz + 3;
                    boolean postPos = edge && (x - cx) % 3 == 0 && (z == cz - 3 || z == cz + 3);
                    p.set(x, yy, z, edge ? (postPos ? post : wall) : Blocks.AIR.defaultBlockState());
                }
        p.clear(cx - 1, y + 1, cz + 3, cx + 1, y + 3, cz + 3);
        for (int k = 0; k <= 4; k++) p.fill(cx - 7, y + 5 + k, cz - 4 + k, cx + 7, y + 5 + k, cz + 4 - k, thatch);
        p.set(cx, y + 1, cz - 2, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.SOUTH));
        p.set(cx - 3, y + 1, cz - 2, facing(ModBlocks.TOTEM.get(), Direction.SOUTH));
        p.set(cx + 3, y + 1, cz - 2, facing(ModBlocks.TOTEM.get(), Direction.SOUTH));
        p.set(cx - 5, y + 1, cz, Blocks.WHITE_CARPET);
        p.set(cx + 5, y + 1, cz, Blocks.LIGHT_BLUE_CARPET);
        p.set(cx, y + 4, cz, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
    }

    // ---------- dock and boat ----------
    public static int shoreX() {
        int z = Locations.DOCK[1];
        for (int dx = Locations.VILLAGE[0] + 10; dx < 150; dx++) {
            if (height(dx, z) <= SEA) return dx;
        }
        return Locations.DOCK[0] - 8;
    }

    /** Origin (stern, port corner, water level) of the boat that is built beside the pier. */
    public static BlockPos boatOrigin() {
        return new BlockPos(wx(shoreX() + 4), SEA, wz(Locations.DOCK[1] + 4));
    }

    public static BlockPos boatFramePos() {
        return new BlockPos(wx(shoreX() + 13), SEA + 2, wz(Locations.DOCK[1]));
    }

    public static BlockPos dockStart() {
        int dx = shoreX() - 2;
        return new BlockPos(wx(dx), Math.max(SEA + 2, height(dx, Locations.DOCK[1]) + 1), wz(Locations.DOCK[1]));
    }

    private void dock(Placer p) {
        int start = shoreX() - 3;
        int end = shoreX() + 16;
        BlockState plank = ModBlocks.PALM_PLANKS.get().defaultBlockState();
        for (int dx = start; dx <= end; dx++) {
            for (int dz = Locations.DOCK[1] - 1; dz <= Locations.DOCK[1] + 1; dz++) {
                p.set(wx(dx), SEA + 1, wz(dz), plank);
                for (int y = SEA + 2; y <= SEA + 4; y++) p.set(wx(dx), y, wz(dz), Blocks.AIR.defaultBlockState());
            }
            if ((dx - start) % 4 == 0) {
                for (int dz : new int[]{Locations.DOCK[1] - 2, Locations.DOCK[1] + 2}) {
                    for (int y = SEA_FLOOR; y <= SEA + 2; y++) p.set(wx(dx), y, wz(dz), ModBlocks.PALM_LOG.get().defaultBlockState());
                }
            }
        }
        BlockPos frame = boatFramePos();
        p.set(frame, facing(ModBlocks.BOAT_FRAME.get(), Direction.SOUTH));
        p.set(frame.getX(), frame.getY() + 1, frame.getZ(), Blocks.LANTERN);
        p.set(frame.getX() - 1, frame.getY(), frame.getZ() - 1, Blocks.BAMBOO_FENCE);
        p.set(frame.getX() - 1, frame.getY() + 1, frame.getZ() - 1, Blocks.LANTERN);
    }

    /** Places the visible part of boat stage {@code stage} (1..5). */
    public static void boatStage(Placer p, int stage) {
        BlockPos o = boatOrigin();
        int x0 = o.getX(), y = o.getY(), z0 = o.getZ();
        BlockState log = ModBlocks.PALM_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState plank = ModBlocks.PALM_PLANKS.get().defaultBlockState();
        switch (stage) {
            case 1 -> {
                for (int x = 0; x <= 11; x++) p.set(x0 + x, y, z0 + 2, log);
                for (int x = 2; x <= 9; x++) { p.set(x0 + x, y, z0 + 1, log); p.set(x0 + x, y, z0 + 3, log); }
                for (int z = z0 - 3; z < z0; z++) p.set(x0 + 6, y + 1, z, plank);
            }
            case 2 -> {
                for (int x = 1; x <= 10; x++) for (int z = 1; z <= 3; z++) p.set(x0 + x, y, z0 + z, plank);
                for (int x = 2; x <= 9; x++) {
                    p.set(x0 + x, y, z0, plank);
                    p.set(x0 + x, y, z0 + 4, plank);
                    p.set(x0 + x, y + 1, z0, x % 3 == 0 ? ModBlocks.FUSELAGE_STRIPE.get().defaultBlockState() : ModBlocks.FUSELAGE.get().defaultBlockState());
                    p.set(x0 + x, y + 1, z0 + 4, x % 3 == 0 ? ModBlocks.FUSELAGE_STRIPE.get().defaultBlockState() : ModBlocks.FUSELAGE.get().defaultBlockState());
                }
                for (int z = 1; z <= 3; z++) { p.set(x0 + 1, y + 1, z0 + z, plank); p.set(x0 + 10, y + 1, z0 + z, plank); }
                p.set(x0 + 11, y + 1, z0 + 2, plank);
                p.set(x0 + 6, y + 1, z0, Blocks.AIR.defaultBlockState());
            }
            case 3 -> {
                BlockState mast = ModBlocks.PALM_LOG.get().defaultBlockState();
                for (int yy = y + 1; yy <= y + 9; yy++) p.set(x0 + 6, yy, z0 + 2, mast);
                p.set(x0 + 6, y + 10, z0 + 2, Blocks.BAMBOO_FENCE);
                for (int yy = y + 2; yy <= y + 4; yy++) {
                    p.set(x0 + 6, yy, z0 + 1, Blocks.CHAIN.defaultBlockState());
                    p.set(x0 + 6, yy, z0 + 3, Blocks.CHAIN.defaultBlockState());
                }
            }
            case 4 -> {
                for (int yy = y + 4; yy <= y + 9; yy++)
                    for (int z = 0; z <= 4; z++) {
                        if (z == 2) continue;
                        BlockState wool = yy == y + 6 ? Blocks.BLUE_WOOL.defaultBlockState() : Blocks.WHITE_WOOL.defaultBlockState();
                        p.set(x0 + 7, yy, z0 + z, wool);
                    }
                for (int yy = y + 5; yy <= y + 8; yy++) p.set(x0 + 7, yy, z0 + 2, Blocks.WHITE_WOOL.defaultBlockState());
            }
            case 5 -> {
                p.set(x0 + 3, y + 1, z0 + 1, Blocks.BARREL);
                p.set(x0 + 3, y + 1, z0 + 3, Blocks.BARREL);
                p.set(x0 + 9, y + 1, z0 + 1, Blocks.BARREL);
                p.set(x0 + 9, y + 1, z0 + 3, ModBlocks.SUITCASE.get().defaultBlockState().setValue(StateInteractBlock.MODE, 1));
                p.set(x0 + 2, y + 1, z0 + 2, facing(ModBlocks.BOAT_HELM.get(), Direction.EAST));
                p.set(x0 + 10, y + 2, z0 + 2, Blocks.LANTERN);
            }
            default -> {}
        }
    }

    public static BlockPos helmPos() {
        BlockPos o = boatOrigin();
        return new BlockPos(o.getX() + 2, o.getY() + 1, o.getZ() + 2);
    }

    // ---------- the Eye: Meridian Experience Lab 4 ----------
    public static int summit() {
        return (int) SUMMIT;
    }

    public static BlockPos station(int ox, int oy, int oz) {
        return new BlockPos(wx(Locations.PEAK[0] + ox), summit() + oy, wz(Locations.PEAK[1] + oz));
    }

    private void station(Placer p) {
        int sx = wx(Locations.PEAK[0]), sz = wz(Locations.PEAK[1]);
        int sy = summit();
        BlockState panel = ModBlocks.BUNKER_PANEL.get().defaultBlockState();
        BlockState light = ModBlocks.BUNKER_LIGHT.get().defaultBlockState();
        p.fill(sx - 9, sy, sz - 7, sx + 9, sy, sz + 9, Blocks.GRAVEL);
        p.clear(sx - 9, sy + 1, sz - 7, sx + 9, sy + 12, sz + 9);
        p.fill(sx - 6, sy, sz - 4, sx + 6, sy, sz + 4, panel);
        for (int y = sy + 1; y <= sy + 5; y++)
            for (int x = sx - 6; x <= sx + 6; x++)
                for (int z = sz - 4; z <= sz + 4; z++) {
                    boolean edge = x == sx - 6 || x == sx + 6 || z == sz - 4 || z == sz + 4;
                    p.set(x, y, z, edge ? panel : Blocks.AIR.defaultBlockState());
                }
        p.fill(sx - 6, sy + 6, sz - 4, sx + 6, sy + 6, sz + 4, panel);
        for (int x = sx - 4; x <= sx + 4; x += 4) p.set(x, sy + 6, sz, light);
        p.set(sx + 4, sy + 6, sz - 2, light);
        // door and keypad
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        p.set(sx, sy + 1, sz + 4, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        p.set(sx, sy + 2, sz + 4, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        p.set(sx + 1, sy + 2, sz + 5, facing(ModBlocks.KEYPAD.get(), Direction.SOUTH));
        p.sign(sx - 1, sy + 3, sz + 5, Direction.SOUTH, net.minecraft.world.item.DyeColor.RED, "MERIDIAN", "EXPERIENCES", "LAB 4", "KEEP SMILING");
        // monitors, terminal, racks
        for (int x = sx - 5; x <= sx + 2; x++)
            for (int y = sy + 2; y <= sy + 4; y++) p.set(x, y, sz - 3, facing(ModBlocks.MONITOR.get(), Direction.SOUTH).setValue(StateInteractBlock.MODE, (x + y) % 3 == 0 ? 1 : 0));
        p.set(sx - 1, sy + 1, sz, facing(ModBlocks.HALO_TERMINAL.get(), Direction.SOUTH));
        for (int z = sz - 2; z <= sz + 2; z++) {
            p.set(sx - 5, sy + 1, z, facing(ModBlocks.SERVER_RACK.get(), Direction.EAST));
            p.set(sx - 5, sy + 2, z, facing(ModBlocks.SERVER_RACK.get(), Direction.EAST));
        }
        p.set(sx - 1, sy + 1, sz + 2, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.NORTH));
        // transmitter room
        for (int y = sy + 1; y <= sy + 5; y++) for (int z = sz - 3; z <= sz + 3; z++) p.set(sx + 3, y, z, panel);
        p.clear(sx + 3, sy + 1, sz + 1, sx + 3, sy + 2, sz + 1);
        p.set(sx + 5, sy + 1, sz - 1, facing(ModBlocks.TRANSMITTER.get(), Direction.WEST));
        p.set(sx + 5, sy + 2, sz - 1, facing(ModBlocks.TRANSMITTER.get(), Direction.WEST));
        for (int y = sy + 3; y <= sy + 5; y++) p.set(sx + 5, y, sz - 1, Blocks.CHAIN);
        for (int y = sy + 7; y <= sy + 17; y++) p.set(sx + 5, y, sz - 1, Blocks.IRON_BARS);
        p.set(sx + 5, sy + 18, sz - 1, Blocks.LIGHTNING_ROD);
        p.set(sx + 4, sy + 12, sz - 1, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
        p.set(sx + 6, sy + 12, sz - 1, Blocks.REDSTONE_BLOCK);
        p.fill(sx - 4, sy + 7, sz - 3, sx - 2, sy + 7, sz - 1, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        p.set(sx - 3, sy + 8, sz - 2, Blocks.IRON_BARS);
        // path markers
        for (int k = 6; k <= 9; k++) p.set(sx, sy, sz + k, Blocks.DIRT_PATH);
    }

    public static void openStationDoor(Placer p, boolean open) {
        int sx = wx(Locations.PEAK[0]), sz = wz(Locations.PEAK[1]);
        int sy = summit();
        for (int y = sy + 1; y <= sy + 2; y++) {
            BlockState s = p.get(sx, y, sz + 4);
            if (s.hasProperty(DoorBlock.OPEN)) p.set(sx, y, sz + 4, s.setValue(DoorBlock.OPEN, open));
        }
    }

    public static void stationPowerDown(Placer p) {
        int sx = wx(Locations.PEAK[0]), sz = wz(Locations.PEAK[1]);
        int sy = summit();
        for (int x = sx - 5; x <= sx + 2; x++)
            for (int y = sy + 2; y <= sy + 4; y++) {
                BlockState s = p.get(x, y, sz - 3);
                if (s.hasProperty(StateInteractBlock.MODE)) p.set(x, y, sz - 3, s.setValue(StateInteractBlock.MODE, 2));
            }
        for (int y = sy + 1; y <= sy + 2; y++) {
            BlockState s = p.get(sx + 5, y, sz - 1);
            if (s.hasProperty(StateInteractBlock.LIT)) p.set(sx + 5, y, sz - 1, s.setValue(StateInteractBlock.LIT, false));
        }
        p.set(sx + 4, sy + 12, sz - 1, Blocks.REDSTONE_LAMP.defaultBlockState());
        p.set(sx + 6, sy + 12, sz - 1, Blocks.COAL_BLOCK);
    }

    // ---------- the Hollow Grove ----------
    public static int groveY() {
        return ground(Locations.GROVE[0], Locations.GROVE[1]);
    }

    public static BlockPos grove(int ox, int oz) {
        return new BlockPos(wx(Locations.GROVE[0] + ox), groveY() + 1, wz(Locations.GROVE[1] + oz));
    }

    private void grove(Placer p) {
        int gx = wx(Locations.GROVE[0]), gz = wz(Locations.GROVE[1]);
        int gy = groveY();
        p.fill(gx - 8, gy, gz - 9, gx + 8, gy, gz + 6, Blocks.PODZOL);
        p.clear(gx - 8, gy + 1, gz - 9, gx + 8, gy + 6, gz + 6);
        for (int x = gx - 2; x <= gx + 2; x++)
            for (int y = gy + 1; y <= gy + 3; y++) p.set(x, y, gz - 7, facing(ModBlocks.POLAROID_WALL.get(), Direction.SOUTH));
        for (int z : new int[]{gz - 4, gz - 2, gz, gz + 2})
            for (int x : new int[]{gx - 2, gx - 1, gx + 1, gx + 2}) p.set(x, gy + 1, z, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.NORTH));
        for (int[] e : new int[][]{{4, -6}, {-4, -6}, {4, 3}, {-4, 3}}) {
            p.set(gx + e[0], gy + 1, gz + e[1], facing(ModBlocks.HOLLOW_EFFIGY.get(), Direction.SOUTH));
            p.set(gx + e[0], gy + 2, gz + e[1], facing(ModBlocks.HOLLOW_EFFIGY.get(), Direction.SOUTH));
        }
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            int x = gx + (int) Math.round(Math.cos(a) * 7), z = gz - 1 + (int) Math.round(Math.sin(a) * 6);
            p.set(x, gy + 1, z, ModBlocks.GHOST_ORCHID.get());
        }
    }

    private void poolRim(Placer p) {
        int px = wx(Locations.POOL[0]), pz = wz(Locations.POOL[1]);
        Random r = new Random(7);
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI * 2 / 24;
            int x = px + (int) Math.round(Math.cos(a) * 7), z = pz + (int) Math.round(Math.sin(a) * 7);
            int h = ground(x - Locations.ISLAND_X, z - Locations.ISLAND_Z);
            if (r.nextFloat() < 0.6f) p.set(x, h, z, r.nextBoolean() ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE);
            else if (p.get(x, h + 1, z).isAir()) p.set(x, h + 1, z, ModBlocks.TARO_PLANT.get());
        }
        p.set(px + 2, poolSurface(), pz + 1, Blocks.LILY_PAD);
        p.set(px - 3, poolSurface(), pz - 2, Blocks.LILY_PAD);
    }
}
