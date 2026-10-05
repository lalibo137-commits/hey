package com.drunkard;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SeatEntity extends Entity {
   public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public static boolean sit(Level level, BlockPos pos, SeatBlock seat, Player player) {
      return sit(level, pos, player);
   }

   public static boolean seat(BlockState state) {
      Block block = state.getBlock();
      if (block instanceof SeatBlock || block instanceof StairBlock) {
         return true;
      } else {
         return block instanceof SlabBlock
            ? state.getValue(SlabBlock.TYPE) != SlabType.DOUBLE
            : block instanceof TrapDoorBlock && !(Boolean)state.getValue(TrapDoorBlock.OPEN);
      }
   }

   static double seatHeight(BlockState state) {
      Block block = state.getBlock();
      if (block instanceof SeatBlock seat) {
         return seat.seatHeight();
      } else if (block instanceof StairBlock) {
         return state.getValue(StairBlock.HALF) == Half.BOTTOM ? 0.5 : 1.0;
      } else if (block instanceof SlabBlock) {
         return state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM ? 0.5 : 1.0;
      } else {
         return state.getValue(TrapDoorBlock.HALF) == Half.BOTTOM ? 0.1875 : 1.0;
      }
   }

   public static boolean sit(Level level, BlockPos pos, Entity rider) {
      return sit(level, pos, rider, null);
   }

   public static boolean sit(Level level, BlockPos pos, Entity rider, BlockPos toward) {
      BlockState state = level.getBlockState(pos);
      if (seat(state) && !rider.isPassenger() && !occupied(level, pos)) {
         SeatEntity entity = new SeatEntity((EntityType<? extends SeatEntity>)Drunkard.SEAT_ENTITY.get(), level);
         double height = seatHeight(state);
         Direction counter = toward == null ? null : counterSide(level, pos, toward);
         Direction own = state.hasProperty(HorizontalDirectionalBlock.FACING) ? (Direction)state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
         Direction facing;
         if (state.getBlock() instanceof SeatBlock seat) {
            facing = toward != null && seat.seatHeight() > 0.8 ? (counter != null ? counter : toward(pos, toward, own)) : own;
         } else if (!(state.getBlock() instanceof StairBlock) || state.getValue(StairBlock.HALF) != Half.BOTTOM) {
            facing = counter != null ? counter : (toward != null ? toward(pos, toward, own.getOpposite()) : own.getOpposite());
         } else if (counter != null) {
            facing = counter;
         } else if (toward != null
            && Math.abs(toward.getX() - pos.getX()) <= 5
            && Math.abs(toward.getZ() - pos.getZ()) <= 5
            && own.getStepX() * (toward.getX() - pos.getX()) + own.getStepZ() * (toward.getZ() - pos.getZ()) > 0) {
            facing = toward(pos, toward, own);
         } else {
            facing = own.getOpposite();
         }

         float yaw = facing.toYRot();
         if (rider instanceof LivingEntity living && !(rider instanceof Player)) {
            living.setYRot(yaw);
            living.yRotO = yaw;
            living.setYBodyRot(yaw);
            living.yBodyRotO = yaw;
            living.setYHeadRot(yaw);
            living.yHeadRotO = yaw;
         }

         entity.moveTo((double)pos.getX() + 0.5, (double)pos.getY() + Math.min(height, 0.99), (double)pos.getZ() + 0.5, yaw, 0.0F);
         level.addFreshEntity(entity);
         return rider.startRiding(entity);
      } else {
         return false;
      }
   }

   private static Direction toward(BlockPos pos, BlockPos toward, Direction fallback) {
      int dx = toward.getX() - pos.getX();
      int dz = toward.getZ() - pos.getZ();
      return dx == 0 && dz == 0 ? fallback : Direction.getNearest((float)dx, 0.0F, (float)dz);
   }

   public static Direction counterSide(Level level, BlockPos seat, BlockPos toward) {
      List<BlockPos> linked = toward != null && level.getBlockEntity(toward) instanceof CashRegisterEntity r ? r.seats : List.of();
      BlockState under = level.getBlockState(seat.below());
      boolean raised = !Block.isShapeFullBlock(under.getCollisionShape(level, seat.below()));
      Direction best = null;
      double bestScore = Double.MAX_VALUE;

      for (Direction d : Plane.HORIZONTAL) {
         for (int dy = 0; dy >= (raised ? -1 : 0); dy--) {
            BlockPos n = seat.relative(d).above(dy);
            BlockState state = level.getBlockState(n);
            if (!(state.getBlock() instanceof SeatBlock)
               && !linked.contains(n)
               && (dy >= 0 || !seat(level.getBlockState(n.above())))
               && (!(state.getBlock() instanceof StairBlock) || state.getValue(StairBlock.HALF) != Half.BOTTOM)
               && !(state.getBlock() instanceof FenceBlock)) {
               VoxelShape shape = state.getCollisionShape(level, n);
               if (!shape.isEmpty() && !(shape.max(Axis.Y) < 0.5)) {
                  AABB bounds = shape.bounds();
                  if (!(bounds.getXsize() < 0.5) && !(bounds.getZsize() < 0.5) && !Block.isShapeFullBlock(level.getBlockState(n.above()).getCollisionShape(level, n.above()))) {
                     double score = (toward == null ? 0.0 : n.distSqr(toward)) - (double)(dy * 6);
                     if (score < bestScore) {
                        best = d;
                        bestScore = score;
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   public static boolean occupied(Level level, BlockPos pos) {
      return !level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty();
   }

   protected void defineSynchedData(Builder builder) {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   // Same rider heights as before 1.20.5: players already sit 0.6 lower through their own vehicle attachment, other riders get the old 0.25 drop.
   protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
      return passenger instanceof Player ? Vec3.ZERO : new Vec3(0.0, -0.25, 0.0);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.getPassengers().isEmpty() || !seat(this.level().getBlockState(this.blockPosition()))) {
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
      if (passenger instanceof LivingEntity living && !(passenger instanceof Player)) {
         float yaw = this.getYRot();
         living.setYRot(yaw);
         living.yRotO = yaw;
         living.setYBodyRot(yaw);
         living.yBodyRotO = yaw;
         living.setYHeadRot(yaw + Mth.clamp(Mth.wrapDegrees(living.getYHeadRot() - yaw), -75.0F, 75.0F));
         return;
      }

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
      Direction[] sides = passenger instanceof BarPatron
         ? new Direction[]{front.getOpposite(), front.getClockWise(), front.getCounterClockWise()}
         : new Direction[]{front, front.getClockWise(), front.getCounterClockWise(), front.getOpposite()};

      for (int down = 0; down <= 3; down++) {
         for (Direction side : sides) {
            Vec3 spot = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), seat.relative(side).below(down), true);
            if (spot != null) {
               return spot;
            }
         }
      }

      for (int down = 0; down <= 3; down++) {
         Vec3 spot = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), seat.relative(front).below(down), true);
         if (spot != null) {
            return spot;
         }
      }

      return new Vec3(this.getX(), (double)(seat.getY() + 1), this.getZ());
   }
}
