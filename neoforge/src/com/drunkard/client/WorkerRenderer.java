package com.drunkard.client;

import com.drunkard.BarWorker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class WorkerRenderer extends MobRenderer<BarWorker, PlayerModel<BarWorker>> {
   private final PlayerModel<BarWorker> wide = (PlayerModel<BarWorker>)this.getModel();
   private final PlayerModel<BarWorker> slim;

   public WorkerRenderer(Context context) {
      super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.slim = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
      this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(BarWorker worker) {
      return PatronSkins.get(worker.skin());
   }

   protected void scale(BarWorker worker, PoseStack pose, float partial) {
      pose.scale(0.9375F, 0.9375F, 0.9375F);
   }

   protected boolean shouldShowName(BarWorker worker) {
      return false;
   }

   public void render(BarWorker worker, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      this.model = worker.slim() ? this.slim : this.wide;
      ((PlayerModel)this.model).rightArmPose = worker.getMainHandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
      super.render(worker, yaw, partial, pose, buffers, light);
      if (!worker.speech().isEmpty() && !(this.entityRenderDispatcher.distanceToSqr(worker) > 1024.0)) {
         pose.pushPose();
         pose.translate(0.0F, worker.getBbHeight() + 0.55F, 0.0F);
         faceCamera(pose);
         pose.scale(-0.025F, -0.025F, 0.025F);
         Font font = this.getFont();
         List<FormattedCharSequence> lines = font.split(Component.literal(worker.speech()), 120);
         int back = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;

         for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            float x = (float)(-font.width(line)) / 2.0F;
            float ly = (float)(i - (lines.size() - 1)) * 10.0F;
            font.drawInBatch(line, x, ly, 553648127, false, pose.last().pose(), buffers, DisplayMode.SEE_THROUGH, back, light);
            font.drawInBatch(line, x, ly, -1, false, pose.last().pose(), buffers, DisplayMode.NORMAL, 0, light);
         }

         pose.popPose();
      }
   }

   // 1.20.5+ camera orientation is the 1.20.1 one turned 180 degrees about Y; undo that so labels and icons face the player as before.
   private void faceCamera(PoseStack pose) {
      pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
      pose.mulPose(Axis.YP.rotationDegrees(180.0F));
   }
}
