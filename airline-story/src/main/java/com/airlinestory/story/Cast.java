package com.airlinestory.story;

import com.airlinestory.entity.ModEntities;
import com.airlinestory.entity.StoryNpc;
import com.airlinestory.story.Locations.Plane;
import com.airlinestory.world.IslandBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.*;

/** Who is where, chapter by chapter. */
public final class Cast {
    private Cast() {}

    static final String[] CROWD_NAMES = {"Traveler", "Passenger", "Commuter", "Tourist", "Businesswoman", "Backpacker",
            "Student", "Grandfather", "Honeymooner", "Consultant", "Pilot-in-training", "Influencer"};

    /** People waiting at B7 who board Flight 417, with their seats. */
    record Passenger(String id, String name, String skin, boolean slim, boolean child, String dialogue, String seat) {}

    static final List<Passenger> B7 = List.of(
            new Passenger("marcus", "Marcus", "marcus", false, false, "marcus", "14C"),
            new Passenger("priya", "Priya", "priya", true, false, "priya", "12D"),
            new Passenger("leo", "Leo", "leo", false, true, "leo", "12E"),
            new Passenger("halvorsen", "Mr. Halvorsen", "halvorsen", false, false, "halvorsen", "3A"),
            new Passenger("ines", "Ines", "ines", true, false, "ines", "14B"));

    static final String[] B7_CROWD_SEATS = {"2C", "4D", "5A", "6F", "7B", "8E", "9C", "10A", "11F", "13D", "15B", "16E", "17A", "18C", "19F", "6D"};
    static final String[] AIR_EXTRA_SEATS = {"1A", "1F", "2D", "3E", "5C", "7E", "9A", "11B", "15F", "17D", "19A", "20C"};

    public static BlockPos seat(Plane plane, String code) {
        int row = Integer.parseInt(code.substring(0, code.length() - 1));
        return plane.seat(row, code.charAt(code.length() - 1));
    }

    private static StoryNpc make(StoryDirector d, ServerLevel level, String id, String group, String name, String skin, boolean slim, boolean child,
                                 String dialogue, BlockPos at) {
        StoryNpc npc = ModEntities.NPC.get().create(level);
        if (npc == null) return null;
        npc.setup(id, group, name, skin, slim, child, dialogue);
        npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        level.getChunkAt(at);
        level.addFreshEntity(npc);
        d.register(npc);
        return npc;
    }

    private static String crowdSkin(Random r) {
        return "traveler_" + r.nextInt(10);
    }

    private static boolean slimSkin(String skin) {
        if (!skin.startsWith("traveler_")) return false;
        int i = Integer.parseInt(skin.substring(9));
        return i % 2 == 1;
    }

    public static void spawn(StoryDirector d, ServerLevel level, String group) {
        Random r = new Random(group.hashCode());
        switch (group) {
            case "airport" -> airport(d, level, r);
            case "edith_airport" -> make(d, level, "edith", group, "Edith", "edith", true, false, "edith", Locations.EDITH_SEAT).sitAt(Locations.EDITH_SEAT);
            case "b7" -> gateB7(d, level, r);
            case "gate_crew" -> {
                Plane g = Locations.GATE_PLANE;
                make(d, level, "june", group, "June", "june", true, false, "june", g.galley()).stand(g.local(10, 1, -2), 0f);
                make(d, level, "theo", group, "Theo", "attendant", false, false, "attendant", g.rearGalley()).stand(g.rearGalley(), 90f);
            }
            case "air_plane" -> airPlane(d, level, r);
            case "tribe" -> tribe(d, level, r);
            case "ending_airport" -> make(d, level, "dana", group, "Dana", "dana", true, false, "dana", Locations.DANA).stand(Locations.DANA, 180f);
            default -> {}
        }
    }

    private static void airport(StoryDirector d, ServerLevel level, Random r) {
        String g = "airport";
        make(d, level, "dana", g, "Dana", "dana", true, false, "dana", Locations.DANA).stand(Locations.DANA, 180f);
        make(d, level, "agent_1", g, "Check-in Agent", "agent", false, false, "agent", new BlockPos(8, 64, 32)).stand(new BlockPos(8, 64, 32), 180f);
        make(d, level, "agent_2", g, "Check-in Agent", "agent", true, false, "agent", new BlockPos(31, 64, 32)).stand(new BlockPos(31, 64, 32), 180f);
        make(d, level, "reyes", g, "Officer Reyes", "reyes", false, false, "reyes", Locations.REYES).stand(Locations.REYES, 90f);
        make(d, level, "officer", g, "Security Officer", "officer", true, false, "officer", new BlockPos(53, 64, 27)).stand(new BlockPos(53, 64, 27), 90f);
        make(d, level, "tomas", g, "Tomas", "tomas", false, false, "tomas", Locations.TOMAS).stand(Locations.TOMAS, 0f);
        make(d, level, "clerk", g, "Shop Clerk", "clerk", true, false, "clerk", new BlockPos(75, 64, 10)).stand(new BlockPos(75, 64, 10), 0f);
        make(d, level, "corinne", g, "Corinne", "corinne", true, false, "corinne", Locations.CORINNE).stand(Locations.CORINNE, 180f);
        make(d, level, "gate_b5", g, "Gate Agent", "agent", false, false, "staff", new BlockPos(Locations.GATE_B5 + 2, 64, 44)).stand(new BlockPos(Locations.GATE_B5 + 2, 64, 44), 180f);
        make(d, level, "gate_b6", g, "Gate Agent", "agent", true, false, "staff", new BlockPos(Locations.GATE_B6 + 2, 64, 44)).stand(new BlockPos(Locations.GATE_B6 + 2, 64, 44), 180f);
        make(d, level, "janitor", g, "Janitor", "janitor", false, false, "janitor", new BlockPos(100, 64, 25)).wander(new BlockPos(100, 64, 25), 14);
        int n = 0;
        // check-in queue
        for (int i = 0; i < 3; i++) {
            BlockPos at = new BlockPos(14 + i * 2, 64, 24 + (i % 2));
            crowd(d, level, r, g, n++, at).stand(at, 180f);
        }
        // landside wanderers
        for (int i = 0; i < 5; i++) crowd(d, level, r, g, n++, new BlockPos(18, 64, 16)).wander(new BlockPos(18 + i, 64, 16), 10);
        // security queue
        for (int i = 0; i < 4; i++) {
            BlockPos at = new BlockPos(42 + (i % 2) * 2, 64, 23 + i);
            crowd(d, level, r, g, n++, at).stand(at, -90f);
        }
        // airside wanderers and cafe
        for (int i = 0; i < 5; i++) crowd(d, level, r, g, n++, new BlockPos(72, 64, 22)).wander(new BlockPos(70 + i * 3, 64, 24), 9);
        BlockPos c1 = new BlockPos(60, 64, 11), c2 = new BlockPos(66, 64, 15);
        crowd(d, level, r, g, n++, c1).sitAt(c1);
        crowd(d, level, r, g, n++, c2).sitAt(c2);
        // other gates
        for (int gx : new int[]{Locations.GATE_B5, Locations.GATE_B6}) {
            for (int i = 0; i < 6; i++) {
                BlockPos s = new BlockPos(gx + (i % 2 == 0 ? -1 : 1) * (1 + r.nextInt(5)), 64, new int[]{28, 31, 34, 37}[r.nextInt(4)]);
                crowd(d, level, r, g, n++, s).sitAt(s);
            }
        }
    }

    private static StoryNpc crowd(StoryDirector d, ServerLevel level, Random r, String group, int n, BlockPos at) {
        String skin = crowdSkin(r);
        String name = CROWD_NAMES[r.nextInt(CROWD_NAMES.length)];
        return make(d, level, group + "_" + n, group, name, skin, slimSkin(skin), false, "traveler_" + r.nextInt(6), at);
    }

    /** Seats at the B7 waiting area, in a fixed order. */
    static List<BlockPos> gateSeats() {
        List<BlockPos> out = new ArrayList<>();
        int gx = Locations.GATE_B7;
        for (int z : new int[]{31, 34, 28, 37})
            for (int dx = 1; dx <= 5; dx++) {
                out.add(new BlockPos(gx - dx, 64, z));
                out.add(new BlockPos(gx + dx, 64, z));
            }
        return out;
    }

    /** Plane seat for each B7 npc id (used by the boarding procession). */
    static final Map<String, String> SEATS = new HashMap<>();

    private static void gateB7(StoryDirector d, ServerLevel level, Random r) {
        String g = "b7";
        List<BlockPos> seats = gateSeats();
        int si = 0;
        for (Passenger p : B7) {
            SEATS.put(p.id(), p.seat());
            BlockPos at;
            StoryNpc npc;
            switch (p.id()) {
                case "marcus" -> { at = new BlockPos(Locations.GATE_B7 + 4, 64, 41); npc = make(d, level, p.id(), g, p.name(), p.skin(), p.slim(), p.child(), p.dialogue(), at); npc.wander(at, 3); }
                case "halvorsen" -> { at = new BlockPos(Locations.GATE_B7 - 3, 64, 42); npc = make(d, level, p.id(), g, p.name(), p.skin(), p.slim(), p.child(), p.dialogue(), at); npc.stand(at, 0f); }
                default -> { at = seats.get(si++); npc = make(d, level, p.id(), g, p.name(), p.skin(), p.slim(), p.child(), p.dialogue(), at); npc.sitAt(at); }
            }
        }
        for (int i = 0; i < B7_CROWD_SEATS.length; i++) {
            String id = "b7_" + i;
            SEATS.put(id, B7_CROWD_SEATS[i]);
            BlockPos at = seats.get(si++ % seats.size());
            String skin = crowdSkin(r);
            StoryNpc npc = make(d, level, id, g, CROWD_NAMES[r.nextInt(CROWD_NAMES.length)], skin, slimSkin(skin), false, "traveler_" + r.nextInt(6), at);
            if (npc != null) npc.sitAt(at);
        }
    }

    @Nullable
    public static String b7Seat(String id) {
        if (SEATS.isEmpty()) {
            for (Passenger p : B7) SEATS.put(p.id(), p.seat());
            for (int i = 0; i < B7_CROWD_SEATS.length; i++) SEATS.put("b7_" + i, B7_CROWD_SEATS[i]);
        }
        return SEATS.get(id);
    }

    /** Walk from the B7 podium, through the jet bridge, down the aisle to the seat. */
    public static List<BlockPos> boardingRoute(String seatCode) {
        Plane g = Locations.GATE_PLANE;
        BlockPos seat = seat(g, seatCode);
        List<BlockPos> route = new ArrayList<>();
        route.add(new BlockPos(Locations.GATE_B7, 64, 45));
        route.add(teleportPoint(new BlockPos(Locations.GATE_B7, 64, 49)));
        route.add(new BlockPos(Locations.GATE_B7, 68, 59));
        route.add(teleportPoint(g.local(9, 1, -3)));
        route.add(g.local(9, 1, 0));
        route.add(new BlockPos(seat.getX(), g.y() + 1, g.z()));
        return route;
    }

    /** Waypoints above y=1000 tell the NPC to step through (doors, scanners) instead of pathing. */
    public static BlockPos teleportPoint(BlockPos p) {
        return p.above(1000);
    }

    private static void airPlane(StoryDirector d, ServerLevel level, Random r) {
        String g = "air_plane";
        Plane a = Locations.AIR_PLANE;
        for (Passenger p : B7) {
            BlockPos s = seat(a, p.seat());
            make(d, level, p.id(), g, p.name(), p.skin(), p.slim(), p.child(), p.dialogue(), s).sitAt(s);
        }
        BlockPos e = seat(a, "20F");
        make(d, level, "edith", g, "Edith", "edith", true, false, "edith", e).sitAt(e);
        int n = 0;
        for (String code : B7_CROWD_SEATS) {
            BlockPos s = seat(a, code);
            String skin = crowdSkin(r);
            make(d, level, "air_" + n, g, "Passenger", skin, slimSkin(skin), false, n % 2 == 0 ? "passenger" : "passenger_b", s).sitAt(s);
            n++;
        }
        for (String code : AIR_EXTRA_SEATS) {
            BlockPos s = seat(a, code);
            String skin = crowdSkin(r);
            make(d, level, "air_" + n, g, "Passenger", skin, slimSkin(skin), false, n % 2 == 0 ? "passenger" : "passenger_b", s).sitAt(s);
            n++;
        }
        make(d, level, "june", g, "June", "june", true, false, "june", a.galley()).stand(a.galley(), 90f);
        make(d, level, "theo", g, "Theo", "attendant", false, false, "attendant", a.rearGalley()).stand(a.rearGalley(), 90f);
    }

    private static void tribe(StoryDirector d, ServerLevel level, Random r) {
        String g = "tribe";
        make(d, level, "ahu", g, "Elder Ahu", "ahu", false, false, "ahu", IslandBuilder.village(0, -10)).stand(IslandBuilder.village(0, -10), 0f);
        make(d, level, "nalu", g, "Nalu", "nalu", false, false, "nalu", IslandBuilder.village(-17, 7)).stand(IslandBuilder.village(-17, 7), 90f);
        make(d, level, "mira", g, "Mira", "mira", true, false, "mira", IslandBuilder.village(-9, 6)).stand(IslandBuilder.village(-9, 6), -90f);
        make(d, level, "teo", g, "Teo", "teo", false, false, "teo", IslandBuilder.dockStart()).stand(IslandBuilder.dockStart(), -90f);
        make(d, level, "pip", g, "Pip", "pip", true, true, "pip", IslandBuilder.village(3, 3)).wander(IslandBuilder.village(3, 3), 5);
        for (int i = 0; i < 4; i++) {
            BlockPos at = IslandBuilder.village(r.nextInt(13) - 6, r.nextInt(13) - 6);
            make(d, level, "tribe_" + i, g, "Tidefolk", "tribe_" + i, i % 2 == 1, false, "tribe_" + (i % 3), at).wander(at, 9);
        }
    }
}
