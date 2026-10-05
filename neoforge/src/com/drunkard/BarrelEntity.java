package com.drunkard;

import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup.Provider;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BarrelEntity extends SyncedEntity implements Container, MenuProvider {
   public static final int FERMENT_TICKS = 1200;
   public static final int BATCH_SIZE = 4;
   private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
   public int progress = 0;
   public int liquid = 0;
   public String grape = "";
   public boolean bottlesReserved = false;
   private ItemStack batchResult = ItemStack.EMPTY;
   public final ContainerData data = new ContainerData() {
      public int get(int i) {
         return switch (i) {
            case 0 -> BarrelEntity.this.progress;
            case 1 -> BarrelEntity.this.liquid;
            case 2 -> Arrays.asList(Drunkard.DRINKS).indexOf(BarrelEntity.this.grape) + 1;
            case 3 -> BarrelEntity.this.bottlesReserved ? 1 : 0;
            default -> 0;
         };
      }

      public void set(int i, int value) {
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

      public int getCount() {
         return 4;
      }
   };

   public BarrelEntity(BlockPos p, BlockState s) {
      super(Drunkard.BARREL_ENTITY.get(), p, s);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, BarrelEntity barrel) {
      if (!level.isClientSide) {
         barrel.work();
      }
   }

   public void work() {
      if (this.level != null && !this.level.isClientSide) {
         for (ItemStack stack : this.items) {
            if (stack.getItem() instanceof WineItem) {
               WineData.initialize(stack, this.level, UUID.randomUUID());
            }
         }

         if (this.batchResult.getItem() instanceof WineItem) {
            WineData.initialize(this.batchResult, this.level, UUID.randomUUID());
         }

         boolean changed = false;
         if (this.grape.isEmpty()) {
            String type = Drunkard.mustType((ItemStack)this.items.get(0));
            if (!type.isEmpty() && ((ItemStack)this.items.get(1)).is(Drunkard.YEAST.get())) {
               this.grape = type;
               this.progress = 0;
               this.liquid = 4;
               ((ItemStack)this.items.get(0)).shrink(1);
               ((ItemStack)this.items.get(1)).shrink(1);
               ItemStack bottles = (ItemStack)this.items.get(2);
               if (isWort(type)) {
                  // Wort is made without a bottle, so there is no empty bottle to hand back.
               } else if (bottles.isEmpty()) {
                  this.items.set(2, new ItemStack(Items.GLASS_BOTTLE));
               } else if (bottles.is(Items.GLASS_BOTTLE) && bottles.getCount() < bottles.getMaxStackSize()) {
                  bottles.grow(1);
               } else {
                  Containers.dropItemStack(
                     this.level,
                     (double)this.worldPosition.getX() + 0.5,
                     (double)(this.worldPosition.getY() + 1),
                     (double)this.worldPosition.getZ() + 0.5,
                     new ItemStack(Items.GLASS_BOTTLE)
                  );
               }

               this.bottlesReserved = false;
               this.batchResult = ItemStack.EMPTY;
               changed = true;
            }
         } else if (this.progress < 1200) {
            this.progress++;
            this.setChanged();
            changed = this.progress == 1200;
         } else if (this.liquid > 0) {
            ItemStack output = (ItemStack)this.items.get(3);
            if (((ItemStack)this.items.get(2)).is(Items.GLASS_BOTTLE)) {
               if (this.batchResult.isEmpty()) {
                  this.batchResult = new ItemStack(
                     Drunkard.SPIRIT_WASH.containsKey(this.grape)
                        ? (ItemLike)Drunkard.SPIRIT_WASH.get(this.grape).get()
                        : (ItemLike)Drunkard.WINE.get(this.grape).get()
                  );
                  if (!Drunkard.SPIRIT_WASH.containsKey(this.grape)) {
                     WineData.initialize(this.batchResult, this.level, UUID.randomUUID());
                  }

                  this.setChanged();
               }

               if (output.isEmpty() || ItemStack.isSameItemSameComponents(output, this.batchResult) && output.getCount() < output.getMaxStackSize()) {
                  ((ItemStack)this.items.get(2)).shrink(1);
                  if (output.isEmpty()) {
                     this.items.set(3, this.batchResult.copy());
                  } else {
                     output.grow(1);
                  }

                  this.liquid--;
                  changed = true;
                  if (this.liquid == 0) {
                     this.grape = "";
                     this.progress = 0;
                     this.bottlesReserved = false;
                     this.batchResult = ItemStack.EMPTY;
                  }
               }
            }
         }

         int stage = this.grape.isEmpty() ? (!((ItemStack)this.items.get(3)).isEmpty() ? 2 : 0) : (this.progress < 1200 ? 1 : 2);
         if ((Integer)this.getBlockState().getValue(BarrelBlock.STAGE) != stage) {
            this.level.setBlock(this.worldPosition, (BlockState)this.getBlockState().setValue(BarrelBlock.STAGE, stage), 3);
         }

         if (changed) {
            this.sync();
         }
      }
   }

   private static boolean isWort(String type) {
      for (String beer : Drunkard.BEERS) {
         if (beer.equals(type)) {
            return true;
         }
      }

      return false;
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      ContainerHelper.saveAllItems(tag, this.items, registries);
      tag.putInt("Progress", this.progress);
      tag.putInt("Liquid", this.liquid);
      tag.putString("Grape", this.grape);
      tag.putBoolean("BottlesReserved", this.bottlesReserved);
      tag.put("BatchResult", this.batchResult.saveOptional(registries));
   }

   public void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.items.clear();
      ContainerHelper.loadAllItems(tag, this.items, registries);
      this.progress = Math.max(0, Math.min(1200, tag.getInt("Progress")));
      this.liquid = Math.max(0, Math.min(4, tag.getInt("Liquid")));
      this.grape = tag.getString("Grape");
      this.bottlesReserved = tag.getBoolean("BottlesReserved");
      this.batchResult = ItemStack.parseOptional(registries, tag.getCompound("BatchResult"));
      if (!Drunkard.MUST.containsKey(this.grape) || this.liquid == 0) {
         this.grape = "";
         this.progress = 0;
         this.liquid = 0;
         this.bottlesReserved = false;
         this.batchResult = ItemStack.EMPTY;
      } else if (this.bottlesReserved) {
         ItemStack bottles = (ItemStack)this.items.get(2);
         if (bottles.isEmpty()) {
            this.items.set(2, new ItemStack(Items.GLASS_BOTTLE, this.liquid));
         } else if (bottles.is(Items.GLASS_BOTTLE)) {
            bottles.grow(this.liquid);
         }

         this.bottlesReserved = false;
      }
   }

   public Component getDisplayName() {
      return Component.translatable("block.drunkard.fermentation_barrel");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
      return new BarrelMenu(id, inv, this, this.data);
   }

   public int getContainerSize() {
      return 4;
   }

   public boolean isEmpty() {
      return this.items.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int i) {
      return (ItemStack)this.items.get(i);
   }

   public ItemStack removeItem(int i, int count) {
      ItemStack result = ContainerHelper.removeItem(this.items, i, count);
      if (!result.isEmpty()) {
         this.setChanged();
      }

      return result;
   }

   public ItemStack removeItemNoUpdate(int i) {
      return ContainerHelper.takeItem(this.items, i);
   }

   public void setItem(int i, ItemStack s) {
      this.items.set(i, s);
      if (s.getCount() > Math.min(this.getMaxStackSize(), s.getMaxStackSize())) {
         s.setCount(Math.min(this.getMaxStackSize(), s.getMaxStackSize()));
      }

      this.setChanged();
   }

   public boolean stillValid(Player p) {
      return this.level != null
         && this.level.getBlockEntity(this.worldPosition) == this
         && p.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) <= 64.0;
   }

   public void clearContent() {
      this.items.clear();
      this.setChanged();
   }

   public boolean canPlaceItem(int i, ItemStack s) {
      return switch (i) {
         case 0 -> !Drunkard.mustType(s).isEmpty();
         case 1 -> s.is(Drunkard.YEAST.get());
         case 2 -> s.is(Items.GLASS_BOTTLE);
         default -> false;
      };
   }
}
