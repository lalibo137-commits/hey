package com.drunkard;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class SafeMenu extends AbstractContainerMenu {
   public final ContainerData data;
   public final BlockPos pos;
   private final SafeEntity safe;

   public SafeMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
      super((MenuType)Drunkard.SAFE_MENU.get(), id);
      this.pos = buf.readBlockPos();
      this.safe = null;
      this.data = new SimpleContainerData(SafeEntity.DATA_COUNT);
      this.addDataSlots(this.data);
   }

   public SafeMenu(int id, Inventory inventory, SafeEntity safe) {
      super((MenuType)Drunkard.SAFE_MENU.get(), id);
      this.pos = safe.getBlockPos();
      this.safe = safe;
      this.data = safe.data;
      this.addDataSlots(this.data);
   }

   public int balance() {
      return this.data.get(0) | this.data.get(1) << 15;
   }

   public boolean linked() {
      return this.data.get(2) != 0;
   }

   public boolean supplier() {
      return this.data.get(5) != 0;
   }

   public int till() {
      return this.data.get(3) | this.data.get(4) << 15;
   }

   public int upgrade(int index) {
      return this.data.get(6 + index);
   }

   public boolean clickMenuButton(Player player, int id) {
      if (this.safe != null && !this.safe.isRemoved() && this.safe.mayUse(player)) {
         this.safe.button(player, id);
         return true;
      } else {
         return false;
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   public boolean stillValid(Player player) {
      return this.safe == null
         || !this.safe.isRemoved()
            && player.distanceToSqr((double)this.pos.getX() + 0.5, (double)this.pos.getY() + 0.5, (double)this.pos.getZ() + 0.5) <= 64.0;
   }
}
