package com.drunkard.client;

import com.drunkard.GlassShelfMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GlassShelfScreen extends AbstractContainerScreen<GlassShelfMenu> {
   private static final ResourceLocation CHEST = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

   public GlassShelfScreen(GlassShelfMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageHeight = 132;
      this.inventoryLabelY = this.imageHeight - 94;
   }

   protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
      g.blit(CHEST, this.leftPos, this.topPos, 0, 0, this.imageWidth, 35);
      g.blit(CHEST, this.leftPos, this.topPos + 18 + 17, 0, 126, this.imageWidth, 96);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      super.render(g, mouseX, mouseY, partial);
      this.renderTooltip(g, mouseX, mouseY);
   }
}
