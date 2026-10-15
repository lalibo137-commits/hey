package com.airlinestory.client;

import com.airlinestory.block.InteractBlock;
import com.airlinestory.block.SeatBlock;
import com.airlinestory.entity.StoryNpc;
import com.airlinestory.story.FlagView;
import com.airlinestory.story.Locations;
import com.airlinestory.story.Objective;
import com.airlinestory.story.Quest;
import com.airlinestory.story.Quests;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/** Where to go next: compass bar, in-world waypoint, and "[Right-click] ..." prompts. */
public final class Guidance {
    private Guidance() {}

    public record Target(Vec3 pos, String label, boolean npc) {}

    @Nullable private static Target cached;
    private static long cachedAt = -1;
    private static final Set<String> doneBefore = new HashSet<>();
    private static String lastQuest = "";

    /** The first unfinished (non-optional first) objective that has somewhere to point at. */
    @Nullable
    public static Target target() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        long now = mc.level.getGameTime();
        if (now == cachedAt) return cached;
        cachedAt = now;
        cached = compute(mc);
        return cached;
    }

    @Nullable
    private static Target compute(Minecraft mc) {
        FlagView v = ClientStory.INSTANCE;
        Quest q = Quests.get(v.quest());
        if (q == null || !ClientStory.INSTANCE.active) return null;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < q.objectives.size(); i++) {
                Objective o = q.objectives.get(i);
                if (o.optional != (pass == 1) || o.isDone(v, q.id, i)) continue;
                Target t = targetFor(mc, o);
                if (t != null) return t;
            }
        }
        return null;
    }

    @Nullable
    private static Target targetFor(Minecraft mc, Objective o) {
        if (o.npcId != null) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof StoryNpc n && o.npcId.equals(n.npcId())) {
                    return new Target(n.position().add(0, n.getBbHeight() + 0.9, 0), o.npcName != null ? o.npcName : n.displayName(), true);
                }
            }
        }
        if (o.at != null) return new Target(Vec3.atCenterOf(o.at).add(0, 0.8, 0), shortText(o.text), false);
        if (o.locId != null) {
            Locations.Loc loc = Locations.get(o.locId);
            if (loc != null) {
                BlockPos p = Locations.resolve(loc, ClientStory.INSTANCE);
                return new Target(Vec3.atBottomCenterOf(p).add(0, 1.6, 0), loc.label(), false);
            }
        }
        return null;
    }

    private static String shortText(String s) {
        return s.length() > 34 ? s.substring(0, 33) + "…" : s;
    }

    // ------------------------------------------------------------------ objective feedback
    static void onSync() {
        FlagView v = ClientStory.INSTANCE;
        Quest q = Quests.get(v.quest());
        if (q == null) return;
        Set<String> now = new HashSet<>();
        for (int i = 0; i < q.objectives.size(); i++) if (q.objectives.get(i).isDone(v, q.id, i)) now.add(q.id + ":" + i);
        if (q.id.equals(lastQuest)) {
            for (String k : now) {
                if (!doneBefore.contains(k)) {
                    int i = Integer.parseInt(k.substring(k.lastIndexOf(':') + 1));
                    ClientFx.objectiveDone(q.objectives.get(i).text);
                }
            }
        }
        lastQuest = q.id;
        doneBefore.clear();
        doneBefore.addAll(now);
    }

    // ------------------------------------------------------------------ world waypoint
    public static void renderWorld(PoseStack pose, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        Target t = target();
        if (t == null || mc.player == null || CutscenePlayer.isPlaying() || ClientFx.hudHidden) return;
        Vec3 cam = camera.getPosition();
        Vec3 dir = t.pos().subtract(cam);
        double dist = dir.length();
        if (dist < (t.npc() ? 6 : 2.5)) return;
        double rd = Math.min(dist, 40);
        Vec3 rp = dir.normalize().scale(rd);
        float scale = (float) (0.0028 * rd);
        float pulse = 0.85f + 0.15f * Mth.sin((mc.player.tickCount) * 0.15f);
        pose.pushPose();
        pose.translate(rp.x, rp.y, rp.z);
        pose.mulPose(camera.rotation());
        pose.scale(-scale, -scale, scale);
        Matrix4f m = pose.last().pose();
        Font font = mc.font;
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        int alpha = (int) (255 * pulse) << 24;
        String diamond = "◆";
        pose.pushPose();
        pose.scale(2.2f, 2.2f, 2.2f);
        font.drawInBatch(diamond, -font.width(diamond) / 2f, -9, 0xFFD020 | alpha, false, pose.last().pose(), buf, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        pose.popPose();
        String label = t.label();
        String d = (int) dist + "m";
        font.drawInBatch(label, -font.width(label) / 2f, 6, 0xFFFFFFFF, false, m, buf, Font.DisplayMode.SEE_THROUGH, 0x90000000, 0xF000F0);
        font.drawInBatch(d, -font.width(d) / 2f, 17, 0xFFFFE070, false, m, buf, Font.DisplayMode.SEE_THROUGH, 0x90000000, 0xF000F0);
        buf.endBatch();
        pose.popPose();
    }

    // ------------------------------------------------------------------ compass bar
    public static void renderCompass(GuiGraphics g, int w) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Font font = mc.font;
        int cw = Math.min(220, w - 120), cx = w / 2, y = 4;
        int x0 = cx - cw / 2;
        g.fill(x0 - 2, y, x0 + cw + 2, y + 13, 0x70000000);
        g.fill(x0 - 2, y + 13, x0 + cw + 2, y + 14, 0x40FFFFFF);
        float yaw = Mth.wrapDegrees(mc.player.getYRot());
        float span = 180f;
        for (int a = -180; a < 180; a += 15) {
            float rel = Mth.wrapDegrees(a - yaw);
            if (Math.abs(rel) > span / 2) continue;
            int x = (int) (cx + rel / span * cw);
            String letter = switch (a) { case 0 -> "S"; case 90 -> "W"; case -180 -> "N"; case -90 -> "E"; default -> null; };
            if (letter != null) g.drawCenteredString(font, letter, x, y + 3, a == -180 ? 0xFFFF6060 : 0xFFFFFFFF);
            else g.fill(x, y + (a % 45 == 0 ? 4 : 6), x + 1, y + 10, 0xA0FFFFFF);
        }
        Target t = target();
        if (t != null) {
            double dx = t.pos().x - mc.player.getX(), dz = t.pos().z - mc.player.getZ();
            float tyaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float rel = Mth.wrapDegrees(tyaw - yaw);
            boolean off = Math.abs(rel) > span / 2;
            float clamped = Mth.clamp(rel, -span / 2, span / 2);
            int x = (int) (cx + clamped / span * cw);
            String mark = off ? (rel < 0 ? "◀" : "▶") : "◆";
            g.drawCenteredString(font, mark, x, y + 3, 0xFFFFD020);
            int dist = (int) Math.sqrt(dx * dx + dz * dz);
            double dy = t.pos().y - 1.6 - mc.player.getY();
            String below = t.label() + "  " + dist + "m" + (dy > 4 ? " ↑" : dy < -4 ? " ↓" : "");
            g.drawCenteredString(font, below, cx, y + 17, 0xFFFFE070);
        }
    }

    // ------------------------------------------------------------------ interaction prompt
    public static void renderPrompt(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        HitResult hit = mc.hitResult;
        String text = null;
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof StoryNpc n) {
            text = "Talk to " + n.displayName();
        } else if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            BlockState s = mc.level.getBlockState(bh.getBlockPos());
            if (s.getBlock() instanceof SeatBlock) text = mc.player.isPassenger() ? null : "Sit down";
            else if (s.getBlock() instanceof InteractBlock) text = "Use " + s.getBlock().getName().getString();
            else if (s.is(Blocks.IRON_DOOR) && ClientStory.INSTANCE.active) text = "Knock";
        }
        if (mc.player.isPassenger() && mc.player.getVehicle() instanceof com.airlinestory.entity.SeatEntity) {
            String stand = "[" + mc.options.keyShift.getTranslatedKeyMessage().getString() + "] Stand up";
            g.drawCenteredString(mc.font, stand, w / 2, h / 2 + 28, 0xFFAAAAAA);
        }
        if (text == null) return;
        String key = "[" + mc.options.keyUse.getTranslatedKeyMessage().getString() + "] ";
        String full = key + text;
        int tw = mc.font.width(full);
        g.fill(w / 2 - tw / 2 - 4, h / 2 + 12, w / 2 + tw / 2 + 4, h / 2 + 24, 0x90000000);
        g.drawString(mc.font, Component.literal(key).withStyle(s -> s.withColor(0xFFD020)), w / 2 - tw / 2, h / 2 + 14, 0xFFFFFFFF, false);
        g.drawString(mc.font, text, w / 2 - tw / 2 + mc.font.width(key), h / 2 + 14, 0xFFFFFFFF, false);
    }
}
