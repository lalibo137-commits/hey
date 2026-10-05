package com.drunkard.client;

import com.drunkard.GlassShelfBlock;
import com.drunkard.GlassShelfEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class GlassShelfRenderer implements BlockEntityRenderer<GlassShelfEntity> {
   private static final float[] TIER = new float[]{0.0625F, 0.375F, 0.6875F};

   public GlassShelfRenderer(Context context) {
   }

   public void render(GlassShelfEntity shelf, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
      BlockState state = shelf.getBlockState();
      Direction front = (Direction)state.getValue(GlassShelfBlock.FACING);
      Direction right = GlassShelfBlock.right(state);
      float scale = 0.4F;

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = (ItemStack)shelf.glasses.get(slot);
         if (!stack.isEmpty()) {
            int tier = slot / 3;
            int column = slot % 3;
            int copies = Math.min(2, stack.getCount());

            for (int k = 0; k < copies; k++) {
               float across = (float)(column - 1) / 3.0F + (copies == 2 ? (k == 0 ? -0.07F : 0.07F) : 0.0F);
               float depth = -0.22F;
               pose.pushPose();
               pose.translate(
                  0.5 + (double)((float)right.getStepX() * across) + (double)((float)front.getStepX() * depth),
                  (double)(TIER[tier] + scale * 0.5F - scale * 2.0F / 16.0F),
                  0.5 + (double)((float)right.getStepZ() * across) + (double)((float)front.getStepZ() * depth)
               );
               pose.mulPose(Axis.YP.rotationDegrees(180.0F - front.toYRot()));
               pose.scale(scale, scale, scale);
               Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, overlay, pose, buffers, shelf.getLevel(), slot);
               pose.popPose();
            }
         }
      }
   }
}
