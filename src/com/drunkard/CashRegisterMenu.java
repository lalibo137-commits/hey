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
      this.pos = buf.m_130135_();
      this.register = null;
      this.data = new SimpleContainerData(CashRegisterEntity.DATA_COUNT);
      this.m_38884_(this.data);
   }

   public CashRegisterMenu(int id, Inventory inventory, CashRegisterEntity register) {
      super((MenuType)Drunkard.CASH_REGISTER_MENU.get(), id);
      this.pos = register.m_58899_();
      this.register = register;
      this.data = register.data;
      this.m_38884_(this.data);
   }

   public boolean open() {
      return this.data.m_6413_(0) != 0;
   }

   public int capacity() {
      return this.data.m_6413_(1);
   }

   public int till() {
      return this.data.m_6413_(2) | this.data.m_6413_(3) << 15;
   }

   public boolean hasDoor() {
      return this.data.m_6413_(4) != 0;
   }

   public int seats() {
      return this.data.m_6413_(5);
   }

   public int patrons() {
      return this.data.m_6413_(6);
   }

   public int maxCapacity() {
      return this.data.m_6413_(13);
   }

   public int reputation() {
      return this.data.m_6413_(7);
   }

   public int servedToday() {
      return this.data.m_6413_(8);
   }

   public int takingsToday() {
      return this.data.m_6413_(9) | this.data.m_6413_(10) << 15;
   }

   public int tipsToday() {
      return this.data.m_6413_(11);
   }

   public int walkoutsToday() {
      return this.data.m_6413_(12);
   }

   public boolean stocked(int product) {
      return this.data.m_6413_(20 + product) != 0;
   }

   public int style() {
      return this.data.m_6413_(CashRegisterEntity.DATA_STYLE);
   }

   public int barkeeps() {
      return this.data.m_6413_(14);
   }

   public boolean staffOff() {
      return this.data.m_6413_(15) != 0;
   }

   public int wages() {
      return this.data.m_6413_(16);
   }

   public boolean hasSafe() {
      return this.data.m_6413_(19) != 0;
   }

   public int safeBalance() {
      return this.data.m_6413_(17) | this.data.m_6413_(18) << 15;
   }

   public boolean m_6366_(Player player, int id) {
      if (this.register != null && !this.register.m_58901_()) {
         this.register.button(player, id);
         return true;
      } else {
         return false;
      }
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }

   public boolean m_6875_(Player player) {
      return this.register == null
         || !this.register.m_58901_()
            && player.m_20275_((double)this.pos.m_123341_() + 0.5, (double)this.pos.m_123342_() + 0.5, (double)this.pos.m_123343_() + 0.5) <= 64.0;
   }
}
