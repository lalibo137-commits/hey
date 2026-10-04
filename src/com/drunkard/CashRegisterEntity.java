package com.drunkard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CashRegisterEntity extends SyncedEntity implements MenuProvider {
   public static final int MAX_CAPACITY = 20;
   public static final int MAX_SEATS = 32;
   public static final int MAX_TILL = 999999;
   public static final int BUTTON_OPEN = 0;
   public static final int BUTTON_LESS = 1;
   public static final int BUTTON_MORE = 2;
   public static final int BUTTON_COLLECT = 3;
   public static final int BUTTON_DOOR = 4;
   public static final int BUTTON_SEATS = 5;
   public static final int BUTTON_CLEAR_SEATS = 6;
   public static final int BUTTON_ALL = 7;
   public static final int BUTTON_NONE = 8;
   public static final int BUTTON_STAFF = 9;
   public static final int BUTTON_STYLE = 10;
   public static final int BUTTON_STOCK = 100;
   public static final int DATA_OPEN = 0;
   public static final int DATA_CAPACITY = 1;
   public static final int DATA_TILL_LOW = 2;
   public static final int DATA_TILL_HIGH = 3;
   public static final int DATA_DOOR = 4;
   public static final int DATA_SEATS = 5;
   public static final int DATA_PATRONS = 6;
   public static final int DATA_REPUTATION = 7;
   public static final int DATA_SERVED = 8;
   public static final int DATA_TAKINGS_LOW = 9;
   public static final int DATA_TAKINGS_HIGH = 10;
   public static final int DATA_TIPS = 11;
   public static final int DATA_WALKOUTS = 12;
   public static final int DATA_MAX_CAPACITY = 13;
   public static final int DATA_STAFF = 14;
   public static final int DATA_STAFF_OFF = 15;
   public static final int DATA_WAGES = 16;
   public static final int DATA_SAFE_LOW = 17;
   public static final int DATA_SAFE_HIGH = 18;
   public static final int DATA_HAS_SAFE = 19;
   public static final int DATA_STOCK = 20;
   public static final int DATA_STYLE = 20 + BarProducts.ALL.size();
   public static final int DATA_COUNT = DATA_STYLE + 1;
   public static final String[] STYLES = new String[]{"Modern", "Medieval"};
   public static final int CAPACITY_LIMIT = 26;
   public static final int MAX_REGULARS = 16;
   public boolean open;
   public boolean staffOff;
   public int style;
   private boolean callIn;
   public int capacity = 8;
   public int till;
   public final BitSet stock = new BitSet();
   public BlockPos door;
   public final List<BlockPos> seats = new ArrayList<>();
   public final NonNullList<ItemStack> dishes = NonNullList.m_122780_(18, ItemStack.f_41583_);
   private final Map<UUID, Long> patrons = new LinkedHashMap<>();
   private final Map<BlockPos, UUID> claims = new HashMap<>();
   private UUID head;
   private int spawnCooldown = 60;
   private int fightCooldown = 2400;
   public int reputation = 50;
   public int servedToday;
   public int takingsToday;
   public int tipsToday;
   public int walkoutsToday;
   private long ledgerDay = -1L;
   public final List<CashRegisterEntity.Regular> regulars = new ArrayList<>();
   private final LinkedHashSet<Long> friendships = new LinkedHashSet<>();
   private int messCooldown = 1200;
   public final ContainerData data = new ContainerData() {
      public int m_6413_(int i) {
         return switch (i) {
            case 0 -> CashRegisterEntity.this.open ? 1 : 0;
            case 1 -> CashRegisterEntity.this.capacity;
            case 2 -> CashRegisterEntity.this.till & 32767;
            case 3 -> CashRegisterEntity.this.till >> 15;
            case 4 -> CashRegisterEntity.this.door == null ? 0 : 1;
            case 5 -> CashRegisterEntity.this.seats.size();
            case 6 -> CashRegisterEntity.this.patrons.size();
            case 7 -> CashRegisterEntity.this.reputation;
            case 8 -> CashRegisterEntity.this.servedToday;
            case 9 -> CashRegisterEntity.this.takingsToday & 32767;
            case 10 -> CashRegisterEntity.this.takingsToday >> 15;
            case 11 -> Math.min(32767, CashRegisterEntity.this.tipsToday);
            case 12 -> CashRegisterEntity.this.walkoutsToday;
            case 13 -> CashRegisterEntity.this.maxCapacity();
            case 14 -> CashRegisterEntity.this.level(BarUpgrades.Upgrade.BARKEEP);
            case 15 -> CashRegisterEntity.this.staffOff ? 1 : 0;
            case 16 -> Math.min(32767, CashRegisterEntity.this.wages());
            case 17 -> CashRegisterEntity.this.safeBalance() & 32767;
            case 18 -> CashRegisterEntity.this.safeBalance() >> 15;
            case 19 -> CashRegisterEntity.this.safeBalance() >= 0 && !CashRegisterEntity.this.cachedSafes.isEmpty() ? 1 : 0;
            default -> i == CashRegisterEntity.DATA_STYLE
               ? CashRegisterEntity.this.style
               : (i >= 20 && i < CashRegisterEntity.DATA_STYLE && CashRegisterEntity.this.stock.get(i - 20) ? 1 : 0);
         };
      }

      public void m_8050_(int i, int value) {
      }

      public int m_6499_() {
         return CashRegisterEntity.DATA_COUNT;
      }
   };
   private Direction customerSide;
   private long sideCheckedAt = Long.MIN_VALUE;
   private int counterFrom = -8;
   private int counterTo = 8;
   private long counterMeasuredAt = Long.MIN_VALUE;
   public final int[] upgrades = new int[BarUpgrades.Upgrade.values().length];
   private List<SafeEntity> cachedSafes = List.of();
   private long safesCheckedAt = -1000L;
   public final Set<UUID> beingServed = new HashSet<>();
   private int eventCooldown = 600;
   private int robberCooldown = 9000;

   private static long pair(int a, int b) {
      return (long)Math.min(a, b) << 32 | (long)Math.max(a, b) & 4294967295L;
   }

   public boolean friends(int a, int b) {
      return a != b && this.friendships.contains(Long.valueOf(pair(a, b)));
   }

   public void befriend(int a, int b) {
      if (a != b && this.friendships.add(Long.valueOf(pair(a, b)))) {
         while (this.friendships.size() > 400) {
            Iterator<Long> it = this.friendships.iterator();
            it.next();
            it.remove();
         }

         this.m_6596_();
      }
   }

   public CashRegisterEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType<?>)Drunkard.CASH_REGISTER_ENTITY.get(), pos, state);
   }

   public boolean medieval() {
      return this.style == 1;
   }

   private int styleBit() {
      return this.medieval() ? Medieval.SKIN_BIT : 0;
   }

   public Direction facing() {
      return (Direction)this.m_58900_().m_61143_(CashRegisterBlock.FACING);
   }

   public boolean ready() {
      return this.open && this.door != null && !this.stock.isEmpty() && this.f_58857_ != null && this.doorSides() != null;
   }

   public int patronCount() {
      return this.patrons.size();
   }

   public List<BarProducts.Product> stocked() {
      List<BarProducts.Product> list = new ArrayList<>();

      for (int i = this.stock.nextSetBit(0); i >= 0; i = this.stock.nextSetBit(i + 1)) {
         if (BarProducts.get(i) != null) {
            list.add(BarProducts.get(i));
         }
      }

      return list;
   }

   public void track(BarPatron patron) {
      this.patrons.put(patron.m_20148_(), this.f_58857_ == null ? 0L : this.f_58857_.m_46467_());
   }

   public void forget(BarPatron patron) {
      this.patrons.remove(patron.m_20148_());
      this.releaseSeat(patron);
   }

   public boolean isHead(BarPatron patron) {
      return patron.m_20148_().equals(this.head);
   }

   public BlockPos[] doorSides() {
      if (this.door != null && this.f_58857_ != null) {
         BlockState state = this.f_58857_.m_8055_(this.door);
         if (!(state.m_60734_() instanceof DoorBlock)) {
            return null;
         } else {
            BlockPos lower = state.m_61143_(DoorBlock.f_52730_) == DoubleBlockHalf.UPPER ? this.door.m_7495_() : this.door;
            Direction facing = (Direction)state.m_61143_(DoorBlock.f_52726_);
            BlockPos a = lower.m_121945_(facing);
            BlockPos b = lower.m_121945_(facing.m_122424_());
            return a.m_123331_(this.f_58858_) > b.m_123331_(this.f_58858_) ? new BlockPos[]{a, b} : new BlockPos[]{b, a};
         }
      } else {
         return null;
      }
   }

   public Direction customerSide() {
      long now = this.f_58857_ == null ? 0L : this.f_58857_.m_46467_();
      if (this.customerSide != null && now - this.sideCheckedAt < 100L) {
         return this.customerSide;
      } else {
         this.sideCheckedAt = now;
         Direction a = this.facing().m_122424_();
         Direction b = this.facing();
         BlockPos fa = this.frontOn(a);
         BlockPos fb = this.frontOn(b);
         BlockPos[] sides = this.doorSides();
         if (fa == null && fb != null) {
            this.customerSide = b;
         } else if (fa != null && fb != null && sides != null) {
            this.customerSide = fa.m_123331_(sides[1]) <= fb.m_123331_(sides[1]) ? a : b;
         } else {
            this.customerSide = a;
         }

         return this.customerSide;
      }
   }

   private BlockPos frontOn(Direction side) {
      BlockPos base = this.f_58858_.m_121945_(side);

      for (int dy : new int[]{0, -1, -2, 1}) {
         if (this.standable(base.m_6630_(dy))) {
            return base.m_6630_(dy);
         }
      }

      return null;
   }

   public BlockPos front() {
      BlockPos spot = this.frontOn(this.customerSide());
      return spot != null ? spot : this.f_58858_.m_121945_(this.customerSide());
   }

   public boolean behindCounter(BlockPos p) {
      Direction bar = this.customerSide().m_122424_();
      Direction along = bar.m_122427_();
      int dx = p.m_123341_() - this.f_58858_.m_123341_();
      int dy = p.m_123342_() - this.f_58858_.m_123342_();
      int dz = p.m_123343_() - this.f_58858_.m_123343_();
      int depth = dx * bar.m_122429_() + dz * bar.m_122431_();
      int side = dx * along.m_122429_() + dz * along.m_122431_();
      if (this.counterMeasuredAt != this.sideCheckedAt) {
         this.measureCounter(along);
      }

      return depth >= 1 && depth <= 4 && side >= this.counterFrom && side <= this.counterTo && dy >= -3 && dy <= 2;
   }

   public boolean onCounter(BlockPos p) {
      Direction bar = this.customerSide().m_122424_();
      Direction along = bar.m_122427_();
      int dx = p.m_123341_() - this.f_58858_.m_123341_();
      int dz = p.m_123343_() - this.f_58858_.m_123343_();
      if (this.counterMeasuredAt != this.sideCheckedAt) {
         this.measureCounter(along);
      }

      int depth = dx * bar.m_122429_() + dz * bar.m_122431_();
      int side = dx * along.m_122429_() + dz * along.m_122431_();
      return depth == 0 && side >= this.counterFrom && side <= this.counterTo && p.m_123342_() >= this.f_58858_.m_123342_();
   }

   private void measureCounter(Direction along) {
      this.counterMeasuredAt = this.sideCheckedAt;
      if (this.f_58857_ != null) {
         int hi = 0;
         int lo = 0;

         while (hi < 16 && this.counter(this.f_58858_.m_7495_().m_5484_(along, hi + 1))) {
            hi++;
         }

         while (lo > -16 && this.counter(this.f_58858_.m_7495_().m_5484_(along, lo - 1))) {
            lo--;
         }

         this.counterFrom = lo;
         this.counterTo = hi;
      }
   }

   private boolean counter(BlockPos p) {
      BlockState state = this.f_58857_.m_8055_(p);
      VoxelShape shape = state.m_60812_(this.f_58857_, p);
      return !shape.m_83281_() && shape.m_83297_(Axis.Y) >= 0.5 && !(state.m_60734_() instanceof SeatBlock) && !this.seats.contains(p);
   }

   public BlockPos claimStandingSpot(BarPatron patron, BlockPos near, RandomSource random) {
      return this.claimStandingSpot(patron, near, random, 0.0);
   }

   public BlockPos claimStandingSpot(BarPatron patron, BlockPos near, RandomSource random, double away) {
      for (int i = 0; i < 24; i++) {
         BlockPos p = near.m_7918_(random.m_188503_(7) - 3, 0, random.m_188503_(7) - 3);

         for (int dy : new int[]{0, -1, 1}) {
            BlockPos q = p.m_6630_(dy);
            if (this.standable(q) && !this.behindCounter(q) && !q.equals(this.front()) && (!(away > 0.0) || !(q.m_123331_(this.front()) < away * away))) {
               UUID owner = this.claims.get(q);
               if (owner == null || owner.equals(patron.m_20148_()) || !this.patrons.containsKey(owner)) {
                  this.claims.put(q.m_7949_(), patron.m_20148_());
                  return q.m_7949_();
               }
            }
         }
      }

      return null;
   }

   public boolean standable(BlockPos p) {
      return this.f_58857_ != null && !Double.isNaN(BarWorker.floorAt(this.f_58857_, p));
   }

   public void releaseSpot(BarPatron patron, BlockPos spot) {
      if (spot != null && patron.m_20148_().equals(this.claims.get(spot))) {
         this.claims.remove(spot);
      }
   }

   public BlockPos claimSeat(BarPatron patron, BlockPos near, double range) {
      return this.claimSeat(patron, near, range, Set.of());
   }

   public BlockPos claimSeat(BarPatron patron, BlockPos near, double range, Set<BlockPos> avoid) {
      if (this.f_58857_ == null) {
         return null;
      } else {
         this.releaseSeat(patron);
         BlockPos best = null;
         double bestDistance = range * range;

         for (BlockPos seat : this.seats) {
            UUID owner = this.claims.get(seat);
            if ((owner == null || owner.equals(patron.m_20148_()) || !this.patrons.containsKey(owner))
               && !avoid.contains(seat)
               && SeatEntity.seat(this.f_58857_.m_8055_(seat))
               && !SeatEntity.occupied(this.f_58857_, seat)) {
               double d = seat.m_123331_(near);
               if (d <= bestDistance) {
                  best = seat;
                  bestDistance = d;
               }
            }
         }

         if (best != null) {
            this.claims.put(best, patron.m_20148_());
         }

         return best;
      }
   }

   public void keepSeat(BarPatron patron, BlockPos seat) {
      if (seat != null) {
         this.claims.put(seat, patron.m_20148_());
      }
   }

   public void releaseSeat(BarPatron patron) {
      this.claims.values().removeIf(id -> id.equals(patron.m_20148_()));
   }

   public void pay(int dollars) {
      this.till = Math.min(999999, this.till + Math.max(0, dollars));
      this.m_6596_();
   }

   public int level(BarUpgrades.Upgrade u) {
      return this.upgrades[u.ordinal()];
   }

   public int maxCapacity() {
      return 20 + 3 * this.level(BarUpgrades.Upgrade.PREMISES);
   }

   public int price(BarProducts.Product p, boolean shot) {
      return p.price(shot);
   }

   public void refund(int dollars) {
      this.till = Math.max(0, this.till - dollars);
      this.takingsToday = Math.max(0, this.takingsToday - dollars);
      this.m_6596_();
   }

   public void collectEmpties() {
      if (this.f_58857_ != null) {
         for (BlockPos p : BlockPos.m_121940_(this.f_58858_.m_7918_(-16, -3, -16), this.f_58858_.m_7918_(16, 1, 16))) {
            BlockState state = this.f_58857_.m_8055_(p);
            if (state.m_60713_((Block)Drunkard.PLACED_BOTTLE.get()) && this.f_58857_.m_7702_(p) instanceof BottleEntity b && empty(b.bottle)) {
               this.returnDish(b.bottle.m_41777_());
               b.bottle = ItemStack.f_41583_;
               this.f_58857_.m_7471_(p, false);
            } else if (state.m_60713_((Block)Drunkard.PLACED_SHOT_GLASS.get())
               && this.f_58857_.m_7702_(p) instanceof ShotGlassEntity g
               && !ShotGlassItem.filled(g.glass)) {
               this.returnDish(new ItemStack((ItemLike)Drunkard.SHOT_GLASS.get()));
               g.glass = ItemStack.f_41583_;
               this.f_58857_.m_7471_(p, false);
            }
         }
      }
   }

   private static boolean empty(ItemStack s) {
      return s.m_150930_(Items.f_42590_)
         || s.m_150930_((Item)Drunkard.BEER_MUG.get())
         || s.m_150930_((Item)Drunkard.ROCKS_GLASS.get())
         || s.m_150930_((Item)Drunkard.HIGHBALL_GLASS.get())
         || s.m_150930_((Item)Drunkard.MARTINI_GLASS.get())
         || s.m_150930_((Item)Drunkard.MARGARITA_GLASS.get());
   }

   private void rollDay() {
      if (this.f_58857_ != null) {
         long day = this.f_58857_.m_46468_() / 24000L;
         if (day != this.ledgerDay) {
            boolean newDay = this.ledgerDay >= 0L;
            this.ledgerDay = day;
            this.servedToday = this.takingsToday = this.tipsToday = this.walkoutsToday = 0;
            this.m_6596_();
            if (newDay) {
               this.payWages();
            }
         }
      }
   }

   public int wages() {
      int total = 0;

      for (BarUpgrades.Upgrade u : BarUpgrades.Upgrade.values()) {
         if (u != BarUpgrades.Upgrade.BARKEEP || !this.staffOff) {
            total += u.wage * this.level(u);
         }
      }

      return total;
   }

   public List<SafeEntity> safes() {
      List<SafeEntity> list = new ArrayList<>();
      if (this.f_58857_ == null) {
         return list;
      } else {
         int range = 16;

         for (BlockPos q : BlockPos.m_121940_(this.f_58858_.m_7918_(-range, -4, -range), this.f_58858_.m_7918_(range, 4, range))) {
            if (this.f_58857_.m_46749_(q)) {
               BlockEntity var6 = this.f_58857_.m_7702_(q);
               if (var6 instanceof SafeEntity) {
                  SafeEntity s = (SafeEntity)var6;
                  if (s.register() == this) {
                     list.add(s);
                  }
               }
            }
         }

         return list;
      }
   }

   public int safeBalance() {
      if (this.f_58857_ == null) {
         return 0;
      } else {
         if (this.f_58857_.m_46467_() - this.safesCheckedAt > 60L) {
            this.safesCheckedAt = this.f_58857_.m_46467_();
            this.cachedSafes = this.safes();
         }

         long sum = 0L;

         for (SafeEntity s : this.cachedSafes) {
            if (!s.m_58901_()) {
               sum += (long)s.balance;
            }
         }

         return (int)Math.min(1073741823L, sum);
      }
   }

   public void payWages() {
      int total = this.wages();
      if (total != 0 && this.f_58857_ != null) {
         List<SafeEntity> safes = this.safes();
         long inSafes = 0L;

         for (SafeEntity s : safes) {
            inSafes += (long)s.balance;
         }

         String message;
         if (inSafes + (long)this.till >= (long)total) {
            int fromSafe = 0;

            for (SafeEntity s : safes) {
               int take = Math.min(s.balance, total - fromSafe);
               if (take > 0) {
                  s.balance -= take;
                  fromSafe += take;
                  s.m_6596_();
                  s.sync();
               }
            }

            this.till -= total - fromSafe;
            message = "Paid your staff $"
               + total
               + " in wages"
               + (
                  fromSafe == total
                     ? " from the safe"
                     : (fromSafe > 0 ? " ($" + fromSafe + " from the safe, $" + (total - fromSafe) + " from the till)" : " from the till")
               );
         } else {
            for (BarUpgrades.Upgrade u : BarUpgrades.Upgrade.values()) {
               if (u.staff()) {
                  this.upgrades[u.ordinal()] = 0;
               }
            }

            this.dismissStaff();
            message = "Not enough in the safe or the till for wages ($" + total + "): your staff quit";
         }

         for (Player p : this.f_58857_.m_45976_(Player.class, new AABB(this.f_58858_).m_82400_(48.0))) {
            p.m_5661_(Component.m_237113_(message), false);
         }

         this.sync();
      }
   }

   public void keepStaff(ServerLevel server) {
      this.rollDay();
      List<BarWorker> staff = this.staff(server);
      int wanted = this.staffOff ? 0 : this.level(BarUpgrades.Upgrade.BARKEEP);

      for (int i = staff.size(); i < wanted; i++) {
         BlockPos home = this.workerHome();
         BarWorker w = (BarWorker)((EntityType)Drunkard.BAR_WORKER.get()).m_20615_(server);
         if (w == null || home == null) {
            return;
         }

         BlockPos[] sides = this.doorSides();
         BlockPos at = this.callIn && sides != null && this.standable(sides[1]) ? sides[1] : home;
         double y = BarWorker.floorAt(this.f_58857_, at);
         w.m_7678_(
            (double)at.m_123341_() + 0.5, Double.isNaN(y) ? (double)at.m_123342_() : y, (double)at.m_123343_() + 0.5, this.customerSide().m_122435_(), 0.0F
         );
         int look = server.f_46441_.m_188503_(65536);
         int tries = 0;

         while (true) {
            if (tries < 30) {
               boolean clash = false;

               for (BarWorker other : staff) {
                  if ((other.skin() & 65535) == look || ((other.skin() ^ look) & 1) == 0) {
                     clash = true;
                  }
               }

               if (clash) {
                  look = server.f_46441_.m_188503_(65536);
                  tries++;
                  continue;
               }
            }

            w.hire(this.f_58858_, look | this.styleBit());
            staff.add(w);
            server.m_7967_(w);
            if (at != home) {
               w.say(server.f_46441_.m_188499_() ? "Evening, boss!" : "Reporting for duty!");
            }
            break;
         }
      }

      this.callIn = false;

      for (int i = wanted; i < staff.size(); i++) {
         staff.get(i).goHome(this.staffOff ? (server.f_46441_.m_188499_() ? "Night, boss. See you tomorrow." : "Off home, then. Night!") : null);
      }
   }

   public List<BarWorker> staff(ServerLevel server) {
      return server.m_6443_(BarWorker.class, new AABB(this.f_58858_).m_82400_(48.0), w -> this.f_58858_.equals(w.registerPos()));
   }

   private void dismissStaff() {
      if (this.f_58857_ instanceof ServerLevel server) {
         for (BarWorker w : this.staff(server)) {
            w.say("No pay, no work. I'm off.");
            w.quit();
         }
      }
   }

   @Nullable
   public BlockPos workerHome() {
      BlockPos base = this.f_58858_.m_121945_(this.customerSide().m_122424_());

      for (int dy : new int[]{-1, -2, -3, 0, 1}) {
         if (this.standable(base.m_6630_(dy))) {
            return base.m_6630_(dy);
         }
      }

      for (int r = 1; r <= 3; r++) {
         for (BlockPos q : BlockPos.m_121940_(base.m_7918_(-r, -3, -r), base.m_7918_(r, 1, r))) {
            if (this.standable(q) && this.behindCounter(q)) {
               return q.m_7949_();
            }
         }
      }

      return null;
   }

   public List<BarPatron> waitingCustomers(ServerLevel server) {
      List<BarPatron> list = new ArrayList<>();

      for (UUID id : this.patrons.keySet()) {
         Entity var6 = server.m_8791_(id);
         if (var6 instanceof BarPatron) {
            BarPatron p = (BarPatron)var6;
            if (p.unserved() && p.product() != null) {
               list.add(p);
            }
         }
      }

      list.sort(Comparator.comparingLong(BarPatron::waitingSince));
      return list;
   }

   public boolean tallBar() {
      return this.f_58858_.m_123342_() - this.front().m_123342_() >= 2;
   }

   private void reputation(int change) {
      this.reputation = Math.max(0, Math.min(100, this.reputation + change));
      this.m_6596_();
   }

   public void served(int price, int waited) {
      this.rollDay();
      this.servedToday++;
      this.takingsToday = Math.min(999999, this.takingsToday + price);
      this.reputation(waited < 400 ? 2 : (waited < 1600 ? 1 : 0));
   }

   public void tipped(int dollars) {
      this.rollDay();
      this.tipsToday += dollars;
      this.m_6596_();
   }

   public void walkedOut() {
      this.rollDay();
      this.walkoutsToday++;
      this.reputation(-4);
   }

   public void hurtReputation(int amount) {
      this.reputation(-amount);
   }

   public void remember(BarPatron patron) {
      for (int i = 0; i < this.regulars.size(); i++) {
         if ((this.regulars.get(i).skin() & ~Medieval.SKIN_BIT) == (patron.skin() & ~Medieval.SKIN_BIT)) {
            CashRegisterEntity.Regular r = this.regulars.get(i);
            this.regulars
               .set(
                  i,
                  new CashRegisterEntity.Regular(
                     r.name(), patron.skin(), patron.order() >= 0 && patron.m_217043_().m_188499_() ? patron.order() : r.favourite(), r.visits() + 1
                  )
               );
            this.sync();
            return;
         }
      }

      if (!patron.regular() && (patron.visits() >= 2 || patron.m_217043_().m_188501_() < 0.35F + (float)this.reputation / 300.0F)) {
         if (this.regulars.size() >= 16) {
            this.regulars.remove(this.regulars.stream().min(Comparator.comparingInt(CashRegisterEntity.Regular::visits)).orElseThrow());
         }

         this.regulars.add(new CashRegisterEntity.Regular(patron.name(), patron.skin(), Math.max(0, patron.order()), 1));
         this.sync();
      }
   }

   private CashRegisterEntity.Regular returning(ServerLevel server) {
      if (!this.regulars.isEmpty() && !(server.f_46441_.m_188501_() > 0.15F + (float)this.reputation * 0.004F)) {
         Set<String> inside = new HashSet<>();

         for (UUID id : this.patrons.keySet()) {
            if (server.m_8791_(id) instanceof BarPatron p) {
               inside.add((p.skin() & ~Medieval.SKIN_BIT) + "");
            }
         }

         List<CashRegisterEntity.Regular> free = new ArrayList<>();

         for (CashRegisterEntity.Regular r : this.regulars) {
            if (!inside.contains((r.skin() & ~Medieval.SKIN_BIT) + "")) {
               free.add(r);
            }
         }

         return free.isEmpty() ? null : free.get(server.f_46441_.m_188503_(free.size()));
      } else {
         return null;
      }
   }

   public int empties() {
      if (this.f_58857_ == null) {
         return 0;
      } else {
         int n = 0;

         for (BlockPos p : BlockPos.m_121940_(this.f_58858_.m_7918_(-16, -3, -16), this.f_58858_.m_7918_(16, 1, 16))) {
            Block b = this.f_58857_.m_8055_(p).m_60734_();
            if (b == Drunkard.PLACED_BOTTLE.get()
               || b == Drunkard.PLACED_SHOT_GLASS.get() && this.f_58857_.m_7702_(p) instanceof ShotGlassEntity g && !ShotGlassItem.filled(g.glass)) {
               n++;
            }
         }

         return n;
      }
   }

   public void returnDish(ItemStack dish) {
      if (!dish.m_41619_()) {
         for (int i = 0; i < this.dishes.size() && !dish.m_41619_(); i++) {
            ItemStack slot = (ItemStack)this.dishes.get(i);
            if (slot.m_41619_()) {
               this.dishes.set(i, dish.m_41777_());
               dish.m_41764_(0);
            } else if (ItemStack.m_150942_(slot, dish) && slot.m_41613_() < slot.m_41741_()) {
               int move = Math.min(dish.m_41613_(), slot.m_41741_() - slot.m_41613_());
               slot.m_41769_(move);
               dish.m_41774_(move);
            }
         }

         this.m_6596_();
      }
   }

   public static void tick(Level level, BlockPos pos, BlockState state, CashRegisterEntity register) {
      if (level instanceof ServerLevel server) {
         long now = level.m_46467_();
         if ((Boolean)state.m_61143_(CashRegisterBlock.OPEN) != register.open) {
            level.m_7731_(pos, (BlockState)state.m_61124_(CashRegisterBlock.OPEN, register.open), 3);
         }

         if (now % 10L == 0L) {
            register.patrons.values().removeIf(seen -> now - seen > 40L);
            register.claims.values().removeIf(idx -> !register.patrons.containsKey(idx));
            register.head = null;
            long earliest = Long.MAX_VALUE;

            for (UUID id : register.patrons.keySet()) {
               Entity var12 = server.m_8791_(id);
               if (var12 instanceof BarPatron) {
                  BarPatron p = (BarPatron)var12;
                  if (p.queuing() && p.waitingSince() < earliest) {
                     earliest = p.waitingSince();
                     register.head = id;
                  }
               }
            }
         }

         if (register.ready() && register.patrons.size() < register.capacity && --register.spawnCooldown <= 0) {
            long time = level.m_46468_() % 24000L;
            float evening = time >= 12000L && time < 23000L ? 0.5F : (time >= 9000L ? 0.8F : 1.2F);
            register.spawnCooldown = (int)(
               (float)(600 + level.f_46441_.m_188503_(1800))
                  * evening
                  * (1.4F - (float)register.reputation / 125.0F)
                  * (1.0F - 0.1F * (float)register.level(BarUpgrades.Upgrade.ADVERTISING))
            );
            register.trySpawn(server);
         }

         if (now % 200L == 100L) {
            register.keepStaff(server);
         }

         if (register.robberCooldown > 0) {
            register.robberCooldown--;
         } else if (now % 200L == 150L) {
            register.maybeRobbery(server);
         }

         if (--register.messCooldown <= 0) {
            register.messCooldown = 1200 + level.f_46441_.m_188503_(1200);
            register.checkMess(server);
         }

         if (register.fightCooldown > 0) {
            register.fightCooldown--;
         } else if (now % 200L == 0L) {
            register.maybeFight(server);
         }

         if (register.eventCooldown > 0) {
            register.eventCooldown--;
         } else if (now % 100L == 50L) {
            register.maybeEvent(server);
         }
      }
   }

   void maybeRobbery(ServerLevel server) {
      if (this.ready() && this.open) {
         int calm = 0;

         for (UUID id : this.patrons.keySet()) {
            if (server.m_8791_(id) instanceof BarPatron p) {
               if (p.robber()) {
                  return;
               }

               if (p.state() == BarPatron.State.ENJOYING || p.state() == BarPatron.State.TO_SEAT || p.state() == BarPatron.State.WAITING) {
                  calm++;
               }

               if (p.state() == BarPatron.State.FIGHTING) {
                  return;
               }
            }
         }

         if (calm >= 3 && server.f_46441_.m_188503_(3) == 0) {
            this.startRobbery(server);
         }
      }
   }

   public BarPatron startRobbery(ServerLevel server) {
      BarPatron robber = this.spawnCustomer(server);
      if (robber != null) {
         robber.becomeRobber();
      }

      this.robberCooldown = 14000 + server.f_46441_.m_188503_(14000);
      return robber;
   }

   void maybeEvent(ServerLevel server) {
      List<BarPatron> ready = new ArrayList<>();

      for (UUID id : this.patrons.keySet()) {
         Entity var6 = server.m_8791_(id);
         if (var6 instanceof BarPatron) {
            BarPatron p = (BarPatron)var6;
            if (p.state() == BarPatron.State.ENJOYING && !p.talking() && p.canChat()) {
               ready.add(p);
            }
         }
      }

      if (!ready.isEmpty() && server.f_46441_.m_188503_(2) == 0) {
         if (ready.get(server.f_46441_.m_188503_(ready.size())).startEvent(null)) {
            this.eventCooldown = 700 + server.f_46441_.m_188503_(1500);
         }
      }
   }

   public boolean free(BlockPos pos, BarPatron patron) {
      UUID owner = this.claims.get(pos);
      return owner == null || owner.equals(patron.m_20148_()) || !this.patrons.containsKey(owner);
   }

   public BarPatron trySpawn(ServerLevel server) {
      if (this.ready() && this.patrons.size() < this.capacity) {
         int room = this.capacity - this.patrons.size();
         if (!this.seats.isEmpty()) {
            room = Math.min(room, this.freeSeats());
         }

         if (room <= 0) {
            return null;
         } else {
            int roll = server.f_46441_.m_188503_(100);
            int size = roll < 30 ? 1 : (roll < 65 ? 2 : (roll < 85 ? 3 : 4));
            List<BarPatron> group = this.spawnGroup(server, Math.min(size, room));
            return group.isEmpty() ? null : group.get(0);
         }
      } else {
         return null;
      }
   }

   public int freeSeats() {
      if (this.f_58857_ == null) {
         return 0;
      } else {
         int usable = 0;

         for (BlockPos seat : this.seats) {
            if (this.f_58857_.m_46749_(seat) && SeatEntity.seat(this.f_58857_.m_8055_(seat))) {
               usable++;
            }
         }

         return usable - this.patrons.size();
      }
   }

   public List<BarPatron> spawnGroup(ServerLevel server, int size) {
      List<BarPatron> group = new ArrayList<>();
      UUID id = UUID.randomUUID();

      for (int i = 0; i < size; i++) {
         BarPatron p = this.spawnCustomer(server, id, i * 20);
         if (p != null) {
            group.add(p);
         }
      }

      return group;
   }

   public BarPatron spawnCustomer(ServerLevel server) {
      return this.spawnCustomer(server, UUID.randomUUID(), 0);
   }

   public BarPatron spawnCustomer(ServerLevel server, UUID group, int delay) {
      BlockPos[] sides = this.doorSides();
      if (sides == null) {
         return null;
      } else {
         BarPatron patron = (BarPatron)((EntityType)Drunkard.BAR_PATRON.get()).m_20615_(server);
         if (patron == null) {
            return null;
         } else {
            BlockPos outside = sides[0];
            BlockPos inside = sides[1];
            Direction out = Direction.m_122372_((float)(outside.m_123341_() - inside.m_123341_()), 0.0F, (float)(outside.m_123343_() - inside.m_123343_()));
            BlockPos far = outside;

            for (int i = 3; i >= 1; i--) {
               if (this.standable(outside.m_5484_(out, i))) {
                  far = outside.m_5484_(out, i);
                  break;
               }
            }

            patron.m_7678_((double)far.m_123341_() + 0.5, (double)far.m_123342_(), (double)far.m_123343_() + 0.5, out.m_122424_().m_122435_(), 0.0F);
            patron.m_6518_(server, server.m_6436_(outside), MobSpawnType.EVENT, null, null);
            CashRegisterEntity.Regular regular = this.returning(server);
            if (regular != null) {
               patron.becomeRegular(regular);
            }

            patron.setMedieval(this.medieval());
            patron.arrive(this.f_58858_, this.door, inside, outside, far, group, delay);
            server.m_7967_(patron);
            this.track(patron);
            return patron;
         }
      }
   }

   public List<BlockPos> counterSpots() {
      List<BlockPos> spots = new ArrayList<>();
      if (this.f_58857_ == null) {
         return spots;
      } else {
         Direction customer = this.customerSide();
         Direction along = customer.m_122427_();
         BlockPos base = this.f_58858_.m_7495_();

         for (int k = 0; k <= 6; k++) {
            for (int sign : k == 0 ? new int[]{1} : new int[]{1, -1}) {
               BlockPos counter = base.m_5484_(along, k * sign);
               VoxelShape shape = this.f_58857_.m_8055_(counter).m_60812_(this.f_58857_, counter);
               if (!shape.m_83281_() && !(shape.m_83297_(Axis.Y) < 0.9)) {
                  BlockPos front = counter.m_121945_(customer);

                  for (int dy : new int[]{0, -1}) {
                     if (this.standable(front.m_6630_(dy))) {
                        spots.add(front.m_6630_(dy).m_7949_());
                        break;
                     }
                  }
               }
            }
         }

         return spots;
      }
   }

   public BlockPos claimSpot(BarPatron patron, BlockPos near) {
      BlockPos front = this.front();
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;

      for (BlockPos spot : this.counterSpots()) {
         if (!spot.equals(front)) {
            UUID owner = this.claims.get(spot);
            if (owner == null || owner.equals(patron.m_20148_()) || !this.patrons.containsKey(owner)) {
               double d = spot.m_123331_(near);
               if (d < bestDistance) {
                  best = spot;
                  bestDistance = d;
               }
            }
         }
      }

      if (best != null) {
         this.releaseSeat(patron);
         this.claims.put(best, patron.m_20148_());
      }

      return best;
   }

   void checkMess(ServerLevel server) {
      if (this.empties() >= 5) {
         for (UUID id : this.patrons.keySet()) {
            if (server.m_8791_(id) instanceof BarPatron p && p.state() == BarPatron.State.ENJOYING && !p.talking()) {
               p.say(
                  new String[]{
                     "Could someone clear these empties?",
                     "This bar's a mess.",
                     "Bartender, you've got a few glasses here...",
                     "It's like a bottle graveyard in here."
                  }[server.f_46441_.m_188503_(4)]
               );
               this.reputation(-1);
               return;
            }
         }
      }
   }

   public void maybeFight(ServerLevel server) {
      List<BarPatron> drunk = new ArrayList<>();

      for (UUID id : this.patrons.keySet()) {
         Entity var6 = server.m_8791_(id);
         if (var6 instanceof BarPatron) {
            BarPatron p = (BarPatron)var6;
            if (p.state() == BarPatron.State.ENJOYING && p.drunk() >= 4.5F) {
               drunk.add(p);
            }
         }
      }

      if (drunk.size() >= 2 && server.f_46441_.m_188503_(12) == 0) {
         Collections.shuffle(drunk, new Random(server.f_46441_.m_188505_()));
         BarPatron.brawl(drunk.get(0), drunk.get(1));
         this.reputation(-3);
         this.fightCooldown = 6000 + server.f_46441_.m_188503_(6000);
      }
   }

   public boolean startFight(BarPatron a, BarPatron b) {
      BarPatron.brawl(a, b);
      this.fightCooldown = 6000;
      return true;
   }

   public void button(Player player, int id) {
      if (this.f_58857_ != null) {
         switch (id) {
            case 0:
               this.open = !this.open;
               if (this.open) {
                  this.spawnCooldown = Math.min(this.spawnCooldown, 60);
               }
               break;
            case 1:
               this.capacity = Math.max(1, this.capacity - 1);
               break;
            case 2:
               this.capacity = Math.min(this.maxCapacity(), this.capacity + 1);
               break;
            case 3:
               this.collect(player);
               break;
            case 4:
            case 5:
               RegisterLinking.start(player, this.f_58858_, id == 4 ? RegisterLinking.Mode.DOOR : RegisterLinking.Mode.SEATS);
               player.m_6915_();
               break;
            case 6:
               this.seats.clear();
               this.claims.clear();
               break;
            case 7:
               this.stock.set(0, BarProducts.ALL.size());
               break;
            case 8:
               this.stock.clear();
               break;
            case 9:
               this.staffOff = !this.staffOff;
               this.callIn = !this.staffOff;
               if (this.f_58857_ instanceof ServerLevel server) {
                  this.keepStaff(server);
               }

               if (this.level(BarUpgrades.Upgrade.BARKEEP) > 0) {
                  player.m_5661_(Component.m_237113_(this.staffOff ? "Sent your barkeep home (no wages while off)" : "Called your barkeep in"), true);
               }
               break;
            case 10:
               if (this.open || !this.patrons.isEmpty()) {
                  player.m_5661_(Component.m_237113_("Close the store and wait for customers to leave before changing their style"), true);
               } else {
                  this.style = (this.style + 1) % STYLES.length;
                  if (this.f_58857_ instanceof ServerLevel server) {
                     for (BarWorker w : this.staff(server)) {
                        w.setMedieval(this.medieval());
                     }
                  }

                  player.m_5661_(Component.m_237113_("Customer style: " + STYLES[this.style]), true);
               }
               break;
            default:
               int product = id - 100;
               if (BarProducts.get(product) != null) {
                  this.stock.flip(product);
               }
         }

         this.sync();
      }
   }

   public void collect(Player player) {
      int left = this.till;

      while (left > 0) {
         int n = Math.min(left, 64);
         left -= n;
         ItemStack cash = new ItemStack((ItemLike)Drunkard.CASH.get(), n);
         if (!player.m_150109_().m_36054_(cash)) {
            player.m_36176_(cash, false);
         }
      }

      for (int i = 0; i < this.dishes.size(); i++) {
         ItemStack dish = (ItemStack)this.dishes.set(i, ItemStack.f_41583_);
         if (!dish.m_41619_() && !player.m_150109_().m_36054_(dish)) {
            player.m_36176_(dish, false);
         }
      }

      if (this.till > 0) {
         player.m_5661_(Component.m_237113_("Collected $" + this.till), true);
      }

      this.till = 0;
   }

   public boolean toggleSeat(BlockPos pos) {
      if (this.seats.remove(pos)) {
         this.claims.remove(pos);
         this.sync();
         return false;
      } else if (this.seats.size() >= 32) {
         return false;
      } else {
         this.seats.add(pos.m_7949_());
         this.sync();
         return true;
      }
   }

   public void setDoor(BlockPos pos) {
      this.door = pos.m_7949_();
      this.sync();
   }

   public void dropContents() {
      if (this.f_58857_ != null) {
         int left = this.till;
         this.till = 0;

         while (left > 0) {
            int n = Math.min(left, 64);
            left -= n;
            Containers.m_18992_(
               this.f_58857_,
               (double)this.f_58858_.m_123341_() + 0.5,
               (double)this.f_58858_.m_123342_() + 0.5,
               (double)this.f_58858_.m_123343_() + 0.5,
               new ItemStack((ItemLike)Drunkard.CASH.get(), n)
            );
         }

         Containers.m_19010_(this.f_58857_, this.f_58858_, this.dishes);
      }
   }

   public Component m_5446_() {
      return Component.m_237115_("block.drunkard.cash_register");
   }

   public AbstractContainerMenu m_7208_(int id, Inventory inventory, Player player) {
      return new CashRegisterMenu(id, inventory, this);
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128379_("Open", this.open);
      tag.m_128405_("Till", this.till);
      if (this.door != null) {
         tag.m_128356_("Door", this.door.m_121878_());
      }

      ListTag list = new ListTag();

      for (BlockPos seat : this.seats) {
         list.add(LongTag.m_128882_(seat.m_121878_()));
      }

      tag.m_128365_("Seats", list);
      ContainerHelper.m_18973_(tag, this.dishes);
      tag.m_128365_("Profile", this.profile());
   }

   // Everything about the pub that is not tied to this spot. It stays on the register item when broken.
   public CompoundTag profile() {
      CompoundTag tag = new CompoundTag();
      tag.m_128405_("Capacity", this.capacity);
      tag.m_128379_("StaffOff", this.staffOff);
      tag.m_128405_("Style", this.style);
      tag.m_128388_("Stock", this.stock.toLongArray());
      tag.m_128405_("Reputation", this.reputation);
      tag.m_128356_("LedgerDay", this.ledgerDay);
      CompoundTag levels = new CompoundTag();

      for (BarUpgrades.Upgrade u : BarUpgrades.Upgrade.values()) {
         if (this.level(u) > 0) {
            levels.m_128405_(u.name(), this.level(u));
         }
      }

      tag.m_128365_("UpgradeLevels", levels);
      tag.m_128405_("Served", this.servedToday);
      tag.m_128405_("Takings", this.takingsToday);
      tag.m_128405_("Tips", this.tipsToday);
      tag.m_128405_("Walkouts", this.walkoutsToday);
      ListTag list2 = new ListTag();

      for (CashRegisterEntity.Regular r : this.regulars) {
         CompoundTag e = new CompoundTag();
         e.m_128359_("Name", r.name());
         e.m_128405_("Skin", r.skin());
         e.m_128405_("Favourite", r.favourite());
         e.m_128405_("Visits", r.visits());
         list2.add(e);
      }

      tag.m_128365_("Regulars", list2);
      tag.m_128388_("Friendships", this.friendships.stream().mapToLong(Long::longValue).toArray());
      return tag;
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.open = tag.m_128471_("Open");
      this.till = Math.max(0, Math.min(999999, tag.m_128451_("Till")));
      this.door = tag.m_128441_("Door") ? BlockPos.m_122022_(tag.m_128454_("Door")) : null;
      this.seats.clear();

      for (Tag t : tag.m_128437_("Seats", 4)) {
         if (this.seats.size() < 32) {
            this.seats.add(BlockPos.m_122022_(((LongTag)t).m_7046_()));
         }
      }

      this.dishes.clear();
      ContainerHelper.m_18980_(tag, this.dishes);
      // Older saves keep the pub's details at the top level; newer ones keep them in "Profile".
      this.loadProfile(tag.m_128441_("Profile") ? tag.m_128469_("Profile") : tag);
   }

   public void loadProfile(CompoundTag tag) {
      this.capacity = tag.m_128441_("Capacity") ? Math.max(1, Math.min(26, tag.m_128451_("Capacity"))) : 8;
      this.staffOff = tag.m_128471_("StaffOff");
      this.style = Math.max(0, Math.min(STYLES.length - 1, tag.m_128451_("Style")));
      this.stock.clear();
      this.stock.or(BitSet.valueOf(tag.m_128467_("Stock")));
      this.stock.clear(BarProducts.ALL.size(), Math.max(BarProducts.ALL.size(), this.stock.length()));
      this.reputation = tag.m_128441_("Reputation") ? Math.max(0, Math.min(100, tag.m_128451_("Reputation"))) : 50;
      Arrays.fill(this.upgrades, 0);
      CompoundTag levels = tag.m_128469_("UpgradeLevels");

      for (BarUpgrades.Upgrade u : BarUpgrades.Upgrade.values()) {
         this.upgrades[u.ordinal()] = Math.max(0, Math.min(u.maxLevel(), levels.m_128451_(u.name())));
      }

      if (tag.m_128441_("Upgrades")) {
         String[] old = new String[]{"ADVERTISING", "PREMISES", "COMFORT", "PREMIUM", "BARBACK", "BOUNCER"};
         int[] saved = tag.m_128465_("Upgrades");

         for (int i = 0; i < Math.min(saved.length, old.length); i++) {
            for (BarUpgrades.Upgrade u : BarUpgrades.Upgrade.values()) {
               if (u.name().equals(old[i])) {
                  this.upgrades[u.ordinal()] = Math.max(0, Math.min(u.maxLevel(), saved[i]));
               }
            }
         }
      }

      this.ledgerDay = tag.m_128441_("LedgerDay") ? tag.m_128454_("LedgerDay") : -1L;
      this.servedToday = tag.m_128451_("Served");
      this.takingsToday = tag.m_128451_("Takings");
      this.tipsToday = tag.m_128451_("Tips");
      this.walkoutsToday = tag.m_128451_("Walkouts");
      this.friendships.clear();

      for (long f : tag.m_128467_("Friendships")) {
         this.friendships.add(Long.valueOf(f));
      }

      this.regulars.clear();

      for (Tag e : tag.m_128437_("Regulars", 10)) {
         CompoundTag c = (CompoundTag)e;
         if (this.regulars.size() < 16) {
            this.regulars.add(new CashRegisterEntity.Regular(c.m_128461_("Name"), c.m_128451_("Skin"), c.m_128451_("Favourite"), c.m_128451_("Visits")));
         }
      }
   }

   public static record Regular(String name, int skin, int favourite, int visits) {
   }
}
