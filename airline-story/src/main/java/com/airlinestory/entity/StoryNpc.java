package com.airlinestory.entity;

import com.airlinestory.story.StoryEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Every person in the story: travellers, staff, passengers and the Tidefolk. The story director gives
 * them a behaviour {@link Mode} (stand, wander, sit, walk a route, follow, stare) and a dialogue tree.
 */
public class StoryNpc extends PathfinderMob {
    public enum Mode { STAND, WANDER, SIT, ROUTE, FOLLOW, STARE }

    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHILD = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MARKER = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SMILE = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.BOOLEAN);

    private String npcId = "";
    private String group = "";
    private String dialogue = "traveler";
    private Mode mode = Mode.STAND;
    private Mode afterRoute = Mode.STAND;
    @Nullable private BlockPos home;
    @Nullable private BlockPos seat;
    private final List<BlockPos> route = new ArrayList<>();
    private int routeIndex;
    private int wanderRadius = 6;
    private float homeYaw = Float.NaN;

    private int talkTicks;
    @Nullable private Player talkingTo;
    private int stuckTicks;
    private double lastDist = Double.MAX_VALUE;
    @Nullable private BlockPos walkTarget;
    private int repathTicks;

    public StoryNpc(EntityType<? extends StoryNpc> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f, 0.4f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN, "traveler_0");
        this.entityData.define(SLIM, false);
        this.entityData.define(CHILD, false);
        this.entityData.define(MARKER, false);
        this.entityData.define(SMILE, false);
    }

    // ----- configuration (server) -----
    public StoryNpc setup(String id, String group, String name, String skin, boolean slim, boolean child, String dialogue) {
        this.npcId = id;
        this.group = group;
        this.dialogue = dialogue;
        this.entityData.set(SKIN, skin);
        this.entityData.set(SLIM, slim);
        this.entityData.set(CHILD, child);
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(false);
        this.refreshDimensions();
        return this;
    }

    public String npcId() { return npcId; }
    public String group() { return group; }
    public String dialogue() { return dialogue; }
    public void setDialogue(String d) { this.dialogue = d; }
    public Mode mode() { return mode; }
    public String skin() { return entityData.get(SKIN); }
    public boolean slim() { return entityData.get(SLIM); }
    public boolean marker() { return entityData.get(MARKER); }
    public boolean smiling() { return entityData.get(SMILE); }
    public void setMarker(boolean m) { if (marker() != m) entityData.set(MARKER, m); }
    public void setSmiling(boolean s) { if (smiling() != s) entityData.set(SMILE, s); }
    public void setSkin(String s) { entityData.set(SKIN, s); }
    public String displayName() { return getCustomName() == null ? "Stranger" : getCustomName().getString(); }
    @Nullable public BlockPos seatPos() { return seat; }
    @Nullable public BlockPos home() { return home; }

    @Override
    public boolean isBaby() {
        return entityData.get(CHILD);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CHILD.equals(key)) refreshDimensions();
    }

    public void stand(BlockPos at, float yaw) {
        leaveSeat();
        this.home = at;
        this.homeYaw = yaw;
        this.mode = Mode.STAND;
    }

    public void wander(BlockPos around, int radius) {
        leaveSeat();
        this.home = around;
        this.wanderRadius = radius;
        this.mode = Mode.WANDER;
    }

    public void sitAt(BlockPos seatPos) {
        if (seatPos.equals(this.seat) && isPassenger()) return;
        leaveSeat();
        this.seat = seatPos;
        this.home = seatPos;
        this.mode = Mode.SIT;
    }

    /** Walk the waypoints in order, then sit at {@code thenSeat} (or stand at the last waypoint). */
    public void walkRoute(List<BlockPos> waypoints, @Nullable BlockPos thenSeat) {
        leaveSeat();
        this.route.clear();
        this.route.addAll(waypoints);
        this.routeIndex = 0;
        this.seat = thenSeat;
        this.afterRoute = thenSeat != null ? Mode.SIT : Mode.STAND;
        this.home = waypoints.isEmpty() ? blockPosition() : waypoints.get(waypoints.size() - 1);
        this.mode = Mode.ROUTE;
    }

    public void follow() {
        leaveSeat();
        this.mode = Mode.FOLLOW;
    }

    public void stare() {
        this.mode = Mode.STARE;
    }

    public void unstare() {
        if (mode == Mode.STARE) mode = seat != null ? Mode.SIT : Mode.STAND;
    }

    public void leaveSeat() {
        if (isPassenger()) stopRiding();
        if (mode == Mode.SIT) seat = null;
    }

    public void teleportHome() {
        BlockPos p = seat != null ? seat : home;
        if (p != null) teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
    }

    /** Freeze and face the player while a conversation is open. */
    public void beginTalk(Player player) {
        this.talkingTo = player;
        this.talkTicks = 20 * 60 * 5;
        getNavigation().stop();
    }

    public void endTalk() {
        this.talkTicks = 0;
        this.talkingTo = null;
    }

    @Override
    public int getMaxHeadYRot() {
        return mode == Mode.STARE ? 175 : super.getMaxHeadYRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount == 2) {
            StoryEvents.validateNpc(this);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (talkTicks > 0) {
            talkTicks--;
            getNavigation().stop();
            if (talkingTo != null) getLookControl().setLookAt(talkingTo, 30f, 30f);
            if (talkingTo == null || !talkingTo.isAlive() || distanceToSqr(talkingTo) > 100) endTalk();
            return;
        }
        switch (mode) {
            case STAND -> tickStand();
            case WANDER -> tickWander();
            case SIT -> tickSit();
            case ROUTE -> tickRoute();
            case FOLLOW -> tickFollow();
            case STARE -> tickStare();
        }
    }

    private void tickStand() {
        if (home == null) return;
        if (isPassenger()) return;
        if (tickCount % 20 == 0 && (level().getBlockState(blockPosition()).getBlock() instanceof com.airlinestory.block.SeatBlock
                || level().getBlockState(blockPosition().below()).getBlock() instanceof com.airlinestory.block.SeatBlock)) {
            BlockPos free = freeSpotNear(home);
            teleportTo(free.getX() + 0.5, free.getY(), free.getZ() + 0.5);
            home = free;
        }
        if (blockPosition().distSqr(home) > 2.5) {
            walkTowards(home, 0.9);
        } else if (getNavigation().isDone() && !Float.isNaN(homeYaw) && tickCount % 40 == 0 && getTarget() == null) {
            if (level().getNearestPlayer(this, 6) == null) {
                setYRot(homeYaw);
                yBodyRot = homeYaw;
                yHeadRot = homeYaw;
            }
        }
    }

    private void tickWander() {
        if (home == null) home = blockPosition();
        if (getNavigation().isDone() && random.nextInt(160) == 0) {
            BlockPos t = home.offset(random.nextInt(wanderRadius * 2 + 1) - wanderRadius, 0, random.nextInt(wanderRadius * 2 + 1) - wanderRadius);
            getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, 0.7);
        }
        if (blockPosition().distSqr(home) > (double) (wanderRadius + 12) * (wanderRadius + 12)) {
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        }
    }

    private void tickSit() {
        if (seat == null) { mode = Mode.STAND; return; }
        if (isPassenger()) return;
        double d = position().distanceToSqr(Vec3.atBottomCenterOf(seat));
        if (d < 30 || stuckTicks > 120) {
            teleportTo(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5);
            BlockState st = level().getBlockState(seat);
            Direction f = st.hasProperty(HorizontalDirectionalBlock.FACING) ? st.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
            if (!SeatEntity.sit(this, seat, f)) {
                // taken: stand beside it rather than on top of it
                mode = Mode.STAND;
                home = freeSpotNear(seat);
                teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            }
            stuckTicks = 0;
        } else {
            walkTowards(seat, 0.8);
        }
    }

    private void tickRoute() {
        if (routeIndex >= route.size()) {
            mode = afterRoute;
            return;
        }
        BlockPos t = route.get(routeIndex);
        if (t.getY() > 1000) {
            getNavigation().stop();
            teleportTo(t.getX() + 0.5, t.getY() - 1000, t.getZ() + 0.5);
            routeIndex++;
            walkTarget = null;
            return;
        }
        if (position().distanceToSqr(Vec3.atBottomCenterOf(t)) < 2.0) {
            routeIndex++;
            walkTarget = null;
            return;
        }
        if (stuckTicks > 100) {
            teleportTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
            stuckTicks = 0;
            routeIndex++;
            return;
        }
        walkTowards(t, 0.75);
    }

    private void tickFollow() {
        Player p = level().getNearestPlayer(this, 64);
        if (p == null) return;
        double d = distanceToSqr(p);
        getLookControl().setLookAt(p, 30f, 30f);
        if (d > 32 * 32) {
            teleportTo(p.getX() + 1, p.getY(), p.getZ() + 1);
        } else if (d > 9) {
            if (repathTicks-- <= 0) {
                getNavigation().moveTo(p, 1.0);
                repathTicks = 10;
            }
        } else {
            getNavigation().stop();
        }
    }

    private void tickStare() {
        getNavigation().stop();
        Player p = level().getNearestPlayer(this, 48);
        if (p != null) {
            getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 180f, 180f);
        }
    }

    /** The nearest spot with two blocks of air above a solid floor (not a seat). */
    private BlockPos freeSpotNear(BlockPos p) {
        for (int r = 0; r <= 3; r++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos c = p.offset(dx, 0, dz);
                    if (level().getBlockState(c).isAir() && level().getBlockState(c.above()).isAir()
                            && level().getBlockState(c.below()).isSolidRender(level(), c.below())) return c;
                }
        return p;
    }

    private void walkTowards(BlockPos t, double speed) {
        double d = position().distanceToSqr(Vec3.atBottomCenterOf(t));
        if (!t.equals(walkTarget)) {
            walkTarget = t;
            lastDist = d;
            stuckTicks = 0;
            repathTicks = 0;
        }
        if (repathTicks-- <= 0 || getNavigation().isDone()) {
            boolean ok = getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, speed);
            repathTicks = 30;
            if (!ok) stuckTicks += 30;
        }
        if (d < lastDist - 0.05) {
            lastDist = d;
            stuckTicks = Math.max(0, stuckTicks - 2);
        } else {
            stuckTicks++;
        }
    }

    // ----- interaction & invulnerability -----
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            StoryEvents.talk(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public double getMyRidingOffset() {
        return -0.35;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    // ----- save -----
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcId", npcId);
        tag.putString("Group", group);
        tag.putString("Dialogue", dialogue);
        tag.putString("Skin", skin());
        tag.putBoolean("Slim", slim());
        tag.putBoolean("Child", isBaby());
        tag.putBoolean("Smile", smiling());
        tag.putString("Mode", mode.name());
        tag.putString("AfterRoute", afterRoute.name());
        if (home != null) tag.putLong("Home", home.asLong());
        if (seat != null) tag.putLong("Seat", seat.asLong());
        tag.putInt("WanderRadius", wanderRadius);
        tag.putFloat("HomeYaw", homeYaw);
        ListTag r = new ListTag();
        for (BlockPos p : route) r.add(LongTag.valueOf(p.asLong()));
        tag.put("Route", r);
        tag.putInt("RouteIndex", routeIndex);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        npcId = tag.getString("NpcId");
        group = tag.getString("Group");
        dialogue = tag.getString("Dialogue");
        entityData.set(SKIN, tag.getString("Skin"));
        entityData.set(SLIM, tag.getBoolean("Slim"));
        entityData.set(CHILD, tag.getBoolean("Child"));
        entityData.set(SMILE, tag.getBoolean("Smile"));
        try {
            mode = Mode.valueOf(tag.getString("Mode"));
            afterRoute = Mode.valueOf(tag.getString("AfterRoute"));
        } catch (IllegalArgumentException e) {
            mode = Mode.STAND;
        }
        home = tag.contains("Home") ? BlockPos.of(tag.getLong("Home")) : null;
        seat = tag.contains("Seat") ? BlockPos.of(tag.getLong("Seat")) : null;
        wanderRadius = tag.getInt("WanderRadius");
        homeYaw = tag.contains("HomeYaw") ? tag.getFloat("HomeYaw") : Float.NaN;
        route.clear();
        for (Tag t : tag.getList("Route", Tag.TAG_LONG)) route.add(BlockPos.of(((LongTag) t).getAsLong()));
        routeIndex = tag.getInt("RouteIndex");
        if (mode == Mode.STARE) mode = seat != null ? Mode.SIT : Mode.STAND;
    }
}
