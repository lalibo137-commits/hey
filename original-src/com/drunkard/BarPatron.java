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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class BarPatron extends PathfinderMob {
   public static final int ROBBER_SKIN = 2097152;
   public static final float DRUNK_FACE = 4.5F;
   public static final int SKINS = 65536;
   public static final int PATIENCE = 4800;
   public static final int SHOT_PATIENCE = 3000;
   public static final int SIP_TICKS = 40;
   private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Integer> ORDER = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Boolean> SHOT = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135035_);
   private static final EntityDataAccessor<Integer> SIP = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Float> DRUNK = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135029_);
   private static final EntityDataAccessor<String> SPEECH = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135030_);
   private static final EntityDataAccessor<Boolean> DANCING = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135035_);
   private static final EntityDataAccessor<Boolean> CROUCH = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135035_);
   private static final EntityDataAccessor<Integer> EMOTE = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Integer> EMOTE_AT = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Boolean> MOUTH = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135035_);
   private static final EntityDataAccessor<String> NAME = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135030_);
   private static final EntityDataAccessor<Integer> IMPATIENCE = SynchedEntityData.m_135353_(BarPatron.class, EntityDataSerializers.f_135028_);
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
      this.m_21530_();
      this.m_21553_(false);
      if (this.m_21573_() instanceof GroundPathNavigation ground) {
         ground.m_26477_(true);
         ground.m_148214_(true);
         ground.m_26529_(3.0F);
      }

      this.m_21409_(EquipmentSlot.MAINHAND, 0.0F);
   }

   public static Builder createAttributes() {
      return Mob.m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.3).m_22268_(Attributes.f_22277_, 48.0);
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(SKIN, 0);
      this.f_19804_.m_135372_(STATE, BarPatron.State.ARRIVING.ordinal());
      this.f_19804_.m_135372_(ORDER, -1);
      this.f_19804_.m_135372_(SHOT, false);
      this.f_19804_.m_135372_(SIP, -1000);
      this.f_19804_.m_135372_(DRUNK, 0.0F);
      this.f_19804_.m_135372_(SPEECH, "");
      this.f_19804_.m_135372_(DANCING, false);
      this.f_19804_.m_135372_(CROUCH, false);
      this.f_19804_.m_135372_(EMOTE, 0);
      this.f_19804_.m_135372_(EMOTE_AT, 0);
      this.f_19804_.m_135372_(MOUTH, false);
      this.f_19804_.m_135372_(NAME, "");
      this.f_19804_.m_135372_(IMPATIENCE, 0);
   }

   protected void m_8099_() {
      this.f_21345_.m_25352_(0, new FloatGoal(this));
   }

   public SpawnGroupData m_6518_(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag
   ) {
      this.f_19804_.m_135381_(SKIN, this.f_19796_.m_188503_(65536));
      this.stander = false;
      int smokeRoll = this.f_19796_.m_188503_(100);
      this.ownSmoke = smokeRoll < 11 ? 1 : (smokeRoll < 17 ? 2 : (smokeRoll < 20 ? 3 : 0));
      this.limit = 4.0F + this.f_19796_.m_188501_() * 6.0F;
      this.maxVisits = 2 + this.f_19796_.m_188503_(4);
      this.pace = 0.95 + this.f_19796_.m_188500_() * 0.3;
      this.jogger = this.f_19796_.m_188503_(4) == 0;
      return super.m_6518_(level, difficulty, reason, data, tag);
   }

   public boolean m_6785_(double distance) {
      return false;
   }

   protected boolean m_8028_() {
      return false;
   }

   protected void m_6138_() {
      if (this.state() != BarPatron.State.ARRIVING && this.state() != BarPatron.State.LEAVING && !this.headingToSeat()) {
         super.m_6138_();
      }
   }

   public boolean m_6094_() {
      return this.state() != BarPatron.State.ARRIVING && this.state() != BarPatron.State.LEAVING && !this.m_20159_() && !this.headingToSeat();
   }

   private boolean headingToSeat() {
      return this.seat != null && !this.m_20159_() && this.wander == null;
   }

   public double m_6049_() {
      return -0.35;
   }

   public int skin() {
      return (Integer)this.f_19804_.m_135370_(SKIN);
   }

   public boolean slim() {
      return (this.skin() & 1) == 1;
   }

   public String name() {
      return (String)this.f_19804_.m_135370_(NAME);
   }

   public boolean regular() {
      return this.regular;
   }

   public int favourite() {
      return this.favourite;
   }

   public int impatience() {
      return (Integer)this.f_19804_.m_135370_(IMPATIENCE);
   }

   public Component m_7755_() {
      return (Component)(this.name().isEmpty() ? super.m_7755_() : Component.m_237113_(this.name()));
   }

   public void becomeRegular(CashRegisterEntity.Regular r) {
      this.regular = true;
      this.favourite = r.favourite();
      this.maxVisits++;
      this.f_19804_.m_135381_(NAME, r.name());
      this.f_19804_.m_135381_(SKIN, Math.floorMod(r.skin(), 65536));
   }

   private String bartenderName() {
      Player p = this.m_9236_().m_45930_(this, 16.0);
      return p == null ? "friend" : p.m_7755_().getString();
   }

   public BarPatron.State state() {
      return BarPatron.State.values()[Math.max(0, Math.min(BarPatron.State.values().length - 1, (Integer)this.f_19804_.m_135370_(STATE)))];
   }

   public int order() {
      return (Integer)this.f_19804_.m_135370_(ORDER);
   }

   public BarProducts.Product product() {
      return BarProducts.get(this.order());
   }

   public boolean wantsShot() {
      return (Boolean)this.f_19804_.m_135370_(SHOT);
   }

   public int sip() {
      return Math.max(0, 40 - (int)(this.m_9236_().m_46467_() - (long)((Integer)this.f_19804_.m_135370_(SIP)).intValue()));
   }

   public float sipAge(float partial) {
      return (float)((int)this.m_9236_().m_46467_() - (Integer)this.f_19804_.m_135370_(SIP)) + partial;
   }

   public float drunk() {
      return (Float)this.f_19804_.m_135370_(DRUNK);
   }

   public String speech() {
      return (String)this.f_19804_.m_135370_(SPEECH);
   }

   public boolean dancing() {
      return (Boolean)this.f_19804_.m_135370_(DANCING);
   }

   public boolean crouching() {
      return (Boolean)this.f_19804_.m_135370_(CROUCH);
   }

   public BarTalk.Emote emote() {
      return BarTalk.Emote.values()[Math.max(0, Math.min(BarTalk.Emote.values().length - 1, (Integer)this.f_19804_.m_135370_(EMOTE)))];
   }

   public float emoteAge(float partial) {
      return (float)((int)this.m_9236_().m_46467_() - (Integer)this.f_19804_.m_135370_(EMOTE_AT)) + partial;
   }

   public boolean cigarInMouth() {
      return (Boolean)this.f_19804_.m_135370_(MOUTH);
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
      this.f_19804_.m_135381_(STATE, state.ordinal());
      this.walkTicks = 0;
   }

   public void setDrunk(float drinks) {
      this.f_19804_.m_135381_(DRUNK, Math.max(0.0F, drinks));
   }

   public void say(String line) {
      this.f_19804_.m_135381_(SPEECH, line);
      this.speechTicks = 50 + line.length() * 2;
   }

   void sayLater(String line, int ticks) {
      this.pendingLine = line;
      this.pendingTicks = ticks;
   }

   public void emote(BarTalk.Emote e) {
      if (e != BarTalk.Emote.NONE) {
         this.f_19804_.m_135381_(EMOTE, e.ordinal());
         this.f_19804_.m_135381_(EMOTE_AT, (int)this.m_9236_().m_46467_());
      }
   }

   void emoteLater(BarTalk.Emote e, int ticks) {
      this.pendingEmote = e;
      this.pendingEmoteTicks = ticks;
   }

   boolean emoteBusy() {
      BarTalk.Emote e = this.emote();
      return e != BarTalk.Emote.NONE && this.m_9236_().m_46467_() - (long)((Integer)this.f_19804_.m_135370_(EMOTE_AT)).intValue() < (long)e.ticks;
   }

   public boolean queuing() {
      return this.state() == BarPatron.State.WAITING && !this.orderedHere && this.seat == null;
   }

   void lookAtHead(Entity other) {
      this.look(other.m_146892_(), 12);
   }

   boolean canChat() {
      if (this.dialogueWith != null) {
         return false;
      } else {
         BarPatron.State s = this.state();
         if (s != BarPatron.State.ENJOYING && s != BarPatron.State.WAITING && s != BarPatron.State.AWAIT_SHOT) {
            return false;
         } else {
            return s == BarPatron.State.WAITING && !this.orderedHere ? this.m_20159_() : this.m_20159_() || this.m_21573_().m_26571_() && this.wander == null;
         }
      }
   }

   private void endTalk() {
      if (this.talk != null) {
         for (BarPatron p : this.talk.cast) {
            p.talk = null;
            p.chatCooldown = 300 + p.f_19796_.m_188503_(700);
         }

         this.talk = null;
      }
   }

   private static String pick(RandomSource random, String... lines) {
      return lines[random.m_188503_(lines.length)];
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
      this.f_19804_.m_135381_(ORDER, -1);
      this.waitingSince = this.m_9236_().m_46467_();
      this.patience = (int)(4800.0F * (r == null ? 1.0F : (0.8F + (float)r.reputation / 250.0F) * (1.0F + 0.15F * (float)r.level(BarUpgrades.Upgrade.COMFORT))));
      this.announced = false;
      this.complained = false;
      this.callCooldown = 40 + this.f_19796_.m_188503_(60);
   }

   @Nullable
   public CashRegisterEntity register() {
      return this.register != null && this.m_9236_().m_46749_(this.register) && this.m_9236_().m_7702_(this.register) instanceof CashRegisterEntity r
         ? r
         : null;
   }

   private List<BarPatron> mates() {
      return this.m_9236_().m_6443_(BarPatron.class, this.m_20191_().m_82400_(16.0), p -> p != this && p.group.equals(this.group) && p.m_6084_());
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
         if (fav == null || !stocked.contains(fav) || !this.forceUsual && !(this.f_19796_.m_188501_() < 0.75F)) {
            this.usual = false;
            BarProducts.Product product = stocked.get(this.f_19796_.m_188503_(stocked.size()));
            this.f_19804_.m_135381_(ORDER, BarProducts.ALL.indexOf(product));
            this.f_19804_.m_135381_(SHOT, product.shots() && this.f_19796_.m_188501_() < 0.75F);
            return true;
         } else {
            this.forceUsual = false;
            this.f_19804_.m_135381_(ORDER, this.favourite);
            this.f_19804_.m_135381_(SHOT, fav.shots() && this.f_19796_.m_188501_() < 0.75F);
            this.usual = true;
            return true;
         }
      }
   }

   public void setOrder(int product, boolean shot) {
      this.f_19804_.m_135381_(ORDER, product);
      this.f_19804_.m_135381_(SHOT, shot && BarProducts.get(product) != null && BarProducts.get(product).shots());
   }

   public String orderLine() {
      BarProducts.Product p = this.product();
      if (p == null) {
         return "";
      } else {
         String name = p.name().getString();
         if (this.usual) {
            return pick(this.f_19796_, "The usual, please!", "My usual, " + this.bartenderName() + "!", "You know what I like.");
         } else {
            return switch (p.kind()) {
               case SPIRIT -> this.wantsShot()
               ? pick(this.f_19796_, "A shot of " + name + ", please", "Pour me a " + name + " shot", "Shot of " + name + "!")
               : "A bottle of " + name + " for the table";
               case BEER -> pick(this.f_19796_, "One " + name + ", please", "Got any " + name + "?", "A cold " + name + " for me");
               case WINE -> pick(this.f_19796_, "A bottle of " + name + ", please", "We'll share a bottle of " + name);
               case COCKTAIL -> pick(
               this.f_19796_, "One " + name + ", please", "Can you make " + ("AEIOUaeiou".indexOf(name.charAt(0)) >= 0 ? "an " : "a ") + name + "?"
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
         this.say(pick(this.f_19796_, "In a shot glass, please!", "Line it up!"));
         bartender.m_5661_(Component.m_237113_("Set a shot glass down near them and pour the " + p.name().getString()), true);
         return true;
      } else {
         ItemStack given = ItemStack.f_41583_;
         if (bartender.m_150110_().f_35937_) {
            given = p.icon();
         } else {
            for (int i = 0; i < bartender.m_150109_().m_6643_(); i++) {
               ItemStack stack = bartender.m_150109_().m_8020_(i);
               if (BarProducts.matches(p, stack)) {
                  given = stack.m_41620_(1);
                  break;
               }
            }
         }

         if (!given.m_41619_()) {
            this.receive(r, given, bartender);
            return true;
         } else {
            bartender.m_5661_(
               Component.m_237113_(
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
      if (r != null && p != null && !held.m_41619_()) {
         boolean shotGlass = (this.state() == BarPatron.State.WAITING || this.state() == BarPatron.State.AWAIT_SHOT)
            && this.wantsShot()
            && ShotGlassItem.filled(held)
            && ShotGlassItem.type(held).equals(p.id());
         boolean whole = this.state() == BarPatron.State.WAITING && !this.wantsShot() && BarProducts.matches(p, held);
         if (!shotGlass && !whole) {
            return false;
         } else {
            ItemStack given = bartender.m_150110_().f_35937_ ? held.m_255036_(1) : held.m_41620_(1);
            this.receive(r, given, bartender);
            return true;
         }
      } else {
         return false;
      }
   }

   public void refuse() {
      if (this.state() == BarPatron.State.WAITING) {
         this.say(pick(this.f_19796_, "Fine, something else then.", "Out of it? Huh.", "Okay, what DO you have?"));
         this.f_19804_.m_135381_(ORDER, -1);
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

      if (held.m_41720_() instanceof CigarItem) {
         CigarItem.ignite(held, this.m_9236_().m_46467_());
      }

      this.f_19804_.m_135381_(MOUTH, held.m_150930_((Item)Drunkard.CIGAR.get()) && this.f_19796_.m_188499_());
      this.m_21008_(InteractionHand.MAIN_HAND, held);
      this.sipsLeft = p.sips(this.wantsShot());
      this.sipTimer = 40 + this.f_19796_.m_188503_(60);
      int price = r.price(p, this.wantsShot());
      if (this.freeNext) {
         price = 0;
         this.freeNext = false;
      }

      this.lastPrice = price;
      r.pay(price);
      this.waitedLast = (int)(this.m_9236_().m_46467_() - this.waitingSince);
      r.served(price, this.waitedLast);
      this.orderedHere = true;
      this.say(
         this.waitedLast < 400
            ? pick(this.f_19796_, "That was quick!", "Now that's service!", "Cheers!", "You're the best.")
            : (
               this.waitedLast > 2400
                  ? pick(this.f_19796_, "Finally...", "About time.", "Took you long enough.")
                  : pick(this.f_19796_, "Cheers!", "Much obliged.", "Now we're talking.", "Put it on my tab... kidding!")
            )
      );
      if (bartender != null) {
         this.look(bartender.m_146892_(), 40);
      }

      Component receipt = Component.m_237113_("+$" + price + " · " + (this.wantsShot() ? "shot of " : "") + p.name().getString());
      if (bartender != null) {
         bartender.m_5661_(receipt, true);
      } else {
         for (Player near : this.m_9236_().m_45976_(Player.class, this.m_20191_().m_82400_(12.0))) {
            near.m_5661_(receipt, true);
         }
      }

      this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_11871_, SoundSource.NEUTRAL, 0.4F, 1.6F);
      this.setState(this.m_20159_() ? BarPatron.State.ENJOYING : BarPatron.State.TO_SEAT);
   }

   public void m_8119_() {
      super.m_8119_();
      if (this.m_9236_().f_46443_) {
         this.clientEffects();
      }
   }

   protected void m_8024_() {
      super.m_8024_();
      if (this.speechTicks > 0 && --this.speechTicks == 0) {
         this.f_19804_.m_135381_(SPEECH, "");
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
         Player partner = this.m_9236_().m_46003_(this.dialogueWith);
         if (partner != null && !(partner.m_20280_(this) > 100.0)) {
            CashRegisterEntity here = this.register();
            if (here != null) {
               here.track(this);
            }

            this.m_21573_().m_26573_();
            this.look(partner.m_146892_(), 10);
            return;
         }

         this.dialogueWith = null;
      }

      if (this.f_19797_ % 1200 == 0) {
         this.avoid.clear();
      }

      if (this.f_19797_ % 20 == 0) {
         int max = this.state() == BarPatron.State.AWAIT_SHOT ? 3000 : 4800;
         int level = this.state() != BarPatron.State.WAITING && this.state() != BarPatron.State.AWAIT_SHOT
            ? 0
            : (this.patience < max / 4 ? 2 : (this.patience < max / 2 ? 1 : 0));
         if (level != this.impatience()) {
            this.f_19804_.m_135381_(IMPATIENCE, level);
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
         && !this.m_20159_()
         && !this.headingToSeat()
         && r.behindCounter(this.m_20183_())) {
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

         if (this.drunk() >= 4.5F && !this.m_20159_() && !this.m_21573_().m_26571_() && this.f_19796_.m_188503_(25) == 0) {
            this.m_20256_(this.m_20184_().m_82520_(this.f_19796_.m_188583_() * 0.12, 0.0, this.f_19796_.m_188583_() * 0.12));
         }
      }
   }

   private boolean wrapUp(CashRegisterEntity r) {
      if (this.closingTicks < 0) {
         boolean served = this.state() == BarPatron.State.ENJOYING || this.state() == BarPatron.State.TO_SEAT;
         this.closingTicks = served ? 100 + this.f_19796_.m_188503_(600) : 20 + this.f_19796_.m_188503_(200);
         this.stay = false;
         if (this.f_19796_.m_188503_(3) == 0) {
            this.sayLater(
               pick(this.f_19796_, "Last call, huh?", "Guess that's closing time.", "Already? Aw.", "Time to wrap up, I suppose.", "Is it that time already?"),
               10 + this.f_19796_.m_188503_(80)
            );
         }
      }

      if (this.sipsLeft > 0 && this.sipTimer > 70) {
         this.sipTimer = 30 + this.f_19796_.m_188503_(40);
      }

      if (--this.closingTicks > 0) {
         return false;
      } else {
         if (this.talk != null) {
            this.endTalk();
         }

         this.say(
            this.drunk() >= 4.5F
               ? pick(this.f_19796_, "Night, everybody! *hic*", "Best... night... ever.")
               : pick(this.f_19796_, "Night!", "Thanks for tonight!", "See you next time.", "Right, home time.", "Night, " + this.bartenderName() + "!")
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
                     this.m_9236_().m_5594_(null, this.door, (SoundEvent)SoundEvents.f_12210_.m_203334_(), SoundSource.BLOCKS, 0.5F, 1.6F);
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
         this.say(pick(this.f_19796_, "Evening, " + bt + "!", "Hey " + bt + "! Missed me?", "Honey, I'm home!", "My favourite pub!"));
      } else if (this.f_19796_.m_188503_(3) == 0) {
         this.say(pick(this.f_19796_, "Evening!", "Nice place.", "Is this the pub everyone talks about?", "Smells like a good night.", "Room for one more?"));
      }
   }

   private void waiting(CashRegisterEntity r) {
      if (r != null) {
         if (this.order() < 0 && !this.chooseOrder(r)) {
            this.say("Nothing on the menu? I'm out.");
            this.leave(r);
         } else if (--this.patience <= 0) {
            this.say(pick(this.f_19796_, "Forget it, I'll go somewhere else.", "Worst service in town!", "I've been waiting forever!"));
            this.angry = true;
            r.walkedOut();
            this.leaveWithGroup(r);
         } else {
            Player bartender = this.nearestBartender(r, 40.0);
            boolean mustSit = r.tallBar();
            if ((!this.orderedHere || mustSit) && this.seat == null && (!this.stander || mustSit) && !this.m_20159_() && this.walkTicks % 30 == 1) {
               BlockPos anchor = r.m_58899_();

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
               if (this.m_20159_() || this.goSit(r)) {
                  this.callForService(bartender);
               }
            } else if (mustSit && !this.m_20159_()) {
               if (this.spot == null && this.walkTicks % 30 == 1) {
                  this.spot = r.claimStandingSpot(this, r.front(), this.f_19796_, 3.0);
               }

               if (this.spot != null) {
                  this.walkTo(this.spot, 1.2);
               }

               if (!this.seatWaitSaid && this.walkTicks > 60) {
                  this.seatWaitSaid = true;
                  this.say(pick(this.f_19796_, "I'll grab a seat when one frees up.", "Full house, huh? I'll wait.", "Anyone leaving soon?"));
               }
            } else if (!this.orderedHere && r.isHead(this)) {
               if (this.walkTo(r.front(), 1.4)) {
                  this.lookAtBartenderOrRegister(r, bartender);
                  if (!this.announced) {
                     this.announced = true;
                     this.say(this.orderLine());
                     this.m_6674_(InteractionHand.MAIN_HAND);
                  }

                  this.callForService(bartender);
               }
            } else if (this.m_20159_()) {
               this.callForService(bartender);
            } else {
               if (this.orderedHere && bartender != null && r.behindCounter(bartender.m_20183_()) && this.walkTicks % 60 == 1) {
                  BlockPos near = r.claimSpot(this, bartender.m_20183_());
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
         this.callCooldown = 240 + this.f_19796_.m_188503_(300);
         if (bartender != null) {
            this.say(this.announced ? pick(this.f_19796_, "Bartender!", "Excuse me!", "Hey, over here!", "Still waiting!", "Yo! Bartender!") : this.orderLine());
            this.announced = true;
            this.look(bartender.m_146892_(), 60);
            if (this.f_19796_.m_188499_()) {
               this.crouchBursts = 2 + this.f_19796_.m_188503_(2);
            } else {
               this.emote(BarTalk.Emote.WAVE);
            }
         }
      }
   }

   private void findPlace(CashRegisterEntity r) {
      if (this.seat == null && this.spot == null && this.walkTicks % 30 == 1) {
         BlockPos anchor = r.m_58899_();

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
            this.spot = r.claimStandingSpot(this, r.front(), this.f_19796_);
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
      } else if (this.f_19797_ % 10 == 0) {
         BlockPos found = null;
         String type = this.product() == null ? "" : this.product().id();
         int reach = this.m_20159_() ? 2 : 3;

         for (BlockPos p : BlockPos.m_121940_(this.m_20183_().m_7918_(-reach, -2, -reach), this.m_20183_().m_7918_(reach, 2, reach))) {
            BlockEntity var8 = this.m_9236_().m_7702_(p);
            if (var8 instanceof ShotGlassEntity) {
               ShotGlassEntity glass = (ShotGlassEntity)var8;
               if (ShotGlassItem.filled(glass.glass)) {
                  if (ShotGlassItem.type(glass.glass).equals(type)) {
                     found = p.m_7949_();
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
            this.look(Vec3.m_82512_(found), 20);
            if (!this.m_20159_() && this.m_20238_(Vec3.m_82512_(found)) > 6.760000000000001) {
               this.walkTo(found, 2.2);
            } else {
               if (this.m_9236_().m_7702_(found) instanceof ShotGlassEntity glass) {
                  ItemStack shot = glass.glass;
                  glass.glass = ItemStack.f_41583_;
                  this.m_9236_().m_7471_(found, false);
                  this.m_6674_(InteractionHand.MAIN_HAND);
                  if (r != null) {
                     this.receive(r, shot, null);
                  }
               }
            }
         }
      }
   }

   private void toSeat(CashRegisterEntity r) {
      if (!this.m_20159_() && r != null) {
         if (this.seat == null && this.walkTicks % 20 == 1) {
            BlockPos anchor = this.m_20183_();

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
                  this.spot = r.claimStandingSpot(this, r.front(), this.f_19796_);
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
      if (r != null && this.seat == null && !this.stander && this.wander == null && !this.m_20159_() && this.walkTicks % 100 == 50) {
         BlockPos free = r.claimSeat(this, this.m_20183_(), 24.0, this.avoid);
         if (free != null) {
            this.seat = free;
            this.spot = null;
         }
      }

      if (this.wander != null) {
         this.mingle(r);
      } else if (this.seat != null && !this.m_20159_()) {
         this.goSit(r);
      } else if (this.spot != null && !this.m_20159_() && !this.near(this.spot, 1.6) && !this.walkTo(this.spot, 1.2) && this.failed()) {
         this.spot = null;
      }

      if (this.sipsLeft <= 0 || p == null && !this.smokingOwn) {
         if (!this.m_21205_().m_41619_() || this.ashtray != null) {
            this.finishDrink(r);
         } else if (this.ownSmoke != 0 && !this.smokedOwn && this.wander == null && (r == null || r.open) && this.f_19796_.m_188503_(50) == 0) {
            this.lightOwn();
         } else if (this.wander == null && --this.lingerTicks <= 0) {
            this.decide(r);
         }
      } else {
         this.restSmoke(r);
         if (--this.sipTimer <= 0) {
            this.sipTimer = this.chug > 0 ? 14 : (this.smokingOwn ? 50 + this.f_19796_.m_188503_(60) : 120 + this.f_19796_.m_188503_(160));
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
         || !this.stay && (this.drunk() >= this.limit || this.visits >= this.maxVisits || mateLeft || this.f_19796_.m_188501_() < 0.25F);
      this.stay = false;
      if (done) {
         String bt = this.bartenderName();
         this.say(
            this.drunk() >= 4.5F
               ? pick(this.f_19796_, "*hic* Best bar in town!", "I love you guys... *hic*", "Somebody call me a cab.")
               : (
                  this.regular
                     ? pick(this.f_19796_, "Same time tomorrow, " + bt + "!", "Night, " + bt + "!", "Put it on my tab... I'll be back!")
                     : pick(
                        this.f_19796_, "I'm good, see ya!", "Nice place. Night!", "That hit the spot.", "Alright, we're heading out!", "Thanks, " + bt + "!"
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
         this.say(pick(this.f_19796_, "Bartender! Another round!", "Hit me again!", "One more won't hurt.", "Same again? Nah, something new."));
         Player bartender = r == null ? null : this.nearestBartender(r, 40.0);
         if (bartender != null) {
            this.look(bartender.m_146892_(), 60);
            this.m_6674_(InteractionHand.MAIN_HAND);
         }
      }
   }

   private void maybeMingle(@Nullable CashRegisterEntity r) {
      if (r != null && this.wander == null && this.state() == BarPatron.State.ENJOYING && --this.mingleCooldown <= 0 && this.talk == null) {
         this.mingleCooldown = 600 + this.f_19796_.m_188503_(900);
         if (!(this.f_19796_.m_188501_() > (this.dancing() ? 0.45F : 0.25F))) {
            BlockPos near = null;
            String line = null;
            BlockPos jukebox = this.dancing() ? this.findJukebox() : null;
            if (jukebox != null) {
               near = jukebox;
               line = pick(this.f_19796_, "I gotta dance to this!", "Come on, dance with me!");
            } else if (this.seat == null && !this.m_20159_()) {
               for (BarPatron mate : this.mates()) {
                  if (mate.m_20280_(this) > 4.0) {
                     near = mate.m_20183_();
                     line = pick(this.f_19796_, "Scoot over!", "Hey, what'd I miss?", "Mind if I join?");
                     break;
                  }
               }
            }

            if (near != null) {
               BlockPos target = r.claimStandingSpot(this, near, this.f_19796_);
               if (target != null) {
                  if (this.ashtray != null) {
                     this.takeFromAshtray();
                     this.releaseAshtray();
                  }

                  this.wander = target;
                  this.wanderArrived = false;
                  this.wanderTicks = 200 + this.f_19796_.m_188503_(300);
                  if (this.m_20159_()) {
                     this.m_8127_();
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
         if (this.f_19797_ % 80 == 0) {
            BarPatron nearest = null;
            double best = 16.0;

            for (BarPatron p : this.m_9236_().m_6443_(BarPatron.class, this.m_20191_().m_82400_(4.0), px -> px != this)) {
               double d = p.m_20280_(this);
               if (d < best) {
                  best = d;
                  nearest = p;
               }
            }

            if (nearest != null) {
               this.look(nearest.m_146892_(), 60);
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
            if (this.besideSeat(this.seat, 3.5) && SeatEntity.sit(this.m_9236_(), this.seat, this, this.register)) {
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
      } else if (SeatEntity.sit(this.m_9236_(), this.seat, this, this.register)) {
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
      BlockState state = this.m_9236_().m_8055_(seat);
      List<Direction> order = new ArrayList<>(Plane.HORIZONTAL.m_122557_().toList());
      Direction first = null;
      Direction counter = this.register == null ? null : SeatEntity.counterSide(this.m_9236_(), seat, this.register);
      if (counter != null) {
         first = counter.m_122424_();
      } else if (state.m_60734_() instanceof StairBlock) {
         first = ((Direction)state.m_61143_(StairBlock.f_56841_)).m_122424_();
      }

      if (first != null) {
         order.remove(first);
         order.add(0, first);
      }

      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;

      for (Direction d : order) {
         for (int dy : new int[]{0, -1, -2, -3, 1}) {
            BlockPos q = seat.m_121945_(d).m_6630_(dy);
            if (r != null && r.standable(q) && !r.behindCounter(q)) {
               if (d == first) {
                  return q;
               }

               double dist = q.m_123331_(this.m_20183_());
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
               BlockPos q = seat.m_121945_(d).m_121945_(d.m_122427_()).m_6630_(dyx);
               if (r.standable(q) && !r.behindCounter(q)) {
                  return q;
               }
            }
         }
      }

      if (best == null && r != null) {
         for (Direction d : order) {
            for (int dyxx : new int[]{0, -1, -2, -3}) {
               BlockPos q = seat.m_121945_(d).m_6630_(dyxx);
               if (r.standable(q)) {
                  return q;
               }
            }
         }
      }

      return best != null ? best : seat;
   }

   private boolean besideSeat(BlockPos seat, double reach) {
      double dx = this.m_20185_() - ((double)seat.m_123341_() + 0.5);
      double dz = this.m_20189_() - ((double)seat.m_123343_() + 0.5);
      double dy = (double)seat.m_123342_() - this.m_20186_();
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
      for (BlockPos p : BlockPos.m_121940_(this.m_20183_().m_7918_(-8, -2, -8), this.m_20183_().m_7918_(8, 3, 8))) {
         if (this.m_9236_().m_8055_(p).m_60734_() instanceof JukeboxBlock && (Boolean)this.m_9236_().m_8055_(p).m_61143_(JukeboxBlock.f_54254_)) {
            return p.m_7949_();
         }
      }

      return null;
   }

   private void lightOwn() {
      ItemStack held = new ItemStack(
         this.ownSmoke == 1 ? (ItemLike)Drunkard.CIGARETTE.get() : (this.ownSmoke == 2 ? (ItemLike)Drunkard.CIGAR.get() : (ItemLike)Drunkard.PIPE.get())
      );
      CigarItem.ignite(held, this.m_9236_().m_46467_());
      this.m_21008_(InteractionHand.MAIN_HAND, held);
      this.f_19804_.m_135381_(MOUTH, this.ownSmoke != 1 && this.f_19796_.m_188499_());
      this.smokingOwn = true;
      this.smokedOwn = true;
      this.sipsLeft = (this.ownSmoke == 1 ? 4 : 6) + this.f_19796_.m_188503_(4);
      this.sipTimer = 50 + this.f_19796_.m_188503_(70);
      this.m_6674_(InteractionHand.MAIN_HAND);
      this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_11942_, SoundSource.NEUTRAL, 0.25F, 1.2F);
      if (this.f_19796_.m_188503_(3) == 0) {
         this.say(pick(this.f_19796_, "Mind if I light up?", "*lights up*", "Time for a smoke."));
      }
   }

   private void sipNow(@Nullable BarProducts.Product p) {
      this.f_19804_.m_135381_(SIP, (int)this.m_9236_().m_46467_());
      if (p != null && !p.smoke()) {
         this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_11911_, SoundSource.NEUTRAL, 0.5F, 0.9F + this.f_19796_.m_188501_() * 0.2F);
         this.setDrunk(this.drunk() + p.drinksPerSip());
      } else {
         this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_11937_, SoundSource.NEUTRAL, 0.08F, 1.8F);
      }

      this.sipsLeft--;
   }

   private void finishDrink(CashRegisterEntity r) {
      if (this.ashtray != null) {
         if (this.m_9236_().m_7702_(this.ashtray) instanceof CigarHolderEntity holder && holder.cigar.m_41720_() instanceof CigarItem) {
            holder.cigar = ItemStack.f_41583_;
            holder.sync();
         }

         this.releaseAshtray();
      }

      this.chug = 0;
      if (this.smokingOwn) {
         this.smokingOwn = false;
         this.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
         this.f_19804_.m_135381_(MOUTH, false);
         this.lingerTicks = 200 + this.f_19796_.m_188503_(600);
      } else {
         ItemStack held = this.m_21205_();
         ItemStack empty = held.m_41720_() instanceof ShotGlassItem
            ? new ItemStack((ItemLike)Drunkard.SHOT_GLASS.get())
            : (
               held.m_41720_() instanceof WineItem && this.product() != null && this.product().kind() == BarProducts.Kind.BEER
                  ? new ItemStack((ItemLike)Drunkard.BEER_MUG.get())
                  : (
                     held.m_41720_() instanceof WineItem
                        ? new ItemStack(Items.f_42590_)
                        : (held.m_41720_() instanceof CocktailItem ? glassOf(held) : ItemStack.f_41583_)
                  )
            );
         this.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
         this.f_19804_.m_135381_(MOUTH, false);
         this.visits++;
         BlockPos top = this.counterTop();
         if (!empty.m_41619_() && (top == null || !this.placeOnCounter(top, empty)) && r != null) {
            r.returnDish(empty);
         }

         BarProducts.Product p = this.product();
         if (r != null && p != null && this.tipEligible) {
            float fast = (this.waitedLast < 400 ? 0.2F : (this.waitedLast < 1200 ? 0.1F : 0.0F)) + Math.max(-0.1F, Math.min(0.3F, (float)this.rapport * 0.05F));
            if (this.f_19796_.m_188501_() < 0.3F + fast * 2.0F + (this.regular ? 0.2F : 0.0F) + (float)r.reputation / 400.0F) {
               this.tip(
                  r,
                  Math.max(
                     1,
                     Math.round(
                        (float)p.price(this.wantsShot())
                           * (fast + 0.05F + this.f_19796_.m_188501_() * 0.12F + (this.regular ? 0.1F : 0.0F) + (this.drunk() >= 4.5F ? 0.1F : 0.0F))
                     )
                  )
               );
            }
         }

         if (this.drunk() >= this.limit + 3.0F) {
            this.say("*hic* ...where's the door?");
            this.leaveWithGroup(r);
         } else {
            this.lingerTicks = this.stay ? 60 + this.f_19796_.m_188503_(100) : 600 + this.f_19796_.m_188503_(1800);
            this.socialCooldown = 60 + this.f_19796_.m_188503_(100);
         }
      }
   }

   @Nullable
   private BlockPos counterTop() {
      BlockPos at = this.m_20159_() && this.seat != null ? this.seat : this.m_20183_();
      if (this.register == null) {
         return null;
      } else {
         Direction side = SeatEntity.counterSide(this.m_9236_(), at, this.register);
         if (side == null) {
            return null;
         } else {
            BlockPos top = at.m_121945_(side).m_7494_();
            return this.m_9236_().m_8055_(top).m_247087_() ? top : null;
         }
      }
   }

   private boolean placeOnCounter(BlockPos top, ItemStack empty) {
      Direction facing = Direction.m_122364_((double)this.m_146908_()).m_122424_();
      if (empty.m_41720_() instanceof ShotGlassItem) {
         BlockState state = (BlockState)((Block)Drunkard.PLACED_SHOT_GLASS.get()).m_49966_().m_61124_(ShotGlassBlock.FACING, facing);
         if (!state.m_60710_(this.m_9236_(), top)) {
            return false;
         }

         this.m_9236_().m_7731_(top, state, 3);
         if (this.m_9236_().m_7702_(top) instanceof ShotGlassEntity glass) {
            glass.glass = empty;
            glass.sync();
         }
      } else {
         BlockState statex = (BlockState)((Block)Drunkard.PLACED_BOTTLE.get()).m_49966_().m_61124_(PlacedBottleBlock.FACING, facing);
         if (!statex.m_60710_(this.m_9236_(), top)) {
            return false;
         }

         this.m_9236_().m_7731_(top, statex, 3);
         if (this.m_9236_().m_7702_(top) instanceof BottleEntity bottle) {
            bottle.bottle = empty;
            bottle.sync();
         }
      }

      this.m_9236_().m_5594_(null, top, SoundEvents.f_11986_, SoundSource.NEUTRAL, 0.5F, 1.3F);
      this.m_6674_(InteractionHand.MAIN_HAND);
      return true;
   }

   private static ItemStack glassOf(ItemStack cocktail) {
      if (cocktail.m_150930_((Item)Drunkard.MARTINI.get())) {
         return new ItemStack((ItemLike)Drunkard.MARTINI_GLASS.get());
      } else if (cocktail.m_150930_((Item)Drunkard.MARGARITA.get())) {
         return new ItemStack((ItemLike)Drunkard.MARGARITA_GLASS.get());
      } else {
         return !cocktail.m_150930_((Item)Drunkard.OLD_FASHIONED.get()) && !(cocktail.m_41720_() instanceof OnTheRocksItem)
            ? new ItemStack((ItemLike)Drunkard.HIGHBALL_GLASS.get())
            : new ItemStack((ItemLike)Drunkard.ROCKS_GLASS.get());
      }
   }

   private void tip(CashRegisterEntity r, int dollars) {
      r.pay(dollars);
      r.tipped(dollars);
      this.m_6674_(InteractionHand.MAIN_HAND);
      this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_11871_, SoundSource.NEUTRAL, 0.25F, 1.9F);

      for (Player near : this.m_9236_().m_45976_(Player.class, this.m_20191_().m_82400_(12.0))) {
         near.m_5661_(Component.m_237113_("+$" + dollars + " tip"), true);
      }
   }

   private void restSmoke(@Nullable CashRegisterEntity r) {
      if (this.ashtrayTicks > 0 && --this.ashtrayTicks == 0 && this.ashtray != null) {
         this.putInAshtray();
      } else {
         ItemStack held = this.m_21205_();
         if (this.ashtray == null
            && held.m_41720_() instanceof CigarItem
            && CigarItem.lit(held)
            && this.wander == null
            && this.sip() <= 0
            && this.f_19797_ % 40 == 0) {
            if (this.m_20159_() || this.m_21573_().m_26571_()) {
               if (!this.cigarInMouth() || this.f_19796_.m_188503_(4) == 0) {
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
      Vec3 hand = this.m_146892_().m_82520_(0.0, -0.5, 0.0);

      for (BlockPos p : BlockPos.m_121940_(this.m_20183_().m_7918_(-2, -1, -2), this.m_20183_().m_7918_(2, 2, 2))) {
         BlockEntity d = this.m_9236_().m_7702_(p);
         if (d instanceof CigarHolderEntity) {
            CigarHolderEntity holder = (CigarHolderEntity)d;
            if (holder.cigar.m_41619_() && (r == null || r.free(p, this))) {
               double dx = Vec3.m_82512_(p).m_82557_(hand);
               if (dx < bestDistance) {
                  best = p.m_7949_();
                  bestDistance = dx;
               }
            }
         }
      }

      return best;
   }

   private void putInAshtray() {
      if (this.ashtray != null) {
         ItemStack held = this.m_21205_();
         if (this.m_9236_().m_7702_(this.ashtray) instanceof CigarHolderEntity holder && holder.cigar.m_41619_() && held.m_41720_() instanceof CigarItem) {
            holder.cigar = held.m_255036_(1);
            if (holder.cigar.m_41782_()) {
               holder.cigar.m_41783_().m_128473_("InMouth");
            }

            holder.sync();
            this.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
            this.f_19804_.m_135381_(MOUTH, false);
            this.look(Vec3.m_82512_(this.ashtray), 20);
            this.m_6674_(InteractionHand.MAIN_HAND);
            return;
         }

         this.releaseAshtray();
      }
   }

   private boolean takeFromAshtray() {
      if (this.ashtray == null) {
         return false;
      } else if (this.m_21205_().m_41720_() instanceof CigarItem) {
         return true;
      } else {
         if (this.m_9236_().m_7702_(this.ashtray) instanceof CigarHolderEntity holder
            && holder.cigar.m_41720_() instanceof CigarItem
            && CigarItem.lit(holder.cigar)) {
            this.m_21008_(InteractionHand.MAIN_HAND, holder.cigar);
            holder.cigar = ItemStack.f_41583_;
            holder.sync();
            this.look(Vec3.m_82512_(this.ashtray), 15);
            this.m_6674_(InteractionHand.MAIN_HAND);
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
         List<BarPatron> nearby = this.m_9236_().m_6443_(BarPatron.class, this.m_20191_().m_82400_(12.0), px -> px != this && px.m_6084_());
         List<BarPatron> partners = new ArrayList<>();

         for (BarPatron p : nearby) {
            if (p.talk == null && p.canChat() && p.m_20280_(this) < 16.0) {
               partners.add(p);
            }
         }

         partners.sort(Comparator.comparingDouble(px -> (double)(px.group.equals(this.group) ? 0 : 50) + px.m_20280_(this)));
         List<BarPatron> crowd = new ArrayList<>();

         for (BarPatron px : nearby) {
            BarPatron.State s = px.state();
            if (s != BarPatron.State.ARRIVING && s != BarPatron.State.LEAVING && s != BarPatron.State.FIGHTING && s != BarPatron.State.ROBBING) {
               crowd.add(px);
            }
         }

         BarTalk.Mood mood = this.mood();
         BarTalk.Script script = forced != null ? forced : BarTalk.pickEvent(1 + Math.min(2, partners.size()), crowd.size(), mood, this.f_19796_);
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
               if (pxx.m_6084_()
                  && pxx.talk == null
                  && s != BarPatron.State.LEAVING
                  && s != BarPatron.State.FIGHTING
                  && s != BarPatron.State.ARRIVING
                  && s != BarPatron.State.ROBBING
                  && !(pxx.m_20280_(this) > 196.0)) {
                  pxx.chatCooldown = Math.max(pxx.chatCooldown, 200);
                  pxx.emoteLater(
                     line.reaction() != BarTalk.Emote.NONE ? line.reaction() : cheer[pxx.f_19796_.m_188503_(cheer.length)], 6 + pxx.f_19796_.m_188503_(12)
                  );
                  if (voices < 5) {
                     pxx.sayLater(BarTalk.fill(shouts[pxx.f_19796_.m_188503_(shouts.length)], c.mood), 10 + voices++ * 7 + pxx.f_19796_.m_188503_(8));
                  }
               }
            }
            break;
         case "WRESTLE":
            for (BarPatron pxx : c.cast) {
               pxx.emote(BarTalk.Emote.ARM_WRESTLE);
            }

            this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_12634_, SoundSource.NEUTRAL, 0.6F, 0.8F);
            break;
         case "HIGHFIVE":
            for (BarPatron pxx : c.cast) {
               pxx.emote(BarTalk.Emote.HIGH_FIVE);
            }

            this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_12318_, SoundSource.NEUTRAL, 0.7F, 1.7F);
            break;
         case "SLEEP":
            this.emote(BarTalk.Emote.SLEEP);
            this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_12321_, SoundSource.NEUTRAL, 0.3F, 0.7F);
            break;
         case "SPILL":
            ItemStack held = this.m_21205_();
            if (held.m_41619_() || held.m_41720_() instanceof CigarItem) {
               return;
            }

            if (this.m_9236_() instanceof ServerLevel s) {
               s.m_8767_(
                  new ItemParticleOption(ParticleTypes.f_123752_, held), this.m_20185_(), this.m_20186_() + 1.1, this.m_20189_(), 12, 0.2, 0.1, 0.2, 0.08
               );
               s.m_8767_(ParticleTypes.f_123769_, this.m_20185_(), this.m_20186_() + 0.9, this.m_20189_(), 24, 0.35, 0.2, 0.35, 0.1);
            }

            this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_11983_, SoundSource.NEUTRAL, 0.7F, 1.2F);
            this.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
            this.sipsLeft = 0;
            this.visits++;
            this.stay = true;
            this.lingerTicks = 80 + this.f_19796_.m_188503_(60);
            break;
         case "CHUG":
            for (BarPatron px : c.cast) {
               if (px.sipsLeft > 0 && !px.m_21205_().m_41619_() && !(px.m_21205_().m_41720_() instanceof CigarItem)) {
                  px.sipsLeft = Math.min(px.sipsLeft, 3);
                  px.chug = 3;
                  px.sipTimer = 4 + px.f_19796_.m_188503_(6);
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
                  if (p.sipsLeft <= 0 && p.m_21205_().m_41619_()) {
                     p.lingerTicks = Math.min(p.lingerTicks, 60 + p.f_19796_.m_188503_(80));
                  }
               }
            }
            break;
         case "TIP":
            CashRegisterEntity r = this.register();
            if (r != null) {
               this.tip(r, 5 + this.f_19796_.m_188503_(11));
            }
            break;
         case "JUMP":
            if (this.m_20159_()) {
               this.m_6674_(InteractionHand.MAIN_HAND);
            } else {
               this.m_21569_().m_24901_();
            }
            break;
         case "SMASH":
            if (this.m_9236_() instanceof ServerLevel s) {
               s.m_8767_(
                  new BlockParticleOption(ParticleTypes.f_123794_, Blocks.f_50058_.m_49966_()),
                  this.m_20185_(),
                  this.m_20186_() + 0.3,
                  this.m_20189_(),
                  18,
                  0.3,
                  0.1,
                  0.3,
                  0.05
               );
            }

            this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_11983_, SoundSource.NEUTRAL, 0.8F, 1.0F);
            break;
         case "TUNE":
            float[] notes = new float[]{0.7F, 0.9F, 1.05F, 1.2F};

            for (int i = 0; i < 3; i++) {
               this.m_9236_()
                  .m_5594_(
                     null,
                     this.m_20183_(),
                     (SoundEvent)SoundEvents.f_12214_.m_203334_(),
                     SoundSource.RECORDS,
                     0.6F,
                     notes[this.f_19796_.m_188503_(notes.length)]
                  );
            }
            break;
         case "FIREWORK":
            if (this.m_9236_() instanceof ServerLevel s) {
               s.m_8767_(ParticleTypes.f_123815_, this.m_20185_(), this.m_20186_() + 2.2, this.m_20189_(), 40, 0.5, 0.4, 0.5, 0.08);
            }

            this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_11928_, SoundSource.NEUTRAL, 0.6F, 1.1F);
            break;
         case "CLINK":
            this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_144243_, SoundSource.NEUTRAL, 0.7F, 1.7F);
            break;
         case "COIN":
            this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_11745_, SoundSource.NEUTRAL, 0.4F, 1.9F);
            break;
         case "THUNDER":
            this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_12090_, SoundSource.WEATHER, 0.25F, 1.4F);
      }
   }

   private void behave(@Nullable CashRegisterEntity r) {
      Player bartender = this.m_9236_().m_45930_(this, 10.0);
      if (bartender != null && this.f_19797_ % 5 == 0) {
         if (this.lookingAtMe(bartender)) {
            this.look(bartender.m_146892_(), 30);
            this.gazeTicks += 5;
            if (this.gazeTicks == 25 && this.talk == null) {
               this.emote(this.f_19796_.m_188499_() ? BarTalk.Emote.WAVE : BarTalk.Emote.NOD);
            }

            if (this.gazeTicks >= 60 && this.m_20280_(bartender) < 36.0) {
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
            this.m_21563_().m_24950_(this.lookTarget.f_82479_, this.lookTarget.f_82480_, this.lookTarget.f_82481_, 15.0F, 30.0F);
         }
      } else if (this.talk == null && --this.glanceTicks <= 0) {
         this.glanceTicks = 30 + this.f_19796_.m_188503_(90);
         int roll = this.f_19796_.m_188503_(10);
         List<BarPatron> nearby = this.m_9236_().m_6443_(BarPatron.class, this.m_20191_().m_82400_(6.0), px -> px != this);
         BarPatron talker = null;

         for (BarPatron p : nearby) {
            if (!p.speech().isEmpty()) {
               talker = p;
               break;
            }
         }

         if (talker != null && roll < 7) {
            this.look(talker.m_146892_(), 40 + this.f_19796_.m_188503_(40));
         } else if (bartender == null || (this.state() == BarPatron.State.WAITING ? roll >= 7 : roll >= 3)) {
            if (!nearby.isEmpty() && roll < 6) {
               this.look(nearby.get(this.f_19796_.m_188503_(nearby.size())).m_146892_(), 30 + this.f_19796_.m_188503_(50));
            } else {
               this.look(
                  this.m_146892_().m_82520_(this.f_19796_.m_188583_() * 4.0, this.f_19796_.m_188583_() * 0.8, this.f_19796_.m_188583_() * 4.0),
                  20 + this.f_19796_.m_188503_(40)
               );
            }
         } else {
            this.look(bartender.m_146892_(), 30 + this.f_19796_.m_188503_(40));
         }
      }

      if (this.crouchBursts > 0 && --this.crouchTicks <= 0) {
         boolean down = !this.crouching();
         this.f_19804_.m_135381_(CROUCH, down);
         this.crouchTicks = 3 + this.f_19796_.m_188503_(2);
         if (!down) {
            this.crouchBursts--;
         }
      } else if (this.crouchBursts == 0 && this.crouching()) {
         this.f_19804_.m_135381_(CROUCH, false);
      }

      if (!this.m_20159_() && this.m_21573_().m_26571_() && this.wander == null && --this.fidgetTicks <= 0) {
         this.fidgetTicks = 100 + this.f_19796_.m_188503_(240);
         BlockPos anchor = this.spot != null ? this.spot : this.m_20183_();
         Vec3 to = Vec3.m_82539_(anchor).m_82520_(this.f_19796_.m_188583_() * 0.35, 0.0, this.f_19796_.m_188583_() * 0.35);
         if (r == null || !r.behindCounter(BlockPos.m_274446_(to))) {
            this.m_21566_().m_6849_(to.f_82479_, to.f_82480_, to.f_82481_, 0.6);
         }

         if (this.f_19796_.m_188503_(5) == 0) {
            this.m_21569_().m_24901_();
         }
      }
   }

   private boolean lookingAtMe(Player player) {
      Vec3 view = player.m_20252_(1.0F);
      Vec3 to = this.m_146892_().m_82546_(player.m_146892_());
      double d = to.m_82553_();
      return d < 10.0 && d > 0.1 && view.m_82526_(to.m_82490_(1.0 / d)) > 0.985 && player.m_142582_(this);
   }

   void look(Vec3 at, int ticks) {
      this.lookTarget = at;
      this.lookTicks = ticks;
      this.m_21563_().m_24950_(at.f_82479_, at.f_82480_, at.f_82481_, 15.0F, 30.0F);
   }

   @Nullable
   private Player nearestBartender(CashRegisterEntity r, double range) {
      Player best = null;
      double bestDistance = range * range;

      for (Player p : this.m_9236_().m_6907_()) {
         if (!p.m_5833_()) {
            double d = p.m_20280_(this);
            if (d < bestDistance && (r.behindCounter(p.m_20183_()) || best == null)) {
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
            this.look(bartender.m_146892_(), 20);
         } else {
            this.look(Vec3.m_82512_(r.m_58899_()), 20);
         }
      }
   }

   private void social(@Nullable CashRegisterEntity r) {
      if (this.f_19797_ % 100 == 0) {
         boolean music = this.findJukebox() != null;
         if (music != this.dancing()) {
            this.f_19804_.m_135381_(DANCING, music);
            if (music && this.talk == null && this.f_19796_.m_188503_(3) == 0) {
               this.say(pick(this.f_19796_, "Love this song!", "Turn it up!", "Ooh, I know this one!"));
            }
         }
      }

      if (this.talk == null) {
         if (this.f_19797_ % 40 == 0) {
            this.greetFriends(r);
         }

         if (this.f_19797_ % 20 == 0) {
            this.chatCooldown -= 20;
            this.startConversation(this.m_9236_().m_6443_(BarPatron.class, this.m_20191_().m_82400_(6.0), px -> px != this));
         }

         if (this.talk == null && this.speechTicks <= 0 && --this.socialCooldown <= 0) {
            this.socialCooldown = 300 + this.f_19796_.m_188503_(600);

            for (BarPatron p : this.m_9236_().m_6443_(BarPatron.class, this.m_20191_().m_82400_(10.0), px -> px != this)) {
               if (p.state() == BarPatron.State.FIGHTING && this.f_19796_.m_188499_()) {
                  this.say(pick(this.f_19796_, "Fight! Fight!", "Take it outside!", "Somebody stop them!", "Get him!"));
                  this.emote(BarTalk.Emote.FIST_PUMP);
                  if (!this.m_20159_()) {
                     this.m_21569_().m_24901_();
                  }

                  return;
               }
            }

            if (this.drunk() >= 4.5F && this.f_19796_.m_188503_(3) == 0) {
               this.say(pick(this.f_19796_, "*hic*", "Is the floor moving?", "I'm not drunk, YOU'RE drunk.", "Who keeps moving the stools?"));
               this.emote(BarTalk.Emote.SHRUG);
               this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_12321_, SoundSource.NEUTRAL, 0.4F, 1.5F);
            }
         }
      }
   }

   private boolean knows(@Nullable CashRegisterEntity r, BarPatron p) {
      return p.group.equals(this.group) || this.talkedWith.contains(p.m_20148_()) || r != null && r.friends(this.skin(), p.skin());
   }

   private void startConversation(List<BarPatron> nearby) {
      if (this.talk == null && this.canChat() && this.chatCooldown <= 0) {
         this.chatCooldown = 100;
         CashRegisterEntity reg = this.register();
         double close = 12.25;
         double adjacent = 6.25;

         for (BarPatron p : nearby) {
            if (p.talk != null && !p.talk.script.event() && p.talk.listener == null && this.knows(reg, p) && p.m_20280_(this) < close && !p.talk.involves(this)
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
            if (cast.size() < 3 && px.group.equals(this.group) && px.m_20280_(this) < close && px.talk == null && px.canChat()) {
               cast.add(px);
            }
         }

         if (cast.size() == 1) {
            for (BarPatron pxx : nearby) {
               if (this.knows(reg, pxx) && pxx.m_20280_(this) < close && pxx.talk == null && pxx.canChat()) {
                  cast.add(pxx);
                  break;
               }
            }
         }

         boolean strangers = false;
         if (cast.size() == 1 && this.f_19796_.m_188499_()) {
            for (BarPatron pxxx : nearby) {
               if (pxxx.m_20280_(this) < adjacent && pxxx.m_20159_() == this.m_20159_() && pxxx.talk == null && pxxx.canChat()) {
                  cast.add(pxxx);
                  strangers = true;
                  break;
               }
            }
         }

         if (cast.size() == 1) {
            CashRegisterEntity r = this.register();
            Player bartender = this.m_9236_().m_45930_(this, 5.0);
            if (r != null && bartender != null && r.behindCounter(bartender.m_20183_()) && this.f_19796_.m_188503_(3) == 0) {
               this.chatCooldown = 0;
               this.bartenderSmallTalk(bartender);
            }
         } else {
            BarTalk.Mood mood = this.mood();
            boolean reunion = !strangers
               && reg != null
               && cast.size() == 2
               && !this.talkedWith.contains(cast.get(1).m_20148_())
               && reg.friends(this.skin(), cast.get(1).skin());
            BarTalk.Script script = BarTalk.pick(strangers ? BarTalk.INTROS : (reunion ? BarTalk.REUNIONS : BarTalk.CHATS), cast.size(), mood, this.f_19796_);
            if (script != null) {
               BarPatron[] roles = cast.subList(0, script.roles()).toArray(new BarPatron[0]);

               for (BarPatron a : roles) {
                  for (BarPatron b : roles) {
                     if (a != b) {
                        a.talkedWith.add(b.m_20148_());
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
         for (BarPatron p : this.m_9236_().m_6443_(BarPatron.class, this.m_20191_().m_82400_(10.0), px -> px != this && !px.group.equals(this.group))) {
            if (!this.greeted.contains(p.m_20148_())
               && r.friends(this.skin(), p.skin())
               && p.state() != BarPatron.State.ARRIVING
               && p.state() != BarPatron.State.LEAVING) {
               this.greeted.add(p.m_20148_());
               p.greeted.add(this.m_20148_());
               this.look(p.m_146892_(), 40);
               this.emote(BarTalk.Emote.WAVE);
               this.say(pick(this.f_19796_, "Hey! You're here too!", "Look who it is!", "Over here, friend!", "Good to see you!"));
               p.look(this.m_146892_(), 40);
               p.emoteLater(BarTalk.Emote.WAVE, 12);
               return;
            }
         }
      }
   }

   private void bartenderSmallTalk(Player bartender) {
      if (this.talk == null && this.canChat() && this.chatCooldown <= 200) {
         BarTalk.Mood mood = this.mood();
         BarTalk.Script script = BarTalk.pick(BarTalk.BARTENDER, 1, mood, this.f_19796_);
         if (script != null) {
            this.talk = new BarTalk.Conversation(script, new BarPatron[]{this}, mood, bartender);
         }
      }
   }

   private BarTalk.Mood mood() {
      Player bartender = this.m_9236_().m_45930_(this, 16.0);
      boolean fight = !this.m_9236_().m_6443_(BarPatron.class, this.m_20191_().m_82400_(12.0), px -> px.state() == BarPatron.State.FIGHTING).isEmpty();
      BarProducts.Product p = this.product();
      boolean smoking = this.m_21205_().m_41720_() instanceof CigarItem || this.ashtray != null;
      BarPatron other = null;
      double best = 196.0;

      for (BarPatron o : this.m_9236_()
         .m_6443_(
            BarPatron.class,
            this.m_20191_().m_82400_(14.0),
            ox -> ox != this && !ox.group.equals(this.group) && ox.state() != BarPatron.State.ARRIVING && ox.state() != BarPatron.State.LEAVING
         )) {
         double d = o.m_20280_(this);
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
         this.m_9236_().m_46462_(),
         this.m_9236_().m_46471_(),
         this.drunk() >= 4.5F,
         fight,
         (!this.m_21205_().m_41619_() || this.ashtray != null) && p != null,
         smoking,
         p == null ? "drink" : p.name().getString(),
         bartender == null ? "the bartender" : bartender.m_7755_().getString(),
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
      this.f_19804_.m_135381_(SKIN, 2097152);
      this.m_21008_(InteractionHand.MAIN_HAND, new ItemStack(Items.f_42383_));
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
         if (this.m_21223_() < this.m_21233_()) {
            this.m_21153_(this.m_21233_());
         }

         this.robTicks++;
         switch (this.robPhase) {
            case 0:
               if (this.walkTo(r.front(), 1.2) || this.failed() || this.robTicks > 400) {
                  this.robPhase = 1;
                  this.robTicks = 0;
                  this.say("Nobody move! Empty the register!");
                  this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_11699_, SoundSource.NEUTRAL, 0.8F, 0.6F);

                  for (Player near : this.m_9236_().m_45976_(Player.class, this.m_20191_().m_82400_(40.0))) {
                     near.m_5661_(Component.m_237113_("A robber is holding up the register!").m_130940_(ChatFormatting.RED), true);
                  }

                  for (BarPatron px : this.room(22.0)) {
                     if (px.state() != BarPatron.State.LEAVING) {
                        px.sayLater(
                           pick(this.f_19796_, "Is that a robber?!", "Oh no...", "Somebody do something!", "He's got a sword!"),
                           10 + this.f_19796_.m_188503_(40)
                        );
                        px.emoteLater(BarTalk.Emote.SHAKE, 10 + this.f_19796_.m_188503_(30));
                     }
                  }
               }
               break;
            case 1:
               this.m_21573_().m_26573_();
               Vec3 till = Vec3.m_82512_(r.m_58899_());
               this.m_21563_().m_24946_(till.f_82479_, till.f_82480_, till.f_82481_);
               if (this.robTicks % 50 == 25) {
                  this.say(pick(this.f_19796_, "Hand over the cash! NOW!", "Don't be a hero!", "Fill the bag! All of it!", "I mean it! Empty the till!"));
               }

               if (this.robTicks == 60) {
                  this.rally();
               }

               boolean defenders = false;

               for (BarPatron p : this.room(30.0)) {
                  if (p.state() == BarPatron.State.FIGHTING && this.m_20148_().equals(p.partner)) {
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
      return this.m_9236_().m_6443_(BarPatron.class, this.m_20191_().m_82400_(range), p -> p != this && !p.robber && p.m_6084_());
   }

   private void rally() {
      List<BarPatron> brave = new ArrayList<>();

      for (BarPatron p : this.room(24.0)) {
         BarPatron.State s = p.state();
         if (s == BarPatron.State.ENJOYING || s == BarPatron.State.TO_SEAT || s == BarPatron.State.WAITING || s == BarPatron.State.AWAIT_SHOT) {
            brave.add(p);
         }
      }

      brave.sort(Comparator.comparingDouble(px -> px.m_20280_(this)));
      int n = 0;

      for (BarPatron px : brave) {
         if (n >= 3) {
            px.sayLater(pick(this.f_19796_, "Get him!", "Go on, lads!", "Teach him a lesson!"), 10 + this.f_19796_.m_188503_(30));
         } else {
            n++;
            if (px.m_20159_()) {
               px.m_8127_();
            }

            if (px.talk != null) {
               px.endTalk();
            }

            px.wander = null;
            px.dialogueWith = null;
            px.crouchBursts = 0;
            px.partner = this.m_20148_();
            px.setState(BarPatron.State.FIGHTING);
            px.fightTicks = 900;
            px.hitCooldown = 15 + px.f_19796_.m_188503_(25);
            px.say(pick(this.f_19796_, "Hey! Get away from the register!", "Not in this pub!", "Drop the sword!", "Get him!"));
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

         for (Player near : this.m_9236_().m_45976_(Player.class, this.m_20191_().m_82400_(40.0))) {
            near.m_5661_(Component.m_237113_("The customers drove off the robber. Nothing was stolen.").m_130940_(ChatFormatting.GREEN), true);
         }

         for (BarPatron p : this.room(30.0)) {
            if (p.state() != BarPatron.State.LEAVING) {
               p.sayLater(
                  pick(this.f_19796_, "Yeah! Don't come back!", "Never robbed here!", "Get lost!", "Hooray!", "Good riddance!"),
                  10 + this.f_19796_.m_188503_(50)
               );
               p.emoteLater(this.f_19796_.m_188499_() ? BarTalk.Emote.CLAP : BarTalk.Emote.FIST_PUMP, 10 + this.f_19796_.m_188503_(40));
            }
         }
      }
   }

   private void defend(BarPatron thief) {
      if (thief.m_6084_() && thief.state() != BarPatron.State.LEAVING && --this.fightTicks > 0) {
         this.m_21563_().m_24960_(thief, 30.0F, 30.0F);
         if (this.m_20280_(thief) > 4.0) {
            this.m_21573_().m_5624_(thief, 1.15);
         } else {
            this.m_21573_().m_26573_();
            if (this.hitCooldown <= 0) {
               this.hitCooldown = 12 + this.f_19796_.m_188503_(14);
               this.m_6674_(InteractionHand.MAIN_HAND);
               thief.m_147240_(0.3, this.m_20185_() - thief.m_20185_(), this.m_20189_() - thief.m_20189_());
               thief.robBeaten++;
               this.m_9236_().m_5594_(null, thief.m_20183_(), SoundEvents.f_12316_, SoundSource.NEUTRAL, 0.8F, 1.0F);
               if (this.f_19796_.m_188503_(3) == 0) {
                  this.say(pick(this.f_19796_, "Take that!", "Out!", "Not here, you don't!", "Drop it!"));
               }
            }
         }
      } else {
         this.partner = null;
         this.calmDown(
            thief.m_6084_() && thief.state() != BarPatron.State.LEAVING
               ? "...where did he go?"
               : pick(this.f_19796_, "Good riddance!", "And stay out!", "That'll teach him.")
         );
      }
   }

   public static void brawl(BarPatron a, BarPatron b) {
      for (BarPatron p : new BarPatron[]{a, b}) {
         if (p.m_20159_()) {
            p.m_8127_();
         }

         p.wander = null;
         p.setState(BarPatron.State.FIGHTING);
         p.fightTicks = 140 + p.f_19796_.m_188503_(80);
         p.hitCooldown = 20;
      }

      a.partner = b.m_20148_();
      b.partner = a.m_20148_();
      a.say(pick(a.f_19796_, "You spilled my drink!", "What'd you just say?!", "That's MY seat, pal!", "You looking at me?"));
      b.say(pick(b.f_19796_, "Oh, it's on!", "Say that again!", "Come at me!", "You wanna go?!"));
   }

   private void fight() {
      Entity other = this.partner != null && this.m_9236_() instanceof ServerLevel server ? server.m_8791_(this.partner) : null;
      if (other instanceof BarPatron thief && thief.robber) {
         this.defend(thief);
         return;
      }

      if (other instanceof BarPatron foe && foe.state() == BarPatron.State.FIGHTING && --this.fightTicks > 0) {
         this.m_21563_().m_24960_(foe, 30.0F, 30.0F);
         if (this.m_20280_(foe) > 4.840000000000001) {
            this.m_21573_().m_5624_(foe, 1.1);
            return;
         }

         this.m_21573_().m_26573_();
         if (this.hitCooldown <= 0) {
            this.hitCooldown = 14 + this.f_19796_.m_188503_(16);
            this.m_6674_(InteractionHand.MAIN_HAND);
            if (this.f_19796_.m_188501_() < 0.35F) {
               this.m_9236_().m_5594_(null, this.m_20183_(), SoundEvents.f_12315_, SoundSource.NEUTRAL, 0.8F, 0.9F);
               return;
            }

            if (foe.m_21223_() > 8.0F) {
               foe.m_6469_(this.m_269291_().m_269333_(this), 1.0F);
            }

            foe.m_147240_(0.35, this.m_20185_() - foe.m_20185_(), this.m_20189_() - foe.m_20189_());
            this.m_9236_().m_5594_(null, foe.m_20183_(), SoundEvents.f_12318_, SoundSource.NEUTRAL, 0.8F, 0.9F);
            if (this.f_19796_.m_188503_(4) == 0) {
               this.say(pick(this.f_19796_, "Take that!", "Hold still!", "*hic* POW!", "My mother hits harder!"));
            }
         }

         return;
      }

      this.endFight(other instanceof BarPatron f ? f : null);
   }

   private void endFight(@Nullable BarPatron foe) {
      this.partner = null;
      this.m_21153_(this.m_21233_());
      if (foe != null && foe.state() == BarPatron.State.FIGHTING) {
         foe.partner = null;
         foe.m_21153_(foe.m_21233_());
         BarPatron loser = this.f_19796_.m_188499_() ? this : foe;
         BarPatron winner = loser == this ? foe : this;
         loser.say(pick(this.f_19796_, "I'm outta here!", "This bar stinks anyway!", "You'll hear from my lawyer!"));
         loser.leaveWithGroup(loser.register());
         winner.calmDown(pick(this.f_19796_, "Sorry about that, folks.", "...where was I?", "Bartender, another!"));
      } else {
         this.calmDown("...where was I?");
      }
   }

   private void calmDown(String line) {
      this.say(line);
      if (this.sipsLeft <= 0 || this.m_21205_().m_41619_()) {
         this.lingerTicks = 0;
      }

      this.setState(BarPatron.State.TO_SEAT);
   }

   public boolean m_6469_(DamageSource source, float amount) {
      boolean hurt = super.m_6469_(source, amount);
      if (this.robber) {
         if (hurt && !this.m_9236_().f_46443_ && source.m_7639_() instanceof Player) {
            this.robBeaten += 2;
            this.say(pick(this.f_19796_, "Argh!", "Ow! Not the face!", "Get off me!"));
         }

         return hurt;
      } else {
         if (hurt
            && !this.m_9236_().f_46443_
            && source.m_7639_() instanceof Player
            && this.state() != BarPatron.State.FIGHTING
            && this.state() != BarPatron.State.LEAVING) {
            this.angry = true;
            if (this.register() != null) {
               this.register().hurtReputation(3);
            }

            this.say(pick(this.f_19796_, "Ow! What was that for?!", "Hey! Watch it!", "I'm leaving, you psycho!"));

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
         this.say(pick(this.f_19796_, "What? I didn't do anything!", "Wow. Okay.", "I'm never coming back here."));
         this.angry = true;
         if (r != null) {
            r.hurtReputation(2);
         }
      } else {
         this.say(pick(this.f_19796_, "Fine, fine... *hic* I'm going.", "I wasn't THAT drunk...", "You're no fun."));
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

      if (this.m_20159_()) {
         this.m_8127_();
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
      this.m_21008_(InteractionHand.MAIN_HAND, this.m_21205_().m_41720_() instanceof CigarItem ? this.m_21205_() : ItemStack.f_41583_);
      this.f_19804_.m_135381_(CROUCH, false);
      this.setState(BarPatron.State.LEAVING);
   }

   private void leaveWithGroup(@Nullable CashRegisterEntity r) {
      this.leave(r);

      for (BarPatron mate : this.mates()) {
         BarPatron.State s = mate.state();
         if (s != BarPatron.State.LEAVING && s != BarPatron.State.FIGHTING && (s != BarPatron.State.ENJOYING || mate.sipsLeft <= 0)) {
            mate.sayLater(pick(this.f_19796_, "Wait up!", "Yeah, I'm beat too.", "Night, everyone!"), 20 + this.f_19796_.m_188503_(30));
            mate.leave(r);
         }
      }
   }

   private void leaving() {
      BlockPos[] route = new BlockPos[]{this.inside, this.outside, this.far};
      if (route[0] == null || route[1] == null) {
         if (++this.legTicks > 400 || this.walkTo(this.m_20183_().m_7918_(8, 0, 8), 2.0)) {
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

      this.m_146870_();
   }

   private boolean near(BlockPos p, double reach) {
      return p != null && this.m_20238_(Vec3.m_82539_(p)) <= reach * reach;
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
         Vec3 c = Vec3.m_82539_(target);
         double d = this.m_20238_(c);
         if (d <= reach * reach) {
            this.m_21573_().m_26573_();
            this.navTarget = null;
            this.navTicks = 0;
            return true;
         } else {
            if (this.m_20159_()) {
               this.m_8127_();
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
               Path first = this.m_21573_().m_26570_();
               if (first == null || !first.m_77403_()) {
                  this.startGrid(target);
               }
            } else if (!this.gridMode && this.m_21573_().m_26571_()) {
               this.repath(c);
            }

            if (this.gridMode) {
               this.gridWalk(target, c);
            }

            this.navTotal++;
            if (this.f_19797_ % 10 == 0) {
               Path path = this.m_21573_().m_26570_();
               if (path != null && path == this.navPath && path.m_77399_() > this.navNode && path.m_77403_() && this.navTotal < 900) {
                  this.navTicks = 0;
               }

               this.navPath = path;
               this.navNode = path == null ? 0 : path.m_77399_();
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
                  this.m_21573_().m_26573_();
                  this.m_21566_()
                     .m_6849_(
                        this.m_20185_() + this.f_19796_.m_188583_() * 0.6,
                        this.m_20186_(),
                        this.m_20189_() + this.f_19796_.m_188583_() * 0.6,
                        this.speedNow() * 0.8
                     );
               }

               if (this.navTicks == 60 && !this.gridMode) {
                  this.repath(c);
               }

               if (this.navTicks == 75) {
                  this.m_21569_().m_24901_();
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
      BlockPos start = this.m_20183_();
      if (Double.isNaN(BarWorker.floorAt(this.m_9236_(), start))) {
         BlockPos below = start.m_7495_();
         if (Double.isNaN(BarWorker.floorAt(this.m_9236_(), below))) {
            return;
         }

         start = below;
      }

      List<BlockPos> route = FloorRoutes.route(
         this.m_9236_(),
         start,
         target,
         q -> r == null || !r.behindCounter(q) && !(this.m_9236_().m_8055_(q.m_7495_()).m_60734_() instanceof SeatBlock) && !r.seats.contains(q.m_7495_()),
         4000
      );
      if (route != null && !route.isEmpty()) {
         this.m_21573_().m_26573_();
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
         double dx = (double)next.m_123341_() + 0.5 - this.m_20185_();
         double dz = (double)next.m_123343_() + 0.5 - this.m_20189_();
         if (!(dx * dx + dz * dz < 0.12249999999999998) || !(Math.abs((double)next.m_123342_() - this.m_20186_()) < 1.3)) {
            break;
         }

         this.gridRoute.poll();
         next = this.gridRoute.peek();
      }

      if (next != null || !(this.m_20275_(c.f_82479_, this.m_20186_(), c.f_82481_) > 2.25)) {
         Vec3 aim = next == null ? c : Vec3.m_82539_(next);
         this.m_21566_().m_6849_(aim.f_82479_, aim.f_82480_, aim.f_82481_, this.speedNow());
      } else if (++this.gridReplans > 3) {
         this.gridMode = false;
         this.gridReplans = 0;
      } else {
         this.startGrid(target);
      }
   }

   private void repath(Vec3 c) {
      this.m_21573_().m_26519_(c.f_82479_, c.f_82480_, c.f_82481_, this.speedNow());
      CashRegisterEntity r = this.register();
      Path path = this.m_21573_().m_26570_();
      if (r != null && path != null && this.navTarget != null && !r.behindCounter(this.navTarget) && !r.behindCounter(this.m_20183_())) {
         for (int i = path.m_77399_(); i < path.m_77398_(); i++) {
            if (r.behindCounter(path.m_77375_(i).m_77288_())) {
               this.m_21573_().m_26573_();
               this.navFailed = true;
               return;
            }
         }
      }
   }

   private boolean followLeg(BlockPos target, double reach) {
      if (this.door != null
         && this.near(this.door, 3.0)
         && this.m_9236_().m_8055_(this.door).m_60734_() instanceof DoorBlock
         && (Boolean)this.m_9236_().m_8055_(this.door).m_61143_(DoorBlock.f_52727_)) {
         Vec3 c = Vec3.m_82539_(target);
         if (this.m_20238_(c) <= reach * reach) {
            this.m_21573_().m_26573_();
            this.legTicks = 0;
            this.bestDistance = Double.MAX_VALUE;
            return true;
         }

         this.m_21573_().m_26573_();
         this.m_21566_().m_6849_(c.f_82479_, c.f_82480_, c.f_82481_, this.speedNow());
      } else if (this.walkTo(target, reach)) {
         this.legTicks = 0;
         this.bestDistance = Double.MAX_VALUE;
         return true;
      }

      double d = this.m_20238_(Vec3.m_82539_(target));
      if (d < this.bestDistance - 0.25) {
         this.bestDistance = d;
         this.legTicks = 0;
      } else if (++this.legTicks > 120) {
         this.m_7678_((double)target.m_123341_() + 0.5, (double)target.m_123342_(), (double)target.m_123343_() + 0.5, this.m_146908_(), this.m_146909_());
         this.m_21573_().m_26573_();
         this.legTicks = 0;
         this.bestDistance = Double.MAX_VALUE;
         return true;
      }

      return false;
   }

   private void openDoor() {
      if (this.door != null) {
         BlockState state = this.m_9236_().m_8055_(this.door);
         if (state.m_60734_() instanceof DoorBlock block) {
            if (!(Boolean)state.m_61143_(DoorBlock.f_52727_)) {
               block.m_153165_(this, this.m_9236_(), state, this.door, true);
            }

            this.openedDoor = true;
         }
      }
   }

   private void closeDoor() {
      if (this.door != null && this.openedDoor) {
         this.openedDoor = false;
         BlockState state = this.m_9236_().m_8055_(this.door);
         boolean another = !this.m_9236_()
            .m_6443_(
               BarPatron.class,
               new AABB(this.door).m_82377_(4.0, 1.0, 4.0),
               p -> p != this && (p.state() == BarPatron.State.ARRIVING || p.state() == BarPatron.State.LEAVING)
            )
            .isEmpty();
         if (state.m_60734_() instanceof DoorBlock block && (Boolean)state.m_61143_(DoorBlock.f_52727_) && !another) {
            block.m_153165_(this, this.m_9236_(), state, this.door, false);
         }
      }
   }

   protected InteractionResult m_6071_(Player player, InteractionHand hand) {
      if (!this.m_9236_().f_46443_ && this.order() >= 0 && this.handOver(player, player.m_21120_(hand))) {
         return InteractionResult.SUCCESS;
      } else {
         if (!this.m_9236_().f_46443_) {
            this.look(player.m_146892_(), 60);
         }

         BarPatron.State s = this.state();
         if (s == BarPatron.State.ROBBING) {
            if (!this.m_9236_().f_46443_) {
               this.say(pick(this.f_19796_, "Back off!", "Hands where I can see them!", "Don't try anything!"));
            }

            return InteractionResult.m_19078_(this.m_9236_().f_46443_);
         } else if (s != BarPatron.State.ARRIVING && s != BarPatron.State.LEAVING && s != BarPatron.State.FIGHTING) {
            if ((s == BarPatron.State.WAITING || s == BarPatron.State.AWAIT_SHOT) && !player.m_6144_()) {
               if (!this.m_9236_().f_46443_ && player instanceof ServerPlayer sp) {
                  CashRegisterEntity r = this.register();
                  if (this.order() < 0 && r != null) {
                     this.chooseOrder(r);
                  }

                  if (!this.announced) {
                     this.announced = true;
                     this.say(this.orderLine());
                  }

                  NetworkHooks.openScreen(
                     sp,
                     new SimpleMenuProvider(
                        (id, inventory, p) -> new PatronMenu(id, inventory, this), Component.m_237113_(this.regular ? "Customer · regular" : "Customer")
                     ),
                     buf -> buf.m_130130_(this.m_19879_())
                  );
               }

               return InteractionResult.m_19078_(this.m_9236_().f_46443_);
            } else {
               if (!this.m_9236_().f_46443_ && player instanceof ServerPlayer sp) {
                  if (this.dialogueWith != null && !this.dialogueWith.equals(player.m_20148_())) {
                     this.say("One at a time!");
                     return InteractionResult.CONSUME;
                  }

                  int node = this.openingNode(player);
                  this.startDialogue(player);
                  this.say(BarDialogue.fill(BarDialogue.get(node).line(), this.drinkName(), this.mood().other(), player.m_7755_().getString()));
                  NetworkHooks.openScreen(
                     sp,
                     new SimpleMenuProvider((id, inventory, p) -> new DialogueMenu(id, inventory, this, node), Component.m_237113_("Conversation")),
                     buf -> {
                        buf.m_130130_(this.m_19879_());
                        buf.m_130130_(node);
                     }
                  );
               }

               return InteractionResult.m_19078_(this.m_9236_().f_46443_);
            }
         } else {
            if (!this.m_9236_().f_46443_) {
               this.say(pick(this.f_19796_, "Not now!", "Hang on a sec.", "Can't talk!"));
            }

            return InteractionResult.m_19078_(this.m_9236_().f_46443_);
         }
      }
   }

   private void clientEffects() {
      if (this.m_21205_().m_41720_() instanceof CigarItem) {
         float yaw = this.f_20885_ * (float) (Math.PI / 180.0);
         double fx = -Math.sin((double)yaw);
         double fz = Math.cos((double)yaw);
         if ((int)(this.m_9236_().m_46467_() - (long)((Integer)this.f_19804_.m_135370_(SIP)).intValue()) == 28) {
            for (int i = 0; i < 5; i++) {
               this.m_9236_()
                  .m_7106_(
                     (ParticleOptions)Drunkard.CIGAR_SMOKE.get(),
                     this.m_20185_() + fx * 0.3,
                     this.m_20188_() - 0.12,
                     this.m_20189_() + fz * 0.3,
                     fx * 0.03 + this.f_19796_.m_188583_() * 0.006,
                     0.015,
                     fz * 0.03 + this.f_19796_.m_188583_() * 0.006
                  );
            }
         }
      }
   }

   public void m_6667_(DamageSource source) {
      super.m_6667_(source);
      if (this.m_9236_() instanceof ServerLevel s) {
         s.m_8767_(ParticleTypes.f_123759_, this.m_20185_(), this.m_20186_() + 1.0, this.m_20189_(), 8, 0.3, 0.5, 0.3, 0.02);
      }

      CashRegisterEntity r = this.register();
      if (r != null) {
         r.forget(this);
      }
   }

   private static void put(CompoundTag tag, String key, @Nullable BlockPos pos) {
      if (pos != null) {
         tag.m_128356_(key, pos.m_121878_());
      }
   }

   @Nullable
   private static BlockPos get(CompoundTag tag, String key) {
      return tag.m_128441_(key) ? BlockPos.m_122022_(tag.m_128454_(key)) : null;
   }

   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      put(tag, "Register", this.register);
      put(tag, "Door", this.door);
      put(tag, "Inside", this.inside);
      put(tag, "Outside", this.outside);
      put(tag, "Far", this.far);
      put(tag, "Seat", this.seat);
      put(tag, "Spot", this.spot);
      tag.m_128362_("Group", this.group);
      tag.m_128405_("Skin", this.skin());
      tag.m_128359_("State", this.state().name());
      tag.m_128405_("Order", this.order());
      tag.m_128379_("Shot", this.wantsShot());
      tag.m_128350_("Drunk", this.drunk());
      tag.m_128350_("Limit", this.limit);
      tag.m_128405_("Visits", this.visits);
      tag.m_128405_("MaxVisits", this.maxVisits);
      tag.m_128405_("SipsLeft", this.sipsLeft);
      tag.m_128405_("Patience", this.patience);
      tag.m_128405_("Linger", this.lingerTicks);
      tag.m_128405_("Leg", this.leg);
      tag.m_128347_("Pace", this.pace);
      tag.m_128379_("Jogger", this.jogger);
      tag.m_128356_("WaitingSince", this.waitingSince);
      tag.m_128379_("OrderedHere", this.orderedHere);
      tag.m_128405_("Rapport", this.rapport);
      tag.m_128379_("Stander", this.stander);
      tag.m_128379_("Regular", this.regular);
      tag.m_128405_("Favourite", this.favourite);
      tag.m_128379_("Angry", this.angry);
      tag.m_128379_("Usual", this.usual);
      tag.m_128405_("OwnSmoke", this.ownSmoke);
      tag.m_128379_("SmokedOwn", this.smokedOwn);
      tag.m_128379_("Robber", this.robber);
      tag.m_128405_("RobPhase", this.robPhase);
      tag.m_128405_("RobBeaten", this.robBeaten);
   }

   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.register = get(tag, "Register");
      this.door = get(tag, "Door");
      this.inside = get(tag, "Inside");
      this.outside = get(tag, "Outside");
      this.far = get(tag, "Far");
      this.seat = get(tag, "Seat");
      this.spot = get(tag, "Spot");
      if (tag.m_128403_("Group")) {
         this.group = tag.m_128342_("Group");
      }

      this.f_19804_.m_135381_(SKIN, tag.m_128471_("Robber") ? 2097152 : Math.floorMod(tag.m_128451_("Skin"), 65536));

      BarPatron.State state;
      try {
         state = BarPatron.State.valueOf(tag.m_128461_("State"));
      } catch (IllegalArgumentException var4) {
         state = BarPatron.State.WAITING;
      }

      if (state == BarPatron.State.FIGHTING || state == BarPatron.State.ENJOYING) {
         state = BarPatron.State.TO_SEAT;
      }

      this.f_19804_.m_135381_(STATE, state.ordinal());
      this.f_19804_.m_135381_(ORDER, BarProducts.get(tag.m_128451_("Order")) == null ? -1 : tag.m_128451_("Order"));
      this.f_19804_.m_135381_(SHOT, tag.m_128471_("Shot"));
      this.setDrunk(tag.m_128457_("Drunk"));
      this.limit = tag.m_128441_("Limit") ? tag.m_128457_("Limit") : 6.0F;
      this.visits = tag.m_128451_("Visits");
      this.maxVisits = tag.m_128441_("MaxVisits") ? tag.m_128451_("MaxVisits") : 3;
      this.sipsLeft = tag.m_128451_("SipsLeft");
      this.patience = tag.m_128441_("Patience") ? tag.m_128451_("Patience") : 4800;
      this.lingerTicks = tag.m_128451_("Linger");
      this.leg = tag.m_128451_("Leg");
      this.pace = tag.m_128441_("Pace") ? tag.m_128459_("Pace") : 1.0;
      this.jogger = tag.m_128471_("Jogger");
      this.waitingSince = tag.m_128454_("WaitingSince");
      this.orderedHere = tag.m_128471_("OrderedHere");
      this.rapport = tag.m_128451_("Rapport");
      this.stander = false;
      this.regular = tag.m_128471_("Regular");
      this.favourite = tag.m_128441_("Favourite") ? tag.m_128451_("Favourite") : -1;
      this.angry = tag.m_128471_("Angry");
      this.usual = tag.m_128471_("Usual");
      this.ownSmoke = tag.m_128451_("OwnSmoke");
      this.smokedOwn = tag.m_128471_("SmokedOwn");
      this.robber = tag.m_128471_("Robber");
      this.robPhase = tag.m_128451_("RobPhase");
      this.robBeaten = tag.m_128451_("RobBeaten");
      if (this.robber) {
         this.f_19804_.m_135381_(SKIN, 2097152);
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
         + this.m_21573_().m_26571_()
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
         if (this.state() == BarPatron.State.ENJOYING && this.sipsLeft <= 0 && this.m_21205_().m_41619_() && this.ashtray == null) {
            return BarDialogue.index("leaving");
         } else {
            List<String> options = new ArrayList<>(BarDialogue.TOPICS);
            if (this.m_9236_().m_46471_()) {
               options.add("weather");
               options.add("weather");
            }

            if (this.m_21205_().m_41720_() instanceof CigarItem || this.ashtray != null) {
               options.add("smoke");
            } else if (!this.m_21205_().m_41619_()) {
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

            if (this.m_9236_().m_46462_()) {
               options.add("late");
            }

            if (options.size() > 1) {
               options.remove(this.lastTopic);
            }

            String pick = options.get(this.f_19796_.m_188503_(options.size()));
            this.lastTopic = pick;
            return BarDialogue.index(pick);
         }
      } else {
         return BarDialogue.index("regular");
      }
   }

   public void startDialogue(Player bartender) {
      this.dialogueWith = bartender.m_20148_();
      if (this.talk != null) {
         this.endTalk();
      }

      this.m_21573_().m_26573_();
   }

   public void endDialogue(Player bartender) {
      if (bartender.m_20148_().equals(this.dialogueWith)) {
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

         this.say(BarDialogue.fill(next.line(), this.drinkName(), this.mood().other(), bartender.m_7755_().getString()));
         this.look(bartender.m_146892_(), 40);
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
                  bartender.m_5661_(
                     Component.m_237113_("They'll remember that.").m_130944_(new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY}), true
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
                        bartender.m_5661_(Component.m_237113_("On the house (-$" + this.lastPrice + ")"), true);
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
      return r != null && r.behindCounter(this.m_20183_());
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
