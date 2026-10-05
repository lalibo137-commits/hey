package com.drunkard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BrewKettleEntity extends MachineEntity {
   public static final int MASH_TICKS = 300;
   public static final int BOIL_TICKS = 300;
   public int water = 0;
   public int mash = 0;
   public int boil = 0;
   public int hops = 0;
   public int fuel = 0;
   public String grain = "";

   public BrewKettleEntity(BlockPos pos, BlockState state) {
      super(Drunkard.KETTLE_ENTITY.get(), pos, state);
   }

   @Override
   public boolean isStill() {
      return false;
   }

   @Override
   public int progressValue() {
      return this.mash;
   }

   @Override
   public int progressTotal() {
      return 300;
   }

   @Override
   public int fuelValue() {
      return this.fuel;
   }

   @Override
   public int liquidValue() {
      return this.water;
   }

   @Override
   public int outputValue() {
      return this.ready() ? 1 : 0;
   }

   public boolean heated() {
      return this.fuel > 0;
   }

   public boolean ready() {
      return this.water > 0 && this.mash >= 300 && this.hops >= (!this.grain.equals("ale") && !this.grain.equals("pale") ? 1 : 2) && this.boil >= 300;
   }

   public boolean spiritMashReady() {
      return this.water > 0 && this.mash >= 300 && this.hops == 0 && !this.grain.isEmpty();
   }

   public String kind() {
      return this.grain.equals("wheat") ? "wheat_beer" : (this.grain.equals("dark") ? "dark_lager" : (this.grain.equals("lager") ? "pale_lager" : "pale_ale"));
   }

   public void reset() {
      this.water = 0;
      this.mash = 0;
      this.boil = 0;
      this.hops = 0;
      this.grain = "";
      this.sync();
   }

   public static void tick(Level level, BlockPos pos, BlockState state, BrewKettleEntity kettle) {
      if (!level.isClientSide) {
         if (kettle.fuel == 0 && (kettle.getItem(3).is(Items.COAL) || kettle.getItem(3).is(Items.CHARCOAL))) {
            kettle.take(3, 1);
            kettle.fuel = 1200;
            kettle.sync();
         }

         if (kettle.water == 0 && kettle.getItem(0).is(Items.WATER_BUCKET)) {
            kettle.take(0, 1);
            kettle.water = 4;
            kettle.setItem(0, new ItemStack(Items.BUCKET));
         }

         if (kettle.water > 0 && kettle.grain.isEmpty()) {
            ItemStack grain = kettle.getItem(1);
            if (!grain.isEmpty() && kettle.canPlaceItem(1, grain)) {
               kettle.grain = grain.is(Items.CACTUS)
                  ? "cactus"
                  : (grain.is(Items.WHEAT) ? "wheat" : (grain.is(Drunkard.ROASTED_MALT.get()) ? "dark" : (grain.is(Drunkard.BARLEY.get()) ? "lager" : "ale")));
               kettle.take(1, 1);
            }
         }

         if (kettle.water > 0
            && !kettle.grain.isEmpty()
            && !kettle.grain.equals("cactus")
            && kettle.hops < (!kettle.grain.equals("ale") && !kettle.grain.equals("pale") ? 1 : 2)
            && kettle.getItem(2).is(Drunkard.HOPS.get())) {
            kettle.take(2, 1);
            kettle.hops++;
         }

         if (kettle.ready()) {
            // Wort comes straight out of the kettle; it no longer needs a glass bottle.
            String type = kettle.kind();
            ItemStack result = new ItemStack((ItemLike)Drunkard.MUST.get(type).get());
            if (kettle.output(result)) {
               kettle.water--;
               if (kettle.water == 0) {
                  kettle.reset();
               } else {
                  kettle.sync();
               }
            }
         }

         boolean working = kettle.heated() && kettle.water > 0 && !kettle.grain.isEmpty() && kettle.mash < 300;
         FiredMachineBlock.setBurning(level, pos, working, kettle.water == 0 ? 0 : (kettle.ready() ? 2 : 1));
         if (working) {
            kettle.fuel--;
            kettle.mash++;
            kettle.boil = kettle.mash;
            if (kettle.mash % 20 != 0 && !kettle.ready()) {
               kettle.setChanged();
            } else {
               kettle.sync();
            }
         }
      }
   }

   @Override
   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putInt("Fuel", this.fuel);
      tag.putInt("Water", this.water);
      tag.putInt("Mash", this.mash);
      tag.putInt("Boil", this.boil);
      tag.putInt("Hops", this.hops);
      tag.putString("Grain", this.grain);
   }

   @Override
   public void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.fuel = Math.max(0, Math.min(1200, tag.getInt("Fuel")));
      this.water = Math.max(0, Math.min(4, tag.getInt("Water")));
      this.mash = Math.max(0, Math.min(300, tag.getInt("Mash")));
      this.boil = this.mash;
      this.hops = Math.max(0, Math.min(2, tag.getInt("Hops")));
      this.grain = tag.getString("Grain");
      if (this.grain.equals("agave")) {
         this.grain = "cactus";
      }

      if (this.grain.equals("pale")) {
         this.grain = this.hops >= 2 ? "ale" : "lager";
      }

      if (!this.grain.isEmpty()
         && !this.grain.equals("wheat")
         && !this.grain.equals("dark")
         && !this.grain.equals("lager")
         && !this.grain.equals("ale")
         && !this.grain.equals("cactus")) {
         this.grain = "";
      }
   }
}
