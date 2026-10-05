package com.drunkard.client;

import com.drunkard.BottleEntity;
import com.drunkard.PlacedBottleBlock;
import com.drunkard.WineData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;

public class BottleRenderer implements BlockEntityRenderer<BottleEntity> {
   public BottleRenderer(Context context) {
   }

   public void render(BottleEntity bottle, float partial, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
      boolean mug = WineData.variety(bottle.bottle).contains("lager")
         || WineData.variety(bottle.bottle).equals("pale_ale")
         || WineData.variety(bottle.bottle).equals("wheat_beer");
      pose.pushPose();
      pose.translate(0.5, mug ? 0.28125 : 0.375, 0.5);
      pose.mulPose(Axis.YP.rotationDegrees(-((Direction)bottle.getBlockState().getValue(PlacedBottleBlock.FACING)).toYRot()));
      pose.scale(0.75F, 0.75F, 0.75F);
      Minecraft.getInstance().getItemRenderer().renderStatic(bottle.bottle, ItemDisplayContext.NONE, light, overlay, pose, buffer, bottle.getLevel(), 0);
      pose.popPose();
   }
}
