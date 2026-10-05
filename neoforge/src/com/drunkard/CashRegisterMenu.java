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

public class CashRegisterMenu extends AbstractContainerMenu {
   public final ContainerData data;
   public final BlockPos pos;
   private final CashRegisterEntity register;

   public CashRegisterMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
      super((MenuType)Drunkard.CASH_REGISTER_MENU.get(), id);
      this.pos = buf.readBlockPos();
      this.register = null;
      this.data = new SimpleContainerData(CashRegisterEntity.DATA_COUNT);
      this.addDataSlots(this.data);
   }

   public CashRegisterMenu(int id, Inventory inventory, CashRegisterEntity register) {
      super((MenuType)Drunkard.CASH_REGISTER_MENU.get(), id);
      this.pos = register.getBlockPos();
      this.register = register;
      this.data = register.data;
      this.addDataSlots(this.data);
   }

   public boolean open() {
      return this.data.get(0) != 0;
   }

   public int capacity() {
      return this.data.get(1);
   }

   public int till() {
      return this.data.get(2) | this.data.get(3) << 15;
   }

   public boolean hasDoor() {
      return this.data.get(4) != 0;
   }

   public int seats() {
      return this.data.get(5);
   }

   public int patrons() {
      return this.data.get(6);
   }

   public int maxCapacity() {
      return this.data.get(13);
   }

   public int reputation() {
      return this.data.get(7);
   }

   public int servedToday() {
      return this.data.get(8);
   }

   public int takingsToday() {
      return this.data.get(9) | this.data.get(10) << 15;
   }

   public int tipsToday() {
      return this.data.get(11);
   }

   public int walkoutsToday() {
      return this.data.get(12);
   }

   public boolean stocked(int product) {
      return this.data.get(20 + product) != 0;
   }

   public int style() {
      return this.data.get(CashRegisterEntity.DATA_STYLE);
   }

   public int barkeeps() {
      return this.data.get(14);
   }

   public boolean staffOff() {
      return this.data.get(15) != 0;
   }

   public int wages() {
      return this.data.get(16);
   }

   public boolean hasSafe() {
      return this.data.get(19) != 0;
   }

   public int safeBalance() {
      return this.data.get(17) | this.data.get(18) << 15;
   }

   public boolean clickMenuButton(Player player, int id) {
      if (this.register != null && !this.register.isRemoved()) {
         this.register.button(player, id);
         return true;
      } else {
         return false;
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   public boolean stillValid(Player player) {
      return this.register == null
         || !this.register.isRemoved()
            && player.distanceToSqr((double)this.pos.getX() + 0.5, (double)this.pos.getY() + 0.5, (double)this.pos.getZ() + 0.5) <= 64.0;
   }
}
