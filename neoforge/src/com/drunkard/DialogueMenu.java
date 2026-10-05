package com.drunkard;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class DialogueMenu extends AbstractContainerMenu {
   public static final int DATA_NODE = 0;
   public static final int DATA_COUNT = 1;
   public final ContainerData data;
   public final int entityId;
   private final BarPatron patron;

   public DialogueMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
      super((MenuType)Drunkard.DIALOGUE_MENU.get(), id);
      this.entityId = buf.readVarInt();
      this.patron = null;
      this.data = new SimpleContainerData(1);
      this.data.set(0, buf.readVarInt());
      this.addDataSlots(this.data);
   }

   public DialogueMenu(int id, Inventory inventory, BarPatron patron, int node) {
      super((MenuType)Drunkard.DIALOGUE_MENU.get(), id);
      this.entityId = patron.getId();
      this.patron = patron;
      this.data = new SimpleContainerData(1);
      this.data.set(0, node);
      this.addDataSlots(this.data);
   }

   public BarDialogue.Node node() {
      return BarDialogue.get(this.data.get(0));
   }

   public boolean clickMenuButton(Player player, int id) {
      if (this.patron == null || !this.patron.isAlive()) {
         return false;
      } else if (id == 91) {
         player.closeContainer();
         return true;
      } else {
         int next = this.patron.answer(player, this.node(), id);
         if (next < 0) {
            player.closeContainer();
         } else {
            this.data.set(0, next);
         }

         return true;
      }
   }

   public void removed(Player player) {
      super.removed(player);
      if (this.patron != null) {
         this.patron.endDialogue(player);
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   public boolean stillValid(Player player) {
      return this.patron == null || this.patron.isAlive() && player.distanceToSqr(this.patron) <= 64.0;
   }
}
