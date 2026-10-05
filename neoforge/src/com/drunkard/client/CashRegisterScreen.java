package com.drunkard.client;

import com.drunkard.BarProducts;
import com.drunkard.CashRegisterEntity;
import com.drunkard.CashRegisterMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class CashRegisterScreen extends AbstractContainerScreen<CashRegisterMenu> {
   static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("drunkard", "textures/gui/cash_register.png");
   private static final int TEXT = 4210752;
   private static int tab;
   private final List<Button> tabs = new ArrayList<>();
   private final List<Button>[] pages = new List[]{new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList()};
   private Button openButton;
   private Button less;
   private Button more;
   private Button collect;
   private Button staffButton;
   private Button styleButton;

   public CashRegisterScreen(CashRegisterMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 176;
      this.imageHeight = 166;
   }

   protected void init() {
      super.init();
      this.tabs.clear();

      for (List<Button> page : this.pages) {
         page.clear();
      }

      String[] names = new String[]{"Store", "Stock", "Seats", "Pub", "Staff"};

      for (int i = 0; i < 5; i++) {
         int t = i;
         this.tabs.add((Button)this.addRenderableWidget(Button.builder(Component.literal(names[i]), b -> {
            tab = t;
            this.refresh();
         }).bounds(this.leftPos + 7 + i * 33, this.topPos + 17, 31, 16).build()));
      }

      this.openButton = this.page(
         0, Button.builder(Component.empty(), b -> this.press(0)).bounds(this.leftPos + 8, this.topPos + 52, 160, 20).build()
      );
      this.less = this.page(
         0, Button.builder(Component.literal("-"), b -> this.press(1)).bounds(this.leftPos + 126, this.topPos + 80, 20, 18).build()
      );
      this.more = this.page(
         0, Button.builder(Component.literal("+"), b -> this.press(2)).bounds(this.leftPos + 148, this.topPos + 80, 20, 18).build()
      );
      this.collect = this.page(
         0, Button.builder(Component.literal("Collect"), b -> this.press(3)).bounds(this.leftPos + 116, this.topPos + 124, 52, 20).build()
      );
      this.page(1, Button.builder(Component.literal("Stock all"), b -> this.press(7)).bounds(this.leftPos + 8, this.topPos + 100, 79, 20).build());
      this.page(1, Button.builder(Component.literal("Clear"), b -> this.press(8)).bounds(this.leftPos + 89, this.topPos + 100, 79, 20).build());
      this.page(
         2, Button.builder(Component.literal("Select door"), b -> this.press(4)).bounds(this.leftPos + 8, this.topPos + 52, 160, 20).build()
      );
      this.page(
         2,
         Button.builder(Component.literal("Add / remove seats"), b -> this.press(5)).bounds(this.leftPos + 8, this.topPos + 92, 110, 20).build()
      );
      this.page(2, Button.builder(Component.literal("Clear"), b -> this.press(6)).bounds(this.leftPos + 120, this.topPos + 92, 48, 20).build());
      this.styleButton = this.page(
         3, Button.builder(Component.empty(), b -> this.press(10)).bounds(this.leftPos + 8, this.topPos + 118, 160, 20).build()
      );
      this.staffButton = this.page(
         4, Button.builder(Component.empty(), b -> this.press(9)).bounds(this.leftPos + 8, this.topPos + 66, 160, 20).build()
      );
      this.refresh();
   }

   private Button page(int page, Button button) {
      this.pages[page].add((Button)this.addRenderableWidget(button));
      return button;
   }

   private void press(int id) {
      if (this.minecraft != null && this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((CashRegisterMenu)this.menu).containerId, id);
      }
   }

   public void selectTab(int page) {
      tab = Math.max(0, Math.min(4, page));
      this.refresh();
   }

   private void refresh() {
      for (int i = 0; i < 5; i++) {
         this.tabs.get(i).active = i != tab;

         for (Button b : this.pages[i]) {
            b.visible = i == tab;
         }
      }
   }

   protected void containerTick() {
      super.containerTick();
      this.openButton.setMessage(Component.literal(((CashRegisterMenu)this.menu).open() ? "Close the store" : "Open the store"));
      this.less.active = ((CashRegisterMenu)this.menu).capacity() > 1;
      this.more.active = ((CashRegisterMenu)this.menu).capacity() < ((CashRegisterMenu)this.menu).maxCapacity();
      this.collect.active = ((CashRegisterMenu)this.menu).till() > 0;
      this.staffButton.setMessage(Component.literal(((CashRegisterMenu)this.menu).staffOff() ? "Call the barkeep in" : "Send the barkeep home"));
      this.staffButton.active = ((CashRegisterMenu)this.menu).barkeeps() > 0;
      int style = Math.max(0, Math.min(CashRegisterEntity.STYLES.length - 1, ((CashRegisterMenu)this.menu).style()));
      this.styleButton.setMessage(Component.literal("Customers: " + CashRegisterEntity.STYLES[style]));
      this.styleButton.active = !((CashRegisterMenu)this.menu).open() && ((CashRegisterMenu)this.menu).patrons() == 0;
   }

   protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
      g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
      if (tab == 1) {
         for (int i = 0; i < BarProducts.ALL.size(); i++) {
            int x = this.leftPos + 7 + i % 9 * 18;
            int y = this.topPos + 39 + i / 9 * 18;
            g.blit(TEXTURE, x, y, 176, 0, 18, 18);
            g.renderItem(BarProducts.ALL.get(i).icon(), x + 1, y + 1);
            if (!((CashRegisterMenu)this.menu).stocked(i)) {
               g.fill(x + 1, y + 1, x + 17, y + 17, 300, -1333032053);
            } else {
               g.blit(TEXTURE, x + 11, y + 1, 300, 194.0F, 0.0F, 6, 6, 256, 256);
            }
         }
      }
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      g.drawString(this.font, this.title, 8, 6, 4210752, false);
      switch (tab) {
         case 0:
            g.drawString(this.font, "Store is ", 8, 40, 4210752, false);
            g.drawString(
               this.font,
               ((CashRegisterMenu)this.menu).open() ? "OPEN" : "CLOSED",
               8 + this.font.width("Store is "),
               40,
               ((CashRegisterMenu)this.menu).open() ? 3046706 : 12000284,
               false
            );
            g.drawString(this.font, "Max customers: " + ((CashRegisterMenu)this.menu).capacity(), 8, 85, 4210752, false);
            g.drawString(this.font, "Customers inside: " + ((CashRegisterMenu)this.menu).patrons(), 8, 104, 4210752, false);
            g.drawString(this.font, "Till: $" + ((CashRegisterMenu)this.menu).till(), 8, 130, 4210752, false);
            if (((CashRegisterMenu)this.menu).open() && (!((CashRegisterMenu)this.menu).hasDoor() || this.stockedCount() == 0)) {
               g.drawString(
                  this.font,
                  !((CashRegisterMenu)this.menu).hasDoor() ? "No door set (Seating tab)" : "Nothing in stock (Stock tab)",
                  8,
                  152,
                  12000284,
                  false
               );
            }
            break;
         case 1:
            g.drawString(this.font, this.stockedCount() + " of " + BarProducts.ALL.size() + " in stock", 8, 126, 4210752, false);
            g.drawString(this.font, "Customers only order these.", 8, 140, 7368816, false);
            break;
         case 2:
         default:
            g.drawString(
               this.font,
               ((CashRegisterMenu)this.menu).hasDoor() ? "Door: linked" : "Door: none yet",
               8,
               40,
               ((CashRegisterMenu)this.menu).hasDoor() ? 4210752 : 12000284,
               false
            );
            g.drawString(this.font, "Seats: " + ((CashRegisterMenu)this.menu).seats(), 8, 80, 4210752, false);
            g.drawString(this.font, "Pick a wooden door, then bar", 8, 120, 7368816, false);
            g.drawString(this.font, "stools or stairs to sit on.", 8, 131, 7368816, false);
            g.drawString(this.font, "Sneak-click when done.", 8, 142, 7368816, false);
            break;
         case 3:
            this.pub(g);
            break;
         case 4:
            this.staff(g);
      }
   }

   private void pub(GuiGraphics g) {
      int rep = ((CashRegisterMenu)this.menu).reputation();
      int stars = Math.max(1, Math.min(5, (rep + 10) / 20));
      g.drawString(this.font, "Reputation ", 8, 39, 4210752, false);
      int x = 8 + this.font.width("Reputation ");

      for (int i = 0; i < 5; i++) {
         g.drawString(this.font, "★", x, 39, i < stars ? 14723072 : 10526880, false);
         x += this.font.width("★") + 1;
      }

      g.drawString(
         this.font,
         rep >= 80
            ? "Everyone's talking about it"
            : (rep >= 60 ? "A popular spot" : (rep >= 40 ? "A decent local" : (rep >= 20 ? "Word is it's slow" : "Nobody wants to come"))),
         8,
         50,
         7368816,
         false
      );
      g.drawString(this.font, "Today: " + ((CashRegisterMenu)this.menu).servedToday() + " served", 8, 64, 4210752, false);
      g.drawString(
         this.font,
         "Takings $" + ((CashRegisterMenu)this.menu).takingsToday() + " · tips $" + ((CashRegisterMenu)this.menu).tipsToday(),
         8,
         75,
         4210752,
         false
      );
      if (((CashRegisterMenu)this.menu).walkoutsToday() > 0) {
         g.drawString(this.font, ((CashRegisterMenu)this.menu).walkoutsToday() + " walked out waiting", 8, 86, 12000284, false);
      }

      g.drawString(this.font, "Happy customers come back.", 8, 100, 7368816, false);
      g.drawString(
         this.font,
         ((CashRegisterMenu)this.menu).open() || ((CashRegisterMenu)this.menu).patrons() > 0
            ? "Close up and empty the pub to change"
            : "Pick before you open the store",
         8,
         142,
         7368816,
         false
      );
   }

   private void staff(GuiGraphics g) {
      int n = ((CashRegisterMenu)this.menu).barkeeps();
      if (n == 0) {
         g.drawString(this.font, "No barkeep hired yet.", 8, 40, 4210752, false);
         g.drawString(this.font, "Hire one from a safe's", 8, 98, 7368816, false);
         g.drawString(this.font, "Upgrades page.", 8, 109, 7368816, false);
      } else {
         g.drawString(this.font, (n == 1 ? "1 barkeep" : n + " barkeeps") + " · ", 8, 40, 4210752, false);
         g.drawString(
            this.font,
            ((CashRegisterMenu)this.menu).staffOff() ? "SENT HOME" : "ON SHIFT",
            8 + this.font.width((n == 1 ? "1 barkeep" : n + " barkeeps") + " · "),
            40,
            ((CashRegisterMenu)this.menu).staffOff() ? 12000284 : 3046706,
            false
         );
         g.drawString(
            this.font, ((CashRegisterMenu)this.menu).staffOff() ? "Off duty: no wages today." : "They serve beer and shots.", 8, 52, 7368816, false
         );
         g.drawString(this.font, "Wages: $" + ((CashRegisterMenu)this.menu).wages() + " a day, at dawn", 8, 96, 4210752, false);
         g.drawString(
            this.font,
            ((CashRegisterMenu)this.menu).hasSafe() ? "Safe: $" + ((CashRegisterMenu)this.menu).safeBalance() : "No safe near the bar",
            8,
            108,
            ((CashRegisterMenu)this.menu).hasSafe() ? 4210752 : 12000284,
            false
         );
         g.drawString(this.font, "Till: $" + ((CashRegisterMenu)this.menu).till(), 8, 120, 4210752, false);
         g.drawString(this.font, "Paid from the safe first,", 8, 138, 7368816, false);
         g.drawString(this.font, "then from the till.", 8, 149, 7368816, false);
      }
   }

   private int stockedCount() {
      int n = 0;

      for (int i = 0; i < BarProducts.ALL.size(); i++) {
         if (((CashRegisterMenu)this.menu).stocked(i)) {
            n++;
         }
      }

      return n;
   }

   private int productAt(double mouseX, double mouseY) {
      if (tab != 1) {
         return -1;
      } else {
         for (int i = 0; i < BarProducts.ALL.size(); i++) {
            int x = this.leftPos + 7 + i % 9 * 18;
            int y = this.topPos + 39 + i / 9 * 18;
            if (mouseX >= (double)x && mouseX < (double)(x + 18) && mouseY >= (double)y && mouseY < (double)(y + 18)) {
               return i;
            }
         }

         return -1;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int product = this.productAt(mouseX, mouseY);
      if (product >= 0 && button == 0) {
         this.press(100 + product);
         if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }

         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      super.render(g, mouseX, mouseY, partial);
      int product = this.productAt((double)mouseX, (double)mouseY);
      if (product >= 0) {
         BarProducts.Product p = BarProducts.ALL.get(product);
         List<Component> lines = new ArrayList<>();
         lines.add(p.name());
         lines.add(
            Component.literal(p.shots() ? "Shot $" + p.shot() + " ·Bottle $" + p.whole() : priceLabel(p) + " $" + p.whole()).withStyle(ChatFormatting.GRAY)
         );
         lines.add(
            Component.literal(((CashRegisterMenu)this.menu).stocked(product) ? "In stock · click to remove" : "Not stocked · click to add")
               .withStyle(((CashRegisterMenu)this.menu).stocked(product) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
         );
         g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
      } else {
         this.renderTooltip(g, mouseX, mouseY);
      }
   }

   static String priceLabel(BarProducts.Product p) {
      return switch (p.kind()) {
         case BEER, WINE, SPIRIT -> "Bottle";
         case COCKTAIL -> "Cocktail";
         case CIGAR -> "Cigar";
         case CIGARETTES -> "Pack";
      };
   }
}
