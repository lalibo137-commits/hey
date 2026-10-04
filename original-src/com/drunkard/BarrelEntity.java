package com.drunkard;

import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BarrelEntity extends SyncedEntity implements Container, MenuProvider {
   public static final int FERMENT_TICKS = 1200;
   public static final int BATCH_SIZE = 4;
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(4, ItemStack.f_41583_);
   public int progress = 0;
   public int liquid = 0;
   public String grape = "";
   public boolean bottlesReserved = false;
   private ItemStack batchResult = ItemStack.f_41583_;
   public final ContainerData data = new ContainerData() {
      public int m_6413_(int i) {
         return switch (i) {
            case 0 -> BarrelEntity.this.progress;
            case 1 -> BarrelEntity.this.liquid;
            case 2 -> Arrays.asList(Drunkard.DRINKS).indexOf(BarrelEntity.this.grape) + 1;
            case 3 -> BarrelEntity.this.bottlesReserved ? 1 : 0;
            default -> 0;
         };
      }

      public void m_8050_(int i, int value) {
         switch (i) {
            case 0:
               BarrelEntity.this.progress = value;
               break;
            case 1:
               BarrelEntity.this.liquid = value;
               break;
            case 2:
               BarrelEntity.this.grape = value > 0 && value <= Drunkard.DRINKS.length ? Drunkard.DRINKS[value - 1] : "";
               break;
            case 3:
               BarrelEntity.this.bottlesReserved = value != 0;
         }
      }

      public int m_6499_() {
         return 4;
      }
   };

   public BarrelEntity(BlockPos p, BlockState s) {
      super((BlockEntityType<?>)Drunkard.BARREL_ENTITY.get(), p, s);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, BarrelEntity barrel) {
      if (!level.f_46443_) {
         barrel.work();
      }
   }

   public void work() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         for (ItemStack stack : this.items) {
            if (stack.m_41720_() instanceof WineItem) {
               WineData.initialize(stack, this.f_58857_, UUID.randomUUID());
            }
         }

         if (this.batchResult.m_41720_() instanceof WineItem) {
            WineData.initialize(this.batchResult, this.f_58857_, UUID.randomUUID());
         }

         boolean changed = false;
         if (this.grape.isEmpty()) {
            String type = Drunkard.mustType((ItemStack)this.items.get(0));
            if (!type.isEmpty() && ((ItemStack)this.items.get(1)).m_150930_((Item)Drunkard.YEAST.get())) {
               this.grape = type;
               this.progress = 0;
               this.liquid = 4;
               ((ItemStack)this.items.get(0)).m_41774_(1);
               ((ItemStack)this.items.get(1)).m_41774_(1);
               ItemStack bottles = (ItemStack)this.items.get(2);
               if (bottles.m_41619_()) {
                  this.items.set(2, new ItemStack(Items.f_42590_));
               } else if (bottles.m_150930_(Items.f_42590_) && bottles.m_41613_() < bottles.m_41741_()) {
                  bottles.m_41769_(1);
               } else {
                  Containers.m_18992_(
                     this.f_58857_,
                     (double)this.f_58858_.m_123341_() + 0.5,
                     (double)(this.f_58858_.m_123342_() + 1),
                     (double)this.f_58858_.m_123343_() + 0.5,
                     new ItemStack(Items.f_42590_)
                  );
               }

               this.bottlesReserved = false;
               this.batchResult = ItemStack.f_41583_;
               changed = true;
            }
         } else if (this.progress < 1200) {
            this.progress++;
            this.m_6596_();
            changed = this.progress == 1200;
         } else if (this.liquid > 0) {
            ItemStack output = (ItemStack)this.items.get(3);
            if (((ItemStack)this.items.get(2)).m_150930_(Items.f_42590_)) {
               if (this.batchResult.m_41619_()) {
                  this.batchResult = new ItemStack(
                     Drunkard.SPIRIT_WASH.containsKey(this.grape)
                        ? (ItemLike)Drunkard.SPIRIT_WASH.get(this.grape).get()
                        : (ItemLike)Drunkard.WINE.get(this.grape).get()
                  );
                  if (!Drunkard.SPIRIT_WASH.containsKey(this.grape)) {
                     WineData.initialize(this.batchResult, this.f_58857_, UUID.randomUUID());
                  }

                  this.m_6596_();
               }

               if (output.m_41619_() || ItemStack.m_150942_(output, this.batchResult) && output.m_41613_() < output.m_41741_()) {
                  ((ItemStack)this.items.get(2)).m_41774_(1);
                  if (output.m_41619_()) {
                     this.items.set(3, this.batchResult.m_41777_());
                  } else {
                     output.m_41769_(1);
                  }

                  this.liquid--;
                  changed = true;
                  if (this.liquid == 0) {
                     this.grape = "";
                     this.progress = 0;
                     this.bottlesReserved = false;
                     this.batchResult = ItemStack.f_41583_;
                  }
               }
            }
         }

         int stage = this.grape.isEmpty() ? (!((ItemStack)this.items.get(3)).m_41619_() ? 2 : 0) : (this.progress < 1200 ? 1 : 2);
         if ((Integer)this.m_58900_().m_61143_(BarrelBlock.STAGE) != stage) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)this.m_58900_().m_61124_(BarrelBlock.STAGE, stage), 3);
         }

         if (changed) {
            this.sync();
         }
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.items);
      tag.m_128405_("Progress", this.progress);
      tag.m_128405_("Liquid", this.liquid);
      tag.m_128359_("Grape", this.grape);
      tag.m_128379_("BottlesReserved", this.bottlesReserved);
      tag.m_128365_("BatchResult", this.batchResult.m_41739_(new CompoundTag()));
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.items.clear();
      ContainerHelper.m_18980_(tag, this.items);
      this.progress = Math.max(0, Math.min(1200, tag.m_128451_("Progress")));
      this.liquid = Math.max(0, Math.min(4, tag.m_128451_("Liquid")));
      this.grape = tag.m_128461_("Grape");
      this.bottlesReserved = tag.m_128471_("BottlesReserved");
      this.batchResult = ItemStack.m_41712_(tag.m_128469_("BatchResult"));
      if (!Drunkard.MUST.containsKey(this.grape) || this.liquid == 0) {
         this.grape = "";
         this.progress = 0;
         this.liquid = 0;
         this.bottlesReserved = false;
         this.batchResult = ItemStack.f_41583_;
      } else if (this.bottlesReserved) {
         ItemStack bottles = (ItemStack)this.items.get(2);
         if (bottles.m_41619_()) {
            this.items.set(2, new ItemStack(Items.f_42590_, this.liquid));
         } else if (bottles.m_150930_(Items.f_42590_)) {
            bottles.m_41769_(this.liquid);
         }

         this.bottlesReserved = false;
      }
   }

   public Component m_5446_() {
      return Component.m_237115_("block.drunkard.fermentation_barrel");
   }

   public AbstractContainerMenu m_7208_(int id, Inventory inv, Player p) {
      return new BarrelMenu(id, inv, this, this.data);
   }

   public int m_6643_() {
      return 4;
   }

   public boolean m_7983_() {
      return this.items.stream().allMatch(ItemStack::m_41619_);
   }

   public ItemStack m_8020_(int i) {
      return (ItemStack)this.items.get(i);
   }

   public ItemStack m_7407_(int i, int count) {
      ItemStack result = ContainerHelper.m_18969_(this.items, i, count);
      if (!result.m_41619_()) {
         this.m_6596_();
      }

      return result;
   }

   public ItemStack m_8016_(int i) {
      return ContainerHelper.m_18966_(this.items, i);
   }

   public void m_6836_(int i, ItemStack s) {
      this.items.set(i, s);
      if (s.m_41613_() > Math.min(this.m_6893_(), s.m_41741_())) {
         s.m_41764_(Math.min(this.m_6893_(), s.m_41741_()));
      }

      this.m_6596_();
   }

   public boolean m_6542_(Player p) {
      return this.f_58857_ != null
         && this.f_58857_.m_7702_(this.f_58858_) == this
         && p.m_20275_((double)this.f_58858_.m_123341_() + 0.5, (double)this.f_58858_.m_123342_() + 0.5, (double)this.f_58858_.m_123343_() + 0.5) <= 64.0;
   }

   public void m_6211_() {
      this.items.clear();
      this.m_6596_();
   }

   public boolean m_7013_(int i, ItemStack s) {
      return switch (i) {
         case 0 -> !Drunkard.mustType(s).isEmpty();
         case 1 -> s.m_150930_((Item)Drunkard.YEAST.get());
         case 2 -> s.m_150930_(Items.f_42590_);
         default -> false;
      };
   }
}
