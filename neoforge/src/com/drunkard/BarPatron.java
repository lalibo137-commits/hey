package com.drunkard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BarPatron extends PathfinderMob {
   public static final int ROBBER_SKIN = 2097152;
   public static final float DRUNK_FACE = 4.5F;
   public static final int SKINS = 65536;
   public static final int PATIENCE = 4800;
   public static final int SHOT_PATIENCE = 3000;
   public static final int SIP_TICKS = 40;
   private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> ORDER = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> SHOT = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> SIP = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DRUNK = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<String> SPEECH = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Boolean> DANCING = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> CROUCH = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> EMOTE = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> EMOTE_AT = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> MOUTH = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<String> NAME = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> IMPATIENCE = SynchedEntityData.defineId(BarPatron.class, EntityDataSerializers.INT);
   private BlockPos register;
   private BlockPos door;
   private BlockPos inside;
   private BlockPos outside;
   private BlockPos far;
   private BlockPos seat;
   private BlockPos spot;
   private BlockPos wander;
   private UUID group = UUID.randomUUID();
   private UUID partner;
   private final Set<BlockPos> avoid = new HashSet<>();
   private long waitingSince;
   private int patience = 4800;
   private int sipsLeft;
   private int sipTimer;
   private int lingerTicks;
   private int visits;
   private int maxVisits = 3;
   private int speechTicks;
   private int walkTicks;
   private int fightTicks;
   private int hitCooldown;
   private int arriveDelay;
   private int leg;
   private int legTicks;
   private int callCooldown = 100;
   private int socialCooldown = 100;
   private int pendingTicks;
   private int glanceTicks;
   private int lookTicks;
   private int crouchTicks;
   private int crouchBursts;
   private int fidgetTicks = 100;
   private int wanderTicks;
   private int mingleCooldown = 400;
   private double bestDistance = Double.MAX_VALUE;
   private double pace = 1.0;
   private double navBest = Double.MAX_VALUE;
   private int navTicks;
   private BlockPos navTarget;
   private boolean gridMode;
   private boolean gridTried;
   private int gridReplans;
   private final ArrayDeque<BlockPos> gridRoute = new ArrayDeque<>();
   private boolean navFailed;
   private boolean wanderArrived;
   private boolean jogger;
   private Vec3 lookTarget;
   private float limit = 6.0F;
   private boolean announced;
   private boolean complained;
   private boolean orderedHere;
   private boolean openedDoor;
   private boolean stay;
   private String pendingLine = "";
   BarTalk.Conversation talk;
   private int chatCooldown = 100 + (int)(Math.random() * 300.0);
   private int gazeTicks;
   private int pendingEmoteTicks;
   private BarTalk.Emote pendingEmote = BarTalk.Emote.NONE;
   private BlockPos seatApproach;
   private BlockPos ashtray;
   private int ashtrayTicks;
   private int chug;
   private boolean regular;
   private boolean usual;
   private boolean angry;
   private boolean rang;
   private boolean stander;
   @Nullable
   private UUID dialogueWith;
   private int rapport;
   private int lastPrice;
   private boolean freeNext;
   private boolean forceUsual;
   private boolean seatWaitSaid;
   private int closingTicks = -1;
   private final Set<UUID> talkedWith = new HashSet<>();
   private final Set<UUID> greeted = new HashSet<>();
   private int favourite = -1;
   private int waitedLast;
   private int navTotal;
   private Path navPath;
   private int navNode;
   private int ownSmoke;
   private boolean smokedOwn;
   private boolean smokingOwn;
   private boolean tipEligible;
   private boolean robber;
   private int robPhase;
   private int robTicks;
   int robBeaten;
   private String lastTopic = "";

   public BarPatron(EntityType<? extends BarPatron> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
      this.setCanPickUpLoot(false);
      if (this.getNavigation() instanceof GroundPathNavigation ground) {
         ground.setCanOpenDoors(true);
         ground.setCanPassDoors(true);
         ground.setMaxVisitedNodesMultiplier(3.0F);
      }

      this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 48.0);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SKIN, 0);
      builder.define(STATE, BarPatron.State.ARRIVING.ordinal());
      builder.define(ORDER, -1);
      builder.define(SHOT, false);
      builder.define(SIP, -1000);
      builder.define(DRUNK, 0.0F);
      builder.define(SPEECH, "");
      builder.define(DANCING, false);
      builder.define(CROUCH, false);
      builder.define(EMOTE, 0);
      builder.define(EMOTE_AT, 0);
      builder.define(MOUTH, false);
      builder.define(NAME, "");
      builder.define(IMPATIENCE, 0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
   }

   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data
   ) {
      this.entityData.set(SKIN, this.random.nextInt(65536));
      this.stander = false;
      int smokeRoll = this.random.nextInt(100);
      this.ownSmoke = smokeRoll < 11 ? 1 : (smokeRoll < 17 ? 2 : (smokeRoll < 20 ? 3 : 0));
      this.limit = 4.0F + this.random.nextFloat() * 6.0F;
      this.maxVisits = 2 + this.random.nextInt(4);
      this.pace = 0.95 + this.random.nextDouble() * 0.3;
      this.jogger = this.random.nextInt(4) == 0;
      return super.finalizeSpawn(level, difficulty, reason, data);
   }

   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   protected boolean shouldDespawnInPeaceful() {
      return false;
   }

   protected void pushEntities() {
      if (this.state() != BarPatron.State.ARRIVING && this.state() != BarPatron.State.LEAVING && !this.headingToSeat()) {
         super.pushEntities();
      }
   }

   public boolean isPushable() {
      return this.state() != BarPatron.State.ARRIVING && this.state() != BarPatron.State.LEAVING && !this.isPassenger() && !this.headingToSeat();
   }

   private boolean headingToSeat() {
      return this.seat != null && !this.isPassenger() && this.wander == null;
   }

   // Sits as low as a player does (the old -0.35 riding offset).
   public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
      return new Vec3(0.0, 0.35, 0.0);
   }

   public int skin() {
      return (Integer)this.entityData.get(SKIN);
   }

   public boolean slim() {
      return (this.skin() & 1) == 1;
   }

   public String name() {
      return (String)this.entityData.get(NAME);
   }

   public boolean regular() {
      return this.regular;
   }

   public int favourite() {
      return this.favourite;
   }

   public int impatience() {
      return (Integer)this.entityData.get(IMPATIENCE);
   }

   public Component getName() {
      return (Component)(this.name().isEmpty() ? super.getName() : Component.literal(this.name()));
   }

   public void becomeRegular(CashRegisterEntity.Regular r) {
      this.regular = true;
      this.favourite = r.favourite();
      this.maxVisits++;
      this.entityData.set(NAME, r.name());
      this.entityData.set(SKIN, Medieval.keep(r.skin(), 65536));
   }

   private String bartenderName() {
      Player p = this.level().getNearestPlayer(this, 16.0);
      return p == null ? "friend" : p.getName().getString();
   }

   public BarPatron.State state() {
      return BarPatron.State.values()[Math.max(0, Math.min(BarPatron.State.values().length - 1, (Integer)this.entityData.get(STATE)))];
   }

   public int order() {
      return (Integer)this.entityData.get(ORDER);
   }

   public BarProducts.Product product() {
      return BarProducts.get(this.order());
   }

   public boolean wantsShot() {
      return (Boolean)this.entityData.get(SHOT);
   }

   public int sip() {
      return Math.max(0, 40 - (int)(this.level().getGameTime() - (long)((Integer)this.entityData.get(SIP)).intValue()));
   }

   public float sipAge(float partial) {
      return (float)((int)this.level().getGameTime() - (Integer)this.entityData.get(SIP)) + partial;
   }

   public float drunk() {
      return (Float)this.entityData.get(DRUNK);
   }

   public String speech() {
      return (String)this.entityData.get(SPEECH);
   }

   public boolean dancing() {
      return (Boolean)this.entityData.get(DANCING);
   }

   public boolean crouching() {
      return (Boolean)this.entityData.get(CROUCH);
   }

   public BarTalk.Emote emote() {
      return BarTalk.Emote.values()[Math.max(0, Math.min(BarTalk.Emote.values().length - 1, (Integer)this.entityData.get(EMOTE)))];
   }

   public float emoteAge(float partial) {
      return (float)((int)this.level().getGameTime() - (Integer)this.entityData.get(EMOTE_AT)) + partial;
   }

   public boolean cigarInMouth() {
      return (Boolean)this.entityData.get(MOUTH);
   }

   public long waitingSince() {
      return this.waitingSince;
   }

   public BlockPos seat() {
      return this.seat;
   }

   public BlockPos registerPos() {
      return this.register;
   }

   public UUID group() {
      return this.group;
   }

   public boolean unserved() {
      return this.state() == BarPatron.State.WAITING;
   }

   void setState(BarPatron.State state) {
      this.entityData.set(STATE, state.ordinal());
      this.walkTicks = 0;
   }

   public void setDrunk(float drinks) {
      this.entityData.set(DRUNK, Math.max(0.0F, drinks));
   }

   public void say(String line) {
      line = Medieval.speak(this.skin(), line);
      this.entityData.set(SPEECH, line);
      this.speechTicks = 50 + line.length() * 2;
   }

   public void setMedieval(boolean medieval) {
      int skin = this.skin() & ~Medieval.SKIN_BIT;
      this.entityData.set(SKIN, medieval ? skin | Medieval.SKIN_BIT : skin);
   }

   void sayLater(String line, int ticks) {
      this.pendingLine = line;
      this.pendingTicks = ticks;
   }

   public void emote(BarTalk.Emote e) {
      if (e != BarTalk.Emote.NONE) {
         this.entityData.set(EMOTE, e.ordinal());
         this.entityData.set(EMOTE_AT, (int)this.level().getGameTime());
      }
   }

   void emoteLater(BarTalk.Emote e, int ticks) {
      this.pendingEmote = e;
      this.pendingEmoteTicks = ticks;
   }

   boolean emoteBusy() {
      BarTalk.Emote e = this.emote();
      return e != BarTalk.Emote.NONE && this.level().getGameTime() - (long)((Integer)this.entityData.get(EMOTE_AT)).intValue() < (long)e.ticks;
   }

   public boolean queuing() {
      return this.state() == BarPatron.State.WAITING && !this.orderedHere && this.seat == null;
   }

   void lookAtHead(Entity other) {
      this.look(other.getEyePosition(), 12);
   }

   boolean canChat() {
      if (this.dialogueWith != null) {
         return false;
      } else {
         BarPatron.State s = this.state();
         if (s != BarPatron.State.ENJOYING && s != BarPatron.State.WAITING && s != BarPatron.State.AWAIT_SHOT) {
            return false;
         } else {
            return s == BarPatron.State.WAITING && !this.orderedHere ? this.isPassenger() : this.isPassenger() || this.getNavigation().isDone() && this.wander == null;
         }
      }
   }

   private void endTalk() {
      if (this.talk != null) {
         for (BarPatron p : this.talk.cast) {
            p.talk = null;
            p.chatCooldown = 300 + p.random.nextInt(700);
         }

         this.talk = null;
      }
   }

   private static String pick(RandomSource random, String... lines) {
      return lines[random.nextInt(lines.length)];
   }

   public void arrive(BlockPos register, @Nullable BlockPos door, BlockPos inside, BlockPos outside, BlockPos far, UUID group, int delay) {
      this.register = register;
      this.door = door;
      this.inside = inside;
      this.outside = outside;
      this.far = far;
      this.group = group;
      this.arriveDelay = delay;
      this.leg = 0;
      this.legTicks = 0;
      this.bestDistance = Double.MAX_VALUE;
      this.setState(BarPatron.State.ARRIVING);
   }

   public void skipEntrance() {
      if (this.robber) {
         this.startRobbery();
      } else {
         this.startWaiting();
      }
   }

   public void giveOwnSmoke(int kind) {
      this.ownSmoke = kind;
      this.smokedOwn = false;
   }

   private void startWaiting() {
      CashRegisterEntity r = this.register();
      this.setState(BarPatron.State.WAITING);
      this.entityData.set(ORDER, -1);
      this.waitingSince = this.level().getGameTime();
      this.patience = (int)(4800.0F * (r == null ? 1.0F : (0.8F + (float)r.reputation / 250.0F) * (1.0F + 0.15F * (float)r.level(BarUpgrades.Upgrade.COMFORT))));
      this.announced = false;
      this.complained = false;
      this.callCooldown = 40 + this.random.nextInt(60);
   }

   @Nullable
   public CashRegisterEntity register() {
      return this.register != null && this.level().isLoaded(this.register) && this.level().getBlockEntity(this.register) instanceof CashRegisterEntity r
         ? r
         : null;
   }

   private List<BarPatron> mates() {
      return this.level().getEntitiesOfClass(BarPatron.class, this.getBoundingBox().inflate(16.0), p -> p != this && p.group.equals(this.group) && p.isAlive());
   }

   public boolean chooseOrder(CashRegisterEntity r) {
      List<BarProducts.Product> stocked = r.stocked();
      if (stocked.isEmpty()) {
         return false;
      } else {
         if (this.regular) {
            BarProducts.get(this.favourite);
         } else {
            Object var10000 = null;
         }

         if (this.forceUsual && this.favourite < 0) {
            this.favourite = this.order();
         }

         BarProducts.Product fav = !this.regular && !this.forceUsual ? null : BarProducts.get(this.favourite);
         if (fav == null || !stocked.contains(fav) || !this.forceUsual && !(this.random.nextFloat() < 0.75F)) {
            this.usual = false;
            BarProducts.Product product = stocked.get(this.random.nextInt(stocked.size()));
            this.entityData.set(ORDER, BarProducts.ALL.indexOf(product));
            this.entityData.set(SHOT, product.shots() && this.random.nextFloat() < 0.75F);
            return true;
         } else {
            this.forceUsual = false;
            this.entityData.set(ORDER, this.favourite);
            this.entityData.set(SHOT, fav.shots() && this.random.nextFloat() < 0.75F);
            this.usual = true;
            return true;
         }
      }
   }

   public void setOrder(int product, boolean shot) {
      this.entityData.set(ORDER, product);
      this.entityData.set(SHOT, shot && BarProducts.get(product) != null && BarProducts.get(product).shots());
   }

   public String orderLine() {
      BarProducts.Product p = this.product();
      if (p == null) {
         return "";
      } else {
         String name = p.name().getString();
         if (this.usual) {
            return pick(this.random, "The usual, please!", "My usual, " + this.bartenderName() + "!", "You know what I like.");
         } else {
            return switch (p.kind()) {
               case SPIRIT -> this.wantsShot()
               ? pick(this.random, "A shot of " + name + ", please", "Pour me a " + name + " shot", "Shot of " + name + "!")
               : "A bottle of " + name + " for the table";
               case BEER -> pick(this.random, "One " + name + ", please", "Got any " + name + "?", "A cold " + name + " for me");
               case WINE -> pick(this.random, "A bottle of " + name + ", please", "We'll share a bottle of " + name);
               case COCKTAIL -> pick(
               this.random, "One " + name + ", please", "Can you make " + ("AEIOUaeiou".indexOf(name.charAt(0)) >= 0 ? "an " : "a ") + name + "?"
            );
               case CIGAR -> "A good cigar, if you've got one";
               case CIGARETTES -> "A pack of smokes, please";
            };
         }
      }
   }

   public boolean serve(Player bartender) {
      CashRegisterEntity r = this.register();
      BarProducts.Product p = this.product();
      if (this.state() != BarPatron.State.WAITING || p == null || r == null) {
         return false;
      } else if (this.wantsShot()) {
         this.setState(BarPatron.State.AWAIT_SHOT);
         this.patience = 3000;
         this.say(pick(this.random, "In a shot glass, please!", "Line it up!"));
         bartender.displayClientMessage(Component.literal("Set a shot glass down near them and pour the " + p.name().getString()), true);
         return true;
      } else {
         ItemStack given = ItemStack.EMPTY;
         if (bartender.getAbilities().instabuild) {
            given = p.icon();
         } else {
            for (int i = 0; i < bartender.getInventory().getContainerSize(); i++) {
               ItemStack stack = bartender.getInventory().getItem(i);
               if (BarProducts.matches(p, stack)) {
                  given = stack.split(1);
                  break;
               }
            }
         }

         if (!given.isEmpty()) {
            this.receive(r, given, bartender);
            return true;
         } else {
            bartender.displayClientMessage(
               Component.literal(
                  "You don't have "
                     + (
                        p.kind() != BarProducts.Kind.SPIRIT && p.kind() != BarProducts.Kind.BEER && p.kind() != BarProducts.Kind.WINE
                           ? ""
                           : "a full bottle of "
                     )
                     + p.name().getString()
               ),
               true
            );
            return false;
         }
      }
   }

   private boolean handOver(Player bartender, ItemStack held) {
      CashRegisterEntity r = this.register();
      BarProducts.Product p = this.product();
      if (r != null && p != null && !held.isEmpty()) {
         boolean shotGlass = (this.state() == BarPatron.State.WAITING || this.state() == BarPatron.State.AWAIT_SHOT)
            && this.wantsShot()
            && ShotGlassItem.filled(held)
            && ShotGlassItem.type(held).equals(p.id());
         boolean whole = this.state() == BarPatron.State.WAITING && !this.wantsShot() && BarProducts.matches(p, held);
         if (!shotGlass && !whole) {
            return false;
         } else {
            ItemStack given = bartender.getAbilities().instabuild ? held.copyWithCount(1) : held.split(1);
            this.receive(r, given, bartender);
            return true;
         }
      } else {
         return false;
      }
   }

   public void refuse() {
      if (this.state() == BarPatron.State.WAITING) {
         this.say(pick(this.random, "Fine, something else then.", "Out of it? Huh.", "Okay, what DO you have?"));
         this.entityData.set(ORDER, -1);
         this.announced = false;
         this.patience -= 600;
      }
   }

   private void receive(CashRegisterEntity r, ItemStack item, @Nullable Player bartender) {
      this.tipEligible = bartender != null;
      BarProducts.Product p = this.product();
      ItemStack held = item;
      if (p.kind() == BarProducts.Kind.CIGARETTES) {
         held = new ItemStack((ItemLike)Drunkard.CIGARETTE.get());
      }

      if (held.getItem() instanceof CigarItem) {
         CigarItem.ignite(held, this.level().getGameTime());
      }

      this.entityData.set(MOUTH, held.is((Item)Drunkard.CIGAR.get()) && this.random.nextBoolean());
      this.setItemInHand(InteractionHand.MAIN_HAND, held);
      this.sipsLeft = p.sips(this.wantsShot());
      this.sipTimer = 40 + this.random.nextInt(60);
      int price = r.price(p, this.wantsShot());
      if (this.freeNext) {
         price = 0;
         this.freeNext = false;
      }

      this.lastPrice = price;
      r.pay(price);
      this.waitedLast = (int)(this.level().getGameTime() - this.waitingSince);
      r.served(price, this.waitedLast);
      this.orderedHere = true;
      this.say(
         this.waitedLast < 400
            ? pick(this.random, "That was quick!", "Now that's service!", "Cheers!", "You're the best.")
            : (
               this.waitedLast > 2400
                  ? pick(this.random, "Finally...", "About time.", "Took you long enough.")
                  : pick(this.random, "Cheers!", "Much obliged.", "Now we're talking.", "Put it on my tab... kidding!")
            )
      );
      if (bartender != null) {
         this.look(bartender.getEyePosition(), 40);
      }

      Component receipt = Component.literal("+$" + price + " · " + (this.wantsShot() ? "shot of " : "") + p.name().getString());
      if (bartender != null) {
         bartender.displayClientMessage(receipt, true);
      } else {
         for (Player near : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(12.0))) {
            near.displayClientMessage(receipt, true);
         }
      }

      this.level().playSound(null, this.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.4F, 1.6F);
      this.setState(this.isPassenger() ? BarPatron.State.ENJOYING : BarPatron.State.TO_SEAT);
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         this.clientEffects();
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      if (this.speechTicks > 0 && --this.speechTicks == 0) {
         this.entityData.set(SPEECH, "");
      }

      if (this.pendingTicks > 0 && --this.pendingTicks == 0 && !this.pendingLine.isEmpty()) {
         this.say(this.pendingLine);
         this.pendingLine = "";
      }

      if (this.pendingEmoteTicks > 0 && --this.pendingEmoteTicks == 0) {
         this.emote(this.pendingEmote);
      }

      if (this.talk != null && this.talk.cast[0] == this && !this.talk.tick()) {
         this.endTalk();
      }

      if (this.talk != null && !this.talk.involves(this)) {
         this.talk = null;
      }

      if (this.hitCooldown > 0) {
         this.hitCooldown--;
      }

      this.walkTicks++;
      if (this.dialogueWith != null) {
         Player partner = this.level().getPlayerByUUID(this.dialogueWith);
         if (partner != null && !(partner.distanceToSqr(this) > 100.0)) {
            CashRegisterEntity here = this.register();
            if (here != null) {
               here.track(this);
            }

            this.getNavigation().stop();
            this.look(partner.getEyePosition(), 10);
            return;
         }

         this.dialogueWith = null;
      }

      if (this.tickCount % 1200 == 0) {
         this.avoid.clear();
      }

      if (this.tickCount % 20 == 0) {
         int max = this.state() == BarPatron.State.AWAIT_SHOT ? 3000 : 4800;
         int level = this.state() != BarPatron.State.WAITING && this.state() != BarPatron.State.AWAIT_SHOT
            ? 0
            : (this.patience < max / 4 ? 2 : (this.patience < max / 2 ? 1 : 0));
         if (level != this.impatience()) {
            this.entityData.set(IMPATIENCE, level);
         }
      }

      CashRegisterEntity r = this.register();
      if (r != null) {
         r.track(this);
         if (this.state() != BarPatron.State.LEAVING) {
            if (this.seat != null) {
               r.keepSeat(this, this.seat);
            }

            if (this.spot != null) {
               r.keepSeat(this, this.spot);
            }

            if (this.wander != null) {
               r.keepSeat(this, this.wander);
            }

            if (this.ashtray != null) {
               r.keepSeat(this, this.ashtray);
            }
         }
      } else if (this.state() != BarPatron.State.LEAVING) {
         this.leave(null);
      }

      if (r != null
         && this.state() != BarPatron.State.ARRIVING
         && this.state() != BarPatron.State.LEAVING
         && this.state() != BarPatron.State.FIGHTING
         && !this.isPassenger()
         && !this.headingToSeat()
         && r.behindCounter(this.blockPosition())) {
         this.walkTo(r.front(), 1.2);
      } else {
         if (r != null
            && !r.open
            && this.state() != BarPatron.State.ARRIVING
            && this.state() != BarPatron.State.LEAVING
            && this.state() != BarPatron.State.FIGHTING
            && this.state() != BarPatron.State.ROBBING) {
            if (this.wrapUp(r)) {
               return;
            }
         } else {
            this.closingTicks = -1;
         }

         switch (this.state()) {
            case ARRIVING:
               this.arriving(r);
               break;
            case WAITING:
               this.waiting(r);
               break;
            case AWAIT_SHOT:
               this.awaitShot(r);
               break;
            case TO_SEAT:
               this.toSeat(r);
               break;
            case ENJOYING:
               this.enjoy(r);
               break;
            case FIGHTING:
               this.fight();
               break;
            case ROBBING:
               this.robbing(r);
               break;
            case LEAVING:
               this.leaving();
         }

         if (this.state() != BarPatron.State.FIGHTING
            && this.state() != BarPatron.State.ARRIVING
            && this.state() != BarPatron.State.LEAVING
            && this.state() != BarPatron.State.ROBBING) {
            this.behave(r);
            this.social(r);
         }

         if (this.drunk() >= 4.5F && !this.isPassenger() && !this.getNavigation().isDone() && this.random.nextInt(25) == 0) {
            this.setDeltaMovement(this.getDeltaMovement().add(this.random.nextGaussian() * 0.12, 0.0, this.random.nextGaussian() * 0.12));
         }
      }
   }

   private boolean wrapUp(CashRegisterEntity r) {
      if (this.closingTicks < 0) {
         boolean served = this.state() == BarPatron.State.ENJOYING || this.state() == BarPatron.State.TO_SEAT;
         this.closingTicks = served ? 100 + this.random.nextInt(600) : 20 + this.random.nextInt(200);
         this.stay = false;
         if (this.random.nextInt(3) == 0) {
            this.sayLater(
               pick(this.random, "Last call, huh?", "Guess that's closing time.", "Already? Aw.", "Time to wrap up, I suppose.", "Is it that time already?"),
               10 + this.random.nextInt(80)
            );
         }
      }

      if (this.sipsLeft > 0 && this.sipTimer > 70) {
         this.sipTimer = 30 + this.random.nextInt(40);
      }

      if (--this.closingTicks > 0) {
         return false;
      } else {
         if (this.talk != null) {
            this.endTalk();
         }

         this.say(
            this.drunk() >= 4.5F
               ? pick(this.random, "Night, everybody! *hic*", "Best... night... ever.")
               : pick(this.random, "Night!", "Thanks for tonight!", "See you next time.", "Right, home time.", "Night, " + this.bartenderName() + "!")
         );
         this.leave(r);
         return true;
      }
   }

   private void arriving(CashRegisterEntity r) {
      if (this.arriveDelay > 0) {
         this.arriveDelay--;
      } else if (r != null && !r.open) {
         this.say("Closed? Aw, man.");
         this.leave(r);
      } else {
         BlockPos[] route = new BlockPos[]{this.outside, this.inside};
         if (route[0] != null && route[1] != null) {
            if (this.leg >= route.length) {
               this.closeDoor();
               if (this.robber) {
                  this.startRobbery();
               } else {
                  this.startWaiting();
                  this.greet();
               }
            } else {
               if (this.near(this.door, 2.5)) {
                  this.openDoor();
                  if (!this.rang) {
                     this.rang = true;
                     this.level().playSound(null, this.door, (SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5F, 1.6F);
                  }
               }

               if (this.followLeg(route[this.leg], 1.1)) {
                  this.leg++;
               }
            }
         } else {
            if (this.robber) {
               this.startRobbery();
            } else {
               this.startWaiting();
            }
         }
      }
   }

   private void greet() {
      String bt = this.bartenderName();
      if (this.regular) {
         this.say(pick(this.random, "Evening, " + bt + "!", "Hey " + bt + "! Missed me?", "Honey, I'm home!", "My favourite pub!"));
      } else if (this.random.nextInt(3) == 0) {
         this.say(pick(this.random, "Evening!", "Nice place.", "Is this the pub everyone talks about?", "Smells like a good night.", "Room for one more?"));
      }
   }

   private void waiting(CashRegisterEntity r) {
      if (r != null) {
         if (this.order() < 0 && !this.chooseOrder(r)) {
            this.say("Nothing on the menu? I'm out.");
            this.leave(r);
         } else if (--this.patience <= 0) {
            this.say(pick(this.random, "Forget it, I'll go somewhere else.", "Worst service in town!", "I've been waiting forever!"));
            this.angry = true;
            r.walkedOut();
            this.leaveWithGroup(r);
         } else {
            Player bartender = this.nearestBartender(r, 40.0);
            boolean mustSit = r.tallBar();
            if ((!this.orderedHere || mustSit) && this.seat == null && (!this.stander || mustSit) && !this.isPassenger() && this.walkTicks % 30 == 1) {
               BlockPos anchor = r.getBlockPos();

               for (BarPatron mate : this.mates()) {
                  if (mate.seat != null) {
                     anchor = mate.seat;
                     break;
                  }
               }

               this.seat = r.claimSeat(this, anchor, 24.0, this.avoid);
               if (this.seat != null) {
                  this.spot = null;
               }
            }

            if ((!this.orderedHere || mustSit) && this.seat != null) {
               if (this.isPassenger() || this.goSit(r)) {
                  this.callForService(bartender);
               }
            } else if (mustSit && !this.isPassenger()) {
               if (this.spot == null && this.walkTicks % 30 == 1) {
                  this.spot = r.claimStandingSpot(this, r.front(), this.random, 3.0);
               }

               if (this.spot != null) {
                  this.walkTo(this.spot, 1.2);
               }

               if (!this.seatWaitSaid && this.walkTicks > 60) {
                  this.seatWaitSaid = true;
                  this.say(pick(this.random, "I'll grab a seat when one frees up.", "Full house, huh? I'll wait.", "Anyone leaving soon?"));
               }
            } else if (!this.orderedHere && r.isHead(this)) {
               if (this.walkTo(r.front(), 1.4)) {
                  this.lookAtBartenderOrRegister(r, bartender);
                  if (!this.announced) {
                     this.announced = true;
                     this.say(this.orderLine());
                     this.swing(InteractionHand.MAIN_HAND);
                  }

                  this.callForService(bartender);
               }
            } else if (this.isPassenger()) {
               this.callForService(bartender);
            } else {
               if (this.orderedHere && bartender != null && r.behindCounter(bartender.blockPosition()) && this.walkTicks % 60 == 1) {
                  BlockPos near = r.claimSpot(this, bartender.blockPosition());
                  if (near != null && !near.equals(this.spot)) {
                     if (this.spot != null && !near.equals(this.spot)) {
                        r.releaseSpot(this, this.spot);
                     }

                     this.spot = near;
                  }
               }

               if (this.spot != null) {
                  if (this.walkTo(this.spot, 1.2)) {
                     this.lookAtBartenderOrRegister(r, bartender);
                     this.callForService(bartender);
                  } else if (this.failed()) {
                     this.avoid.add(this.spot);
                     r.releaseSpot(this, this.spot);
                     this.spot = null;
                  }
               } else {
                  this.findPlace(r);
               }
            }
         }
      }
   }

   private void callForService(@Nullable Player bartender) {
      if (--this.callCooldown <= 0 && this.talk == null) {
         this.callCooldown = 240 + this.random.nextInt(300);
         if (bartender != null) {
            this.say(this.announced ? pick(this.random, "Bartender!", "Excuse me!", "Hey, over here!", "Still waiting!", "Yo! Bartender!") : this.orderLine());
            this.announced = true;
            this.look(bartender.getEyePosition(), 60);
            if (this.random.nextBoolean()) {
               this.crouchBursts = 2 + this.random.nextInt(2);
            } else {
               this.emote(BarTalk.Emote.WAVE);
            }
         }
      }
   }

   private void findPlace(CashRegisterEntity r) {
      if (this.seat == null && this.spot == null && this.walkTicks % 30 == 1) {
         BlockPos anchor = r.getBlockPos();

         for (BarPatron mate : this.mates()) {
            if (mate.seat != null) {
               anchor = mate.seat;
               break;
            }
         }

         this.seat = this.stander ? null : r.claimSeat(this, anchor, 24.0, this.avoid);
         if (this.seat == null) {
            this.spot = r.claimSpot(this, anchor);
         }

         if (this.seat == null && this.spot == null) {
            this.spot = r.claimStandingSpot(this, r.front(), this.random);
         }
      }

      if (this.seat != null) {
         this.goSit(r);
      } else if (this.spot != null && !this.walkTo(this.spot, 1.2) && this.failed()) {
         this.avoid.add(this.spot);
         r.releaseSpot(this, this.spot);
         this.spot = null;
      }
   }

   private void awaitShot(CashRegisterEntity r) {
      if (--this.patience <= 0) {
         this.say("Forget the shot, I'm leaving.");
         this.angry = true;
         if (r != null) {
            r.walkedOut();
         }

         this.leaveWithGroup(r);
      } else if (this.tickCount % 10 == 0) {
         BlockPos found = null;
         String type = this.product() == null ? "" : this.product().id();
         int reach = this.isPassenger() ? 2 : 3;

         for (BlockPos p : BlockPos.betweenClosed(this.blockPosition().offset(-reach, -2, -reach), this.blockPosition().offset(reach, 2, reach))) {
            BlockEntity var8 = this.level().getBlockEntity(p);
            if (var8 instanceof ShotGlassEntity) {
               ShotGlassEntity glass = (ShotGlassEntity)var8;
               if (ShotGlassItem.filled(glass.glass)) {
                  if (ShotGlassItem.type(glass.glass).equals(type)) {
                     found = p.immutable();
                     break;
                  }

                  if (!this.complained) {
                     this.complained = true;
                     this.say("That's not what I ordered!");
                  }
               }
            }
         }

         if (found == null) {
            if (r != null) {
               this.lookAtBartenderOrRegister(r, this.nearestBartender(r, 12.0));
            }
         } else {
            this.look(Vec3.atCenterOf(found), 20);
            if (!this.isPassenger() && this.distanceToSqr(Vec3.atCenterOf(found)) > 6.760000000000001) {
               this.walkTo(found, 2.2);
            } else {
               if (this.level().getBlockEntity(found) instanceof ShotGlassEntity glass) {
                  ItemStack shot = glass.glass;
                  glass.glass = ItemStack.EMPTY;
                  this.level().removeBlock(found, false);
                  this.swing(InteractionHand.MAIN_HAND);
                  if (r != null) {
                     this.receive(r, shot, null);
                  }
               }
            }
         }
      }
   }

   private void toSeat(CashRegisterEntity r) {
      if (!this.isPassenger() && r != null) {
         if (this.seat == null && this.walkTicks % 20 == 1) {
            BlockPos anchor = this.blockPosition();

            for (BarPatron mate : this.mates()) {
               if (mate.seat != null) {
                  anchor = mate.seat;
                  break;
               }
            }

            this.seat = this.stander ? null : r.claimSeat(this, anchor, 24.0, this.avoid);
            if (this.seat == null && this.spot == null) {
               this.spot = r.claimSpot(this, anchor);
               if (this.spot == null) {
                  this.spot = r.claimStandingSpot(this, r.front(), this.random);
               }
            }

            if (this.seat == null && this.spot == null) {
               this.setState(BarPatron.State.ENJOYING);
               return;
            }
         }

         if (this.seat != null) {
            if (this.goSit(r)) {
               if (this.spot != null) {
                  r.releaseSpot(this, this.spot);
                  this.spot = null;
               }

               this.setState(BarPatron.State.ENJOYING);
            }
         } else if (this.spot != null && (this.walkTo(this.spot, 1.2) || this.failed())) {
            this.setState(BarPatron.State.ENJOYING);
         }
      } else {
         this.setState(BarPatron.State.ENJOYING);
      }
   }

   private void enjoy(CashRegisterEntity r) {
      BarProducts.Product p = this.product();
      if (r != null && this.seat == null && !this.stander && this.wander == null && !this.isPassenger() && this.walkTicks % 100 == 50) {
         BlockPos free = r.claimSeat(this, this.blockPosition(), 24.0, this.avoid);
         if (free != null) {
            this.seat = free;
            this.spot = null;
         }
      }

      if (this.wander != null) {
         this.mingle(r);
      } else if (this.seat != null && !this.isPassenger()) {
         this.goSit(r);
      } else if (this.spot != null && !this.isPassenger() && !this.near(this.spot, 1.6) && !this.walkTo(this.spot, 1.2) && this.failed()) {
         this.spot = null;
      }

      if (this.sipsLeft <= 0 || p == null && !this.smokingOwn) {
         if (!this.getMainHandItem().isEmpty() || this.ashtray != null) {
            this.finishDrink(r);
         } else if (this.ownSmoke != 0 && !this.smokedOwn && this.wander == null && (r == null || r.open) && this.random.nextInt(50) == 0) {
            this.lightOwn();
         } else if (this.wander == null && --this.lingerTicks <= 0) {
            this.decide(r);
         }
      } else {
         this.restSmoke(r);
         if (--this.sipTimer <= 0) {
            this.sipTimer = this.chug > 0 ? 14 : (this.smokingOwn ? 50 + this.random.nextInt(60) : 120 + this.random.nextInt(160));
            if (this.chug > 0) {
               this.chug--;
            }

            if (this.ashtray != null && !this.takeFromAshtray()) {
               this.sipsLeft = 0;
               this.finishDrink(r);
               return;
            }

            this.sipNow(this.smokingOwn ? null : p);
            if (this.ashtray != null) {
               this.ashtrayTicks = 50;
            }

            if (this.sipsLeft <= 0) {
               this.finishDrink(r);
            }
         }
      }

      this.maybeMingle(r);
   }

   private void decide(CashRegisterEntity r) {
      boolean mateLeft = false;

      for (BarPatron mate : this.mates()) {
         if (mate.state() == BarPatron.State.LEAVING) {
            mateLeft = true;
         }
      }

      boolean done = r == null
         || !r.open
         || !this.stay && (this.drunk() >= this.limit || this.visits >= this.maxVisits || mateLeft || this.random.nextFloat() < 0.25F);
      this.stay = false;
      if (done) {
         String bt = this.bartenderName();
         this.say(
            this.drunk() >= 4.5F
               ? pick(this.random, "*hic* Best bar in town!", "I love you guys... *hic*", "Somebody call me a cab.")
               : (
                  this.regular
                     ? pick(this.random, "Same time tomorrow, " + bt + "!", "Night, " + bt + "!", "Put it on my tab... I'll be back!")
                     : pick(
                        this.random, "I'm good, see ya!", "Nice place. Night!", "That hit the spot.", "Alright, we're heading out!", "Thanks, " + bt + "!"
                     )
               )
         );
         this.leaveWithGroup(r);
      } else {
         this.startWaiting();
         if (r != null) {
            this.chooseOrder(r);
         }

         this.announced = true;
         this.say(pick(this.random, "Bartender! Another round!", "Hit me again!", "One more won't hurt.", "Same again? Nah, something new."));
         Player bartender = r == null ? null : this.nearestBartender(r, 40.0);
         if (bartender != null) {
            this.look(bartender.getEyePosition(), 60);
            this.swing(InteractionHand.MAIN_HAND);
         }
      }
   }

   private void maybeMingle(@Nullable CashRegisterEntity r) {
      if (r != null && this.wander == null && this.state() == BarPatron.State.ENJOYING && --this.mingleCooldown <= 0 && this.talk == null) {
         this.mingleCooldown = 600 + this.random.nextInt(900);
         if (!(this.random.nextFloat() > (this.dancing() ? 0.45F : 0.25F))) {
            BlockPos near = null;
            String line = null;
            BlockPos jukebox = this.dancing() ? this.findJukebox() : null;
            if (jukebox != null) {
               near = jukebox;
               line = pick(this.random, "I gotta dance to this!", "Come on, dance with me!");
            } else if (this.seat == null && !this.isPassenger()) {
               for (BarPatron mate : this.mates()) {
                  if (mate.distanceToSqr(this) > 4.0) {
                     near = mate.blockPosition();
                     line = pick(this.random, "Scoot over!", "Hey, what'd I miss?", "Mind if I join?");
                     break;
                  }
               }
            }

            if (near != null) {
               BlockPos target = r.claimStandingSpot(this, near, this.random);
               if (target != null) {
                  if (this.ashtray != null) {
                     this.takeFromAshtray();
                     this.releaseAshtray();
                  }

                  this.wander = target;
                  this.wanderArrived = false;
                  this.wanderTicks = 200 + this.random.nextInt(300);
                  if (this.isPassenger()) {
                     this.stopRiding();
                  }

                  if (line != null) {
                     this.say(line);
                  }
               }
            }
         }
      }
   }

   private void mingle(@Nullable CashRegisterEntity r) {
      if (!this.wanderArrived) {
         if (this.walkTo(this.wander, 1.3)) {
            this.wanderArrived = true;
         } else if (this.failed()) {
            this.endWander(r);
         }
      } else if (--this.wanderTicks <= 0) {
         this.endWander(r);
      } else {
         if (this.tickCount % 80 == 0) {
            BarPatron nearest = null;
            double best = 16.0;

            for (BarPatron p : this.level().getEntitiesOfClass(BarPatron.class, this.getBoundingBox().inflate(4.0), px -> px != this)) {
               double d = p.distanceToSqr(this);
               if (d < best) {
                  best = d;
                  nearest = p;
               }
            }

            if (nearest != null) {
               this.look(nearest.getEyePosition(), 60);
            }
         }
      }
   }

   private boolean goSit(CashRegisterEntity r) {
      if (this.seat == null) {
         return false;
      } else if (!this.besideSeat(this.seat, 2.0) && (this.seatApproach == null || !this.near(this.seatApproach, 1.1))) {
         if (this.seatApproach == null || this.walkTicks % 200 == 0) {
            this.seatApproach = this.approach(r, this.seat);
         }

         if (!this.walkTo(this.seatApproach, 0.8) && this.failed()) {
            if (this.besideSeat(this.seat, 3.5) && SeatEntity.sit(this.level(), this.seat, this, this.register)) {
               this.seatApproach = null;
               return true;
            }

            this.avoid.add(this.seat);
            if (r != null) {
               r.releaseSeat(this);
            }

            this.seat = null;
            this.seatApproach = null;
         }

         return false;
      } else if (SeatEntity.sit(this.level(), this.seat, this, this.register)) {
         this.seatApproach = null;
         return true;
      } else {
         this.avoid.add(this.seat);
         if (r != null) {
            r.releaseSeat(this);
         }

         this.seat = null;
         this.seatApproach = null;
         return false;
      }
   }

   private BlockPos approach(@Nullable CashRegisterEntity r, BlockPos seat) {
      BlockState state = this.level().getBlockState(seat);
      List<Direction> order = new ArrayList<>(Plane.HORIZONTAL.stream().toList());
      Direction first = null;
      Direction counter = this.register == null ? null : SeatEntity.counterSide(this.level(), seat, this.register);
      if (counter != null) {
         first = counter.getOpposite();
      } else if (state.getBlock() instanceof StairBlock) {
         first = ((Direction)state.getValue(StairBlock.FACING)).getOpposite();
      }

      if (first != null) {
         order.remove(first);
         order.add(0, first);
      }

      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;

      for (Direction d : order) {
         for (int dy : new int[]{0, -1, -2, -3, 1}) {
            BlockPos q = seat.relative(d).above(dy);
            if (r != null && r.standable(q) && !r.behindCounter(q)) {
               if (d == first) {
                  return q;
               }

               double dist = q.distSqr(this.blockPosition());
               if (dist < bestDistance) {
                  best = q;
                  bestDistance = dist;
               }
            }
         }
      }

      if (best == null && r != null) {
         for (Direction d : Plane.HORIZONTAL) {
            for (int dyx : new int[]{0, -1, -2, -3}) {
               BlockPos q = seat.relative(d).relative(d.getClockWise()).above(dyx);
               if (r.standable(q) && !r.behindCounter(q)) {
                  return q;
               }
            }
         }
      }

      if (best == null && r != null) {
         for (Direction d : order) {
            for (int dyxx : new int[]{0, -1, -2, -3}) {
               BlockPos q = seat.relative(d).above(dyxx);
               if (r.standable(q)) {
                  return q;
               }
            }
         }
      }

      return best != null ? best : seat;
   }

   private boolean besideSeat(BlockPos seat, double reach) {
      double dx = this.getX() - ((double)seat.getX() + 0.5);
      double dz = this.getZ() - ((double)seat.getZ() + 0.5);
      double dy = (double)seat.getY() - this.getY();
      return dx * dx + dz * dz <= reach * reach && dy > -1.2 && dy < 3.2;
   }

   private void endWander(@Nullable CashRegisterEntity r) {
      if (r != null) {
         r.releaseSpot(this, this.wander);
      }

      this.wander = null;
   }

   @Nullable
   private BlockPos findJukebox() {
      for (BlockPos p : BlockPos.betweenClosed(this.blockPosition().offset(-8, -2, -8), this.blockPosition().offset(8, 3, 8))) {
         if (this.level().getBlockState(p).getBlock() instanceof JukeboxBlock && (Boolean)this.level().getBlockState(p).getValue(JukeboxBlock.HAS_RECORD)) {
            return p.immutable();
         }
      }

      return null;
   }

   private void lightOwn() {
      ItemStack held = new ItemStack(
         this.ownSmoke == 1 ? (ItemLike)Drunkard.CIGARETTE.get() : (this.ownSmoke == 2 ? (ItemLike)Drunkard.CIGAR.get() : (ItemLike)Drunkard.PIPE.get())
      );
      CigarItem.ignite(held, this.level().getGameTime());
      this.setItemInHand(InteractionHand.MAIN_HAND, held);
      this.entityData.set(MOUTH, this.ownSmoke != 1 && this.random.nextBoolean());
      this.smokingOwn = true;
      this.smokedOwn = true;
      this.sipsLeft = (this.ownSmoke == 1 ? 4 : 6) + this.random.nextInt(4);
      this.sipTimer = 50 + this.random.nextInt(70);
      this.swing(InteractionHand.MAIN_HAND);
      this.level().playSound(null, this.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.NEUTRAL, 0.25F, 1.2F);
      if (this.random.nextInt(3) == 0) {
         this.say(pick(this.random, "Mind if I light up?", "*lights up*", "Time for a smoke."));
      }
   }

   private void sipNow(@Nullable BarProducts.Product p) {
      this.entityData.set(SIP, (int)this.level().getGameTime());
      if (p != null && !p.smoke()) {
         this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.5F, 0.9F + this.random.nextFloat() * 0.2F);
         this.setDrunk(this.drunk() + p.drinksPerSip());
      } else {
         this.level().playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.08F, 1.8F);
      }

      this.sipsLeft--;
   }

   private void finishDrink(CashRegisterEntity r) {
      if (this.ashtray != null) {
         if (this.level().getBlockEntity(this.ashtray) instanceof CigarHolderEntity holder && holder.cigar.getItem() instanceof CigarItem) {
            holder.cigar = ItemStack.EMPTY;
            holder.sync();
         }

         this.releaseAshtray();
      }

      this.chug = 0;
      if (this.smokingOwn) {
         this.smokingOwn = false;
         this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
         this.entityData.set(MOUTH, false);
         this.lingerTicks = 200 + this.random.nextInt(600);
      } else {
         ItemStack held = this.getMainHandItem();
         ItemStack empty = held.getItem() instanceof ShotGlassItem
            ? new ItemStack((ItemLike)Drunkard.SHOT_GLASS.get())
            : (
               held.getItem() instanceof WineItem && this.product() != null && this.product().kind() == BarProducts.Kind.BEER
                  ? new ItemStack((ItemLike)Drunkard.BEER_MUG.get())
                  : (
                     held.getItem() instanceof WineItem
                        ? new ItemStack(Items.GLASS_BOTTLE)
                        : (held.getItem() instanceof CocktailItem ? glassOf(held) : ItemStack.EMPTY)
                  )
            );
         this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
         this.entityData.set(MOUTH, false);
         this.visits++;
         BlockPos top = this.counterTop();
         if (!empty.isEmpty() && (top == null || !this.placeOnCounter(top, empty)) && r != null) {
            r.returnDish(empty);
         }

         BarProducts.Product p = this.product();
         if (r != null && p != null && this.tipEligible) {
            float fast = (this.waitedLast < 400 ? 0.2F : (this.waitedLast < 1200 ? 0.1F : 0.0F)) + Math.max(-0.1F, Math.min(0.3F, (float)this.rapport * 0.05F));
            if (this.random.nextFloat() < 0.3F + fast * 2.0F + (this.regular ? 0.2F : 0.0F) + (float)r.reputation / 400.0F) {
               this.tip(
                  r,
                  Math.max(
                     1,
                     Math.round(
                        (float)p.price(this.wantsShot())
                           * (fast + 0.05F + this.random.nextFloat() * 0.12F + (this.regular ? 0.1F : 0.0F) + (this.drunk() >= 4.5F ? 0.1F : 0.0F))
                     )
                  )
               );
            }
         }

         if (this.drunk() >= this.limit + 3.0F) {
            this.say("*hic* ...where's the door?");
            this.leaveWithGroup(r);
         } else {
            this.lingerTicks = this.stay ? 60 + this.random.nextInt(100) : 600 + this.random.nextInt(1800);
            this.socialCooldown = 60 + this.random.nextInt(100);
         }
      }
   }

   @Nullable
   private BlockPos counterTop() {
      BlockPos at = this.isPassenger() && this.seat != null ? this.seat : this.blockPosition();
      if (this.register == null) {
         return null;
      } else {
         Direction side = SeatEntity.counterSide(this.level(), at, this.register);
         if (side == null) {
            return null;
         } else {
            BlockPos top = at.relative(side).above();
            return this.level().getBlockState(top).canBeReplaced() ? top : null;
         }
      }
   }

   private boolean placeOnCounter(BlockPos top, ItemStack empty) {
      Direction facing = Direction.fromYRot((double)this.getYRot()).getOpposite();
      if (empty.getItem() instanceof ShotGlassItem) {
         BlockState state = (BlockState)((Block)Drunkard.PLACED_SHOT_GLASS.get()).defaultBlockState().setValue(ShotGlassBlock.FACING, facing);
         if (!state.canSurvive(this.level(), top)) {
            return false;
         }

         this.level().setBlock(top, state, 3);
         if (this.level().getBlockEntity(top) instanceof ShotGlassEntity glass) {
            glass.glass = empty;
            glass.sync();
         }
      } else {
         BlockState statex = (BlockState)((Block)Drunkard.PLACED_BOTTLE.get()).defaultBlockState().setValue(PlacedBottleBlock.FACING, facing);
         if (!statex.canSurvive(this.level(), top)) {
            return false;
         }

         this.level().setBlock(top, statex, 3);
         if (this.level().getBlockEntity(top) instanceof BottleEntity bottle) {
            bottle.bottle = empty;
            bottle.sync();
         }
      }

      this.level().playSound(null, top, SoundEvents.GLASS_PLACE, SoundSource.NEUTRAL, 0.5F, 1.3F);
      this.swing(InteractionHand.MAIN_HAND);
      return true;
   }

   private static ItemStack glassOf(ItemStack cocktail) {
      if (cocktail.is((Item)Drunkard.MARTINI.get())) {
         return new ItemStack((ItemLike)Drunkard.MARTINI_GLASS.get());
      } else if (cocktail.is((Item)Drunkard.MARGARITA.get())) {
         return new ItemStack((ItemLike)Drunkard.MARGARITA_GLASS.get());
      } else {
         return !cocktail.is((Item)Drunkard.OLD_FASHIONED.get()) && !(cocktail.getItem() instanceof OnTheRocksItem)
            ? new ItemStack((ItemLike)Drunkard.HIGHBALL_GLASS.get())
            : new ItemStack((ItemLike)Drunkard.ROCKS_GLASS.get());
      }
   }

   private void tip(CashRegisterEntity r, int dollars) {
      r.pay(dollars);
      r.tipped(dollars);
      this.swing(InteractionHand.MAIN_HAND);
      this.level().playSound(null, this.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.25F, 1.9F);

      for (Player near : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(12.0))) {
         near.displayClientMessage(Component.literal("+$" + dollars + " tip"), true);
      }
   }

   private void restSmoke(@Nullable CashRegisterEntity r) {
      if (this.ashtrayTicks > 0 && --this.ashtrayTicks == 0 && this.ashtray != null) {
         this.putInAshtray();
      } else {
         ItemStack held = this.getMainHandItem();
         if (this.ashtray == null
            && held.getItem() instanceof CigarItem
            && CigarItem.lit(held)
            && this.wander == null
            && this.sip() <= 0
            && this.tickCount % 40 == 0) {
            if (this.isPassenger() || this.getNavigation().isDone()) {
               if (!this.cigarInMouth() || this.random.nextInt(4) == 0) {
                  BlockPos found = this.findAshtray(r);
                  if (found != null) {
                     this.ashtray = found;
                     if (r != null) {
                        r.keepSeat(this, found);
                     }

                     this.putInAshtray();
                  }
               }
            }
         }
      }
   }

   @Nullable
   private BlockPos findAshtray(@Nullable CashRegisterEntity r) {
      BlockPos best = null;
      double bestDistance = 6.760000000000001;
      Vec3 hand = this.getEyePosition().add(0.0, -0.5, 0.0);

      for (BlockPos p : BlockPos.betweenClosed(this.blockPosition().offset(-2, -1, -2), this.blockPosition().offset(2, 2, 2))) {
         BlockEntity d = this.level().getBlockEntity(p);
         if (d instanceof CigarHolderEntity) {
            CigarHolderEntity holder = (CigarHolderEntity)d;
            if (holder.cigar.isEmpty() && (r == null || r.free(p, this))) {
               double dx = Vec3.atCenterOf(p).distanceToSqr(hand);
               if (dx < bestDistance) {
                  best = p.immutable();
                  bestDistance = dx;
               }
            }
         }
      }

      return best;
   }

   private void putInAshtray() {
      if (this.ashtray != null) {
         ItemStack held = this.getMainHandItem();
         if (this.level().getBlockEntity(this.ashtray) instanceof CigarHolderEntity holder && holder.cigar.isEmpty() && held.getItem() instanceof CigarItem) {
            holder.cigar = held.copyWithCount(1);
            if (ItemData.get(holder.cigar).contains("InMouth")) {
               ItemData.update(holder.cigar, tag -> tag.remove("InMouth"));
            }

            holder.sync();
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.entityData.set(MOUTH, false);
            this.look(Vec3.atCenterOf(this.ashtray), 20);
            this.swing(InteractionHand.MAIN_HAND);
            return;
         }

         this.releaseAshtray();
      }
   }

   private boolean takeFromAshtray() {
      if (this.ashtray == null) {
         return false;
      } else if (this.getMainHandItem().getItem() instanceof CigarItem) {
         return true;
      } else {
         if (this.level().getBlockEntity(this.ashtray) instanceof CigarHolderEntity holder
            && holder.cigar.getItem() instanceof CigarItem
            && CigarItem.lit(holder.cigar)) {
            this.setItemInHand(InteractionHand.MAIN_HAND, holder.cigar);
            holder.cigar = ItemStack.EMPTY;
            holder.sync();
            this.look(Vec3.atCenterOf(this.ashtray), 15);
            this.swing(InteractionHand.MAIN_HAND);
            return true;
         }

         this.releaseAshtray();
         return false;
      }
   }

   private void releaseAshtray() {
      CashRegisterEntity r = this.register();
      if (this.ashtray != null && r != null) {
         r.releaseSpot(this, this.ashtray);
      }

      this.ashtray = null;
      this.ashtrayTicks = 0;
   }

   public BlockPos ashtray() {
      return this.ashtray;
   }

   public boolean startEvent(@Nullable BarTalk.Script forced) {
      if (this.talk == null && this.canChat()) {
         List<BarPatron> nearby = this.level().getEntitiesOfClass(BarPatron.class, this.getBoundingBox().inflate(12.0), px -> px != this && px.isAlive());
         List<BarPatron> partners = new ArrayList<>();

         for (BarPatron p : nearby) {
            if (p.talk == null && p.canChat() && p.distanceToSqr(this) < 16.0) {
               partners.add(p);
            }
         }

         partners.sort(Comparator.comparingDouble(px -> (double)(px.group.equals(this.group) ? 0 : 50) + px.distanceToSqr(this)));
         List<BarPatron> crowd = new ArrayList<>();

         for (BarPatron px : nearby) {
            BarPatron.State s = px.state();
            if (s != BarPatron.State.ARRIVING && s != BarPatron.State.LEAVING && s != BarPatron.State.FIGHTING && s != BarPatron.State.ROBBING) {
               crowd.add(px);
            }
         }

         BarTalk.Mood mood = this.mood();
         BarTalk.Script script = forced != null ? forced : BarTalk.pickEvent(1 + Math.min(2, partners.size()), crowd.size(), mood, this.random);
         if (script != null && script.roles() - 1 <= partners.size()) {
            BarPatron[] cast = new BarPatron[script.roles()];
            cast[0] = this;

            for (int i = 1; i < cast.length; i++) {
               cast[i] = partners.get(i - 1);
            }

            crowd.removeAll(Arrays.asList(cast));
            BarTalk.Conversation c = new BarTalk.Conversation(script, cast, mood, null, crowd);

            for (BarPatron pxx : cast) {
               pxx.talk = c;
               pxx.socialCooldown = Math.max(pxx.socialCooldown, 300);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   void act(String action, BarTalk.Conversation c, BarTalk.Line line) {
      String[] parts = action.split("=", 2);
      String var5 = parts[0];
      switch (var5) {
         case "CROWD":
            String[] shouts = parts.length > 1 ? parts[1].split("/") : new String[]{"Woo!"};
            BarTalk.Emote[] cheer = new BarTalk.Emote[]{BarTalk.Emote.CHEERS, BarTalk.Emote.CLAP, BarTalk.Emote.FIST_PUMP, BarTalk.Emote.LAUGH};
            int voices = 0;

            for (BarPatron pxx : c.crowd) {
               BarPatron.State s = pxx.state();
               if (pxx.isAlive()
                  && pxx.talk == null
                  && s != BarPatron.State.LEAVING
                  && s != BarPatron.State.FIGHTING
                  && s != BarPatron.State.ARRIVING
                  && s != BarPatron.State.ROBBING
                  && !(pxx.distanceToSqr(this) > 196.0)) {
                  pxx.chatCooldown = Math.max(pxx.chatCooldown, 200);
                  pxx.emoteLater(
                     line.reaction() != BarTalk.Emote.NONE ? line.reaction() : cheer[pxx.random.nextInt(cheer.length)], 6 + pxx.random.nextInt(12)
                  );
                  if (voices < 5) {
                     pxx.sayLater(BarTalk.fill(shouts[pxx.random.nextInt(shouts.length)], c.mood), 10 + voices++ * 7 + pxx.random.nextInt(8));
                  }
               }
            }
            break;
         case "WRESTLE":
            for (BarPatron pxx : c.cast) {
               pxx.emote(BarTalk.Emote.ARM_WRESTLE);
            }

            this.level().playSound(null, this.blockPosition(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 0.6F, 0.8F);
            break;
         case "HIGHFIVE":
            for (BarPatron pxx : c.cast) {
               pxx.emote(BarTalk.Emote.HIGH_FIVE);
            }

            this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.NEUTRAL, 0.7F, 1.7F);
            break;
         case "SLEEP":
            this.emote(BarTalk.Emote.SLEEP);
            this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.3F, 0.7F);
            break;
         case "SPILL":
            ItemStack held = this.getMainHandItem();
            if (held.isEmpty() || held.getItem() instanceof CigarItem) {
               return;
            }

            if (this.level() instanceof ServerLevel s) {
               s.sendParticles(
                  new ItemParticleOption(ParticleTypes.ITEM, held), this.getX(), this.getY() + 1.1, this.getZ(), 12, 0.2, 0.1, 0.2, 0.08
               );
               s.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.9, this.getZ(), 24, 0.35, 0.2, 0.35, 0.1);
            }

            this.level().playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.7F, 1.2F);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.sipsLeft = 0;
            this.visits++;
            this.stay = true;
            this.lingerTicks = 80 + this.random.nextInt(60);
            break;
         case "CHUG":
            for (BarPatron px : c.cast) {
               if (px.sipsLeft > 0 && !px.getMainHandItem().isEmpty() && !(px.getMainHandItem().getItem() instanceof CigarItem)) {
                  px.sipsLeft = Math.min(px.sipsLeft, 3);
                  px.chug = 3;
                  px.sipTimer = 4 + px.random.nextInt(6);
               }
            }
            break;
         case "ROUND":
            Set<BarPatron> table = new HashSet<>(this.mates());
            table.add(this);
            Collections.addAll(table, c.cast);

            for (BarPatron p : table) {
               if (p.state() == BarPatron.State.ENJOYING) {
                  p.stay = true;
                  if (p.sipsLeft <= 0 && p.getMainHandItem().isEmpty()) {
                     p.lingerTicks = Math.min(p.lingerTicks, 60 + p.random.nextInt(80));
                  }
               }
            }
            break;
         case "TIP":
            CashRegisterEntity r = this.register();
            if (r != null) {
               this.tip(r, 5 + this.random.nextInt(11));
            }
            break;
         case "JUMP":
            if (this.isPassenger()) {
               this.swing(InteractionHand.MAIN_HAND);
            } else {
               this.getJumpControl().jump();
            }
            break;
         case "SMASH":
            if (this.level() instanceof ServerLevel s) {
               s.sendParticles(
                  new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()),
                  this.getX(),
                  this.getY() + 0.3,
                  this.getZ(),
                  18,
                  0.3,
                  0.1,
                  0.3,
                  0.05
               );
            }

            this.level().playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.8F, 1.0F);
            break;
         case "TUNE":
            float[] notes = new float[]{0.7F, 0.9F, 1.05F, 1.2F};

            for (int i = 0; i < 3; i++) {
               this.level()
                  .playSound(
                     null,
                     this.blockPosition(),
                     (SoundEvent)SoundEvents.NOTE_BLOCK_HARP.value(),
                     SoundSource.RECORDS,
                     0.6F,
                     notes[this.random.nextInt(notes.length)]
                  );
            }
            break;
         case "FIREWORK":
            if (this.level() instanceof ServerLevel s) {
               s.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY() + 2.2, this.getZ(), 40, 0.5, 0.4, 0.5, 0.08);
            }

            this.level().playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 0.6F, 1.1F);
            break;
         case "CLINK":
            this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.7F, 1.7F);
            break;
         case "COIN":
            this.level().playSound(null, this.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 0.4F, 1.9F);
            break;
         case "THUNDER":
            this.level().playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.25F, 1.4F);
      }
   }

   private void behave(@Nullable CashRegisterEntity r) {
      Player bartender = this.level().getNearestPlayer(this, 10.0);
      if (bartender != null && this.tickCount % 5 == 0) {
         if (this.lookingAtMe(bartender)) {
            this.look(bartender.getEyePosition(), 30);
            this.gazeTicks += 5;
            if (this.gazeTicks == 25 && this.talk == null) {
               this.emote(this.random.nextBoolean() ? BarTalk.Emote.WAVE : BarTalk.Emote.NOD);
            }

            if (this.gazeTicks >= 60 && this.distanceToSqr(bartender) < 36.0) {
               this.bartenderSmallTalk(bartender);
               this.gazeTicks = -200;
            }
         } else if (this.gazeTicks > 0) {
            this.gazeTicks = 0;
         } else if (this.gazeTicks < 0) {
            this.gazeTicks += 5;
         }
      }

      if (this.lookTicks > 0) {
         this.lookTicks--;
         if (this.lookTarget != null) {
            this.getLookControl().setLookAt(this.lookTarget.x, this.lookTarget.y, this.lookTarget.z, 15.0F, 30.0F);
         }
      } else if (this.talk == null && --this.glanceTicks <= 0) {
         this.glanceTicks = 30 + this.random.nextInt(90);
         int roll = this.random.nextInt(10);
         List<BarPatron> nearby = this.level().getEntitiesOfClass(BarPatron.class, this.getBoundingBox().inflate(6.0), px -> px != this);
         BarPatron talker = null;

         for (BarPatron p : nearby) {
            if (!p.speech().isEmpty()) {
               talker = p;
               break;
            }
         }

         if (talker != null && roll < 7) {
            this.look(talker.getEyePosition(), 40 + this.random.nextInt(40));
         } else if (bartender == null || (this.state() == BarPatron.State.WAITING ? roll >= 7 : roll >= 3)) {
            if (!nearby.isEmpty() && roll < 6) {
               this.look(nearby.get(this.random.nextInt(nearby.size())).getEyePosition(), 30 + this.random.nextInt(50));
            } else {
               this.look(
                  this.getEyePosition().add(this.random.nextGaussian() * 4.0, this.random.nextGaussian() * 0.8, this.random.nextGaussian() * 4.0),
                  20 + this.random.nextInt(40)
               );
            }
         } else {
            this.look(bartender.getEyePosition(), 30 + this.random.nextInt(40));
         }
      }

      if (this.crouchBursts > 0 && --this.crouchTicks <= 0) {
         boolean down = !this.crouching();
         this.entityData.set(CROUCH, down);
         this.crouchTicks = 3 + this.random.nextInt(2);
         if (!down) {
            this.crouchBursts--;
         }
      } else if (this.crouchBursts == 0 && this.crouching()) {
         this.entityData.set(CROUCH, false);
      }

      if (!this.isPassenger() && this.getNavigation().isDone() && this.wander == null && --this.fidgetTicks <= 0) {
         this.fidgetTicks = 100 + this.random.nextInt(240);
         BlockPos anchor = this.spot != null ? this.spot : this.blockPosition();
         Vec3 to = Vec3.atBottomCenterOf(anchor).add(this.random.nextGaussian() * 0.35, 0.0, this.random.nextGaussian() * 0.35);
         if (r == null || !r.behindCounter(BlockPos.containing(to))) {
            this.getMoveControl().setWantedPosition(to.x, to.y, to.z, 0.6);
         }

         if (this.random.nextInt(5) == 0) {
            this.getJumpControl().jump();
         }
      }
   }

   private boolean lookingAtMe(Player player) {
      Vec3 view = player.getViewVector(1.0F);
      Vec3 to = this.getEyePosition().subtract(player.getEyePosition());
      double d = to.length();
      return d < 10.0 && d > 0.1 && view.dot(to.scale(1.0 / d)) > 0.985 && player.hasLineOfSight(this);
   }

   void look(Vec3 at, int ticks) {
      this.lookTarget = at;
      this.lookTicks = ticks;
      this.getLookControl().setLookAt(at.x, at.y, at.z, 15.0F, 30.0F);
   }

   @Nullable
   private Player nearestBartender(CashRegisterEntity r, double range) {
      Player best = null;
      double bestDistance = range * range;

      for (Player p : this.level().players()) {
         if (!p.isSpectator()) {
            double d = p.distanceToSqr(this);
            if (d < bestDistance && (r.behindCounter(p.blockPosition()) || best == null)) {
               best = p;
               bestDistance = d;
            }
         }
      }

      return best;
   }

   private void lookAtBartenderOrRegister(CashRegisterEntity r, @Nullable Player bartender) {
      if (this.lookTicks <= 0) {
         if (bartender != null) {
            this.look(bartender.getEyePosition(), 20);
         } else {
            this.look(Vec3.atCenterOf(r.getBlockPos()), 20);
         }
      }
   }

   private void social(@Nullable CashRegisterEntity r) {
      if (this.tickCount % 100 == 0) {
         boolean music = this.findJukebox() != null;
         if (music != this.dancing()) {
            this.entityData.set(DANCING, music);
            if (music && this.talk == null && this.random.nextInt(3) == 0) {
               this.say(pick(this.random, "Love this song!", "Turn it up!", "Ooh, I know this one!"));
            }
         }
      }

      if (this.talk == null) {
         if (this.tickCount % 40 == 0) {
            this.greetFriends(r);
         }

         if (this.tickCount % 20 == 0) {
            this.chatCooldown -= 20;
            this.startConversation(this.level().getEntitiesOfClass(BarPatron.class, this.getBoundingBox().inflate(6.0), px -> px != this));
         }

         if (this.talk == null && this.speechTicks <= 0 && --this.socialCooldown <= 0) {
            this.socialCooldown = 300 + this.random.nextInt(600);

            for (BarPatron p : this.level().getEntitiesOfClass(BarPatron.class, this.getBoundingBox().inflate(10.0), px -> px != this)) {
               if (p.state() == BarPatron.State.FIGHTING && this.random.nextBoolean()) {
                  this.say(pick(this.random, "Fight! Fight!", "Take it outside!", "Somebody stop them!", "Get him!"));
                  this.emote(BarTalk.Emote.FIST_PUMP);
                  if (!this.isPassenger()) {
                     this.getJumpControl().jump();
                  }

                  return;
               }
            }

            if (this.drunk() >= 4.5F && this.random.nextInt(3) == 0) {
               this.say(pick(this.random, "*hic*", "Is the floor moving?", "I'm not drunk, YOU'RE drunk.", "Who keeps moving the stools?"));
               this.emote(BarTalk.Emote.SHRUG);
               this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.4F, 1.5F);
            }
         }
      }
   }

   private boolean knows(@Nullable CashRegisterEntity r, BarPatron p) {
      return p.group.equals(this.group) || this.talkedWith.contains(p.getUUID()) || r != null && r.friends(this.skin(), p.skin());
   }

   private void startConversation(List<BarPatron> nearby) {
      if (this.talk == null && this.canChat() && this.chatCooldown <= 0) {
         this.chatCooldown = 100;
         CashRegisterEntity reg = this.register();
         double close = 12.25;
         double adjacent = 6.25;

         for (BarPatron p : nearby) {
            if (p.talk != null && !p.talk.script.event() && p.talk.listener == null && this.knows(reg, p) && p.distanceToSqr(this) < close && !p.talk.involves(this)
               )
             {
               p.talk.join(this);
               this.chatCooldown = 400;
               return;
            }
         }

         List<BarPatron> cast = new ArrayList<>();
         cast.add(this);

         for (BarPatron px : nearby) {
            if (cast.size() < 3 && px.group.equals(this.group) && px.distanceToSqr(this) < close && px.talk == null && px.canChat()) {
               cast.add(px);
            }
         }

         if (cast.size() == 1) {
            for (BarPatron pxx : nearby) {
               if (this.knows(reg, pxx) && pxx.distanceToSqr(this) < close && pxx.talk == null && pxx.canChat()) {
                  cast.add(pxx);
                  break;
               }
            }
         }

         boolean strangers = false;
         if (cast.size() == 1 && this.random.nextBoolean()) {
            for (BarPatron pxxx : nearby) {
               if (pxxx.distanceToSqr(this) < adjacent && pxxx.isPassenger() == this.isPassenger() && pxxx.talk == null && pxxx.canChat()) {
                  cast.add(pxxx);
                  strangers = true;
                  break;
               }
            }
         }

         if (cast.size() == 1) {
            CashRegisterEntity r = this.register();
            Player bartender = this.level().getNearestPlayer(this, 5.0);
            if (r != null && bartender != null && r.behindCounter(bartender.blockPosition()) && this.random.nextInt(3) == 0) {
               this.chatCooldown = 0;
               this.bartenderSmallTalk(bartender);
            }
         } else {
            BarTalk.Mood mood = this.mood();
            boolean reunion = !strangers
               && reg != null
               && cast.size() == 2
               && !this.talkedWith.contains(cast.get(1).getUUID())
               && reg.friends(this.skin(), cast.get(1).skin());
            BarTalk.Script script = BarTalk.pick(strangers ? BarTalk.INTROS : (reunion ? BarTalk.REUNIONS : BarTalk.CHATS), cast.size(), mood, this.random);
            if (script != null) {
               BarPatron[] roles = cast.subList(0, script.roles()).toArray(new BarPatron[0]);

               for (BarPatron a : roles) {
                  for (BarPatron b : roles) {
                     if (a != b) {
                        a.talkedWith.add(b.getUUID());
                        if (reg != null && !a.group.equals(b.group)) {
                           reg.befriend(a.skin(), b.skin());
                        }
                     }
                  }
               }

               BarTalk.Conversation c = new BarTalk.Conversation(script, roles, mood, null);

               for (BarPatron r : roles) {
                  r.talk = c;
                  r.socialCooldown = Math.max(r.socialCooldown, 200);
               }
            }
         }
      }
   }

   private void greetFriends(@Nullable CashRegisterEntity r) {
      if (r != null && this.speechTicks <= 0) {
         for (BarPatron p : this.level().getEntitiesOfClass(BarPatron.class, this.getBoundingBox().inflate(10.0), px -> px != this && !px.group.equals(this.group))) {
            if (!this.greeted.contains(p.getUUID())
               && r.friends(this.skin(), p.skin())
               && p.state() != BarPatron.State.ARRIVING
               && p.state() != BarPatron.State.LEAVING) {
               this.greeted.add(p.getUUID());
               p.greeted.add(this.getUUID());
               this.look(p.getEyePosition(), 40);
               this.emote(BarTalk.Emote.WAVE);
               this.say(pick(this.random, "Hey! You're here too!", "Look who it is!", "Over here, friend!", "Good to see you!"));
               p.look(this.getEyePosition(), 40);
               p.emoteLater(BarTalk.Emote.WAVE, 12);
               return;
            }
         }
      }
   }

   private void bartenderSmallTalk(Player bartender) {
      if (this.talk == null && this.canChat() && this.chatCooldown <= 200) {
         BarTalk.Mood mood = this.mood();
         BarTalk.Script script = BarTalk.pick(BarTalk.BARTENDER, 1, mood, this.random);
         if (script != null) {
            this.talk = new BarTalk.Conversation(script, new BarPatron[]{this}, mood, bartender);
         }
      }
   }

   private BarTalk.Mood mood() {
      Player bartender = this.level().getNearestPlayer(this, 16.0);
      boolean fight = !this.level().getEntitiesOfClass(BarPatron.class, this.getBoundingBox().inflate(12.0), px -> px.state() == BarPatron.State.FIGHTING).isEmpty();
      BarProducts.Product p = this.product();
      boolean smoking = this.getMainHandItem().getItem() instanceof CigarItem || this.ashtray != null;
      BarPatron other = null;
      double best = 196.0;

      for (BarPatron o : this.level()
         .getEntitiesOfClass(
            BarPatron.class,
            this.getBoundingBox().inflate(14.0),
            ox -> ox != this && !ox.group.equals(this.group) && ox.state() != BarPatron.State.ARRIVING && ox.state() != BarPatron.State.LEAVING
         )) {
         double d = o.distanceToSqr(this);
         if (d < best && d > 4.0) {
            best = d;
            other = o;
         }
      }

      CashRegisterEntity r = this.register();
      boolean busy = r != null && r.patronCount() >= 6;
      boolean slow = this.state() == BarPatron.State.WAITING && this.impatience() >= 1 || this.waitedLast > 2000 || r != null && r.reputation < 30;
      boolean quick = !slow && (this.waitedLast > 0 && this.waitedLast < 500 || r != null && r.reputation >= 70);
      return new BarTalk.Mood(
         this.dancing(),
         this.level().isNight(),
         this.level().isRaining(),
         this.drunk() >= 4.5F,
         fight,
         (!this.getMainHandItem().isEmpty() || this.ashtray != null) && p != null,
         smoking,
         p == null ? "drink" : p.name().getString(),
         bartender == null ? "the bartender" : bartender.getName().getString(),
         true,
         other == null ? null : SkinPainter.describe(other.skin()),
         busy,
         quick,
         slow,
         this.regular
      );
   }

   public boolean robber() {
      return this.robber;
   }

   public void becomeRobber() {
      this.robber = true;
      this.stander = false;
      this.ownSmoke = 0;
      this.entityData.set(SKIN, 2097152 | this.skin() & Medieval.SKIN_BIT);
      this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
   }

   private void startRobbery() {
      this.robPhase = 0;
      this.robTicks = 0;
      this.robBeaten = 0;
      this.setState(BarPatron.State.ROBBING);
   }

   private void robbing(@Nullable CashRegisterEntity r) {
      if (r == null) {
         this.leave(null);
      } else {
         if (this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
         }

         this.robTicks++;
         switch (this.robPhase) {
            case 0:
               if (this.walkTo(r.front(), 1.2) || this.failed() || this.robTicks > 400) {
                  this.robPhase = 1;
                  this.robTicks = 0;
                  this.say("Nobody move! Empty the register!");
                  this.level().playSound(null, this.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 0.8F, 0.6F);

                  for (Player near : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40.0))) {
                     near.displayClientMessage(Component.literal("A robber is holding up the register!").withStyle(ChatFormatting.RED), true);
                  }

                  for (BarPatron px : this.room(22.0)) {
                     if (px.state() != BarPatron.State.LEAVING) {
                        px.sayLater(
                           pick(this.random, "Is that a robber?!", "Oh no...", "Somebody do something!", "He's got a sword!"),
                           10 + this.random.nextInt(40)
                        );
                        px.emoteLater(BarTalk.Emote.SHAKE, 10 + this.random.nextInt(30));
                     }
                  }
               }
               break;
            case 1:
               this.getNavigation().stop();
               Vec3 till = Vec3.atCenterOf(r.getBlockPos());
               this.getLookControl().setLookAt(till.x, till.y, till.z);
               if (this.robTicks % 50 == 25) {
                  this.say(pick(this.random, "Hand over the cash! NOW!", "Don't be a hero!", "Fill the bag! All of it!", "I mean it! Empty the till!"));
               }

               if (this.robTicks == 60) {
                  this.rally();
               }

               boolean defenders = false;

               for (BarPatron p : this.room(30.0)) {
                  if (p.state() == BarPatron.State.FIGHTING && this.getUUID().equals(p.partner)) {
                     defenders = true;
                  }
               }

               if (this.robBeaten >= 6) {
                  this.flee(r, "Ow! Okay, okay! I'm out of here!", true);
               } else if (this.robTicks > 100 && !defenders && this.robTicks > 160) {
                  this.flee(r, "Tch. Not worth it. Forget it.", false);
               } else if (this.robTicks > 1600) {
                  this.flee(r, "This is going nowhere. I'm gone.", false);
               }
         }
      }
   }

   private List<BarPatron> room(double range) {
      return this.level().getEntitiesOfClass(BarPatron.class, this.getBoundingBox().inflate(range), p -> p != this && !p.robber && p.isAlive());
   }

   private void rally() {
      List<BarPatron> brave = new ArrayList<>();

      for (BarPatron p : this.room(24.0)) {
         BarPatron.State s = p.state();
         if (s == BarPatron.State.ENJOYING || s == BarPatron.State.TO_SEAT || s == BarPatron.State.WAITING || s == BarPatron.State.AWAIT_SHOT) {
            brave.add(p);
         }
      }

      brave.sort(Comparator.comparingDouble(px -> px.distanceToSqr(this)));
      int n = 0;

      for (BarPatron px : brave) {
         if (n >= 3) {
            px.sayLater(pick(this.random, "Get him!", "Go on, lads!", "Teach him a lesson!"), 10 + this.random.nextInt(30));
         } else {
            n++;
            if (px.isPassenger()) {
               px.stopRiding();
            }

            if (px.talk != null) {
               px.endTalk();
            }

            px.wander = null;
            px.dialogueWith = null;
            px.crouchBursts = 0;
            px.partner = this.getUUID();
            px.setState(BarPatron.State.FIGHTING);
            px.fightTicks = 900;
            px.hitCooldown = 15 + px.random.nextInt(25);
            px.say(pick(this.random, "Hey! Get away from the register!", "Not in this pub!", "Drop the sword!", "Get him!"));
         }
      }
   }

   private void flee(CashRegisterEntity r, String line, boolean beaten) {
      this.robPhase = 2;
      this.angry = true;
      this.jogger = true;
      this.say(line);
      this.emote(BarTalk.Emote.SHAKE);
      this.leave(r);
      if (beaten) {
         r.hurtReputation(-3);

         for (Player near : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40.0))) {
            near.displayClientMessage(Component.literal("The customers drove off the robber. Nothing was stolen.").withStyle(ChatFormatting.GREEN), true);
         }

         for (BarPatron p : this.room(30.0)) {
            if (p.state() != BarPatron.State.LEAVING) {
               p.sayLater(
                  pick(this.random, "Yeah! Don't come back!", "Never robbed here!", "Get lost!", "Hooray!", "Good riddance!"),
                  10 + this.random.nextInt(50)
               );
               p.emoteLater(this.random.nextBoolean() ? BarTalk.Emote.CLAP : BarTalk.Emote.FIST_PUMP, 10 + this.random.nextInt(40));
            }
         }
      }
   }

   private void defend(BarPatron thief) {
      if (thief.isAlive() && thief.state() != BarPatron.State.LEAVING && --this.fightTicks > 0) {
         this.getLookControl().setLookAt(thief, 30.0F, 30.0F);
         if (this.distanceToSqr(thief) > 4.0) {
            this.getNavigation().moveTo(thief, 1.15);
         } else {
            this.getNavigation().stop();
            if (this.hitCooldown <= 0) {
               this.hitCooldown = 12 + this.random.nextInt(14);
               this.swing(InteractionHand.MAIN_HAND);
               thief.knockback(0.3, this.getX() - thief.getX(), this.getZ() - thief.getZ());
               thief.robBeaten++;
               this.level().playSound(null, thief.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 0.8F, 1.0F);
               if (this.random.nextInt(3) == 0) {
                  this.say(pick(this.random, "Take that!", "Out!", "Not here, you don't!", "Drop it!"));
               }
            }
         }
      } else {
         this.partner = null;
         this.calmDown(
            thief.isAlive() && thief.state() != BarPatron.State.LEAVING
               ? "...where did he go?"
               : pick(this.random, "Good riddance!", "And stay out!", "That'll teach him.")
         );
      }
   }

   public static void brawl(BarPatron a, BarPatron b) {
      for (BarPatron p : new BarPatron[]{a, b}) {
         if (p.isPassenger()) {
            p.stopRiding();
         }

         p.wander = null;
         p.setState(BarPatron.State.FIGHTING);
         p.fightTicks = 140 + p.random.nextInt(80);
         p.hitCooldown = 20;
      }

      a.partner = b.getUUID();
      b.partner = a.getUUID();
      a.say(pick(a.random, "You spilled my drink!", "What'd you just say?!", "That's MY seat, pal!", "You looking at me?"));
      b.say(pick(b.random, "Oh, it's on!", "Say that again!", "Come at me!", "You wanna go?!"));
   }

   private void fight() {
      Entity other = this.partner != null && this.level() instanceof ServerLevel server ? server.getEntity(this.partner) : null;
      if (other instanceof BarPatron thief && thief.robber) {
         this.defend(thief);
         return;
      }

      if (other instanceof BarPatron foe && foe.state() == BarPatron.State.FIGHTING && --this.fightTicks > 0) {
         this.getLookControl().setLookAt(foe, 30.0F, 30.0F);
         if (this.distanceToSqr(foe) > 4.840000000000001) {
            this.getNavigation().moveTo(foe, 1.1);
            return;
         }

         this.getNavigation().stop();
         if (this.hitCooldown <= 0) {
            this.hitCooldown = 14 + this.random.nextInt(16);
            this.swing(InteractionHand.MAIN_HAND);
            if (this.random.nextFloat() < 0.35F) {
               this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.NEUTRAL, 0.8F, 0.9F);
               return;
            }

            if (foe.getHealth() > 8.0F) {
               foe.hurt(this.damageSources().mobAttack(this), 1.0F);
            }

            foe.knockback(0.35, this.getX() - foe.getX(), this.getZ() - foe.getZ());
            this.level().playSound(null, foe.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.NEUTRAL, 0.8F, 0.9F);
            if (this.random.nextInt(4) == 0) {
               this.say(pick(this.random, "Take that!", "Hold still!", "*hic* POW!", "My mother hits harder!"));
            }
         }

         return;
      }

      this.endFight(other instanceof BarPatron f ? f : null);
   }

   private void endFight(@Nullable BarPatron foe) {
      this.partner = null;
      this.setHealth(this.getMaxHealth());
      if (foe != null && foe.state() == BarPatron.State.FIGHTING) {
         foe.partner = null;
         foe.setHealth(foe.getMaxHealth());
         BarPatron loser = this.random.nextBoolean() ? this : foe;
         BarPatron winner = loser == this ? foe : this;
         loser.say(pick(this.random, "I'm outta here!", "This bar stinks anyway!", "You'll hear from my lawyer!"));
         loser.leaveWithGroup(loser.register());
         winner.calmDown(pick(this.random, "Sorry about that, folks.", "...where was I?", "Bartender, another!"));
      } else {
         this.calmDown("...where was I?");
      }
   }

   private void calmDown(String line) {
      this.say(line);
      if (this.sipsLeft <= 0 || this.getMainHandItem().isEmpty()) {
         this.lingerTicks = 0;
      }

      this.setState(BarPatron.State.TO_SEAT);
   }

   public boolean hurt(DamageSource source, float amount) {
      boolean hurt = super.hurt(source, amount);
      if (this.robber) {
         if (hurt && !this.level().isClientSide && source.getEntity() instanceof Player) {
            this.robBeaten += 2;
            this.say(pick(this.random, "Argh!", "Ow! Not the face!", "Get off me!"));
         }

         return hurt;
      } else {
         if (hurt
            && !this.level().isClientSide
            && source.getEntity() instanceof Player
            && this.state() != BarPatron.State.FIGHTING
            && this.state() != BarPatron.State.LEAVING) {
            this.angry = true;
            if (this.register() != null) {
               this.register().hurtReputation(3);
            }

            this.say(pick(this.random, "Ow! What was that for?!", "Hey! Watch it!", "I'm leaving, you psycho!"));

            for (BarPatron mate : this.mates()) {
               if (mate.state() != BarPatron.State.LEAVING && mate.state() != BarPatron.State.FIGHTING) {
                  mate.sayLater("Whoa, let's go.", 20);
               }
            }

            this.leaveWithGroup(this.register());
         }

         return hurt;
      }
   }

   public void showOut() {
      CashRegisterEntity r = this.register();
      if (!(this.drunk() >= 4.5F) && this.state() != BarPatron.State.FIGHTING) {
         this.say(pick(this.random, "What? I didn't do anything!", "Wow. Okay.", "I'm never coming back here."));
         this.angry = true;
         if (r != null) {
            r.hurtReputation(2);
         }
      } else {
         this.say(pick(this.random, "Fine, fine... *hic* I'm going.", "I wasn't THAT drunk...", "You're no fun."));
      }

      if (this.talk != null) {
         this.endTalk();
      }

      this.leave(r);
   }

   public void leave(@Nullable CashRegisterEntity r) {
      if (this.ashtray != null) {
         this.takeFromAshtray();
         this.releaseAshtray();
      }

      if (this.isPassenger()) {
         this.stopRiding();
      }

      if (r != null) {
         r.releaseSeat(this);
      }

      this.seat = null;
      this.spot = null;
      this.wander = null;
      this.leg = 0;
      this.legTicks = 0;
      this.bestDistance = Double.MAX_VALUE;
      this.setItemInHand(InteractionHand.MAIN_HAND, this.getMainHandItem().getItem() instanceof CigarItem ? this.getMainHandItem() : ItemStack.EMPTY);
      this.entityData.set(CROUCH, false);
      this.setState(BarPatron.State.LEAVING);
   }

   private void leaveWithGroup(@Nullable CashRegisterEntity r) {
      this.leave(r);

      for (BarPatron mate : this.mates()) {
         BarPatron.State s = mate.state();
         if (s != BarPatron.State.LEAVING && s != BarPatron.State.FIGHTING && (s != BarPatron.State.ENJOYING || mate.sipsLeft <= 0)) {
            mate.sayLater(pick(this.random, "Wait up!", "Yeah, I'm beat too.", "Night, everyone!"), 20 + this.random.nextInt(30));
            mate.leave(r);
         }
      }
   }

   private void leaving() {
      BlockPos[] route = new BlockPos[]{this.inside, this.outside, this.far};
      if (route[0] == null || route[1] == null) {
         if (++this.legTicks > 400 || this.walkTo(this.blockPosition().offset(8, 0, 8), 2.0)) {
            this.gone();
         }
      } else if (this.leg < route.length && route[this.leg] != null) {
         if (this.near(this.door, 2.5)) {
            this.openDoor();
         }

         if (this.followLeg(route[this.leg], 1.1)) {
            if (this.leg == 1) {
               this.closeDoor();
            }

            this.leg++;
         }
      } else {
         this.closeDoor();
         this.gone();
      }
   }

   private void gone() {
      CashRegisterEntity r = this.register();
      if (r != null && !this.angry && (this.visits > 0 || this.rapport >= 3)) {
         r.remember(this);
      }

      if (r != null) {
         r.forget(this);
      }

      this.discard();
   }

   private boolean near(BlockPos p, double reach) {
      return p != null && this.distanceToSqr(Vec3.atBottomCenterOf(p)) <= reach * reach;
   }

   private double speedNow() {
      return this.pace * (this.drunk() >= 4.5F ? 0.85 : 1.0) * (this.state() == BarPatron.State.LEAVING && this.jogger ? 1.35 : 1.0);
   }

   private boolean failed() {
      boolean f = this.navFailed;
      this.navFailed = false;
      return f;
   }

   private boolean walkTo(BlockPos target, double reach) {
      if (target == null) {
         return true;
      } else {
         Vec3 c = Vec3.atBottomCenterOf(target);
         double d = this.distanceToSqr(c);
         if (d <= reach * reach) {
            this.getNavigation().stop();
            this.navTarget = null;
            this.navTicks = 0;
            return true;
         } else {
            if (this.isPassenger()) {
               this.stopRiding();
            }

            if (!target.equals(this.navTarget)) {
               this.navTarget = target;
               this.navBest = d;
               this.navTicks = 0;
               this.navTotal = 0;
               this.navPath = null;
               this.navFailed = false;
               this.gridMode = false;
               this.gridTried = false;
               this.gridReplans = 0;
               this.gridRoute.clear();
               this.repath(c);
               Path first = this.getNavigation().getPath();
               if (first == null || !first.canReach()) {
                  this.startGrid(target);
               }
            } else if (!this.gridMode && this.getNavigation().isDone()) {
               this.repath(c);
            }

            if (this.gridMode) {
               this.gridWalk(target, c);
            }

            this.navTotal++;
            if (this.tickCount % 10 == 0) {
               Path path = this.getNavigation().getPath();
               if (path != null && path == this.navPath && path.getNextNodeIndex() > this.navNode && path.canReach() && this.navTotal < 900) {
                  this.navTicks = 0;
               }

               this.navPath = path;
               this.navNode = path == null ? 0 : path.getNextNodeIndex();
            }

            if (d < this.navBest - 0.2) {
               this.navBest = d;
               this.navTicks = 0;
            } else {
               this.navTicks++;
               if (this.navTicks == 30) {
                  if (!this.gridMode && !this.gridTried) {
                     this.startGrid(target);
                  } else if (this.gridMode) {
                     this.startGrid(target);
                  } else {
                     this.repath(c);
                  }
               }

               if (this.navTicks == 50) {
                  this.getNavigation().stop();
                  this.getMoveControl()
                     .setWantedPosition(
                        this.getX() + this.random.nextGaussian() * 0.6,
                        this.getY(),
                        this.getZ() + this.random.nextGaussian() * 0.6,
                        this.speedNow() * 0.8
                     );
               }

               if (this.navTicks == 60 && !this.gridMode) {
                  this.repath(c);
               }

               if (this.navTicks == 75) {
                  this.getJumpControl().jump();
               }

               if (this.navTicks >= 100) {
                  this.navFailed = true;
                  this.navTicks = 0;
                  this.navBest = d;
               }
            }

            return false;
         }
      }
   }

   private void startGrid(BlockPos target) {
      this.gridTried = true;
      CashRegisterEntity r = this.register();
      BlockPos start = this.blockPosition();
      if (Double.isNaN(BarWorker.floorAt(this.level(), start))) {
         BlockPos below = start.below();
         if (Double.isNaN(BarWorker.floorAt(this.level(), below))) {
            return;
         }

         start = below;
      }

      List<BlockPos> route = FloorRoutes.route(
         this.level(),
         start,
         target,
         q -> r == null || !r.behindCounter(q) && !(this.level().getBlockState(q.below()).getBlock() instanceof SeatBlock) && !r.seats.contains(q.below()),
         4000
      );
      if (route != null && !route.isEmpty()) {
         this.getNavigation().stop();
         this.gridRoute.clear();
         this.gridRoute.addAll(route);
         this.gridMode = true;
         this.navTicks = 0;
      } else {
         this.gridMode = false;
      }
   }

   private void gridWalk(BlockPos target, Vec3 c) {
      BlockPos next;
      for (next = this.gridRoute.peek(); next != null; this.navTicks = 0) {
         double dx = (double)next.getX() + 0.5 - this.getX();
         double dz = (double)next.getZ() + 0.5 - this.getZ();
         if (!(dx * dx + dz * dz < 0.12249999999999998) || !(Math.abs((double)next.getY() - this.getY()) < 1.3)) {
            break;
         }

         this.gridRoute.poll();
         next = this.gridRoute.peek();
      }

      if (next != null || !(this.distanceToSqr(c.x, this.getY(), c.z) > 2.25)) {
         Vec3 aim = next == null ? c : Vec3.atBottomCenterOf(next);
         this.getMoveControl().setWantedPosition(aim.x, aim.y, aim.z, this.speedNow());
      } else if (++this.gridReplans > 3) {
         this.gridMode = false;
         this.gridReplans = 0;
      } else {
         this.startGrid(target);
      }
   }

   private void repath(Vec3 c) {
      this.getNavigation().moveTo(c.x, c.y, c.z, this.speedNow());
      CashRegisterEntity r = this.register();
      Path path = this.getNavigation().getPath();
      if (r != null && path != null && this.navTarget != null && !r.behindCounter(this.navTarget) && !r.behindCounter(this.blockPosition())) {
         for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
            if (r.behindCounter(path.getNode(i).asBlockPos())) {
               this.getNavigation().stop();
               this.navFailed = true;
               return;
            }
         }
      }
   }

   private boolean followLeg(BlockPos target, double reach) {
      if (this.door != null
         && this.near(this.door, 3.0)
         && this.level().getBlockState(this.door).getBlock() instanceof DoorBlock
         && (Boolean)this.level().getBlockState(this.door).getValue(DoorBlock.OPEN)) {
         Vec3 c = Vec3.atBottomCenterOf(target);
         if (this.distanceToSqr(c) <= reach * reach) {
            this.getNavigation().stop();
            this.legTicks = 0;
            this.bestDistance = Double.MAX_VALUE;
            return true;
         }

         this.getNavigation().stop();
         this.getMoveControl().setWantedPosition(c.x, c.y, c.z, this.speedNow());
      } else if (this.walkTo(target, reach)) {
         this.legTicks = 0;
         this.bestDistance = Double.MAX_VALUE;
         return true;
      }

      double d = this.distanceToSqr(Vec3.atBottomCenterOf(target));
      if (d < this.bestDistance - 0.25) {
         this.bestDistance = d;
         this.legTicks = 0;
      } else if (++this.legTicks > 120) {
         this.moveTo((double)target.getX() + 0.5, (double)target.getY(), (double)target.getZ() + 0.5, this.getYRot(), this.getXRot());
         this.getNavigation().stop();
         this.legTicks = 0;
         this.bestDistance = Double.MAX_VALUE;
         return true;
      }

      return false;
   }

   private void openDoor() {
      if (this.door != null) {
         BlockState state = this.level().getBlockState(this.door);
         if (state.getBlock() instanceof DoorBlock block) {
            if (!(Boolean)state.getValue(DoorBlock.OPEN)) {
               block.setOpen(this, this.level(), state, this.door, true);
            }

            this.openedDoor = true;
         }
      }
   }

   private void closeDoor() {
      if (this.door != null && this.openedDoor) {
         this.openedDoor = false;
         BlockState state = this.level().getBlockState(this.door);
         boolean another = !this.level()
            .getEntitiesOfClass(
               BarPatron.class,
               new AABB(this.door).inflate(4.0, 1.0, 4.0),
               p -> p != this && (p.state() == BarPatron.State.ARRIVING || p.state() == BarPatron.State.LEAVING)
            )
            .isEmpty();
         if (state.getBlock() instanceof DoorBlock block && (Boolean)state.getValue(DoorBlock.OPEN) && !another) {
            block.setOpen(this, this.level(), state, this.door, false);
         }
      }
   }

   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (!this.level().isClientSide && this.order() >= 0 && this.handOver(player, player.getItemInHand(hand))) {
         return InteractionResult.SUCCESS;
      } else {
         if (!this.level().isClientSide) {
            this.look(player.getEyePosition(), 60);
         }

         BarPatron.State s = this.state();
         if (s == BarPatron.State.ROBBING) {
            if (!this.level().isClientSide) {
               this.say(pick(this.random, "Back off!", "Hands where I can see them!", "Don't try anything!"));
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
         } else if (s != BarPatron.State.ARRIVING && s != BarPatron.State.LEAVING && s != BarPatron.State.FIGHTING) {
            if ((s == BarPatron.State.WAITING || s == BarPatron.State.AWAIT_SHOT) && !player.isShiftKeyDown()) {
               if (!this.level().isClientSide && player instanceof ServerPlayer sp) {
                  CashRegisterEntity r = this.register();
                  if (this.order() < 0 && r != null) {
                     this.chooseOrder(r);
                  }

                  if (!this.announced) {
                     this.announced = true;
                     this.say(this.orderLine());
                  }

                  sp.openMenu(
                     new SimpleMenuProvider(
                        (id, inventory, p) -> new PatronMenu(id, inventory, this), Component.literal(this.regular ? "Customer · regular" : "Customer")
                     ),
                     buf -> buf.writeVarInt(this.getId())
                  );
               }

               return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else {
               if (!this.level().isClientSide && player instanceof ServerPlayer sp) {
                  if (this.dialogueWith != null && !this.dialogueWith.equals(player.getUUID())) {
                     this.say("One at a time!");
                     return InteractionResult.CONSUME;
                  }

                  int node = this.openingNode(player);
                  this.startDialogue(player);
                  this.say(BarDialogue.fill(BarDialogue.get(node).line(), this.drinkName(), this.mood().other(), player.getName().getString()));
                  sp.openMenu(
                     new SimpleMenuProvider((id, inventory, p) -> new DialogueMenu(id, inventory, this, node), Component.literal("Conversation")),
                     buf -> {
                        buf.writeVarInt(this.getId());
                        buf.writeVarInt(node);
                     }
                  );
               }

               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
         } else {
            if (!this.level().isClientSide) {
               this.say(pick(this.random, "Not now!", "Hang on a sec.", "Can't talk!"));
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
         }
      }
   }

   private void clientEffects() {
      if (this.getMainHandItem().getItem() instanceof CigarItem) {
         float yaw = this.yHeadRot * (float) (Math.PI / 180.0);
         double fx = -Math.sin((double)yaw);
         double fz = Math.cos((double)yaw);
         if ((int)(this.level().getGameTime() - (long)((Integer)this.entityData.get(SIP)).intValue()) == 28) {
            for (int i = 0; i < 5; i++) {
               this.level()
                  .addParticle(
                     (ParticleOptions)Drunkard.CIGAR_SMOKE.get(),
                     this.getX() + fx * 0.3,
                     this.getEyeY() - 0.12,
                     this.getZ() + fz * 0.3,
                     fx * 0.03 + this.random.nextGaussian() * 0.006,
                     0.015,
                     fz * 0.03 + this.random.nextGaussian() * 0.006
                  );
            }
         }
      }
   }

   public void die(DamageSource source) {
      super.die(source);
      if (this.level() instanceof ServerLevel s) {
         s.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
      }

      CashRegisterEntity r = this.register();
      if (r != null) {
         r.forget(this);
      }
   }

   private static void put(CompoundTag tag, String key, @Nullable BlockPos pos) {
      if (pos != null) {
         tag.putLong(key, pos.asLong());
      }
   }

   @Nullable
   private static BlockPos get(CompoundTag tag, String key) {
      return tag.contains(key) ? BlockPos.of(tag.getLong(key)) : null;
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      put(tag, "Register", this.register);
      put(tag, "Door", this.door);
      put(tag, "Inside", this.inside);
      put(tag, "Outside", this.outside);
      put(tag, "Far", this.far);
      put(tag, "Seat", this.seat);
      put(tag, "Spot", this.spot);
      tag.putUUID("Group", this.group);
      tag.putInt("Skin", this.skin());
      tag.putString("State", this.state().name());
      tag.putInt("Order", this.order());
      tag.putBoolean("Shot", this.wantsShot());
      tag.putFloat("Drunk", this.drunk());
      tag.putFloat("Limit", this.limit);
      tag.putInt("Visits", this.visits);
      tag.putInt("MaxVisits", this.maxVisits);
      tag.putInt("SipsLeft", this.sipsLeft);
      tag.putInt("Patience", this.patience);
      tag.putInt("Linger", this.lingerTicks);
      tag.putInt("Leg", this.leg);
      tag.putDouble("Pace", this.pace);
      tag.putBoolean("Jogger", this.jogger);
      tag.putLong("WaitingSince", this.waitingSince);
      tag.putBoolean("OrderedHere", this.orderedHere);
      tag.putInt("Rapport", this.rapport);
      tag.putBoolean("Stander", this.stander);
      tag.putBoolean("Regular", this.regular);
      tag.putInt("Favourite", this.favourite);
      tag.putBoolean("Angry", this.angry);
      tag.putBoolean("Usual", this.usual);
      tag.putInt("OwnSmoke", this.ownSmoke);
      tag.putBoolean("SmokedOwn", this.smokedOwn);
      tag.putBoolean("Robber", this.robber);
      tag.putInt("RobPhase", this.robPhase);
      tag.putInt("RobBeaten", this.robBeaten);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.register = get(tag, "Register");
      this.door = get(tag, "Door");
      this.inside = get(tag, "Inside");
      this.outside = get(tag, "Outside");
      this.far = get(tag, "Far");
      this.seat = get(tag, "Seat");
      this.spot = get(tag, "Spot");
      if (tag.hasUUID("Group")) {
         this.group = tag.getUUID("Group");
      }

      this.entityData.set(SKIN, tag.getBoolean("Robber") ? 2097152 | tag.getInt("Skin") & Medieval.SKIN_BIT : Medieval.keep(tag.getInt("Skin"), 65536));

      BarPatron.State state;
      try {
         state = BarPatron.State.valueOf(tag.getString("State"));
      } catch (IllegalArgumentException var4) {
         state = BarPatron.State.WAITING;
      }

      if (state == BarPatron.State.FIGHTING || state == BarPatron.State.ENJOYING) {
         state = BarPatron.State.TO_SEAT;
      }

      this.entityData.set(STATE, state.ordinal());
      this.entityData.set(ORDER, BarProducts.get(tag.getInt("Order")) == null ? -1 : tag.getInt("Order"));
      this.entityData.set(SHOT, tag.getBoolean("Shot"));
      this.setDrunk(tag.getFloat("Drunk"));
      this.limit = tag.contains("Limit") ? tag.getFloat("Limit") : 6.0F;
      this.visits = tag.getInt("Visits");
      this.maxVisits = tag.contains("MaxVisits") ? tag.getInt("MaxVisits") : 3;
      this.sipsLeft = tag.getInt("SipsLeft");
      this.patience = tag.contains("Patience") ? tag.getInt("Patience") : 4800;
      this.lingerTicks = tag.getInt("Linger");
      this.leg = tag.getInt("Leg");
      this.pace = tag.contains("Pace") ? tag.getDouble("Pace") : 1.0;
      this.jogger = tag.getBoolean("Jogger");
      this.waitingSince = tag.getLong("WaitingSince");
      this.orderedHere = tag.getBoolean("OrderedHere");
      this.rapport = tag.getInt("Rapport");
      this.stander = false;
      this.regular = tag.getBoolean("Regular");
      this.favourite = tag.contains("Favourite") ? tag.getInt("Favourite") : -1;
      this.angry = tag.getBoolean("Angry");
      this.usual = tag.getBoolean("Usual");
      this.ownSmoke = tag.getInt("OwnSmoke");
      this.smokedOwn = tag.getBoolean("SmokedOwn");
      this.robber = tag.getBoolean("Robber");
      this.robPhase = tag.getInt("RobPhase");
      this.robBeaten = tag.getInt("RobBeaten");
      if (this.robber) {
         this.entityData.set(SKIN, 2097152 | this.skin() & Medieval.SKIN_BIT);
      }
   }

   public void setSipsLeft(int sips) {
      this.sipsLeft = sips;
      this.sipTimer = 1;
   }

   public int sipsLeft() {
      return this.sipsLeft;
   }

   public void endLinger(boolean again) {
      this.lingerTicks = 0;
      this.stay = again;
      this.mingleCooldown = 6000;
   }

   public void markOrdered() {
      this.orderedHere = true;
   }

   public boolean serveFromWorker(ItemStack drink) {
      CashRegisterEntity r = this.register();
      if (r != null && this.product() != null && (this.state() == BarPatron.State.WAITING || this.state() == BarPatron.State.AWAIT_SHOT)) {
         this.receive(r, drink, null);
         return true;
      } else {
         return false;
      }
   }

   public int rapport() {
      return this.rapport;
   }

   public int closingTicks() {
      return this.closingTicks;
   }

   public String navDebug() {
      return "grid="
         + this.gridMode
         + " route="
         + this.gridRoute.size()
         + " next="
         + this.gridRoute.peek()
         + " tried="
         + this.gridTried
         + " navTicks="
         + this.navTicks
         + " target="
         + this.navTarget
         + " navDone="
         + this.getNavigation().isDone()
         + " approach="
         + this.seatApproach
         + " avoid="
         + this.avoid.size();
   }

   private String drinkName() {
      BarProducts.Product p = this.product();
      return p == null ? "drink" : p.name().getString();
   }

   public int openingNode(Player bartender) {
      if (this.rapport <= -2) {
         return BarDialogue.index("cold");
      } else if (this.drunk() >= 4.5F) {
         return BarDialogue.index("drunk");
      } else if (this.state() == BarPatron.State.ENJOYING && this.waitedLast > 2000 && this.sipsLeft > 0) {
         return BarDialogue.index("slow");
      } else if (!this.regular && this.rapport < 3) {
         if (this.state() == BarPatron.State.ENJOYING && this.sipsLeft <= 0 && this.getMainHandItem().isEmpty() && this.ashtray == null) {
            return BarDialogue.index("leaving");
         } else {
            List<String> options = new ArrayList<>(BarDialogue.TOPICS);
            if (this.level().isRaining()) {
               options.add("weather");
               options.add("weather");
            }

            if (this.getMainHandItem().getItem() instanceof CigarItem || this.ashtray != null) {
               options.add("smoke");
            } else if (!this.getMainHandItem().isEmpty()) {
               options.add("drink");
               options.add("complain");
            } else {
               options.add("recommend");
            }

            if (this.mood().other() != null) {
               options.add("gossip");
            }

            if (!this.dancing()) {
               options.add("music");
            }

            if (this.level().isNight()) {
               options.add("late");
            }

            if (options.size() > 1) {
               options.remove(this.lastTopic);
            }

            String pick = options.get(this.random.nextInt(options.size()));
            this.lastTopic = pick;
            return BarDialogue.index(pick);
         }
      } else {
         return BarDialogue.index("regular");
      }
   }

   public void startDialogue(Player bartender) {
      this.dialogueWith = bartender.getUUID();
      if (this.talk != null) {
         this.endTalk();
      }

      this.getNavigation().stop();
   }

   public void endDialogue(Player bartender) {
      if (bartender.getUUID().equals(this.dialogueWith)) {
         this.dialogueWith = null;
      }
   }

   public int answer(Player bartender, BarDialogue.Node node, int choice) {
      if (node.end()) {
         return -1;
      } else {
         BarDialogue.Node next;
         if (choice == 90) {
            next = BarDialogue.get(BarDialogue.index("silence"));
         } else {
            if (choice < 0 || choice >= node.options().size()) {
               return -1;
            }

            BarDialogue.Option o = node.options().get(choice);
            this.consequences(bartender, o.effect());
            if (o.next() == null) {
               return -1;
            }

            next = BarDialogue.get(BarDialogue.index(o.next()));
         }

         this.say(BarDialogue.fill(next.line(), this.drinkName(), this.mood().other(), bartender.getName().getString()));
         this.look(bartender.getEyePosition(), 40);
         if (next.end()) {
            this.emote(next.line().contains("!") ? BarTalk.Emote.LAUGH : BarTalk.Emote.NOD);
         }

         return BarDialogue.index(next.id());
      }
   }

   private void consequences(Player bartender, String effects) {
      CashRegisterEntity r = this.register();

      for (String e : effects.split(",")) {
         e = e.trim();
         if (!e.isEmpty()) {
            if (e.startsWith("RAPPORT")) {
               int n = Integer.parseInt(e.substring(7));
               this.rapport = Math.max(-5, Math.min(10, this.rapport + n));
               if (n >= 2 || n < 0) {
                  bartender.displayClientMessage(
                     Component.literal("They'll remember that.").withStyle(new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY}), true
                  );
               }
            } else if (e.startsWith("TIP")) {
               if (r != null) {
                  this.tip(r, Integer.parseInt(e.substring(3)));
               }
            } else if (e.startsWith("REP")) {
               if (r != null) {
                  r.hurtReputation(-Integer.parseInt(e.substring(3)));
               }
            } else {
               switch (e) {
                  case "FREE":
                     if (r != null && this.lastPrice > 0) {
                        r.refund(this.lastPrice);
                        bartender.displayClientMessage(Component.literal("On the house (-$" + this.lastPrice + ")"), true);
                        this.lastPrice = 0;
                     } else {
                        this.freeNext = true;
                     }
                     break;
                  case "ORDER":
                     this.stay = true;
                     this.lingerTicks = Math.min(this.lingerTicks, 40);
                     break;
                  case "USUAL":
                     this.stay = true;
                     this.forceUsual = true;
                     if (this.favourite < 0) {
                        this.favourite = this.order();
                     }

                     this.lingerTicks = Math.min(this.lingerTicks, 40);
                     break;
                  case "CALM":
                     this.limit = Math.min(this.limit, this.drunk());
                     this.stay = false;
                     this.lingerTicks = Math.min(this.lingerTicks, 200);
                     break;
                  case "WATER":
                     this.setDrunk(this.drunk() - 2.0F);
                     break;
                  case "STAY":
                     this.stay = true;
                     this.lingerTicks += 1200;
                     this.maxVisits++;
                     break;
                  case "PATIENCE":
                     this.patience = Math.max(this.patience, 4800);
                     break;
                  case "LEAVE_HAPPY":
                     if (this.talk != null) {
                        this.endTalk();
                     }

                     this.leave(r);
                     break;
                  case "LEAVE_ANGRY":
                     this.angry = true;
                     if (this.talk != null) {
                        this.endTalk();
                     }

                     this.leave(r);
               }
            }
         }
      }
   }

   public int visits() {
      return this.visits;
   }

   public void setStander(boolean stander) {
      this.stander = stander;
   }

   public void setVisits(int n) {
      this.visits = n;
   }

   public void losePatience() {
      this.patience = 1;
   }

   public void chatNow() {
      this.chatCooldown = 0;
      this.socialCooldown = 0;
   }

   public boolean talking() {
      return this.talk != null;
   }

   public void joinGroup(BarPatron other) {
      this.group = other.group;
   }

   public void goHome() {
      this.leaveWithGroup(this.register());
   }

   public boolean behindTheBar() {
      CashRegisterEntity r = this.register();
      return r != null && r.behindCounter(this.blockPosition());
   }

   public static enum State {
      ARRIVING,
      WAITING,
      AWAIT_SHOT,
      TO_SEAT,
      ENJOYING,
      FIGHTING,
      LEAVING,
      ROBBING;
   }
}
