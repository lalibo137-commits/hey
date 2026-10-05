package com.drunkard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GlassShelfEntity extends SyncedEntity implements Container, MenuProvider {
   public static final int SLOTS = 9;
   public final NonNullList<ItemStack> glasses = NonNullList.withSize(9, ItemStack.EMPTY);

   public GlassShelfEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType<?>)Drunkard.GLASS_SHELF_ENTITY.get(), pos, state);
   }

   public static boolean glassware(ItemStack s) {
      return s.is((Item)Drunkard.BEER_MUG.get())
         || s.is((Item)Drunkard.SHOT_GLASS.get()) && !ShotGlassItem.filled(s)
         || s.is((Item)Drunkard.ROCKS_GLASS.get())
         || s.is((Item)Drunkard.HIGHBALL_GLASS.get())
         || s.is((Item)Drunkard.MARTINI_GLASS.get())
         || s.is((Item)Drunkard.MARGARITA_GLASS.get());
   }

   public int count(Item item) {
      int n = 0;

      for (ItemStack s : this.glasses) {
         if (s.is(item)) {
            n += s.getCount();
         }
      }

      return n;
   }

   public ItemStack take(Item item) {
      for (int i = 0; i < 9; i++) {
         if (((ItemStack)this.glasses.get(i)).is(item)) {
            return this.takeFrom(i);
         }
      }

      return ItemStack.EMPTY;
   }

   public ItemStack takeFrom(int slot) {
      if (slot >= 0 && slot < 9 && !((ItemStack)this.glasses.get(slot)).isEmpty()) {
         ItemStack one = ((ItemStack)this.glasses.get(slot)).split(1);
         this.sync();
         return one;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public boolean insert(ItemStack stack, int preferred) {
      if (!glassware(stack)) {
         return false;
      } else {
         int before = stack.getCount();
         if (preferred >= 0 && preferred < 9) {
            this.merge(preferred, stack);
         }

         for (int i = 0; i < 9 && !stack.isEmpty(); i++) {
            if (((ItemStack)this.glasses.get(i)).is(stack.getItem())) {
               this.merge(i, stack);
            }
         }

         for (int ix = 0; ix < 9 && !stack.isEmpty(); ix++) {
            if (((ItemStack)this.glasses.get(ix)).isEmpty()) {
               this.merge(ix, stack);
            }
         }

         if (stack.getCount() != before) {
            this.sync();
         }

         return stack.getCount() != before;
      }
   }

   private void merge(int i, ItemStack stack) {
      ItemStack slot = (ItemStack)this.glasses.get(i);
      if (slot.isEmpty()) {
         this.glasses.set(i, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
      } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
         int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
         slot.grow(move);
         stack.shrink(move);
      }
   }

   public int getContainerSize() {
      return 9;
   }

   public boolean isEmpty() {
      for (ItemStack s : this.glasses) {
         if (!s.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack getItem(int slot) {
      return (ItemStack)this.glasses.get(slot);
   }

   public ItemStack removeItem(int slot, int count) {
      ItemStack s = ContainerHelper.removeItem(this.glasses, slot, count);
      if (!s.isEmpty()) {
         this.sync();
      }

      return s;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      return ContainerHelper.takeItem(this.glasses, slot);
   }

   public void setItem(int slot, ItemStack stack) {
      this.glasses.set(slot, stack);
      this.sync();
   }

   public boolean canPlaceItem(int slot, ItemStack stack) {
      return glassware(stack);
   }

   public boolean stillValid(Player player) {
      return Container.stillValidBlockEntity(this, player);
   }

   public void clearContent() {
      this.glasses.clear();
      this.sync();
   }

   public void setChanged() {
      super.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public Component getDisplayName() {
      return Component.translatable("block.drunkard.glass_shelf");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      return new GlassShelfMenu(id, inventory, this);
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      ContainerHelper.saveAllItems(tag, this.glasses, registries);
   }

   public void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.glasses.clear();
      ContainerHelper.loadAllItems(tag, this.glasses, registries);
   }
}
