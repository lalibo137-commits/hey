package com.drunkard;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
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
      public int m_6413_(int i) {
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

      public void m_8050_(int i, int value) {
      }

      public int m_6499_() {
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
      return this.owner == null || this.owner.equals(player.m_20148_()) || player.m_150110_().f_35937_ && player.m_20310_(2);
   }

   @Nullable
   public CashRegisterEntity register() {
      if (this.f_58857_ == null) {
         return null;
      } else {
         long now = this.f_58857_.m_46467_();
         if (now - this.registerCheckedAt >= 40L) {
            this.registerCheckedAt = now;
            // Stay linked to the same register while it exists, so a second pub nearby never takes over this safe.
            if (this.registerPos != null
               && this.f_58857_.m_46749_(this.registerPos)
               && Math.abs(this.registerPos.m_123341_() - this.f_58858_.m_123341_()) <= 16
               && Math.abs(this.registerPos.m_123342_() - this.f_58858_.m_123342_()) <= 4
               && Math.abs(this.registerPos.m_123343_() - this.f_58858_.m_123343_()) <= 16
               && this.f_58857_.m_7702_(this.registerPos) instanceof CashRegisterEntity linked) {
               return linked;
            }

            BlockPos before = this.registerPos;
            this.registerPos = null;
            CashRegisterEntity best = null;
            double bestDistance = Double.MAX_VALUE;

            for (BlockPos p : BlockPos.m_121940_(this.f_58858_.m_7918_(-16, -4, -16), this.f_58858_.m_7918_(16, 4, 16))) {
               if (this.f_58857_.m_46749_(p)) {
                  BlockEntity d = this.f_58857_.m_7702_(p);
                  if (d instanceof CashRegisterEntity) {
                     CashRegisterEntity r = (CashRegisterEntity)d;
                     double dx = p.m_123331_(this.f_58858_);
                     if (dx < bestDistance) {
                        best = r;
                        bestDistance = dx;
                        this.registerPos = p.m_7949_();
                     }
                  }
               }
            }

            if (before == null ? this.registerPos != null : !before.equals(this.registerPos)) {
               this.m_6596_();
            }

            return best;
         } else {
            return this.registerPos != null && this.f_58857_.m_7702_(this.registerPos) instanceof CashRegisterEntity r ? r : null;
         }
      }
   }

   public void button(Player player, int id) {
      if (this.f_58857_ != null && this.mayUse(player)) {
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
                  player.m_5661_(Component.m_237113_("Moved $" + r.till + " from the till"), true);
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

      for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
         ItemStack s = player.m_150109_().m_8020_(i);
         if (s.m_150930_((Item)Drunkard.CASH.get())) {
            total += s.m_41613_();
            player.m_150109_().m_6836_(i, ItemStack.f_41583_);
         }
      }

      if (total == 0) {
         player.m_5661_(Component.m_237113_("No cash on you"), true);
      } else {
         this.add(total);
         this.f_58857_.m_5594_(null, this.f_58858_, SoundEvents.f_12011_, SoundSource.BLOCKS, 0.5F, 1.4F);
         player.m_5661_(Component.m_237113_("Deposited $" + total), true);
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
            player.m_150109_().m_36054_(cash);
            given += n - cash.m_41613_();
            if (!cash.m_41619_()) {
               break;
            }
         }

         this.balance -= given;
         if (given < dollars) {
            player.m_5661_(
               Component.m_237113_(given == 0 ? "No room in your inventory" : "Took $" + given + ": your inventory is full, the rest stays in the safe"), true
            );
         }

         if (given != 0) {
            this.f_58857_.m_5594_(null, this.f_58858_, SoundEvents.f_12012_, SoundSource.BLOCKS, 0.5F, 1.4F);
         }
      }
   }

   public boolean buyUpgrade(Player player, int index) {
      if (index >= 0 && index < BarUpgrades.Upgrade.values().length) {
         BarUpgrades.Upgrade u = BarUpgrades.Upgrade.values()[index];
         CashRegisterEntity r = this.register();
         if (r == null) {
            player.m_5661_(Component.m_237113_("No cash register within 16 blocks"), true);
            return false;
         } else {
            int price = u.price(r.upgrades[index]);
            if (price < 0) {
               return false;
            } else if (this.balance < price) {
               player.m_5661_(Component.m_237113_("Not enough in the safe ($" + price + " needed)"), true);
               return false;
            } else {
               this.balance -= price;
               r.upgrades[index]++;
               r.sync();
               this.f_58857_.m_5594_(null, this.f_58858_, SoundEvents.f_12275_, SoundSource.BLOCKS, 0.6F, 1.2F);
               player.m_5661_(Component.m_237113_(u.title + " bought (level " + r.upgrades[index] + ")"), true);
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
         player.m_5661_(Component.m_237113_("Not enough in the safe ($5000 needed)"), true);
         return false;
      } else {
         this.balance -= 5000;
         this.supplier = true;
         this.f_58857_.m_5594_(null, this.f_58858_, SoundEvents.f_12275_, SoundSource.BLOCKS, 0.6F, 1.4F);
         player.m_5661_(Component.m_237113_("Supplier deal signed: wholesale prices unlocked"), true);
         return true;
      }
   }

   public boolean buySupply(Player player, int index) {
      if (!this.supplier) {
         player.m_5661_(Component.m_237113_("Sign the supplier's deal first ($5000)"), true);
         return false;
      } else {
         List<BarUpgrades.Supply> list = BarUpgrades.supplies();
         if (index >= 0 && index < list.size()) {
            BarUpgrades.Supply s = list.get(index);
            if (this.balance < s.price()) {
               player.m_5661_(Component.m_237113_("Not enough in the safe ($" + s.price() + " needed)"), true);
               return false;
            } else {
               this.balance = this.balance - s.price();
               ItemStack stack = s.stack().get();
               if (!player.m_150109_().m_36054_(stack)) {
                  player.m_36176_(stack, false);
               }

               this.f_58857_.m_5594_(null, this.f_58858_, SoundEvents.f_12019_, SoundSource.BLOCKS, 0.5F, 1.0F);
               return true;
            }
         } else {
            return false;
         }
      }
   }

   public Component m_5446_() {
      return Component.m_237115_("block.drunkard.safe");
   }

   public AbstractContainerMenu m_7208_(int id, Inventory inventory, Player player) {
      return new SafeMenu(id, inventory, this);
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128405_("Balance", this.balance);
      tag.m_128379_("Supplier", this.supplier);
      if (this.owner != null) {
         tag.m_128362_("Owner", this.owner);
      }

      if (this.registerPos != null) {
         tag.m_128356_("Register", this.registerPos.m_121878_());
      }
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.balance = Math.max(0, Math.min(999999999, tag.m_128451_("Balance")));
      this.supplier = tag.m_128471_("Supplier");
      this.owner = tag.m_128403_("Owner") ? tag.m_128342_("Owner") : null;
      this.registerPos = tag.m_128441_("Register") ? BlockPos.m_122022_(tag.m_128454_("Register")) : null;
   }
}
