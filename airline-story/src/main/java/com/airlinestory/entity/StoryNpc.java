package com.airlinestory.entity;

import com.airlinestory.block.SeatBlock;
import com.airlinestory.registry.ModItems;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Every person in the story. The director gives each one a behaviour {@link Mode} and an {@link Activity}
 * (what they're doing when they get where they're going), a dialogue tree, a bark pool and a SmileScore.
 */
public class StoryNpc extends PathfinderMob {
    public enum Mode { STAND, WANDER, SIT, ROUTE, FOLLOW, STARE, PATROL }

    public enum Activity { IDLE, PHONE, READING, COFFEE, CHATTING, SLEEPING, WORKING, BROWSING, KNITTING, GUARD, WAVING }

    private static final EntityDataAccessor<String> NPC_ID = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHILD = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.BOOLEAN);
    /** 0 none, 1 main objective "!", 2 has something new to say "...". */
    private static final EntityDataAccessor<Integer> MARKER = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SMILE = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> BARK = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.STRING);
    /** SmileScore x100, or -1 for "unrated" (the Tidefolk). */
    private static final EntityDataAccessor<Integer> SCORE = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTIVITY = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.INT);

    private String group = "";
    private String dialogue = "traveler";
    private String barks = "";
    private Mode mode = Mode.STAND;
    private Mode afterRoute = Mode.STAND;
    private Activity baseActivity = Activity.IDLE;
    @Nullable private BlockPos home;
    @Nullable private BlockPos seat;
    private final List<BlockPos> route = new ArrayList<>();
    private final List<BlockPos> anchors = new ArrayList<>();
    private int routeIndex;
    private float homeYaw = Float.NaN;
    private boolean loopRoute;

    // transient movement state
    private int talkTicks;
    @Nullable private Player talkingTo;
    @Nullable private BlockPos walkTarget;
    private int dwell;
    private int barkTicks;
    private int barkCooldown;
    private int stuckChecks;
    private int stuckFails;
    private Vec3 lastProgressPos = Vec3.ZERO;
    private int progressTimer;
    private double walkSpeed = 0.6;

    public StoryNpc(EntityType<? extends StoryNpc> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanPassDoors(true);
            nav.setCanFloat(true);
        }
        this.setPathfindingMalus(BlockPathTypes.WATER, 8f);
        this.setPathfindingMalus(BlockPathTypes.FENCE, -1f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {}

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(NPC_ID, "");
        entityData.define(SKIN, "traveler_0");
        entityData.define(SLIM, false);
        entityData.define(CHILD, false);
        entityData.define(MARKER, 0);
        entityData.define(SMILE, false);
        entityData.define(BARK, "");
        entityData.define(SCORE, -1);
        entityData.define(ACTIVITY, 0);
    }

    // =========================================================================================
    // configuration (server)
    // =========================================================================================
    public StoryNpc setup(String id, String group, String name, String skin, boolean slim, boolean child, String dialogue) {
        entityData.set(NPC_ID, id);
        this.group = group;
        this.dialogue = dialogue;
        this.barks = dialogue;
        entityData.set(SKIN, skin);
        entityData.set(SLIM, slim);
        entityData.set(CHILD, child);
        setCustomName(Component.literal(name));
        setCustomNameVisible(false);
        refreshDimensions();
        return this;
    }

    public StoryNpc score(float s) { entityData.set(SCORE, s < 0 ? -1 : Math.round(s * 100)); return this; }
    public StoryNpc barks(String pool) { this.barks = pool; return this; }
    public StoryNpc activity(Activity a) { this.baseActivity = a; setActivity(a); return this; }
    public StoryNpc speed(double s) { this.walkSpeed = s; return this; }

    public String npcId() { return entityData.get(NPC_ID); }
    public String group() { return group; }
    public String dialogue() { return dialogue; }
    public String barkPool() { return barks; }
    public void setDialogue(String d) { this.dialogue = d; }
    public Mode mode() { return mode; }
    public String skin() { return entityData.get(SKIN); }
    public boolean slim() { return entityData.get(SLIM); }
    public int marker() { return entityData.get(MARKER); }
    public boolean smiling() { return entityData.get(SMILE); }
    public String bark() { return entityData.get(BARK); }
    public float smileScore() { int s = entityData.get(SCORE); return s < 0 ? -1 : s / 100f; }
    public Activity activityNow() { int a = entityData.get(ACTIVITY); return Activity.values()[Math.max(0, Math.min(a, Activity.values().length - 1))]; }
    public void setMarker(int m) { if (marker() != m) entityData.set(MARKER, m); }
    public void setSmiling(boolean s) { if (smiling() != s) entityData.set(SMILE, s); }
    public void setSkin(String s) { entityData.set(SKIN, s); }
    public String displayName() { return getCustomName() == null ? "Stranger" : getCustomName().getString(); }
    @Nullable public BlockPos seatPos() { return seat; }
    @Nullable public BlockPos home() { return home; }
    public boolean talking() { return talkTicks > 0; }

    public void adjustScore(float d) {
        float s = smileScore();
        if (s >= 0) score(Math.max(0.5f, Math.min(5f, s + d)));
    }

    private void setActivity(Activity a) {
        if (entityData.get(ACTIVITY) != a.ordinal()) entityData.set(ACTIVITY, a.ordinal());
        ItemStack hand = switch (a) {
            case PHONE -> new ItemStack(ModItems.SMARTPHONE.get());
            case READING -> new ItemStack(Items.BOOK);
            case COFFEE -> new ItemStack(ModItems.COFFEE.get());
            case WORKING -> new ItemStack(Items.PAPER);
            case KNITTING -> new ItemStack(Items.STRING);
            default -> ItemStack.EMPTY;
        };
        if (!ItemStack.isSameItem(getItemBySlot(EquipmentSlot.MAINHAND), hand)) setItemSlot(EquipmentSlot.MAINHAND, hand);
    }

    /** Show a line above the head for a few seconds. */
    public void say(String line, int ticks) {
        entityData.set(BARK, line);
        barkTicks = ticks;
    }

    public boolean canBark() { return barkCooldown <= 0 && barkTicks <= 0; }
    public void barkCooldown(int t) { barkCooldown = t; }

    @Override
    public boolean isBaby() { return entityData.get(CHILD); }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CHILD.equals(key)) refreshDimensions();
    }

    // ----- behaviour orders -----
    public void stand(BlockPos at, float yaw) {
        leaveSeat();
        home = at;
        homeYaw = yaw;
        mode = Mode.STAND;
        walkTarget = null;
    }

    /** Stroll between points of interest (shop fronts, boards, seats), lingering at each. */
    public void wander(List<BlockPos> points) {
        leaveSeat();
        anchors.clear();
        anchors.addAll(points);
        home = points.isEmpty() ? blockPosition() : points.get(0);
        mode = Mode.WANDER;
        dwell = random.nextInt(200);
        walkTarget = null;
    }

    /** Short-range wander around a single spot. */
    public void wander(BlockPos around, int radius) {
        List<BlockPos> pts = new ArrayList<>();
        pts.add(around);
        for (int i = 0; i < 5; i++) pts.add(around.offset(random.nextInt(radius * 2 + 1) - radius, 0, random.nextInt(radius * 2 + 1) - radius));
        wander(pts);
    }

    public void sitAt(BlockPos seatPos) {
        if (seatPos.equals(seat) && isPassenger()) return;
        leaveSeat();
        seat = seatPos;
        home = seatPos;
        mode = Mode.SIT;
        walkTarget = null;
    }

    public void walkRoute(List<BlockPos> waypoints, @Nullable BlockPos thenSeat) {
        leaveSeat();
        route.clear();
        route.addAll(waypoints);
        routeIndex = 0;
        loopRoute = false;
        seat = thenSeat;
        afterRoute = thenSeat != null ? Mode.SIT : Mode.STAND;
        home = waypoints.isEmpty() ? blockPosition() : waypoints.get(waypoints.size() - 1);
        mode = Mode.ROUTE;
        walkTarget = null;
    }

    /** Walk a loop forever (guards on patrol). */
    public void patrol(List<BlockPos> loop) {
        walkRoute(loop, null);
        loopRoute = true;
        mode = Mode.PATROL;
    }

    public void follow() { leaveSeat(); mode = Mode.FOLLOW; }
    public void stare() { mode = Mode.STARE; }
    public void unstare() { if (mode == Mode.STARE) mode = seat != null ? Mode.SIT : Mode.STAND; }

    public void leaveSeat() {
        if (isPassenger()) stopRiding();
    }

    public void beginTalk(Player player) {
        talkingTo = player;
        talkTicks = 20 * 60 * 5;
        getNavigation().stop();
    }

    public void endTalk() {
        talkTicks = 0;
        talkingTo = null;
    }

    @Override
    public int getMaxHeadYRot() {
        return mode == Mode.STARE ? 175 : 65;
    }

    // =========================================================================================
    // ticking
    // =========================================================================================
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount == 2) StoryEvents.validateNpc(this);
        if (barkTicks > 0 && --barkTicks == 0) entityData.set(BARK, "");
        if (barkCooldown > 0) barkCooldown--;
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
            case ROUTE, PATROL -> tickRoute();
            case FOLLOW -> tickFollow();
            case STARE -> tickStare();
        }
        lookAround();
    }

    /** Glance at a nearby player who is looking at us; otherwise keep to our own business. */
    private void lookAround() {
        if (mode == Mode.STARE || mode == Mode.FOLLOW) return;
        Player p = level().getNearestPlayer(this, 5);
        if (p != null && !isMoving()) {
            getLookControl().setLookAt(p, 20f, 20f);
            return;
        }
        Activity a = activityNow();
        if (!isMoving() && (a == Activity.PHONE || a == Activity.READING || a == Activity.KNITTING)) {
            Vec3 f = Vec3.directionFromRotation(30f, yBodyRot).scale(2);
            getLookControl().setLookAt(getX() + f.x, getEyeY() - 1.2, getZ() + f.z, 10f, 10f);
        }
    }

    private boolean isMoving() {
        return !getNavigation().isDone();
    }

    private void tickStand() {
        if (home == null || isPassenger()) return;
        if (tickCount % 20 == 0 && onSeatBlock()) {
            BlockPos free = freeSpotNear(home);
            safeTeleport(free);
            home = free;
        }
        if (position().distanceToSqr(Vec3.atBottomCenterOf(home)) > 2.0) {
            setActivity(Activity.IDLE);
            walkTowards(home, walkSpeed);
        } else {
            getNavigation().stop();
            setActivity(baseActivity);
            if (!Float.isNaN(homeYaw) && level().getNearestPlayer(this, 5) == null) {
                yBodyRot = Mth(homeYaw, yBodyRot);
                setYRot(yBodyRot);
            }
        }
    }

    private static float Mth(float target, float current) {
        float d = net.minecraft.util.Mth.wrapDegrees(target - current);
        return current + d * 0.2f;
    }

    private void tickWander() {
        if (anchors.isEmpty()) { mode = Mode.STAND; return; }
        if (walkTarget == null) {
            if (isPassenger()) {
                if (--dwell <= 0) leaveSeat();
                return;
            }
            if (dwell > 0) {
                dwell--;
                setActivity(baseActivity);
                return;
            }
            BlockPos next = anchors.get(random.nextInt(anchors.size()));
            walkTarget = next;
            stuckFails = 0;
            setActivity(Activity.IDLE);
        }
        boolean seatTarget = level().getBlockState(walkTarget).getBlock() instanceof SeatBlock;
        double d = position().distanceToSqr(Vec3.atBottomCenterOf(walkTarget));
        if (d < (seatTarget ? 4.0 : 1.6)) {
            getNavigation().stop();
            if (seatTarget && !SeatEntity.occupied(level(), walkTarget)) {
                BlockState st = level().getBlockState(walkTarget);
                if (SeatEntity.sit(this, walkTarget, st.getValue(HorizontalDirectionalBlock.FACING))) {
                    dwell = 300 + random.nextInt(900);
                    walkTarget = null;
                    setActivity(baseActivity);
                    return;
                }
            }
            dwell = 120 + random.nextInt(400);
            walkTarget = null;
            setActivity(baseActivity);
            return;
        }
        if (!walkTowards(walkTarget, walkSpeed)) {
            walkTarget = null;
            dwell = 40;
        }
    }

    private void tickSit() {
        if (seat == null) { mode = Mode.STAND; return; }
        if (isPassenger()) { setActivity(baseActivity); return; }
        double d = position().distanceToSqr(Vec3.atBottomCenterOf(seat));
        // Close enough to step into the seat (planes: from the aisle; airports: from in front).
        if (d < 12 || stuckFails >= 3) {
            BlockState st = level().getBlockState(seat);
            Direction f = st.hasProperty(HorizontalDirectionalBlock.FACING) ? st.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
            if (d >= 12 && !unseen()) return;
            if (SeatEntity.sit(this, seat, f)) {
                stuckFails = 0;
            } else {
                mode = Mode.STAND;
                home = freeSpotNear(seat);
                safeTeleport(home);
            }
        } else {
            walkTowards(seat, walkSpeed);
        }
    }

    private void tickRoute() {
        if (routeIndex >= route.size()) {
            if (loopRoute && !route.isEmpty()) { routeIndex = 0; return; }
            mode = afterRoute;
            walkTarget = null;
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
        if (position().distanceToSqr(Vec3.atBottomCenterOf(t)) < 1.8) {
            routeIndex++;
            walkTarget = null;
            if (mode == Mode.PATROL) dwell = 40 + random.nextInt(60);
            return;
        }
        if (mode == Mode.PATROL && dwell > 0) { dwell--; return; }
        if (!walkTowards(t, mode == Mode.PATROL ? 0.5 : walkSpeed)) {
            // Can't reach this waypoint: skip it quietly.
            safeTeleport(t);
            routeIndex++;
        }
    }

    private void tickFollow() {
        Player p = level().getNearestPlayer(this, 64);
        if (p == null) return;
        double d = distanceToSqr(p);
        getLookControl().setLookAt(p, 30f, 30f);
        if (d > 40 * 40) safeTeleport(p.blockPosition());
        else if (d > 9) walkTowards(p.blockPosition(), 0.9);
        else { getNavigation().stop(); walkTarget = null; }
    }

    private void tickStare() {
        getNavigation().stop();
        Player p = level().getNearestPlayer(this, 48);
        if (p != null) getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 180f, 180f);
    }

    /**
     * Pathfind towards {@code t}. Re-paths only when needed, notices when we're not getting anywhere and
     * tries again; after repeated failures returns false (the caller decides what to do).
     */
    private boolean walkTowards(BlockPos t, double speed) {
        if (!t.equals(walkTarget) || getNavigation().isDone()) {
            if (!t.equals(walkTarget)) {
                walkTarget = t;
                stuckFails = 0;
                stuckChecks = 0;
            }
            boolean ok = getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, speed);
            if (!ok) stuckFails++;
            lastProgressPos = position();
            progressTimer = 0;
        }
        if (++progressTimer >= 50) {
            progressTimer = 0;
            if (position().distanceToSqr(lastProgressPos) < 0.25) {
                stuckFails++;
                getNavigation().stop();
                // Nudge sideways and try again.
                Vec3 side = new Vec3(random.nextGaussian(), 0, random.nextGaussian()).normalize().scale(0.4);
                setDeltaMovement(getDeltaMovement().add(side.x * 0.3, 0, side.z * 0.3));
            }
            lastProgressPos = position();
        }
        if (stuckFails >= 4) {
            if (unseen()) {
                safeTeleport(t);
                stuckFails = 0;
                return true;
            }
            return stuckFails < 12;
        }
        return true;
    }

    /** True if no player could see us teleport. */
    private boolean unseen() {
        Player p = level().getNearestPlayer(this, 40);
        if (p == null) return true;
        Vec3 look = p.getViewVector(1f);
        Vec3 to = position().add(0, 1, 0).subtract(p.getEyePosition());
        return look.dot(to.normalize()) < 0.3 || !p.hasLineOfSight(this);
    }

    private void safeTeleport(BlockPos t) {
        BlockPos free = level().getBlockState(t).isAir() || level().getBlockState(t).getBlock() instanceof SeatBlock ? t : freeSpotNear(t);
        teleportTo(free.getX() + 0.5, free.getY(), free.getZ() + 0.5);
        getNavigation().stop();
    }

    private boolean onSeatBlock() {
        return level().getBlockState(blockPosition()).getBlock() instanceof SeatBlock
                || level().getBlockState(blockPosition().below()).getBlock() instanceof SeatBlock;
    }

    public BlockPos freeSpotNear(BlockPos p) {
        for (int r = 0; r <= 3; r++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++)
                    for (int dy = 0; dy >= -1; dy--) {
                        BlockPos c = p.offset(dx, dy, dz);
                        if (level().getBlockState(c).isAir() && level().getBlockState(c.above()).isAir()
                                && level().getBlockState(c.below()).isSolidRender(level(), c.below())) return c;
                    }
        return p;
    }

    public void teleportHome() {
        BlockPos p = seat != null ? seat : home;
        if (p != null) safeTeleport(p);
    }

    // =========================================================================================
    // interaction
    // =========================================================================================
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) StoryEvents.talk(sp, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (!level().isClientSide && source.getEntity() instanceof ServerPlayer sp) StoryEvents.npcHit(sp, this);
        return false;
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity entity) {}
    @Override public boolean removeWhenFarAway(double dist) { return false; }
    @Override public double getMyRidingOffset() { return -0.35; }
    @Override public boolean canBeLeashed(Player player) { return false; }

    // =========================================================================================
    // save
    // =========================================================================================
    private static ListTag positions(List<BlockPos> list) {
        ListTag r = new ListTag();
        for (BlockPos p : list) r.add(LongTag.valueOf(p.asLong()));
        return r;
    }

    private static void positions(ListTag tag, List<BlockPos> out) {
        out.clear();
        for (Tag t : tag) out.add(BlockPos.of(((LongTag) t).getAsLong()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcId", npcId());
        tag.putString("Group", group);
        tag.putString("Dialogue", dialogue);
        tag.putString("Barks", barks);
        tag.putString("Skin", skin());
        tag.putBoolean("Slim", slim());
        tag.putBoolean("Child", isBaby());
        tag.putBoolean("Smile", smiling());
        tag.putInt("Score", entityData.get(SCORE));
        tag.putString("Mode", mode.name());
        tag.putString("AfterRoute", afterRoute.name());
        tag.putString("Activity", baseActivity.name());
        if (home != null) tag.putLong("Home", home.asLong());
        if (seat != null) tag.putLong("Seat", seat.asLong());
        tag.putFloat("HomeYaw", homeYaw);
        tag.put("Route", positions(route));
        tag.put("Anchors", positions(anchors));
        tag.putInt("RouteIndex", routeIndex);
        tag.putBoolean("Loop", loopRoute);
        tag.putDouble("Speed", walkSpeed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(NPC_ID, tag.getString("NpcId"));
        group = tag.getString("Group");
        dialogue = tag.getString("Dialogue");
        barks = tag.getString("Barks");
        entityData.set(SKIN, tag.getString("Skin"));
        entityData.set(SLIM, tag.getBoolean("Slim"));
        entityData.set(CHILD, tag.getBoolean("Child"));
        entityData.set(SMILE, tag.getBoolean("Smile"));
        entityData.set(SCORE, tag.contains("Score") ? tag.getInt("Score") : -1);
        try {
            mode = Mode.valueOf(tag.getString("Mode"));
            afterRoute = Mode.valueOf(tag.getString("AfterRoute"));
            baseActivity = Activity.valueOf(tag.getString("Activity"));
        } catch (IllegalArgumentException e) {
            mode = Mode.STAND;
        }
        home = tag.contains("Home") ? BlockPos.of(tag.getLong("Home")) : null;
        seat = tag.contains("Seat") ? BlockPos.of(tag.getLong("Seat")) : null;
        homeYaw = tag.contains("HomeYaw") ? tag.getFloat("HomeYaw") : Float.NaN;
        positions(tag.getList("Route", Tag.TAG_LONG), route);
        positions(tag.getList("Anchors", Tag.TAG_LONG), anchors);
        routeIndex = tag.getInt("RouteIndex");
        loopRoute = tag.getBoolean("Loop");
        walkSpeed = tag.contains("Speed") ? tag.getDouble("Speed") : 0.6;
        if (mode == Mode.STARE) mode = seat != null ? Mode.SIT : Mode.STAND;
        setActivity(baseActivity);
    }
}
