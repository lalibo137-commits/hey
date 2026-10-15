package com.airlinestory.world;

import com.airlinestory.story.Locations;

/**
 * The island's shape. Pure functions of (dx, dz) relative to the island centre, so any pass (terrain,
 * plants, structures, the client's cutscenes) can ask about any column without touching the world.
 */
public final class IslandTerrain {
    private IslandTerrain() {}

    public static final int SEA = 62;
    public static final int SEA_FLOOR = 52;

    // ----------------------------------------------------------------- noise
    static double hash(int x, int z, int seed) {
        long h = x * 374761393L + z * 668265263L + seed * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    static double smooth(double t) { return t * t * (3 - 2 * t); }
    static double clamp01(double t) { return t < 0 ? 0 : t > 1 ? 1 : t; }
    static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    static double vnoise(double x, double z, int seed) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = smooth(x - x0), fz = smooth(z - z0);
        double a = hash(x0, z0, seed), b = hash(x0 + 1, z0, seed), c = hash(x0, z0 + 1, seed), d = hash(x0 + 1, z0 + 1, seed);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fz);
    }

    public static double fbm(double x, double z) {
        return vnoise(x / 48.0, z / 48.0, 11) * 0.5 + vnoise(x / 22.0, z / 22.0, 23) * 0.3 + vnoise(x / 9.0, z / 9.0, 37) * 0.15
                + vnoise(x / 4.0, z / 4.0, 41) * 0.05;
    }

    static double ridged(double x, double z) {
        double n = vnoise(x / 30.0, z / 30.0, 53) * 2 - 1;
        return 1 - Math.abs(n);
    }

    static double dist(double dx, double dz, int[] p) {
        double a = dx - p[0], b = dz - p[1];
        return Math.sqrt(a * a + b * b);
    }

    static double segDist(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az;
        double t = clamp01(((px - ax) * vx + (pz - az) * vz) / (vx * vx + vz * vz));
        double cx = ax + vx * t - px, cz = az + vz * t - pz;
        return Math.sqrt(cx * cx + cz * cz);
    }

    // ----------------------------------------------------------------- places (offsets from the centre)
    public static final int[] PEAK = {10, -85};
    public static final int[] LOOKOUT = {-78, -28};
    public static final int[] UPPER_POOL = {-14, -68};
    public static final int[] LOWER_POOL = {-18, -46};
    public static final int[] LAGOON = {92, 102};
    public static final int[] VILLAGE = {112, 34};
    public static final int[] MANGROVE = {82, -70};
    public static final int[] BURNED = {92, -82};
    public static final int[] CAVE_MOUTH = {-162, -20};
    public static final int[] CAVE_CAMP = {-124, -24};
    public static final int[] GROVE = {-96, 86};
    public static final int[] CRASH = {14, 134};
    public static final int[] LONE_SEAT = {-12, 156};
    public static final int[] SUNKEN = {98, 108};

    /** River control points: x, z, water level, half-width. The first two are the pool above the falls. */
    public static final double[][] RIVER = {
            {-14, -76, 104, 5}, {-14, -60, 104, 3}, {-16, -55, 84, 3}, {-18, -46, 84, 6},
            {-30, -10, 78, 3}, {-46, 28, 72, 3.5}, {-54, 66, 67, 4}, {-48, 108, 64, 4}, {-52, 146, 62, 5}, {-56, 190, 61, 6}};

    // ----------------------------------------------------------------- shape
    static double coast(double dx, double dz) {
        double ang = Math.atan2(dz, dx);
        return 165 + 14 * Math.sin(3 * ang + 1.3) + 9 * Math.sin(5 * ang + 0.4) + 6 * Math.sin(9 * ang + 2.0) + 16 * (fbm(dx * 0.5 + 400, dz * 0.5) - 0.5);
    }

    public static double radial(int dx, int dz) {
        return Math.sqrt((double) dx * dx + (double) dz * dz) / coast(dx, dz);
    }

    /** Westward-facing sector: sea cliffs. */
    static double cliffFactor(double dx, double dz) {
        double ang = Math.atan2(dz, dx);
        double d = Math.abs(Math.atan2(Math.sin(ang - Math.PI), Math.cos(ang - Math.PI)));
        return clamp01(1 - d / 0.75);
    }

    private static double land(int dx, int dz, double r) {
        double n = fbm(dx, dz);
        double h = 62.6 + Math.pow(Math.max(0, 1 - r), 0.75) * 11 + (n - 0.5) * 14 * Math.min(1, (1 - r) * 2.5);
        // volcano
        // a broad cone with gullies, gentle enough to climb (about one block up per block across near the top)
        double md = dist(dx, dz, PEAK) / 100.0;
        if (md < 1) h += 64 * Math.pow(1 - md, 1.2) * (0.9 + 0.2 * ridged(dx, dz));
        // ridge to the lookout
        double rd = segDist(dx, dz, PEAK[0], PEAK[1], LOOKOUT[0], LOOKOUT[1]);
        if (rd < 22) h += 26 * Math.pow(1 - rd / 22, 1.6);
        // lookout knoll
        double ld = dist(dx, dz, LOOKOUT);
        if (ld < 16) h += 10 * smooth(1 - ld / 16);
        // western sea cliffs: a plateau that ends abruptly
        double cf = cliffFactor(dx, dz);
        if (cf > 0) {
            double up = smooth(clamp01((r - 0.55) / 0.3)) * (1 - smooth(clamp01((r - 0.93) / 0.05)));
            h += 20 * cf * up;
        }
        return h;
    }

    public static final double SUMMIT;

    static {
        SUMMIT = Math.floor(land(PEAK[0], PEAK[1], radial(PEAK[0], PEAK[1])));
    }

    private static double flatten(double h, double d, double inner, double outer, double target) {
        if (d >= outer) return h;
        double t = d <= inner ? 1 : smooth((outer - d) / (outer - inner));
        return lerp(h, target, t);
    }

    /** River info for a column: {distance to centre line, water level, half width}, or null if far away. */
    public static double[] river(double dx, double dz) {
        double best = 1e9, level = 0, width = 0;
        for (int i = 0; i < RIVER.length - 1; i++) {
            double[] a = RIVER[i], b = RIVER[i + 1];
            double vx = b[0] - a[0], vz = b[1] - a[1];
            double t = clamp01(((dx - a[0]) * vx + (dz - a[1]) * vz) / (vx * vx + vz * vz));
            double cx = a[0] + vx * t - dx, cz = a[1] + vz * t - dz;
            double d = Math.sqrt(cx * cx + cz * cz) + (vnoise(dx / 7.0, dz / 7.0, 61) - 0.5) * 1.6;
            if (d < best) {
                best = d;
                level = Math.floor(lerp(a[2], b[2], i == 1 ? (t < 0.6 ? 0 : 1) : t));
                width = lerp(a[3], b[3], t);
            }
        }
        return best < width + 6 ? new double[]{best, level, width} : null;
    }

    /** Height of the top solid block. Below sea level means sea floor. */
    public static int height(int dx, int dz) {
        double r = radial(dx, dz);
        double h;
        if (r >= 1) {
            double beyond = (r - 1) * coast(dx, dz);
            double cf = cliffFactor(dx, dz);
            h = Math.max(SEA_FLOOR, 61.5 - beyond * (0.32 + cf * 0.5));
        } else {
            h = land(dx, dz, r);
        }
        h = flatten(h, dist(dx, dz, PEAK), 13, 22, SUMMIT);
        h = flatten(h, dist(dx, dz, VILLAGE), 20, 32, 64.4);
        h = flatten(h, dist(dx, dz, CRASH), 16, 30, 63.4);
        h = flatten(h, dist(dx, dz, GROVE), 14, 24, 66.2);
        h = flatten(h, dist(dx, dz, BURNED), 12, 20, 63.6);
        h = flatten(h, dist(dx, dz, MANGROVE), 22, 34, 62.8);
        // lagoon basin and its reef
        double lg = dist(dx, dz, LAGOON);
        if (lg < 46) {
            double ang = Math.atan2(dz - LAGOON[1], dx - LAGOON[0]);
            boolean gap = Math.abs(ang - 0.35) < 0.22 || Math.abs(ang - 1.9) < 0.12;
            if (lg < 32) h = Math.min(h, lerp(54, 61, smooth(lg / 32)) + (fbm(dx * 3, dz * 3) - 0.5) * 2);
            else if (lg < 38 && !gap && r > 0.55) h = 62 + (hash(dx, dz, 7) < 0.25 ? 1 : 0);
            else if (lg < 38) h = Math.min(h, 59);
        }
        // the river and its waterfall
        double[] rv = river(dx, dz);
        if (rv != null) {
            double d = rv[0], level = rv[1], w = rv[2];
            if (d < w) h = level - 2 - (w - d > 2 ? 1 : 0);
            else h = Math.max(h, level + 1 + (d - w) * 0.6);
        }
        return (int) Math.floor(h);
    }

    /** Water surface for river/pool columns (above sea level), or -1. */
    public static int riverWater(int dx, int dz) {
        double[] rv = river(dx, dz);
        if (rv == null || rv[0] >= rv[2]) return -1;
        return (int) rv[1];
    }

    public static boolean isLand(int dx, int dz) {
        return height(dx, dz) > SEA && riverWater(dx, dz) < 0;
    }

    public static boolean beach(int dx, int dz, int h) {
        return h <= 64 || radial(dx, dz) > 0.92 && cliffFactor(dx, dz) < 0.3;
    }

    public static boolean isPool(int dx, int dz) {
        return riverWater(dx, dz) >= 0;
    }

    public static int poolSurface() {
        return (int) RIVER[3][2];
    }

    /** Region names for the journal map and for plants. */
    public static String region(int dx, int dz) {
        if (dist(dx, dz, MANGROVE) < 34) return "mangrove";
        if (dist(dx, dz, GROVE) < 24) return "grove";
        if (dist(dx, dz, LAGOON) < 38) return "lagoon";
        if (dist(dx, dz, PEAK) < 60) return "mountain";
        if (cliffFactor(dx, dz) > 0.5 && radial(dx, dz) > 0.6) return "cliffs";
        return "jungle";
    }

}
