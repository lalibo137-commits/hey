package com.airlinestory.world;

import com.airlinestory.block.StateInteractBlock;
import com.airlinestory.registry.ModBlocks;
import com.airlinestory.story.Locations;
import com.airlinestory.story.StoryState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.AquaticFeatures;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.airlinestory.world.IslandTerrain.*;
import static com.airlinestory.world.Placer.*;

/**
 * Builds the island a little at a time, in the background while you're still at the airport.
 * Phase 0 terrain, 1 plants (vanilla tree and vegetation generators), 2 places, 3 done. Progress is saved.
 */
public final class IslandBuilder {
    public static final int EXTENT = 200;
    private final List<ChunkPos> chunks = new ArrayList<>();
    private final StoryState state;

    public IslandBuilder(StoryState state) {
        this.state = state;
        int cx0 = (Locations.ISLAND_X - EXTENT) >> 4, cx1 = (Locations.ISLAND_X + EXTENT) >> 4;
        int cz0 = (Locations.ISLAND_Z - EXTENT) >> 4, cz1 = (Locations.ISLAND_Z + EXTENT) >> 4;
        for (int cx = cx0; cx <= cx1; cx++)
            for (int cz = cz0; cz <= cz1; cz++) {
                int dx = (cx << 4) + 8 - Locations.ISLAND_X, dz = (cz << 4) + 8 - Locations.ISLAND_Z;
                if (radial(dx, dz) < 1.35) chunks.add(new ChunkPos(cx, cz));
            }
    }

    public boolean done() { return state.counter("island_phase") >= 3; }

    public int percent() {
        int phase = state.counter("island_phase");
        if (phase >= 3) return 100;
        int idx = state.counter("island_idx");
        return (int) ((phase * chunks.size() + idx) * 100L / (2L * chunks.size() + 1));
    }

    /** Clue blocks placed by the last step that finished the structures. */
    public final java.util.Map<BlockPos, String> placedClues = new java.util.LinkedHashMap<>();

    public boolean step(ServerLevel level, long deadline) {
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
                Placer p = new Placer(level);
                ChunkPos c = chunks.get(idx);
                if (phase == 0) terrainChunk(p, c);
                else plantChunk(p, level, c);
                p.finish();
                state.setCounter("island_idx", idx + 1);
            } else {
                Placer p = new Placer(level);
                structures(p, level);
                p.finish();
                placedClues.putAll(p.clues);
                state.setCounter("island_phase", 3);
                return true;
            }
        }
        return done();
    }

    private static int wx(int dx) { return Locations.ISLAND_X + dx; }
    private static int wz(int dz) { return Locations.ISLAND_Z + dz; }

    // =========================================================================================
    // Phase 0: terrain
    // =========================================================================================
    private void terrainChunk(Placer p, ChunkPos c) {
        for (int x = c.getMinBlockX(); x <= c.getMaxBlockX(); x++) {
            for (int z = c.getMinBlockZ(); z <= c.getMaxBlockZ(); z++) {
                int dx = x - Locations.ISLAND_X, dz = z - Locations.ISLAND_Z;
                double r = radial(dx, dz);
                if (r > 1.35) continue;
                int h = height(dx, dz);
                int water = riverWater(dx, dz);
                String region = region(dx, dz);
                if (h <= SEA && water < 0) {
                    boolean lagoon = region.equals("lagoon");
                    for (int y = SEA_FLOOR + 1; y <= h; y++) {
                        Block b = y == h && lagoon && hash(x, z, 3) < 0.15 ? Blocks.GRAVEL : y == h && region.equals("mangrove") ? Blocks.MUD : Blocks.SAND;
                        p.set(x, y, z, b);
                    }
                    if (h < SEA_FLOOR + 1 && lagoon) p.set(x, SEA_FLOOR, z, Blocks.SAND);
                    continue;
                }
                int slope = Math.max(Math.max(Math.abs(height(dx + 1, dz) - h), Math.abs(height(dx - 1, dz) - h)),
                        Math.max(Math.abs(height(dx, dz + 1) - h), Math.abs(height(dx, dz - 1) - h)));
                boolean beach = water < 0 && beach(dx, dz, h);
                for (int y = SEA_FLOOR + 1; y <= h; y++) {
                    p.set(x, y, z, column(x, y, z, h, slope, beach, water >= 0, region));
                }
                if (water >= 0) {
                    for (int y = h + 1; y <= water; y++) p.set(x, y, z, Blocks.WATER);
                    for (int y = Math.max(h + 1, water + 1); y <= water + 3; y++) if (y <= SEA) continue; else p.set(x, y, z, Blocks.AIR);
                }
            }
        }
    }

    private Block column(int x, int y, int z, int h, int slope, boolean beach, boolean riverbed, String region) {
        int depth = h - y;
        if (riverbed && depth <= 1) return hash(x, z, 17) < 0.6 ? Blocks.GRAVEL : hash(x, z, 18) < 0.5 ? Blocks.CLAY : Blocks.SAND;
        if (beach) return depth <= 3 ? Blocks.SAND : depth <= 6 ? Blocks.SANDSTONE : Blocks.STONE;
        boolean steep = slope >= 3;
        boolean high = h > 122;
        if (depth == 0) {
            if (steep || high) {
                double n = hash(x, z, 21);
                if (high && n < 0.2) return Blocks.GRAVEL;
                if (n < 0.35) return Blocks.ANDESITE;
                if (n < 0.45 && !high) return Blocks.MOSSY_COBBLESTONE;
                return y > 130 ? Blocks.TUFF : Blocks.STONE;
            }
            return switch (region) {
                case "mangrove" -> hash(x, z, 22) < 0.7 ? Blocks.MUD : Blocks.GRASS_BLOCK;
                case "grove" -> hash(x, z, 23) < 0.5 ? Blocks.PODZOL : hash(x, z, 24) < 0.5 ? Blocks.COARSE_DIRT : Blocks.SOUL_SOIL;
                case "mountain" -> h > 105 && hash(x, z, 25) < 0.4 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK;
                default -> {
                    double n = fbm(x * 2, z * 2);
                    yield n < 0.3 ? Blocks.PODZOL : n > 0.72 ? Blocks.MOSS_BLOCK : Blocks.GRASS_BLOCK;
                }
            };
        }
        if (depth <= 3) return (steep || high) ? Blocks.STONE : region.equals("mangrove") ? Blocks.MUD : Blocks.DIRT;
        double ore = hash(x, y * 31 + z, 9);
        if (ore < 0.012) return Blocks.COAL_ORE;
        if (ore < 0.018) return Blocks.IRON_ORE;
        if (ore < 0.02 && y < 58) return Blocks.COPPER_ORE;
        return y < 56 && hash(x, y, z) < 0.3 ? Blocks.DEEPSLATE : Blocks.STONE;
    }

    // =========================================================================================
    // Phase 1: plants, using vanilla's own generators
    // =========================================================================================
    private static boolean reserved(int dx, int dz) {
        return dist(dx, dz, VILLAGE) < 30 || dist(dx, dz, CRASH) < 24 || dist(dx, dz, PEAK) < 16 || dist(dx, dz, LONE_SEAT) < 6
                || dist(dx, dz, LOOKOUT) < 10 || dist(dx, dz, BURNED) < 14 || dist(dx, dz, LOWER_POOL) < 9 || dist(dx, dz, UPPER_POOL) < 9
                || (dx > VILLAGE[0] && Math.abs(dz - VILLAGE[1]) < 10) || dist(dx, dz, CAVE_MOUTH) < 8;
    }

    private void feature(ServerLevel level, ResourceKey<ConfiguredFeature<?, ?>> key, BlockPos pos, long seed) {
        try {
            level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(key)
                    .ifPresent(h -> h.value().place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), pos));
        } catch (Exception ignored) {
        }
    }

    private void plantChunk(Placer p, ServerLevel level, ChunkPos c) {
        for (int x = c.getMinBlockX(); x <= c.getMaxBlockX(); x++) {
            for (int z = c.getMinBlockZ(); z <= c.getMaxBlockZ(); z++) {
                int dx = x - Locations.ISLAND_X, dz = z - Locations.ISLAND_Z;
                double r = radial(dx, dz);
                if (r > 1.3) continue;
                int h = height(dx, dz);
                double roll = hash(x, z, 101);
                long seed = x * 3129871L ^ z * 116129781L;
                String region = region(dx, dz);
                BlockPos above = new BlockPos(x, h + 1, z);
                if (h < SEA) {
                    if (riverWater(dx, dz) >= 0) continue;
                    if (region.equals("lagoon") && roll < 1 / 22.0) feature(level, AquaticFeatures.WARM_OCEAN_VEGETATION, above, seed);
                    else if (roll < 1 / 9.0 && h > 54) feature(level, AquaticFeatures.SEAGRASS_TALL, above, seed);
                    else if (roll < 1 / 70.0 && h < 57) feature(level, AquaticFeatures.KELP, above, seed);
                    continue;
                }
                if (riverWater(dx, dz) >= 0 || reserved(dx, dz)) continue;
                if (!p.get(x, h + 1, z).isAir()) continue;
                BlockState ground = p.get(x, h, z);
                switch (region) {
                    case "grove" -> {
                        if (dist(dx, dz, GROVE) < 10) continue;
                        if (roll < 1 / 34.0) deadTree(p, x, h + 1, z, hash(x, z, 5));
                        else if (roll < 1 / 12.0) p.set(x, h + 1, z, ModBlocks.GHOST_ORCHID.get());
                        else if (roll < 1 / 6.0) p.set(x, h + 1, z, Blocks.DEAD_BUSH);
                        else if (roll < 0.2) p.set(x, h + 1, z, Blocks.MOSS_CARPET);
                    }
                    case "mangrove" -> {
                        if (roll < 1 / 26.0) feature(level, hash(x, z, 6) < 0.3 ? TreeFeatures.TALL_MANGROVE : TreeFeatures.MANGROVE, above, seed);
                        else if (roll < 0.25 && ground.is(Blocks.GRASS_BLOCK)) p.set(x, h + 1, z, Blocks.GRASS);
                    }
                    default -> {
                        if (beach(dx, dz, h) && ground.is(Blocks.SAND)) {
                            if (roll < 1 / 40.0) palm(p, x, h + 1, z, hash(x, z, 7));
                            else if (roll < 1 / 25.0 && r < 0.98) p.set(x, h + 1, z, Blocks.DEAD_BUSH);
                            else if (roll < 1 / 18.0 && dist(dx, dz, CRASH) > 30) p.set(x, h + 1, z, Blocks.SEA_PICKLE);
                            continue;
                        }
                        if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.PODZOL) && !ground.is(Blocks.MOSS_BLOCK) && !ground.is(Blocks.COARSE_DIRT)) continue;
                        boolean mountain = h > 98;
                        if (mountain) {
                            if (roll < 1 / 160.0 && h < 120) feature(level, TreeFeatures.ACACIA, above, seed);
                            else if (roll < 1 / 70.0) feature(level, TreeFeatures.JUNGLE_BUSH, above, seed);
                            else if (roll < 0.2) p.set(x, h + 1, z, Blocks.GRASS);
                            continue;
                        }
                        if (roll < 1 / 230.0 && r < 0.8) feature(level, TreeFeatures.MEGA_JUNGLE_TREE, above, seed);
                        else if (roll < 1 / 75.0) feature(level, TreeFeatures.JUNGLE_TREE, above, seed);
                        else if (roll < 1 / 34.0) feature(level, TreeFeatures.JUNGLE_BUSH, above, seed);
                        else if (roll < 1 / 30.0 && r < 0.9) palm(p, x, h + 1, z, hash(x, z, 8));
                        else if (roll < 1 / 26.0 && region.equals("jungle") && dx > 20) feature(level, VegetationFeatures.BAMBOO_SOME_PODZOL, above, seed);
                        else if (roll < 1 / 24.0) p.set(x, h + 1, z, ModBlocks.BITTERLEAF.get());
                        else if (roll < 1 / 22.0) feature(level, VegetationFeatures.PATCH_MELON, above, seed);
                        else if (roll < 1 / 20.0 && h < 72) p.set(x, h + 1, z, ModBlocks.TARO_PLANT.get());
                        else if (roll < 1 / 19.0) feature(level, VegetationFeatures.PATCH_BERRY_BUSH, above, seed);
                        else if (roll < 0.30) p.set(x, h + 1, z, Blocks.GRASS);
                        else if (roll < 0.38) p.set(x, h + 1, z, Blocks.FERN);
                        else if (roll < 0.395) p.set(x, h + 1, z, Blocks.POPPY);
                        else if (roll < 0.40) p.set(x, h + 1, z, Blocks.AZURE_BLUET);
                        else if (roll < 0.403) p.set(x, h + 1, z, Blocks.BLUE_ORCHID);
                    }
                }
            }
        }
        // river banks get sugar cane
        for (int x = c.getMinBlockX(); x <= c.getMaxBlockX(); x += 3)
            for (int z = c.getMinBlockZ(); z <= c.getMaxBlockZ(); z += 3) {
                int dx = x - Locations.ISLAND_X, dz = z - Locations.ISLAND_Z;
                double[] rv = river(dx, dz);
                if (rv != null && rv[0] >= rv[2] && rv[0] < rv[2] + 1.5 && hash(x, z, 31) < 0.4) {
                    int h = height(dx, dz);
                    if (p.get(x, h + 1, z).isAir()) feature(level, VegetationFeatures.PATCH_SUGAR_CANE, new BlockPos(x, h + 1, z), x * 31L + z);
                }
            }
    }

    public static void palm(Placer p, int x, int y, int z, double seed) {
        Random r = new Random((long) (seed * 1e9));
        int height = 6 + r.nextInt(5);
        Direction lean = Direction.from2DDataValue(r.nextInt(4));
        BlockState log = ModBlocks.PALM_LOG.get().defaultBlockState();
        int lx = 0, lz = 0;
        for (int i = 0; i < height; i++) {
            if (i == height / 2 || i == height - 2) { lx += lean.getStepX(); lz += lean.getStepZ(); }
            p.set(x + lx, y + i, z + lz, log);
        }
        int tx = x + lx, ty = y + height, tz = z + lz;
        BlockState leaf = leaves(ModBlocks.PALM_LEAVES.get());
        p.set(tx, ty, tz, leaf);
        p.set(tx, ty + 1, tz, leaf);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int k = 1; k <= 4; k++) p.set(tx + d.getStepX() * k, ty + (k == 1 ? 1 : k <= 3 ? 0 : -1), tz + d.getStepZ() * k, leaf);
            p.set(tx + d.getStepX() * 4, ty - 2, tz + d.getStepZ() * 4, leaf);
        }
        for (int[] dg : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            p.set(tx + dg[0], ty, tz + dg[1], leaf);
            p.set(tx + dg[0] * 2, ty - 1, tz + dg[1] * 2, leaf);
            p.set(tx + dg[0] * 3, ty - 2, tz + dg[1] * 3, leaf);
        }
        int nuts = 1 + r.nextInt(3);
        for (int i = 0; i < nuts; i++) {
            Direction d = Direction.from2DDataValue(r.nextInt(4));
            p.set(tx + d.getStepX(), ty - 1, tz + d.getStepZ(), facing(ModBlocks.COCONUT.get(), d));
        }
    }

    private static void deadTree(Placer p, int x, int y, int z, double seed) {
        bleachedTree(p, x, y, z, new Random((long) (seed * 1e9)));
    }

    // =========================================================================================
    // Phase 2: places
    // =========================================================================================
    public static int ground(int dx, int dz) { return height(dx, dz); }

    private void structures(Placer p, ServerLevel level) {
        waterfall(p);
        crashSite(p, level);
        loneSeat(p);
        village(p, level);
        dock(p);
        station(p);
        grove(p);
        lookout(p);
        burnedVillage(p);
        sunkenPlane(p);
        haleCave(p);
        state.setCounter("y:crash", ground(CRASH[0], CRASH[1]) + 1);
        state.setCounter("y:village", villageY() + 1);
        state.setCounter("y:grove", ground(GROVE[0], GROVE[1]) + 1);
        state.setCounter("y:peak", (int) SUMMIT + 1);
        state.setCounter("y:lab", (int) SUMMIT + 1);
        state.setCounter("y:summit", (int) SUMMIT);
        state.setCounter("y:dock", SEA + 2);
        state.setCounter("x:dock", wx(shoreX() + 8));
        state.setCounter("y:lone_seat", loneSeatPos().getY());
        state.setCounter("y:pool", LOWER_POOL_LEVEL + 1);
        state.setCounter("y:falls", LOWER_POOL_LEVEL + 1);
        state.setCounter("y:lookout", ground(LOOKOUT[0], LOOKOUT[1]) + 1);
        state.setCounter("y:burned", ground(BURNED[0], BURNED[1]) + 1);
        state.setCounter("y:lagoon", SEA + 1);
        state.setCounter("y:sunken", SEA + 1);
        state.setCounter("y:cave", ground(CAVE_MOUTH[0], CAVE_MOUTH[1]) + 1);
        state.setCounter("y:cave_camp", CAVE_Y + 1);
        state.setCounter("y:river", 70);
        state.setCounter("y:mangrove", SEA + 1);
    }

    public static final int LOWER_POOL_LEVEL = (int) RIVER[3][2];

    private void waterfall(Placer p) {
        // the drop between the two pools: falling water in front of a mossy cliff
        double[] a = RIVER[1], b = RIVER[2];
        int top = (int) a[2], bottom = (int) b[2];
        for (double t = 0.45; t <= 0.75; t += 0.08) {
            int x = (int) Math.round(lerp(a[0], b[0], t)) + Locations.ISLAND_X;
            int z = (int) Math.round(lerp(a[1], b[1], t)) + Locations.ISLAND_Z;
            for (int w = -1; w <= 1; w++) {
                for (int y = bottom; y <= top; y++) {
                    p.set(x + w, y, z, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8));
                }
            }
        }
        // mist: a few cobwebs? no - glow lichen and moss around the plunge pool
        int px = wx(LOWER_POOL[0]), pz = wz(LOWER_POOL[1]);
        Random r = new Random(5);
        for (int k = 0; k < 40; k++) {
            double ang = r.nextDouble() * Math.PI * 2;
            int x = px + (int) Math.round(Math.cos(ang) * (7 + r.nextInt(3))), z = pz + (int) Math.round(Math.sin(ang) * (7 + r.nextInt(3)));
            int h = ground(x - Locations.ISLAND_X, z - Locations.ISLAND_Z);
            if (riverWater(x - Locations.ISLAND_X, z - Locations.ISLAND_Z) >= 0) continue;
            p.set(x, h, z, r.nextBoolean() ? Blocks.MOSS_BLOCK : Blocks.MOSSY_COBBLESTONE);
            if (r.nextInt(3) == 0 && p.get(x, h + 1, z).isAir()) p.set(x, h + 1, z, ModBlocks.TARO_PLANT.get());
        }
        p.set(px + 3, LOWER_POOL_LEVEL + 1, pz + 2, Blocks.LILY_PAD);
        p.set(px - 2, LOWER_POOL_LEVEL + 1, pz - 1, Blocks.LILY_PAD);
    }

    // ---------------------------------------------------------------- crash site
    private void fuselagePiece(Placer p, Random r, int x0, int x1, int cz, int g, boolean brokenStart, boolean brokenEnd, boolean seats) {
        BlockState fus = ModBlocks.FUSELAGE.get().defaultBlockState();
        BlockState stripe = ModBlocks.FUSELAGE_STRIPE.get().defaultBlockState();
        BlockState window = ModBlocks.AIRPLANE_WINDOW.get().defaultBlockState();
        BlockState wreck = ModBlocks.WRECKAGE.get().defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            boolean broken = (brokenStart && x - x0 < 3) || (brokenEnd && x1 - x < 3);
            for (int dz = -4; dz <= 4; dz++) {
                for (int ry = 0; ry <= 6; ry++) {
                    boolean shell = Math.abs(dz) == 4 && ry >= 1 && ry <= 5 || ry == 0 && Math.abs(dz) <= 3 || ry == 6 && Math.abs(dz) <= 3;
                    if (Math.abs(dz) == 4 && (ry == 0 || ry == 6)) continue;
                    int y = g + ry;
                    if (!shell) { p.set(x, y, cz + dz, Blocks.AIR.defaultBlockState()); continue; }
                    if (broken && r.nextFloat() < 0.5f) { p.set(x, y, cz + dz, r.nextBoolean() ? wreck : Blocks.AIR.defaultBlockState()); continue; }
                    BlockState s = ry == 0 ? ModBlocks.CABIN_FLOOR.get().defaultBlockState() : ry == 2 ? stripe : ry == 3 && x % 2 == 0 ? window : fus;
                    if (ry == 6 && r.nextFloat() < 0.08f) s = Blocks.AIR.defaultBlockState();
                    p.set(x, y, cz + dz, s);
                }
            }
            if (seats && x % 2 == 0 && !broken) {
                for (int dz : new int[]{-3, -2, -1, 1, 2, 3}) {
                    if (r.nextFloat() < 0.8f) p.set(x, g + 1, cz + dz, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.WEST));
                }
                if (r.nextFloat() < 0.3f) p.set(x - 1, g + 2, cz - 3, facing(ModBlocks.OXYGEN_MASK.get(), Direction.WEST));
            }
        }
    }

    private void crashSite(Placer p, ServerLevel level) {
        int cx = CRASH[0], cz = CRASH[1];
        Random r = new Random(417);
        BlockState wreck = ModBlocks.WRECKAGE.get().defaultBlockState();
        // main cabin section, half-buried on the beach
        int g = ground(cx - 6, cz - 6) ;
        fuselagePiece(p, r, wx(cx - 20), wx(cx + 2), wz(cz - 6), g, true, true, true);
        // nose section, nose-first into the treeline
        int g2 = ground(cx - 38, cz - 16);
        fuselagePiece(p, r, wx(cx - 46), wx(cx - 34), wz(cz - 16), g2 - 1, false, true, false);
        p.fill(wx(cx - 47), g2, wz(cz - 18), wx(cx - 47), g2 + 3, wz(cz - 14), Blocks.BLACK_STAINED_GLASS);
        // tail section standing in the shallows
        int tx = wx(cx + 22), tz = wz(cz + 14);
        int tg = Math.max(SEA - 2, ground(cx + 22, cz + 14));
        for (int x = tx; x <= tx + 9; x++)
            for (int dz = -3; dz <= 3; dz++)
                for (int y = tg; y <= tg + 5; y++) {
                    boolean shell = Math.abs(dz) == 3 || y == tg || y == tg + 5;
                    if (shell) p.set(x, y, tz + dz, x == tx && r.nextBoolean() ? wreck : ModBlocks.FUSELAGE.get().defaultBlockState());
                }
        for (int h = 0; h < 11; h++)
            for (int x = tx + 2 + h / 2; x <= tx + 9; x++) p.set(x, tg + 6 + h, tz, ModBlocks.FUSELAGE_STRIPE.get().defaultBlockState());
        // a wing torn off in the trees
        int wg = ground(cx + 4, cz - 26);
        for (int x = wx(cx - 6); x <= wx(cx + 14); x++)
            for (int z = wz(cz - 30); z <= wz(cz - 30) + Math.max(2, 7 - (x - wx(cx - 6)) / 3); z++)
                p.set(x, wg + 1 + (x - wx(cx - 6)) / 7, z, ModBlocks.FUSELAGE.get().defaultBlockState());
        // engine in a scorched crater
        int ex = cx + 12, ez = cz - 14;
        int eg = ground(ex, ez);
        for (int ox = -5; ox <= 5; ox++)
            for (int oz = -5; oz <= 5; oz++) {
                double d = Math.sqrt(ox * ox + oz * oz);
                if (d > 5) continue;
                int dep = d < 3 ? 2 : 1;
                for (int y = eg - dep + 1; y <= eg + 4; y++) p.set(wx(ex + ox), y, wz(ez + oz), Blocks.AIR.defaultBlockState());
                p.set(wx(ex + ox), eg - dep, wz(ez + oz), r.nextFloat() < 0.4f ? Blocks.SOUL_SOIL : Blocks.COARSE_DIRT);
            }
        for (int ox = -2; ox <= 3; ox++)
            for (int y = eg - 1; y <= eg + 1; y++)
                for (int oz = -1; oz <= 1; oz++)
                    p.set(wx(ex + ox), y, wz(ez + oz), (y == eg && oz == 0) ? Blocks.BLACK_CONCRETE.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        p.set(wx(ex + 4), eg, wz(ez), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false));
        // the debris trail through the jungle: felled trees and scorched ground
        for (int i = 0; i < 60; i++) {
            double t = i / 60.0;
            int dx = (int) lerp(-70, cx - 30, t) + r.nextInt(9) - 4, dz = (int) lerp(80, cz - 18, t) + r.nextInt(9) - 4;
            int h = ground(dx, dz);
            if (h <= SEA) continue;
            p.set(wx(dx), h, wz(dz), r.nextFloat() < 0.5f ? Blocks.COARSE_DIRT : Blocks.PODZOL);
            if (i % 5 == 0) {
                Direction d = r.nextBoolean() ? Direction.EAST : Direction.SOUTH;
                for (int k = 0; k < 4 + r.nextInt(4); k++)
                    p.set(wx(dx + d.getStepX() * k), h + 1, wz(dz + d.getStepZ() * k), axis(Blocks.JUNGLE_LOG, d.getAxis()));
            } else if (i % 3 == 0) {
                p.set(wx(dx), h + 1, wz(dz), wreck);
            }
        }
        // luggage, seats, fires
        int luggage = 0;
        for (int i = 0; i < 90 && luggage < 18; i++) {
            int dx = cx + r.nextInt(51) - 30, dz = cz + r.nextInt(31) - 18;
            int h = ground(dx, dz);
            if (h <= SEA || !p.get(wx(dx), h + 1, wz(dz)).isAir()) continue;
            p.set(wx(dx), h + 1, wz(dz), facing(ModBlocks.SUITCASE.get(), Direction.from2DDataValue(r.nextInt(4))).setValue(StateInteractBlock.MODE, 0));
            luggage++;
        }
        for (int i = 0; i < 16; i++) {
            int dx = cx + r.nextInt(41) - 24, dz = cz + r.nextInt(25) - 14;
            int h = ground(dx, dz);
            if (h <= SEA || !p.get(wx(dx), h + 1, wz(dz)).isAir()) continue;
            p.set(wx(dx), h + 1, wz(dz), facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.from2DDataValue(r.nextInt(4))));
        }
        for (int i = 0; i < 50; i++) {
            int dx = cx + r.nextInt(45) - 25, dz = cz + r.nextInt(30) - 16;
            int h = ground(dx, dz);
            if (h < SEA - 1 || !p.get(wx(dx), h + 1, wz(dz)).isAir()) continue;
            p.set(wx(dx), h + 1, wz(dz), r.nextFloat() < 0.15f ? Blocks.YELLOW_CARPET.defaultBlockState() : wreck);
        }
        for (int[] f : new int[][]{{cx - 4, cz + 2}, {cx + 8, cz - 2}, {cx - 26, cz - 10}, {cx + 18, cz + 4}}) {
            int h = ground(f[0], f[1]);
            p.set(wx(f[0]), h, wz(f[1]), Blocks.HAY_BLOCK);
            p.set(wx(f[0]), h + 1, wz(f[1]), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
        }
        // a production crate, half buried
        int ch = ground(cx + 26, cz - 2);
        p.fill(wx(cx + 25), ch, wz(cz - 3), wx(cx + 27), ch + 1, wz(cz - 1), Blocks.STRIPPED_SPRUCE_WOOD);
        p.clue(wx(cx + 26), ch + 2, wz(cz - 2), ModBlocks.CLUE_NOTE.get(), "set_crate");
    }

    public static BlockPos loneSeatPos() {
        int dx = LONE_SEAT[0], dz = LONE_SEAT[1];
        return new BlockPos(wx(dx), Math.max(SEA, ground(dx, dz)) + 1, wz(dz));
    }

    private void loneSeat(Placer p) {
        BlockPos s = loneSeatPos();
        p.set(s.getX(), s.getY() - 1, s.getZ(), Blocks.SAND);
        p.set(s, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.SOUTH));
    }

    public static BlockPos wakePos() {
        return new BlockPos(wx(CRASH[0] + 4), Math.max(SEA, ground(CRASH[0] + 4, CRASH[1] + 8)) + 1, wz(CRASH[1] + 8));
    }

    // ---------------------------------------------------------------- village on stilts
    public static int villageY() { return ground(VILLAGE[0], VILLAGE[1]); }

    public static BlockPos village(int ox, int oz) {
        return new BlockPos(wx(VILLAGE[0] + ox), villageY() + 1, wz(VILLAGE[1] + oz));
    }

    /** Height of the stilt walkways. */
    public static int deckY() { return villageY() + 4; }

    private void village(Placer p, ServerLevel level) {
        int vx = wx(VILLAGE[0]), vz = wz(VILLAGE[1]);
        int vy = villageY();
        int dy = deckY();
        BlockState plank = ModBlocks.PALM_PLANKS.get().defaultBlockState();
        // plaza
        for (int x = -7; x <= 7; x++)
            for (int z = -7; z <= 7; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= 7) p.set(vx + x, vy, vz + z, d < 2.5 ? Blocks.MUD_BRICKS.defaultBlockState() : hash(x, z, 4) < 0.6 ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
            }
        p.set(vx, vy, vz, Blocks.HAY_BLOCK);
        p.set(vx, vy + 1, vz, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
        for (int[] s : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {2, 2}, {-2, -2}}) {
            Direction.Axis ax = Math.abs(s[0]) >= Math.abs(s[1]) ? Direction.Axis.Z : Direction.Axis.X;
            p.set(vx + s[0], vy + 1, vz + s[1], axis(ModBlocks.PALM_LOG.get(), ax));
        }
        // stilt huts around the plaza, joined by walkways
        int[][] huts = {{-14, -10}, {14, -11}, {-15, 9}, {15, 10}, {0, 16}, {-2, -17}};
        for (int[] h : huts) stiltHut(p, vx + h[0], dy, vz + h[1], vy);
        for (int i = 0; i < huts.length; i++) {
            int[] a = huts[i], b = huts[(i + 1) % huts.length];
            walkway(p, vx + a[0], vz + a[1], vx + b[0], vz + b[1], dy, plank);
        }
        // totems and lanterns
        for (int[] t : new int[][]{{8, 8}, {-8, 8}, {8, -8}, {-8, -8}}) {
            Direction face = t[1] > 0 ? Direction.NORTH : Direction.SOUTH;
            for (int y = 1; y <= 3; y++) p.set(vx + t[0], vy + y, vz + t[1], facing(ModBlocks.TOTEM.get(), face));
            p.set(vx + t[0], vy + 4, vz + t[1], Blocks.LANTERN);
        }
        // fish racks, gardens, canoes
        for (int k = 0; k < 4; k++) {
            p.set(vx + 9 + k, vy + 1, vz + 3, Blocks.BAMBOO_FENCE);
            p.set(vx + 9 + k, vy + 2, vz + 3, Blocks.BAMBOO_FENCE);
            p.set(vx + 9 + k, vy + 3, vz + 3, ModBlocks.THATCH.get().defaultBlockState());
        }
        for (int x = -12; x <= -7; x++)
            for (int z = 1; z <= 4; z++) {
                p.set(vx + x, vy, vz + z, Blocks.FARMLAND);
                p.set(vx + x, vy + 1, vz + z, (x + z) % 3 == 0 ? ModBlocks.TARO_PLANT.get().defaultBlockState() : Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
            }
        p.set(vx - 12, vy, vz + 2, Blocks.WATER);
        // the shrine: airplane seats facing the fire, oxygen masks hanging like charms
        for (int k = -2; k <= 2; k++) {
            p.set(vx + k, vy + 1, vz - 7, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.SOUTH));
            p.set(vx + k, vy + 4, vz - 8, ModBlocks.THATCH.get().defaultBlockState());
            if (k % 2 == 0) p.set(vx + k, vy + 3, vz - 7, facing(ModBlocks.OXYGEN_MASK.get(), Direction.SOUTH));
        }
        for (int y = 1; y <= 3; y++) { p.set(vx - 3, vy + y, vz - 8, Blocks.BAMBOO_BLOCK); p.set(vx + 3, vy + y, vz - 8, Blocks.BAMBOO_BLOCK); }
        // longhouse (Ahu) to the north
        longhouse(p, vx, vy, vz - 26);
        // Mira's herb hut: brewing stand and drying herbs at the west hut
        p.set(vx - 15, dy + 1, vz + 9, Blocks.BREWING_STAND);
        p.set(vx - 16, dy + 1, vz + 8, Blocks.FLOWER_POT);
        // Teo's workshop on the beach to the east
        int wsx = vx + 22;
        p.fill(wsx - 2, vy, vz - 3, wsx + 4, vy, vz + 3, plank);
        for (int[] c : new int[][]{{-2, -3}, {4, -3}, {-2, 3}, {4, 3}}) p.fill(wsx + c[0], vy + 1, vz + c[1], wsx + c[0], vy + 3, vz + c[1], ModBlocks.PALM_LOG.get().defaultBlockState());
        p.fill(wsx - 3, vy + 4, vz - 4, wsx + 5, vy + 4, vz + 4, ModBlocks.THATCH.get().defaultBlockState());
        p.set(wsx, vy + 1, vz, Blocks.SMITHING_TABLE);
        p.set(wsx + 1, vy + 1, vz, Blocks.CRAFTING_TABLE);
        p.set(wsx + 2, vy + 1, vz - 2, Blocks.STONECUTTER);
        for (int x = 0; x <= 4; x++) p.set(wsx + x - 1, vy + 1, vz + 2, axis(ModBlocks.PALM_LOG.get(), Direction.Axis.X));
    }

    private void stiltHut(Placer p, int cx, int dy, int cz, int ground) {
        BlockState post = ModBlocks.PALM_LOG.get().defaultBlockState();
        BlockState wall = Blocks.BAMBOO_PLANKS.defaultBlockState();
        BlockState thatch = ModBlocks.THATCH.get().defaultBlockState();
        BlockState deck = ModBlocks.PALM_PLANKS.get().defaultBlockState();
        for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            int g = ground(cx + c[0] - Locations.ISLAND_X, cz + c[1] - Locations.ISLAND_Z);
            for (int y = Math.min(g, ground) ; y < dy; y++) p.set(cx + c[0], y, cz + c[1], post);
        }
        p.fill(cx - 3, dy, cz - 3, cx + 3, dy, cz + 3, deck);
        for (int y = dy + 1; y <= dy + 3; y++)
            for (int x = cx - 2; x <= cx + 2; x++)
                for (int z = cz - 2; z <= cz + 2; z++) {
                    boolean edge = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
                    boolean corner = Math.abs(x - cx) == 2 && Math.abs(z - cz) == 2;
                    p.set(x, y, z, edge ? (corner ? Blocks.BAMBOO_BLOCK.defaultBlockState() : wall) : Blocks.AIR.defaultBlockState());
                }
        // door facing the plaza
        Direction door = Math.abs(cx - wx(VILLAGE[0])) > Math.abs(cz - wz(VILLAGE[1])) ? (cx > wx(VILLAGE[0]) ? Direction.WEST : Direction.EAST)
                : (cz > wz(VILLAGE[1]) ? Direction.NORTH : Direction.SOUTH);
        p.set(cx + door.getStepX() * 2, dy + 1, cz + door.getStepZ() * 2, Blocks.AIR);
        p.set(cx + door.getStepX() * 2, dy + 2, cz + door.getStepZ() * 2, Blocks.AIR);
        p.set(cx - door.getStepX() * 2, dy + 2, cz - door.getStepZ() * 2, Blocks.AIR);
        p.set(cx + door.getStepZ() * 2, dy + 2, cz + door.getStepX() * 2, Blocks.AIR);
        for (int k = 0; k <= 3; k++) p.fill(cx - 3 + k, dy + 4 + k, cz - 3 + k, cx + 3 - k, dy + 4 + k, cz + 3 - k, thatch);
        // ladder down to the ground on the plaza side
        int lx = cx + door.getStepX() * 3, lz = cz + door.getStepZ() * 3;
        for (int y = ground + 1; y <= dy; y++)
            p.set(lx, y, lz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, door));
        p.set(lx - door.getStepX(), dy, lz - door.getStepZ(), deck);
        // inside: a bedroll and a barrel
        p.set(cx - door.getStepX(), dy + 1, cz - door.getStepZ(), Blocks.WHITE_CARPET);
        p.set(cx + door.getStepZ(), dy + 1, cz + door.getStepX(), Blocks.BARREL);
        p.set(cx, dy + 3, cz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private void walkway(Placer p, int x1, int z1, int x2, int z2, int y, BlockState plank) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / Math.max(1, steps), z = z1 + (z2 - z1) * i / Math.max(1, steps);
            if (Math.abs(x - x1) <= 3 && Math.abs(z - z1) <= 3 || Math.abs(x - x2) <= 3 && Math.abs(z - z2) <= 3) continue;
            p.set(x, y, z, plank);
            p.set(x + 1, y, z, plank);
            if (i % 4 == 0) {
                int g = ground(x - Locations.ISLAND_X, z - Locations.ISLAND_Z);
                for (int yy = g + 1; yy < y; yy++) p.set(x, yy, z, Blocks.BAMBOO_FENCE);
            }
        }
    }

    private void longhouse(Placer p, int cx, int y, int cz) {
        BlockState wall = ModBlocks.PALM_PLANKS.get().defaultBlockState();
        BlockState post = ModBlocks.PALM_LOG.get().defaultBlockState();
        BlockState thatch = ModBlocks.THATCH.get().defaultBlockState();
        p.fill(cx - 7, y, cz - 4, cx + 7, y, cz + 4, wall);
        for (int yy = y + 1; yy <= y + 4; yy++)
            for (int x = cx - 7; x <= cx + 7; x++)
                for (int z = cz - 4; z <= cz + 4; z++) {
                    boolean edge = x == cx - 7 || x == cx + 7 || z == cz - 4 || z == cz + 4;
                    boolean postPos = edge && (x - cx) % 3 == 0;
                    p.set(x, yy, z, edge ? (postPos ? post : wall) : Blocks.AIR.defaultBlockState());
                }
        p.clear(cx - 1, y + 1, cz + 4, cx + 1, y + 3, cz + 4);
        for (int x = cx - 5; x <= cx + 5; x += 5) p.clear(x, y + 2, cz - 4, x, y + 3, cz - 4);
        for (int k = 0; k <= 5; k++) p.fill(cx - 8, y + 5 + k, cz - 5 + k, cx + 8, y + 5 + k, cz + 5 - k, thatch);
        p.set(cx, y + 1, cz - 3, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.SOUTH));
        p.set(cx - 3, y + 1, cz - 3, facing(ModBlocks.TOTEM.get(), Direction.SOUTH));
        p.set(cx + 3, y + 1, cz - 3, facing(ModBlocks.TOTEM.get(), Direction.SOUTH));
        p.fill(cx - 6, y + 1, cz - 3, cx - 6, y + 1, cz + 3, Blocks.WHITE_CARPET);
        p.fill(cx + 6, y + 1, cz - 3, cx + 6, y + 1, cz + 3, Blocks.LIGHT_BLUE_CARPET);
        p.set(cx, y + 4, cz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        p.set(cx - 2, y + 1, cz, Blocks.CARTOGRAPHY_TABLE);
        p.frame(cx, y + 3, cz - 4, Direction.SOUTH, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPASS), false);
    }

    // ---------------------------------------------------------------- dock and boat
    public static int shoreX() {
        int z = VILLAGE[1];
        for (int dx = VILLAGE[0] + 12; dx < 220; dx++) if (height(dx, z) <= SEA) return dx;
        return VILLAGE[0] + 40;
    }

    public static BlockPos boatOrigin() { return new BlockPos(wx(shoreX() + 4), SEA, wz(VILLAGE[1] + 4)); }
    public static BlockPos boatFramePos() { return new BlockPos(wx(shoreX() + 13), SEA + 2, wz(VILLAGE[1])); }

    public static BlockPos dockStart() {
        int dx = shoreX() - 2;
        return new BlockPos(wx(dx), Math.max(SEA + 2, height(dx, VILLAGE[1]) + 1), wz(VILLAGE[1]));
    }

    private void dock(Placer p) {
        int start = shoreX() - 4, end = shoreX() + 16;
        BlockState plank = ModBlocks.PALM_PLANKS.get().defaultBlockState();
        for (int dx = start; dx <= end; dx++) {
            for (int dz = VILLAGE[1] - 1; dz <= VILLAGE[1] + 1; dz++) {
                p.set(wx(dx), SEA + 1, wz(dz), plank);
                for (int y = SEA + 2; y <= SEA + 4; y++) p.set(wx(dx), y, wz(dz), Blocks.AIR.defaultBlockState());
            }
            if ((dx - start) % 4 == 0) {
                for (int dz : new int[]{VILLAGE[1] - 2, VILLAGE[1] + 2}) {
                    for (int y = SEA_FLOOR; y <= SEA + 2; y++) p.set(wx(dx), y, wz(dz), ModBlocks.PALM_LOG.get().defaultBlockState());
                    p.set(wx(dx), SEA + 3, wz(dz), Blocks.LANTERN);
                }
            }
        }
        BlockPos frame = boatFramePos();
        p.set(frame, facing(ModBlocks.BOAT_FRAME.get(), Direction.SOUTH));
        p.sign(frame.getX() - 1, frame.getY(), frame.getZ(), Direction.WEST, DyeColor.BROWN, "TEO'S", "BOATYARD", "Bring wood.", "");
    }

    public static void boatStage(Placer p, int stage) {
        BlockPos o = boatOrigin();
        int x0 = o.getX(), y = o.getY(), z0 = o.getZ();
        BlockState log = axis(ModBlocks.PALM_LOG.get(), Direction.Axis.X);
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
                    BlockState rail = x % 3 == 0 ? ModBlocks.FUSELAGE_STRIPE.get().defaultBlockState() : ModBlocks.FUSELAGE.get().defaultBlockState();
                    p.set(x0 + x, y + 1, z0, rail);
                    p.set(x0 + x, y + 1, z0 + 4, rail);
                }
                for (int z = 1; z <= 3; z++) { p.set(x0 + 1, y + 1, z0 + z, plank); p.set(x0 + 10, y + 1, z0 + z, plank); }
                p.set(x0 + 11, y + 1, z0 + 2, plank);
                p.set(x0 + 6, y + 1, z0, Blocks.AIR.defaultBlockState());
            }
            case 3 -> {
                for (int yy = y + 1; yy <= y + 9; yy++) p.set(x0 + 6, yy, z0 + 2, ModBlocks.PALM_LOG.get().defaultBlockState());
                p.set(x0 + 6, y + 10, z0 + 2, Blocks.BAMBOO_FENCE);
                for (int yy = y + 2; yy <= y + 4; yy++) {
                    p.set(x0 + 6, yy, z0 + 1, Blocks.CHAIN.defaultBlockState());
                    p.set(x0 + 6, yy, z0 + 3, Blocks.CHAIN.defaultBlockState());
                }
            }
            case 4 -> {
                for (int yy = y + 4; yy <= y + 9; yy++)
                    for (int z = 0; z <= 4; z++) {
                        if (z == 2 && (yy == y + 4 || yy == y + 9)) continue;
                        p.set(x0 + 7, yy, z0 + z, yy == y + 6 ? Blocks.BLUE_WOOL.defaultBlockState() : Blocks.WHITE_WOOL.defaultBlockState());
                    }
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

    // ---------------------------------------------------------------- the Eye
    public static int summit() { return (int) SUMMIT; }

    public static BlockPos station(int ox, int oy, int oz) {
        return new BlockPos(wx(PEAK[0] + ox), summit() + oy, wz(PEAK[1] + oz));
    }

    private void station(Placer p) {
        int sx = wx(PEAK[0]), sz = wz(PEAK[1]), sy = summit();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState wall = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState rib = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState plinth = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState glass = Blocks.TINTED_GLASS.defaultBlockState();
        BlockState panel = ModBlocks.BUNKER_PANEL.get().defaultBlockState();
        Random r = new Random(17);

        // the summit yard: gravel and worn concrete, with the slope cut back behind the building
        p.fill(sx - 15, sy, sz - 12, sx + 15, sy, sz + 15, Blocks.GRAVEL);
        p.clear(sx - 15, sy + 1, sz - 12, sx + 15, sy + 30, sz + 15);
        for (int x = sx - 12; x <= sx + 12; x++)
            for (int z = sz - 9; z <= sz + 12; z++)
                if (r.nextFloat() < 0.55f) p.set(x, sy, z, r.nextFloat() < 0.7f ? Blocks.STONE.defaultBlockState() : Blocks.ANDESITE.defaultBlockState());
        p.fill(sx - 11, sy, sz - 8, sx + 11, sy, sz + 8, Blocks.POLISHED_ANDESITE);

        // ---- main block: plinth, ribbed concrete walls, window strips, flat roof with parapet
        for (int y = sy + 1; y <= sy + 5; y++)
            for (int x = sx - 10; x <= sx + 10; x++)
                for (int z = sz - 7; z <= sz + 7; z++) {
                    boolean edge = x == sx - 10 || x == sx + 10 || z == sz - 7 || z == sz + 7;
                    if (!edge) { p.set(x, y, z, air); continue; }
                    boolean corner = (x == sx - 10 || x == sx + 10) && (z == sz - 7 || z == sz + 7);
                    boolean ribPos = corner || ((z == sz - 7 || z == sz + 7) && (x - sx) % 4 == 0) || ((x == sx - 10 || x == sx + 10) && (z - sz) % 4 == 0);
                    BlockState s = y == sy + 1 ? plinth : ribPos ? rib : wall;
                    boolean windowRow = y == sy + 3 || y == sy + 4;
                    boolean south = z == sz + 7 && (Math.abs(x - sx) >= 5 && Math.abs(x - sx) <= 9);
                    boolean side = (x == sx - 10 || x == sx + 10) && Math.abs(z - sz) <= 5 && z != sz;
                    if (windowRow && !ribPos && (south || side)) s = glass;
                    p.set(x, y, z, s);
                }
        p.fill(sx - 11, sy + 6, sz - 8, sx + 11, sy + 6, sz + 8, Blocks.SMOOTH_STONE);
        for (int x = sx - 11; x <= sx + 11; x++) {
            p.set(x, sy + 7, sz - 8, Blocks.ANDESITE_WALL);
            p.set(x, sy + 7, sz + 8, Blocks.ANDESITE_WALL);
        }
        for (int z = sz - 8; z <= sz + 8; z++) {
            p.set(sx - 11, sy + 7, z, Blocks.ANDESITE_WALL);
            p.set(sx + 11, sy + 7, z, Blocks.ANDESITE_WALL);
        }
        // interior ceiling with strip lights
        p.fill(sx - 9, sy + 5, sz - 6, sx + 9, sy + 5, sz + 6, panel);
        for (int x = sx - 8; x <= sx + 8; x += 4) for (int z = sz - 5; z <= sz + 5; z += 5) p.set(x, sy + 5, z, ModBlocks.BUNKER_LIGHT.get().defaultBlockState());
        p.fill(sx - 9, sy, sz - 6, sx + 9, sy, sz + 6, Blocks.POLISHED_DEEPSLATE);
        // inner walls: server room (east) and bunk room (south-east)
        for (int y = sy + 1; y <= sy + 4; y++) {
            for (int z = sz - 6; z <= sz + 6; z++) p.set(sx + 4, y, z, panel);
            for (int x = sx + 5; x <= sx + 9; x++) p.set(x, y, sz + 2, panel);
        }
        p.door(sx + 4, sy + 1, sz - 2, Blocks.IRON_DOOR, Direction.WEST, true);
        p.door(sx + 4, sy + 1, sz + 4, Blocks.IRON_DOOR, Direction.WEST, true);

        // ---- entrance: recessed door under a cantilevered canopy, steps, keypad and signs
        p.door(sx, sy + 1, sz + 7, Blocks.IRON_DOOR, Direction.NORTH, false);
        p.set(sx - 1, sy + 1, sz + 7, rib);
        p.set(sx + 1, sy + 1, sz + 7, rib);
        p.set(sx + 1, sy + 2, sz + 8, facing(ModBlocks.KEYPAD.get(), Direction.SOUTH));
        p.sign(sx - 1, sy + 3, sz + 8, Direction.SOUTH, DyeColor.RED, "MERIDIAN", "EXPERIENCES", "LAB 4", "KEEP SMILING");
        p.sign(sx - 3, sy + 2, sz + 8, Direction.SOUTH, DyeColor.WHITE, "AUTHORISED", "PERSONNEL", "ONLY", "");
        for (int x = sx - 3; x <= sx + 3; x++)
            for (int z = sz + 8; z <= sz + 10; z++) p.set(x, sy + 4, z, slab(Blocks.SMOOTH_STONE_SLAB, true));
        for (int x : new int[]{sx - 3, sx + 3}) p.set(x, sy + 3, sz + 10, Blocks.CHAIN);
        p.set(sx, sy + 3, sz + 9, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        for (int x = sx - 2; x <= sx + 2; x++) {
            p.set(x, sy, sz + 8, Blocks.POLISHED_ANDESITE);
            p.set(x, sy, sz + 9, Blocks.POLISHED_ANDESITE);
        }
        for (int k = 10; k <= 15; k++) p.set(sx, sy, sz + k, Blocks.DIRT_PATH);

        // ---- interior: monitor wall, desks, HALO terminal, server room, bunk room
        for (int x = sx - 9; x <= sx + 3; x++)
            for (int y = sy + 2; y <= sy + 4; y++)
                p.set(x, y, sz - 6, facing(ModBlocks.MONITOR.get(), Direction.SOUTH).setValue(StateInteractBlock.MODE, (x * 3 + y) % 4 == 0 ? 1 : 0));
        for (int x = sx - 8; x <= sx + 2; x++) {
            p.set(x, sy + 1, sz - 4, Blocks.POLISHED_BLACKSTONE);
            p.set(x, sy + 2, sz - 4, slab(Blocks.POLISHED_BLACKSTONE_SLAB, false));
            if (x % 3 == 0) p.set(x, sy + 1, sz - 2, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.NORTH));
        }
        p.set(sx - 3, sy + 1, sz + 1, facing(ModBlocks.HALO_TERMINAL.get(), Direction.SOUTH));
        p.fill(sx - 4, sy, sz, sx - 2, sy, sz + 2, Blocks.BLACK_CONCRETE);
        p.set(sx - 3, sy, sz + 1, Blocks.RED_CONCRETE);
        for (int z = sz - 6; z <= sz + 1; z++) {
            p.set(sx + 9, sy + 1, z, facing(ModBlocks.SERVER_RACK.get(), Direction.WEST));
            p.set(sx + 9, sy + 2, z, facing(ModBlocks.SERVER_RACK.get(), Direction.WEST));
            p.set(sx + 9, sy + 3, z, facing(ModBlocks.SERVER_RACK.get(), Direction.WEST));
        }
        p.set(sx + 6, sy + 1, sz - 3, facing(ModBlocks.TRANSMITTER.get(), Direction.WEST));
        p.set(sx + 6, sy + 2, sz - 3, facing(ModBlocks.TRANSMITTER.get(), Direction.WEST));
        for (int y = sy + 3; y <= sy + 4; y++) p.set(sx + 6, y, sz - 3, Blocks.CHAIN);
        p.set(sx + 6, sy + 1, sz + 5, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
        p.set(sx + 7, sy + 1, sz + 5, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        p.set(sx + 8, sy + 1, sz + 3, Blocks.BARREL);
        p.set(sx + 9, sy + 1, sz + 6, Blocks.BOOKSHELF);
        p.clue(sx + 6, sy + 1, sz + 3, ModBlocks.CLUE_NOTE.get(), "lab_contract");
        p.clue(sx - 7, sy + 3, sz - 4, ModBlocks.CLUE_NOTE.get(), "viewer_mail");
        for (int x : new int[]{sx - 8, sx - 6}) p.set(x, sy + 1, sz + 5, Blocks.CAULDRON);
        p.set(sx - 7, sy + 1, sz + 6, Blocks.WATER_CAULDRON);

        // ---- the Eye: an octagonal observation tower on the roof, its south face one enormous eye
        int ex = sx - 4, ez = sz - 1, ey0 = sy + 6, ey1 = sy + 15;
        for (int y = ey0; y <= ey1; y++)
            for (int dx = -6; dx <= 6; dx++)
                for (int dz = -6; dz <= 6; dz++) {
                    int m = Math.abs(dx) + Math.abs(dz);
                    if (Math.max(Math.abs(dx), Math.abs(dz)) > 6 || m > 9) continue;
                    boolean shell = Math.abs(dx) == 6 || Math.abs(dz) == 6 || m >= 8;
                    if (y == ey0) { p.set(ex + dx, y, ez + dz, Blocks.SMOOTH_STONE); continue; }
                    if (y == ey1) { p.set(ex + dx, y, ez + dz, Blocks.POLISHED_ANDESITE); continue; }
                    p.set(ex + dx, y, ez + dz, shell ? (m >= 8 && (y - ey0) % 3 == 0 ? rib : wall) : air);
                }
        for (int dx = -5; dx <= 5; dx++)
            for (int dz = -5; dz <= 5; dz++)
                if (Math.abs(dx) + Math.abs(dz) <= 7) p.set(ex + dx, ey1 + 1, ez + dz, slab(Blocks.SMOOTH_STONE_SLAB, false));
        p.set(ex, ey1 + 1, ez, Blocks.LIGHTNING_ROD);
        // the eye itself, in the flat south face
        int faceZ = ez + 6, eyeY = ey0 + 5;
        for (int dx = -4; dx <= 4; dx++)
            for (int dy = -3; dy <= 3; dy++) {
                double d = Math.sqrt(dx * dx + dy * dy * 1.6);
                BlockState s;
                if (d > 4.3) continue;
                else if (d > 3.4) s = Blocks.BLACK_CONCRETE.defaultBlockState();
                else if (d > 2.3) s = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
                else if (d > 1.2) s = Blocks.RED_STAINED_GLASS.defaultBlockState();
                else s = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
                p.set(ex + dx, eyeY + dy, faceZ, s);
            }
        p.set(ex, eyeY + 4, faceZ, Blocks.BLACK_CONCRETE);
        p.set(ex, eyeY - 4, faceZ, Blocks.BLACK_CONCRETE);
        // the viewing room inside, reached by a ladder from the hall
        for (int y = sy + 1; y <= sy + 5; y++) p.set(sx - 9, y, sz + 1, rib);
        for (int y = sy + 1; y <= ey0; y++) p.set(sx - 8, y, sz + 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        p.set(ex, ey0 + 1, ez + 3, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.SOUTH));
        p.set(ex - 2, ey0 + 1, ez + 4, Blocks.POLISHED_BLACKSTONE);
        p.frame(ex - 2, ey0 + 2, ez + 4, Direction.UP, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPYGLASS), false);
        p.set(ex + 3, ey0 + 1, ez - 3, Blocks.LECTERN);
        p.set(ex, ey1 - 1, ez, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        p.sign(ex + 2, ey0 + 2, ez - 5, Direction.SOUTH, DyeColor.WHITE, "OBSERVATION", "Subject visible", "from all", "angles");

        // ---- broadcast mast: a lattice truss with dishes and a blinking beacon
        int mx = sx + 6, mz = sz - 3;
        for (int y = sy + 7; y <= sy + 28; y++) {
            for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) p.set(mx + c[0], y, mz + c[1], Blocks.IRON_BARS);
            if (y % 4 == 0)
                for (int k = -1; k <= 1; k++) {
                    p.set(mx + k, y, mz - 1, Blocks.IRON_BARS); p.set(mx + k, y, mz + 1, Blocks.IRON_BARS);
                    p.set(mx - 1, y, mz + k, Blocks.IRON_BARS); p.set(mx + 1, y, mz + k, Blocks.IRON_BARS);
                }
        }
        for (int y = sy + 29; y <= sy + 33; y++) p.set(mx, y, mz, Blocks.IRON_BARS);
        p.set(mx, sy + 34, mz, Blocks.LIGHTNING_ROD);
        p.set(mx, sy + 29, mz, Blocks.REDSTONE_BLOCK);
        p.set(mx, sy + 30, mz, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
        p.set(sx + 5, sy + 16, sz - 3, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
        p.set(sx + 7, sy + 16, sz - 3, Blocks.REDSTONE_BLOCK);
        dish(p, mx, sy + 22, mz + 2, Direction.SOUTH);
        dish(p, mx - 2, sy + 13, mz, Direction.WEST);
        dish(p, mx + 2, sy + 18, mz, Direction.EAST);

        // ---- roof plant: vents, solar panels, an AC unit
        for (int x = sx + 1; x <= sx + 9; x += 2) {
            p.set(x, sy + 7, sz + 4, Blocks.DAYLIGHT_DETECTOR);
            p.set(x, sy + 7, sz + 6, Blocks.DAYLIGHT_DETECTOR);
        }
        p.set(sx + 9, sy + 7, sz - 6, facing(Blocks.BLAST_FURNACE, Direction.SOUTH));
        p.set(sx + 8, sy + 7, sz - 6, facing(Blocks.SMOKER, Direction.SOUTH));
        p.set(sx + 3, sy + 7, sz + 1, Blocks.CAULDRON);
        p.set(sx + 3, sy + 8, sz + 1, trapdoor(Blocks.IRON_TRAPDOOR, Direction.NORTH, true, false));

        // ---- generator annex to the west, with fuel tanks and pipes
        int gx = sx - 13, gz = sz + 4;
        p.fill(gx - 1, sy + 1, gz - 3, gx + 2, sy + 3, gz + 3, Blocks.IRON_BLOCK);
        p.fill(gx - 1, sy + 4, gz - 3, gx + 2, sy + 4, gz + 3, slab(Blocks.SMOOTH_STONE_SLAB, false));
        p.set(gx + 2, sy + 2, gz, facing(Blocks.BLAST_FURNACE, Direction.EAST));
        p.set(gx + 2, sy + 2, gz - 2, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACING, Direction.EAST));
        for (int[] t : new int[][]{{-14, -4}, {-12, -4}}) {
            p.set(sx + t[0], sy + 1, sz + t[1], Blocks.EXPOSED_COPPER);
            p.set(sx + t[0], sy + 2, sz + t[1], Blocks.EXPOSED_COPPER);
            p.set(sx + t[0], sy + 3, sz + t[1], Blocks.CUT_COPPER_SLAB);
        }

        // ---- perimeter fence with floodlights and a gate on the path
        for (int x = sx - 15; x <= sx + 15; x++)
            for (int z : new int[]{sz - 12, sz + 15}) {
                if (z == sz + 15 && Math.abs(x - sx) <= 1) continue;
                fencePost(p, x, sy + 1, z, (x - sx) % 6 == 0);
            }
        for (int z = sz - 12; z <= sz + 15; z++)
            for (int x : new int[]{sx - 15, sx + 15}) fencePost(p, x, sy + 1, z, (z - sz) % 6 == 0);
        p.sign(sx + 2, sy + 2, sz + 15, Direction.SOUTH, DyeColor.RED, "DANGER", "BROADCAST IN", "PROGRESS", "Smile.");

        // ---- years of weather: moss, vines and rust
        for (int i = 0; i < 70; i++) {
            int x = sx - 11 + r.nextInt(23), z = sz - 9 + r.nextInt(19);
            if (p.get(x, sy + 1, z).isAir() && (Math.abs(x - sx) > 10 || Math.abs(z - sz) > 7)) p.set(x, sy + 1, z, r.nextBoolean() ? Blocks.MOSS_CARPET : Blocks.FERN);
        }
        for (int x = sx - 10; x <= sx + 10; x++)
            if (r.nextFloat() < 0.45f)
                for (int y = sy + 5 - r.nextInt(3); y <= sy + 5; y++)
                    p.set(x, y, sz - 8, Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, true));
        for (int z = sz - 6; z <= sz + 6; z++)
            if (r.nextFloat() < 0.35f)
                for (int y = sy + 4 - r.nextInt(3); y <= sy + 5; y++)
                    p.set(sx - 11, y, z, Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true));
    }

    private static void fencePost(Placer p, int x, int y, int z, boolean light) {
        p.set(x, y, z, Blocks.IRON_BARS);
        p.set(x, y + 1, z, Blocks.IRON_BARS);
        if (light) {
            p.set(x, y + 2, z, Blocks.IRON_BARS);
            p.set(x, y + 3, z, Blocks.LANTERN);
        }
    }

    /** A small satellite dish facing {@code d}: a disc of slabs on a short arm. */
    private static void dish(Placer p, int x, int y, int z, Direction d) {
        Direction side = d.getClockWise();
        for (int a = -1; a <= 1; a++)
            for (int b = -1; b <= 1; b++) {
                if (Math.abs(a) + Math.abs(b) == 2) continue;
                p.set(x + side.getStepX() * a + d.getStepX(), y + b, z + side.getStepZ() * a + d.getStepZ(), Blocks.SMOOTH_QUARTZ);
            }
        p.set(x + d.getStepX() * 2, y, z + d.getStepZ() * 2, Blocks.IRON_BARS);
        p.set(x, y, z, Blocks.IRON_BLOCK);
    }

    public static void openStationDoor(Placer p, boolean open) {
        int sx = wx(PEAK[0]), sz = wz(PEAK[1]), sy = summit();
        for (int y = sy + 1; y <= sy + 2; y++) {
            BlockState s = p.get(sx, y, sz + 7);
            if (s.hasProperty(DoorBlock.OPEN)) p.set(sx, y, sz + 7, s.setValue(DoorBlock.OPEN, open));
        }
    }

    public static void stationPowerDown(Placer p) {
        int sx = wx(PEAK[0]), sz = wz(PEAK[1]), sy = summit();
        for (int x = sx - 9; x <= sx + 3; x++)
            for (int y = sy + 2; y <= sy + 4; y++) {
                BlockState s = p.get(x, y, sz - 6);
                if (s.hasProperty(StateInteractBlock.MODE)) p.set(x, y, sz - 6, s.setValue(StateInteractBlock.MODE, 2));
            }
        for (int y = sy + 1; y <= sy + 2; y++) {
            BlockState s = p.get(sx + 6, y, sz - 3);
            if (s.hasProperty(StateInteractBlock.LIT)) p.set(sx + 6, y, sz - 3, s.setValue(StateInteractBlock.LIT, false));
        }
        p.set(sx + 5, sy + 16, sz - 3, Blocks.REDSTONE_LAMP.defaultBlockState());
        p.set(sx + 7, sy + 16, sz - 3, Blocks.COAL_BLOCK);
    }

    // ---------------------------------------------------------------- the Hollow Grove
    public static int groveY() { return ground(GROVE[0], GROVE[1]); }

    public static BlockPos grove(int ox, int oz) { return new BlockPos(wx(GROVE[0] + ox), groveY() + 1, wz(GROVE[1] + oz)); }

    private void grove(Placer p) {
        int gx = wx(GROVE[0]), gz = wz(GROVE[1]), gy = groveY();
        Random r = new Random(41);
        // an uneven clearing of dark, rooty soil with bones working their way up
        for (int x = -12; x <= 12; x++)
            for (int z = -14; z <= 10; z++) {
                double d = Math.sqrt(x * x + (z + 2) * (z + 2)) + (hash(gx + x, gz + z, 31) - 0.5) * 3;
                if (d > 12) continue;
                double n = hash(gx + x, gz + z, 32);
                BlockState g = n < 0.35 ? Blocks.PODZOL.defaultBlockState() : n < 0.6 ? Blocks.COARSE_DIRT.defaultBlockState()
                        : n < 0.8 ? Blocks.ROOTED_DIRT.defaultBlockState() : n < 0.95 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.BONE_BLOCK.defaultBlockState();
                p.set(gx + x, gy, gz + z, g);
                if (d < 10) p.clear(gx + x, gy + 1, gz + z, gx + x, gy + 16, gz + z);
                if (d >= 4 && d < 10 && n > 0.86) p.set(gx + x, gy + 1, gz + z, n > 0.93 ? Blocks.MOSS_CARPET : Blocks.DEAD_BUSH);
            }

        // the great hollow tree: bleached, split, its insides papered with photographs
        int cx = gx - 1, cz = gz - 10;
        BlockState bark = Blocks.STRIPPED_BIRCH_WOOD.defaultBlockState();
        BlockState bark2 = Blocks.MUSHROOM_STEM.defaultBlockState();
        for (int y = gy - 1; y <= gy + 13; y++)
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    int top = gy + 13 - (Math.abs(dx + dz * 3 + 7) % 4);
                    if (y > top) continue;
                    boolean inside = Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && y >= gy + 1 && y <= gy + 6;
                    boolean door = dx == 0 && dz == 2 && y >= gy + 1 && y <= gy + 3;
                    p.set(cx + dx, y, cz + dz, inside || door ? Blocks.AIR.defaultBlockState() : (hash(cx + dx, y, cz + dz) < 0.3 ? bark2 : bark));
                }
        // roots spreading out over the ground
        for (Direction d : new Direction[]{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH}) {
            for (int k = 3; k <= 5; k++) {
                int x = cx + d.getStepX() * k + (d.getAxis() == Direction.Axis.Z ? (k % 2) - 1 : 0);
                int z = cz + d.getStepZ() * k + (d.getAxis() == Direction.Axis.X ? (k % 2) : 0);
                if (d == Direction.SOUTH && Math.abs(x - cx) <= 1) continue;
                p.set(x, gy + (k == 5 ? 0 : 1), z, axis(Blocks.STRIPPED_BIRCH_LOG, d.getAxis()));
            }
        }
        // bare branches at the crown
        for (int[] b : new int[][]{{3, 0, 10}, {-3, 0, 11}, {0, -3, 12}, {2, 2, 9}, {-2, -2, 10}}) {
            for (int k = 1; k <= 3; k++) {
                int x = cx + Integer.signum(b[0]) * (Math.abs(b[0]) - 1 + k), z = cz + Integer.signum(b[1]) * (Math.abs(b[1]) - 1 + k);
                p.set(x, gy + b[2] + k / 2, z, Blocks.STRIPPED_BIRCH_WOOD);
            }
        }
        // inside: the photographs, a cold blue light, more polaroids pinned to the inner walls
        for (int x = cx - 1; x <= cx + 1; x++)
            for (int y = gy + 2; y <= gy + 3; y++) p.set(x, y, cz - 1, facing(ModBlocks.POLAROID_WALL.get(), Direction.SOUTH));
        p.set(cx, gy + 6, cz, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        net.minecraft.world.item.Item[] photos = {com.airlinestory.registry.ModItems.POLAROID_SEAT.get(), com.airlinestory.registry.ModItems.POLAROID_BEACH.get(),
                com.airlinestory.registry.ModItems.POLAROID_GROVE.get()};
        p.frame(cx - 1, gy + 4, cz, Direction.EAST, new net.minecraft.world.item.ItemStack(photos[0]), true);
        p.frame(cx + 1, gy + 4, cz, Direction.WEST, new net.minecraft.world.item.ItemStack(photos[1]), true);
        p.frame(cx - 1, gy + 2, cz + 1, Direction.EAST, new net.minecraft.world.item.ItemStack(photos[2]), true);
        p.frame(cx + 1, gy + 2, cz + 1, Direction.WEST, new net.minecraft.world.item.ItemStack(photos[0]), true);

        // the lone seat facing the tree, inside a ring of stones and burnt-down candles
        int sx = gx - 1, sz = gz - 4;
        p.set(sx, gy + 1, sz, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.NORTH));
        for (int k = 0; k < 9; k++) {
            double a = k * Math.PI * 2 / 9 + 0.3;
            int x = sx + (int) Math.round(Math.cos(a) * 4), z = sz + (int) Math.round(Math.sin(a) * 4);
            if (z < sz - 3 && Math.abs(x - sx) <= 1) continue;
            p.set(x, gy + 1, z, k % 3 == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
            if (k % 2 == 0) p.set(x, gy + 2, z, Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, k % 4 == 0));
        }
        p.set(gx + 1, gy + 1, gz - 3, Blocks.STRIPPED_BIRCH_LOG);
        p.clue(gx + 1, gy + 2, gz - 3, ModBlocks.CLUE_NOTE.get(), "hollow_note");

        // bleached trees all around, each with a photograph nailed at eye height, facing the seat
        for (int k = 0; k < 11; k++) {
            double a = k * Math.PI * 2 / 11 + r.nextDouble() * 0.3;
            double rad = 7.5 + r.nextDouble() * 3;
            int tx = sx + (int) Math.round(Math.cos(a) * rad), tz = sz + (int) Math.round(Math.sin(a) * rad);
            if (Math.abs(tx - cx) <= 3 && Math.abs(tz - cz) <= 3) continue;
            int h = groundAt(tx, tz);
            bleachedTree(p, tx, h + 1, tz, r);
            Direction face = Math.abs(sx - tx) > Math.abs(sz - tz) ? (sx > tx ? Direction.EAST : Direction.WEST) : (sz > tz ? Direction.SOUTH : Direction.NORTH);
            if (r.nextFloat() < 0.8f)
                p.frame(tx + face.getStepX(), h + 2, tz + face.getStepZ(), face, new net.minecraft.world.item.ItemStack(photos[k % 3]), false);
        }
        // the watchers: three effigies half-hidden at the treeline
        for (int[] e : new int[][]{{9, -6}, {-10, 1}, {6, 7}}) {
            int x = gx + e[0], z = gz + e[1], h = groundAt(x, z);
            p.set(x, h + 1, z, facing(ModBlocks.HOLLOW_EFFIGY.get(), Direction.SOUTH));
        }
        // ghost orchids glowing among the roots
        for (int k = 0; k < 14; k++) {
            double a = r.nextDouble() * Math.PI * 2, rad = 5 + r.nextDouble() * 5;
            int x = sx + (int) Math.round(Math.cos(a) * rad), z = sz + (int) Math.round(Math.sin(a) * rad);
            int h = groundAt(x, z);
            if (p.get(x, h + 1, z).isAir()) p.set(x, h + 1, z, ModBlocks.GHOST_ORCHID.get());
        }
    }

    private static int groundAt(int x, int z) {
        return Math.max(SEA, ground(x - Locations.ISLAND_X, z - Locations.ISLAND_Z));
    }

    /** A gnarled dead tree: the trunk kinks sideways partway up, limbs climb diagonally at staggered heights. */
    private static void bleachedTree(Placer p, int x, int y, int z, Random r) {
        BlockState log = (r.nextInt(3) == 0 ? Blocks.STRIPPED_DARK_OAK_LOG : Blocks.STRIPPED_BIRCH_LOG).defaultBlockState();
        int height = 6 + r.nextInt(6);
        int kink = 2 + r.nextInt(3);
        Direction lean = Direction.from2DDataValue(r.nextInt(4));
        int cx = x, cz = z;
        for (int i = 0; i < height; i++) {
            if (i == kink) {
                p.set(cx + lean.getStepX(), y + i, cz + lean.getStepZ(), log.setValue(RotatedPillarBlock.AXIS, lean.getAxis()));
                cx += lean.getStepX();
                cz += lean.getStepZ();
            } else {
                p.set(cx, y + i, cz, log);
            }
        }
        int limbs = 1 + r.nextInt(2);
        int used = -1;
        for (int i = 0; i < limbs; i++) {
            Direction d;
            do d = Direction.from2DDataValue(r.nextInt(4)); while (d.get2DDataValue() == used || d == lean.getOpposite());
            used = d.get2DDataValue();
            int by = y + kink + 1 + r.nextInt(Math.max(1, height - kink - 2)) + i;
            int len = 2 + r.nextInt(2);
            for (int k = 1; k <= len; k++) {
                int lx = cx + d.getStepX() * k, lz = cz + d.getStepZ() * k, ly = by + k / 2 + (k == len ? 1 : 0);
                p.set(lx, ly, lz, k == len ? log : log.setValue(RotatedPillarBlock.AXIS, d.getAxis()));
            }
        }
    }

    // ---------------------------------------------------------------- lookout and radio tower
    public static BlockPos lookoutTop() {
        int h = ground(LOOKOUT[0], LOOKOUT[1]);
        return new BlockPos(wx(LOOKOUT[0]), h + 17, wz(LOOKOUT[1]));
    }

    private void lookout(Placer p) {
        int lx = wx(LOOKOUT[0]), lz = wz(LOOKOUT[1]), ly = ground(LOOKOUT[0], LOOKOUT[1]);
        for (int y = ly + 1; y <= ly + 16; y++) {
            for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) p.set(lx + c[0], y, lz + c[1], Blocks.IRON_BARS);
            if (y % 4 == 0) {
                for (int k = -2; k <= 2; k++) {
                    p.set(lx + k, y, lz - 2, Blocks.IRON_BARS); p.set(lx + k, y, lz + 2, Blocks.IRON_BARS);
                    p.set(lx - 2, y, lz + k, Blocks.IRON_BARS); p.set(lx + 2, y, lz + k, Blocks.IRON_BARS);
                }
            }
            p.set(lx, y, lz + 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
            p.set(lx, y, lz + 2, Blocks.IRON_BARS);
        }
        for (int y = ly + 1; y <= ly + 16; y++) p.set(lx, y, lz + 2, Blocks.SPRUCE_PLANKS);
        p.fill(lx - 2, ly + 17, lz - 2, lx + 2, ly + 17, lz + 2, Blocks.SPRUCE_PLANKS);
        p.set(lx, ly + 17, lz + 1, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH, true, true));
        for (int k = -2; k <= 2; k++) {
            p.set(lx + k, ly + 18, lz - 2, Blocks.SPRUCE_FENCE); p.set(lx + k, ly + 18, lz + 2, Blocks.SPRUCE_FENCE);
            p.set(lx - 2, ly + 18, lz + k, Blocks.SPRUCE_FENCE); p.set(lx + 2, ly + 18, lz + k, Blocks.SPRUCE_FENCE);
        }
        p.set(lx, ly + 18, lz + 2, Blocks.AIR);
        for (int y = ly + 18; y <= ly + 24; y++) p.set(lx + 2, y, lz - 2, Blocks.IRON_BARS);
        p.set(lx + 2, ly + 25, lz - 2, Blocks.LIGHTNING_ROD);
        p.set(lx - 1, ly + 18, lz - 1, Blocks.NOTE_BLOCK);
        p.clue(lx + 1, ly + 18, lz - 1, ModBlocks.CLUE_TAPE.get(), "tape_2");
    }

    // ---------------------------------------------------------------- iteration 3: the burned village
    private void burnedVillage(Placer p) {
        Random r = new Random(33);
        int bx = wx(BURNED[0]), bz = wz(BURNED[1]), by = ground(BURNED[0], BURNED[1]);
        int[][] huts = {{-7, -5}, {6, -6}, {-6, 6}, {7, 5}};
        for (int[] h : huts) {
            int cx = bx + h[0], cz = bz + h[1];
            for (int x = cx - 2; x <= cx + 2; x++)
                for (int z = cz - 2; z <= cz + 2; z++) {
                    p.set(x, by, z, r.nextBoolean() ? Blocks.COAL_BLOCK : Blocks.BLACKSTONE);
                    boolean edge = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
                    if (edge) {
                        int hgt = r.nextInt(4);
                        for (int y = by + 1; y <= by + hgt; y++)
                            p.set(x, y, z, r.nextFloat() < 0.3f ? Blocks.BASALT.defaultBlockState() : Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
                    } else if (r.nextFloat() < 0.2f) {
                        p.set(x, by + 1, z, Blocks.COBWEB);
                    }
                }
        }
        p.set(bx, by, bz, Blocks.SOUL_SOIL);
        p.set(bx, by + 1, bz, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        for (int k = 0; k < 14; k++) p.set(bx + r.nextInt(17) - 8, by + 1, bz + r.nextInt(17) - 8, Blocks.SOUL_SAND);
        p.set(bx - 7, by + 1, bz - 5, Blocks.BARREL);
        p.clue(bx - 6, by + 1, bz - 5, ModBlocks.CLUE_NOTE.get(), "bell_letter");
        p.clue(bx + 7, by + 1, bz + 5, ModBlocks.CLUE_PHOTO.get(), "burned_photo");
        p.sign(bx + 1, by + 1, bz - 1, Direction.SOUTH, DyeColor.WHITE, "ITERATION 3", "FAMILY DRAMA", "(cancelled)", "");
    }

    // ---------------------------------------------------------------- iteration 2: the sunken plane
    private void sunkenPlane(Placer p) {
        int sx = wx(SUNKEN[0]), sz = wz(SUNKEN[1]);
        int floor = Math.min(56, ground(SUNKEN[0], SUNKEN[1]));
        BlockState fus = ModBlocks.FUSELAGE.get().defaultBlockState();
        BlockState stripe = ModBlocks.FUSELAGE_STRIPE.get().defaultBlockState();
        Random r = new Random(22);
        for (int z = sz - 10; z <= sz + 10; z++) {
            for (int dx = -3; dx <= 3; dx++)
                for (int ry = 0; ry <= 5; ry++) {
                    boolean shell = Math.abs(dx) == 3 || ry == 0 || ry == 5;
                    int y = floor + 1 + ry;
                    if (shell) {
                        if (r.nextFloat() < 0.12f) continue;
                        p.set(sx + dx, y, z, ry == 2 ? stripe : r.nextFloat() < 0.25f ? Blocks.TUBE_CORAL_BLOCK.defaultBlockState() : fus);
                    } else {
                        p.set(sx + dx, y, z, Blocks.WATER);
                    }
                }
            if (z % 2 == 0) for (int dx : new int[]{-2, -1, 1, 2}) p.set(sx + dx, floor + 2, z, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.NORTH));
        }
        // a broken-off wing and an engine on the lagoon floor
        for (int x = sx + 4; x <= sx + 16; x++) for (int z = sz - 2; z <= sz + 2 - (x - sx) / 6; z++) p.set(x, floor + 1, z, fus);
        p.fill(sx + 8, floor + 2, sz + 5, sx + 11, floor + 4, sz + 7, Blocks.LIGHT_GRAY_CONCRETE);
        p.set(sx + 8, floor + 3, sz + 6, Blocks.BLACK_CONCRETE);
        p.set(sx, floor + 2, sz + 8, ModBlocks.ENGINE_PART.get().defaultBlockState());
        p.clue(sx, floor + 2, sz - 6, ModBlocks.CLUE_NOTE.get(), "okafor_diary");
        p.set(sx, floor + 5, sz, Blocks.SEA_LANTERN);
        p.set(sx, floor + 5, sz - 8, Blocks.SEA_LANTERN);
    }

    // ---------------------------------------------------------------- iteration 1: Captain Hale's cave
    public static final int CAVE_Y = 64;

    private void haleCave(Placer p) {
        int[][] path = {{CAVE_MOUTH[0] - 4, CAVE_MOUTH[1]}, {CAVE_MOUTH[0] + 8, CAVE_MOUTH[1] + 1}, {CAVE_MOUTH[0] + 20, CAVE_MOUTH[1] - 3}, {CAVE_CAMP[0] - 4, CAVE_CAMP[1]}};
        for (int i = 0; i < path.length - 1; i++) {
            int[] a = path[i], b = path[i + 1];
            int steps = (int) Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1]));
            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                double x = lerp(a[0], b[0], t), z = lerp(a[1], b[1], t);
                carve(p, wx((int) x), CAVE_Y + 2, wz((int) z), 2.4);
            }
        }
        int cx = wx(CAVE_CAMP[0]), cz = wz(CAVE_CAMP[1]);
        for (int x = -7; x <= 7; x++)
            for (int y = -1; y <= 5; y++)
                for (int z = -6; z <= 6; z++) {
                    double d = x * x / 49.0 + (y - 1) * (y - 1) / 20.0 + z * z / 36.0;
                    if (d < 1) p.set(cx + x, CAVE_Y + 1 + y, cz + z, y == -1 ? Blocks.GRAVEL.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
        p.fill(cx - 7, CAVE_Y, cz - 6, cx + 7, CAVE_Y, cz + 6, Blocks.STONE);
        // the camp
        p.set(cx, CAVE_Y + 1, cz, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        p.set(cx - 3, CAVE_Y + 1, cz + 2, Blocks.WHITE_CARPET);
        p.set(cx - 3, CAVE_Y + 1, cz + 3, Blocks.WHITE_CARPET);
        p.set(cx + 3, CAVE_Y + 1, cz - 3, Blocks.BARREL);
        p.set(cx + 4, CAVE_Y + 1, cz - 3, Blocks.CARTOGRAPHY_TABLE);
        p.set(cx + 4, CAVE_Y + 1, cz + 2, facing(ModBlocks.AIRPLANE_SEAT.get(), Direction.WEST));
        p.set(cx - 5, CAVE_Y + 3, cz, Blocks.SOUL_LANTERN);
        p.set(cx + 5, CAVE_Y + 3, cz + 1, Blocks.SOUL_LANTERN);
        p.fill(cx - 6, CAVE_Y + 1, cz - 4, cx - 6, CAVE_Y + 3, cz - 2, Blocks.STONE);
        p.sign(cx - 5, CAVE_Y + 2, cz - 3, Direction.EAST, DyeColor.WHITE, "DAY 11", "IIIII IIIII I", "HALE, R.", "MA417");
        p.sign(cx - 5, CAVE_Y + 3, cz - 2, Direction.EAST, DyeColor.WHITE, "IT ISN'T", "A STORM", "IT'S A", "SCRIPT");
        p.clue(cx + 2, CAVE_Y + 1, cz + 3, ModBlocks.CLUE_TAPE.get(), "tape_1");
        p.clue(cx - 2, CAVE_Y + 1, cz - 4, ModBlocks.CLUE_TAPE.get(), "tape_3");
        p.frame(cx + 3, CAVE_Y + 2, cz - 3, Direction.SOUTH, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CLOCK), false);
    }

    private void carve(Placer p, int x, int y, int z, double r) {
        int ri = (int) Math.ceil(r);
        for (int ox = -ri; ox <= ri; ox++)
            for (int oy = -ri; oy <= ri; oy++)
                for (int oz = -ri; oz <= ri; oz++) {
                    if (ox * ox + oy * oy * 1.4 + oz * oz > r * r) continue;
                    int yy = y + oy;
                    if (yy <= CAVE_Y) continue;
                    p.set(x + ox, yy, z + oz, Blocks.AIR.defaultBlockState());
                }
    }

    public static Map<String, int[]> places() {
        return Map.of("crash", CRASH, "village", VILLAGE, "peak", PEAK, "grove", GROVE, "lookout", LOOKOUT, "burned", BURNED,
                "lagoon", LAGOON, "cave", CAVE_MOUTH, "falls", LOWER_POOL, "mangrove", MANGROVE);
    }
}
