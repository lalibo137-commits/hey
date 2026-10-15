package com.airlinestory.client;

import com.airlinestory.net.Net;
import com.airlinestory.story.FlagView;
import com.airlinestory.story.Locations;
import com.airlinestory.story.Objective;
import com.airlinestory.story.Quest;
import com.airlinestory.story.Quests;
import com.airlinestory.story.Snd;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** Client-side effects: announcements, toasts, fades, turbulence, scares and the HUD tracker. */
public final class ClientFx {
    private ClientFx() {}

    private static final class Msg {
        final String type, text;
        final float value;
        int age;
        final int life;
        Msg(String type, String text, float value, int life) { this.type = type; this.text = text; this.value = value; this.life = life; }
    }

    private static final List<Msg> messages = new ArrayList<>();
    private static final RandomSource rand = RandomSource.create();
    public static int turbulence;
    public static boolean black;
    private static float fade; // 0 clear .. 1 black
    private static float fadeTarget;
    private static float fadeSpeed = 0.05f;
    private static int scareTicks, glitchTicks;
    private static int loading = -1;
    private static int ticks;
    private static float shakeRoll, shakePitch, shakeYaw;
    public static boolean hudHidden;

    public static void reset() {
        messages.clear();
        turbulence = 0;
        black = false;
        fade = fadeTarget = 0;
        scareTicks = glitchTicks = 0;
        loading = -1;
    }

    static void handle(Net.Fx f) {
        Minecraft mc = Minecraft.getInstance();
        switch (f.type()) {
            case "pa" -> { add("pa", f.text(), 0, 120 + f.text().length() * 2); chime(); }
            case "halo" -> { add("halo", f.text(), 0, 120 + f.text().length() * 2); play(Snd.of(SoundEvents.BEACON_AMBIENT), 0.6f, 1.6f); glitchTicks = 10; }
            case "hint" -> add("hint", f.text(), 0, 100 + f.text().length());
            case "whisper" -> { add("whisper", f.text(), 0, 70); play(Snd.of(SoundEvents.AMBIENT_CAVE), 0.7f, 0.6f); }
            case "rating" -> add("rating", f.text(), f.value(), 90);
            case "remember" -> add("remember", f.text(), 0, 110);
            case "item" -> add("item", f.text(), f.value(), 80);
            case "quest" -> add("quest", f.text(), f.value(), 110);
            case "turbulence" -> turbulence = (int) f.value();
            case "fade_out" -> { fadeTarget = 1; fadeSpeed = 1f / Math.max(1, f.ticks()); }
            case "fade_in" -> { fade = 1; fadeTarget = 0; fadeSpeed = 1f / Math.max(1, f.ticks()); }
            case "black" -> { black = f.value() > 0; if (!black) { fade = 1; fadeTarget = 0; fadeSpeed = 1f / 50; loading = -1; } }
            case "loading" -> loading = (int) f.value();
            case "scare" -> { scareTicks = Math.max(6, f.ticks()); play(Snd.of(SoundEvents.ELDER_GUARDIAN_CURSE), 1f, 0.7f); }
            case "glitch" -> glitchTicks = f.ticks();
            case "chime" -> chime();
            default -> {}
        }
        if (mc.player == null) messages.clear();
    }

    static void onQuestChanged() {
        Quest q = Quests.get(ClientStory.INSTANCE.quest);
        if (q != null && ClientStory.INSTANCE.completed.size() > 0) play(Snd.of(SoundEvents.PLAYER_LEVELUP), 0.5f, 1.4f);
    }

    private static void add(String type, String text, float value, int life) {
        messages.removeIf(m -> m.type.equals(type) && (type.equals("pa") || type.equals("halo") || type.equals("hint") || type.equals("quest")));
        messages.add(new Msg(type, text, value, life));
    }

    public static void chime() {
        play(Snd.of(SoundEvents.NOTE_BLOCK_CHIME), 0.8f, 1.19f);
        Minecraft.getInstance().execute(() -> {});
        CutscenePlayer.later(6, () -> play(Snd.of(SoundEvents.NOTE_BLOCK_CHIME), 0.8f, 0.94f));
    }

    public static void play(SoundEvent s, float vol, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    // ------------------------------------------------------------------ ticking
    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        for (Msg m : messages) m.age++;
        messages.removeIf(m -> m.age > m.life);
        if (fade < fadeTarget) fade = Math.min(fadeTarget, fade + fadeSpeed);
        else if (fade > fadeTarget) fade = Math.max(fadeTarget, fade - fadeSpeed);
        if (scareTicks > 0) scareTicks--;
        if (glitchTicks > 0) glitchTicks--;
        if (mc.player == null) return;
        if (turbulence > 0) {
            if (rand.nextInt(Math.max(2, 50 - turbulence * 14)) == 0) {
                play(Snd.of(SoundEvents.MINECART_RIDING), 0.25f * turbulence, 0.5f);
                shakeRoll += (rand.nextFloat() - 0.5f) * 6f * turbulence;
                shakePitch += (rand.nextFloat() - 0.5f) * 4f * turbulence;
            }
            if (rand.nextInt(120 / turbulence) == 0) play(Snd.of(SoundEvents.CHAIN_FALL), 0.4f, 0.6f);
        }
        shakeRoll *= 0.85f;
        shakePitch *= 0.85f;
        shakeYaw *= 0.85f;
        // the cabin hums
        BlockPos p = mc.player.blockPosition();
        Locations.Plane a = Locations.AIR_PLANE;
        if (ticks % 90 == 0 && Math.abs(p.getY() - a.y()) < 8 && p.getX() >= a.x() - 2 && p.getX() <= a.x() + 62 && Math.abs(p.getZ() - a.z()) < 6) {
            play(Snd.of(SoundEvents.BEACON_AMBIENT), 0.35f, 0.5f);
        }
    }

    /** Extra camera roll/pitch, used by the ComputeCameraAngles hook. */
    public static float[] shake(float partial) {
        float t = ticks + partial;
        float base = turbulence * 0.45f + CutscenePlayer.shake();
        float roll = shakeRoll + Mth.sin(t * 0.9f) * base * 1.6f + Mth.sin(t * 2.3f) * base * 0.7f;
        float pitch = shakePitch + Mth.sin(t * 1.7f + 1) * base * 0.8f;
        float yaw = shakeYaw + Mth.sin(t * 1.3f + 2) * base * 0.4f;
        return new float[]{yaw, pitch, roll};
    }

    // ------------------------------------------------------------------ drawing
    /** Drawn above everything (also from screens). */
    public static void renderTop(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        if (black) {
            g.fill(0, 0, w, h, 0xFF000000);
            if (loading >= 0) g.drawCenteredString(font, "...", w / 2, h / 2, 0xFF666666);
        } else if (fade > 0.001f) {
            g.fill(0, 0, w, h, VanillaGui.alpha(fade));
        }
        if (glitchTicks > 0) {
            for (int i = 0; i < 14; i++) {
                int y = rand.nextInt(h);
                g.fill(0, y, w, y + 1 + rand.nextInt(3), (rand.nextBoolean() ? 0x40FF0040 : 0x4000FFFF));
            }
        }
        if (scareTicks > 0) {
            g.fill(0, 0, w, h, 0xE0000000);
            drawFace(g, w / 2, h / 2, Math.min(w, h) / 2);
        }
    }

    private static void drawFace(GuiGraphics g, int cx, int cy, int size) {
        int s = size / 8;
        int x0 = cx - s * 4, y0 = cy - s * 4;
        g.fill(x0, y0, x0 + s * 8, y0 + s * 8, 0xFFD8CFC4);
        for (int i = 0; i < 18; i++) {
            int px = x0 + rand.nextInt(8) * s, py = y0 + rand.nextInt(8) * s;
            g.fill(px, py, px + s, py + s, 0xFFCBC1B5);
        }
        g.fill(x0, y0, x0 + s * 8, y0 + s, 0xFF2B2B2B);
        g.fill(x0 + s * 2, y0 + s * 3, x0 + s * 3, y0 + s * 4, 0xFF000000);
        g.fill(x0 + s * 5, y0 + s * 3, x0 + s * 6, y0 + s * 4, 0xFF000000);
        g.fill(x0 + s, y0 + s * 5, x0 + s * 7, y0 + s * 6, 0xFF000000);
        g.fill(x0 + s * 2, y0 + s * 6, x0 + s * 6, y0 + s * 7, 0xFF3A0000);
    }

    /** The HUD: objective tracker, rating, announcements and toasts. */
    public static void renderHud(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientStory.INSTANCE.active || CutscenePlayer.isPlaying()) {
            renderTop(g, w, h);
            return;
        }
        Font font = mc.font;
        if (!hudHidden && mc.screen == null) tracker(g, font, w);
        int toastY = 4;
        int remY = hudHidden ? 6 : 58;
        for (Msg m : messages) {
            float a = Math.min(1f, Math.min(m.age / 8f, (m.life - m.age) / 15f));
            int alpha = VanillaGui.alpha(Math.max(0.05f, a));
            switch (m.type) {
                case "pa", "halo" -> {
                    boolean halo = m.type.equals("halo");
                    List<FormattedCharSequence> lines = VanillaGui.wrap(font, m.text, Math.min(300, w - 40));
                    int bw = Math.min(320, w - 20), bh = 18 + lines.size() * 10;
                    int bx = (w - bw) / 2, by = 22;
                    VanillaGui.hudBox(g, bx, by, bw, bh, halo ? 0xFFB040FF : 0xFF4FA8E0);
                    String head = halo ? "◉ HALO" : "✈ MERIDIAN CABIN ANNOUNCEMENT";
                    g.drawString(font, head, bx + 6, by + 4, (halo ? 0xD080FF : 0x7FC8FF) | alpha, false);
                    int ly = by + 15;
                    for (FormattedCharSequence l : lines) { g.drawString(font, l, bx + 6, ly, 0xFFFFFF | alpha, false); ly += 10; }
                }
                case "hint" -> {
                    List<FormattedCharSequence> lines = VanillaGui.wrap(font, m.text, Math.min(280, w - 40));
                    int y = h - 70 - lines.size() * 10;
                    for (FormattedCharSequence l : lines) {
                        int lw = font.width(l);
                        g.fill(w / 2 - lw / 2 - 3, y - 1, w / 2 + lw / 2 + 3, y + 9, 0x80000000);
                        g.drawString(font, l, w / 2 - lw / 2, y, 0xFFFFE0 | alpha, false);
                        y += 10;
                    }
                }
                case "whisper" -> {
                    long seed = m.text.hashCode();
                    int x = (int) (Math.abs(seed) % Math.max(1, w - 160)) + 20, y = (int) (Math.abs(seed / 7) % Math.max(1, h - 100)) + 40;
                    g.drawString(font, "§o" + m.text, x, y, 0x9A9A9A | VanillaGui.alpha(a * 0.7f), false);
                }
                case "rating" -> {
                    float d = m.value;
                    String arrow = d > 0 ? "▲" : d < 0 ? "▼" : "";
                    String s = "SmileScore " + m.text + " " + arrow + String.format(java.util.Locale.ROOT, "%.2f", Math.abs(d));
                    int sw = font.width(s) + 12;
                    VanillaGui.hudBox(g, w - sw - 4, toastY, sw, 14, d >= 0 ? 0xFF55FF55 : 0xFFFF5555);
                    g.drawString(font, s, w - sw + 2, toastY + 3, (d >= 0 ? 0x88FF88 : 0xFF8888) | alpha, false);
                    toastY += 16;
                }
                case "item" -> {
                    String s = "+ " + (m.value > 1 ? (int) m.value + "x " : "") + m.text;
                    int sw = font.width(s) + 10;
                    VanillaGui.hudBox(g, w - sw - 4, toastY, sw, 14, 0xFFAAAAAA);
                    g.drawString(font, s, w - sw + 1, toastY + 3, 0xFFFFFF | alpha, false);
                    toastY += 16;
                }
                case "remember" -> {
                    String s = "◆ " + m.text + " will remember that.";
                    g.drawString(font, s, 6, remY, 0xFFDD55 | alpha, true);
                    remY += 11;
                }
                case "quest" -> {
                    String ch = "Chapter " + (int) m.value + ": " + Quests.CHAPTERS[Math.min(Quests.CHAPTERS.length - 1, (int) m.value)];
                    g.pose().pushPose();
                    g.pose().translate(w / 2f, h / 4f, 0);
                    g.pose().scale(1.6f, 1.6f, 1);
                    g.drawCenteredString(font, m.text, 0, 0, 0xFFFF55 | alpha);
                    g.pose().popPose();
                    g.drawCenteredString(font, ch, w / 2, h / 4 + 18, 0xBBBBBB | alpha);
                    g.drawCenteredString(font, "Press [" + ClientModEvents.JOURNAL.getTranslatedKeyMessage().getString() + "] for your journal", w / 2, h / 4 + 30, 0x888888 | alpha);
                }
                default -> {}
            }
        }
        renderTop(g, w, h);
    }

    private static void tracker(GuiGraphics g, Font font, int w) {
        FlagView v = ClientStory.INSTANCE;
        Quest q = Quests.get(v.quest());
        if (q == null) return;
        int x = 6, y = 6;
        g.drawString(font, q.title, x, y, 0xFFFF55, true);
        y += 11;
        int shown = 0;
        Objective first = null;
        for (int i = 0; i < q.objectives.size() && shown < 3; i++) {
            Objective o = q.objectives.get(i);
            if (o.isDone(v, q.id, i)) continue;
            if (first == null) first = o;
            for (FormattedCharSequence l : font.split(net.minecraft.network.chat.Component.literal("• " + o.display(v)), 200)) {
                g.drawString(font, l, x, y, 0xE0E0E0, true);
                y += 10;
            }
            shown++;
        }
        if (first != null && first.locId != null) {
            String dir = direction(first.locId);
            if (dir != null) {
                g.drawString(font, dir, x, y, 0x7FC8FF, true);
            }
        }
        String stars = String.format(java.util.Locale.ROOT, "★ %.1f", v.rating());
        int color = v.rating() >= 4 ? 0x55FF55 : v.rating() >= 3 ? 0xFFFF55 : v.rating() >= 2 ? 0xFFAA00 : 0xFF5555;
        if (!v.has("ending_free")) g.drawString(font, stars, w - font.width(stars) - 6, 6 + countToasts() * 16, color, true);
    }

    private static int countToasts() {
        int n = 0;
        for (Msg m : messages) if (m.type.equals("rating") || m.type.equals("item")) n++;
        return n;
    }

    /** "Gate B7: 34m north-east" style text for the journal and HUD. */
    public static String direction(String locId) {
        Minecraft mc = Minecraft.getInstance();
        Locations.Loc loc = Locations.get(locId);
        if (loc == null || mc.player == null) return null;
        BlockPos at = Locations.resolve(loc, ClientStory.INSTANCE);
        double dx = at.getX() + 0.5 - mc.player.getX(), dz = at.getZ() + 0.5 - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 2000) return null;
        if (dist < loc.radius() + 1) return "◎ " + loc.label() + ": here";
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = Mth.wrapDegrees(target - mc.player.getYRot());
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        int idx = (int) Math.floorMod(Math.round(rel / 45.0), 8);
        String[] compass = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
        int cidx = (int) Math.floorMod(Math.round(target / 45.0), 8);
        int dy = Locations.y(loc, ClientStory.INSTANCE) - (int) mc.player.getY();
        String up = dy > 4 ? " (up)" : dy < -4 ? " (down)" : "";
        return arrows[idx] + " " + loc.label() + ": " + (int) dist + "m " + compass[cidx] + up;
    }

    @SuppressWarnings("unused")
    private static String gray(String s) {
        return ChatFormatting.GRAY + s;
    }
}
