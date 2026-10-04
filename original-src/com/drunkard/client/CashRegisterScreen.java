package com.drunkard.client;

import com.drunkard.BarProducts;
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
   static final ResourceLocation TEXTURE = new ResourceLocation("drunkard", "textures/gui/cash_register.png");
   private static final int TEXT = 4210752;
   private static int tab;
   private final List<Button> tabs = new ArrayList<>();
   private final List<Button>[] pages = new List[]{new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList()};
   private Button openButton;
   private Button less;
   private Button more;
   private Button collect;
   private Button staffButton;

   public CashRegisterScreen(CashRegisterMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.f_97726_ = 176;
      this.f_97727_ = 166;
   }

   protected void m_7856_() {
      super.m_7856_();
      this.tabs.clear();

      for (List<Button> page : this.pages) {
         page.clear();
      }

      String[] names = new String[]{"Store", "Stock", "Seats", "Pub", "Staff"};

      for (int i = 0; i < 5; i++) {
         int t = i;
         this.tabs.add((Button)this.m_142416_(Button.m_253074_(Component.m_237113_(names[i]), b -> {
            tab = t;
            this.refresh();
         }).m_252987_(this.f_97735_ + 7 + i * 33, this.f_97736_ + 17, 31, 16).m_253136_()));
      }

      this.openButton = this.page(
         0, Button.m_253074_(Component.m_237119_(), b -> this.press(0)).m_252987_(this.f_97735_ + 8, this.f_97736_ + 52, 160, 20).m_253136_()
      );
      this.less = this.page(
         0, Button.m_253074_(Component.m_237113_("-"), b -> this.press(1)).m_252987_(this.f_97735_ + 126, this.f_97736_ + 80, 20, 18).m_253136_()
      );
      this.more = this.page(
         0, Button.m_253074_(Component.m_237113_("+"), b -> this.press(2)).m_252987_(this.f_97735_ + 148, this.f_97736_ + 80, 20, 18).m_253136_()
      );
      this.collect = this.page(
         0, Button.m_253074_(Component.m_237113_("Collect"), b -> this.press(3)).m_252987_(this.f_97735_ + 116, this.f_97736_ + 124, 52, 20).m_253136_()
      );
      this.page(1, Button.m_253074_(Component.m_237113_("Stock all"), b -> this.press(7)).m_252987_(this.f_97735_ + 8, this.f_97736_ + 100, 79, 20).m_253136_());
      this.page(1, Button.m_253074_(Component.m_237113_("Clear"), b -> this.press(8)).m_252987_(this.f_97735_ + 89, this.f_97736_ + 100, 79, 20).m_253136_());
      this.page(
         2, Button.m_253074_(Component.m_237113_("Select door"), b -> this.press(4)).m_252987_(this.f_97735_ + 8, this.f_97736_ + 52, 160, 20).m_253136_()
      );
      this.page(
         2,
         Button.m_253074_(Component.m_237113_("Add / remove seats"), b -> this.press(5)).m_252987_(this.f_97735_ + 8, this.f_97736_ + 92, 110, 20).m_253136_()
      );
      this.page(2, Button.m_253074_(Component.m_237113_("Clear"), b -> this.press(6)).m_252987_(this.f_97735_ + 120, this.f_97736_ + 92, 48, 20).m_253136_());
      this.staffButton = this.page(
         4, Button.m_253074_(Component.m_237119_(), b -> this.press(9)).m_252987_(this.f_97735_ + 8, this.f_97736_ + 66, 160, 20).m_253136_()
      );
      this.refresh();
   }

   private Button page(int page, Button button) {
      this.pages[page].add((Button)this.m_142416_(button));
      return button;
   }

   private void press(int id) {
      if (this.f_96541_ != null && this.f_96541_.f_91072_ != null) {
         this.f_96541_.f_91072_.m_105208_(((CashRegisterMenu)this.f_97732_).f_38840_, id);
      }
   }

   public void selectTab(int page) {
      tab = Math.max(0, Math.min(4, page));
      this.refresh();
   }

   private void refresh() {
      for (int i = 0; i < 5; i++) {
         this.tabs.get(i).f_93623_ = i != tab;

         for (Button b : this.pages[i]) {
            b.f_93624_ = i == tab;
         }
      }
   }

   protected void m_181908_() {
      super.m_181908_();
      this.openButton.m_93666_(Component.m_237113_(((CashRegisterMenu)this.f_97732_).open() ? "Close the store" : "Open the store"));
      this.less.f_93623_ = ((CashRegisterMenu)this.f_97732_).capacity() > 1;
      this.more.f_93623_ = ((CashRegisterMenu)this.f_97732_).capacity() < ((CashRegisterMenu)this.f_97732_).maxCapacity();
      this.collect.f_93623_ = ((CashRegisterMenu)this.f_97732_).till() > 0;
      this.staffButton.m_93666_(Component.m_237113_(((CashRegisterMenu)this.f_97732_).staffOff() ? "Call the barkeep in" : "Send the barkeep home"));
      this.staffButton.f_93623_ = ((CashRegisterMenu)this.f_97732_).barkeeps() > 0;
   }

   protected void m_7286_(GuiGraphics g, float partial, int mouseX, int mouseY) {
      g.m_280218_(TEXTURE, this.f_97735_, this.f_97736_, 0, 0, this.f_97726_, this.f_97727_);
      if (tab == 1) {
         for (int i = 0; i < BarProducts.ALL.size(); i++) {
            int x = this.f_97735_ + 7 + i % 9 * 18;
            int y = this.f_97736_ + 39 + i / 9 * 18;
            g.m_280218_(TEXTURE, x, y, 176, 0, 18, 18);
            g.m_280480_(BarProducts.ALL.get(i).icon(), x + 1, y + 1);
            if (!((CashRegisterMenu)this.f_97732_).stocked(i)) {
               g.m_280046_(x + 1, y + 1, x + 17, y + 17, 300, -1333032053);
            } else {
               g.m_280398_(TEXTURE, x + 11, y + 1, 300, 194.0F, 0.0F, 6, 6, 256, 256);
            }
         }
      }
   }

   protected void m_280003_(GuiGraphics g, int mouseX, int mouseY) {
      g.m_280614_(this.f_96547_, this.f_96539_, 8, 6, 4210752, false);
      switch (tab) {
         case 0:
            g.m_280056_(this.f_96547_, "Store is ", 8, 40, 4210752, false);
            g.m_280056_(
               this.f_96547_,
               ((CashRegisterMenu)this.f_97732_).open() ? "OPEN" : "CLOSED",
               8 + this.f_96547_.m_92895_("Store is "),
               40,
               ((CashRegisterMenu)this.f_97732_).open() ? 3046706 : 12000284,
               false
            );
            g.m_280056_(this.f_96547_, "Max customers: " + ((CashRegisterMenu)this.f_97732_).capacity(), 8, 85, 4210752, false);
            g.m_280056_(this.f_96547_, "Customers inside: " + ((CashRegisterMenu)this.f_97732_).patrons(), 8, 104, 4210752, false);
            g.m_280056_(this.f_96547_, "Till: $" + ((CashRegisterMenu)this.f_97732_).till(), 8, 130, 4210752, false);
            if (((CashRegisterMenu)this.f_97732_).open() && (!((CashRegisterMenu)this.f_97732_).hasDoor() || this.stockedCount() == 0)) {
               g.m_280056_(
                  this.f_96547_,
                  !((CashRegisterMenu)this.f_97732_).hasDoor() ? "No door set (Seating tab)" : "Nothing in stock (Stock tab)",
                  8,
                  152,
                  12000284,
                  false
               );
            }
            break;
         case 1:
            g.m_280056_(this.f_96547_, this.stockedCount() + " of " + BarProducts.ALL.size() + " in stock", 8, 126, 4210752, false);
            g.m_280056_(this.f_96547_, "Customers only order these.", 8, 140, 7368816, false);
            break;
         case 2:
         default:
            g.m_280056_(
               this.f_96547_,
               ((CashRegisterMenu)this.f_97732_).hasDoor() ? "Door: linked" : "Door: none yet",
               8,
               40,
               ((CashRegisterMenu)this.f_97732_).hasDoor() ? 4210752 : 12000284,
               false
            );
            g.m_280056_(this.f_96547_, "Seats: " + ((CashRegisterMenu)this.f_97732_).seats(), 8, 80, 4210752, false);
            g.m_280056_(this.f_96547_, "Pick a wooden door, then bar", 8, 120, 7368816, false);
            g.m_280056_(this.f_96547_, "stools or stairs to sit on.", 8, 131, 7368816, false);
            g.m_280056_(this.f_96547_, "Sneak-click when done.", 8, 142, 7368816, false);
            break;
         case 3:
            this.pub(g);
            break;
         case 4:
            this.staff(g);
      }
   }

   private void pub(GuiGraphics g) {
      int rep = ((CashRegisterMenu)this.f_97732_).reputation();
      int stars = Math.max(1, Math.min(5, (rep + 10) / 20));
      g.m_280056_(this.f_96547_, "Reputation ", 8, 39, 4210752, false);
      int x = 8 + this.f_96547_.m_92895_("Reputation ");

      for (int i = 0; i < 5; i++) {
         g.m_280056_(this.f_96547_, "★", x, 39, i < stars ? 14723072 : 10526880, false);
         x += this.f_96547_.m_92895_("★") + 1;
      }

      g.m_280056_(
         this.f_96547_,
         rep >= 80
            ? "Everyone's talking about it"
            : (rep >= 60 ? "A popular spot" : (rep >= 40 ? "A decent local" : (rep >= 20 ? "Word is it's slow" : "Nobody wants to come"))),
         8,
         50,
         7368816,
         false
      );
      g.m_280056_(this.f_96547_, "Today: " + ((CashRegisterMenu)this.f_97732_).servedToday() + " served", 8, 64, 4210752, false);
      g.m_280056_(
         this.f_96547_,
         "Takings $" + ((CashRegisterMenu)this.f_97732_).takingsToday() + " · tips $" + ((CashRegisterMenu)this.f_97732_).tipsToday(),
         8,
         75,
         4210752,
         false
      );
      if (((CashRegisterMenu)this.f_97732_).walkoutsToday() > 0) {
         g.m_280056_(this.f_96547_, ((CashRegisterMenu)this.f_97732_).walkoutsToday() + " walked out waiting", 8, 86, 12000284, false);
      }

      g.m_280056_(this.f_96547_, "Happy customers come back.", 8, 104, 7368816, false);
   }

   private void staff(GuiGraphics g) {
      int n = ((CashRegisterMenu)this.f_97732_).barkeeps();
      if (n == 0) {
         g.m_280056_(this.f_96547_, "No barkeep hired yet.", 8, 40, 4210752, false);
         g.m_280056_(this.f_96547_, "Hire one from a safe's", 8, 98, 7368816, false);
         g.m_280056_(this.f_96547_, "Upgrades page.", 8, 109, 7368816, false);
      } else {
         g.m_280056_(this.f_96547_, (n == 1 ? "1 barkeep" : n + " barkeeps") + " · ", 8, 40, 4210752, false);
         g.m_280056_(
            this.f_96547_,
            ((CashRegisterMenu)this.f_97732_).staffOff() ? "SENT HOME" : "ON SHIFT",
            8 + this.f_96547_.m_92895_((n == 1 ? "1 barkeep" : n + " barkeeps") + " · "),
            40,
            ((CashRegisterMenu)this.f_97732_).staffOff() ? 12000284 : 3046706,
            false
         );
         g.m_280056_(
            this.f_96547_, ((CashRegisterMenu)this.f_97732_).staffOff() ? "Off duty: no wages today." : "They serve beer and shots.", 8, 52, 7368816, false
         );
         g.m_280056_(this.f_96547_, "Wages: $" + ((CashRegisterMenu)this.f_97732_).wages() + " a day, at dawn", 8, 96, 4210752, false);
         g.m_280056_(
            this.f_96547_,
            ((CashRegisterMenu)this.f_97732_).hasSafe() ? "Safe: $" + ((CashRegisterMenu)this.f_97732_).safeBalance() : "No safe near the bar",
            8,
            108,
            ((CashRegisterMenu)this.f_97732_).hasSafe() ? 4210752 : 12000284,
            false
         );
         g.m_280056_(this.f_96547_, "Till: $" + ((CashRegisterMenu)this.f_97732_).till(), 8, 120, 4210752, false);
         g.m_280056_(this.f_96547_, "Paid from the safe first,", 8, 138, 7368816, false);
         g.m_280056_(this.f_96547_, "then from the till.", 8, 149, 7368816, false);
      }
   }

   private int stockedCount() {
      int n = 0;

      for (int i = 0; i < BarProducts.ALL.size(); i++) {
         if (((CashRegisterMenu)this.f_97732_).stocked(i)) {
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
            int x = this.f_97735_ + 7 + i % 9 * 18;
            int y = this.f_97736_ + 39 + i / 9 * 18;
            if (mouseX >= (double)x && mouseX < (double)(x + 18) && mouseY >= (double)y && mouseY < (double)(y + 18)) {
               return i;
            }
         }

         return -1;
      }
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      int product = this.productAt(mouseX, mouseY);
      if (product >= 0 && button == 0) {
         this.press(100 + product);
         if (this.f_96541_ != null) {
            this.f_96541_.m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
         }

         return true;
      } else {
         return super.m_6375_(mouseX, mouseY, button);
      }
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.m_280273_(g);
      super.m_88315_(g, mouseX, mouseY, partial);
      int product = this.productAt((double)mouseX, (double)mouseY);
      if (product >= 0) {
         BarProducts.Product p = BarProducts.ALL.get(product);
         List<Component> lines = new ArrayList<>();
         lines.add(p.name());
         lines.add(
            Component.m_237113_(p.shots() ? "Shot $" + p.shot() + " ·Bottle $" + p.whole() : priceLabel(p) + " $" + p.whole()).m_130940_(ChatFormatting.GRAY)
         );
         lines.add(
            Component.m_237113_(((CashRegisterMenu)this.f_97732_).stocked(product) ? "In stock · click to remove" : "Not stocked · click to add")
               .m_130940_(((CashRegisterMenu)this.f_97732_).stocked(product) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
         );
         g.m_280666_(this.f_96547_, lines, mouseX, mouseY);
      } else {
         this.m_280072_(g, mouseX, mouseY);
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
