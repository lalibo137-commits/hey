package com.drunkard;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class PatronMenu extends AbstractContainerMenu {
   public static final int BUTTON_SERVE = 0;
   public static final int BUTTON_REFUSE = 1;
   public static final int BUTTON_SHOW_OUT = 2;
   public static final int DATA_ORDER = 0;
   public static final int DATA_SHOT = 1;
   public static final int DATA_STATE = 2;
   public static final int DATA_COUNT = 3;
   public final ContainerData data;
   public final int entityId;
   private final BarPatron patron;

   public PatronMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
      super((MenuType)Drunkard.PATRON_MENU.get(), id);
      this.entityId = buf.readVarInt();
      this.patron = null;
      this.data = new SimpleContainerData(3);
      this.addDataSlots(this.data);
   }

   public PatronMenu(int id, Inventory inventory, final BarPatron patron) {
      super((MenuType)Drunkard.PATRON_MENU.get(), id);
      this.entityId = patron.getId();
      this.patron = patron;
      this.data = new ContainerData() {
         public int get(int i) {
            return switch (i) {
               case 0 -> patron.order();
               case 1 -> patron.wantsShot() ? 1 : 0;
               default -> patron.state().ordinal();
            };
         }

         public void set(int i, int value) {
         }

         public int getCount() {
            return 3;
         }
      };
      this.addDataSlots(this.data);
   }

   public BarProducts.Product product() {
      return BarProducts.get(this.data.get(0));
   }

   public boolean shot() {
      return this.data.get(1) != 0;
   }

   public BarPatron.State state() {
      return BarPatron.State.values()[Math.max(0, Math.min(BarPatron.State.values().length - 1, this.data.get(2)))];
   }

   public boolean clickMenuButton(Player player, int id) {
      if (this.patron != null && this.patron.isAlive()) {
         if (id == 0) {
            if (this.patron.serve(player)) {
               player.closeContainer();
            }
         } else if (id == 1) {
            this.patron.refuse();
            player.closeContainer();
         } else if (id == 2) {
            this.patron.showOut();
            player.closeContainer();
         }

         return true;
      } else {
         return false;
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   public boolean stillValid(Player player) {
      return this.patron == null || this.patron.isAlive() && player.distanceToSqr(this.patron) <= 64.0;
   }
}
