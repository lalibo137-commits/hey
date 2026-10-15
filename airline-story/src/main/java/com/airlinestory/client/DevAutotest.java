package com.airlinestory.client;

import com.airlinestory.AirlineStory;
import com.airlinestory.story.DevHooks;
import com.airlinestory.story.Locations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Automated tour for CI (-Dairlinestory.autotest=true): starts a story world, jumps through key scenes and
 * takes screenshots into run/screenshots, then quits.
 */
public final class DevAutotest {
    private DevAutotest() {}

    public static final boolean ENABLED = Boolean.getBoolean("airlinestory.autotest");

    private record Step(int delay, Runnable action) {}

    private static final List<Step> steps = new ArrayList<>();
    private static int index = -1, timer, titleTicks, worldTicks;
    private static boolean started;

    private static void server(String action) {
        IntegratedServer s = Minecraft.getInstance().getSingleplayerServer();
        if (s != null) s.execute(() -> DevHooks.run(s, action));
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> AirlineStory.LOG.info("[autotest] screenshot {}", name));
    }

    private static void add(int wait, Runnable r) {
        steps.add(new Step(wait, r));
    }

    private static void tp(double x, double y, double z, float yaw, float pitch) {
        server("tp:" + x + "," + y + "," + z + "," + yaw + "," + pitch);
    }

    private static void view(String name, int wait, Runnable where) {
        add(10, where);
        add(wait, () -> shot(name));
    }

    private static void build() {
        Locations.Plane g = Locations.GATE_PLANE;
        Locations.Plane a = Locations.AIR_PLANE;
        int[] V = com.airlinestory.world.IslandTerrain.VILLAGE;
        add(160, () -> shot("02_intro_cutscene"));
        add(420, () -> shot("03_arrival_hud"));
        add(20, () -> Minecraft.getInstance().setScreen(new JournalScreen()));
        add(30, () -> shot("04_journal"));
        add(10, () -> Minecraft.getInstance().setScreen(null));
        view("05_hall_entrance", 70, () -> tp(26.5, 64, 3.5, 0, 8));
        add(10, () -> tp(26.5, 64, 26.5, 180, 5));
        add(40, () -> server("talk:dana,dana"));
        add(80, () -> shot("06_dialogue_dana"));
        add(10, () -> Minecraft.getInstance().setScreen(null));
        view("07_checkin_islands", 60, () -> tp(40.5, 66, 18.5, 140, 15));
        view("08_west_benches", 60, () -> tp(14.5, 64, 36.5, 130, 12));
        view("09_closet", 50, () -> tp(47.5, 64, 49.5, 0, 20));
        add(10, () -> server("spawn:security"));
        view("10_security", 70, () -> tp(56.5, 65, 18.5, -60, 15));
        view("11_interview_room", 50, () -> tp(68.5, 64, 44.5, 0, 15));
        add(10, () -> server("spawn:airside"));
        view("12_cafe", 70, () -> tp(84.5, 64, 12.5, 180, 10));
        view("13_lounge", 60, () -> tp(88.5, 64, 43.5, 0, 5));
        view("13b_lounge_tv", 50, () -> tp(87.5, 64, 50.5, 0, -10));
        view("13c_poster", 40, () -> tp(100.5, 64, 37.5, 0, 0));
        view("14_concourse", 60, () -> tp(108.5, 66, 30.5, -90, 10));
        add(10, () -> server("spawn:backstage"));
        view("15_backstage", 60, () -> tp(106.5, 64, -4.5, 180, 5));
        view("16_control_room", 50, () -> tp(130.5, 64, -24.5, 180, 15));
        add(10, () -> server("quest:wait_gate"));
        view("17_gate_b7", 80, () -> tp(148.5, 64, 32.5, 0, 10));
        add(10, () -> server("quest:boarding_call"));
        add(10, () -> tp(140.5, 64, 50.5, -30, 10));
        add(400, () -> shot("18_boarding"));
        view("19_jet_bridge", 50, () -> tp(148.5, 70, 69.5, 0, 5));
        add(10, () -> server("quest:find_seat"));
        view("20_cabin_gate", 80, () -> tp(g.x() + 10.5, g.y() + 1, g.z() + 0.5, -90, 5));
        add(10, () -> server("sitgate"));
        add(20, () -> server("quest:takeoff"));
        add(10, () -> server("ticks:495"));
        add(140, () -> shot("21_takeoff_cutscene"));
        add(300, () -> shot("22_cruising"));
        add(10, () -> server("quest:the_hold"));
        view("23_cargo_hold", 60, () -> tp(a.x() + 20.5, a.y() - 3, a.z() + 0.5, -90, 10));
        add(10, () -> server("quest:lights_out"));
        add(10, () -> server("flag:june_stare"));
        add(10, () -> server("sitair"));
        add(150, () -> shot("24_stare_cutscene"));
        add(260, () -> shot("25_after_stare"));
        add(10, () -> server("quest:turbulence"));
        add(300, () -> shot("26_turbulence"));
        add(10, () -> server("island"));
        add(40, () -> server("land"));
        add(10, () -> server("quest:wake"));
        add(160, () -> shot("27_shore_cutscene"));
        add(300, () -> shot("28_crash_site"));
        add(10, () -> server("gm:spectator"));
        add(10, () -> server("spawn:tribe"));
        String[][] spots = {
                {"29_crash_aerial", "CRASH", "-30,28,40"}, {"30_lone_seat", "LONE_SEAT", "10,6,-12"},
                {"31_falls", "LOWER_POOL", "-22,14,22"}, {"32_upper_falls", "UPPER_POOL", "18,10,18"},
                {"33_lookout_tower", "LOOKOUT", "24,16,24"}, {"34_village_aerial", "VILLAGE", "-36,30,36"},
                {"35_village_ground", "VILLAGE", "-14,4,14"}, {"36_dock", "VILLAGE", "48,10,-14"},
                {"37_burned_village", "BURNED", "-18,14,18"}, {"38_mangroves", "MANGROVE", "24,16,-24"},
                {"39_cave_mouth", "CAVE_MOUTH", "-16,8,10"}, {"40_lagoon", "LAGOON", "-40,26,-30"},
                {"41_grove", "GROVE", "-20,14,20"}, {"41b_grove_ground", "GROVE", "-1,3,9"}, {"42_station", "PEAK", "-26,16,30"},
                {"42b_station_eye", "PEAK", "6,6,32"}, {"42c_station_far", "PEAK", "60,30,90"},
                {"43_island_from_sea", "CRASH", "60,40,120"}, {"44_island_overview", "PEAK", "0,170,140"}};
        for (String[] sp : spots) {
            int[] at = switch (sp[1]) {
                case "CRASH" -> com.airlinestory.world.IslandTerrain.CRASH; case "LONE_SEAT" -> com.airlinestory.world.IslandTerrain.LONE_SEAT; case "LOWER_POOL" -> com.airlinestory.world.IslandTerrain.LOWER_POOL;
                case "UPPER_POOL" -> com.airlinestory.world.IslandTerrain.UPPER_POOL; case "LOOKOUT" -> com.airlinestory.world.IslandTerrain.LOOKOUT; case "VILLAGE" -> com.airlinestory.world.IslandTerrain.VILLAGE;
                case "BURNED" -> com.airlinestory.world.IslandTerrain.BURNED; case "MANGROVE" -> com.airlinestory.world.IslandTerrain.MANGROVE; case "CAVE_MOUTH" -> com.airlinestory.world.IslandTerrain.CAVE_MOUTH;
                case "LAGOON" -> com.airlinestory.world.IslandTerrain.LAGOON; case "GROVE" -> com.airlinestory.world.IslandTerrain.GROVE; default -> com.airlinestory.world.IslandTerrain.PEAK;
            };
            view(sp[0], 100, () -> server("look:" + at[0] + "," + at[1] + "," + sp[2]));
        }
        add(10, () -> server("gm:adventure"));
        add(10, () -> {
            int[] cc = com.airlinestory.world.IslandTerrain.CAVE_CAMP;
            tp(Locations.ISLAND_X + cc[0] - 5.5, com.airlinestory.world.IslandBuilder.CAVE_Y + 1, Locations.ISLAND_Z + cc[1] + 0.5, -90, 10);
        });
        add(60, () -> shot("45_cave_camp"));
        add(10, () -> {
            var v = com.airlinestory.world.IslandBuilder.village(0, -24);
            tp(v.getX() + 0.5, v.getY(), v.getZ() + 0.5, 180, 5);
        });
        add(20, () -> server("talk:ahu,ahu"));
        add(80, () -> shot("46_longhouse_ahu"));
        add(10, () -> Minecraft.getInstance().setScreen(null));
        add(10, () -> {
            var s = com.airlinestory.world.IslandBuilder.station(-3, 1, 4);
            tp(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, 180, 10);
        });
        add(80, () -> shot("47_station_inside"));
        add(10, () -> server("time:18000"));
        add(10, () -> server("hollow"));
        add(80, () -> shot("48_hollow_night"));
        add(40, () -> Minecraft.getInstance().stop());
    }

    public static void tick() {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen instanceof TitleScreen) {
                if (++titleTicks == 60) shot("01_title_screen");
                if (titleTicks == 80) {
                    started = true;
                    mc.setScreen(new StoryStartScreen(mc.screen));
                    StoryStartScreen.startNew();
                }
            }
            return;
        }
        if (mc.player == null || mc.level == null) return;
        if (index < 0) {
            if (++worldTicks < 40) return;
            build();
            index = 0;
            timer = 0;
        }
        if (index >= steps.size()) return;
        if (++timer >= steps.get(index).delay) {
            timer = 0;
            try {
                steps.get(index).action.run();
            } catch (Exception e) {
                AirlineStory.LOG.error("[autotest] step {} failed", index, e);
            }
            index++;
        }
    }
}
