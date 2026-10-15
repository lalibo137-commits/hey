package com.airlinestory.client;

import com.airlinestory.AirlineStory;
import com.airlinestory.entity.StoryNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Locale;

/**
 * Story NPCs: player models and hand-made skins. Above their heads: a bobbing objective icon, a speech
 * bubble when they say something, and (in Meridian's world) their SmileScore, the way everyone sees it.
 */
public class NpcRenderer extends HumanoidMobRenderer<StoryNpc, PlayerModel<StoryNpc>> {
    private final PlayerModel<StoryNpc> wide;
    private final PlayerModel<StoryNpc> slimModel;

    public NpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wide = this.model;
        this.slimModel = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(StoryNpc npc, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        this.model = npc.slim() ? slimModel : wide;
        StoryNpc.Activity act = npc.activityNow();
        HumanoidModel.ArmPose arm = switch (act) {
            case PHONE, READING, KNITTING, WORKING -> HumanoidModel.ArmPose.CROSSBOW_HOLD;
            case COFFEE -> HumanoidModel.ArmPose.ITEM;
            default -> npc.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        };
        if (npc.getMainArm() == HumanoidArm.RIGHT) { model.rightArmPose = arm; model.leftArmPose = HumanoidModel.ArmPose.EMPTY; }
        else { model.leftArmPose = arm; model.rightArmPose = HumanoidModel.ArmPose.EMPTY; }
        super.render(npc, yaw, partial, pose, buffers, light);
        overlays(npc, partial, pose, buffers, light);
    }

    @Override
    protected boolean shouldShowName(StoryNpc npc) {
        return false;
    }

    private void overlays(StoryNpc npc, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || CutscenePlayer.isPlaying() && !npc.smiling()) {
            if (mc.player == null) return;
        }
        double dist = entityRenderDispatcher.distanceToSqr(npc);
        float top = npc.getBbHeight() + 0.35f;
        boolean looking = mc.crosshairPickEntity == npc;
        // objective icon
        if (npc.marker() > 0 && !CutscenePlayer.isPlaying()) {
            float bob = (float) Math.sin((npc.tickCount + partial) * 0.12) * 0.08f;
            String icon = npc.marker() == 1 ? "!" : "…";
            int color = npc.marker() == 1 ? 0xFFFFD020 : 0xFFB0B0B0;
            float s = npc.marker() == 1 ? 2.6f : 1.6f;
            label(pose, buffers, Component.literal(icon), top + 0.45f + bob, color, s, true, light, 0);
        }
        if (dist > 20 * 20 || CutscenePlayer.isPlaying()) return;
        float y = top;
        // speech bubble
        String bark = npc.bark();
        if (!bark.isEmpty()) {
            Font font = getFont();
            List<FormattedCharSequence> lines = font.split(Component.literal(bark), 140);
            for (int i = lines.size() - 1; i >= 0; i--) {
                labelSeq(pose, buffers, lines.get(i), y, 0xFFFFFFFF, 1.0f, light, 0xA0000000);
                y += 0.27f;
            }
            y += 0.05f;
        }
        // name and score (close up, or when looked at)
        if (looking || dist < 6 * 6) {
            float score = npc.smileScore();
            String name = npc.displayName();
            if (score >= 0 && ClientStory.INSTANCE.showsScores()) {
                int c = score >= 4.5f ? 0xFF7CFC7C : score >= 3.5f ? 0xFFFFE066 : score >= 2.5f ? 0xFFFFA040 : 0xFFFF5050;
                label(pose, buffers, Component.literal(String.format(Locale.ROOT, "★ %.1f", score)), y, c, 0.9f, false, light, 0x60000000);
                y += 0.24f;
            }
            label(pose, buffers, Component.literal(name), y, looking ? 0xFFFFFFFF : 0xFFCCCCCC, 0.9f, false, light, 0x60000000);
        }
    }

    private void label(PoseStack pose, MultiBufferSource buffers, Component text, float y, int color, float scale, boolean seeThrough, int light, int bg) {
        labelSeq(pose, buffers, text.getVisualOrderText(), y, color, scale, light, bg, seeThrough);
    }

    private void labelSeq(PoseStack pose, MultiBufferSource buffers, FormattedCharSequence text, float y, int color, float scale, int light, int bg) {
        labelSeq(pose, buffers, text, y, color, scale, light, bg, false);
    }

    private void labelSeq(PoseStack pose, MultiBufferSource buffers, FormattedCharSequence text, float y, int color, float scale, int light, int bg, boolean seeThrough) {
        pose.pushPose();
        pose.translate(0, y, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float s = 0.025f * scale;
        pose.scale(-s, -s, s);
        Matrix4f m = pose.last().pose();
        Font font = getFont();
        float x = -font.width(text) / 2f;
        font.drawInBatch(text, x, 0, color, false, m, buffers, seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, bg, 0xF000F0);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(StoryNpc npc) {
        String skin = npc.skin();
        if (skin == null || skin.isEmpty()) skin = "traveler_0";
        return AirlineStory.id("textures/entity/npc/" + skin + (npc.smiling() ? "_smile" : "") + ".png");
    }
}
