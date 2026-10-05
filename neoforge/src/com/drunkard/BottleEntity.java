package com.drunkard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class BottleEntity extends SyncedEntity {
   public ItemStack bottle = ItemStack.EMPTY;

   public BottleEntity(BlockPos pos, BlockState state) {
      super(Drunkard.BOTTLE_ENTITY.get(), pos, state);
   }

   public static boolean placeable(ItemStack s) {
      Item item = s.getItem();
      return item instanceof WineItem
         || item instanceof CocktailItem
         || item instanceof CigarettePackItem
         || item instanceof CocktailShakerItem
         || item instanceof BarwareItem
         || s.is(Items.GLASS_BOTTLE);
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("Bottle", this.bottle.saveOptional(registries));
   }

   public void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.bottle = ItemStack.parseOptional(registries, tag.getCompound("Bottle"));
      if (!placeable(this.bottle)) {
         this.bottle = ItemStack.EMPTY;
      } else {
         this.bottle.setCount(1);
      }
   }
}
