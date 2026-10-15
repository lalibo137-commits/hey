package com.airlinestory.client;

import com.airlinestory.net.Net;
import com.airlinestory.story.FlagView;
import com.airlinestory.story.Locations;
import com.airlinestory.story.Objective;
import com.airlinestory.story.Quest;
import com.airlinestory.story.Quests;
import com.airlinestory.story.Snd;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Client-side effects: announcements, toasts, fades, turbulence, scares and the HUD. */
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
    private static float fade, fadeTarget, fadeSpeed = 0.05f;
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
        switch (f.type()) {
            case "pa" -> { add("pa", f.text(), 0, 140 + f.text().length() * 2); chime(); }
            case "halo" -> { add("halo", f.text(), 0, 140 + f.text().length() * 2); play(Snd.of(SoundEvents.BEACON_AMBIENT), 0.6f, 1.6f); glitchTicks = 10; }
            case "hint" -> add("hint", f.text(), 0, 110 + f.text().length());
            case "whisper" -> { add("whisper", f.text(), 0, 80); play(Snd.of(SoundEvents.AMBIENT_CAVE), 0.7f, 0.6f); }
            case "rating" -> add("rating", f.text(), f.value(), 100);
            case "rated" -> add("rated", f.text(), f.value(), 120);
            case "remember" -> add("remember", f.text(), 0, 130);
            case "item" -> add("item", f.text(), f.value(), 90);
            case "clue" -> { add("clue", f.text(), 0, 140); play(Snd.of(SoundEvents.BOOK_PAGE_TURN), 1f, 0.8f); }
            case "quest" -> add("quest", f.text(), f.value(), 130);
            case "turbulence" -> turbulence = (int) f.value();
            case "fade_out" -> { fadeTarget = 1; fadeSpeed = 1f / Math.max(1, f.ticks()); }
            case "fade_in" -> { fade = 1; fadeTarget = 0; fadeSpeed = 1f / Math.max(1, f.ticks()); }
            case "black" -> { black = f.value() > 0; if (!black) { fade = 1; fadeTarget = 0; fadeSpeed = 1f / 50; loading = -1; } }
            case "loading" -> loading = (int) f.value();
            case "scare" -> { scareTicks = Math.max(6, f.ticks()); play(Snd.of(SoundEvents.ELDER_GUARDIAN_CURSE), 1f, 0.7f); }
            case "glitch" -> glitchTicks = f.ticks();
            case "chime" -> chime();
            case "caught" -> { add("caught", f.text(), 0, 90); play(Snd.of(SoundEvents.NOTE_BLOCK_BASS), 1f, 0.5f); }
            default -> {}
        }
    }

    static void onQuestChanged() {
        if (!ClientStory.INSTANCE.completed.isEmpty()) play(Snd.of(SoundEvents.PLAYER_LEVELUP), 0.4f, 1.4f);
    }

    static void objectiveDone(String text) {
        add("done", text, 0, 70);
        play(Snd.of(SoundEvents.EXPERIENCE_ORB_PICKUP), 0.6f, 1.2f);
    }

    private static void add(String type, String text, float value, int life) {
        boolean single = type.equals("pa") || type.equals("halo") || type.equals("hint") || type.equals("quest") || type.equals("done");
        if (single) messages.removeIf(m -> m.type.equals(type));
        messages.add(new Msg(type, text, value, life));
    }

    public static void chime() {
        play(Snd.of(SoundEvents.NOTE_BLOCK_CHIME), 0.8f, 1.19f);
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
        BlockPos p = mc.player.blockPosition();
        Locations.Plane a = Locations.AIR_PLANE;
        if (ticks % 90 == 0 && Math.abs(p.getY() - a.y()) < 8 && p.getX() >= a.x() - 2 && p.getX() <= a.x() + 62 && Math.abs(p.getZ() - a.z()) < 6) {
            play(Snd.of(SoundEvents.BEACON_AMBIENT), 0.35f, 0.5f);
        }
    }

    public static float[] shake(float partial) {
        float t = ticks + partial;
        float base = turbulence * 0.45f + CutscenePlayer.shake();
        float roll = shakeRoll + Mth.sin(t * 0.9f) * base * 1.6f + Mth.sin(t * 2.3f) * base * 0.7f;
        float pitch = shakePitch + Mth.sin(t * 1.7f + 1) * base * 0.8f;
        float yaw = shakeYaw + Mth.sin(t * 1.3f + 2) * base * 0.4f;
        return new float[]{yaw, pitch, roll};
    }

    // ------------------------------------------------------------------ drawing
    public static void renderTop(GuiGraphics g, int w, int h) {
        Font font = Minecraft.getInstance().font;
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

    private static float alphaOf(Msg m) {
        return Math.max(0.05f, Math.min(1f, Math.min(m.age / 8f, (m.life - m.age) / 15f)));
    }

    public static void renderHud(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientStory.INSTANCE.active || CutscenePlayer.isPlaying()) {
            renderTop(g, w, h);
            return;
        }
        Font font = mc.font;
        boolean dialogue = mc.screen instanceof DialogueScreen;
        boolean plain = mc.screen == null;
        if (plain && !hudHidden) {
            Guidance.renderCompass(g, w);
            tracker(g, font, w);
            Guidance.renderPrompt(g, w, h);
        }
        int toastY = 4;
        int leftY = hudHidden ? 6 : 100;
        int paY = dialogue ? h / 8 + 4 : 34;
        for (Msg m : messages) {
            float a = alphaOf(m);
            int alpha = VanillaGui.alpha(a);
            switch (m.type) {
                case "pa", "halo" -> {
                    boolean halo = m.type.equals("halo");
                    List<FormattedCharSequence> lines = VanillaGui.wrap(font, m.text, Math.min(280, w - 40));
                    int bw = Math.min(300, w - 20), bh = 18 + lines.size() * 10;
                    int bx = (w - bw) / 2;
                    VanillaGui.hudBox(g, bx, paY, bw, bh, halo ? 0xFFB040FF : 0xFF4FA8E0);
                    String head = halo ? "◉ HALO" : "✈ MERIDIAN ANNOUNCEMENT";
                    g.drawString(font, head, bx + 6, paY + 4, (halo ? 0xD080FF : 0x7FC8FF) | alpha, false);
                    int ly = paY + 15;
                    for (FormattedCharSequence l : lines) { g.drawString(font, l, bx + 6, ly, 0xFFFFFF | alpha, false); ly += 10; }
                    paY += bh + 4;
                }
                case "hint" -> {
                    if (dialogue) continue;
                    List<FormattedCharSequence> lines = VanillaGui.wrap(font, m.text, Math.min(300, w - 40));
                    int y = h - 66 - lines.size() * 11;
                    for (FormattedCharSequence l : lines) {
                        int lw = font.width(l);
                        g.fill(w / 2 - lw / 2 - 4, y - 2, w / 2 + lw / 2 + 4, y + 9, VanillaGui.alpha(a * 0.55f));
                        g.drawString(font, l, w / 2 - lw / 2, y, 0xFFF0C0 | alpha, false);
                        y += 11;
                    }
                }
                case "done" -> {
                    if (!plain) continue;
                    String s = "✔ " + m.text;
                    int sw = font.width(s);
                    int y = h / 2 - 40;
                    g.fill(w / 2 - sw / 2 - 6, y - 3, w / 2 + sw / 2 + 6, y + 11, VanillaGui.alpha(a * 0.6f));
                    g.drawString(font, s, w / 2 - sw / 2, y, 0x80FF80 | alpha, false);
                }
                case "caught" -> {
                    String s = m.text;
                    g.pose().pushPose();
                    g.pose().translate(w / 2f, h / 3f, 0);
                    g.pose().scale(1.5f, 1.5f, 1);
                    g.drawCenteredString(font, s, 0, 0, 0xFF5050 | alpha);
                    g.pose().popPose();
                }
                case "whisper" -> {
                    long seed = m.text.hashCode();
                    int x = (int) (Math.abs(seed) % Math.max(1, w - 160)) + 20, y = (int) (Math.abs(seed / 7) % Math.max(1, h - 100)) + 40;
                    g.drawString(font, "§o" + m.text, x, y, 0x9A9A9A | VanillaGui.alpha(a * 0.7f), false);
                }
                case "rating" -> {
                    float d = m.value;
                    String arrow = d > 0 ? "▲" : d < 0 ? "▼" : "";
                    String s = "SmileScore " + m.text + " " + arrow + String.format(Locale.ROOT, "%.2f", Math.abs(d));
                    toastY = toast(g, font, w, toastY, s, d >= 0 ? 0xFF55FF55 : 0xFFFF5555, (d >= 0 ? 0x88FF88 : 0xFF8888) | alpha);
                }
                case "rated" -> {
                    int stars = Math.round(m.value);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= 5; i++) sb.append(i <= stars ? '★' : '☆');
                    toastY = toast(g, font, w, toastY, m.text + " rated you " + sb, 0xFFFFD020, 0xFFE070 | alpha);
                }
                case "item" -> toastY = toast(g, font, w, toastY, "+ " + (m.value > 1 ? (int) m.value + "x " : "") + m.text, 0xFFAAAAAA, 0xFFFFFF | alpha);
                case "clue" -> toastY = toast(g, font, w, toastY, "✉ Clue found: " + m.text, 0xFF7FC8FF, 0xBFE4FF | alpha);
                case "remember" -> {
                    g.drawString(font, "◆ " + m.text + " will remember that.", 6, leftY, 0xFFDD55 | alpha, true);
                    leftY += 11;
                }
                case "quest" -> {
                    if (!plain) continue;
                    String ch = "CHAPTER " + (int) m.value + " — " + Quests.CHAPTERS[Math.min(Quests.CHAPTERS.length - 1, (int) m.value)].toUpperCase(Locale.ROOT);
                    int y = h / 3;
                    g.drawCenteredString(font, ch, w / 2, y - 12, 0xBBBBBB | alpha);
                    g.pose().pushPose();
                    g.pose().translate(w / 2f, y, 0);
                    g.pose().scale(2f, 2f, 1);
                    g.drawCenteredString(font, m.text, 0, 0, 0xFFE070 | alpha);
                    g.pose().popPose();
                }
                default -> {}
            }
        }
        renderTop(g, w, h);
    }

    private static int toast(GuiGraphics g, Font font, int w, int y, String s, int border, int color) {
        int sw = font.width(s) + 12;
        VanillaGui.hudBox(g, w - sw - 4, y + 14, sw, 14, border);
        g.drawString(font, s, w - sw + 2, y + 17, color, false);
        return y + 16;
    }

    private static void tracker(GuiGraphics g, Font font, int w) {
        FlagView v = ClientStory.INSTANCE;
        Quest q = Quests.get(v.quest());
        if (q == null) return;
        int x = 6, y = 6;
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < q.objectives.size(); i++) {
            Objective o = q.objectives.get(i);
            boolean done = o.isDone(v, q.id, i);
            rows.add(new String[]{(done ? "✔ " : o.optional ? "○ " : "□ ") + o.display(v) + (o.optional && !done ? " (optional)" : ""), done ? "d" : o.optional ? "o" : "n"});
        }
        int width = 168;
        int height = 26;
        List<List<FormattedCharSequence>> wrapped = new ArrayList<>();
        for (String[] r : rows) {
            List<FormattedCharSequence> l = font.split(Component.literal(r[0]), width - 10);
            wrapped.add(l);
            height += l.size() * 10;
        }
        g.fill(x - 2, y - 2, x + width, y + height, 0x60000000);
        g.fill(x - 2, y - 2, x, y + height, 0xC0FFD020);
        String ch = "CH. " + q.chapter + " — " + Quests.CHAPTERS[Math.min(q.chapter, Quests.CHAPTERS.length - 1)].toUpperCase(Locale.ROOT);
        g.drawString(font, ch, x + 4, y + 1, 0xFF9A9A9A, false);
        g.drawString(font, q.title, x + 4, y + 11, 0xFFFFE070, true);
        int yy = y + 24;
        for (int i = 0; i < wrapped.size(); i++) {
            String kind = rows.get(i)[1];
            int c = kind.equals("d") ? 0xFF6ABF6A : kind.equals("o") ? 0xFFA0A0A0 : 0xFFE8E8E8;
            for (FormattedCharSequence l : wrapped.get(i)) {
                g.drawString(font, l, x + 6, yy, c, true);
                yy += 10;
            }
        }
        if (ClientStory.INSTANCE.showsScores()) {
            float r = v.rating();
            StringBuilder stars = new StringBuilder();
            for (int i = 1; i <= 5; i++) stars.append(r >= i - 0.25f ? '★' : '☆');
            String s = String.format(Locale.ROOT, "%s %.2f", stars, r);
            int color = r >= 4 ? 0x55FF55 : r >= 3 ? 0xFFE070 : r >= 2 ? 0xFFAA00 : 0xFF5555;
            g.fill(w - font.width(s) - 12, 2, w - 2, 15, 0x60000000);
            g.drawString(font, s, w - font.width(s) - 7, 5, color, true);
        }
    }

    /** Distance and direction text for the journal. */
    public static String direction(String locId) {
        Minecraft mc = Minecraft.getInstance();
        Locations.Loc loc = Locations.get(locId);
        if (loc == null || mc.player == null) return null;
        BlockPos at = Locations.resolve(loc, ClientStory.INSTANCE);
        double dx = at.getX() + 0.5 - mc.player.getX(), dz = at.getZ() + 0.5 - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 3000) return loc.label();
        if (dist < loc.radius() + 1) return loc.label() + ": you're here";
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        String[] compass = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
        int cidx = (int) Math.floorMod(Math.round(target / 45.0), 8);
        int dy = Locations.y(loc, ClientStory.INSTANCE) - (int) mc.player.getY();
        String up = dy > 4 ? ", uphill" : dy < -4 ? ", downhill" : "";
        return loc.label() + ": " + (int) dist + "m " + compass[cidx] + up;
    }
}
