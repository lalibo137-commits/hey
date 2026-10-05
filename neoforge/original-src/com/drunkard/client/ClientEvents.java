package com.drunkard.client;

import com.drunkard.CigarItem;
import com.drunkard.CigarettePackItem;
import com.drunkard.Drunkard;
import com.drunkard.OnTheRocksItem;
import com.drunkard.PipeItem;
import com.drunkard.ShotGlassItem;
import com.drunkard.WineData;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

@EventBusSubscriber(
   modid = "drunkard",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class ClientEvents {
   @SubscribeEvent
   public static void reload(RegisterClientReloadListenersEvent e) {
      e.registerReloadListener((ResourceManagerReloadListener)manager -> {
         DrunkHeadTextures.clear();
         RenderSystem.recordRenderCall(HangoverVision::close);
      });
   }

   @SubscribeEvent
   public static void particles(RegisterParticleProvidersEvent e) {
      e.registerSpriteSet((ParticleType)Drunkard.BUBBLE.get(), FermentationBubble.Provider::new);
      e.registerSpriteSet((ParticleType)Drunkard.CIGAR_SMOKE.get(), CigarSmoke.Provider::new);
   }

   @SubscribeEvent
   public static void cigarSetup(FMLClientSetupEvent e) {
      e.enqueueWork(
         () -> {
            for (Supplier<Item> smoke : List.of(Drunkard.CIGAR, Drunkard.CIGARETTE)) {
               ItemProperties.register(
                  smoke.get(), ResourceLocation.fromNamespaceAndPath("drunkard", "burn"), (stack, level, entity, seed) -> (float)CigarItem.burnStage(stack)
               );
            }
         }
      );
   }

   @SubscribeEvent
   public static void setup(FMLClientSetupEvent e) {
      e.enqueueWork(
         () -> {
            for (Supplier<Item> smoke : List.of(Drunkard.CIGAR, Drunkard.CIGARETTE)) {
               ItemProperties.register(
                  smoke.get(), ResourceLocation.fromNamespaceAndPath("drunkard", "lit"), (stack, level, entity, seed) -> CigarItem.lit(stack) ? 1.0F : 0.0F
               );
            }

            for (Supplier<Item> drink : Drunkard.WINE.values()) {
               ItemProperties.register(
                  drink.get(),
                  ResourceLocation.fromNamespaceAndPath("drunkard", "drinking"),
                  (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
               );
            }
         }
      );
   }

   @SubscribeEvent
   public static void barSetup(FMLClientSetupEvent e) {
      e.enqueueWork(
         () -> {
            ItemProperties.register(
               Drunkard.SHOT_GLASS.get(),
               ResourceLocation.fromNamespaceAndPath("drunkard", "filled"),
               (stack, level, entity, seed) -> ShotGlassItem.filled(stack) ? 1.0F : 0.0F
            );
            ItemProperties.register(
               Drunkard.PIPE.get(),
               ResourceLocation.fromNamespaceAndPath("drunkard", "packed"),
               (stack, level, entity, seed) -> PipeItem.packed(stack) ? 1.0F : 0.0F
            );
            ItemProperties.register(
               Drunkard.CIGARETTE_PACK.get(),
               ResourceLocation.fromNamespaceAndPath("drunkard", "cigarettes"),
               (stack, level, entity, seed) -> (float)CigarettePackItem.count(stack) / 20.0F
            );
         }
      );
   }

   @SubscribeEvent
   public static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) {
      e.register(
         (stack, tint) -> tint == 0 && ShotGlassItem.filled(stack) ? 0xFF000000 | WineData.liquidColor(ShotGlassItem.type(stack)) : -1,
         new ItemLike[]{(ItemLike)Drunkard.SHOT_GLASS.get()}
      );
      e.register(
         (stack, tint) -> tint == 0 ? 0xFF000000 | WineData.liquidColor(OnTheRocksItem.type(stack)) : -1, new ItemLike[]{(ItemLike)Drunkard.ON_THE_ROCKS.get()}
      );
   }

   @SubscribeEvent
   public static void renderers(RegisterRenderers e) {
      e.registerBlockEntityRenderer(Drunkard.BOTTLE_ENTITY.get(), BottleRenderer::new);
      e.registerBlockEntityRenderer(Drunkard.HUMIDOR_ENTITY.get(), HumidorRenderer::new);
      e.registerBlockEntityRenderer(Drunkard.RACK_ENTITY.get(), RackRenderer::new);
      e.registerBlockEntityRenderer(Drunkard.TUB_ENTITY.get(), TubRenderer::new);
      e.registerBlockEntityRenderer(Drunkard.CIGAR_HOLDER_ENTITY.get(), CigarHolderRenderer::new);
      e.registerBlockEntityRenderer(Drunkard.SHOT_GLASS_ENTITY.get(), ShotGlassRenderer::new);
      e.registerBlockEntityRenderer(Drunkard.COOLER_ENTITY.get(), CoolerRenderer::new);
      e.registerEntityRenderer(Drunkard.MOLOTOV_ENTITY.get(), ThrownItemRenderer::new);
      e.registerEntityRenderer(Drunkard.SEAT_ENTITY.get(), NoopRenderer::new);
   }

   private static void replaceHandLayer(PlayerRenderer renderer) {
      List layers = (List)ObfuscationReflectionHelper.getPrivateValue(LivingEntityRenderer.class, renderer, "layers");

      for (int i = 0; i < layers.size(); i++) {
         if (layers.get(i).getClass() == PlayerItemInHandLayer.class) {
            layers.set(i, new DrinkingItemLayer(renderer));
            return;
         }
      }
   }

   @SubscribeEvent
   public static void layers(AddLayers e) {
      for (Model skin : e.getSkins()) {
         PlayerRenderer renderer = (PlayerRenderer)e.getSkin(skin);
         if (renderer != null) {
            renderer.addLayer(new DrunkFaceLayer(renderer));
            renderer.addLayer(new CigarMouthLayer(renderer));
            replaceHandLayer(renderer);
         }
      }
   }

   @SubscribeEvent
   public static void screens(RegisterMenuScreensEvent e) {
      e.register(Drunkard.BARREL_MENU.get(), BarrelScreen::new);
      e.register(Drunkard.MACHINE_MENU.get(), MachineScreen::new);
   }
}
