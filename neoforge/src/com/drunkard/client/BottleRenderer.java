package com.drunkard.client;

import com.drunkard.BottleEntity;
import com.drunkard.Drunkard;
import com.drunkard.PlacedBottleBlock;
import com.drunkard.WineData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;

public class BottleRenderer implements BlockEntityRenderer<BottleEntity> {
   public BottleRenderer(Context context) {
   }

   public void render(BottleEntity bottle, float partial, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
      String variety = WineData.variety(bottle.bottle);
      boolean mug = variety.contains("lager")
         || variety.equals("pale_ale")
         || variety.equals("wheat_beer")
         || bottle.bottle.is((Item)Drunkard.BEER_MUG.get());
      float yaw = -((Direction)bottle.getBlockState().getValue(PlacedBottleBlock.FACING)).toYRot();
      if (!bottle.bottle.is(Items.GLASS_BOTTLE)) {
         pose.pushPose();
         pose.translate(0.5, mug ? 0.28125 : 0.375, 0.5);
         pose.mulPose(Axis.YP.rotationDegrees(yaw));
         pose.scale(0.75F, 0.75F, 0.75F);
         Minecraft.getInstance().getItemRenderer().renderStatic(bottle.bottle, ItemDisplayContext.NONE, light, overlay, pose, buffer, bottle.getLevel(), 0);
         pose.popPose();
      } else {
         for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.translate(0.5, 0.3, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(yaw + 45.0F + (float)(i * 90)));
            pose.scale(0.6F, 0.6F, 0.6F);
            Minecraft.getInstance().getItemRenderer().renderStatic(bottle.bottle, ItemDisplayContext.FIXED, light, overlay, pose, buffer, bottle.getLevel(), 0);
            pose.popPose();
         }
      }
   }
}
