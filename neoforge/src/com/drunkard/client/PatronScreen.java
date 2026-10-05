package com.drunkard.client;

import com.drunkard.BarPatron;
import com.drunkard.BarProducts;
import com.drunkard.PatronMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PatronScreen extends AbstractContainerScreen<PatronMenu> {
   private static final int TEXT = 4210752;
   private Button serve;

   public PatronScreen(PatronMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 176;
      this.imageHeight = 90;
   }

   protected void init() {
      super.init();
      this.serve = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Serve"), b -> this.press(0)).bounds(this.leftPos + 8, this.topPos + 62, 54, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Not today"), b -> this.press(1)).bounds(this.leftPos + 64, this.topPos + 62, 52, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Show out"), b -> this.press(2)).bounds(this.leftPos + 118, this.topPos + 62, 50, 20).build()
      );
   }

   private void press(int id) {
      if (this.minecraft != null && this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((PatronMenu)this.menu).containerId, id);
      }
   }

   protected void containerTick() {
      super.containerTick();
      this.serve.active = ((PatronMenu)this.menu).state() == BarPatron.State.WAITING && ((PatronMenu)this.menu).product() != null;
      this.serve.setMessage(Component.literal(((PatronMenu)this.menu).state() == BarPatron.State.AWAIT_SHOT ? "Pour it" : "Serve"));
   }

   protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
      g.blit(CashRegisterScreen.TEXTURE, this.leftPos, this.topPos, 0, 166, this.imageWidth, this.imageHeight);
      g.blit(CashRegisterScreen.TEXTURE, this.leftPos + 7, this.topPos + 21, 176, 0, 18, 18);
      BarProducts.Product p = ((PatronMenu)this.menu).product();
      if (p != null) {
         g.renderItem(p.icon(), this.leftPos + 8, this.topPos + 22);
      }
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      g.drawString(this.font, this.title, 8, 6, 4210752, false);
      BarProducts.Product p = ((PatronMenu)this.menu).product();
      if (p == null) {
         g.drawString(this.font, "Still deciding...", 30, 26, 4210752, false);
      } else {
         boolean shot = ((PatronMenu)this.menu).shot();

         String what = switch (p.kind()) {
            case SPIRIT -> shot ? "A shot of " : "A bottle of ";
            case BEER, WINE -> "A bottle of ";
            case CIGARETTES -> "";
            default -> "One ";
         };
         g.drawString(this.font, what + p.name().getString(), 30, 22, 4210752, false);
         g.drawString(this.font, "$" + p.price(shot) + (shot ? " · in a shot glass" : ""), 30, 33, 7368816, false);
         String status = ((PatronMenu)this.menu).state() == BarPatron.State.AWAIT_SHOT
            ? "Set a shot glass near them and pour it"
            : (shot ? "Serve, then pour it for them" : "Taken from your inventory");
         g.drawString(this.font, status, 8, 48, 4210752, false);
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      super.render(g, mouseX, mouseY, partial);
      this.renderTooltip(g, mouseX, mouseY);
   }
}
