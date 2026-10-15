package com.airlinestory.client;

import com.airlinestory.AirlineStory;
import com.airlinestory.entity.HollowEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The Unrated: passengers whose faces have been rated away. */
public class HollowRenderer extends HumanoidMobRenderer<HollowEntity, PlayerModel<HollowEntity>> {
    public HollowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(HollowEntity e) {
        return AirlineStory.id("textures/entity/hollow/hollow_" + e.variant() + ".png");
    }
}
