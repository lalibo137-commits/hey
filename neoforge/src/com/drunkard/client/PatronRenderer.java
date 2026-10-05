package com.drunkard.client;

import com.drunkard.BarPatron;
import com.drunkard.BarTalk;
import com.drunkard.CigarItem;
import com.drunkard.Drunkard;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PatronRenderer extends MobRenderer<BarPatron, PatronRenderer.Model> {
   private final PatronRenderer.Model wide = (PatronRenderer.Model)this.getModel();
   private final PatronRenderer.Model slim;
   static final int SPEECH_WIDTH = 120;

   public PatronRenderer(Context context) {
      super(context, new PatronRenderer.Model(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.slim = new PatronRenderer.Model(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
      this.addLayer(new PatronRenderer.HeldItems(this, context));
      this.addLayer(new PatronRenderer.DrunkFace(this));
   }

   public ResourceLocation getTextureLocation(BarPatron patron) {
      return PatronSkins.get(patron.skin());
   }

   protected void scale(BarPatron patron, PoseStack pose, float partial) {
      pose.scale(0.9375F, 0.9375F, 0.9375F);
   }

   protected boolean shouldShowName(BarPatron patron) {
      return false;
   }

   public Vec3 getRenderOffset(BarPatron patron, float partial) {
      return patron.crouching() && !patron.isPassenger() ? new Vec3(0.0, -0.125, 0.0) : super.getRenderOffset(patron, partial);
   }

   static float lift(BarPatron patron, float partial) {
      float t = patron.sipAge(partial);
      if (!(t < 0.0F) && !(t >= 40.0F)) {
         float up = Mth.clamp(t / 8.0F, 0.0F, 1.0F);
         float down = Mth.clamp((40.0F - t) / 10.0F, 0.0F, 1.0F);
         float v = Math.min(up, down);
         return v * v * (3.0F - 2.0F * v);
      } else {
         return 0.0F;
      }
   }

   public void render(BarPatron patron, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      this.model = patron.slim() ? this.slim : this.wide;
      ((PatronRenderer.Model)this.getModel()).crouching = patron.crouching() && !patron.isPassenger();
      ((PatronRenderer.Model)this.getModel()).partial = partial;
      super.render(patron, yaw, partial, pose, buffers, light);
      if (!(this.entityRenderDispatcher.distanceToSqr(patron) > 1024.0)) {
         float top = patron.getBbHeight() + 0.55F;
         float bob = Mth.sin(((float)patron.tickCount + partial) * 0.12F) * 0.06F;
         if (patron.unserved()) {
            int colour = patron.impatience() == 2 ? -46534 : (patron.impatience() == 1 ? -26066 : -11205);
            boolean ordered = patron.product() != null;
            this.label(
               Component.literal("?").withStyle(ChatFormatting.BOLD), pose, buffers, light, top + 0.25F + bob, 2.6F, colour, false, ordered ? -0.2F : 0.0F
            );
            if (ordered) {
               pose.pushPose();
               pose.translate(0.0F, top - 0.02F + bob, 0.0F);
               pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
               pose.translate(0.24F, 0.0F, 0.0F);
               pose.scale(0.58F, 0.58F, 0.58F);
               Minecraft.getInstance()
                  .getItemRenderer()
                  .renderStatic(
                     patron.product().icon(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, patron.level(), patron.getId()
                  );
               pose.popPose();
            }

            top += 0.7F;
         } else if (patron.state() == BarPatron.State.AWAIT_SHOT && patron.product() != null) {
            pose.pushPose();
            pose.translate(0.0F, top + 0.2F + bob, 0.0F);
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            pose.scale(0.55F, 0.55F, 0.55F);
            Minecraft.getInstance()
               .getItemRenderer()
               .renderStatic(
                  new ItemStack((ItemLike)Drunkard.SHOT_GLASS.get()),
                  ItemDisplayContext.FIXED,
                  light,
                  OverlayTexture.NO_OVERLAY,
                  pose,
                  buffers,
                  patron.level(),
                  patron.getId()
               );
            pose.popPose();
            top += 0.6F;
         }

         if (!patron.speech().isEmpty()) {
            int below = 0;

            for (BarPatron other : patron.level()
               .getEntitiesOfClass(BarPatron.class, patron.getBoundingBox().inflate(3.0, 1.0, 3.0), o -> o != patron && !o.speech().isEmpty())) {
               if (other.getId() < patron.getId()) {
                  below++;
               }
            }

            int rows = this.getFont().split(Component.literal(patron.speech()), 120).size();
            this.label(Component.literal(patron.speech()), pose, buffers, light, top + (float)below * 0.3F * (float)rows, 1.0F, -1, true, 0.0F);
         }
      }
   }

   private void label(Component text, PoseStack pose, MultiBufferSource buffers, int light, float y, float scale, int color, boolean background, float dx) {
      pose.pushPose();
      pose.translate(0.0F, y, 0.0F);
      pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
      pose.translate(dx, 0.0F, 0.0F);
      pose.scale(-0.025F * scale, -0.025F * scale, 0.025F * scale);
      Matrix4f matrix = pose.last().pose();
      Font font = this.getFont();
      List<FormattedCharSequence> lines = background ? font.split(text, 120) : List.of(text.getVisualOrderText());
      int rows = lines.size();

      for (int i = 0; i < rows; i++) {
         FormattedCharSequence line = lines.get(i);
         float x = (float)(-font.width(line)) / 2.0F;
         float ly = (float)(i - (rows - 1)) * 10.0F;
         if (background) {
            int back = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
            font.drawInBatch(line, x, ly, 553648127, false, matrix, buffers, DisplayMode.SEE_THROUGH, back, light);
            font.drawInBatch(line, x, ly, color, false, matrix, buffers, DisplayMode.NORMAL, 0, light);
         } else {
            font.drawInBatch(line, x, ly, color, true, matrix, buffers, DisplayMode.NORMAL, 0, light);
         }
      }

      pose.popPose();
   }

   static class DrunkFace extends RenderLayer<BarPatron, PatronRenderer.Model> {
      private final ModelPart face;

      DrunkFace(RenderLayerParent<BarPatron, PatronRenderer.Model> parent) {
         super(parent);
         MeshDefinition mesh = new MeshDefinition();
         mesh.getRoot().addOrReplaceChild("face", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
         this.face = LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("face");
      }

      public void render(
         PoseStack pose,
         MultiBufferSource buffers,
         int light,
         BarPatron patron,
         float limbSwing,
         float limbAmount,
         float partial,
         float age,
         float yaw,
         float pitch
      ) {
         if (!patron.isInvisible() && !(patron.drunk() < 4.5F)) {
            this.face.copyFrom(((PatronRenderer.Model)this.getParentModel()).head);
            this.face
               .render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(DrunkHeadTextures.get(PatronSkins.get(patron.skin())))), light, OverlayTexture.NO_OVERLAY);
         }
      }
   }

   static class HeldItems extends ItemInHandLayer<BarPatron, PatronRenderer.Model> {
      HeldItems(RenderLayerParent<BarPatron, PatronRenderer.Model> parent, Context context) {
         super(parent, context.getItemInHandRenderer());
      }

      protected void renderArmWithItem(
         LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack pose, MultiBufferSource buffers, int light
      ) {
         if (entity instanceof BarPatron patron && arm == HumanoidArm.RIGHT) {
            PatronRenderer.Model model = (PatronRenderer.Model)this.getParentModel();
            float lift = PatronRenderer.lift(patron, model.partial);
            if (stack.getItem() instanceof CigarItem) {
               PoseStack mouth = new PoseStack();
               model.head.translateAndRotate(mouth);
               mouth.translate(0.055, -0.125, -0.469375);
               CigarMouthLayer.upright(mouth, stack);
               if (patron.cigarInMouth()) {
                  pose.pushPose();
                  model.head.translateAndRotate(pose);
                  pose.translate(0.055, -0.125, -0.469375);
                  CigarMouthLayer.upright(pose, stack);
                  CigarEffects.captureRenderedTip(entity, stack, pose, true);
                  Minecraft.getInstance()
                     .getItemRenderer()
                     .renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), 0);
                  pose.popPose();
                  return;
               }

               PoseStack held = new PoseStack();
               model.translateToHand(arm, held);
               held.translate(0.0625, 0.65625, -0.09375);
               held.mulPose(Axis.XP.rotationDegrees(18.0F));
               CigarMouthLayer.upright(held, stack);
               Vector3f p = held.last().pose().getTranslation(new Vector3f()).lerp(mouth.last().pose().getTranslation(new Vector3f()), lift);
               Quaternionf r = held.last()
                  .pose()
                  .getUnnormalizedRotation(new Quaternionf())
                  .slerp(mouth.last().pose().getUnnormalizedRotation(new Quaternionf()), lift);
               pose.pushPose();
               pose.translate(p.x, p.y, p.z);
               pose.mulPose(r);
               CigarEffects.captureRenderedTip(entity, stack, pose, false);
               Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), 0);
               pose.popPose();
               return;
            }

            if (!(lift <= 0.0F) && DrinkPose.drinkable(stack)) {
               DrinkPose shape = DrinkPose.of(stack);
               PoseStack held = new PoseStack();
               model.translateToHand(arm, held);
               held.mulPose(Axis.XP.rotationDegrees(-90.0F));
               held.mulPose(Axis.YP.rotationDegrees(180.0F));
               held.translate(0.0625F, 0.1875F, -0.5625F);
               held.mulPose(Axis.ZP.rotationDegrees(-15.0F));
               PoseStack sipping = new PoseStack();
               model.head.translateAndRotate(sipping);
               sipping.translate(0.0F, -0.125F, -0.253125F);
               sipping.mulPose(Axis.XP.rotationDegrees(shape.thirdTilt()));
               sipping.translate(0.0F, -shape.lipY() * 0.6F / 16.0F, shape.lipZ() * 0.6F / 16.0F);
               Vector3f position = held.last()
                  .pose()
                  .getTranslation(new Vector3f())
                  .lerp(sipping.last().pose().getTranslation(new Vector3f()), lift);
               Quaternionf rotation = held.last()
                  .pose()
                  .getUnnormalizedRotation(new Quaternionf())
                  .slerp(sipping.last().pose().getUnnormalizedRotation(new Quaternionf()), lift);
               pose.pushPose();
               pose.translate(position.x, position.y, position.z);
               pose.mulPose(rotation);
               pose.scale(0.6F, 0.6F, 0.6F);
               Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(entity, stack, ItemDisplayContext.NONE, false, pose, buffers, light);
               pose.popPose();
               return;
            }

            super.renderArmWithItem(entity, stack, context, arm, pose, buffers, light);
            return;
         }

         super.renderArmWithItem(entity, stack, context, arm, pose, buffers, light);
      }
   }

   public static class Model extends PlayerModel<BarPatron> {
      float partial;

      public Model(ModelPart root, boolean slim) {
         super(root, slim);
      }

      public void setupAnim(BarPatron patron, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
         super.setupAnim(patron, limbSwing, limbAmount, age, headYaw, headPitch);
         this.head.zRot = 0.0F;
         ItemStack held = patron.getMainHandItem();
         float lift = PatronRenderer.lift(patron, this.partial);
         boolean cigar = held.getItem() instanceof CigarItem;
         if (cigar && !patron.cigarInMouth()) {
            this.rightArm.xRot = -0.3F + (-1.95F + this.head.xRot * 0.6F + 0.3F) * lift;
            this.rightArm.yRot = -0.08F - 0.82F * lift + this.head.yRot * 0.6F * lift;
            this.rightArm.zRot = -0.08F + 0.08F * lift;
         } else if (!held.isEmpty() && !cigar && lift > 0.0F) {
            this.rightArm.xRot = Mth.lerp(lift, this.rightArm.xRot, -1.65F + this.head.xRot * 0.5F);
            this.rightArm.yRot = Mth.lerp(lift, this.rightArm.yRot, -0.6F + this.head.yRot * 0.5F);
            this.rightArm.zRot = 0.0F;
         } else if (!held.isEmpty() && !cigar) {
            this.rightArm.xRot = patron.isPassenger() ? -0.75F : -0.45F;
            this.rightArm.yRot = -0.12F;
         }

         if (lift <= 0.0F) {
            this.emote(patron, age);
         }

         if (patron.dancing() && lift <= 0.0F && patron.emote() == BarTalk.Emote.NONE && patron.state() != BarPatron.State.FIGHTING) {
            float beat = age * 0.55F;
            this.head.xRot = this.head.xRot + (patron.isPassenger() ? Mth.sin(age * 0.3F) * 0.06F : Mth.sin(beat * 2.0F) * 0.16F);
            if (!patron.isPassenger()) {
               this.leftArm.xRot = -2.5F - Mth.sin(beat) * 0.5F;
               this.leftArm.zRot = -0.25F;
               if (held.isEmpty()) {
                  this.rightArm.xRot = -2.5F + Mth.sin(beat) * 0.5F;
                  this.rightArm.zRot = 0.25F;
               }

               this.body.yRot = Mth.sin(beat) * 0.2F;
            }
         }

         if (patron.state() == BarPatron.State.ROBBING) {
            this.rightArm.xRot = -1.45F;
            this.rightArm.yRot = -0.1F;
         }

         if (patron.state() == BarPatron.State.FIGHTING && this.attackTime <= 0.0F) {
            this.rightArm.xRot = -1.25F;
            this.rightArm.yRot = -0.35F;
            this.leftArm.xRot = -1.25F;
            this.leftArm.yRot = 0.35F;
         }

         this.hat.copyFrom(this.head);
         this.rightSleeve.copyFrom(this.rightArm);
         this.leftSleeve.copyFrom(this.leftArm);
         this.jacket.copyFrom(this.body);
      }

      private void emote(BarPatron patron, float age) {
         BarTalk.Emote e = patron.emote();
         if (e != BarTalk.Emote.NONE) {
            float t = patron.emoteAge(this.partial);
            if (!(t < 0.0F) && !(t > (float)e.ticks)) {
               float p = t / (float)e.ticks;
               float env = Mth.sin(p * (float) Math.PI);
               boolean handFree = patron.getMainHandItem().isEmpty();
               switch (e) {
                  case NOD:
                     this.head.xRot = this.head.xRot + Mth.sin(p * (float) Math.PI * 4.0F) * 0.32F * env;
                     break;
                  case SHAKE:
                     this.head.yRot = this.head.yRot + Mth.sin(p * (float) Math.PI * 5.0F) * 0.42F * env;
                     break;
                  case LAUGH:
                     this.head.xRot -= 0.45F * env;
                     float shake = Mth.sin(t * 2.2F) * 0.07F * env;
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -0.55F) + shake;
                     this.leftArm.zRot = shake;
                     this.body.xRot = -0.08F * env;
                     if (handFree) {
                        this.rightArm.xRot = Mth.lerp(env, this.rightArm.xRot, -0.55F) - shake;
                     }
                     break;
                  case SHRUG:
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -0.35F);
                     this.leftArm.zRot = -0.75F * env;
                     this.leftArm.yRot = 0.3F * env;
                     if (handFree) {
                        this.rightArm.xRot = Mth.lerp(env, this.rightArm.xRot, -0.35F);
                        this.rightArm.zRot = 0.75F * env;
                        this.rightArm.yRot = -0.3F * env;
                     }

                     this.head.zRot = 0.18F * env;
                     break;
                  case POINT:
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -1.55F + this.head.xRot);
                     this.leftArm.yRot = Mth.lerp(env, this.leftArm.yRot, this.head.yRot + 0.1F);
                     break;
                  case FACEPALM:
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -2.25F);
                     this.leftArm.yRot = 0.55F * env;
                     this.leftArm.zRot = 0.0F;
                     this.head.xRot += 0.38F * env;
                     break;
                  case WAVE:
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -2.9F);
                     this.leftArm.zRot = (-0.15F + Mth.sin(t * 0.9F) * 0.35F) * env;
                     break;
                  case GESTURE:
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -0.55F + Mth.sin(t * 0.12F) * 0.12F);
                     this.leftArm.yRot = 0.2F * env;
                     this.head.yRot = this.head.yRot + Mth.sin(t * 0.1F) * 0.05F * env;
                     break;
                  case THINK:
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -1.85F);
                     this.leftArm.yRot = 0.6F * env;
                     this.head.xRot += 0.1F * env;
                     this.head.zRot = -0.08F * env;
                     break;
                  case CHEERS:
                     if (!handFree) {
                        this.rightArm.xRot = Mth.lerp(env, this.rightArm.xRot, -2.3F);
                        this.rightArm.yRot = Mth.lerp(env, this.rightArm.yRot, -0.2F);
                     } else {
                        this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -2.3F);
                     }
                     break;
                  case FIST_PUMP:
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -2.7F + Mth.sin(t * 1.2F) * 0.35F);
                     this.leftArm.zRot = -0.1F * env;
                     break;
                  case ARM_WRESTLE:
                     float rampx = Mth.clamp(Math.min(t / 8.0F, ((float)e.ticks - t) / 8.0F), 0.0F, 1.0F);
                     float strain = Mth.sin(t * 1.9F) * 0.06F * rampx;
                     ModelPart a = handFree ? this.rightArm : this.leftArm;
                     float side = handFree ? 1.0F : -1.0F;
                     a.xRot = Mth.lerp(rampx, a.xRot, -1.35F + strain);
                     a.yRot = Mth.lerp(rampx, a.yRot, -0.35F * side);
                     a.zRot = strain * side;
                     this.head.xRot += 0.12F * rampx + strain;
                     break;
                  case HIGH_FIVE:
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -2.6F);
                     this.leftArm.yRot = -0.2F * env;
                     this.leftArm.zRot = -0.35F * env;
                     break;
                  case SLEEP:
                     float ramp = Mth.clamp(Math.min(t / 12.0F, ((float)e.ticks - t) / 8.0F), 0.0F, 1.0F);
                     this.head.xRot = Mth.lerp(ramp, this.head.xRot, 0.95F);
                     this.head.yRot *= 1.0F - ramp;
                     this.leftArm.xRot = Mth.lerp(ramp, this.leftArm.xRot, -1.35F);
                     this.leftArm.yRot = Mth.lerp(ramp, this.leftArm.yRot, 0.55F);
                     if (handFree) {
                        this.rightArm.xRot = Mth.lerp(ramp, this.rightArm.xRot, -1.35F);
                        this.rightArm.yRot = Mth.lerp(ramp, this.rightArm.yRot, -0.55F);
                     }
                     break;
                  case CLAP:
                     float beat = Mth.sin(t * 1.4F) * 0.5F + 0.5F;
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -1.1F);
                     this.leftArm.yRot = (0.25F + 0.35F * beat) * env;
                     if (handFree) {
                        this.rightArm.xRot = Mth.lerp(env, this.rightArm.xRot, -1.1F);
                        this.rightArm.yRot = -(0.25F + 0.35F * beat) * env;
                     }
                     break;
                  case SWAY:
                     this.head.zRot = Mth.sin(t * 0.25F) * 0.2F * env;
                     this.head.xRot = this.head.xRot + Mth.sin(t * 0.5F) * 0.08F * env;
                     this.leftArm.xRot = Mth.lerp(env, this.leftArm.xRot, -2.4F + Mth.sin(t * 0.25F) * 0.3F);
                     this.leftArm.zRot = -0.3F * env;
               }
            }
         }
      }
   }
}
