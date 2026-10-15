package com.airlinestory.story;

import com.airlinestory.entity.ModEntities;
import com.airlinestory.entity.StoryNpc;
import com.airlinestory.entity.StoryNpc.Activity;
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
            "Student", "Grandfather", "Honeymooner", "Consultant", "Pilot-in-training", "Influencer", "Retiree", "Nurse"};

    /** Plane seats of everyone who boards Flight 417 from gate B7. */
    static final Map<String, String> SEATS = new LinkedHashMap<>();
    static final String[] B7_CROWD_SEATS = {"2C", "4D", "5A", "6F", "7B", "8E", "9C", "10A", "11F", "13D", "15B", "16E", "17A", "18C", "19F", "6D"};
    static final String[] AIR_EXTRA_SEATS = {"1A", "1F", "2D", "3E", "5C", "7E", "9A", "11B", "15F", "17D", "19A", "20C"};

    static {
        SEATS.put("marcus", "14C");
        SEATS.put("ines", "14B");
        SEATS.put("priya", "12D");
        SEATS.put("leo", "12E");
        SEATS.put("halvorsen", "3A");
        for (int i = 0; i < B7_CROWD_SEATS.length; i++) SEATS.put("b7_" + i, B7_CROWD_SEATS[i]);
    }

    public static BlockPos seat(Plane plane, String code) {
        int row = Integer.parseInt(code.substring(0, code.length() - 1));
        return plane.seat(row, code.charAt(code.length() - 1));
    }

    @Nullable
    public static String b7Seat(String id) {
        return SEATS.get(id);
    }

    private static BlockPos b(int x, int z) {
        return new BlockPos(x, 64, z);
    }

    private static StoryNpc make(StoryDirector d, ServerLevel level, String id, String group, String name, String skin, boolean slim, boolean child,
                                 String dialogue, BlockPos at) {
        StoryNpc npc = ModEntities.NPC.get().create(level);
        if (npc == null) throw new IllegalStateException("could not create npc");
        npc.setup(id, group, name, skin, slim, child, dialogue);
        npc.barks(id);
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
        return Integer.parseInt(skin.substring(9)) % 2 == 1;
    }

    private static StoryNpc crowd(StoryDirector d, ServerLevel level, Random r, String group, int n, BlockPos at) {
        String skin = crowdSkin(r);
        String name = CROWD_NAMES[r.nextInt(CROWD_NAMES.length)];
        StoryNpc npc = make(d, level, group + "_" + n, group, name, skin, slimSkin(skin), false, "traveler_" + r.nextInt(8), at);
        npc.barks("crowd").score(2.6f + r.nextInt(23) / 10f);
        Activity[] acts = {Activity.PHONE, Activity.PHONE, Activity.IDLE, Activity.COFFEE, Activity.READING, Activity.IDLE};
        npc.activity(acts[r.nextInt(acts.length)]);
        return npc;
    }

    public static void spawn(StoryDirector d, ServerLevel level, String group) {
        Random r = new Random(group.hashCode());
        switch (group) {
            case "landside" -> landside(d, level, r);
            case "security" -> security(d, level, r);
            case "airside" -> airside(d, level, r);
            case "backstage" -> backstage(d, level);
            case "b7" -> gateB7(d, level, r);
            case "gate_crew" -> {
                Plane g = Locations.GATE_PLANE;
                make(d, level, "june", group, "June", "june", true, false, "june", g.local(10, 1, -2)).score(4.97f).stand(g.local(10, 1, -2), -90f);
                make(d, level, "theo", group, "Theo", "attendant", false, false, "theo", g.rearGalley()).score(4.8f).stand(g.rearGalley(), 90f);
            }
            case "air_plane" -> airPlane(d, level, r);
            case "tribe" -> tribe(d, level, r);
            case "ending_airport" -> make(d, level, "dana", group, "Dana", "dana", true, false, "dana", Locations.DANA).score(4.99f).stand(Locations.DANA, 180f);
            default -> {}
        }
    }

    // =========================================================================================
    // Act I: Meridian International
    // =========================================================================================
    static final List<BlockPos> LANDSIDE_SPOTS = List.of(b(24, 13), b(29, 13), b(23, 19), b(30, 19), b(26, 10), b(9, 25), b(44, 25),
            b(39, 8), b(41, 9), b(12, 4), b(36, 4), b(16, 35), b(36, 36), b(3, 36), b(22, 52), b(30, 52), b(48, 40));

    private static List<BlockPos> some(Random r, List<BlockPos> from, int n) {
        List<BlockPos> l = new ArrayList<>(from);
        Collections.shuffle(l, r);
        return l.subList(0, Math.min(n, l.size()));
    }

    private static void landside(StoryDirector d, ServerLevel level, Random r) {
        String g = "landside";
        make(d, level, "dana", g, "Dana", "dana", true, false, "dana", Locations.DANA).score(4.97f).activity(Activity.WORKING).stand(Locations.DANA, 180f);
        make(d, level, "agent_1", g, "Solace Agent", "agent", false, false, "agent_solace", b(9, 29)).score(4.1f).activity(Activity.WORKING).barks("staff").stand(b(9, 29), 180f);
        make(d, level, "agent_2", g, "Harrow Agent", "agent", true, false, "agent_harrow", b(44, 29)).score(3.9f).activity(Activity.WORKING).barks("staff").stand(b(44, 29), 180f);
        make(d, level, "info", g, "Information Desk", "clerk", true, false, "info_desk", b(26, 7)).score(4.6f).barks("staff").stand(b(26, 7), 0f);
        make(d, level, "edith", g, "Edith", "edith", true, false, "edith", Locations.EDITH_SEAT).score(-1).activity(Activity.KNITTING).sitAt(Locations.EDITH_SEAT);
        make(d, level, "priya", g, "Priya", "priya", true, false, "priya", Locations.PRIYA_SEAT).score(3.4f).activity(Activity.PHONE).sitAt(Locations.PRIYA_SEAT);
        make(d, level, "leo", g, "Leo", "leo", false, true, "leo", Locations.LEO_SEAT).score(-1).sitAt(Locations.LEO_SEAT);
        make(d, level, "gus", g, "Gus", "janitor", false, false, "gus", Locations.GUS).score(2.4f).activity(Activity.WORKING)
                .wander(List.of(Locations.GUS, b(36, 44), b(40, 40), b(46, 42), b(30, 44)));
        int n = 0;
        // check-in queue at the Meridian desk and the other islands
        for (int i = 0; i < 3; i++) crowd(d, level, r, g, n++, b(21 + i * 3, 22)).stand(b(21 + i * 3, 22), 180f);
        crowd(d, level, r, g, n++, b(9, 26)).stand(b(9, 26), 180f);
        crowd(d, level, r, g, n++, b(43, 26)).stand(b(43, 26), 180f);
        // people strolling between the board, the newsstand, the benches and the windows
        for (int i = 0; i < 7; i++) crowd(d, level, r, g, n++, LANDSIDE_SPOTS.get(i)).wander(some(r, LANDSIDE_SPOTS, 5));
        // sitting and waiting
        int[][] seats = {{3, 40}, {11, 40}, {2, 46}, {6, 46}, {8, 41}};
        for (int[] s : seats) crowd(d, level, r, g, n++, b(s[0], s[1])).sitAt(b(s[0], s[1]));
        // two friends chatting by the board
        crowd(d, level, r, g, n++, b(22, 13)).activity(Activity.CHATTING).stand(b(22, 13), -90f);
        crowd(d, level, r, g, n, b(23, 13)).activity(Activity.CHATTING).stand(b(23, 13), 90f);
    }

    private static void security(StoryDirector d, ServerLevel level, Random r) {
        String g = "security";
        make(d, level, "reyes", g, "Officer Reyes", "reyes", false, false, "reyes", Locations.REYES).score(3.2f).activity(Activity.GUARD).stand(Locations.REYES, -90f);
        make(d, level, "officer_1", g, "Officer Pell", "officer", true, false, "officer", b(60, 17)).score(3.6f).activity(Activity.GUARD).barks("guard").stand(b(60, 17), -90f);
        make(d, level, "officer_2", g, "Officer Grant", "officer", false, false, "officer", b(60, 33)).score(3.5f).activity(Activity.GUARD).barks("guard").stand(b(60, 33), -90f);
        make(d, level, "xray", g, "X-ray Operator", "agent", true, false, "xray", b(66, 23)).score(3.9f).activity(Activity.WORKING).barks("staff").stand(b(66, 23), 0f);
        int n = 0;
        for (int i = 0; i < 5; i++) {
            BlockPos at = b(55 + (i % 2) * 3, 22 + i * 2);
            crowd(d, level, r, g, n++, at).stand(at, -90f);
        }
        crowd(d, level, r, g, n, b(72, 33)).activity(Activity.IDLE).stand(b(72, 33), 90f);
    }

    static final List<BlockPos> AIRSIDE_SPOTS = List.of(b(84, 11), b(88, 11), b(98, 16), b(98, 6), b(95, 24), b(86, 25), b(100, 30),
            b(110, 28), b(120, 28), b(130, 27), b(114, 4), b(124, 4), b(134, 4), b(104, 36), b(91, 37));

    private static void airside(StoryDirector d, ServerLevel level, Random r) {
        String g = "airside";
        make(d, level, "tomas", g, "Tomas", "tomas", false, false, "tomas", Locations.TOMAS).score(3.1f).activity(Activity.WORKING).stand(Locations.TOMAS, 0f);
        make(d, level, "barista", g, "Barista", "clerk", true, false, "barista", b(80, 4)).score(4.0f).activity(Activity.WORKING).barks("staff").stand(b(80, 4), 0f);
        make(d, level, "clerk", g, "Shop Clerk", "clerk", true, false, "clerk", b(102, 11)).score(4.3f).activity(Activity.WORKING).barks("staff").stand(b(102, 11), 180f);
        make(d, level, "host", g, "Lounge Host", "agent", true, false, "lounge_host", b(87, 42)).score(4.8f).barks("staff").stand(b(87, 42), 0f);
        make(d, level, "halvorsen", g, "Mr. Halvorsen", "halvorsen", false, false, "halvorsen", b(82, 50)).score(4.91f).activity(Activity.PHONE).sitAt(b(82, 50));
        make(d, level, "corinne", g, "Corinne", "corinne", true, false, "corinne", Locations.CORINNE).score(4.7f).activity(Activity.WORKING).stand(Locations.CORINNE, 180f);
        make(d, level, "gate_b5", g, "Gate Agent", "agent", false, false, "staff", b(Locations.GATE_B5 + 2, 53)).score(4.2f).barks("staff").stand(b(Locations.GATE_B5 + 2, 53), 180f);
        make(d, level, "gate_b6", g, "Gate Agent", "agent", true, false, "staff", b(Locations.GATE_B6 + 2, 53)).score(4.4f).barks("staff").stand(b(Locations.GATE_B6 + 2, 53), 180f);
        int n = 0;
        // the lounge
        for (int[] s : new int[][]{{86, 50}, {91, 50}, {95, 50}}) crowd(d, level, r, g, n++, b(s[0], s[1])).score(4.6f + r.nextInt(4) / 10f).sitAt(b(s[0], s[1]));
        // concourse strollers
        for (int i = 0; i < 9; i++) crowd(d, level, r, g, n++, AIRSIDE_SPOTS.get(i)).wander(some(r, AIRSIDE_SPOTS, 5));
        // people waiting at the other gates
        for (int gx : new int[]{Locations.GATE_B5, Locations.GATE_B6}) {
            for (int i = 0; i < 7; i++) {
                int dx = new int[]{1, 2, 3, 5, 6}[r.nextInt(5)] * (r.nextBoolean() ? 1 : -1);
                BlockPos s = b(gx + dx, new int[]{36, 39, 44, 47}[r.nextInt(4)]);
                crowd(d, level, r, g, n++, s).sitAt(s);
            }
        }
    }

    /** Stagehands patrolling the set behind the staff door. */
    private static void backstage(StoryDirector d, ServerLevel level) {
        String g = "backstage";
        make(d, level, "stagehand_1", g, "Stagehand", "officer", false, false, "stagehand", b(112, -6)).score(-1).activity(Activity.GUARD).barks("guard")
                .speed(0.55).patrol(List.of(b(112, -6), b(138, -6), b(138, -12), b(112, -12)));
        make(d, level, "stagehand_2", g, "Stagehand", "agent", true, false, "stagehand", b(110, -19)).score(-1).activity(Activity.GUARD).barks("guard")
                .speed(0.5).patrol(List.of(b(110, -19), b(139, -19), b(139, -20), b(110, -20)));
        make(d, level, "pa", g, "Production Assistant", "clerk", false, false, "production", b(129, -29)).score(-1).activity(Activity.SLEEPING).sitAt(b(129, -29));
    }

    /** Seats at the B7 waiting area, in a fixed order. */
    static List<BlockPos> gateSeats() {
        List<BlockPos> out = new ArrayList<>();
        int gx = Locations.GATE_B7;
        for (int z : new int[]{39, 44, 36, 47})
            for (int dx : new int[]{1, 2, 3, 5, 6}) {
                out.add(b(gx - dx, z));
                out.add(b(gx + dx, z));
            }
        return out;
    }

    private static void gateB7(StoryDirector d, ServerLevel level, Random r) {
        String g = "b7";
        int gx = Locations.GATE_B7;
        make(d, level, "marcus", g, "Marcus", "marcus", false, false, "marcus", b(gx + 4, 50)).score(2.9f).activity(Activity.PHONE)
                .wander(List.of(b(gx + 4, 50), b(gx + 7, 51), b(gx + 2, 50), b(gx + 6, 49)));
        make(d, level, "ines", g, "Ines", "ines", true, false, "ines", b(gx - 6, 47)).score(3.3f).activity(Activity.READING).sitAt(b(gx - 6, 47));
        List<BlockPos> seats = gateSeats();
        int si = 0;
        for (int i = 0; i < B7_CROWD_SEATS.length; i++) {
            BlockPos at = seats.get(si++);
            if (at.equals(b(gx - 6, 47))) at = seats.get(si++);
            crowd(d, level, r, g, i, at).sitAt(at);
        }
    }

    /** Walk from the B7 podium, through the jet bridge, down the aisle to the seat. */
    public static List<BlockPos> boardingRoute(String seatCode) {
        Plane g = Locations.GATE_PLANE;
        BlockPos seat = seat(g, seatCode);
        int gx = Locations.GATE_B7;
        int fy = g.y() + 1;
        List<BlockPos> route = new ArrayList<>();
        route.add(b(gx, 54));
        route.add(teleportPoint(new BlockPos(gx, fy, 68)));
        route.add(new BlockPos(gx, fy, 70));
        route.add(teleportPoint(g.local(9, 1, -3)));
        route.add(g.aisle(9));
        route.add(new BlockPos(seat.getX(), fy, g.z()));
        return route;
    }

    /** Waypoints above y=1000 tell the NPC to step through (doors, jet bridge) instead of pathing. */
    public static BlockPos teleportPoint(BlockPos p) {
        return p.above(1000);
    }

    // =========================================================================================
    // Act II: Flight 417
    // =========================================================================================
    private static void airPlane(StoryDirector d, ServerLevel level, Random r) {
        String g = "air_plane";
        Plane a = Locations.AIR_PLANE;
        String[][] named = {{"marcus", "Marcus", "marcus", "0"}, {"ines", "Ines", "ines", "1"}, {"priya", "Priya", "priya", "1"},
                {"leo", "Leo", "leo", "0"}, {"halvorsen", "Mr. Halvorsen", "halvorsen", "0"}};
        for (String[] p : named) {
            BlockPos s = seat(a, SEATS.get(p[0]));
            StoryNpc n = make(d, level, p[0], g, p[1], p[2], p[3].equals("1"), p[0].equals("leo"), p[0], s);
            n.score(p[0].equals("halvorsen") ? 4.99f : p[0].equals("leo") ? -1 : 3.2f);
            if (p[0].equals("ines")) n.activity(Activity.READING);
            if (p[0].equals("halvorsen")) n.activity(Activity.PHONE);
            n.sitAt(s);
        }
        BlockPos e = seat(a, "20F");
        make(d, level, "edith", g, "Edith", "edith", true, false, "edith", e).score(-1).activity(Activity.KNITTING).sitAt(e);
        int n = 0;
        List<String> codes = new ArrayList<>(List.of(B7_CROWD_SEATS));
        codes.addAll(List.of(AIR_EXTRA_SEATS));
        for (String code : codes) {
            BlockPos s = seat(a, code);
            String skin = crowdSkin(r);
            StoryNpc p = make(d, level, "air_" + n, g, "Passenger", skin, slimSkin(skin), false, n % 3 == 0 ? "passenger" : n % 3 == 1 ? "passenger_b" : "passenger_c", s);
            p.barks("crowd").score(3f + r.nextInt(20) / 10f).activity(n % 4 == 0 ? Activity.SLEEPING : n % 4 == 1 ? Activity.READING : Activity.IDLE);
            p.sitAt(s);
            n++;
        }
        make(d, level, "june", g, "June", "june", true, false, "june", a.galley()).score(4.97f).stand(a.galley(), 90f);
        make(d, level, "theo", g, "Theo", "attendant", false, false, "theo", a.rearGalley()).score(4.8f).stand(a.rearGalley(), 90f);
    }

    // =========================================================================================
    // Act III: the island
    // =========================================================================================
    private static void tribe(StoryDirector d, ServerLevel level, Random r) {
        String g = "tribe";
        make(d, level, "ahu", g, "Elder Ahu", "ahu", false, false, "ahu", IslandBuilder.village(0, -29)).score(-1).sitAt(IslandBuilder.village(0, -29));
        make(d, level, "nalu", g, "Nalu", "nalu", false, false, "nalu", IslandBuilder.village(-8, 10)).score(-1).activity(Activity.GUARD).stand(IslandBuilder.village(-8, 10), 135f);
        make(d, level, "mira", g, "Mira", "mira", true, false, "mira", IslandBuilder.village(-9, 6)).score(-1).activity(Activity.WORKING).stand(IslandBuilder.village(-9, 6), 0f);
        make(d, level, "teo", g, "Teo", "teo", false, false, "teo", IslandBuilder.dockStart()).score(-1).activity(Activity.WORKING).stand(IslandBuilder.dockStart(), -90f);
        make(d, level, "pip", g, "Pip", "pip", true, true, "pip", IslandBuilder.village(3, 3)).score(-1)
                .wander(List.of(IslandBuilder.village(3, 3), IslandBuilder.village(-4, 4), IslandBuilder.village(5, -4), IslandBuilder.village(0, 6), IslandBuilder.village(10, 2)));
        List<BlockPos> plaza = List.of(IslandBuilder.village(5, 5), IslandBuilder.village(-5, -5), IslandBuilder.village(6, -4), IslandBuilder.village(-6, 3),
                IslandBuilder.village(10, 4), IslandBuilder.village(-10, 3), IslandBuilder.village(2, -9), IslandBuilder.village(0, 9));
        for (int i = 0; i < 6; i++) {
            BlockPos at = plaza.get(i);
            StoryNpc t = make(d, level, "tribe_" + i, g, "Tidefolk", "tribe_" + (i % 4), i % 2 == 1, false, "tribe_" + (i % 4), at);
            t.barks("tribe").score(-1);
            if (i < 4) t.wander(some(r, plaza, 4));
            else t.activity(Activity.WORKING).stand(i == 4 ? IslandBuilder.village(10, 2) : IslandBuilder.village(-12, 0), i == 4 ? 0f : 180f);
        }
    }
}
