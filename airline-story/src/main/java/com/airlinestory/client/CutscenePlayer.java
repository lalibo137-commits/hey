package com.airlinestory.client;

import com.airlinestory.net.Net;
import com.airlinestory.story.Cutscenes;
import com.airlinestory.story.Snd;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Flies a camera along a cutscene's shots, then hands control back to the player. */
public final class CutscenePlayer {
    private CutscenePlayer() {}

    @Nullable private static String id;
    private static List<Cutscenes.Shot> shots = List.of();
    private static int shot, t;
    @Nullable private static Marker camera;
    private static boolean hadHideGui;
    private static float lastYaw;

    private record Task(int at, Runnable r) {}
    private static final List<Task> tasks = new ArrayList<>();
    private static int clock;

    public static boolean isPlaying() {
        return id != null;
    }

    public static void later(int ticks, Runnable r) {
        tasks.add(new Task(clock + ticks, r));
    }

    static void play(String cutscene) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (id != null) finish(false);
        id = cutscene;
        shots = Cutscenes.get(cutscene, ClientStory.INSTANCE);
        shot = 0;
        t = 0;
        camera = new Marker(EntityType.MARKER, mc.level);
        hadHideGui = mc.options.hideGui;
        mc.options.hideGui = true;
        Cutscenes.Shot s = shots.get(0);
        place(s, 0f, true);
        mc.setCameraEntity(camera);
        mc.setScreen(new CutsceneScreen());
        cue(s);
    }

    static void tick() {
        clock++;
        if (!tasks.isEmpty()) {
            List<Task> due = new ArrayList<>();
            for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
                Task k = it.next();
                if (k.at <= clock) { due.add(k); it.remove(); }
            }
            for (Task k : due) k.r.run();
        }
        if (id == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || camera == null) { id = null; return; }
        t++;
        Cutscenes.Shot s = shots.get(shot);
        if (t >= s.ticks) {
            shot++;
            t = 0;
            if (shot >= shots.size()) {
                finish(true);
                return;
            }
            s = shots.get(shot);
            place(s, 0f, true);
            cue(s);
            return;
        }
        place(s, t / (float) s.ticks, false);
        if (!(mc.screen instanceof CutsceneScreen)) mc.setScreen(new CutsceneScreen());
    }

    private static void place(Cutscenes.Shot s, float k, boolean snap) {
        if (camera == null) return;
        float e = ease(k);
        Vec3 pos = s.from.lerp(s.to, e);
        Vec3 look = s.lookFrom.lerp(s.lookTo, e);
        Vec3 d = look.subtract(pos);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        if (!snap) yaw = lastYaw + Mth.wrapDegrees(yaw - lastYaw);
        lastYaw = yaw;
        if (snap) {
            camera.setPos(pos.x, pos.y, pos.z);
            camera.xo = pos.x; camera.yo = pos.y; camera.zo = pos.z;
            camera.xOld = pos.x; camera.yOld = pos.y; camera.zOld = pos.z;
            camera.setYRot(yaw); camera.setXRot(pitch);
            camera.yRotO = yaw; camera.xRotO = pitch;
        } else {
            camera.xo = camera.getX(); camera.yo = camera.getY(); camera.zo = camera.getZ();
            camera.xOld = camera.getX(); camera.yOld = camera.getY(); camera.zOld = camera.getZ();
            camera.yRotO = camera.getYRot(); camera.xRotO = camera.getXRot();
            camera.setPos(pos.x, pos.y, pos.z);
            camera.setYRot(yaw); camera.setXRot(pitch);
        }
    }

    private static float ease(float k) {
        return k * k * (3 - 2 * k);
    }

    private static void cue(Cutscenes.Shot s) {
        SoundEvent e = switch (s.sound) {
            case "wind" -> Snd.of(SoundEvents.ELYTRA_FLYING);
            case "engine", "hum" -> Snd.of(SoundEvents.BEACON_AMBIENT);
            case "heartbeat" -> Snd.of(SoundEvents.WARDEN_HEARTBEAT);
            case "sting" -> Snd.of(SoundEvents.ELDER_GUARDIAN_CURSE);
            case "thunder" -> Snd.of(SoundEvents.LIGHTNING_BOLT_THUNDER);
            case "alarm" -> Snd.of(SoundEvents.BELL_BLOCK);
            case "static" -> Snd.of(SoundEvents.FIRE_AMBIENT);
            case "waves" -> Snd.of(SoundEvents.WEATHER_RAIN);
            default -> null;
        };
        if (e == null) return;
        float pitch = s.sound.equals("engine") || s.sound.equals("hum") ? 0.5f : s.sound.equals("waves") ? 0.5f : 1f;
        ClientFx.play(e, s.sound.equals("wind") ? 0.4f : 0.9f, pitch);
        if (s.sound.equals("heartbeat")) {
            for (int i = 1; i < 8; i++) later(i * 18, () -> ClientFx.play(Snd.of(SoundEvents.WARDEN_HEARTBEAT), 1f, 0.9f));
        }
        if (s.sound.equals("alarm")) {
            for (int i = 1; i < 5; i++) later(i * 12, () -> ClientFx.play(Snd.of(SoundEvents.BELL_BLOCK), 0.8f, 1.6f));
        }
        if (s.sound.equals("thunder")) {
            later(40, () -> ClientFx.play(Snd.of(SoundEvents.LIGHTNING_BOLT_THUNDER), 1f, 0.7f));
            later(90, () -> ClientFx.play(Snd.of(SoundEvents.LIGHTNING_BOLT_IMPACT), 1f, 0.6f));
        }
    }

    static void finish(boolean notify) {
        Minecraft mc = Minecraft.getInstance();
        String done = id;
        boolean endBlack = !shots.isEmpty() && shots.get(shots.size() - 1).black;
        id = null;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        mc.options.hideGui = hadHideGui;
        camera = null;
        if (mc.screen instanceof CutsceneScreen) mc.setScreen(null);
        if (endBlack) ClientFx.black = true;
        if (notify && done != null) Net.toServer(new Net.CutsceneDone(done));
    }

    /** Skip what's left of the current cutscene. */
    static void skip() {
        if (id != null) finish(true);
    }

    @Nullable
    static Cutscenes.Shot current() {
        return id == null || shot >= shots.size() ? null : shots.get(shot);
    }

    static float progress(float partial) {
        Cutscenes.Shot s = current();
        return s == null ? 0 : Math.min(1f, (t + partial) / s.ticks);
    }

    static int shotTicks() {
        return t;
    }

    static float shake() {
        Cutscenes.Shot s = current();
        return s == null ? 0 : s.shake;
    }
}
