package com.drunkard.compat;

import com.drunkard.Drunkard;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Keeps the JEI pages in line with wort no longer using glass bottles. */
public final class JeiWortRecipes {
   /** Kettle inputs: drops the glass bottles from the wort recipe. */
   public static List<Object> kettleInputs(Object water, Object grain, Object hops, Object fuel, Object bottles) {
      return isBottle(bottles) ? List.of(water, grain, hops, fuel) : List.of(water, grain, hops, fuel, bottles);
   }

   /** Barrel outputs: beer brewed from wort hands back no empty bottle. */
   public static List<Object> pair(Object first, Object second) {
      return isBeer(first) && isBottle(second) ? List.of(first) : List.of(first, second);
   }

   private static boolean isBottle(Object o) {
      return o instanceof ItemStack s && s.m_150930_(Items.f_42590_);
   }

   private static boolean isBeer(Object o) {
      if (o instanceof ItemStack s) {
         for (String beer : Drunkard.BEERS) {
            if (s.m_150930_((Item)Drunkard.WINE.get(beer).get())) {
               return true;
            }
         }
      }

      return false;
   }

   private JeiWortRecipes() {
   }
}
