package com.airlinestory.story;

import com.airlinestory.world.IslandTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixed places of the story world. The airport sits at the origin (landside hall in the west, gates in the
 * east, the kerb to the north), the airborne copy of the plane far away above the clouds, and the island
 * to the south-east. Island heights are measured when it is generated and stored as "y:&lt;place&gt;".
 */
public final class Locations {
    private Locations() {}

    public record Loc(String id, String label, int x, int y, int z, double radius) {
        public BlockPos pos() { return new BlockPos(x, y, z); }
        public Vec3 center() { return new Vec3(x + 0.5, y, z + 0.5); }
    }

    // ----- Airport -----
    public static final int FLOOR = 63;
    public static final BlockPos SPAWN = new BlockPos(26, 64, -8);
    public static final BlockPos DANA = new BlockPos(26, 64, 29);
    public static final BlockPos EDITH_SEAT = new BlockPos(5, 64, 41);
    public static final BlockPos PRIYA_SEAT = new BlockPos(9, 64, 41);
    public static final BlockPos LEO_SEAT = new BlockPos(10, 64, 41);
    public static final BlockPos GUS = new BlockPos(42, 64, 44);
    public static final BlockPos REYES = new BlockPos(60, 64, 25);
    public static final BlockPos REYES_ROOM = new BlockPos(70, 64, 50);
    public static final BlockPos INTERVIEW_CHAIR = new BlockPos(68, 64, 47);
    public static final BlockPos SCANNER_WALK = new BlockPos(67, 64, 26);
    public static final BlockPos TOMAS = new BlockPos(84, 64, 5);
    public static final BlockPos LOUNGE_TV = new BlockPos(87, 66, 54);
    public static final BlockPos POSTER = new BlockPos(100, 65, 39);
    public static final BlockPos STAFF_DOOR = new BlockPos(106, 64, 0);
    public static final BlockPos STAFF_KEYPAD = new BlockPos(107, 65, 1);
    public static final int GATE_B5 = 116, GATE_B6 = 132, GATE_B7 = 148;
    public static final BlockPos CORINNE = new BlockPos(GATE_B7 + 2, 64, 53);
    public static final BlockPos B7_PODIUM = new BlockPos(GATE_B7, 64, 52);

    // ----- Planes -----
    public static final Plane GATE_PLANE = new Plane(139, 69, 75);
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
        public BlockPos hold() { return local(40, -3, 0); }
        public Vec3 at(double dx, double dy, double dz) { return new Vec3(x + dx, y + dy, z + dz); }
    }

    // ----- Island -----
    public static final int ISLAND_X = 3000, ISLAND_Z = 3000;

    public static BlockPos island(int dx, int y, int dz) {
        return new BlockPos(ISLAND_X + dx, y, ISLAND_Z + dz);
    }

    /** Fresh water is everything above sea level on the island: the river, its pools and the falls. */
    public static boolean isFreshWater(BlockPos p) {
        return onIsland(p) && p.getY() > IslandTerrain.SEA;
    }

    public static boolean onIsland(BlockPos p) {
        int dx = p.getX() - ISLAND_X, dz = p.getZ() - ISLAND_Z;
        return dx * dx + dz * dz < 200 * 200;
    }

    private static final Map<String, Loc> ALL = new LinkedHashMap<>();

    private static void add(String id, String label, int x, int y, int z, double r) {
        ALL.put(id, new Loc(id, label, x, y, z, r));
    }

    private static void add(String id, String label, BlockPos p, double r) {
        add(id, label, p.getX(), p.getY(), p.getZ(), r);
    }

    static {
        // landside
        add("terminal", "Departures Hall", 26, 64, 8, 6);
        add("checkin", "Meridian Check-in", 26, 64, 25, 4);
        add("board", "Departures Board", 26, 64, 13, 4);
        add("benches", "West Benches", 7, 64, 43, 5);
        add("closet", "Janitor's Closet", 47, 64, 51, 3);
        // security
        add("security", "Security Checkpoint", 58, 64, 25, 5);
        add("scanner", "Body Scanner", SCANNER_WALK, 1.3);
        add("bag_pickup", "Bag Collection", 73, 64, 25, 2.5);
        add("interview", "Secondary Screening", 68, 64, 46, 3);
        // airside
        add("cafe", "Cloud Nine Coffee", 84, 64, 10, 4);
        add("lounge", "Meridian Prime Lounge", 88, 64, 48, 5);
        add("poster", "Notice Pillar", 100, 64, 38, 2.5);
        add("staff_door", "Staff Door", 106, 64, 2, 2.5);
        add("backstage", "Backstage", 106, 64, -6, 5);
        add("office", "Production Office", 104, 64, -29, 3);
        add("control", "Control Room", 130, 64, -27, 5);
        add("gate_b7", "Gate B7", GATE_B7, 64, 42, 7);
        add("podium", "B7 Podium", B7_PODIUM, 2.5);
        add("jet_bridge", "Jet Bridge B7", GATE_B7, 70, 62, 3);
        // the plane at the gate
        add("plane_door", "Aircraft Door", GATE_PLANE.door().getX(), GATE_PLANE.y() + 1, GATE_PLANE.door().getZ() + 1, 2.5);
        add("seat_14a", "Seat 14A", GATE_PLANE.seat(14, 'A'), 2);
        // in the air
        add("air_seat_14a", "Seat 14A", AIR_PLANE.seat(14, 'A'), 2);
        add("lavatory", "Rear Lavatory", AIR_PLANE.lavatory(), 2);
        add("row20", "Row 20", AIR_PLANE.seat(20, 'D').getX(), AIR_PLANE.y() + 1, AIR_PLANE.z(), 3);
        add("cockpit", "Cockpit Door", AIR_PLANE.cockpitDoor().getX() + 1, AIR_PLANE.y() + 1, AIR_PLANE.z(), 2);
        add("galley", "Forward Galley", AIR_PLANE.galley().getX(), AIR_PLANE.y() + 1, AIR_PLANE.z(), 3);
        add("rear_galley", "Rear Galley", AIR_PLANE.rearGalley(), 2.5);
        add("hold", "Cargo Hold", AIR_PLANE.hold(), 14);
        // the island
        addIsland("crash", "Crash Site", IslandTerrain.CRASH, 16);
        addIsland("lone_seat", "The Empty Seat", IslandTerrain.LONE_SEAT, 3);
        addIsland("falls", "The Falls", IslandTerrain.LOWER_POOL, 9);
        addIsland("lookout", "Radio Tower", IslandTerrain.LOOKOUT, 5);
        addIsland("village", "Tidefolk Village", IslandTerrain.VILLAGE, 16);
        addIsland("dock", "Teo's Dock", new int[]{IslandTerrain.VILLAGE[0] + 30, IslandTerrain.VILLAGE[1]}, 7);
        addIsland("cave", "Captain's Cave", IslandTerrain.CAVE_MOUTH, 6);
        addIsland("cave_camp", "The Captain's Camp", IslandTerrain.CAVE_CAMP, 7);
        addIsland("sunken", "Sunken Plane", IslandTerrain.SUNKEN, 14);
        addIsland("burned", "Burned Village", IslandTerrain.BURNED, 10);
        addIsland("grove", "The Hollow Grove", IslandTerrain.GROVE, 10);
        addIsland("peak", "The Eye", new int[]{IslandTerrain.PEAK[0], IslandTerrain.PEAK[1] + 10}, 5);
        addIsland("lab", "Experience Lab 4", IslandTerrain.PEAK, 5);
    }

    private static void addIsland(String id, String label, int[] off, double r) {
        add(id, label, ISLAND_X + off[0], Integer.MIN_VALUE, ISLAND_Z + off[1], r);
    }

    @Nullable
    public static Loc get(String id) {
        return ALL.get(id);
    }

    /** Resolves an island place's height from the story counters (falls back to the terrain). */
    public static int y(Loc loc, FlagView v) {
        if (loc.y() != Integer.MIN_VALUE) return loc.y();
        int y = v.counter("y:" + loc.id());
        if (y != 0) return y;
        return Math.max(IslandTerrain.SEA, IslandTerrain.height(loc.x() - ISLAND_X, loc.z() - ISLAND_Z)) + 1;
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
        return Math.abs(py - y(loc, v)) < (loc.y() == Integer.MIN_VALUE ? 16 : loc.id().equals("hold") ? 2.5 : 3.5);
    }
}
