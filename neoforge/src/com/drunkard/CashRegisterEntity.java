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
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
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
import net.minecraft.world.level.block.entity.BlockEntity.DataComponentInput;
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
   public final NonNullList<ItemStack> dishes = NonNullList.withSize(18, ItemStack.EMPTY);
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
      public int get(int i) {
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

      public void set(int i, int value) {
      }

      public int getCount() {
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

         this.setChanged();
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
      return (Direction)this.getBlockState().getValue(CashRegisterBlock.FACING);
   }

   public boolean ready() {
      return this.open && this.door != null && !this.stock.isEmpty() && this.level != null && this.doorSides() != null;
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
      this.patrons.put(patron.getUUID(), this.level == null ? 0L : this.level.getGameTime());
   }

   public void forget(BarPatron patron) {
      this.patrons.remove(patron.getUUID());
      this.releaseSeat(patron);
   }

   public boolean isHead(BarPatron patron) {
      return patron.getUUID().equals(this.head);
   }

   public BlockPos[] doorSides() {
      if (this.door != null && this.level != null) {
         BlockState state = this.level.getBlockState(this.door);
         if (!(state.getBlock() instanceof DoorBlock)) {
            return null;
         } else {
            BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? this.door.below() : this.door;
            Direction facing = (Direction)state.getValue(DoorBlock.FACING);
            BlockPos a = lower.relative(facing);
            BlockPos b = lower.relative(facing.getOpposite());
            return a.distSqr(this.worldPosition) > b.distSqr(this.worldPosition) ? new BlockPos[]{a, b} : new BlockPos[]{b, a};
         }
      } else {
         return null;
      }
   }

   public Direction customerSide() {
      long now = this.level == null ? 0L : this.level.getGameTime();
      if (this.customerSide != null && now - this.sideCheckedAt < 100L) {
         return this.customerSide;
      } else {
         this.sideCheckedAt = now;
         Direction a = this.facing().getOpposite();
         Direction b = this.facing();
         BlockPos fa = this.frontOn(a);
         BlockPos fb = this.frontOn(b);
         BlockPos[] sides = this.doorSides();
         if (fa == null && fb != null) {
            this.customerSide = b;
         } else if (fa != null && fb != null && sides != null) {
            this.customerSide = fa.distSqr(sides[1]) <= fb.distSqr(sides[1]) ? a : b;
         } else {
            this.customerSide = a;
         }

         return this.customerSide;
      }
   }

   private BlockPos frontOn(Direction side) {
      BlockPos base = this.worldPosition.relative(side);

      for (int dy : new int[]{0, -1, -2, 1}) {
         if (this.standable(base.above(dy))) {
            return base.above(dy);
         }
      }

      return null;
   }

   public BlockPos front() {
      BlockPos spot = this.frontOn(this.customerSide());
      return spot != null ? spot : this.worldPosition.relative(this.customerSide());
   }

   public boolean behindCounter(BlockPos p) {
      Direction bar = this.customerSide().getOpposite();
      Direction along = bar.getClockWise();
      int dx = p.getX() - this.worldPosition.getX();
      int dy = p.getY() - this.worldPosition.getY();
      int dz = p.getZ() - this.worldPosition.getZ();
      int depth = dx * bar.getStepX() + dz * bar.getStepZ();
      int side = dx * along.getStepX() + dz * along.getStepZ();
      if (this.counterMeasuredAt != this.sideCheckedAt) {
         this.measureCounter(along);
      }

      return depth >= 1 && depth <= 4 && side >= this.counterFrom && side <= this.counterTo && dy >= -3 && dy <= 2;
   }

   public boolean onCounter(BlockPos p) {
      Direction bar = this.customerSide().getOpposite();
      Direction along = bar.getClockWise();
      int dx = p.getX() - this.worldPosition.getX();
      int dz = p.getZ() - this.worldPosition.getZ();
      if (this.counterMeasuredAt != this.sideCheckedAt) {
         this.measureCounter(along);
      }

      int depth = dx * bar.getStepX() + dz * bar.getStepZ();
      int side = dx * along.getStepX() + dz * along.getStepZ();
      return depth == 0 && side >= this.counterFrom && side <= this.counterTo && p.getY() >= this.worldPosition.getY();
   }

   private void measureCounter(Direction along) {
      this.counterMeasuredAt = this.sideCheckedAt;
      if (this.level != null) {
         int hi = 0;
         int lo = 0;

         while (hi < 16 && this.counter(this.worldPosition.below().relative(along, hi + 1))) {
            hi++;
         }

         while (lo > -16 && this.counter(this.worldPosition.below().relative(along, lo - 1))) {
            lo--;
         }

         this.counterFrom = lo;
         this.counterTo = hi;
      }
   }

   private boolean counter(BlockPos p) {
      BlockState state = this.level.getBlockState(p);
      VoxelShape shape = state.getCollisionShape(this.level, p);
      return !shape.isEmpty() && shape.max(Axis.Y) >= 0.5 && !(state.getBlock() instanceof SeatBlock) && !this.seats.contains(p);
   }

   public BlockPos claimStandingSpot(BarPatron patron, BlockPos near, RandomSource random) {
      return this.claimStandingSpot(patron, near, random, 0.0);
   }

   public BlockPos claimStandingSpot(BarPatron patron, BlockPos near, RandomSource random, double away) {
      for (int i = 0; i < 24; i++) {
         BlockPos p = near.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);

         for (int dy : new int[]{0, -1, 1}) {
            BlockPos q = p.above(dy);
            if (this.standable(q) && !this.behindCounter(q) && !q.equals(this.front()) && (!(away > 0.0) || !(q.distSqr(this.front()) < away * away))) {
               UUID owner = this.claims.get(q);
               if (owner == null || owner.equals(patron.getUUID()) || !this.patrons.containsKey(owner)) {
                  this.claims.put(q.immutable(), patron.getUUID());
                  return q.immutable();
               }
            }
         }
      }

      return null;
   }

   public boolean standable(BlockPos p) {
      return this.level != null && !Double.isNaN(BarWorker.floorAt(this.level, p));
   }

   public void releaseSpot(BarPatron patron, BlockPos spot) {
      if (spot != null && patron.getUUID().equals(this.claims.get(spot))) {
         this.claims.remove(spot);
      }
   }

   public BlockPos claimSeat(BarPatron patron, BlockPos near, double range) {
      return this.claimSeat(patron, near, range, Set.of());
   }

   public BlockPos claimSeat(BarPatron patron, BlockPos near, double range, Set<BlockPos> avoid) {
      if (this.level == null) {
         return null;
      } else {
         this.releaseSeat(patron);
         BlockPos best = null;
         double bestDistance = range * range;

         for (BlockPos seat : this.seats) {
            UUID owner = this.claims.get(seat);
            if ((owner == null || owner.equals(patron.getUUID()) || !this.patrons.containsKey(owner))
               && !avoid.contains(seat)
               && SeatEntity.seat(this.level.getBlockState(seat))
               && !SeatEntity.occupied(this.level, seat)) {
               double d = seat.distSqr(near);
               if (d <= bestDistance) {
                  best = seat;
                  bestDistance = d;
               }
            }
         }

         if (best != null) {
            this.claims.put(best, patron.getUUID());
         }

         return best;
      }
   }

   public void keepSeat(BarPatron patron, BlockPos seat) {
      if (seat != null) {
         this.claims.put(seat, patron.getUUID());
      }
   }

   public void releaseSeat(BarPatron patron) {
      this.claims.values().removeIf(id -> id.equals(patron.getUUID()));
   }

   public void pay(int dollars) {
      this.till = Math.min(999999, this.till + Math.max(0, dollars));
      this.setChanged();
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
      this.setChanged();
   }

   public void collectEmpties() {
      if (this.level != null) {
         for (BlockPos p : BlockPos.betweenClosed(this.worldPosition.offset(-16, -3, -16), this.worldPosition.offset(16, 1, 16))) {
            BlockState state = this.level.getBlockState(p);
            if (state.is((Block)Drunkard.PLACED_BOTTLE.get()) && this.level.getBlockEntity(p) instanceof BottleEntity b && empty(b.bottle)) {
               this.returnDish(b.bottle.copy());
               b.bottle = ItemStack.EMPTY;
               this.level.removeBlock(p, false);
            } else if (state.is((Block)Drunkard.PLACED_SHOT_GLASS.get())
               && this.level.getBlockEntity(p) instanceof ShotGlassEntity g
               && !ShotGlassItem.filled(g.glass)) {
               this.returnDish(new ItemStack((ItemLike)Drunkard.SHOT_GLASS.get()));
               g.glass = ItemStack.EMPTY;
               this.level.removeBlock(p, false);
            }
         }
      }
   }

   private static boolean empty(ItemStack s) {
      return s.is(Items.GLASS_BOTTLE)
         || s.is((Item)Drunkard.BEER_MUG.get())
         || s.is((Item)Drunkard.ROCKS_GLASS.get())
         || s.is((Item)Drunkard.HIGHBALL_GLASS.get())
         || s.is((Item)Drunkard.MARTINI_GLASS.get())
         || s.is((Item)Drunkard.MARGARITA_GLASS.get());
   }

   private void rollDay() {
      if (this.level != null) {
         long day = this.level.getDayTime() / 24000L;
         if (day != this.ledgerDay) {
            boolean newDay = this.ledgerDay >= 0L;
            this.ledgerDay = day;
            this.servedToday = this.takingsToday = this.tipsToday = this.walkoutsToday = 0;
            this.setChanged();
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
      if (this.level == null) {
         return list;
      } else {
         int range = 16;

         for (BlockPos q : BlockPos.betweenClosed(this.worldPosition.offset(-range, -4, -range), this.worldPosition.offset(range, 4, range))) {
            if (this.level.isLoaded(q)) {
               BlockEntity var6 = this.level.getBlockEntity(q);
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
      if (this.level == null) {
         return 0;
      } else {
         if (this.level.getGameTime() - this.safesCheckedAt > 60L) {
            this.safesCheckedAt = this.level.getGameTime();
            this.cachedSafes = this.safes();
         }

         long sum = 0L;

         for (SafeEntity s : this.cachedSafes) {
            if (!s.isRemoved()) {
               sum += (long)s.balance;
            }
         }

         return (int)Math.min(1073741823L, sum);
      }
   }

   public void payWages() {
      int total = this.wages();
      if (total != 0 && this.level != null) {
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
                  s.setChanged();
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

         for (Player p : this.level.getEntitiesOfClass(Player.class, new AABB(this.worldPosition).inflate(48.0))) {
            p.displayClientMessage(Component.literal(message), false);
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
         BarWorker w = (BarWorker)((EntityType)Drunkard.BAR_WORKER.get()).create(server);
         if (w == null || home == null) {
            return;
         }

         BlockPos[] sides = this.doorSides();
         BlockPos at = this.callIn && sides != null && this.standable(sides[1]) ? sides[1] : home;
         double y = BarWorker.floorAt(this.level, at);
         w.moveTo(
            (double)at.getX() + 0.5, Double.isNaN(y) ? (double)at.getY() : y, (double)at.getZ() + 0.5, this.customerSide().toYRot(), 0.0F
         );
         int look = server.random.nextInt(65536);
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
                  look = server.random.nextInt(65536);
                  tries++;
                  continue;
               }
            }

            w.hire(this.worldPosition, look | this.styleBit());
            staff.add(w);
            server.addFreshEntity(w);
            if (at != home) {
               w.say(server.random.nextBoolean() ? "Evening, boss!" : "Reporting for duty!");
            }
            break;
         }
      }

      this.callIn = false;

      for (int i = wanted; i < staff.size(); i++) {
         staff.get(i).goHome(this.staffOff ? (server.random.nextBoolean() ? "Night, boss. See you tomorrow." : "Off home, then. Night!") : null);
      }
   }

   public List<BarWorker> staff(ServerLevel server) {
      return server.getEntitiesOfClass(BarWorker.class, new AABB(this.worldPosition).inflate(48.0), w -> this.worldPosition.equals(w.registerPos()));
   }

   private void dismissStaff() {
      if (this.level instanceof ServerLevel server) {
         for (BarWorker w : this.staff(server)) {
            w.say("No pay, no work. I'm off.");
            w.quit();
         }
      }
   }

   @Nullable
   public BlockPos workerHome() {
      BlockPos base = this.worldPosition.relative(this.customerSide().getOpposite());

      for (int dy : new int[]{-1, -2, -3, 0, 1}) {
         if (this.standable(base.above(dy))) {
            return base.above(dy);
         }
      }

      for (int r = 1; r <= 3; r++) {
         for (BlockPos q : BlockPos.betweenClosed(base.offset(-r, -3, -r), base.offset(r, 1, r))) {
            if (this.standable(q) && this.behindCounter(q)) {
               return q.immutable();
            }
         }
      }

      return null;
   }

   public List<BarPatron> waitingCustomers(ServerLevel server) {
      List<BarPatron> list = new ArrayList<>();

      for (UUID id : this.patrons.keySet()) {
         Entity var6 = server.getEntity(id);
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
      return this.worldPosition.getY() - this.front().getY() >= 2;
   }

   private void reputation(int change) {
      this.reputation = Math.max(0, Math.min(100, this.reputation + change));
      this.setChanged();
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
      this.setChanged();
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
                     r.name(), patron.skin(), patron.order() >= 0 && patron.getRandom().nextBoolean() ? patron.order() : r.favourite(), r.visits() + 1
                  )
               );
            this.sync();
            return;
         }
      }

      if (!patron.regular() && (patron.visits() >= 2 || patron.getRandom().nextFloat() < 0.35F + (float)this.reputation / 300.0F)) {
         if (this.regulars.size() >= 16) {
            this.regulars.remove(this.regulars.stream().min(Comparator.comparingInt(CashRegisterEntity.Regular::visits)).orElseThrow());
         }

         this.regulars.add(new CashRegisterEntity.Regular(patron.name(), patron.skin(), Math.max(0, patron.order()), 1));
         this.sync();
      }
   }

   private CashRegisterEntity.Regular returning(ServerLevel server) {
      if (!this.regulars.isEmpty() && !(server.random.nextFloat() > 0.15F + (float)this.reputation * 0.004F)) {
         Set<String> inside = new HashSet<>();

         for (UUID id : this.patrons.keySet()) {
            if (server.getEntity(id) instanceof BarPatron p) {
               inside.add((p.skin() & ~Medieval.SKIN_BIT) + "");
            }
         }

         List<CashRegisterEntity.Regular> free = new ArrayList<>();

         for (CashRegisterEntity.Regular r : this.regulars) {
            if (!inside.contains((r.skin() & ~Medieval.SKIN_BIT) + "")) {
               free.add(r);
            }
         }

         return free.isEmpty() ? null : free.get(server.random.nextInt(free.size()));
      } else {
         return null;
      }
   }

   public int empties() {
      if (this.level == null) {
         return 0;
      } else {
         int n = 0;

         for (BlockPos p : BlockPos.betweenClosed(this.worldPosition.offset(-16, -3, -16), this.worldPosition.offset(16, 1, 16))) {
            Block b = this.level.getBlockState(p).getBlock();
            if (b == Drunkard.PLACED_BOTTLE.get()
               || b == Drunkard.PLACED_SHOT_GLASS.get() && this.level.getBlockEntity(p) instanceof ShotGlassEntity g && !ShotGlassItem.filled(g.glass)) {
               n++;
            }
         }

         return n;
      }
   }

   public void returnDish(ItemStack dish) {
      if (!dish.isEmpty()) {
         for (int i = 0; i < this.dishes.size() && !dish.isEmpty(); i++) {
            ItemStack slot = (ItemStack)this.dishes.get(i);
            if (slot.isEmpty()) {
               this.dishes.set(i, dish.copy());
               dish.setCount(0);
            } else if (ItemStack.isSameItemSameComponents(slot, dish) && slot.getCount() < slot.getMaxStackSize()) {
               int move = Math.min(dish.getCount(), slot.getMaxStackSize() - slot.getCount());
               slot.grow(move);
               dish.shrink(move);
            }
         }

         this.setChanged();
      }
   }

   public static void tick(Level level, BlockPos pos, BlockState state, CashRegisterEntity register) {
      if (level instanceof ServerLevel server) {
         long now = level.getGameTime();
         if ((Boolean)state.getValue(CashRegisterBlock.OPEN) != register.open) {
            level.setBlock(pos, (BlockState)state.setValue(CashRegisterBlock.OPEN, register.open), 3);
         }

         if (now % 10L == 0L) {
            register.patrons.values().removeIf(seen -> now - seen > 40L);
            register.claims.values().removeIf(idx -> !register.patrons.containsKey(idx));
            register.head = null;
            long earliest = Long.MAX_VALUE;

            for (UUID id : register.patrons.keySet()) {
               Entity var12 = server.getEntity(id);
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
            long time = level.getDayTime() % 24000L;
            float evening = time >= 12000L && time < 23000L ? 0.5F : (time >= 9000L ? 0.8F : 1.2F);
            register.spawnCooldown = (int)(
               (float)(600 + level.random.nextInt(1800))
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
            register.messCooldown = 1200 + level.random.nextInt(1200);
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
            if (server.getEntity(id) instanceof BarPatron p) {
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

         if (calm >= 3 && server.random.nextInt(3) == 0) {
            this.startRobbery(server);
         }
      }
   }

   public BarPatron startRobbery(ServerLevel server) {
      BarPatron robber = this.spawnCustomer(server);
      if (robber != null) {
         robber.becomeRobber();
      }

      this.robberCooldown = 14000 + server.random.nextInt(14000);
      return robber;
   }

   void maybeEvent(ServerLevel server) {
      List<BarPatron> ready = new ArrayList<>();

      for (UUID id : this.patrons.keySet()) {
         Entity var6 = server.getEntity(id);
         if (var6 instanceof BarPatron) {
            BarPatron p = (BarPatron)var6;
            if (p.state() == BarPatron.State.ENJOYING && !p.talking() && p.canChat()) {
               ready.add(p);
            }
         }
      }

      if (!ready.isEmpty() && server.random.nextInt(2) == 0) {
         if (ready.get(server.random.nextInt(ready.size())).startEvent(null)) {
            this.eventCooldown = 700 + server.random.nextInt(1500);
         }
      }
   }

   public boolean free(BlockPos pos, BarPatron patron) {
      UUID owner = this.claims.get(pos);
      return owner == null || owner.equals(patron.getUUID()) || !this.patrons.containsKey(owner);
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
            int roll = server.random.nextInt(100);
            int size = roll < 30 ? 1 : (roll < 65 ? 2 : (roll < 85 ? 3 : 4));
            List<BarPatron> group = this.spawnGroup(server, Math.min(size, room));
            return group.isEmpty() ? null : group.get(0);
         }
      } else {
         return null;
      }
   }

   public int freeSeats() {
      if (this.level == null) {
         return 0;
      } else {
         int usable = 0;

         for (BlockPos seat : this.seats) {
            if (this.level.isLoaded(seat) && SeatEntity.seat(this.level.getBlockState(seat))) {
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
         BarPatron patron = (BarPatron)((EntityType)Drunkard.BAR_PATRON.get()).create(server);
         if (patron == null) {
            return null;
         } else {
            BlockPos outside = sides[0];
            BlockPos inside = sides[1];
            Direction out = Direction.getNearest((float)(outside.getX() - inside.getX()), 0.0F, (float)(outside.getZ() - inside.getZ()));
            BlockPos far = outside;

            for (int i = 3; i >= 1; i--) {
               if (this.standable(outside.relative(out, i))) {
                  far = outside.relative(out, i);
                  break;
               }
            }

            patron.moveTo((double)far.getX() + 0.5, (double)far.getY(), (double)far.getZ() + 0.5, out.getOpposite().toYRot(), 0.0F);
            patron.finalizeSpawn(server, server.getCurrentDifficultyAt(outside), MobSpawnType.EVENT, null);
            CashRegisterEntity.Regular regular = this.returning(server);
            if (regular != null) {
               patron.becomeRegular(regular);
            }

            patron.setMedieval(this.medieval());
            patron.arrive(this.worldPosition, this.door, inside, outside, far, group, delay);
            server.addFreshEntity(patron);
            this.track(patron);
            return patron;
         }
      }
   }

   public List<BlockPos> counterSpots() {
      List<BlockPos> spots = new ArrayList<>();
      if (this.level == null) {
         return spots;
      } else {
         Direction customer = this.customerSide();
         Direction along = customer.getClockWise();
         BlockPos base = this.worldPosition.below();

         for (int k = 0; k <= 6; k++) {
            for (int sign : k == 0 ? new int[]{1} : new int[]{1, -1}) {
               BlockPos counter = base.relative(along, k * sign);
               VoxelShape shape = this.level.getBlockState(counter).getCollisionShape(this.level, counter);
               if (!shape.isEmpty() && !(shape.max(Axis.Y) < 0.9)) {
                  BlockPos front = counter.relative(customer);

                  for (int dy : new int[]{0, -1}) {
                     if (this.standable(front.above(dy))) {
                        spots.add(front.above(dy).immutable());
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
            if (owner == null || owner.equals(patron.getUUID()) || !this.patrons.containsKey(owner)) {
               double d = spot.distSqr(near);
               if (d < bestDistance) {
                  best = spot;
                  bestDistance = d;
               }
            }
         }
      }

      if (best != null) {
         this.releaseSeat(patron);
         this.claims.put(best, patron.getUUID());
      }

      return best;
   }

   void checkMess(ServerLevel server) {
      if (this.empties() >= 5) {
         for (UUID id : this.patrons.keySet()) {
            if (server.getEntity(id) instanceof BarPatron p && p.state() == BarPatron.State.ENJOYING && !p.talking()) {
               p.say(
                  new String[]{
                     "Could someone clear these empties?",
                     "This bar's a mess.",
                     "Bartender, you've got a few glasses here...",
                     "It's like a bottle graveyard in here."
                  }[server.random.nextInt(4)]
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
         Entity var6 = server.getEntity(id);
         if (var6 instanceof BarPatron) {
            BarPatron p = (BarPatron)var6;
            if (p.state() == BarPatron.State.ENJOYING && p.drunk() >= 4.5F) {
               drunk.add(p);
            }
         }
      }

      if (drunk.size() >= 2 && server.random.nextInt(12) == 0) {
         Collections.shuffle(drunk, new Random(server.random.nextLong()));
         BarPatron.brawl(drunk.get(0), drunk.get(1));
         this.reputation(-3);
         this.fightCooldown = 6000 + server.random.nextInt(6000);
      }
   }

   public boolean startFight(BarPatron a, BarPatron b) {
      BarPatron.brawl(a, b);
      this.fightCooldown = 6000;
      return true;
   }

   public void button(Player player, int id) {
      if (this.level != null) {
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
               RegisterLinking.start(player, this.worldPosition, id == 4 ? RegisterLinking.Mode.DOOR : RegisterLinking.Mode.SEATS);
               player.closeContainer();
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
               if (this.level instanceof ServerLevel server) {
                  this.keepStaff(server);
               }

               if (this.level(BarUpgrades.Upgrade.BARKEEP) > 0) {
                  player.displayClientMessage(Component.literal(this.staffOff ? "Sent your barkeep home (no wages while off)" : "Called your barkeep in"), true);
               }
               break;
            case 10:
               if (this.open || !this.patrons.isEmpty()) {
                  player.displayClientMessage(Component.literal("Close the store and wait for customers to leave before changing their style"), true);
               } else {
                  this.style = (this.style + 1) % STYLES.length;
                  if (this.level instanceof ServerLevel server) {
                     for (BarWorker w : this.staff(server)) {
                        w.setMedieval(this.medieval());
                     }
                  }

                  player.displayClientMessage(Component.literal("Customer style: " + STYLES[this.style]), true);
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
         if (!player.getInventory().add(cash)) {
            player.drop(cash, false);
         }
      }

      for (int i = 0; i < this.dishes.size(); i++) {
         ItemStack dish = (ItemStack)this.dishes.set(i, ItemStack.EMPTY);
         if (!dish.isEmpty() && !player.getInventory().add(dish)) {
            player.drop(dish, false);
         }
      }

      if (this.till > 0) {
         player.displayClientMessage(Component.literal("Collected $" + this.till), true);
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
         this.seats.add(pos.immutable());
         this.sync();
         return true;
      }
   }

   public void setDoor(BlockPos pos) {
      this.door = pos.immutable();
      this.sync();
   }

   public void dropContents() {
      if (this.level != null) {
         int left = this.till;
         this.till = 0;

         while (left > 0) {
            int n = Math.min(left, 64);
            left -= n;
            Containers.dropItemStack(
               this.level,
               (double)this.worldPosition.getX() + 0.5,
               (double)this.worldPosition.getY() + 0.5,
               (double)this.worldPosition.getZ() + 0.5,
               new ItemStack((ItemLike)Drunkard.CASH.get(), n)
            );
         }

         Containers.dropContents(this.level, this.worldPosition, this.dishes);
      }
   }

   public Component getDisplayName() {
      return Component.translatable("block.drunkard.cash_register");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      return new CashRegisterMenu(id, inventory, this);
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putBoolean("Open", this.open);
      tag.putInt("Till", this.till);
      if (this.door != null) {
         tag.putLong("Door", this.door.asLong());
      }

      ListTag list = new ListTag();

      for (BlockPos seat : this.seats) {
         list.add(LongTag.valueOf(seat.asLong()));
      }

      tag.put("Seats", list);
      ContainerHelper.saveAllItems(tag, this.dishes, registries);
      tag.put("Profile", this.profile());
   }

   // The pub profile rides on the dropped register item (copied by the loot table) and comes back when it is placed.
   protected void collectImplicitComponents(Builder components) {
      super.collectImplicitComponents(components);
      CompoundTag data = new CompoundTag();
      data.put("Profile", this.profile());
      components.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
   }

   protected void applyImplicitComponents(DataComponentInput input) {
      super.applyImplicitComponents(input);
      CompoundTag data = ((CustomData)input.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      if (data.contains("Profile")) {
         this.loadProfile(data.getCompound("Profile"));
      }
   }

   // Everything about the pub that is not tied to this spot. It stays on the register item when broken.
   public CompoundTag profile() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("Capacity", this.capacity);
      tag.putBoolean("StaffOff", this.staffOff);
      tag.putInt("Style", this.style);
      tag.putLongArray("Stock", this.stock.toLongArray());
      tag.putInt("Reputation", this.reputation);
      tag.putLong("LedgerDay", this.ledgerDay);
      CompoundTag levels = new CompoundTag();

      for (BarUpgrades.Upgrade u : BarUpgrades.Upgrade.values()) {
         if (this.level(u) > 0) {
            levels.putInt(u.name(), this.level(u));
         }
      }

      tag.put("UpgradeLevels", levels);
      tag.putInt("Served", this.servedToday);
      tag.putInt("Takings", this.takingsToday);
      tag.putInt("Tips", this.tipsToday);
      tag.putInt("Walkouts", this.walkoutsToday);
      ListTag list2 = new ListTag();

      for (CashRegisterEntity.Regular r : this.regulars) {
         CompoundTag e = new CompoundTag();
         e.putString("Name", r.name());
         e.putInt("Skin", r.skin());
         e.putInt("Favourite", r.favourite());
         e.putInt("Visits", r.visits());
         list2.add(e);
      }

      tag.put("Regulars", list2);
      tag.putLongArray("Friendships", this.friendships.stream().mapToLong(Long::longValue).toArray());
      return tag;
   }

   public void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.open = tag.getBoolean("Open");
      this.till = Math.max(0, Math.min(999999, tag.getInt("Till")));
      this.door = tag.contains("Door") ? BlockPos.of(tag.getLong("Door")) : null;
      this.seats.clear();

      for (Tag t : tag.getList("Seats", 4)) {
         if (this.seats.size() < 32) {
            this.seats.add(BlockPos.of(((LongTag)t).getAsLong()));
         }
      }

      this.dishes.clear();
      ContainerHelper.loadAllItems(tag, this.dishes, registries);
      // Older saves keep the pub's details at the top level; newer ones keep them in "Profile".
      this.loadProfile(tag.contains("Profile") ? tag.getCompound("Profile") : tag);
   }

   public void loadProfile(CompoundTag tag) {
      this.capacity = tag.contains("Capacity") ? Math.max(1, Math.min(26, tag.getInt("Capacity"))) : 8;
      this.staffOff = tag.getBoolean("StaffOff");
      this.style = Math.max(0, Math.min(STYLES.length - 1, tag.getInt("Style")));
      this.stock.clear();
      this.stock.or(BitSet.valueOf(tag.getLongArray("Stock")));
      this.stock.clear(BarProducts.ALL.size(), Math.max(BarProducts.ALL.size(), this.stock.length()));
      this.reputation = tag.contains("Reputation") ? Math.max(0, Math.min(100, tag.getInt("Reputation"))) : 50;
      Arrays.fill(this.upgrades, 0);
      CompoundTag levels = tag.getCompound("UpgradeLevels");

      for (BarUpgrades.Upgrade u : BarUpgrades.Upgrade.values()) {
         this.upgrades[u.ordinal()] = Math.max(0, Math.min(u.maxLevel(), levels.getInt(u.name())));
      }

      if (tag.contains("Upgrades")) {
         String[] old = new String[]{"ADVERTISING", "PREMISES", "COMFORT", "PREMIUM", "BARBACK", "BOUNCER"};
         int[] saved = tag.getIntArray("Upgrades");

         for (int i = 0; i < Math.min(saved.length, old.length); i++) {
            for (BarUpgrades.Upgrade u : BarUpgrades.Upgrade.values()) {
               if (u.name().equals(old[i])) {
                  this.upgrades[u.ordinal()] = Math.max(0, Math.min(u.maxLevel(), saved[i]));
               }
            }
         }
      }

      this.ledgerDay = tag.contains("LedgerDay") ? tag.getLong("LedgerDay") : -1L;
      this.servedToday = tag.getInt("Served");
      this.takingsToday = tag.getInt("Takings");
      this.tipsToday = tag.getInt("Tips");
      this.walkoutsToday = tag.getInt("Walkouts");
      this.friendships.clear();

      for (long f : tag.getLongArray("Friendships")) {
         this.friendships.add(Long.valueOf(f));
      }

      this.regulars.clear();

      for (Tag e : tag.getList("Regulars", 10)) {
         CompoundTag c = (CompoundTag)e;
         if (this.regulars.size() < 16) {
            this.regulars.add(new CashRegisterEntity.Regular(c.getString("Name"), c.getInt("Skin"), c.getInt("Favourite"), c.getInt("Visits")));
         }
      }
   }

   public static record Regular(String name, int skin, int favourite, int visits) {
   }
}
