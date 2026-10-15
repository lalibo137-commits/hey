package com.airlinestory.client;

import com.airlinestory.AirlineStory;
import com.airlinestory.entity.StoryNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Story NPCs use player models and hand-made skins. A yellow "!" marks people you should talk to. */
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
        super.render(npc, yaw, partial, pose, buffers, light);
        if (npc.marker()) {
            pose.pushPose();
            pose.translate(0, 0.35 + Math.sin((npc.tickCount + partial) * 0.12) * 0.06, 0);
            renderNameTag(npc, Component.literal("!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), pose, buffers, light);
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(StoryNpc npc) {
        String skin = npc.skin();
        if (skin == null || skin.isEmpty()) skin = "traveler_0";
        return AirlineStory.id("textures/entity/npc/" + skin + (npc.smiling() ? "_smile" : "") + ".png");
    }
}
