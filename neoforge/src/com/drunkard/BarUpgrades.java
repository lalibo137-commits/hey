package com.drunkard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class BarUpgrades {
   private static List<BarUpgrades.Supply> supplies;

   public static List<BarUpgrades.Supply> supplies() {
      if (supplies == null) {
         List<BarUpgrades.Supply> list = new ArrayList<>();

         for (BarProducts.Product p : BarProducts.ALL) {
            list.add(new BarUpgrades.Supply(p::icon, Math.max(2, p.whole() / 3)));
         }

         list.add(new BarUpgrades.Supply(() -> new ItemStack((ItemLike)Drunkard.SHOT_GLASS.get()), 2));
         list.add(new BarUpgrades.Supply(() -> new ItemStack((ItemLike)Drunkard.BEER_MUG.get()), 3));
         list.add(new BarUpgrades.Supply(() -> new ItemStack((ItemLike)Drunkard.ROCKS_GLASS.get()), 3));
         list.add(new BarUpgrades.Supply(() -> new ItemStack((ItemLike)Drunkard.HIGHBALL_GLASS.get()), 3));
         list.add(new BarUpgrades.Supply(() -> new ItemStack((ItemLike)Drunkard.MARTINI_GLASS.get()), 4));
         list.add(new BarUpgrades.Supply(() -> new ItemStack((ItemLike)Drunkard.MARGARITA_GLASS.get()), 4));
         supplies = list;
      }

      return supplies;
   }

   private BarUpgrades() {
   }

   public static record Supply(Supplier<ItemStack> stack, int price) {
   }

   public static enum Upgrade {
      ADVERTISING("Street sign", "Customers come in a little more often", 0, 300, 800),
      PREMISES("Bigger premises", "Room for 3 more customers", 0, 500, 1200),
      COMFORT("Comfy furnishings", "Customers wait a bit longer before giving up", 0, 400),
      BARKEEP("Hire a barkeep", "Pours draught beer and shots for customers", 40, 600, 1000);

      public final String title;
      public final String description;
      public final int wage;
      private final int[] prices;

      private Upgrade(String title, String description, int wage, int... prices) {
         this.title = title;
         this.description = description;
         this.wage = wage;
         this.prices = prices;
      }

      public int maxLevel() {
         return this.prices.length;
      }

      public int price(int level) {
         return level < this.prices.length ? this.prices[level] : -1;
      }

      public boolean staff() {
         return this.wage > 0;
      }
   }
}
