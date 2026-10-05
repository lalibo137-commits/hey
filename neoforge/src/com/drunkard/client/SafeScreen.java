package com.drunkard.client;

import com.drunkard.BarUpgrades;
import com.drunkard.SafeMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class SafeScreen extends AbstractContainerScreen<SafeMenu> {
   private static final int TEXT = 4210752;
   private static int tab;
   private final List<Button> tabs = new ArrayList<>();
   private final List<Button>[] pages = new List[]{new ArrayList(), new ArrayList(), new ArrayList()};
   private final List<Button> buy = new ArrayList<>();
   private Button till;
   private Button unlock;

   public SafeScreen(SafeMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 176;
      this.imageHeight = 166;
   }

   protected void init() {
      super.init();
      this.tabs.clear();
      this.buy.clear();

      for (List<Button> p : this.pages) {
         p.clear();
      }

      String[] names = new String[]{"Money", "Upgrades", "Supplies"};

      for (int i = 0; i < 3; i++) {
         int t = i;
         this.tabs.add((Button)this.addRenderableWidget(Button.builder(Component.literal(names[i]), b -> {
            tab = t;
            this.refresh();
         }).bounds(this.leftPos + 7 + i * 55, this.topPos + 17, 52, 16).build()));
      }

      this.page(
         0, Button.builder(Component.literal("Deposit all cash"), b -> this.press(0)).bounds(this.leftPos + 8, this.topPos + 62, 160, 20).build()
      );
      this.page(0, Button.builder(Component.literal("$1"), b -> this.press(1)).bounds(this.leftPos + 8, this.topPos + 98, 38, 20).build());
      this.page(0, Button.builder(Component.literal("$10"), b -> this.press(2)).bounds(this.leftPos + 48, this.topPos + 98, 38, 20).build());
      this.page(0, Button.builder(Component.literal("$64"), b -> this.press(3)).bounds(this.leftPos + 88, this.topPos + 98, 38, 20).build());
      this.page(0, Button.builder(Component.literal("All"), b -> this.press(4)).bounds(this.leftPos + 128, this.topPos + 98, 40, 20).build());
      this.till = this.page(
         0,
         Button.builder(Component.literal("Empty the till into the safe"), b -> this.press(5))
            .bounds(this.leftPos + 8, this.topPos + 134, 160, 20)
            .build()
      );
      BarUpgrades.Upgrade[] ups = BarUpgrades.Upgrade.values();

      for (int i = 0; i < ups.length; i++) {
         int u = i;
         this.buy
            .add(
               this.page(
                  1,
                  Button.builder(Component.empty(), b -> this.press(100 + u))
                     .bounds(this.leftPos + 112, this.topPos + 37 + i * 20, 56, 18)
                     .build()
               )
            );
      }

      this.unlock = this.page(
         2,
         Button.builder(Component.literal("Sign the supplier's deal · $5000"), b -> this.press(6))
            .bounds(this.leftPos + 8, this.topPos + 112, 160, 20)
            .build()
      );
      this.refresh();
   }

   private Button page(int page, Button b) {
      this.pages[page].add((Button)this.addRenderableWidget(b));
      return b;
   }

   private void press(int id) {
      if (this.minecraft != null && this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((SafeMenu)this.menu).containerId, id);
      }
   }

   public void selectTab(int page) {
      tab = Math.max(0, Math.min(2, page));
      this.refresh();
   }

   private void refresh() {
      for (int i = 0; i < 3; i++) {
         this.tabs.get(i).active = i != tab;

         for (Button b : this.pages[i]) {
            b.visible = i == tab;
         }
      }
   }

   protected void containerTick() {
      super.containerTick();
      this.unlock.visible = tab == 2 && !((SafeMenu)this.menu).supplier();
      this.unlock.active = ((SafeMenu)this.menu).balance() >= 5000;
      this.till.active = ((SafeMenu)this.menu).linked() && ((SafeMenu)this.menu).till() > 0;
      this.till
         .setMessage(
            Component.literal(((SafeMenu)this.menu).linked() ? "Empty the till ($" + ((SafeMenu)this.menu).till() + ")" : "No register nearby")
         );
      BarUpgrades.Upgrade[] ups = BarUpgrades.Upgrade.values();

      for (int i = 0; i < ups.length; i++) {
         int price = ups[i].price(((SafeMenu)this.menu).upgrade(i));
         this.buy.get(i).setMessage(Component.literal(price < 0 ? "Maxed" : "$" + price));
         this.buy.get(i).active = ((SafeMenu)this.menu).linked() && price >= 0 && ((SafeMenu)this.menu).balance() >= price;
      }
   }

   protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
      g.blit(CashRegisterScreen.TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
      if (tab == 2 && ((SafeMenu)this.menu).supplier()) {
         List<BarUpgrades.Supply> list = BarUpgrades.supplies();

         for (int i = 0; i < list.size(); i++) {
            int x = this.leftPos + 7 + i % 9 * 18;
            int y = this.topPos + 37 + i / 9 * 18;
            g.blit(CashRegisterScreen.TEXTURE, x, y, 176, 0, 18, 18);
            g.renderItem(list.get(i).stack().get(), x + 1, y + 1);
            if (((SafeMenu)this.menu).balance() < list.get(i).price()) {
               g.fill(x + 1, y + 1, x + 17, y + 17, 300, -1869902965);
            }
         }
      }
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      g.drawString(this.font, this.title, 8, 6, 4210752, false);
      switch (tab) {
         case 0:
            g.drawString(this.font, "Balance", 8, 40, 4210752, false);
            g.drawString(this.font, "$" + ((SafeMenu)this.menu).balance(), 8, 50, 3046706, false);
            g.drawString(this.font, "Withdraw", 8, 88, 4210752, false);
            g.drawString(this.font, "Pays for upgrades and supplies", 8, 122, 7368816, false);
            break;
         case 1:
            BarUpgrades.Upgrade[] ups = BarUpgrades.Upgrade.values();

            for (int i = 0; i < ups.length; i++) {
               int lvl = ((SafeMenu)this.menu).upgrade(i);
               int y = 37 + i * 20;
               g.drawString(this.font, ups[i].title, 8, y + 1, 4210752, false);
               StringBuilder pips = new StringBuilder();

               for (int k = 0; k < ups[i].maxLevel(); k++) {
                  pips.append(k < lvl ? "■" : "□");
               }

               g.drawString(this.font, pips.toString(), 8, y + 10, lvl > 0 ? 3046706 : 9474192, false);
            }

            if (!((SafeMenu)this.menu).linked()) {
               g.drawString(this.font, "Place a cash register nearby", 8, 158, 12000284, false);
            }
            break;
         default:
            if (((SafeMenu)this.menu).supplier()) {
               g.drawString(this.font, "Click to buy · $" + ((SafeMenu)this.menu).balance() + " in the safe", 8, 150, 7368816, false);
            } else {
               g.drawString(this.font, "A wholesaler will deliver", 8, 42, 4210752, false);
               g.drawString(this.font, "bottles, smokes and glassware", 8, 53, 4210752, false);
               g.drawString(this.font, "at a third of the shelf price,", 8, 64, 4210752, false);
               g.drawString(this.font, "but only after you sign a", 8, 75, 4210752, false);
               g.drawString(this.font, "one-off deal.", 8, 86, 4210752, false);
               g.drawString(
                  this.font,
                  "$" + ((SafeMenu)this.menu).balance() + " in the safe",
                  8,
                  142,
                  ((SafeMenu)this.menu).balance() >= 5000 ? 3046706 : 12000284,
                  false
               );
            }
      }
   }

   private int supplyAt(double mx, double my) {
      if (tab == 2 && ((SafeMenu)this.menu).supplier()) {
         List<BarUpgrades.Supply> list = BarUpgrades.supplies();

         for (int i = 0; i < list.size(); i++) {
            int x = this.leftPos + 7 + i % 9 * 18;
            int y = this.topPos + 37 + i / 9 * 18;
            if (mx >= (double)x && mx < (double)(x + 18) && my >= (double)y && my < (double)(y + 18)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      int i = this.supplyAt(mx, my);
      if (i >= 0 && button == 0) {
         this.press(200 + i);
         if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }

         return true;
      } else {
         return super.mouseClicked(mx, my, button);
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      super.render(g, mouseX, mouseY, partial);
      int i = this.supplyAt((double)mouseX, (double)mouseY);
      if (i >= 0) {
         BarUpgrades.Supply s = BarUpgrades.supplies().get(i);
         ItemStack stack = s.stack().get();
         g.renderComponentTooltip(
            this.font,
            List.of(
               stack.getHoverName(),
               Component.literal("Buy for $" + s.price())
                  .withStyle(((SafeMenu)this.menu).balance() >= s.price() ? ChatFormatting.GREEN : ChatFormatting.RED)
            ),
            mouseX,
            mouseY
         );
      } else {
         if (tab == 1) {
            BarUpgrades.Upgrade[] ups = BarUpgrades.Upgrade.values();

            for (int k = 0; k < ups.length; k++) {
               int y = this.topPos + 37 + k * 20;
               if (mouseX >= this.leftPos + 8 && mouseX < this.leftPos + 110 && mouseY >= y && mouseY < y + 18) {
                  g.renderComponentTooltip(
                     this.font,
                     List.of(
                        Component.literal(ups[k].title),
                        Component.literal(ups[k].description).withStyle(ChatFormatting.GRAY),
                        Component.literal("Level " + ((SafeMenu)this.menu).upgrade(k) + " of " + ups[k].maxLevel()).withStyle(ChatFormatting.DARK_GRAY)
                     ),
                     mouseX,
                     mouseY
                  );
                  return;
               }
            }
         }

         this.renderTooltip(g, mouseX, mouseY);
      }
   }
}
