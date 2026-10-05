package com.drunkard.client;

import com.drunkard.BarDialogue;
import com.drunkard.Medieval;
import com.drunkard.BarPatron;
import com.drunkard.DialogueMenu;
import com.drunkard.SkinPainter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

public class DialogueScreen extends AbstractContainerScreen<DialogueMenu> {
   private final List<Button> choices = new ArrayList<>();
   private int shownNode = -1;
   private long shownAt;
   private boolean answered;

   public DialogueScreen(DialogueMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
   }

   protected void init() {
      super.init();
      this.imageWidth = this.width;
      this.imageHeight = this.height;
      this.leftPos = 0;
      this.topPos = 0;
      this.shownNode = -1;
      this.rebuild();
   }

   private void rebuild() {
      for (Button b : this.choices) {
         this.removeWidget(b);
      }

      this.choices.clear();
      this.shownNode = ((DialogueMenu)this.menu).data.get(0);
      this.shownAt = System.currentTimeMillis();
      this.answered = false;
      BarDialogue.Node node = ((DialogueMenu)this.menu).node();
      int w = Math.min(320, this.width - 40);
      int x = (this.width - w) / 2;
      if (node.end()) {
         this.choices
            .add(
               (Button)this.addRenderableWidget(
                  Button.builder(Component.literal("Leave"), b -> this.press(91)).bounds(x + w / 2 - 50, this.height - 34, 100, 20).build()
               )
            );
      } else {
         int n = node.options().size();

         for (int i = 0; i < n; i++) {
            int choice = i;
            this.choices
               .add(
                  (Button)this.addRenderableWidget(
                     Button.builder(Component.literal(i + 1 + ". " + this.fill(node.options().get(i).text())), b -> this.choose(choice))
                        .bounds(x, this.height - 14 - (n - i) * 22, w, 20)
                        .build()
                  )
               );
         }
      }
   }

   private void choose(int choice) {
      if (!this.answered) {
         this.answered = true;
         this.press(choice);
      }
   }

   private void press(int id) {
      if (this.minecraft != null && this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((DialogueMenu)this.menu).containerId, id);
      }
   }

   private String fill(String text) {
      String drink = "drink";
      String other = null;
      int skin = 0;
      String bartender = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getName().getString() : "friend";
      if (this.minecraft != null
         && this.minecraft.level != null
         && this.minecraft.level.getEntity(((DialogueMenu)this.menu).entityId) instanceof BarPatron patron) {
         skin = patron.skin();
         if (patron.product() != null) {
            drink = patron.product().name().getString();
         }

         BarPatron nearest = null;
         double best = 196.0;

         for (BarPatron o : this.minecraft.level.getEntitiesOfClass(BarPatron.class, patron.getBoundingBox().inflate(14.0), ox -> ox != patron)) {
            double d = o.distanceToSqr(patron);
            if (d < best && d > 4.0) {
               best = d;
               nearest = o;
            }
         }

         if (nearest != null) {
            other = SkinPainter.describe(nearest.skin());
         }
      }

      return Medieval.speak(skin, BarDialogue.fill(text, drink, other, bartender));
   }

   protected void containerTick() {
      super.containerTick();
      if (((DialogueMenu)this.menu).data.get(0) != this.shownNode) {
         this.rebuild();
      }

      if (!this.answered && !((DialogueMenu)this.menu).node().end() && System.currentTimeMillis() - this.shownAt > 12000L) {
         this.answered = true;
         this.press(90);
      }
   }

   public boolean keyPressed(int key, int scan, int modifiers) {
      BarDialogue.Node node = ((DialogueMenu)this.menu).node();
      if (!node.end() && key >= 49 && key < 49 + node.options().size()) {
         this.choose(key - 49);
         return true;
      } else if (!node.end() || key != 257 && key != 32) {
         return super.keyPressed(key, scan, modifiers);
      } else {
         this.press(91);
         return true;
      }
   }

   // Keep the world visible behind the conversation (1.21 would otherwise blur and dim it).
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBg(g, partial, mouseX, mouseY);
   }

   protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
      BarDialogue.Node node = ((DialogueMenu)this.menu).node();
      int n = node.end() ? 1 : node.options().size();
      int bandTop = this.height - 14 - n * 22 - 52;
      g.fillGradient(0, bandTop - 30, this.width, bandTop, 0, -1610612736);
      g.fill(0, bandTop, this.width, this.height, -1610612736);
      int w = Math.min(320, this.width - 40);
      List<FormattedCharSequence> lines = this.font.split(Component.literal("“" + this.fill(node.line()) + "”"), w);
      int y = bandTop + 8;

      for (FormattedCharSequence line : lines) {
         g.drawString(this.font, line, (this.width - this.font.width(line)) / 2, y, 16777215, true);
         y += 11;
      }

      if (!node.end() && !this.answered) {
         float left = 1.0F - Math.min(1.0F, (float)(System.currentTimeMillis() - this.shownAt) / 12000.0F);
         int bar = (int)((float)w * left);
         int x = (this.width - w) / 2;
         int by = this.height - 14 - n * 22 - 8;
         g.fill(x, by, x + w, by + 3, 1627389951);
         g.fill(x, by, x + bar, by + 3, left > 0.3F ? -1513240 : -2074038);
      }
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
   }

   public boolean isPauseScreen() {
      return false;
   }
}
