package com.airlinestory.entity;

import com.airlinestory.block.SeatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** The invisible entity a player or NPC rides while sitting on a seat block. */
public class SeatEntity extends Entity {
    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Sits {@code who} on the seat at {@code pos}. Returns false if the seat is taken. */
    public static boolean sit(LivingEntity who, BlockPos pos, Direction facing) {
        Level level = who.level();
        if (occupied(level, pos)) return false;
        SeatEntity seat = ModEntities.SEAT.get().create(level);
        if (seat == null) return false;
        seat.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, facing.toYRot(), 0f);
        level.addFreshEntity(seat);
        boolean ok = who.startRiding(seat, true);
        if (ok) {
            who.setYRot(facing.toYRot());
            who.setYHeadRot(facing.toYRot());
            who.yBodyRot = facing.toYRot();
        } else {
            seat.discard();
        }
        return ok;
    }

    public static boolean occupied(Level level, BlockPos pos) {
        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
        for (SeatEntity s : seats) {
            if (s.isAlive() && s.isVehicle()) return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!isVehicle()) {
            if (tickCount > 5) discard();
            return;
        }
        if (tickCount % 20 == 0 && !(level().getBlockState(blockPosition()).getBlock() instanceof SeatBlock)) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.05;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
