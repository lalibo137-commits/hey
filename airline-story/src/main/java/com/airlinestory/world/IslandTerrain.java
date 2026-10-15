package com.airlinestory.world;

import com.airlinestory.story.Locations;

/** The island's height field. Deterministic, so every pass can ask for any column's height. */
public final class IslandTerrain {
    private IslandTerrain() {}

    public static final int SEA = 62;
    public static final int SEA_FLOOR = 52;

    static double hash(int x, int z, int seed) {
        long h = x * 374761393L + z * 668265263L + seed * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    static double vnoise(double x, double z, int seed) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = smooth(x - x0), fz = smooth(z - z0);
        double a = hash(x0, z0, seed), b = hash(x0 + 1, z0, seed), c = hash(x0, z0 + 1, seed), d = hash(x0 + 1, z0 + 1, seed);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fz);
    }

    static double fbm(double x, double z) {
        return vnoise(x / 34.0, z / 34.0, 11) * 0.55 + vnoise(x / 15.0, z / 15.0, 23) * 0.3 + vnoise(x / 6.0, z / 6.0, 37) * 0.15;
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    static double dist(int dx, int dz, int[] p) {
        double a = dx - p[0], b = dz - p[1];
        return Math.sqrt(a * a + b * b);
    }

    /** Radius of the coastline at this angle. */
    static double coast(int dx, int dz) {
        double ang = Math.atan2(dz, dx);
        return 100 + 11 * Math.sin(3 * ang + 1.3) + 7 * Math.sin(5 * ang + 0.4) + 10 * (fbm(dx * 0.6 + 400, dz * 0.6) - 0.5);
    }

    /** 0 at the centre, 1 at the coast. */
    public static double radial(int dx, int dz) {
        return Math.sqrt((double) dx * dx + (double) dz * dz) / coast(dx, dz);
    }

    private static double rawLand(int dx, int dz, double r) {
        double n = fbm(dx, dz);
        double h = 62.4 + Math.pow(Math.max(0, 1 - r), 0.8) * 8 + (n - 0.5) * 7 * (1 - r);
        double md = dist(dx, dz, Locations.PEAK) / 54.0;
        if (md < 1) h += 40 * Math.pow(1 - md, 1.25) * (0.88 + 0.24 * n);
        return h;
    }

    public static final double SUMMIT = Math.floor(rawLand(Locations.PEAK[0], Locations.PEAK[1], radial(Locations.PEAK[0], Locations.PEAK[1])));

    /** Terrain top block height (the solid block), or the sea floor if under water. */
    public static int height(int dx, int dz) {
        double r = radial(dx, dz);
        if (r >= 1) {
            double beyond = (r - 1) * coast(dx, dz);
            return (int) Math.max(SEA_FLOOR, Math.floor(61.5 - beyond * 0.42));
        }
        double h = rawLand(dx, dz, r);
        h = flatten(h, dist(dx, dz, Locations.PEAK), 9, 13, SUMMIT);
        h = flatten(h, dist(dx, dz, Locations.VILLAGE), 15, 26, 64.5);
        h = flatten(h, dist(dx, dz, Locations.GROVE), 13, 22, 65.2);
        h = flatten(h, dist(dx, dz, Locations.CRASH), 10, 22, 63.2);
        return (int) Math.floor(h);
    }

    private static double flatten(double h, double d, double inner, double outer, double target) {
        if (d >= outer) return h;
        double t = d <= inner ? 1 : smooth((outer - d) / (outer - inner));
        return lerp(h, target, t);
    }

    public static boolean isPool(int dx, int dz) {
        return dist(dx, dz, Locations.POOL) < 6.5;
    }

    public static boolean isLand(int dx, int dz) {
        return height(dx, dz) > SEA;
    }

    public static int poolSurface() {
        return height(Locations.POOL[0] + 8, Locations.POOL[1]);
    }

    public static boolean beach(int dx, int dz, int h) {
        return h <= 64 || radial(dx, dz) > 0.9;
    }
}
