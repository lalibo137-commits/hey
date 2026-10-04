package com.drunkard;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
      super((BlockEntityType<?>)Drunkard.KETTLE_ENTITY.get(), pos, state);
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
      if (!level.f_46443_) {
         if (kettle.fuel == 0 && (kettle.m_8020_(3).m_150930_(Items.f_42413_) || kettle.m_8020_(3).m_150930_(Items.f_42414_))) {
            kettle.take(3, 1);
            kettle.fuel = 1200;
            kettle.sync();
         }

         if (kettle.water == 0 && kettle.m_8020_(0).m_150930_(Items.f_42447_)) {
            kettle.take(0, 1);
            kettle.water = 4;
            kettle.m_6836_(0, new ItemStack(Items.f_42446_));
         }

         if (kettle.water > 0 && kettle.grain.isEmpty()) {
            ItemStack grain = kettle.m_8020_(1);
            if (!grain.m_41619_() && kettle.m_7013_(1, grain)) {
               kettle.grain = grain.m_150930_(Items.f_41982_)
                  ? "cactus"
                  : (
                     grain.m_150930_(Items.f_42405_)
                        ? "wheat"
                        : (grain.m_150930_((Item)Drunkard.ROASTED_MALT.get()) ? "dark" : (grain.m_150930_((Item)Drunkard.BARLEY.get()) ? "lager" : "ale"))
                  );
               kettle.take(1, 1);
            }
         }

         if (kettle.water > 0
            && !kettle.grain.isEmpty()
            && !kettle.grain.equals("cactus")
            && kettle.hops < (!kettle.grain.equals("ale") && !kettle.grain.equals("pale") ? 1 : 2)
            && kettle.m_8020_(2).m_150930_((Item)Drunkard.HOPS.get())) {
            kettle.take(2, 1);
            kettle.hops++;
         }

         if (kettle.ready() && kettle.m_8020_(4).m_150930_(Items.f_42590_)) {
            String type = kettle.kind();
            ItemStack result = new ItemStack((ItemLike)Drunkard.MUST.get(type).get());
            if (kettle.output(result)) {
               kettle.take(4, 1);
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
               kettle.m_6596_();
            } else {
               kettle.sync();
            }
         }
      }
   }

   @Override
   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128405_("Fuel", this.fuel);
      tag.m_128405_("Water", this.water);
      tag.m_128405_("Mash", this.mash);
      tag.m_128405_("Boil", this.boil);
      tag.m_128405_("Hops", this.hops);
      tag.m_128359_("Grain", this.grain);
   }

   @Override
   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.fuel = Math.max(0, Math.min(1200, tag.m_128451_("Fuel")));
      this.water = Math.max(0, Math.min(4, tag.m_128451_("Water")));
      this.mash = Math.max(0, Math.min(300, tag.m_128451_("Mash")));
      this.boil = this.mash;
      this.hops = Math.max(0, Math.min(2, tag.m_128451_("Hops")));
      this.grain = tag.m_128461_("Grain");
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
