package com.airlinestory.story;

import com.airlinestory.AirlineStory;
import com.airlinestory.entity.HollowEntity;
import com.airlinestory.entity.ModEntities;
import com.airlinestory.world.IslandBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Hooks used only by the automated test run (-Dairlinestory.autotest=true). */
public final class DevHooks {
    private DevHooks() {}

    public static void run(MinecraftServer server, String action) {
        StoryDirector d = StoryDirector.get();
        ServerPlayer p = server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
        if (d == null || p == null) {
            AirlineStory.LOG.warn("[autotest] no director/player for {}", action);
            return;
        }
        AirlineStory.LOG.info("[autotest] {}", action);
        String[] a = action.split(":", 2);
        String arg = a.length > 1 ? a[1] : "";
        switch (a[0]) {
            case "quest" -> d.jump(p, arg);
            case "ticks" -> d.state().questTicks = Integer.parseInt(arg);
            case "flag" -> { d.state().flag(arg); d.markSync(); }
            case "tp" -> {
                String[] c = arg.split(",");
                if (p.getVehicle() != null) p.stopRiding();
                p.teleportTo(d.level(), Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2]),
                        Float.parseFloat(c[3]), Float.parseFloat(c[4]));
            }
            case "time" -> d.level().setDayTime(Long.parseLong(arg));
            case "spawn" -> d.spawnGroup(arg);
            case "island" -> {
                long until = System.nanoTime() + 240_000_000_000L;
                IslandBuilder b = new IslandBuilder(d.state());
                while (!b.done() && System.nanoTime() < until) b.step(d.level(), System.nanoTime() + 1_000_000_000L);
                AirlineStory.LOG.info("[autotest] island done={} {}%", b.done(), b.percent());
            }
            case "land" -> { d.state().flag("crashing"); d.tryLand(p); }
            case "hollow" -> {
                HollowEntity h = ModEntities.HOLLOW.get().create(d.level());
                if (h == null) return;
                Vec3 at = p.position().add(p.getLookAngle().multiply(5, 0, 5));
                h.configure(false, true, 20 * 60);
                h.moveTo(at.x, p.getY(), at.z, p.getYRot() + 180, 0);
                d.level().addFreshEntity(h);
            }
            case "talk" -> d.openDialogue(p, arg.split(",")[0], arg.contains(",") ? d.npc(arg.split(",")[1]) : null);
            case "skip" -> d.skip(p);
            default -> AirlineStory.LOG.warn("[autotest] unknown action {}", action);
        }
    }
}
