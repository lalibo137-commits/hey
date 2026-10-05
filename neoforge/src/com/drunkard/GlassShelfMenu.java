package com.drunkard;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GlassShelfMenu extends AbstractContainerMenu {
   private final Container shelf;

   public GlassShelfMenu(int id, Inventory inventory) {
      this(id, inventory, new SimpleContainer(9));
   }

   public GlassShelfMenu(int id, Inventory inventory, Container shelf) {
      super((MenuType)Drunkard.GLASS_SHELF_MENU.get(), id);
      this.shelf = shelf;
      shelf.startOpen(inventory.player);

      for (int i = 0; i < 9; i++) {
         this.addSlot(new Slot(shelf, i, 8 + i * 18, 18) {
            public boolean mayPlace(ItemStack stack) {
               return GlassShelfEntity.glassware(stack);
            }
         });
      }

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 49 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(inventory, col, 8 + col * 18, 107));
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = (Slot)this.slots.get(index);
      if (!slot.hasItem()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack stack = slot.getItem();
         ItemStack original = stack.copy();
         int shelfEnd = 9;
         if (index < shelfEnd) {
            if (!this.moveItemStackTo(stack, shelfEnd, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!GlassShelfEntity.glassware(stack) || !this.moveItemStackTo(stack, 0, shelfEnd, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         return original;
      }
   }

   public boolean stillValid(Player player) {
      return this.shelf.stillValid(player);
   }

   public void removed(Player player) {
      super.removed(player);
      this.shelf.stopOpen(player);
   }
}
