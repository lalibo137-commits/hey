package com.drunkard;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;

public class BarwareItem extends Item {
   public BarwareItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      return PlacedBottleBlock.place(context);
   }
}
