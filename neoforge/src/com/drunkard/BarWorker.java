package com.drunkard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.MoveControl.Operation;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BarWorker extends PathfinderMob {
   public static final int UNIFORM = 1048576;
   private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(BarWorker.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<String> SPEECH = SynchedEntityData.defineId(BarWorker.class, EntityDataSerializers.STRING);
   public static final int REACH = 10;
   public static final int FLOOR_REACH = 32;
   private static final double SERVE_REACH = 3.5;
   private static final double WORK_REACH = 2.5;
   @Nullable
   private BlockPos register;
   private BarWorker.Step step = BarWorker.Step.IDLE;
   @Nullable
   private UUID customer;
   @Nullable
   private BlockPos glassFrom;
   @Nullable
   private BlockPos pourFrom;
   private int speechTicks;
   private int stepTicks;
   private int complainCooldown;
   private int quitTicks;
   private int handTicks;
   private final List<BlockPos> taps = new ArrayList<>();
   private final List<BlockPos> racks = new ArrayList<>();
   private final List<BlockPos> shelves = new ArrayList<>();
   private long scannedAt = -1000L;
   private final Set<BlockPos> floor = new HashSet<>();
   private final Set<BlockPos> area = new HashSet<>();
   @Nullable
   private BlockPos openedDoor;
   private boolean farTrip;
   @Nullable
   private BlockPos idleSpot;
   private int idleWait = 100;
   private int idleTicks;
   private int idleLength;
   private int idleDo = -1;
   @Nullable
   private BlockPos areaHome;
   @Nullable
   private CashRegisterEntity bar;
   private final ArrayDeque<BlockPos> route = new ArrayDeque<>();
   @Nullable
   private BlockPos routeGoal;
   private Vec3 lastPos = Vec3.ZERO;
   private int stillTicks;
   private int reroutes;
   private boolean goingHome;
   @Nullable
   private BlockPos exit;
   private int homeTicks;
   @Nullable
   private UUID chatTarget;
   private final Set<BlockPos> claimed = new HashSet<>();
   private final Set<BlockPos> standing = new HashSet<>();
   private long claimsAt = Long.MIN_VALUE;
   private final Map<BlockPos, Double> heights = new HashMap<>();
   @Nullable
   private BlockPos myHome;
   @Nullable
   private BlockPos arrivedAt;
   @Nullable
   private BlockPos restockShelf;
   private int restockTicks;

   public BarWorker(EntityType<? extends BarWorker> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
      this.moveControl = new BarWorker.WalkControl(this);
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 32.0);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SKIN, 1048576);
      builder.define(SPEECH, "");
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
   }

   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   protected boolean shouldDespawnInPeaceful() {
      return false;
   }

   public boolean isPushable() {
      return false;
   }

   protected void pushEntities() {
   }

   public int skin() {
      return (Integer)this.entityData.get(SKIN);
   }

   public boolean slim() {
      return (this.skin() & 1) == 1;
   }

   public String speech() {
      return (String)this.entityData.get(SPEECH);
   }

   @Nullable
   public BlockPos registerPos() {
      return this.register;
   }

   public void say(String line) {
      line = Medieval.speak(this.skin(), line);
      this.entityData.set(SPEECH, line);
      this.speechTicks = 50 + line.length() * 2;
   }

   public void hire(BlockPos register, int look) {
      this.register = register;
      this.entityData.set(SKIN, look & 65535 | 1048576 | look & Medieval.SKIN_BIT);
   }

   public void setMedieval(boolean medieval) {
      int skin = this.skin() & ~Medieval.SKIN_BIT;
      this.entityData.set(SKIN, medieval ? skin | Medieval.SKIN_BIT : skin);
   }

   public void quit() {
      this.goHome(null);
   }

   public boolean goingHome() {
      return this.goingHome;
   }

   public void goHome(@Nullable String line) {
      CashRegisterEntity r = this.register();
      if (r != null) {
         if (!this.getMainHandItem().isEmpty()) {
            r.returnDish(this.getMainHandItem());
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
         }

         if (this.customer != null) {
            r.beingServed.remove(this.customer);
         }

         BlockPos[] sides = r.doorSides();
         if (sides != null) {
            BlockPos doorway = new BlockPos(
               (sides[0].getX() + sides[1].getX()) / 2, sides[0].getY(), (sides[0].getZ() + sides[1].getZ()) / 2
            );
            this.exit = this.floor.contains(sides[0]) ? sides[0] : (this.floor.contains(doorway) ? doorway : (this.floor.contains(sides[1]) ? sides[1] : null));
         }
      }

      if (line != null) {
         this.say(line);
      }

      this.customer = null;
      this.glassFrom = null;
      this.pourFrom = null;
      this.restockShelf = null;
      this.step = BarWorker.Step.IDLE;
      this.register = null;
      this.goingHome = true;
      this.homeTicks = 0;
      this.routeGoal = null;
   }

   private void walkHome() {
      this.homeTicks++;
      boolean out = this.exit == null ? this.homeTicks > 60 : this.homeTicks > 40 && this.goTo(this.exit, 1.0) || this.homeTicks > 600;
      if (out) {
         if (this.openedDoor != null) {
            this.setDoor(this.openedDoor, false);
         }

         this.discard();
      }
   }

   @Nullable
   private CashRegisterEntity register() {
      return this.register != null && this.level().isLoaded(this.register) && this.level().getBlockEntity(this.register) instanceof CashRegisterEntity r
         ? r
         : null;
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      if (this.speechTicks > 0 && --this.speechTicks == 0) {
         this.entityData.set(SPEECH, "");
      }

      if (this.complainCooldown > 0) {
         this.complainCooldown--;
      }

      if (this.quitTicks > 0 && --this.quitTicks == 0) {
         this.discard();
      } else if (this.goingHome) {
         this.walkHome();
      } else {
         CashRegisterEntity r = this.register();
         if (r == null) {
            if (this.quitTicks == 0) {
               this.quitTicks = 100;
            }
         } else if (this.level() instanceof ServerLevel server) {
            if (this.level().getGameTime() - this.scannedAt > 200L) {
               this.scan(r);
            }

            this.refreshClaims();
            this.keepApart();

            this.stepTicks++;
            switch (this.step) {
               case IDLE:
                  this.idle(server, r);
                  break;
               case GLASS:
                  this.fetchGlass(r);
                  break;
               case POUR:
                  this.pour(r);
                  break;
               case SERVE:
                  this.serve(r);
            }
         }
      }
   }

   private void scan(CashRegisterEntity r) {
      this.scannedAt = this.level().getGameTime();
      this.taps.clear();
      this.racks.clear();
      this.shelves.clear();
      BlockPos c = r.getBlockPos();

      for (BlockPos p : BlockPos.betweenClosed(c.offset(-10, -3, -10), c.offset(10, 3, 10))) {
         BlockState state = this.level().getBlockState(p);
         if (state.getBlock() instanceof TapTowerBlock) {
            this.taps.add(p.immutable());
         } else if (this.level().getBlockEntity(p) instanceof RackEntity) {
            this.racks.add(p.immutable());
         } else if (this.level().getBlockEntity(p) instanceof GlassShelfEntity) {
            this.shelves.add(p.immutable());
         }
      }

      this.mapArea(r);
   }

   private void mapArea(CashRegisterEntity r) {
      this.floor.clear();
      this.area.clear();
      this.heights.clear();
      this.myHome = null;
      this.route.clear();
      this.routeGoal = null;
      this.bar = r;
      this.areaHome = r.workerHome();
      if (this.areaHome != null) {
         ArrayDeque<BlockPos> open = new ArrayDeque<>();
         this.floor.add(this.areaHome);
         open.add(this.areaHome);

         while (!open.isEmpty() && this.floor.size() < 9000) {
            BlockPos cell = open.poll();

            for (Direction d : Plane.HORIZONTAL) {
               BlockPos n = this.step(cell, d);
               if (n != null
                  && !this.floor.contains(n)
                  && Math.abs(n.getX() - r.getBlockPos().getX()) <= 32
                  && Math.abs(n.getZ() - r.getBlockPos().getZ()) <= 32) {
                  this.floor.add(n);
                  open.add(n);
               }
            }
         }

         for (BlockPos cell : this.floor) {
            if (r.behindCounter(cell)) {
               this.area.add(cell);
            }
         }
      }
   }

   public static double floorAt(BlockGetter level, BlockPos p) {
      VoxelShape here = level.getBlockState(p).getCollisionShape(level, p);
      double base;
      if (!here.isEmpty()) {
         double top = here.max(Axis.Y);
         if (top > 0.5001) {
            return Double.NaN;
         }

         base = (double)p.getY() + top;
      } else {
         BlockPos b = p.below();
         VoxelShape under = level.getBlockState(b).getCollisionShape(level, b);
         if (under.isEmpty()) {
            return Double.NaN;
         }

         double top = under.max(Axis.Y);
         if (top < 0.75 || top > 1.0001 || topArea(under, top) < 0.4) {
            return Double.NaN;
         }

         base = (double)b.getY() + top;
      }

      for (int dy = 1; dy <= 2; dy++) {
         BlockPos q = p.above(dy);
         VoxelShape s = level.getBlockState(q).getCollisionShape(level, q);
         if (!s.isEmpty() && (double)q.getY() + s.min(Axis.Y) < base + 1.8) {
            return Double.NaN;
         }
      }

      return base;
   }

   private static double topArea(VoxelShape shape, double top) {
      double area = 0.0;

      for (AABB box : shape.toAabbs()) {
         if (box.maxY >= top - 0.001) {
            area += (box.maxX - box.minX) * (box.maxZ - box.minZ);
         }
      }

      return area;
   }

   private double height(BlockPos p) {
      if (!this.passage(p)) {
         return floorAt(this.level(), p);
      } else {
         VoxelShape under = this.level().getBlockState(p.below()).getCollisionShape(this.level(), p.below());
         return under.isEmpty() ? Double.NaN : (double)(p.getY() - 1) + under.max(Axis.Y);
      }
   }

   @Nullable
   private BlockPos step(BlockPos from, Direction d) {
      double h = this.height(from);
      if (!Double.isNaN(h) && (!this.passage(from) || this.along(from, d))) {
         BlockPos n = from.relative(d);
         BlockPos best = null;
         double bestGap = 9.0;

         for (int dy = 1; dy >= -1; dy--) {
            BlockPos q = n.above(dy);
            double hq = this.height(q);
            if ((Double.isNaN(hq) || !this.passage(q) || this.along(q, d) && this.floorBeyond(q, d))
               && !Double.isNaN(hq)
               && (this.bar == null || !this.bar.onCounter(q))
               && (!(hq > h + 0.1) || this.level().getBlockEntity(q.below()) == null)) {
               double gap = Math.abs(hq - h);
               if (gap <= (this.onStair(hq > h ? q : from) ? 1.01 : 0.6) && gap < bestGap) {
                  best = q;
                  bestGap = gap;
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private boolean along(BlockPos p, Direction d) {
      BlockState state = this.level().getBlockState(p);
      return state.hasProperty(HorizontalDirectionalBlock.FACING)
         && ((Direction)state.getValue(HorizontalDirectionalBlock.FACING)).getAxis() == d.getAxis();
   }

   private boolean floorBeyond(BlockPos door, Direction d) {
      BlockPos b = door.relative(d);

      for (int dy = -1; dy <= 1; dy++) {
         if (!Double.isNaN(this.height(b.above(dy)))) {
            return true;
         }
      }

      return false;
   }

   private boolean onStair(BlockPos cell) {
      BlockState below = this.level().getBlockState(cell.below());
      return below.getBlock() instanceof StairBlock && below.getValue(StairBlock.HALF) == Half.BOTTOM;
   }

   private boolean passage(BlockPos p) {
      BlockState state = this.level().getBlockState(p);
      return state.getBlock() instanceof FenceGateBlock || state.getBlock() instanceof DoorBlock && state.is(BlockTags.WOODEN_DOORS);
   }

   @Nullable
   private BlockPos nearest(Vec3 thing, double reach) {
      return this.nearest(this.area, thing, reach);
   }

   @Nullable
   private BlockPos nearest(Set<BlockPos> cells, Vec3 thing, double reach) {
      BlockPos best = null;
      double bestDistance = reach * reach;

      for (BlockPos cell : cells) {
         double dx = (double)cell.getX() + 0.5 - thing.x;
         double dz = (double)cell.getZ() + 0.5 - thing.z;
         double d = dx * dx + dz * dz;
         if (d < bestDistance && Math.abs((double)cell.getY() - thing.y) < 3.5 && !this.passage(cell)) {
            best = cell;
            bestDistance = d;
         }
      }

      return best;
   }

   @Nullable
   private BlockPos nearestFree(Set<BlockPos> cells, Vec3 thing, double reach) {
      BlockPos best = null;
      double bestDistance = reach * reach;

      for (BlockPos cell : cells) {
         if (!this.claimed.contains(cell)) {
            double dx = (double)cell.getX() + 0.5 - thing.x;
            double dz = (double)cell.getZ() + 0.5 - thing.z;
            double d = dx * dx + dz * dz;
            if (d < bestDistance && Math.abs((double)cell.getY() - thing.y) < 3.5 && !this.passage(cell)) {
               best = cell;
               bestDistance = d;
            }
         }
      }

      return best != null ? best : this.nearest(cells, thing, reach);
   }

   @Nullable
   private BlockPos besideBlock(BlockPos thing) {
      return this.nearestFree(this.area, Vec3.atBottomCenterOf(thing), 2.5);
   }

   @Nullable
   private BlockPos serveSpot(BarPatron p) {
      BlockPos behind = this.nearestFree(this.area, p.position(), 3.5);
      if (behind != null) {
         return behind;
      } else {
         BlockPos spot = this.nearestFree(this.floor, p.position(), 3.0);
         return spot != null && !this.passage(spot) ? spot : null;
      }
   }

   // Cells other bar staff are standing on, plus cells that staff with priority are already walking to.
   private void refreshClaims() {
      long now = this.level().getGameTime();
      if (now != this.claimsAt) {
         this.claimsAt = now;
         this.claimed.clear();
         this.standing.clear();

         for (BarWorker w : this.colleagues()) {
            BlockPos at = w.blockPosition();
            Vec3 v = w.getDeltaMovement();
            boolean settled = w.routeGoal == null || w.routeGoal.equals(at) || v.x * v.x + v.z * v.z < 1.0E-4;
            if (settled) {
               this.standing.add(at);
               this.claimed.add(at);
            }

            if (w.routeGoal != null && w.getId() < this.getId()) {
               this.claimed.add(w.routeGoal);
            }
         }
      }
   }

   // Gentle nudge so two workers never end up inside each other.
   private void keepApart() {
      for (BarWorker w : this.level().getEntitiesOfClass(BarWorker.class, this.getBoundingBox().inflate(1.0), w -> w != this)) {
         double dx = this.getX() - w.getX();
         double dz = this.getZ() - w.getZ();
         double d = Math.sqrt(dx * dx + dz * dz);
         if (d < 0.8 && Math.abs(this.getY() - w.getY()) < 1.5) {
            if (d < 1.0E-4) {
               double a = (double)this.getId() * 2.399;
               dx = Math.cos(a);
               dz = Math.sin(a);
               d = 1.0;
            }

            double push = (0.8 - d) * 0.1;
            this.setDeltaMovement(this.getDeltaMovement().add(dx / d * push, 0.0, dz / d * push));
         }
      }
   }

   // The register's worker spot, or the closest free cell beside it when a colleague is already there.
   @Nullable
   private BlockPos homeSpot() {
      if (this.areaHome == null) {
         return null;
      } else if (!this.claimed.contains(this.areaHome)) {
         this.myHome = null;
         return this.areaHome;
      } else {
         if (this.myHome == null || this.claimed.contains(this.myHome) || !this.area.contains(this.myHome)) {
            BlockPos spot = this.nearestFree(this.area, Vec3.atBottomCenterOf(this.areaHome), 4.0);
            this.myHome = spot != null && !this.claimed.contains(spot) ? spot : null;
         }

         return this.myHome != null ? this.myHome : this.areaHome;
      }
   }

   private double pace() {
      return 0.91 + (double)(this.getUUID().getLeastSignificantBits() & 255L) / 255.0 * 0.18;
   }

   private boolean goTo(BlockPos goal, double speed) {
      Vec3 target = Vec3.atBottomCenterOf(goal);
      double dx = target.x - this.getX();
      double dz = target.z - this.getZ();
      double left = dx * dx + dz * dz;
      boolean level = Math.abs(target.y - this.getY()) < 1.2;
      if (level && (left < 0.0625 || goal.equals(this.arrivedAt) && left < 0.36)) {
         this.arrivedAt = goal;
         this.halt();
         return true;
      } else if (level && left < 1.69 && this.standing.contains(goal)) {
         // A colleague is standing right there; stop beside them instead of walking into them.
         this.arrivedAt = goal;
         this.halt();
         return true;
      } else {
         this.arrivedAt = null;
         if (!goal.equals(this.routeGoal)) {
            this.routeGoal = goal;
            this.plan(goal);
            this.reroutes = 0;
         }

         if (this.position().distanceToSqr(this.lastPos) > 0.0025) {
            this.lastPos = this.position();
            this.stillTicks = 0;
         } else if (++this.stillTicks > 30) {
            this.stillTicks = 0;
            this.plan(goal);
            if (++this.reroutes > 4) {
               return false;
            }
         }

         BlockPos next;
         for (next = this.route.peek(); next != null && this.horizontal(next) < 0.09; next = this.route.peek()) {
            this.route.poll();
         }

         this.skipReached();
         next = this.route.peek();
         this.doors();
         BlockPos aimAt = this.aimNode();
         double aimY = aimAt == null ? target.y : this.height(aimAt);
         Vec3 aim = aimAt == null
            ? target
            : new Vec3((double)aimAt.getX() + 0.5, Double.isNaN(aimY) ? (double)aimAt.getY() : aimY, (double)aimAt.getZ() + 0.5);
         double pace = speed * this.pace();
         if (this.route.size() <= 1 && left < 2.25) {
            // Ease into the final spot rather than stopping dead.
            pace *= Math.max(0.4, Math.sqrt(left) / 1.5);
         }

         this.getMoveControl().setWantedPosition(aim.x, aim.y, aim.z, pace);
         this.getLookControl().setLookAt(aim.x, this.getEyeY(), aim.z);
         return false;
      }
   }

   // Drop route nodes we already walked past while cutting a corner.
   private void skipReached() {
      BlockPos at = this.blockPosition();
      int i = 0;
      int reached = -1;

      for (BlockPos c : this.route) {
         if (i >= 6) {
            break;
         }

         if (c.getX() == at.getX() && c.getZ() == at.getZ() && Math.abs(c.getY() - at.getY()) <= 1) {
            reached = i;
         }

         i++;
      }

      for (int k = 0; k < reached; k++) {
         this.route.poll();
      }
   }

   // Furthest upcoming route node we can walk to in a straight line, so paths curve instead of zig-zagging.
   @Nullable
   private BlockPos aimNode() {
      BlockPos best = this.route.peek();
      if (best != null && !this.passage(best)) {
         int i = 0;

         for (BlockPos c : this.route) {
            if (i++ >= 6 || c.getY() != best.getY() || this.passage(c) || !this.clearLine(c)) {
               break;
            }

            best = c;
         }

         return best;
      } else {
         return best;
      }
   }

   private boolean clearLine(BlockPos c) {
      double sx = this.getX();
      double sz = this.getZ();
      double ex = (double)c.getX() + 0.5;
      double ez = (double)c.getZ() + 0.5;
      double len = Math.sqrt((ex - sx) * (ex - sx) + (ez - sz) * (ez - sz));
      if (len < 0.001) {
         return true;
      } else {
         double h = this.cachedHeight(c);
         if (Double.isNaN(h)) {
            return false;
         } else {
            double nx = -(ez - sz) / len * 0.35;
            double nz = (ex - sx) / len * 0.35;
            int steps = (int)Math.ceil(len / 0.25);

            for (int step = 0; step <= steps; step++) {
               double t = (double)step / (double)steps;
               double x = sx + (ex - sx) * t;
               double z = sz + (ez - sz) * t;

               for (int side = -1; side <= 1; side++) {
                  BlockPos p = new BlockPos((int)Math.floor(x + nx * (double)side), c.getY(), (int)Math.floor(z + nz * (double)side));
                  if (!this.floor.contains(p) || this.passage(p) || this.standing.contains(p)) {
                     return false;
                  }

                  double ph = this.cachedHeight(p);
                  if (Double.isNaN(ph) || Math.abs(ph - h) > 0.3) {
                     return false;
                  }
               }
            }

            return true;
         }
      }
   }

   private double cachedHeight(BlockPos p) {
      Double h = this.heights.get(p);
      if (h == null) {
         h = this.height(p);
         this.heights.put(p.immutable(), h);
      }

      return h;
   }

   private void doors() {
      if (this.openedDoor != null && this.horizontal(this.openedDoor) > 6.25) {
         this.setDoor(this.openedDoor, false);
         this.openedDoor = null;
      }

      int i = 0;

      for (BlockPos c : this.route) {
         if (i++ >= 3) {
            break;
         }

         if (this.passage(c) && this.horizontal(c) < 3.24 && this.setDoor(c, true)) {
            if (this.openedDoor != null && !this.openedDoor.equals(c)) {
               this.setDoor(this.openedDoor, false);
            }

            this.openedDoor = c;
         }
      }
   }

   private boolean setDoor(BlockPos p, boolean open) {
      BlockState state = this.level().getBlockState(p);
      if (state.getBlock() instanceof DoorBlock door) {
         if ((Boolean)state.getValue(DoorBlock.OPEN) == open) {
            return false;
         } else {
            door.setOpen(this, this.level(), state, p, open);
            return true;
         }
      } else if (state.getBlock() instanceof FenceGateBlock) {
         if ((Boolean)state.getValue(FenceGateBlock.OPEN) == open) {
            return false;
         } else {
            this.level().setBlock(p, (BlockState)state.setValue(FenceGateBlock.OPEN, open), 10);
            this.level().playSound(null, p, open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
         }
      } else {
         return false;
      }
   }

   private double horizontal(BlockPos cell) {
      double dx = (double)cell.getX() + 0.5 - this.getX();
      double dz = (double)cell.getZ() + 0.5 - this.getZ();
      return dx * dx + dz * dz;
   }

   private void halt() {
      this.route.clear();
      this.getNavigation().stop();
      this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
      this.setZza(0.0F);
      this.setXxa(0.0F);
   }

   private void plan(BlockPos goal) {
      this.route.clear();
      BlockPos start = this.floor.contains(this.blockPosition()) ? this.blockPosition() : this.nearest(this.floor, this.position(), 4.0);
      if (start != null && this.floor.contains(goal)) {
         Map<BlockPos, BlockPos> from = this.search(start, goal, true);
         if (!from.containsKey(goal)) {
            from = this.search(start, goal, false);
         }

         if (from.containsKey(goal)) {
            for (BlockPos c = goal; !c.equals(start); c = from.get(c)) {
               this.route.addFirst(c);
            }

            if (!start.equals(this.blockPosition())) {
               this.route.addFirst(start);
            }
         }
      }
   }

   private Map<BlockPos, BlockPos> search(BlockPos start, BlockPos goal, boolean aroundColleagues) {
      Map<BlockPos, BlockPos> from = new HashMap<>();
      ArrayDeque<BlockPos> open = new ArrayDeque<>();
      open.add(start);
      from.put(start, start);

      while (!open.isEmpty()) {
         BlockPos cell = open.poll();
         if (cell.equals(goal)) {
            break;
         }

         for (Direction d : Plane.HORIZONTAL) {
            BlockPos n = this.step(cell, d);
            if (n != null && this.floor.contains(n) && !from.containsKey(n) && (!aroundColleagues || n.equals(goal) || !this.standing.contains(n))) {
               from.put(n, cell);
               open.add(n);
            }
         }
      }

      return from;
   }

   private void idle(ServerLevel server, CashRegisterEntity r) {
      if (this.restockShelf != null) {
         if (this.getMainHandItem().isEmpty() && this.stepTicks % 10 == 0 && !this.noOneWaiting(server, r)) {
            // Customers first: only carry glasses over when nobody is waiting.
            this.finishRestock(r);
         } else {
            this.restock(r);
         }
      } else if (this.stepTicks % 40 == 20 && this.noOneWaiting(server, r) && this.startRestock(r)) {
         this.idleSpot = null;
      } else if (this.stepTicks % 10 == 0 && this.takeOrder(server, r)) {
         this.idleSpot = null;
      } else {
         if (this.stepTicks % 600 == 300) {
            r.collectEmpties();
         }

         if (this.idleSpot == null && this.idleWait > 0) {
            this.idleWait--;
         }

         if (this.idleSpot == null && this.idleWait <= 0) {
            this.idleSpot = this.pickIdleSpot(r);
            this.idleTicks = 0;
            this.idleDo = -1;
            if (this.idleSpot == null) {
               this.idleWait = 200;
            }
         }

         if (this.idleSpot != null) {
            if (!this.goTo(this.idleSpot, 0.85)) {
               if (this.reroutes > 4) {
                  this.idleSpot = null;
                  this.idleWait = 150;
               }
            } else {
               this.idleTicks++;
               this.chore(server, r);
               if (this.idleTicks >= this.idleLength) {
                  this.idleSpot = null;
                  this.idleWait = 120 + this.random.nextInt(300);
               }
            }
         } else if (this.areaHome == null || this.goTo(this.homeSpot(), 0.9)) {
            if (this.stepTicks % 30 == 0) {
               List<BarPatron> waiting = r.waitingCustomers(server);
               Entity look = (Entity)(waiting.isEmpty() ? this.level().getNearestPlayer(this, 8.0) : (Entity)waiting.get(0));
               if (look != null) {
                  this.getLookControl().setLookAt(look, 20.0F, 20.0F);
               } else {
                  Vec3 out = this.position().add(Vec3.atLowerCornerOf(r.customerSide().getNormal()).scale(3.0));
                  this.getLookControl().setLookAt(out.x, this.getEyeY(), out.z);
               }
            }
         }
      }
   }

   private boolean noOneWaiting(ServerLevel server, CashRegisterEntity r) {
      return r.waitingCustomers(server).isEmpty();
   }

   // Glassware handed back to the register gets carried over to the glass shelves.
   private boolean startRestock(CashRegisterEntity r) {
      for (ItemStack s : r.dishes) {
         if (!s.isEmpty() && GlassShelfEntity.glassware(s) && this.shelfWithRoom(s) != null) {
            this.restockShelf = this.shelfWithRoom(s);
            this.restockTicks = 0;
            return true;
         }
      }

      return false;
   }

   @Nullable
   private BlockPos shelfWithRoom(ItemStack glass) {
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;

      for (BlockPos p : this.shelves) {
         if (this.level().getBlockEntity(p) instanceof GlassShelfEntity shelf && room(shelf, glass) > 0 && this.besideBlock(p) != null) {
            double d = p.distSqr(this.blockPosition());
            if (d < bestDistance) {
               best = p;
               bestDistance = d;
            }
         }
      }

      return best;
   }

   private static int room(GlassShelfEntity shelf, ItemStack glass) {
      int room = 0;

      for (ItemStack g : shelf.glasses) {
         if (g.isEmpty()) {
            room += glass.getMaxStackSize();
         } else if (ItemStack.isSameItemSameComponents(g, glass)) {
            room += g.getMaxStackSize() - g.getCount();
         }
      }

      return room;
   }

   private void restock(CashRegisterEntity r) {
      if (++this.restockTicks <= 500 && this.level().getBlockEntity(this.restockShelf) instanceof GlassShelfEntity shelf) {
         ItemStack held = this.getMainHandItem();
         if (held.isEmpty()) {
            BlockPos home = this.homeSpot();
            if (home != null && !this.goTo(home, 1.0)) {
               if (this.reroutes > 4) {
                  this.finishRestock(r);
               }
            } else {
               this.getLookControl().setLookAt(Vec3.atCenterOf(r.getBlockPos()));
               ItemStack take = ItemStack.EMPTY;

               for (ItemStack s : r.dishes) {
                  int room = GlassShelfEntity.glassware(s) ? room(shelf, s) : 0;
                  if (!s.isEmpty() && room > 0) {
                     take = s.split(Math.min(16, room));
                     r.setChanged();
                     break;
                  }
               }

               if (take.isEmpty()) {
                  this.finishRestock(r);
               } else {
                  this.swing(InteractionHand.MAIN_HAND);
                  this.setItemInHand(InteractionHand.MAIN_HAND, take);
                  this.routeGoal = null;
               }
            }
         } else {
            BlockPos spot = this.besideBlock(this.restockShelf);
            if (spot == null) {
               this.finishRestock(r);
            } else if (!this.goTo(spot, 1.0)) {
               if (this.reroutes > 4) {
                  this.finishRestock(r);
               }
            } else {
               this.getLookControl().setLookAt(Vec3.atCenterOf(this.restockShelf));
               if (this.restockTicks % 6 == 0) {
                  this.swing(InteractionHand.MAIN_HAND);
                  shelf.insert(held, -1);
                  if (held.isEmpty()) {
                     this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                     this.finishRestock(r);
                  } else {
                     BlockPos other = this.shelfWithRoom(held);
                     if (other == null) {
                        this.finishRestock(r);
                     } else {
                        this.restockShelf = other;
                        this.routeGoal = null;
                     }
                  }
               }
            }
         }
      } else {
         this.finishRestock(r);
      }
   }

   private void finishRestock(CashRegisterEntity r) {
      if (!this.getMainHandItem().isEmpty()) {
         r.returnDish(this.getMainHandItem());
         this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      }

      this.restockShelf = null;
      this.restockTicks = 0;
      this.routeGoal = null;
      this.idleWait = Math.max(this.idleWait, 40);
   }

   @Nullable
   public BlockPos idleSpot() {
      return this.idleSpot;
   }

   private List<BarWorker> colleagues() {
      return this.level().getEntitiesOfClass(BarWorker.class, this.getBoundingBox().inflate(32.0), w -> w != this && w.register != null && w.register.equals(this.register));
   }

   @Nullable
   private BlockPos pickIdleSpot(CashRegisterEntity r) {
      if (this.areaHome == null) {
         return null;
      } else {
         List<BarWorker> others = this.colleagues();
         List<BlockPos> options = new ArrayList<>();

         for (BlockPos cell : this.area) {
            if (!this.passage(cell) && !this.claimed.contains(cell) && !(cell.distSqr(this.areaHome) > 36.0) && !(cell.distSqr(this.blockPosition()) < 2.25)) {
               boolean taken = false;

               for (BarWorker w : others) {
                  if (w.distanceToSqr(Vec3.atBottomCenterOf(cell)) < 9.0 || w.idleSpot() != null && w.idleSpot().distSqr(cell) < 9.0) {
                     taken = true;
                  }
               }

               if (!taken) {
                  options.add(cell);
               }
            }
         }

         if (options.isEmpty()) {
            return null;
         } else {
            return this.random.nextInt(4) == 0 && !this.blockPosition().equals(this.areaHome) && !this.claimed.contains(this.areaHome)
               ? this.areaHome
               : options.get(this.random.nextInt(options.size()));
         }
      }
   }

   private void chore(ServerLevel server, CashRegisterEntity r) {
      if (this.idleTicks == 1) {
         this.idleLength = 90 + this.random.nextInt(110);
         BarPatron guest = this.nearbyGuest(server, r);
         this.idleDo = guest != null && this.random.nextInt(5) < 3 ? 1 : (this.random.nextBoolean() ? 0 : 2);
         if (this.idleDo == 1) {
            this.chatTarget = guest.getUUID();
         }
      }

      switch (this.idleDo) {
         case 0:
            Vec3 bar = this.position().add(Vec3.atLowerCornerOf(r.customerSide().getNormal()).scale(1.5)).add(0.0, 0.6, 0.0);
            this.getLookControl().setLookAt(bar.x, bar.y, bar.z);
            if (this.idleTicks % 18 == 4) {
               this.swing(InteractionHand.MAIN_HAND);
            }
            break;
         case 1:
            BarPatron guest = this.chatTarget != null && server.getEntity(this.chatTarget) instanceof BarPatron p && p.isAlive() ? p : null;
            if (guest == null) {
               this.idleDo = 2;
               return;
            }

            this.getLookControl().setLookAt(guest, 30.0F, 30.0F);
            if (this.idleTicks == 14) {
               this.talkTo(guest);
            }
            break;
         default:
            if (this.idleTicks % 45 == 5) {
               BarPatron any = this.nearbyGuest(server, r);
               Vec3 out = this.position()
                  .add(Vec3.atLowerCornerOf(r.customerSide().getNormal()).scale(3.0))
                  .add(this.random.nextGaussian() * 2.0, 0.0, this.random.nextGaussian() * 2.0);
               if (any != null && this.random.nextBoolean()) {
                  this.getLookControl().setLookAt(any, 20.0F, 20.0F);
               } else {
                  this.getLookControl().setLookAt(out.x, this.getEyeY(), out.z);
               }
            }
      }
   }

   @Nullable
   private BarPatron nearbyGuest(ServerLevel server, CashRegisterEntity r) {
      BarPatron best = null;
      double bestDistance = 36.0;

      for (BarPatron p : server.getEntitiesOfClass(BarPatron.class, this.getBoundingBox().inflate(6.0, 2.0, 6.0))) {
         if (p.isAlive() && !p.talking() && !p.unserved() && p.state() == BarPatron.State.ENJOYING && r.getBlockPos().equals(p.registerPos())) {
            double d = p.distanceToSqr(this);
            if (d < bestDistance) {
               best = p;
               bestDistance = d;
            }
         }
      }

      return best;
   }

   private void talkTo(BarPatron guest) {
      String drink = guest.product() == null ? "drink" : guest.product().name().getString();
      boolean empty = guest.getMainHandItem().isEmpty() && guest.sipsLeft() <= 0;
      if (empty) {
         this.say(this.pick("Can I get you another?", "Same again?", "Top you up, " + (guest.slim() ? "miss" : "sir") + "?", "Another round?"));
         if (this.random.nextInt(3) != 0) {
            guest.sayLater(this.pick("Go on then!", "Why not.", "You read my mind."), 40);
            guest.endLinger(true);
         } else {
            guest.sayLater(this.pick("I'm good, thanks.", "Not just yet.", "Maybe in a bit."), 40);
         }
      } else if (guest.getMainHandItem().getItem() instanceof CigarItem) {
         this.say(this.pick("That smells good.", "Mind the holder, now.", "Nice smoke."));
         guest.sayLater(this.pick("Cheers, it's a good one.", "Only on a night out.", "Don't tell my doctor."), 40);
      } else {
         switch (this.random.nextInt(4)) {
            case 0:
               this.say("How's the " + drink + "?");
               guest.sayLater(this.pick("Perfect, thanks!", "Best in town.", "Colder would be nice. Kidding!"), 40);
               break;
            case 1:
               this.say(this.pick("Busy night, isn't it?", "Everything alright over here?", "Shout if you need anything."));
               guest.sayLater(this.pick("All good, thanks.", "Couldn't be better.", "Keep them coming!"), 40);
               break;
            case 2:
               this.say(this.pick("Good to see you in again.", "Haven't seen you around.", "First time here?"));
               guest.sayLater(this.pick("Nice place you've got.", "I'll be back.", "Heard good things."), 40);
               break;
            default:
               this.say(this.pick("Mind the step on your way out.", "Last orders are late tonight.", "Try the lager sometime."));
               guest.sayLater(this.pick("Will do.", "Good to know!", "Cheers."), 40);
         }

         guest.emote(BarTalk.Emote.NOD);
      }

      guest.look(this.position().add(0.0, 1.5, 0.0), 40);
   }

   private boolean takeOrder(ServerLevel server, CashRegisterEntity r) {
      List<BarPatron> order = new ArrayList<>(r.waitingCustomers(server));
      Map<BarPatron, Double> cost = new HashMap<>();

      for (BarPatron p : order) {
         BlockPos spot = this.serveSpot(p);
         cost.put(p, spot == null ? Double.MAX_VALUE : (double)(this.area.contains(spot) ? 0 : 1000) + spot.distSqr(this.blockPosition()));
      }

      order.sort(Comparator.comparingDouble(cost::get));

      for (BarPatron p : order) {
         if (!r.beingServed.contains(p.getUUID())) {
            BarProducts.Product product = p.product();
            boolean beer = product.kind() == BarProducts.Kind.BEER;
            boolean shot = product.kind() == BarProducts.Kind.SPIRIT && p.wantsShot();
            if ((beer || shot) && cost.get(p) != Double.MAX_VALUE) {
               BlockPos source = beer ? this.tapFor(product.id()) : this.rackFor(product.id());
               if (source != null) {
                  Item needed = beer ? (Item)Drunkard.BEER_MUG.get() : (Item)Drunkard.SHOT_GLASS.get();
                  BlockPos shelf = this.shelfWith(needed);
                  if (shelf != null || dishesHave(r, needed)) {
                     r.beingServed.add(p.getUUID());
                     this.customer = p.getUUID();
                     this.pourFrom = source;
                     this.glassFrom = shelf;
                     this.next(BarWorker.Step.GLASS);
                     if (this.random.nextInt(3) == 0) {
                        this.say(this.pick("Coming right up!", "One sec!", "I've got you.", "Be right with you."));
                     }

                     return true;
                  }

                  this.complain(beer ? "Somebody put some mugs on the shelf!" : "We need shot glasses on the shelf!");
               } else {
                  this.complain(beer ? "We're out of " + product.name().getString() + " on tap!" : "No " + product.name().getString() + " in the racks!");
               }
            }
         }
      }

      return false;
   }

   @Nullable
   private BlockPos tapFor(String beer) {
      for (BlockPos t : this.taps) {
         BlockState state = this.level().getBlockState(t);
         if (state.getBlock() instanceof TapTowerBlock && this.besideBlock(t) != null) {
            for (int tap = -1; tap <= 1; tap++) {
               KegEntity keg = TapTowerBlock.keg(this.level(), t, state, tap);
               if (keg != null && keg.servings > 0 && keg.beer.equals(beer)) {
                  return t;
               }
            }
         }
      }

      return null;
   }

   @Nullable
   private BlockPos rackFor(String spirit) {
      for (BlockPos p : this.racks) {
         BlockEntity var5 = this.level().getBlockEntity(p);
         if (var5 instanceof RackEntity) {
            RackEntity rack = (RackEntity)var5;
            if (this.besideBlock(p) != null) {
               for (ItemStack b : rack.bottles) {
                  if (!b.isEmpty() && WineData.variety(b).equals(spirit)) {
                     return p;
                  }
               }
            }
         }
      }

      return null;
   }

   @Nullable
   private BlockPos shelfWith(Item glassItem) {
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;

      for (BlockPos p : this.shelves) {
         BlockEntity d = this.level().getBlockEntity(p);
         if (d instanceof GlassShelfEntity) {
            GlassShelfEntity s = (GlassShelfEntity)d;
            if (s.count(glassItem) > 0 && this.besideBlock(p) != null) {
               double dx = p.distSqr(this.blockPosition());
               if (dx < bestDistance) {
                  best = p;
                  bestDistance = dx;
               }
            }
         }
      }

      return best;
   }

   private static boolean dishesHave(CashRegisterEntity r, Item item) {
      for (ItemStack s : r.dishes) {
         if (s.is(item)) {
            return true;
         }
      }

      return false;
   }

   private void fetchGlass(CashRegisterEntity r) {
      BarPatron p = this.target();
      if (p == null) {
         this.drop(r);
      } else {
         Item needed = p.product().kind() == BarProducts.Kind.BEER ? (Item)Drunkard.BEER_MUG.get() : (Item)Drunkard.SHOT_GLASS.get();
         ItemStack glass = ItemStack.EMPTY;
         if (this.glassFrom == null) {
            if (this.areaHome != null && !this.arrive(this.homeSpot(), 1.0)) {
               return;
            }

            for (ItemStack s : r.dishes) {
               if (s.is(needed)) {
                  glass = s.split(1);
                  r.setChanged();
                  break;
               }
            }
         } else {
            BlockPos spot = this.besideBlock(this.glassFrom);
            if (spot == null) {
               this.drop(r);
               return;
            }

            if (!this.arrive(spot, 1.0)) {
               return;
            }

            this.getLookControl().setLookAt(Vec3.atCenterOf(this.glassFrom));
            if (this.level().getBlockEntity(this.glassFrom) instanceof GlassShelfEntity shelf) {
               glass = shelf.take(needed);
            }
         }

         if (glass.isEmpty()) {
            this.drop(r);
         } else {
            this.swing(InteractionHand.MAIN_HAND);
            this.setItemInHand(InteractionHand.MAIN_HAND, glass);
            this.next(BarWorker.Step.POUR);
         }
      }
   }

   private void pour(CashRegisterEntity r) {
      BarPatron p = this.target();
      if (p != null && this.pourFrom != null) {
         BlockPos spot = this.besideBlock(this.pourFrom);
         if (spot == null) {
            this.drop(r);
         } else if (this.arrive(spot, 1.0)) {
            this.getLookControl().setLookAt(Vec3.atCenterOf(this.pourFrom));
            if (this.stepTicks >= 6) {
               ItemStack drink = ItemStack.EMPTY;
               String id = p.product().id();
               BlockState state = this.level().getBlockState(this.pourFrom);
               if (state.getBlock() instanceof TapTowerBlock) {
                  for (int tap = -1; tap <= 1 && drink.isEmpty(); tap++) {
                     KegEntity keg = TapTowerBlock.keg(this.level(), this.pourFrom, state, tap);
                     if (keg != null && keg.servings > 0 && keg.beer.equals(id)) {
                        drink = keg.draw();
                     }
                  }

                  if (!drink.isEmpty()) {
                     this.level().playSound(null, this.pourFrom, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8F, 0.7F);
                  }
               } else if (this.level().getBlockEntity(this.pourFrom) instanceof RackEntity rack) {
                  for (int i = 0; i < rack.bottles.size() && drink.isEmpty(); i++) {
                     ItemStack bottle = (ItemStack)rack.bottles.get(i);
                     if (!bottle.isEmpty() && WineData.variety(bottle).equals(id)) {
                        drink = ShotGlassItem.pour(bottle);
                        ItemStack rest = WineData.pourOne(bottle);
                        if (rest.is(Items.GLASS_BOTTLE)) {
                           rack.bottles.set(i, ItemStack.EMPTY);
                           r.returnDish(rest);
                        } else {
                           rack.bottles.set(i, rest);
                        }

                        rack.sync();
                     }
                  }

                  if (!drink.isEmpty()) {
                     this.level().playSound(null, this.pourFrom, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 1.3F);
                  }
               }

               if (drink.isEmpty()) {
                  this.complain("Ran out!");
                  this.drop(r);
               } else {
                  this.swing(InteractionHand.MAIN_HAND);
                  this.setItemInHand(InteractionHand.MAIN_HAND, drink);
                  this.next(BarWorker.Step.SERVE);
               }
            }
         }
      } else {
         this.drop(r);
      }
   }

   private void serve(CashRegisterEntity r) {
      BarPatron p = this.target();
      BlockPos spot = p == null ? null : this.serveSpot(p);
      if (spot == null) {
         this.drop(r);
      } else {
         if (this.handTicks == 0) {
            this.farTrip = !this.area.contains(spot);
            if (!this.arrive(spot, 1.0)) {
               return;
            }
         }

         this.getLookControl().setLookAt(p, 40.0F, 40.0F);
         this.setYBodyRot(this.rotTo(p));
         this.setYRot(this.rotTo(p));
         if (++this.handTicks == 8) {
            ItemStack drink = this.getMainHandItem();
            if (p.serveFromWorker(drink)) {
               this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
               this.swing(InteractionHand.MAIN_HAND);
               this.say(
                  this.farTrip
                     ? this.pick(
                        "Here you go, enjoy your drink!", "One pint, as ordered.", "There you are. Anything else, just give me a shout.", "Fresh from the tap!"
                     )
                     : this.pick("Here you go!", "Enjoy!", "There you are.", "Cheers!")
               );
            }
         }

         if (this.farTrip && this.handTicks == 30 && this.random.nextBoolean()) {
            this.say(this.pick("Give me a wave if you need anything.", "Holler if you want another.", "Enjoy your night!"));
         }

         if (this.handTicks >= (this.farTrip ? 45 : 16)) {
            this.drop(r);
         }
      }
   }

   private float rotTo(Entity e) {
      return (float)(Math.toDegrees(Math.atan2(e.getZ() - this.getZ(), e.getX() - this.getX())) - 90.0);
   }

   private boolean arrive(BlockPos cell, double speed) {
      boolean there = this.goTo(cell, speed);
      if (!there && (this.reroutes > 4 || this.stepTicks > 400)) {
         CashRegisterEntity r = this.register();
         if (r != null) {
            this.drop(r);
         }
      }

      return there;
   }

   @Nullable
   private BarPatron target() {
      return this.customer != null
            && this.level() instanceof ServerLevel server
            && server.getEntity(this.customer) instanceof BarPatron p
            && p.isAlive()
            && p.unserved()
            && p.product() != null
         ? p
         : null;
   }

   private void next(BarWorker.Step s) {
      this.step = s;
      this.stepTicks = 0;
      this.handTicks = 0;
      this.reroutes = 0;
      this.routeGoal = null;
   }

   private void drop(CashRegisterEntity r) {
      this.idleSpot = null;
      this.idleWait = 60 + this.random.nextInt(160);
      if (!this.getMainHandItem().isEmpty()) {
         r.returnDish(this.getMainHandItem());
         this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      }

      if (this.customer != null) {
         r.beingServed.remove(this.customer);
      }

      this.customer = null;
      this.glassFrom = null;
      this.pourFrom = null;
      this.next(BarWorker.Step.IDLE);
   }

   private void complain(String line) {
      if (this.complainCooldown == 0) {
         this.say(line);
         this.complainCooldown = 1200;
      }
   }

   private String pick(String... lines) {
      return lines[this.random.nextInt(lines.length)];
   }

   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (!this.level().isClientSide) {
         this.getLookControl().setLookAt(player, 30.0F, 30.0F);
         this.say(
            this.step == BarWorker.Step.IDLE
               ? this.pick("Evening, boss.", "All quiet here.", "Keep the taps full and I'll keep them happy.", "Need anything, boss?")
               : this.pick("On it, boss!", "Busy busy!")
         );
      }

      return InteractionResult.sidedSuccess(this.level().isClientSide);
   }

   public boolean hurt(DamageSource source, float amount) {
      if (source.getEntity() instanceof Player && !this.level().isClientSide) {
         this.say(this.pick("Hey! I work here!", "Ow! What was that for, boss?"));
      }

      return super.hurt(source, amount);
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.register != null && !this.goingHome) {
         tag.putLong("Register", this.register.asLong());
      }

      tag.putInt("Skin", this.skin());
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.register = tag.contains("Register") ? BlockPos.of(tag.getLong("Register")) : null;
      this.entityData.set(SKIN, tag.getInt("Skin") | 1048576);
   }

   private static enum Step {
      IDLE,
      GLASS,
      POUR,
      SERVE;
   }

   private static final class WalkControl extends MoveControl {
      private float pace;

      WalkControl(Mob mob) {
         super(mob);
      }

      public void tick() {
         if (this.operation != Operation.MOVE_TO) {
            this.pace = 0.0F;
            super.tick();
         } else {
            this.operation = Operation.WAIT;
            double dx = this.wantedX - this.mob.getX();
            double dz = this.wantedZ - this.mob.getZ();
            if (!(dx * dx + dz * dz < 2.5E-7) && !(this.speedModifier <= 0.0)) {
               // Turn gradually and slow down while turning, then ease up to full pace.
               float want = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
               float off = Math.abs(Mth.wrapDegrees(want - this.mob.getYRot()));
               this.mob.setYRot(this.rotlerp(this.mob.getYRot(), want, off > 100.0F ? 30.0F : 18.0F));
               float after = Math.abs(Mth.wrapDegrees(want - this.mob.getYRot()));
               float facing = after < 25.0F ? 1.0F : Math.max(0.1F, (float)Math.cos(Math.toRadians((double)after)));
               float target = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)) * facing;
               this.pace = this.pace + (target - this.pace) * (target > this.pace ? 0.2F : 0.35F);
               this.mob.setSpeed(this.pace);
            } else {
               this.pace = 0.0F;
               this.mob.setZza(0.0F);
            }
         }
      }
   }
}
