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
   private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.m_135353_(BarWorker.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<String> SPEECH = SynchedEntityData.m_135353_(BarWorker.class, EntityDataSerializers.f_135030_);
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
   private Vec3 lastPos = Vec3.f_82478_;
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
   private BlockPos restockShelf;
   private int restockTicks;

   public BarWorker(EntityType<? extends BarWorker> type, Level level) {
      super(type, level);
      this.m_21530_();
      this.f_21342_ = new BarWorker.WalkControl(this);
   }

   public static Builder createAttributes() {
      return Mob.m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.25).m_22268_(Attributes.f_22277_, 32.0);
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(SKIN, 1048576);
      this.f_19804_.m_135372_(SPEECH, "");
   }

   protected void m_8099_() {
      this.f_21345_.m_25352_(0, new FloatGoal(this));
   }

   public boolean m_6785_(double distance) {
      return false;
   }

   protected boolean m_8028_() {
      return false;
   }

   public boolean m_6094_() {
      return false;
   }

   protected void m_6138_() {
   }

   public int skin() {
      return (Integer)this.f_19804_.m_135370_(SKIN);
   }

   public boolean slim() {
      return (this.skin() & 1) == 1;
   }

   public String speech() {
      return (String)this.f_19804_.m_135370_(SPEECH);
   }

   @Nullable
   public BlockPos registerPos() {
      return this.register;
   }

   public void say(String line) {
      line = Medieval.speak(this.skin(), line);
      this.f_19804_.m_135381_(SPEECH, line);
      this.speechTicks = 50 + line.length() * 2;
   }

   public void hire(BlockPos register, int look) {
      this.register = register;
      this.f_19804_.m_135381_(SKIN, look & 65535 | 1048576 | look & Medieval.SKIN_BIT);
   }

   public void setMedieval(boolean medieval) {
      int skin = this.skin() & ~Medieval.SKIN_BIT;
      this.f_19804_.m_135381_(SKIN, medieval ? skin | Medieval.SKIN_BIT : skin);
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
         if (!this.m_21205_().m_41619_()) {
            r.returnDish(this.m_21205_());
            this.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
         }

         if (this.customer != null) {
            r.beingServed.remove(this.customer);
         }

         BlockPos[] sides = r.doorSides();
         if (sides != null) {
            BlockPos doorway = new BlockPos(
               (sides[0].m_123341_() + sides[1].m_123341_()) / 2, sides[0].m_123342_(), (sides[0].m_123343_() + sides[1].m_123343_()) / 2
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

         this.m_146870_();
      }
   }

   @Nullable
   private CashRegisterEntity register() {
      return this.register != null && this.m_9236_().m_46749_(this.register) && this.m_9236_().m_7702_(this.register) instanceof CashRegisterEntity r
         ? r
         : null;
   }

   protected void m_8024_() {
      super.m_8024_();
      if (this.speechTicks > 0 && --this.speechTicks == 0) {
         this.f_19804_.m_135381_(SPEECH, "");
      }

      if (this.complainCooldown > 0) {
         this.complainCooldown--;
      }

      if (this.quitTicks > 0 && --this.quitTicks == 0) {
         this.m_146870_();
      } else if (this.goingHome) {
         this.walkHome();
      } else {
         CashRegisterEntity r = this.register();
         if (r == null) {
            if (this.quitTicks == 0) {
               this.quitTicks = 100;
            }
         } else if (this.m_9236_() instanceof ServerLevel server) {
            if (this.m_9236_().m_46467_() - this.scannedAt > 200L) {
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
      this.scannedAt = this.m_9236_().m_46467_();
      this.taps.clear();
      this.racks.clear();
      this.shelves.clear();
      BlockPos c = r.m_58899_();

      for (BlockPos p : BlockPos.m_121940_(c.m_7918_(-10, -3, -10), c.m_7918_(10, 3, 10))) {
         BlockState state = this.m_9236_().m_8055_(p);
         if (state.m_60734_() instanceof TapTowerBlock) {
            this.taps.add(p.m_7949_());
         } else if (this.m_9236_().m_7702_(p) instanceof RackEntity) {
            this.racks.add(p.m_7949_());
         } else if (this.m_9236_().m_7702_(p) instanceof GlassShelfEntity) {
            this.shelves.add(p.m_7949_());
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
                  && Math.abs(n.m_123341_() - r.m_58899_().m_123341_()) <= 32
                  && Math.abs(n.m_123343_() - r.m_58899_().m_123343_()) <= 32) {
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
      VoxelShape here = level.m_8055_(p).m_60812_(level, p);
      double base;
      if (!here.m_83281_()) {
         double top = here.m_83297_(Axis.Y);
         if (top > 0.5001) {
            return Double.NaN;
         }

         base = (double)p.m_123342_() + top;
      } else {
         BlockPos b = p.m_7495_();
         VoxelShape under = level.m_8055_(b).m_60812_(level, b);
         if (under.m_83281_()) {
            return Double.NaN;
         }

         double top = under.m_83297_(Axis.Y);
         if (top < 0.75 || top > 1.0001 || topArea(under, top) < 0.4) {
            return Double.NaN;
         }

         base = (double)b.m_123342_() + top;
      }

      for (int dy = 1; dy <= 2; dy++) {
         BlockPos q = p.m_6630_(dy);
         VoxelShape s = level.m_8055_(q).m_60812_(level, q);
         if (!s.m_83281_() && (double)q.m_123342_() + s.m_83288_(Axis.Y) < base + 1.8) {
            return Double.NaN;
         }
      }

      return base;
   }

   private static double topArea(VoxelShape shape, double top) {
      double area = 0.0;

      for (AABB box : shape.m_83299_()) {
         if (box.f_82292_ >= top - 0.001) {
            area += (box.f_82291_ - box.f_82288_) * (box.f_82293_ - box.f_82290_);
         }
      }

      return area;
   }

   private double height(BlockPos p) {
      if (!this.passage(p)) {
         return floorAt(this.m_9236_(), p);
      } else {
         VoxelShape under = this.m_9236_().m_8055_(p.m_7495_()).m_60812_(this.m_9236_(), p.m_7495_());
         return under.m_83281_() ? Double.NaN : (double)(p.m_123342_() - 1) + under.m_83297_(Axis.Y);
      }
   }

   @Nullable
   private BlockPos step(BlockPos from, Direction d) {
      double h = this.height(from);
      if (!Double.isNaN(h) && (!this.passage(from) || this.along(from, d))) {
         BlockPos n = from.m_121945_(d);
         BlockPos best = null;
         double bestGap = 9.0;

         for (int dy = 1; dy >= -1; dy--) {
            BlockPos q = n.m_6630_(dy);
            double hq = this.height(q);
            if ((Double.isNaN(hq) || !this.passage(q) || this.along(q, d) && this.floorBeyond(q, d))
               && !Double.isNaN(hq)
               && (this.bar == null || !this.bar.onCounter(q))
               && (!(hq > h + 0.1) || this.m_9236_().m_7702_(q.m_7495_()) == null)) {
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
      BlockState state = this.m_9236_().m_8055_(p);
      return state.m_61138_(HorizontalDirectionalBlock.f_54117_)
         && ((Direction)state.m_61143_(HorizontalDirectionalBlock.f_54117_)).m_122434_() == d.m_122434_();
   }

   private boolean floorBeyond(BlockPos door, Direction d) {
      BlockPos b = door.m_121945_(d);

      for (int dy = -1; dy <= 1; dy++) {
         if (!Double.isNaN(this.height(b.m_6630_(dy)))) {
            return true;
         }
      }

      return false;
   }

   private boolean onStair(BlockPos cell) {
      BlockState below = this.m_9236_().m_8055_(cell.m_7495_());
      return below.m_60734_() instanceof StairBlock && below.m_61143_(StairBlock.f_56842_) == Half.BOTTOM;
   }

   private boolean passage(BlockPos p) {
      BlockState state = this.m_9236_().m_8055_(p);
      return state.m_60734_() instanceof FenceGateBlock || state.m_60734_() instanceof DoorBlock && state.m_204336_(BlockTags.f_13095_);
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
         double dx = (double)cell.m_123341_() + 0.5 - thing.f_82479_;
         double dz = (double)cell.m_123343_() + 0.5 - thing.f_82481_;
         double d = dx * dx + dz * dz;
         if (d < bestDistance && Math.abs((double)cell.m_123342_() - thing.f_82480_) < 3.5 && !this.passage(cell)) {
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
            double dx = (double)cell.m_123341_() + 0.5 - thing.f_82479_;
            double dz = (double)cell.m_123343_() + 0.5 - thing.f_82481_;
            double d = dx * dx + dz * dz;
            if (d < bestDistance && Math.abs((double)cell.m_123342_() - thing.f_82480_) < 3.5 && !this.passage(cell)) {
               best = cell;
               bestDistance = d;
            }
         }
      }

      return best != null ? best : this.nearest(cells, thing, reach);
   }

   @Nullable
   private BlockPos besideBlock(BlockPos thing) {
      return this.nearestFree(this.area, Vec3.m_82539_(thing), 2.5);
   }

   @Nullable
   private BlockPos serveSpot(BarPatron p) {
      BlockPos behind = this.nearestFree(this.area, p.m_20182_(), 3.5);
      if (behind != null) {
         return behind;
      } else {
         BlockPos spot = this.nearestFree(this.floor, p.m_20182_(), 3.0);
         return spot != null && !this.passage(spot) ? spot : null;
      }
   }

   // Cells other bar staff are standing on, plus cells that staff with priority are already walking to.
   private void refreshClaims() {
      long now = this.m_9236_().m_46467_();
      if (now != this.claimsAt) {
         this.claimsAt = now;
         this.claimed.clear();
         this.standing.clear();

         for (BarWorker w : this.colleagues()) {
            BlockPos at = w.m_20183_();
            Vec3 v = w.m_20184_();
            boolean settled = w.routeGoal == null || w.routeGoal.equals(at) || v.f_82479_ * v.f_82479_ + v.f_82481_ * v.f_82481_ < 1.0E-4;
            if (settled) {
               this.standing.add(at);
               this.claimed.add(at);
            }

            if (w.routeGoal != null && w.m_19879_() < this.m_19879_()) {
               this.claimed.add(w.routeGoal);
            }
         }
      }
   }

   // Gentle nudge so two workers never end up inside each other.
   private void keepApart() {
      for (BarWorker w : this.m_9236_().m_6443_(BarWorker.class, this.m_20191_().m_82400_(1.0), w -> w != this)) {
         double dx = this.m_20185_() - w.m_20185_();
         double dz = this.m_20189_() - w.m_20189_();
         double d = Math.sqrt(dx * dx + dz * dz);
         if (d < 0.8 && Math.abs(this.m_20186_() - w.m_20186_()) < 1.5) {
            if (d < 1.0E-4) {
               double a = (double)this.m_19879_() * 2.399;
               dx = Math.cos(a);
               dz = Math.sin(a);
               d = 1.0;
            }

            double push = (0.8 - d) * 0.1;
            this.m_20256_(this.m_20184_().m_82520_(dx / d * push, 0.0, dz / d * push));
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
            BlockPos spot = this.nearestFree(this.area, Vec3.m_82539_(this.areaHome), 4.0);
            this.myHome = spot != null && !this.claimed.contains(spot) ? spot : null;
         }

         return this.myHome != null ? this.myHome : this.areaHome;
      }
   }

   private boolean goTo(BlockPos goal, double speed) {
      Vec3 target = Vec3.m_82539_(goal);
      double dx = target.f_82479_ - this.m_20185_();
      double dz = target.f_82481_ - this.m_20189_();
      boolean level = Math.abs(target.f_82480_ - this.m_20186_()) < 1.2;
      if (level && dx * dx + dz * dz < 0.0625) {
         this.halt();
         return true;
      } else if (level && dx * dx + dz * dz < 1.69 && this.standing.contains(goal)) {
         // A colleague is standing right there; stop beside them instead of walking into them.
         this.halt();
         return true;
      } else {
         if (!goal.equals(this.routeGoal)) {
            this.routeGoal = goal;
            this.plan(goal);
            this.reroutes = 0;
         }

         if (this.m_20182_().m_82557_(this.lastPos) > 0.0025) {
            this.lastPos = this.m_20182_();
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

         this.doors();
         double aimY = next == null ? target.f_82480_ : this.height(next);
         Vec3 aim = next == null
            ? target
            : new Vec3((double)next.m_123341_() + 0.5, Double.isNaN(aimY) ? (double)next.m_123342_() : aimY, (double)next.m_123343_() + 0.5);
         this.m_21566_().m_6849_(aim.f_82479_, aim.f_82480_, aim.f_82481_, speed);
         return false;
      }
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
      BlockState state = this.m_9236_().m_8055_(p);
      if (state.m_60734_() instanceof DoorBlock door) {
         if ((Boolean)state.m_61143_(DoorBlock.f_52727_) == open) {
            return false;
         } else {
            door.m_153165_(this, this.m_9236_(), state, p, open);
            return true;
         }
      } else if (state.m_60734_() instanceof FenceGateBlock) {
         if ((Boolean)state.m_61143_(FenceGateBlock.f_53341_) == open) {
            return false;
         } else {
            this.m_9236_().m_7731_(p, (BlockState)state.m_61124_(FenceGateBlock.f_53341_, open), 10);
            this.m_9236_().m_5594_(null, p, open ? SoundEvents.f_11873_ : SoundEvents.f_11872_, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
         }
      } else {
         return false;
      }
   }

   private double horizontal(BlockPos cell) {
      double dx = (double)cell.m_123341_() + 0.5 - this.m_20185_();
      double dz = (double)cell.m_123343_() + 0.5 - this.m_20189_();
      return dx * dx + dz * dz;
   }

   private void halt() {
      this.route.clear();
      this.m_21573_().m_26573_();
      this.m_21566_().m_6849_(this.m_20185_(), this.m_20186_(), this.m_20189_(), 0.0);
      this.m_21564_(0.0F);
      this.m_21570_(0.0F);
   }

   private void plan(BlockPos goal) {
      this.route.clear();
      BlockPos start = this.floor.contains(this.m_20183_()) ? this.m_20183_() : this.nearest(this.floor, this.m_20182_(), 4.0);
      if (start != null && this.floor.contains(goal)) {
         Map<BlockPos, BlockPos> from = this.search(start, goal, true);
         if (!from.containsKey(goal)) {
            from = this.search(start, goal, false);
         }

         if (from.containsKey(goal)) {
            for (BlockPos c = goal; !c.equals(start); c = from.get(c)) {
               this.route.addFirst(c);
            }

            if (!start.equals(this.m_20183_())) {
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
         if (this.m_21205_().m_41619_() && this.stepTicks % 10 == 0 && !this.noOneWaiting(server, r)) {
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
                  this.idleWait = 120 + this.f_19796_.m_188503_(300);
               }
            }
         } else if (this.areaHome == null || this.goTo(this.homeSpot(), 0.9)) {
            if (this.stepTicks % 30 == 0) {
               List<BarPatron> waiting = r.waitingCustomers(server);
               Entity look = (Entity)(waiting.isEmpty() ? this.m_9236_().m_45930_(this, 8.0) : (Entity)waiting.get(0));
               if (look != null) {
                  this.m_21563_().m_24960_(look, 20.0F, 20.0F);
               } else {
                  Vec3 out = this.m_20182_().m_82549_(Vec3.m_82528_(r.customerSide().m_122436_()).m_82490_(3.0));
                  this.m_21563_().m_24946_(out.f_82479_, this.m_20188_(), out.f_82481_);
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
         if (!s.m_41619_() && GlassShelfEntity.glassware(s) && this.shelfWithRoom(s) != null) {
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
         if (this.m_9236_().m_7702_(p) instanceof GlassShelfEntity shelf && room(shelf, glass) > 0 && this.besideBlock(p) != null) {
            double d = p.m_123331_(this.m_20183_());
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
         if (g.m_41619_()) {
            room += glass.m_41741_();
         } else if (ItemStack.m_150942_(g, glass)) {
            room += g.m_41741_() - g.m_41613_();
         }
      }

      return room;
   }

   private void restock(CashRegisterEntity r) {
      if (++this.restockTicks <= 500 && this.m_9236_().m_7702_(this.restockShelf) instanceof GlassShelfEntity shelf) {
         ItemStack held = this.m_21205_();
         if (held.m_41619_()) {
            BlockPos home = this.homeSpot();
            if (home != null && !this.goTo(home, 1.0)) {
               if (this.reroutes > 4) {
                  this.finishRestock(r);
               }
            } else {
               this.m_21563_().m_24964_(Vec3.m_82512_(r.m_58899_()));
               ItemStack take = ItemStack.f_41583_;

               for (ItemStack s : r.dishes) {
                  int room = GlassShelfEntity.glassware(s) ? room(shelf, s) : 0;
                  if (!s.m_41619_() && room > 0) {
                     take = s.m_41620_(Math.min(16, room));
                     r.m_6596_();
                     break;
                  }
               }

               if (take.m_41619_()) {
                  this.finishRestock(r);
               } else {
                  this.m_6674_(InteractionHand.MAIN_HAND);
                  this.m_21008_(InteractionHand.MAIN_HAND, take);
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
               this.m_21563_().m_24964_(Vec3.m_82512_(this.restockShelf));
               if (this.restockTicks % 6 == 0) {
                  this.m_6674_(InteractionHand.MAIN_HAND);
                  shelf.insert(held, -1);
                  if (held.m_41619_()) {
                     this.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
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
      if (!this.m_21205_().m_41619_()) {
         r.returnDish(this.m_21205_());
         this.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
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
      return this.m_9236_().m_6443_(BarWorker.class, this.m_20191_().m_82400_(32.0), w -> w != this && w.register != null && w.register.equals(this.register));
   }

   @Nullable
   private BlockPos pickIdleSpot(CashRegisterEntity r) {
      if (this.areaHome == null) {
         return null;
      } else {
         List<BarWorker> others = this.colleagues();
         List<BlockPos> options = new ArrayList<>();

         for (BlockPos cell : this.area) {
            if (!this.passage(cell) && !this.claimed.contains(cell) && !(cell.m_123331_(this.areaHome) > 36.0) && !(cell.m_123331_(this.m_20183_()) < 2.25)) {
               boolean taken = false;

               for (BarWorker w : others) {
                  if (w.m_20238_(Vec3.m_82539_(cell)) < 9.0 || w.idleSpot() != null && w.idleSpot().m_123331_(cell) < 9.0) {
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
            return this.f_19796_.m_188503_(4) == 0 && !this.m_20183_().equals(this.areaHome) && !this.claimed.contains(this.areaHome)
               ? this.areaHome
               : options.get(this.f_19796_.m_188503_(options.size()));
         }
      }
   }

   private void chore(ServerLevel server, CashRegisterEntity r) {
      if (this.idleTicks == 1) {
         this.idleLength = 90 + this.f_19796_.m_188503_(110);
         BarPatron guest = this.nearbyGuest(server, r);
         this.idleDo = guest != null && this.f_19796_.m_188503_(5) < 3 ? 1 : (this.f_19796_.m_188499_() ? 0 : 2);
         if (this.idleDo == 1) {
            this.chatTarget = guest.m_20148_();
         }
      }

      switch (this.idleDo) {
         case 0:
            Vec3 bar = this.m_20182_().m_82549_(Vec3.m_82528_(r.customerSide().m_122436_()).m_82490_(1.5)).m_82520_(0.0, 0.6, 0.0);
            this.m_21563_().m_24946_(bar.f_82479_, bar.f_82480_, bar.f_82481_);
            if (this.idleTicks % 18 == 4) {
               this.m_6674_(InteractionHand.MAIN_HAND);
            }
            break;
         case 1:
            BarPatron guest = this.chatTarget != null && server.m_8791_(this.chatTarget) instanceof BarPatron p && p.m_6084_() ? p : null;
            if (guest == null) {
               this.idleDo = 2;
               return;
            }

            this.m_21563_().m_24960_(guest, 30.0F, 30.0F);
            if (this.idleTicks == 14) {
               this.talkTo(guest);
            }
            break;
         default:
            if (this.idleTicks % 45 == 5) {
               BarPatron any = this.nearbyGuest(server, r);
               Vec3 out = this.m_20182_()
                  .m_82549_(Vec3.m_82528_(r.customerSide().m_122436_()).m_82490_(3.0))
                  .m_82520_(this.f_19796_.m_188583_() * 2.0, 0.0, this.f_19796_.m_188583_() * 2.0);
               if (any != null && this.f_19796_.m_188499_()) {
                  this.m_21563_().m_24960_(any, 20.0F, 20.0F);
               } else {
                  this.m_21563_().m_24946_(out.f_82479_, this.m_20188_(), out.f_82481_);
               }
            }
      }
   }

   @Nullable
   private BarPatron nearbyGuest(ServerLevel server, CashRegisterEntity r) {
      BarPatron best = null;
      double bestDistance = 36.0;

      for (BarPatron p : server.m_45976_(BarPatron.class, this.m_20191_().m_82377_(6.0, 2.0, 6.0))) {
         if (p.m_6084_() && !p.talking() && !p.unserved() && p.state() == BarPatron.State.ENJOYING && r.m_58899_().equals(p.registerPos())) {
            double d = p.m_20280_(this);
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
      boolean empty = guest.m_21205_().m_41619_() && guest.sipsLeft() <= 0;
      if (empty) {
         this.say(this.pick("Can I get you another?", "Same again?", "Top you up, " + (guest.slim() ? "miss" : "sir") + "?", "Another round?"));
         if (this.f_19796_.m_188503_(3) != 0) {
            guest.sayLater(this.pick("Go on then!", "Why not.", "You read my mind."), 40);
            guest.endLinger(true);
         } else {
            guest.sayLater(this.pick("I'm good, thanks.", "Not just yet.", "Maybe in a bit."), 40);
         }
      } else if (guest.m_21205_().m_41720_() instanceof CigarItem) {
         this.say(this.pick("That smells good.", "Mind the holder, now.", "Nice smoke."));
         guest.sayLater(this.pick("Cheers, it's a good one.", "Only on a night out.", "Don't tell my doctor."), 40);
      } else {
         switch (this.f_19796_.m_188503_(4)) {
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

      guest.look(this.m_20182_().m_82520_(0.0, 1.5, 0.0), 40);
   }

   private boolean takeOrder(ServerLevel server, CashRegisterEntity r) {
      List<BarPatron> order = new ArrayList<>(r.waitingCustomers(server));
      Map<BarPatron, Double> cost = new HashMap<>();

      for (BarPatron p : order) {
         BlockPos spot = this.serveSpot(p);
         cost.put(p, spot == null ? Double.MAX_VALUE : (double)(this.area.contains(spot) ? 0 : 1000) + spot.m_123331_(this.m_20183_()));
      }

      order.sort(Comparator.comparingDouble(cost::get));

      for (BarPatron p : order) {
         if (!r.beingServed.contains(p.m_20148_())) {
            BarProducts.Product product = p.product();
            boolean beer = product.kind() == BarProducts.Kind.BEER;
            boolean shot = product.kind() == BarProducts.Kind.SPIRIT && p.wantsShot();
            if ((beer || shot) && cost.get(p) != Double.MAX_VALUE) {
               BlockPos source = beer ? this.tapFor(product.id()) : this.rackFor(product.id());
               if (source != null) {
                  Item needed = beer ? (Item)Drunkard.BEER_MUG.get() : (Item)Drunkard.SHOT_GLASS.get();
                  BlockPos shelf = this.shelfWith(needed);
                  if (shelf != null || dishesHave(r, needed)) {
                     r.beingServed.add(p.m_20148_());
                     this.customer = p.m_20148_();
                     this.pourFrom = source;
                     this.glassFrom = shelf;
                     this.next(BarWorker.Step.GLASS);
                     if (this.f_19796_.m_188503_(3) == 0) {
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
         BlockState state = this.m_9236_().m_8055_(t);
         if (state.m_60734_() instanceof TapTowerBlock && this.besideBlock(t) != null) {
            for (int tap = -1; tap <= 1; tap++) {
               KegEntity keg = TapTowerBlock.keg(this.m_9236_(), t, state, tap);
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
         BlockEntity var5 = this.m_9236_().m_7702_(p);
         if (var5 instanceof RackEntity) {
            RackEntity rack = (RackEntity)var5;
            if (this.besideBlock(p) != null) {
               for (ItemStack b : rack.bottles) {
                  if (!b.m_41619_() && WineData.variety(b).equals(spirit)) {
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
         BlockEntity d = this.m_9236_().m_7702_(p);
         if (d instanceof GlassShelfEntity) {
            GlassShelfEntity s = (GlassShelfEntity)d;
            if (s.count(glassItem) > 0 && this.besideBlock(p) != null) {
               double dx = p.m_123331_(this.m_20183_());
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
         if (s.m_150930_(item)) {
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
         ItemStack glass = ItemStack.f_41583_;
         if (this.glassFrom == null) {
            if (this.areaHome != null && !this.arrive(this.homeSpot(), 1.0)) {
               return;
            }

            for (ItemStack s : r.dishes) {
               if (s.m_150930_(needed)) {
                  glass = s.m_41620_(1);
                  r.m_6596_();
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

            this.m_21563_().m_24964_(Vec3.m_82512_(this.glassFrom));
            if (this.m_9236_().m_7702_(this.glassFrom) instanceof GlassShelfEntity shelf) {
               glass = shelf.take(needed);
            }
         }

         if (glass.m_41619_()) {
            this.drop(r);
         } else {
            this.m_6674_(InteractionHand.MAIN_HAND);
            this.m_21008_(InteractionHand.MAIN_HAND, glass);
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
            this.m_21563_().m_24964_(Vec3.m_82512_(this.pourFrom));
            if (this.stepTicks >= 6) {
               ItemStack drink = ItemStack.f_41583_;
               String id = p.product().id();
               BlockState state = this.m_9236_().m_8055_(this.pourFrom);
               if (state.m_60734_() instanceof TapTowerBlock) {
                  for (int tap = -1; tap <= 1 && drink.m_41619_(); tap++) {
                     KegEntity keg = TapTowerBlock.keg(this.m_9236_(), this.pourFrom, state, tap);
                     if (keg != null && keg.servings > 0 && keg.beer.equals(id)) {
                        drink = keg.draw();
                     }
                  }

                  if (!drink.m_41619_()) {
                     this.m_9236_().m_5594_(null, this.pourFrom, SoundEvents.f_11770_, SoundSource.BLOCKS, 0.8F, 0.7F);
                  }
               } else if (this.m_9236_().m_7702_(this.pourFrom) instanceof RackEntity rack) {
                  for (int i = 0; i < rack.bottles.size() && drink.m_41619_(); i++) {
                     ItemStack bottle = (ItemStack)rack.bottles.get(i);
                     if (!bottle.m_41619_() && WineData.variety(bottle).equals(id)) {
                        drink = ShotGlassItem.pour(bottle);
                        ItemStack rest = WineData.pourOne(bottle);
                        if (rest.m_150930_(Items.f_42590_)) {
                           rack.bottles.set(i, ItemStack.f_41583_);
                           r.returnDish(rest);
                        } else {
                           rack.bottles.set(i, rest);
                        }

                        rack.sync();
                     }
                  }

                  if (!drink.m_41619_()) {
                     this.m_9236_().m_5594_(null, this.pourFrom, SoundEvents.f_11769_, SoundSource.BLOCKS, 0.6F, 1.3F);
                  }
               }

               if (drink.m_41619_()) {
                  this.complain("Ran out!");
                  this.drop(r);
               } else {
                  this.m_6674_(InteractionHand.MAIN_HAND);
                  this.m_21008_(InteractionHand.MAIN_HAND, drink);
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

         this.m_21563_().m_24960_(p, 40.0F, 40.0F);
         this.m_5618_(this.rotTo(p));
         this.m_146922_(this.rotTo(p));
         if (++this.handTicks == 8) {
            ItemStack drink = this.m_21205_();
            if (p.serveFromWorker(drink)) {
               this.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
               this.m_6674_(InteractionHand.MAIN_HAND);
               this.say(
                  this.farTrip
                     ? this.pick(
                        "Here you go, enjoy your drink!", "One pint, as ordered.", "There you are. Anything else, just give me a shout.", "Fresh from the tap!"
                     )
                     : this.pick("Here you go!", "Enjoy!", "There you are.", "Cheers!")
               );
            }
         }

         if (this.farTrip && this.handTicks == 30 && this.f_19796_.m_188499_()) {
            this.say(this.pick("Give me a wave if you need anything.", "Holler if you want another.", "Enjoy your night!"));
         }

         if (this.handTicks >= (this.farTrip ? 45 : 16)) {
            this.drop(r);
         }
      }
   }

   private float rotTo(Entity e) {
      return (float)(Math.toDegrees(Math.atan2(e.m_20189_() - this.m_20189_(), e.m_20185_() - this.m_20185_())) - 90.0);
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
            && this.m_9236_() instanceof ServerLevel server
            && server.m_8791_(this.customer) instanceof BarPatron p
            && p.m_6084_()
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
      this.idleWait = 60 + this.f_19796_.m_188503_(160);
      if (!this.m_21205_().m_41619_()) {
         r.returnDish(this.m_21205_());
         this.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
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
      return lines[this.f_19796_.m_188503_(lines.length)];
   }

   protected InteractionResult m_6071_(Player player, InteractionHand hand) {
      if (!this.m_9236_().f_46443_) {
         this.m_21563_().m_24960_(player, 30.0F, 30.0F);
         this.say(
            this.step == BarWorker.Step.IDLE
               ? this.pick("Evening, boss.", "All quiet here.", "Keep the taps full and I'll keep them happy.", "Need anything, boss?")
               : this.pick("On it, boss!", "Busy busy!")
         );
      }

      return InteractionResult.m_19078_(this.m_9236_().f_46443_);
   }

   public boolean m_6469_(DamageSource source, float amount) {
      if (source.m_7639_() instanceof Player && !this.m_9236_().f_46443_) {
         this.say(this.pick("Hey! I work here!", "Ow! What was that for, boss?"));
      }

      return super.m_6469_(source, amount);
   }

   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      if (this.register != null && !this.goingHome) {
         tag.m_128356_("Register", this.register.m_121878_());
      }

      tag.m_128405_("Skin", this.skin());
   }

   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.register = tag.m_128441_("Register") ? BlockPos.m_122022_(tag.m_128454_("Register")) : null;
      this.f_19804_.m_135381_(SKIN, tag.m_128451_("Skin") | 1048576);
   }

   private static enum Step {
      IDLE,
      GLASS,
      POUR,
      SERVE;
   }

   private static final class WalkControl extends MoveControl {
      WalkControl(Mob mob) {
         super(mob);
      }

      public void m_8126_() {
         if (this.f_24981_ != Operation.MOVE_TO) {
            super.m_8126_();
         } else {
            this.f_24981_ = Operation.WAIT;
            double dx = this.f_24975_ - this.f_24974_.m_20185_();
            double dz = this.f_24977_ - this.f_24974_.m_20189_();
            if (dx * dx + dz * dz < 2.5E-7) {
               this.f_24974_.m_21564_(0.0F);
            } else {
               this.f_24974_.m_146922_(this.m_24991_(this.f_24974_.m_146908_(), (float)(Mth.m_14136_(dz, dx) * 180.0F / (float)Math.PI) - 90.0F, 90.0F));
               this.f_24974_.m_7910_((float)(this.f_24978_ * this.f_24974_.m_21133_(Attributes.f_22279_)));
            }
         }
      }
   }
}
