package com.drunkard;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class BarProducts {
   public static final int COCKTAIL_SIPS = 4;
   public static final List<BarProducts.Product> ALL = List.of(
      spirit("vodka", 7, 150),
      spirit("tequila", 8, 180),
      spirit("whiskey", 9, 200),
      spirit("rum", 7, 140),
      spirit("brandy", 10, 220),
      spirit("moonshine", 8, 60),
      spirit("vermouth", 6, 30),
      beer("pale_lager", 6),
      beer("pale_ale", 7),
      beer("dark_lager", 7),
      beer("wheat_beer", 7),
      wine("grenache", 38),
      wine("merlot", 40),
      wine("chardonnay", 36),
      wine("pinot_noir", 45),
      wine("champagne", 85),
      wine("mead", 28),
      cocktail("martini", () -> (Item)Drunkard.MARTINI.get(), 14),
      cocktail("margarita", () -> (Item)Drunkard.MARGARITA.get(), 12),
      cocktail("old_fashioned", () -> (Item)Drunkard.OLD_FASHIONED.get(), 14),
      cocktail("screwdriver", () -> (Item)Drunkard.SCREWDRIVER.get(), 10),
      cocktail("mojito", () -> (Item)Drunkard.MOJITO.get(), 12),
      cocktail("sangria", () -> (Item)Drunkard.SANGRIA.get(), 11),
      new BarProducts.Product("cigar", BarProducts.Kind.CIGAR, () -> (Item)Drunkard.CIGAR.get(), 0, 18),
      new BarProducts.Product("cigarette_pack", BarProducts.Kind.CIGARETTES, () -> (Item)Drunkard.CIGARETTE_PACK.get(), 0, 14)
   );

   private static Supplier<Item> wine(String type) {
      return () -> (Item)Drunkard.WINE.get(type).get();
   }

   private static BarProducts.Product spirit(String id, int shot, int bottle) {
      return new BarProducts.Product(id, BarProducts.Kind.SPIRIT, wine(id), shot, bottle);
   }

   private static BarProducts.Product beer(String id, int price) {
      return new BarProducts.Product(id, BarProducts.Kind.BEER, wine(id), 0, price);
   }

   private static BarProducts.Product wine(String id, int price) {
      return new BarProducts.Product(id, BarProducts.Kind.WINE, wine(id), 0, price);
   }

   private static BarProducts.Product cocktail(String id, Supplier<Item> item, int price) {
      return new BarProducts.Product(id, BarProducts.Kind.COCKTAIL, item::get, 0, price);
   }

   public static BarProducts.Product get(int index) {
      return index >= 0 && index < ALL.size() ? ALL.get(index) : null;
   }

   public static boolean matches(BarProducts.Product product, ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return switch (product.kind()) {
            case SPIRIT, BEER, WINE -> stack.getItem() instanceof WineItem
            && WineData.variety(stack).equals(product.id())
            && WineData.servingsRemaining(stack) == 10;
            default -> stack.is(product.item().get());
            case CIGAR -> stack.is(product.item().get()) && !CigarItem.lit(stack);
         };
      }
   }

   private BarProducts() {
   }

   public static enum Kind {
      SPIRIT,
      BEER,
      WINE,
      COCKTAIL,
      CIGAR,
      CIGARETTES;
   }

   public static record Product(String id, BarProducts.Kind kind, Supplier<Item> item, int shot, int whole) {
      public boolean shots() {
         return this.shot > 0;
      }

      public boolean wholeOnly() {
         return this.shot == 0;
      }

      public ItemStack icon() {
         return new ItemStack((ItemLike)this.item.get());
      }

      public Component name() {
         return this.item.get().getDescription();
      }

      public int price(boolean asShot) {
         return asShot ? this.shot : this.whole;
      }

      public float drinksPerSip() {
         return switch (this.kind) {
            case SPIRIT, BEER, WINE -> WineData.defaultAlcohol(this.icon()) / 40.0F;
            case COCKTAIL -> this.item.get() instanceof CocktailItem c ? c.strength(this.icon()) / 4.0F / 4.0F : 0.0F;
            default -> 0.0F;
         };
      }

      public int sips(boolean asShot) {
         if (asShot) {
            return 1;
         } else {
            return switch (this.kind) {
               case COCKTAIL -> 4;
               case CIGAR -> 12;
               case CIGARETTES -> 6;
               default -> 10;
            };
         }
      }

      public boolean smoke() {
         return this.kind == BarProducts.Kind.CIGAR || this.kind == BarProducts.Kind.CIGARETTES;
      }
   }
}
