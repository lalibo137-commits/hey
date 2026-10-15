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

    private record Step(int wait, Runnable action) {}

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

    private static void build() {
        Locations.Plane g = Locations.GATE_PLANE;
        add(200, () -> shot("02_intro_cutscene"));
        add(320, () -> shot("03_arrival"));
        add(20, () -> Minecraft.getInstance().setScreen(new JournalScreen()));
        add(30, () -> shot("04_journal"));
        add(10, () -> Minecraft.getInstance().setScreen(null));
        add(10, () -> server("tp:16.5,64,26.5,180,5"));
        add(40, () -> server("talk:dana,dana"));
        add(80, () -> shot("05_dialogue_dana"));
        add(10, () -> Minecraft.getInstance().setScreen(null));
        add(10, () -> server("tp:30.5,64,20.5,150,10"));
        add(60, () -> shot("06_checkin_hall"));
        add(10, () -> server("tp:43.5,64,23.5,-90,10"));
        add(60, () -> shot("07_security"));
        add(10, () -> server("tp:63.5,64,13.5,180,10"));
        add(60, () -> shot("08_cafe"));
        add(10, () -> server("quest:wait_gate"));
        add(10, () -> server("tp:116.5,64,30.5,0,10"));
        add(80, () -> shot("09_gate_b7"));
        add(10, () -> server("quest:boarding_call"));
        add(400, () -> shot("10_boarding"));
        add(10, () -> server("quest:find_seat"));
        add(10, () -> server("tp:" + (g.x() + 10.5) + "," + (g.y() + 1) + "," + (g.z() + 0.5) + ",-90,5"));
        add(80, () -> shot("11_cabin_gate"));
        add(10, () -> server("quest:takeoff"));
        add(10, () -> server("ticks:495"));
        add(140, () -> shot("12_takeoff_cutscene"));
        add(300, () -> shot("13_cruising"));
        add(10, () -> server("quest:lights_out"));
        add(150, () -> shot("14_stare_cutscene"));
        add(260, () -> shot("15_after_stare"));
        add(10, () -> server("quest:turbulence"));
        add(300, () -> shot("16_turbulence"));
        add(10, () -> server("island"));
        add(40, () -> server("land"));
        add(10, () -> server("quest:wake"));
        add(160, () -> shot("17_shore_cutscene"));
        add(300, () -> shot("18_crash_site"));
        add(10, () -> server("spawn:tribe"));
        add(10, () -> {
            var v = com.airlinestory.world.IslandBuilder.village(-6, 10);
            server("tp:" + (v.getX() + 0.5) + "," + (v.getY() + 2) + "," + (v.getZ() + 0.5) + ",160,15");
        });
        add(120, () -> shot("19_village"));
        add(10, () -> server("talk:ahu,ahu"));
        add(80, () -> shot("20_dialogue_ahu"));
        add(10, () -> Minecraft.getInstance().setScreen(null));
        add(10, () -> {
            var s = com.airlinestory.world.IslandBuilder.station(0, 1, 9);
            server("tp:" + (s.getX() + 0.5) + "," + (s.getY() + 1) + "," + (s.getZ() + 0.5) + ",180,5");
        });
        add(120, () -> shot("21_station"));
        add(10, () -> {
            var s = com.airlinestory.world.IslandBuilder.grove(0, 6);
            server("tp:" + (s.getX() + 0.5) + "," + (s.getY() + 1) + "," + (s.getZ() + 0.5) + ",180,10");
        });
        add(100, () -> shot("22_grove"));
        add(10, () -> server("time:18000"));
        add(10, () -> server("hollow"));
        add(80, () -> shot("23_hollow_night"));
        add(10, () -> {
            var b = com.airlinestory.world.IslandBuilder.dockStart();
            server("tp:" + (b.getX() + 0.5) + "," + (b.getY() + 1) + "," + (b.getZ() + 0.5) + ",-90,10");
            server("time:1000");
        });
        add(100, () -> shot("24_dock"));
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
        if (++timer >= steps.get(index).wait) {
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
