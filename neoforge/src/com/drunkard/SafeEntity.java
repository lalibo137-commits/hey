package com.drunkard;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity.DataComponentInput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SafeEntity extends SyncedEntity implements MenuProvider {
   public static final int MAX_BALANCE = 999999999;
   public static final int LINK_RANGE = 16;
   public static final int BUTTON_DEPOSIT = 0;
   public static final int BUTTON_WITHDRAW_1 = 1;
   public static final int BUTTON_WITHDRAW_10 = 2;
   public static final int BUTTON_WITHDRAW_64 = 3;
   public static final int BUTTON_WITHDRAW_ALL = 4;
   public static final int BUTTON_TILL = 5;
   public static final int BUTTON_UNLOCK = 6;
   public static final int BUTTON_UPGRADE = 100;
   public static final int BUTTON_SUPPLY = 200;
   public static final int SUPPLIER_PRICE = 5000;
   public static final int DATA_BALANCE_LOW = 0;
   public static final int DATA_BALANCE_HIGH = 1;
   public static final int DATA_LINKED = 2;
   public static final int DATA_TILL_LOW = 3;
   public static final int DATA_TILL_HIGH = 4;
   public static final int DATA_SUPPLIER = 5;
   public static final int DATA_UPGRADES = 6;
   public static final int DATA_COUNT = 6 + BarUpgrades.Upgrade.values().length;
   public int balance;
   public boolean supplier;
   @Nullable
   public UUID owner;
   public final ContainerData data = new ContainerData() {
      public int get(int i) {
         CashRegisterEntity r = SafeEntity.this.register();

         return switch (i) {
            case 0 -> SafeEntity.this.balance & 32767;
            case 1 -> SafeEntity.this.balance >> 15;
            case 2 -> r == null ? 0 : 1;
            case 3 -> r == null ? 0 : r.till & 32767;
            case 4 -> r == null ? 0 : r.till >> 15;
            case 5 -> SafeEntity.this.supplier ? 1 : 0;
            default -> r != null && i - 6 < r.upgrades.length ? r.upgrades[i - 6] : 0;
         };
      }

      public void set(int i, int value) {
      }

      public int getCount() {
         return SafeEntity.DATA_COUNT;
      }
   };
   @Nullable
   private BlockPos registerPos;
   private long registerCheckedAt = -1000L;

   public SafeEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType<?>)Drunkard.SAFE_ENTITY.get(), pos, state);
   }

   public boolean mayUse(Player player) {
      return this.owner == null || this.owner.equals(player.getUUID()) || player.getAbilities().instabuild && player.hasPermissions(2);
   }

   @Nullable
   public CashRegisterEntity register() {
      if (this.level == null) {
         return null;
      } else {
         long now = this.level.getGameTime();
         if (now - this.registerCheckedAt >= 40L) {
            this.registerCheckedAt = now;
            // Stay linked to the same register while it exists, so a second pub nearby never takes over this safe.
            if (this.registerPos != null
               && this.level.isLoaded(this.registerPos)
               && Math.abs(this.registerPos.getX() - this.worldPosition.getX()) <= 16
               && Math.abs(this.registerPos.getY() - this.worldPosition.getY()) <= 4
               && Math.abs(this.registerPos.getZ() - this.worldPosition.getZ()) <= 16
               && this.level.getBlockEntity(this.registerPos) instanceof CashRegisterEntity linked) {
               return linked;
            }

            BlockPos before = this.registerPos;
            this.registerPos = null;
            CashRegisterEntity best = null;
            double bestDistance = Double.MAX_VALUE;

            for (BlockPos p : BlockPos.betweenClosed(this.worldPosition.offset(-16, -4, -16), this.worldPosition.offset(16, 4, 16))) {
               if (this.level.isLoaded(p)) {
                  BlockEntity d = this.level.getBlockEntity(p);
                  if (d instanceof CashRegisterEntity) {
                     CashRegisterEntity r = (CashRegisterEntity)d;
                     double dx = p.distSqr(this.worldPosition);
                     if (dx < bestDistance) {
                        best = r;
                        bestDistance = dx;
                        this.registerPos = p.immutable();
                     }
                  }
               }
            }

            if (before == null ? this.registerPos != null : !before.equals(this.registerPos)) {
               this.setChanged();
            }

            return best;
         } else {
            return this.registerPos != null && this.level.getBlockEntity(this.registerPos) instanceof CashRegisterEntity r ? r : null;
         }
      }
   }

   public void button(Player player, int id) {
      if (this.level != null && this.mayUse(player)) {
         switch (id) {
            case 0:
               this.deposit(player);
               break;
            case 1:
               this.withdraw(player, 1);
               break;
            case 2:
               this.withdraw(player, 10);
               break;
            case 3:
               this.withdraw(player, 64);
               break;
            case 4:
               this.withdraw(player, this.balance);
               break;
            case 5:
               CashRegisterEntity r = this.register();
               if (r != null && r.till > 0) {
                  this.add(r.till);
                  player.displayClientMessage(Component.literal("Moved $" + r.till + " from the till"), true);
                  r.till = 0;
                  r.sync();
               }
               break;
            case 6:
               this.unlockSupplier(player);
               break;
            default:
               if (id >= 200) {
                  this.buySupply(player, id - 200);
               } else if (id >= 100) {
                  this.buyUpgrade(player, id - 100);
               }
         }

         this.sync();
      }
   }

   private void add(int dollars) {
      this.balance = (int)Math.min(999999999L, (long)this.balance + (long)Math.max(0, dollars));
   }

   public void deposit(Player player) {
      int total = 0;

      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s.is((Item)Drunkard.CASH.get())) {
            total += s.getCount();
            player.getInventory().setItem(i, ItemStack.EMPTY);
         }
      }

      if (total == 0) {
         player.displayClientMessage(Component.literal("No cash on you"), true);
      } else {
         this.add(total);
         this.level.playSound(null, this.worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 1.4F);
         player.displayClientMessage(Component.literal("Deposited $" + total), true);
      }
   }

   public void withdraw(Player player, int dollars) {
      dollars = Math.min(dollars, this.balance);
      if (dollars > 0) {
         int given = 0;
         int left = dollars;

         while (left > 0) {
            int n = Math.min(64, left);
            left -= n;
            ItemStack cash = new ItemStack((ItemLike)Drunkard.CASH.get(), n);
            player.getInventory().add(cash);
            given += n - cash.getCount();
            if (!cash.isEmpty()) {
               break;
            }
         }

         this.balance -= given;
         if (given < dollars) {
            player.displayClientMessage(
               Component.literal(given == 0 ? "No room in your inventory" : "Took $" + given + ": your inventory is full, the rest stays in the safe"), true
            );
         }

         if (given != 0) {
            this.level.playSound(null, this.worldPosition, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.4F);
         }
      }
   }

   public boolean buyUpgrade(Player player, int index) {
      if (index >= 0 && index < BarUpgrades.Upgrade.values().length) {
         BarUpgrades.Upgrade u = BarUpgrades.Upgrade.values()[index];
         CashRegisterEntity r = this.register();
         if (r == null) {
            player.displayClientMessage(Component.literal("No cash register within 16 blocks"), true);
            return false;
         } else {
            int price = u.price(r.upgrades[index]);
            if (price < 0) {
               return false;
            } else if (this.balance < price) {
               player.displayClientMessage(Component.literal("Not enough in the safe ($" + price + " needed)"), true);
               return false;
            } else {
               this.balance -= price;
               r.upgrades[index]++;
               r.sync();
               this.level.playSound(null, this.worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.2F);
               player.displayClientMessage(Component.literal(u.title + " bought (level " + r.upgrades[index] + ")"), true);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public boolean unlockSupplier(Player player) {
      if (this.supplier) {
         return false;
      } else if (this.balance < 5000) {
         player.displayClientMessage(Component.literal("Not enough in the safe ($5000 needed)"), true);
         return false;
      } else {
         this.balance -= 5000;
         this.supplier = true;
         this.level.playSound(null, this.worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.4F);
         player.displayClientMessage(Component.literal("Supplier deal signed: wholesale prices unlocked"), true);
         return true;
      }
   }

   public boolean buySupply(Player player, int index) {
      if (!this.supplier) {
         player.displayClientMessage(Component.literal("Sign the supplier's deal first ($5000)"), true);
         return false;
      } else {
         List<BarUpgrades.Supply> list = BarUpgrades.supplies();
         if (index >= 0 && index < list.size()) {
            BarUpgrades.Supply s = list.get(index);
            if (this.balance < s.price()) {
               player.displayClientMessage(Component.literal("Not enough in the safe ($" + s.price() + " needed)"), true);
               return false;
            } else {
               this.balance = this.balance - s.price();
               ItemStack stack = s.stack().get();
               if (!player.getInventory().add(stack)) {
                  player.drop(stack, false);
               }

               this.level.playSound(null, this.worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
               return true;
            }
         } else {
            return false;
         }
      }
   }

   public Component getDisplayName() {
      return Component.translatable("block.drunkard.safe");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      return new SafeMenu(id, inventory, this);
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putInt("Balance", this.balance);
      tag.putBoolean("Supplier", this.supplier);
      if (this.owner != null) {
         tag.putUUID("Owner", this.owner);
      }

      if (this.registerPos != null) {
         tag.putLong("Register", this.registerPos.asLong());
      }
   }

   // Money, owner and the supplier deal stay on the dropped safe (copied by the loot table); the register link does not.
   protected void collectImplicitComponents(Builder components) {
      super.collectImplicitComponents(components);
      CompoundTag data = new CompoundTag();
      data.putInt("Balance", this.balance);
      data.putBoolean("Supplier", this.supplier);
      if (this.owner != null) {
         data.putUUID("Owner", this.owner);
      }

      components.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
   }

   protected void applyImplicitComponents(DataComponentInput input) {
      super.applyImplicitComponents(input);
      CompoundTag data = ((CustomData)input.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      if (data.contains("Balance")) {
         this.balance = Math.max(0, Math.min(999999999, data.getInt("Balance")));
         this.supplier = data.getBoolean("Supplier");
         this.owner = data.hasUUID("Owner") ? data.getUUID("Owner") : null;
      }
   }

   public void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.balance = Math.max(0, Math.min(999999999, tag.getInt("Balance")));
      this.supplier = tag.getBoolean("Supplier");
      this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.registerPos = tag.contains("Register") ? BlockPos.of(tag.getLong("Register")) : null;
   }
}
