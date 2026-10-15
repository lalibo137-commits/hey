package com.airlinestory.story;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixed places of the story world. The airport sits at the origin, the airborne copy of the plane far to
 * the north-east above the clouds, and the island to the south-east. Island heights are measured when it
 * is generated and stored in the story counters as "y:&lt;place&gt;".
 */
public final class Locations {
    private Locations() {}

    public record Loc(String id, String label, int x, int y, int z, double radius) {
        public BlockPos pos() { return new BlockPos(x, y, z); }
        public Vec3 center() { return new Vec3(x + 0.5, y, z + 0.5); }
    }

    // ----- Airport -----
    public static final int FLOOR = 63;
    public static final BlockPos SPAWN = new BlockPos(20, 64, -6);
    public static final BlockPos DANA = new BlockPos(16, 64, 32);
    public static final BlockPos EDITH_SEAT = new BlockPos(5, 64, 7);
    public static final BlockPos REYES = new BlockPos(53, 64, 21);
    public static final BlockPos TOMAS = new BlockPos(63, 64, 4);
    public static final int GATE_B5 = 88, GATE_B6 = 102, GATE_B7 = 116;
    public static final BlockPos CORINNE = new BlockPos(GATE_B7 + 2, 64, 44);
    public static final BlockPos SCANNER_WALK = new BlockPos(50, 64, 21);

    // ----- Planes -----
    public static final Plane GATE_PLANE = new Plane(107, 67, 66);
    public static final Plane AIR_PLANE = new Plane(1007, 210, -1000);

    /** Cabin layout shared by both copies of the aircraft. Nose at x, aisle on z, floor block at y. */
    public record Plane(int x, int y, int z) {
        public static final int ROWS = 20;
        public static final String LETTERS = "ABCDEF";

        public BlockPos seat(int row, char letter) {
            int dz = switch (letter) { case 'A' -> -3; case 'B' -> -2; case 'C' -> -1; case 'D' -> 1; case 'E' -> 2; default -> 3; };
            return new BlockPos(x + 12 + 2 * (row - 1), y + 1, z + dz);
        }

        public BlockPos local(int dx, int dy, int dz) {
            return new BlockPos(x + dx, y + dy, z + dz);
        }

        public BlockPos door() { return local(9, 1, -4); }
        public BlockPos aisle(int dx) { return local(dx, 1, 0); }
        public BlockPos galley() { return local(7, 1, 0); }
        public BlockPos rearGalley() { return local(56, 1, 0); }
        public BlockPos lavatory() { return local(53, 1, -2); }
        public BlockPos cockpitDoor() { return local(4, 1, 0); }
        public Vec3 at(double dx, double dy, double dz) { return new Vec3(x + dx, y + dy, z + dz); }
    }

    // ----- Island -----
    public static final int ISLAND_X = 3000, ISLAND_Z = 3000;

    public static BlockPos island(int dx, int y, int dz) {
        return new BlockPos(ISLAND_X + dx, y, ISLAND_Z + dz);
    }

    public static final int[] CRASH = {0, 78};
    public static final int[] LONE_SEAT = {-20, 89};
    public static final int[] POOL = {-18, -8};
    public static final int[] VILLAGE = {62, 5};
    public static final int[] DOCK = {104, 5};
    public static final int[] PEAK = {0, -45};
    public static final int[] GROVE = {-62, 10};

    public static boolean isFreshWater(BlockPos p) {
        int dx = p.getX() - (ISLAND_X + POOL[0]), dz = p.getZ() - (ISLAND_Z + POOL[1]);
        return dx * dx + dz * dz < 12 * 12;
    }

    public static boolean onIsland(BlockPos p) {
        int dx = p.getX() - ISLAND_X, dz = p.getZ() - ISLAND_Z;
        return dx * dx + dz * dz < 170 * 170;
    }

    private static final Map<String, Loc> ALL = new LinkedHashMap<>();

    private static void add(String id, String label, int x, int y, int z, double r) {
        ALL.put(id, new Loc(id, label, x, y, z, r));
    }

    static {
        add("terminal", "Departures Hall", 20, 64, 8, 5);
        add("checkin", "Meridian Check-in Desk", 16, 64, 28, 4);
        add("board", "Departures Board", 26, 64, 13, 4);
        add("benches", "Waiting Benches", 6, 64, 9, 4);
        add("security", "Security Checkpoint", 44, 64, 23, 5);
        add("scanner", "Body Scanner", SCANNER_WALK.getX(), 64, SCANNER_WALK.getZ(), 1.3);
        add("bag_pickup", "Bag Collection", 54, 64, 19, 3);
        add("cafe", "Cloud Nine Coffee", 63, 64, 9, 4);
        add("gate_b7", "Gate B7", GATE_B7, 64, 36, 7);
        add("jet_bridge", "Jet Bridge B7", GATE_B7, 68, 60, 3);
        add("plane_door", "Aircraft Door", GATE_PLANE.door().getX(), GATE_PLANE.y() + 1, GATE_PLANE.door().getZ() + 1, 2.5);
        add("seat_14a", "Seat 14A", GATE_PLANE.seat(14, 'A').getX(), GATE_PLANE.y() + 1, GATE_PLANE.seat(14, 'A').getZ(), 2);
        add("air_seat_14a", "Seat 14A", AIR_PLANE.seat(14, 'A').getX(), AIR_PLANE.y() + 1, AIR_PLANE.seat(14, 'A').getZ(), 2);
        add("lavatory", "Rear Lavatory", AIR_PLANE.lavatory().getX(), AIR_PLANE.y() + 1, AIR_PLANE.lavatory().getZ(), 2);
        add("row20", "Row 20", AIR_PLANE.seat(20, 'D').getX(), AIR_PLANE.y() + 1, AIR_PLANE.z(), 3);
        add("cockpit", "Cockpit Door", AIR_PLANE.cockpitDoor().getX() + 1, AIR_PLANE.y() + 1, AIR_PLANE.z(), 2);
        add("galley", "Forward Galley", AIR_PLANE.galley().getX(), AIR_PLANE.y() + 1, AIR_PLANE.z(), 3);
        addIsland("crash", "Crash Site", CRASH, 14);
        addIsland("lone_seat", "The Empty Seat", LONE_SEAT, 3);
        addIsland("pool", "Freshwater Spring", POOL, 8);
        addIsland("village", "Tidefolk Village", VILLAGE, 16);
        addIsland("dock", "Teo's Dock", DOCK, 6);
        addIsland("peak", "The Eye", new int[]{PEAK[0], PEAK[1] + 8}, 5);
        addIsland("lab", "Experience Lab 4", PEAK, 4);
        addIsland("grove", "The Hollow Grove", GROVE, 10);
    }

    private static void addIsland(String id, String label, int[] off, double r) {
        add(id, label, ISLAND_X + off[0], Integer.MIN_VALUE, ISLAND_Z + off[1], r);
    }

    @Nullable
    public static Loc get(String id) {
        return ALL.get(id);
    }

    /** Resolves an island place's height from the story counters (falls back to sea level). */
    public static int y(Loc loc, FlagView v) {
        if (loc.y() != Integer.MIN_VALUE) return loc.y();
        int y = v.counter("y:" + loc.id());
        return y == 0 ? 64 : y;
    }

    /** The place's block position, with island heights (and the dock's shoreline) filled in. */
    public static BlockPos resolve(Loc loc, FlagView v) {
        int x = v.counter("x:" + loc.id());
        int z = v.counter("z:" + loc.id());
        return new BlockPos(x != 0 ? x : loc.x(), y(loc, v), z != 0 ? z : loc.z());
    }

    public static boolean isNear(Loc loc, FlagView v, double px, double py, double pz) {
        BlockPos at = resolve(loc, v);
        double dx = px - (at.getX() + 0.5), dz = pz - (at.getZ() + 0.5);
        if (dx * dx + dz * dz > loc.radius() * loc.radius()) return false;
        return Math.abs(py - y(loc, v)) < (loc.y() == Integer.MIN_VALUE ? 16 : 3.5);
    }
}
