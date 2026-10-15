package com.airlinestory.story;

import com.airlinestory.world.IslandBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Camera paths for the story's cutscenes. Pure data, shared by both sides. */
public final class Cutscenes {
    private Cutscenes() {}

    public static final class Shot {
        public final Vec3 from, to, lookFrom, lookTo;
        public final int ticks;
        public String title = "", sub = "";
        public int fadeIn, fadeOut;
        public float shake;
        public boolean black;
        public String sound = "";

        Shot(Vec3 from, Vec3 to, Vec3 lookFrom, Vec3 lookTo, int ticks) {
            this.from = from;
            this.to = to;
            this.lookFrom = lookFrom;
            this.lookTo = lookTo;
            this.ticks = ticks;
        }

        Shot text(String t, String s) { this.title = t; this.sub = s; return this; }
        Shot fade(int in, int out) { this.fadeIn = in; this.fadeOut = out; return this; }
        Shot shake(float s) { this.shake = s; return this; }
        Shot black() { this.black = true; return this; }
        Shot sound(String s) { this.sound = s; return this; }
    }

    private static Shot shot(Vec3 from, Vec3 to, Vec3 look, int ticks) {
        return new Shot(from, to, look, look, ticks);
    }

    private static Shot shot(Vec3 from, Vec3 to, Vec3 lookFrom, Vec3 lookTo, int ticks) {
        return new Shot(from, to, lookFrom, lookTo, ticks);
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    public static int duration(String id, FlagView view) {
        int t = 0;
        for (Shot s : get(id, view)) t += s.ticks;
        return t;
    }

    public static List<Shot> get(String id, FlagView view) {
        List<Shot> l = new ArrayList<>();
        Locations.Plane a = Locations.AIR_PLANE;
        switch (id) {
            case "intro" -> {
                l.add(shot(v(-30, 95, -60), v(60, 85, -50), v(60, 66, 20), v(70, 66, 20), 140)
                        .text("MERIDIAN INTERNATIONAL AIRPORT", "Tuesday. 6:42 AM.").fade(40, 0).sound("wind"));
                l.add(shot(v(19.5, 66, -24), v(19.5, 65.6, -10), v(19.5, 66, 10), 110)
                        .text("Flight MA417 to Port Ellery", "Departs Gate B7 at 9:10"));
                l.add(shot(v(30, 70, 8), v(22, 68, 18), v(16.5, 65.5, 32.5), 120)
                        .text("Everyone is rated.", "Everyone is watching.").fade(0, 30));
            }
            case "takeoff" -> {
                l.add(shot(a.at(-25, 8, -30), a.at(35, 5, -30), a.at(5, 2, 0), a.at(40, 2, 0), 160)
                        .text("36,000 FEET", "Somewhere over the ocean").fade(40, 0).sound("engine"));
                l.add(shot(a.at(28, 12, -26), a.at(31, 4, -12), a.at(30, 0, -20), a.at(28, 0, -16), 100)
                        .text("Meridian Flight 417", "Six hours to Port Ellery"));
                l.add(shot(a.at(52, 3.0, 0.5), a.at(36, 3.0, 0.5), a.at(0, 2.6, 0.5), 120)
                        .text("Please enjoy", "your experience.").fade(0, 20));
            }
            case "stare" -> {
                l.add(shot(a.at(39.5, 2.7, 0.5), a.at(37.5, 2.7, 0.5), a.at(4, 2.0, 0.5), 170).fade(15, 0).sound("heartbeat"));
                l.add(shot(a.at(33.6, 2.6, 0.5), a.at(32.8, 2.5, 0.2), a.at(30.5, 2.2, -2.5), 45).shake(0.3f).fade(0, 6).sound("sting"));
            }
            case "storm" -> {
                l.add(shot(a.at(70, 14, 35), a.at(50, 8, 25), a.at(25, 2, 0), 130).shake(0.6f).sound("thunder"));
                l.add(shot(a.at(20, 3, 0.5), a.at(30, 3, 0.5), a.at(60, 2, 0.5), 60).shake(1.6f).text("BRACE! BRACE! BRACE!", "").sound("alarm"));
                l.add(shot(a.at(20, 3, 0.5), a.at(20, 3, 0.5), a.at(60, 2, 0.5), 140).black()
                        .text("MAYDAY. MAYDAY. MAYDAY.", "Meridian four-one-seven... we have lost all...").sound("static"));
            }
            case "shore" -> {
                int cy = view.counter("y:crash") == 0 ? 64 : view.counter("y:crash");
                Vec3 s = Vec3.atBottomCenterOf(Locations.island(Locations.CRASH[0], cy, Locations.CRASH[1]));
                l.add(shot(s.add(25, 30, 45), s.add(5, 10, 18), s.add(-5, 0, 0), s.add(6, 0, 6), 200)
                        .text("DAY 1", "Location: unknown").fade(70, 0).sound("waves"));
                l.add(shot(s.add(6, 3, 12), s.add(6, 1.6, 8), s.add(6, 0.5, 6), 80).fade(0, 25));
            }
            case "lab" -> {
                BlockPos c = IslandBuilder.station(0, 0, 0);
                Vec3 L = Vec3.atBottomCenterOf(c);
                l.add(shot(L.add(0, 2.6, 2.5), L.add(-1.5, 3, -0.5), L.add(-1.5, 3, -3.5), 160)
                        .text("EXPERIENCE LAB 4", "Meridian Experiences").fade(15, 15).sound("hum"));
            }
            case "escape" -> {
                Vec3 b = Vec3.atBottomCenterOf(IslandBuilder.boatOrigin());
                Vec3 c = new Vec3(Locations.ISLAND_X, 70, Locations.ISLAND_Z);
                l.add(shot(b.add(-6, 4, -6), b.add(6, 6, -10), b.add(6, 2, 2), 120).text("FIRST LIGHT", "").fade(20, 0).sound("waves"));
                l.add(shot(b.add(20, 10, 20), b.add(70, 34, 70), b.add(6, 1, 2), c, 220)
                        .text("The island grows smaller.", "It does not get any less watched.").fade(0, 50));
            }
            default -> l.add(shot(v(0, 100, 0), v(0, 100, 0), v(0, 90, 10), 20));
        }
        return l;
    }
}
