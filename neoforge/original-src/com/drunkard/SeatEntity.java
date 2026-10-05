package com.drunkard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SeatEntity extends Entity {
   public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public static boolean sit(Level level, BlockPos pos, SeatBlock seat, Player player) {
      if (!player.isPassenger() && level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty()) {
         SeatEntity entity = new SeatEntity(Drunkard.SEAT_ENTITY.get(), level);
         float yaw = ((Direction)level.getBlockState(pos).getValue(SeatBlock.FACING)).toYRot();
         entity.moveTo((double)pos.getX() + 0.5, (double)pos.getY() + seat.seatHeight(), (double)pos.getZ() + 0.5, yaw, 0.0F);
         level.addFreshEntity(entity);
         return player.startRiding(entity);
      } else {
         return false;
      }
   }

   protected void defineSynchedData(Builder builder) {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
      return Vec3.ZERO;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.getPassengers().isEmpty() || !(this.level().getBlockState(this.blockPosition()).getBlock() instanceof SeatBlock)) {
            this.discard();
         }
      }
   }

   public void onPassengerTurned(Entity passenger) {
      this.clamp(passenger);
   }

   protected void positionRider(Entity passenger, MoveFunction move) {
      super.positionRider(passenger, move);
      this.clamp(passenger);
   }

   private void clamp(Entity passenger) {
      passenger.setYBodyRot(this.getYRot());
      float offset = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
      float limited = Mth.clamp(offset, -105.0F, 105.0F);
      passenger.yRotO += limited - offset;
      passenger.setYRot(passenger.getYRot() + limited - offset);
      passenger.setYHeadRot(passenger.getYRot());
   }

   public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
      BlockPos seat = this.blockPosition();
      Direction front = Direction.fromYRot((double)this.getYRot());

      for (Direction side : new Direction[]{front, front.getClockWise(), front.getCounterClockWise(), front.getOpposite()}) {
         Vec3 spot = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), seat.relative(side), true);
         if (spot != null) {
            return spot;
         }
      }

      return new Vec3(this.getX(), (double)(seat.getY() + 1), this.getZ());
   }
}
