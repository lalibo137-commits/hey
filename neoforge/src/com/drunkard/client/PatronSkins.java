package com.drunkard.client;

import com.drunkard.SkinPainter;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public final class PatronSkins {
   private static final ResourceLocation STEVE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
   private static final ResourceLocation ALEX = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/slim/alex.png");
   private static final Map<Integer, ResourceLocation> MADE = new LinkedHashMap<>(64, 0.75F, true);

   public static ResourceLocation get(int variant) {
      ResourceLocation id = MADE.get(variant);
      if (id != null) {
         return id;
      } else {
         SkinPainter.Look look = SkinPainter.roll(variant);

         NativeImage image;
         try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(look.alex ? ALEX : STEVE).open()) {
            image = NativeImage.read(in);
         } catch (Exception var9) {
            return look.alex ? ALEX : STEVE;
         }

         if (image.getWidth() == 64 && image.getHeight() == 64) {
            int[] px = new int[4096];

            for (int y = 0; y < 64; y++) {
               for (int x = 0; x < 64; x++) {
                  px[y * 64 + x] = swap(image.getPixelRGBA(x, y));
               }
            }

            SkinPainter.paint(px, look);

            for (int y = 0; y < 64; y++) {
               for (int x = 0; x < 64; x++) {
                  image.setPixelRGBA(x, y, swap(px[y * 64 + x]));
               }
            }

            id = ResourceLocation.fromNamespaceAndPath("drunkard", "patron/skin_" + variant);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            MADE.put(variant, id);
            if (MADE.size() > 96) {
               Iterator<Entry<Integer, ResourceLocation>> eldest = MADE.entrySet().iterator();
               Minecraft.getInstance().getTextureManager().release(eldest.next().getValue());
               eldest.remove();
            }

            return id;
         } else {
            image.close();
            return look.alex ? ALEX : STEVE;
         }
      }
   }

   private static int swap(int c) {
      return c & -16711936 | (c & 0xFF) << 16 | c >> 16 & 0xFF;
   }

   public static void clear() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(PatronSkins::clear);
      } else {
         for (ResourceLocation id : MADE.values()) {
            Minecraft.getInstance().getTextureManager().release(id);
         }

         MADE.clear();
      }
   }

   private PatronSkins() {
   }
}
