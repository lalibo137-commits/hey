package com.drunkard.test;

import com.drunkard.AlcoholSystem;
import com.drunkard.AutomationPorts;
import com.drunkard.BarDialogue;
import com.drunkard.BarPatron;
import com.drunkard.BarProducts;
import com.drunkard.BarTalk;
import com.drunkard.BarUpgrades;
import com.drunkard.BarWorker;
import com.drunkard.BarrelBlock;
import com.drunkard.BarrelEntity;
import com.drunkard.BottleEntity;
import com.drunkard.BrewKettleEntity;
import com.drunkard.BrewingCropBlock;
import com.drunkard.CashRegisterBlock;
import com.drunkard.CashRegisterEntity;
import com.drunkard.CashRegisterMenu;
import com.drunkard.CigarHolderBlock;
import com.drunkard.CigarHolderEntity;
import com.drunkard.CigarItem;
import com.drunkard.CigarMouthState;
import com.drunkard.CigarettePackItem;
import com.drunkard.CocktailItem;
import com.drunkard.CoolerBlock;
import com.drunkard.CoolerEntity;
import com.drunkard.CopperPipeBlock;
import com.drunkard.DartBoardBlock;
import com.drunkard.DistillationStillEntity;
import com.drunkard.DrinkBenefits;
import com.drunkard.Drunkard;
import com.drunkard.FiredMachineBlock;
import com.drunkard.FullBottleRecipe;
import com.drunkard.GlassShelfBlock;
import com.drunkard.GlassShelfEntity;
import com.drunkard.HumidorEntity;
import com.drunkard.ItemData;
import com.drunkard.KegBlock;
import com.drunkard.KegEntity;
import com.drunkard.OnTheRocksItem;
import com.drunkard.OnTheRocksRecipe;
import com.drunkard.PipeContents;
import com.drunkard.PipeItem;
import com.drunkard.PlacedBottleBlock;
import com.drunkard.PotStillBlock;
import com.drunkard.PotStillEntity;
import com.drunkard.PourRecipe;
import com.drunkard.RackEntity;
import com.drunkard.RegisterLinking;
import com.drunkard.SafeEntity;
import com.drunkard.SeatBlock;
import com.drunkard.SeatEntity;
import com.drunkard.ShotGlassBlock;
import com.drunkard.ShotGlassEntity;
import com.drunkard.ShotGlassItem;
import com.drunkard.TapTowerBlock;
import com.drunkard.ThrownMolotov;
import com.drunkard.TobaccoCropBlock;
import com.drunkard.TransferPumpBlock;
import com.drunkard.TransferPumpEntity;
import com.drunkard.TrellisBlock;
import com.drunkard.TubEntity;
import com.drunkard.TubStomping;
import com.drunkard.WildCropBlock;
import com.drunkard.WineData;
import com.drunkard.WineItem;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag.Default;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Pub game tests (cash register, customers, barkeeps, safe, glass shelf), ported from the Forge 1.2.0 build. */
@GameTestHolder("drunkard")
@PrefixGameTestTemplate(false)
public final class PubGameTests {
   private static final BlockPos POS = new BlockPos(1, 2, 1);


   private static ItemStack drink(String type) {
      return new ItemStack((ItemLike)Drunkard.WINE.get(type).get());
   }
   private static final BlockPos REGISTER = new BlockPos(6, 2, 6);
   private static final BlockPos DOOR = new BlockPos(6, 1, 12);

   private static CashRegisterEntity bar(GameTestHelper h, String... stock) {
      for (int x = 0; x < 13; x++) {
         for (int z = 0; z < 15; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.SMOOTH_STONE);
         }
      }

      h.setBlock(REGISTER.below(), Blocks.SPRUCE_PLANKS);
      h.setBlock(REGISTER, (BlockState)((Block)Drunkard.CASH_REGISTER.get()).defaultBlockState().setValue(CashRegisterBlock.FACING, Direction.NORTH));
      BlockState door = (BlockState)Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
      h.setBlock(DOOR, (BlockState)door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
      h.setBlock(DOOR.above(), (BlockState)door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
      CashRegisterEntity register = (CashRegisterEntity)h.getBlockEntity(REGISTER);
      register.setDoor(h.absolutePos(DOOR));

      for (String id : stock) {
         register.stock.set(product(id));
      }

      register.open = true;
      return register;
   }

   private static int product(String id) {
      for (int i = 0; i < BarProducts.ALL.size(); i++) {
         if (BarProducts.ALL.get(i).id().equals(id)) {
            return i;
         }
      }

      throw new IllegalArgumentException(id);
   }

   private static BarPatron customer(GameTestHelper h, CashRegisterEntity register, BlockPos at) {
      BarPatron patron = register.spawnCustomer(h.getLevel());
      BlockPos p = h.absolutePos(at);
      patron.moveTo((double)p.getX() + 0.5, (double)p.getY(), (double)p.getZ() + 0.5, 0.0F, 0.0F);
      patron.skipEntrance();
      patron.setStander(false);
      patron.giveOwnSmoke(0);
      return patron;
   }

   @GameTest(
      template = "empty",
      timeoutTicks = 30
   )
   public static void bar_menu_prices_are_realistic(GameTestHelper h) {
      for (BarProducts.Product p : BarProducts.ALL) {
         h.assertTrue(p.whole() > 0 && p.icon().getItem() != Items.AIR, "Product has no price or item: " + p.id());
         if (p.kind() == BarProducts.Kind.SPIRIT) {
            h.assertTrue(p.shot() >= 5 && p.shot() <= 12 && p.whole() >= 30, "Odd spirit prices: " + p.id());
         }

         if (p.kind() == BarProducts.Kind.BEER) {
            h.assertTrue(!p.shots() && p.whole() >= 5 && p.whole() <= 9, "Odd beer price: " + p.id());
         }

         if (p.kind() == BarProducts.Kind.COCKTAIL) {
            h.assertTrue(p.whole() >= 9 && p.whole() <= 16, "Odd cocktail price: " + p.id());
         }
      }

      h.assertTrue(BarProducts.ALL.size() <= 27, "The stock grid holds 27 products");
      h.assertTrue(BarProducts.get(product("tequila")).drinksPerSip() == 1.0F, "A tequila shot should be one standard drink");
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void register_menu_controls_store(GameTestHelper h) {
      CashRegisterEntity register = bar(h);
      register.open = false;
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      CashRegisterMenu menu = new CashRegisterMenu(0, player.getInventory(), register);
      menu.clickMenuButton(player, 0);
      h.assertTrue(register.open && menu.open(), "Open button did not open the store");

      for (int i = 0; i < 30; i++) {
         menu.clickMenuButton(player, 2);
      }

      h.assertTrue(register.capacity == 20, "Customer limit must stop at 20");

      for (int i = 0; i < 30; i++) {
         menu.clickMenuButton(player, 1);
      }

      h.assertTrue(register.capacity == 1, "Customer limit must stop at 1");
      menu.clickMenuButton(player, 100 + product("rum"));
      h.assertTrue(menu.stocked(product("rum")) && register.stocked().size() == 1, "Stock toggle did not add rum");
      menu.clickMenuButton(player, 7);
      h.assertTrue(register.stocked().size() == BarProducts.ALL.size(), "Stock all missed products");
      menu.clickMenuButton(player, 8);
      h.assertTrue(register.stocked().isEmpty(), "Clear left products in stock");
      register.pay(70000);
      register.returnDish(new ItemStack(Items.GLASS_BOTTLE, 3));
      h.assertTrue(menu.till() == 70000, "Menu till is wrong past 32767: " + menu.till());
      menu.clickMenuButton(player, 3);
      int cash = 0;
      int bottles = 0;

      for (ItemStack s : player.getInventory().items) {
         if (s.is((Item)Drunkard.CASH.get())) {
            cash += s.getCount();
         }

         if (s.is(Items.GLASS_BOTTLE)) {
            bottles += s.getCount();
         }
      }

      List<ItemEntity> dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(3.0));

      for (ItemEntity e : dropped) {
         if (e.getItem().is((Item)Drunkard.CASH.get())) {
            cash += e.getItem().getCount();
         }

         if (e.getItem().is(Items.GLASS_BOTTLE)) {
            bottles += e.getItem().getCount();
         }
      }

      h.assertTrue(cash == 70000 && bottles == 3 && register.till == 0, "Collect paid out $" + cash + " and " + bottles + " bottles");
      dropped.forEach(Entity::discard);
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void register_links_door_and_seats(GameTestHelper h) {
      CashRegisterEntity register = bar(h);
      register.door = null;
      h.setBlock(new BlockPos(3, 1, 8), Blocks.OAK_STAIRS.defaultBlockState());
      h.setBlock(new BlockPos(4, 1, 8), (Block)Drunkard.BAR_STOOL.get());
      h.setBlock(new BlockPos(5, 1, 9), Blocks.STONE);
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      Consumer<BlockPos> click = pos -> RegisterLinking.click(
            new RightClickBlock(
               player, InteractionHand.MAIN_HAND, h.absolutePos(pos), new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false)
            )
         );
      RegisterLinking.start(player, h.absolutePos(REGISTER), RegisterLinking.Mode.DOOR);
      click.accept(DOOR.above());
      h.assertTrue(h.absolutePos(DOOR).equals(register.door), "Clicking the upper half did not link the door's lower half");
      BlockPos[] sides = register.doorSides();
      h.assertTrue(sides != null && sides[0].equals(h.absolutePos(DOOR.south())), "Customers must appear outside the door, away from the register");
      RegisterLinking.start(player, h.absolutePos(REGISTER), RegisterLinking.Mode.SEATS);
      click.accept(new BlockPos(3, 1, 8));
      click.accept(new BlockPos(4, 1, 8));
      click.accept(new BlockPos(5, 1, 9));
      h.assertTrue(register.seats.size() == 2, "Stairs and stool should be seats, stone not: " + register.seats.size());
      click.accept(new BlockPos(4, 1, 8));
      h.assertTrue(register.seats.size() == 1, "Clicking a seat again should remove it");
      player.setShiftKeyDown(true);
      click.accept(new BlockPos(3, 1, 8));
      h.assertTrue(
         register.seats.size() == 1 && !player.getPersistentData().contains("DrunkardRegisterLink"), "Sneak-click should end picking without changing seats"
      );
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void customers_come_in_through_the_door_up_to_the_limit(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      register.capacity = 2;

      for (int i = 0; i < 6; i++) {
         register.trySpawn(h.getLevel());
      }

      List<BarPatron> patrons = h.getLevel().getEntitiesOfClass(BarPatron.class, new AABB(h.absolutePos(DOOR)).inflate(4.0));
      h.assertTrue(patrons.size() == 2 && register.patronCount() == 2, "Customer limit not respected: " + patrons.size());
      h.assertTrue(
         patrons.stream().allMatch(p -> p.getZ() > (double)h.absolutePos(DOOR).getZ() + 0.9 && p.state() == BarPatron.State.ARRIVING),
         "Customers did not appear outside the door, walking in"
      );
      h.assertTrue(patrons.stream().allMatch(p -> p.skin() >= 0 && p.skin() < 65536), "Customer skin out of range");
      register.open = false;
      h.assertTrue(register.trySpawn(h.getLevel()) == null, "A closed store let a customer in");
      patrons.forEach(Entity::discard);
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 60
   )
   public static void customers_order_from_stock_and_pay_for_bottles(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      BarPatron patron = customer(h, register, new BlockPos(6, 1, 7));

      for (int i = 0; i < 20; i++) {
         patron.chooseOrder(register);
         h.assertTrue(patron.product().id().equals("pale_lager") && !patron.wantsShot(), "Ordered something out of stock");
      }

      Player player = h.makeMockPlayer(GameType.CREATIVE);
      h.assertTrue(!patron.serve(player) && patron.state() == BarPatron.State.WAITING, "Served a beer the bartender did not have");
      player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
      h.assertTrue(patron.serve(player), "Could not serve the beer");
      h.assertTrue(
         patron.getMainHandItem().is((Item)Drunkard.WINE.get("pale_lager").get()) && patron.state() == BarPatron.State.TO_SEAT,
         "Customer is not holding the beer"
      );
      h.assertTrue(
         player.getInventory().items.stream().noneMatch(s -> s.is((Item)Drunkard.WINE.get("pale_lager").get())),
         "The beer was not taken from the bartender"
      );
      h.assertTrue(register.till >= 6 && register.till <= 7, "A pale lager costs $6 plus a small tip, till: " + register.till);
      patron.discard();
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 120
   )
   public static void shot_orders_take_a_poured_glass(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "tequila");
      BarPatron patron = customer(h, register, new BlockPos(6, 1, 7));
      patron.setOrder(product("tequila"), true);
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      h.assertTrue(patron.serve(player) && patron.state() == BarPatron.State.AWAIT_SHOT, "Shot order should wait for a poured glass");
      h.setBlock(new BlockPos(7, 1, 8), Blocks.SPRUCE_PLANKS);
      h.setBlock(new BlockPos(5, 1, 8), Blocks.SPRUCE_PLANKS);
      h.setBlock(new BlockPos(5, 2, 8), (Block)Drunkard.PLACED_SHOT_GLASS.get());
      h.setBlock(new BlockPos(7, 2, 8), (Block)Drunkard.PLACED_SHOT_GLASS.get());
      ShotGlassEntity wrong = (ShotGlassEntity)h.getBlockEntity(new BlockPos(5, 2, 8));
      wrong.glass = ShotGlassItem.pour(new ItemStack((ItemLike)Drunkard.WINE.get("vodka").get()));
      ShotGlassEntity right = (ShotGlassEntity)h.getBlockEntity(new BlockPos(7, 2, 8));
      right.glass = ShotGlassItem.pour(new ItemStack((ItemLike)Drunkard.WINE.get("tequila").get()));
      h.succeedWhen(() -> {
         h.assertTrue(ShotGlassItem.type(patron.getMainHandItem()).equals("tequila"), "Customer has not taken the tequila shot");
         h.assertBlockNotPresent((Block)Drunkard.PLACED_SHOT_GLASS.get(), new BlockPos(7, 2, 8));
         h.assertBlockPresent((Block)Drunkard.PLACED_SHOT_GLASS.get(), new BlockPos(5, 2, 8));
         h.assertTrue(register.till >= 8, "Shot was not paid for");
         patron.discard();
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 300
   )
   public static void served_customers_sit_on_linked_stairs(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "mead");
      h.setBlock(new BlockPos(2, 1, 9), (BlockState)Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
      register.toggleSeat(h.absolutePos(new BlockPos(2, 1, 9)));
      BarPatron patron = customer(h, register, new BlockPos(6, 1, 7));
      patron.setOrder(product("mead"), false);
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("mead").get()));
      h.assertTrue(patron.serve(player), "Could not serve the mead");
      h.succeedWhen(
         () -> {
            h.assertTrue(
               patron.getVehicle() instanceof SeatEntity,
               "Customer did not sit down: "
                  + patron.state()
                  + " at "
                  + patron.blockPosition()
                  + " seat "
                  + patron.seat()
                  + " target "
                  + h.absolutePos(new BlockPos(2, 1, 9))
                  + " nav done "
                  + patron.getNavigation().isDone()
            );
            h.assertTrue(patron.getVehicle().blockPosition().equals(h.absolutePos(new BlockPos(2, 1, 9))), "Customer sat in the wrong place");
            h.assertTrue(patron.state() == BarPatron.State.ENJOYING, "Seated customer is not drinking");
            patron.stopRiding();
            patron.discard();
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 1200
   )
   public static void customers_drink_and_leave_through_the_door(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_ale");
      BarPatron patron = customer(h, register, new BlockPos(6, 1, 7));
      patron.setOrder(product("pale_ale"), false);
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_ale").get()));
      patron.serve(player);
      patron.setSipsLeft(1);
      patron.setDrunk(40.0F);
      h.succeedWhen(
         () -> {
            h.assertTrue(patron.drunk() > 40.0F, "Sipping did not make the customer drunker");
            h.assertTrue(
               register.dishes.stream().anyMatch(s -> s.is((Item)Drunkard.BEER_MUG.get()))
                  || h.getLevel().getBlockStates(new AABB(h.absolutePos(REGISTER)).inflate(8.0)).anyMatch(s -> s.is((Block)Drunkard.PLACED_BOTTLE.get())),
               "The empty mug was not handed back or left on the bar"
            );
            h.assertTrue(patron.isRemoved(), "Customer did not leave through the door: " + patron.state() + " at " + patron.blockPosition());
            h.assertTrue(!(Boolean)h.getBlockState(DOOR).getValue(DoorBlock.OPEN), "The customer left the door open");
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 500
   )
   public static void closing_the_store_sends_waiting_customers_home(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "vodka");
      BarPatron patron = customer(h, register, new BlockPos(6, 1, 8));
      BarPatron arriving = register.spawnCustomer(h.getLevel());
      register.open = false;
      h.succeedWhen(() -> {
         h.assertTrue(patron.state() == BarPatron.State.LEAVING, "Waiting customer stayed after closing");
         h.assertTrue(arriving.state() == BarPatron.State.LEAVING, "An arriving customer came in after closing");
         patron.discard();
         arriving.discard();
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 400
   )
   public static void drunk_customers_brawl_and_break_it_up(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "whiskey");
      BarPatron a = customer(h, register, new BlockPos(4, 1, 9));
      BarPatron b = customer(h, register, new BlockPos(8, 1, 9));

      for (BarPatron p : new BarPatron[]{a, b}) {
         p.setDrunk(7.0F);
         p.setOrder(product("whiskey"), true);
      }

      register.startFight(a, b);
      h.assertTrue(a.state() == BarPatron.State.FIGHTING && b.state() == BarPatron.State.FIGHTING, "The brawl did not start");
      h.assertTrue(!a.speech().isEmpty() && !b.speech().isEmpty(), "Brawlers should shout at each other");
      h.succeedWhen(
         () -> {
            h.assertTrue(a.state() != BarPatron.State.FIGHTING && b.state() != BarPatron.State.FIGHTING, "The brawl never ended");
            h.assertTrue(
               (a.state() == BarPatron.State.LEAVING || a.isRemoved()) != (b.state() == BarPatron.State.LEAVING || b.isRemoved()),
               "Exactly one brawler should storm off"
            );
            h.assertTrue(a.isRemoved() || a.getHealth() > 0.0F, "A brawl killed a customer");
            a.discard();
            b.discard();
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void seat_clicks_count_once_per_click(GameTestHelper h) {
      CashRegisterEntity register = bar(h);
      h.setBlock(new BlockPos(3, 1, 8), Blocks.OAK_STAIRS.defaultBlockState());
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      RegisterLinking.start(player, h.absolutePos(REGISTER), RegisterLinking.Mode.SEATS);
      BlockPos pos = h.absolutePos(new BlockPos(3, 1, 8));
      BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
      RegisterLinking.click(new RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit));
      RegisterLinking.click(new RightClickBlock(player, InteractionHand.OFF_HAND, pos, hit));
      h.assertTrue(register.seats.size() == 1, "Stairs were added and removed again by one click");
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 60
   )
   public static void stool_customers_face_the_counter_at_player_height(GameTestHelper h) {
      CashRegisterEntity register = bar(h);
      h.setBlock(new BlockPos(5, 1, 6), Blocks.SPRUCE_PLANKS);
      h.setBlock(new BlockPos(5, 1, 7), (BlockState)((Block)Drunkard.BAR_STOOL.get()).defaultBlockState().setValue(SeatBlock.FACING, Direction.EAST));
      BarPatron patron = customer(h, register, new BlockPos(5, 1, 8));
      BlockPos stool = h.absolutePos(new BlockPos(5, 1, 7));
      h.assertTrue(SeatEntity.sit(h.getLevel(), stool, patron, h.absolutePos(REGISTER)), "Customer could not sit on the stool");
      h.assertTrue(
         Direction.fromYRot((double)patron.getVehicle().getYRot()) == Direction.NORTH,
         "Stool customer faces " + Direction.fromYRot((double)patron.getVehicle().getYRot())
      );
      patron.getVehicle().positionRider(patron);
      double expected = (double)stool.getY() + 0.9375 - 0.25 - 0.35;
      h.assertTrue(Math.abs(patron.getY() - expected) < 0.01, "Customer sits at " + patron.getY() + ", a player would sit at " + expected);
      patron.stopRiding();
      patron.discard();
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void customers_arrive_in_groups(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      register.capacity = 10;
      List<BarPatron> group = register.spawnGroup(h.getLevel(), 3);
      h.assertTrue(group.size() == 3 && group.stream().allMatch(p -> p.group().equals(group.get(0).group())), "A group should share one group id");
      h.assertTrue(register.patronCount() == 3, "Group not tracked by the register");
      BarPatron solo = register.spawnCustomer(h.getLevel());
      h.assertTrue(!solo.group().equals(group.get(0).group()), "A lone customer joined someone else's group");
      group.forEach(Entity::discard);
      solo.discard();
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 60
   )
   public static void drinks_can_be_handed_straight_over(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "margarita", "tequila");
      BarPatron patron = customer(h, register, new BlockPos(6, 1, 7));
      patron.setOrder(product("margarita"), false);
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)Drunkard.MARGARITA.get()));
      patron.interact(player, InteractionHand.MAIN_HAND);
      h.assertTrue(
         patron.getMainHandItem().is((Item)Drunkard.MARGARITA.get()) && player.getMainHandItem().isEmpty() && register.till == 12,
         "Handing over the margarita did not serve it"
      );
      BarPatron second = customer(h, register, new BlockPos(5, 1, 8));
      second.setOrder(product("tequila"), true);
      player.setItemInHand(InteractionHand.MAIN_HAND, ShotGlassItem.pour(new ItemStack((ItemLike)Drunkard.WINE.get("tequila").get())));
      second.interact(player, InteractionHand.MAIN_HAND);
      h.assertTrue(ShotGlassItem.type(second.getMainHandItem()).equals("tequila") && register.till == 20, "Handing over the poured shot did not serve it");
      patron.discard();
      second.discard();
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 200
   )
   public static void seated_customers_order_the_next_round_from_their_seat(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      h.setBlock(new BlockPos(3, 1, 9), (BlockState)Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
      register.toggleSeat(h.absolutePos(new BlockPos(3, 1, 9)));
      BarPatron patron = customer(h, register, new BlockPos(3, 1, 8));
      h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 9)), patron, h.absolutePos(REGISTER)), "Could not seat the customer");
      patron.setOrder(product("pale_lager"), false);
      patron.markOrdered();
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
      h.assertTrue(patron.serve(player) && patron.state() == BarPatron.State.ENJOYING, "A seated customer served at their seat should keep drinking there");
      patron.setSipsLeft(1);
      h.runAfterDelay(
         10L,
         () -> {
            patron.endLinger(true);
            h.runAfterDelay(
               5L,
               () -> {
                  h.assertTrue(
                     patron.state() == BarPatron.State.WAITING && patron.isPassenger(),
                     "Customer should order again without getting up: " + patron.state() + " seated " + patron.isPassenger()
                  );
                  h.assertTrue(patron.product() != null && !patron.speech().isEmpty(), "Customer did not call for the next round");
                  patron.stopRiding();
                  patron.discard();
                  h.succeed();
               }
            );
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 300
   )
   public static void customers_stay_on_their_side_of_the_counter(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "vodka");
      BlockPos reg = h.absolutePos(REGISTER);
      BlockPos door = h.absolutePos(DOOR);
      Direction toDoor = Direction.getNearest((float)(door.getX() - reg.getX()), 0.0F, (float)(door.getZ() - reg.getZ()));
      h.getLevel().setBlock(reg, (BlockState)((Block)Drunkard.CASH_REGISTER.get()).defaultBlockState().setValue(CashRegisterBlock.FACING, toDoor), 3);
      register = (CashRegisterEntity)h.getLevel().getBlockEntity(reg);
      register.setDoor(door);
      register.stock.set(product("vodka"));
      register.open = true;
      h.assertTrue(
         register.customerSide() == toDoor,
         "Customer side should be the door's side (" + toDoor + "), not taken from the register's facing: " + register.customerSide()
      );
      h.assertTrue(
         register.front().equals(reg.relative(toDoor).below()),
         "Ordering spot on the wrong side: "
            + register.front()
            + " register "
            + reg
            + " standable(expected)="
            + register.standable(reg.relative(toDoor).below())
            + " at="
            + h.getLevel().getBlockState(reg.relative(toDoor).below())
            + " above="
            + h.getLevel().getBlockState(reg.relative(toDoor))
            + " below="
            + h.getLevel().getBlockState(reg.relative(toDoor).below(2))
      );
      BlockPos behind = reg.relative(toDoor.getOpposite(), 2).below();
      h.assertTrue(register.behindCounter(behind) && !register.behindCounter(reg.relative(toDoor, 2).below()), "Behind-the-bar area is wrong");
      BarPatron patron = customer(h, register, h.relativePos(behind));
      h.succeedWhen(() -> {
         h.assertTrue(!patron.behindTheBar(), "Customer stayed behind the bar at " + patron.blockPosition());
         patron.discard();
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 600
   )
   public static void friends_hold_real_conversations(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      h.setBlock(new BlockPos(4, 1, 9), Blocks.OAK_STAIRS.defaultBlockState());
      h.setBlock(new BlockPos(5, 1, 9), Blocks.OAK_STAIRS.defaultBlockState());
      register.toggleSeat(h.absolutePos(new BlockPos(4, 1, 9)));
      register.toggleSeat(h.absolutePos(new BlockPos(5, 1, 9)));
      BarPatron a = customer(h, register, new BlockPos(4, 1, 8));
      BarPatron b = customer(h, register, new BlockPos(5, 1, 8));
      b.joinGroup(a);

      for (BarPatron p : new BarPatron[]{a, b}) {
         h.assertTrue(
            SeatEntity.sit(h.getLevel(), p == a ? h.absolutePos(new BlockPos(4, 1, 9)) : h.absolutePos(new BlockPos(5, 1, 9)), p, h.absolutePos(REGISTER)),
            "Could not seat a customer"
         );
         p.setOrder(product("pale_lager"), false);
         p.markOrdered();
         Player player = h.makeMockPlayer(GameType.CREATIVE);
         player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
         p.serve(player);
         p.setSipsLeft(10);
         p.chatNow();
      }

      HashMap<BarPatron, Set<String>> said = new HashMap<>();
      HashSet<BarTalk.Emote> emotes = new HashSet<>();

      for (int i = 1; i < 580; i += 2) {
         h.runAtTickTime((long)i, () -> {
            for (BarPatron p : new BarPatron[]{a, b}) {
               if (!p.speech().isEmpty()) {
                  said.computeIfAbsent(p, k -> new HashSet<>()).add(p.speech());
               }

               emotes.add(p.emote());
            }
         });
      }

      h.runAtTickTime(590L, () -> {
         h.assertTrue(said.containsKey(a) && said.containsKey(b), "Both friends should talk: " + said);
         int lines = said.values().stream().mapToInt(Set::size).sum();
         h.assertTrue(lines >= 3, "A conversation should go back and forth (at least three lines): " + said);
         emotes.remove(BarTalk.Emote.NONE);
         h.assertTrue(!emotes.isEmpty(), "Talking should come with body language");
         a.stopRiding();
         b.stopRiding();
         a.discard();
         b.discard();
         h.succeed();
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 400
   )
   public static void customers_climb_onto_raised_stools_and_order_from_them(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      h.setBlock(new BlockPos(3, 1, 9), Blocks.OAK_FENCE);
      h.setBlock(new BlockPos(3, 2, 9), (BlockState)Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.TOP));
      BlockPos seat = h.absolutePos(new BlockPos(3, 2, 9));
      register.toggleSeat(seat);
      h.assertTrue(register.seats.size() == 1, "An upside-down stair should count as a seat");
      BarPatron patron = customer(h, register, new BlockPos(6, 1, 10));
      h.succeedWhen(
         () -> {
            h.assertTrue(
               patron.getVehicle() instanceof SeatEntity && patron.getVehicle().blockPosition().equals(seat),
               "Customer did not climb onto the raised stool: at " + patron.blockPosition() + " seat " + patron.seat()
            );
            h.assertTrue(
               patron.state() == BarPatron.State.WAITING && patron.unserved() && !patron.queuing(),
               "A seated customer should order from the seat, not queue at the register"
            );
            patron.stopRiding();
            patron.discard();
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 1200
   )
   public static void tips_go_in_the_till_not_on_the_bar(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_ale");

      for (int i = 0; i < 4; i++) {
         BarPatron patron = customer(h, register, new BlockPos(4 + i, 1, 7));
         patron.setOrder(product("pale_ale"), false);
         Player player = h.makeMockPlayer(GameType.CREATIVE);
         player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_ale").get()));
         patron.serve(player);
         patron.setSipsLeft(1);
      }

      h.succeedWhen(
         () -> {
            List<BarPatron> patrons = h.getLevel().getEntitiesOfClass(BarPatron.class, new AABB(h.absolutePos(REGISTER)).inflate(12.0));
            h.assertTrue(patrons.stream().noneMatch(p -> p.sipsLeft() > 0), "Still drinking");
            List<ItemEntity> cash = h.getLevel()
               .getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(REGISTER)).inflate(12.0), e -> e.getItem().is((Item)Drunkard.CASH.get()));
            h.assertTrue(cash.isEmpty(), "Tips were dropped as items");
            h.assertTrue(register.till >= 4 * BarProducts.get(product("pale_ale")).price(false), "Four beers should be paid for: " + register.till);
            patrons.forEach(Entity::discard);
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 600
   )
   public static void smokers_rest_their_smoke_in_a_cigar_holder(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "cigarette_pack");
      h.setBlock(new BlockPos(3, 1, 8), Blocks.SPRUCE_PLANKS);
      h.setBlock(new BlockPos(3, 2, 8), (Block)Drunkard.CIGAR_HOLDER.get());
      h.setBlock(new BlockPos(3, 1, 9), (Block)Drunkard.BAR_STOOL.get());
      register.toggleSeat(h.absolutePos(new BlockPos(3, 1, 9)));
      BarPatron patron = customer(h, register, new BlockPos(3, 1, 10));
      h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 9)), patron, h.absolutePos(REGISTER)), "Could not seat the smoker");
      patron.setOrder(product("cigarette_pack"), false);
      patron.markOrdered();
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      player.getAbilities().instabuild = true;
      h.assertTrue(patron.serve(player) && patron.getMainHandItem().getItem() instanceof CigarItem, "Smoker was not handed a cigarette");
      patron.setSipsLeft(6);
      CigarHolderEntity holder = (CigarHolderEntity)h.getBlockEntity(new BlockPos(3, 2, 8));
      boolean[] rested = new boolean[1];
      boolean[] puffedAgain = new boolean[1];

      for (int i = 1; i < 560; i++) {
         h.runAtTickTime((long)i, () -> {
            if (CigarItem.lit(holder.cigar) && patron.getMainHandItem().isEmpty()) {
               rested[0] = true;
            } else if (rested[0] && patron.getMainHandItem().getItem() instanceof CigarItem) {
               puffedAgain[0] = true;
            }
         });
      }

      h.runAtTickTime(580L, () -> {
         h.assertTrue(rested[0], "The lit cigarette was never rested in the holder");
         h.assertTrue(puffedAgain[0], "The smoker never picked it back up for a puff");
         patron.stopRiding();
         patron.discard();
         h.succeed();
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 500
   )
   public static void bar_events_put_on_a_show_for_the_room(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      BlockPos[] seats = new BlockPos[]{new BlockPos(4, 1, 9), new BlockPos(5, 1, 9), new BlockPos(9, 1, 9)};
      BarPatron[] cast = new BarPatron[3];

      for (int i = 0; i < 3; i++) {
         h.setBlock(seats[i], Blocks.OAK_STAIRS.defaultBlockState());
         register.toggleSeat(h.absolutePos(seats[i]));
         cast[i] = customer(h, register, seats[i].north());
         h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(seats[i]), cast[i], h.absolutePos(REGISTER)), "Could not seat a customer");
         cast[i].setOrder(product("pale_lager"), false);
         cast[i].markOrdered();
         Player player = h.makeMockPlayer(GameType.CREATIVE);
         player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
         cast[i].serve(player);
         cast[i].setSipsLeft(10);
      }

      h.assertTrue(cast[0].startEvent(BarTalk.EVENTS.get(0)), "The arm wrestle did not start");
      h.assertTrue(cast[0].talking() && cast[1].talking() && !cast[2].talking(), "The two neighbours should wrestle; the third watches");
      HashSet<BarPatron> wrestled = new HashSet<>();
      boolean[] shouted = new boolean[1];

      for (int i = 1; i < 440; i++) {
         h.runAtTickTime((long)i, () -> {
            for (int k = 0; k < 2; k++) {
               if (cast[k].emote() == BarTalk.Emote.ARM_WRESTLE) {
                  wrestled.add(cast[k]);
               }
            }

            if (!cast[2].speech().isEmpty()) {
               shouted[0] = true;
            }
         });
      }

      h.runAtTickTime(450L, () -> {
         h.assertTrue(wrestled.size() == 2, "Both should arm wrestle: " + wrestled.size());
         h.assertTrue(shouted[0], "The crowd should cheer them on");

         for (BarPatron p : cast) {
            p.stopRiding();
            p.discard();
         }

         h.succeed();
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void stair_stools_side_by_side_face_the_bar(GameTestHelper h) {
      CashRegisterEntity register = bar(h);

      for (int x = 3; x <= 5; x++) {
         h.setBlock(new BlockPos(x, 1, 6), Blocks.SPRUCE_PLANKS);
      }

      BlockState stair = (BlockState)Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
      h.setBlock(new BlockPos(4, 1, 7), stair);
      h.setBlock(new BlockPos(5, 1, 7), stair);
      h.setBlock(new BlockPos(3, 1, 7), Blocks.OAK_FENCE);
      h.setBlock(new BlockPos(3, 2, 7), (BlockState)stair.setValue(StairBlock.HALF, Half.TOP));
      h.setBlock(new BlockPos(3, 2, 6), Blocks.SPRUCE_PLANKS);

      for (BlockPos pos : new BlockPos[]{new BlockPos(4, 1, 7), new BlockPos(5, 1, 7), new BlockPos(3, 2, 7)}) {
         BarPatron patron = customer(h, register, new BlockPos(pos.getX(), 1, 8));
         h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(pos), patron, h.absolutePos(REGISTER)), "Could not sit at " + pos);
         patron.getVehicle().positionRider(patron);
         Direction facing = Direction.fromYRot((double)patron.getVehicle().getYRot());
         h.assertTrue(facing == Direction.NORTH, "Customer at " + pos + " faces " + facing + ", not the bar");
         h.assertTrue(Math.abs(Mth.wrapDegrees(patron.yBodyRot - patron.getVehicle().getYRot())) < 1.0F, "Body not turned with the seat at " + pos);
         patron.stopRiding();
         patron.discard();
      }

      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void stair_stools_face_the_bar_at_any_height(GameTestHelper h) {
      CashRegisterEntity register = bar(h);
      Direction N = Direction.NORTH;
      BlockState stair = (BlockState)Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, N);
      h.setBlock(new BlockPos(3, 1, 6), Blocks.SPRUCE_PLANKS);
      h.setBlock(new BlockPos(3, 1, 7), Blocks.OAK_FENCE);
      h.setBlock(new BlockPos(3, 2, 7), stair);
      h.setBlock(new BlockPos(7, 1, 9), stair);
      h.setBlock(new BlockPos(10, 1, 6), (BlockState)Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.TOP));
      h.setBlock(new BlockPos(10, 1, 7), (BlockState)stair.setValue(StairBlock.FACING, Direction.EAST));

      for (BlockPos pos : new BlockPos[]{new BlockPos(3, 2, 7), new BlockPos(7, 1, 9), new BlockPos(10, 1, 7)}) {
         register.toggleSeat(h.absolutePos(pos));
         BarPatron patron = customer(h, register, new BlockPos(pos.getX(), 1, 10));
         h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(pos), patron, h.absolutePos(REGISTER)), "Could not sit at " + pos);
         Direction facing = Direction.fromYRot((double)patron.getVehicle().getYRot());
         h.assertTrue(facing == N, "Customer at " + pos + " faces " + facing + ", not the bar");
         patron.stopRiding();
         patron.discard();
      }

      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 700
   )
   public static void a_full_row_of_fence_post_stools_fills_up(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      Direction S = Direction.SOUTH;
      ArrayList<BlockPos> stools = new ArrayList<>();

      for (int x = 1; x <= 5; x++) {
         h.setBlock(new BlockPos(x, 1, 6), Blocks.SPRUCE_PLANKS);
         h.setBlock(new BlockPos(x, 2, 6), Blocks.SPRUCE_PLANKS);
         h.setBlock(new BlockPos(x, 1, 7), Blocks.OAK_FENCE);
         h.setBlock(new BlockPos(x, 2, 7), (BlockState)Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, S));
         stools.add(new BlockPos(x, 2, 7));
      }

      h.setBlock(new BlockPos(9, 1, 9), Blocks.SPRUCE_PLANKS);
      h.setBlock(new BlockPos(9, 2, 9), Blocks.WHITE_CARPET);
      h.setBlock(new BlockPos(10, 1, 9), (BlockState)Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));

      for (int z : new int[]{8, 10}) {
         h.setBlock(
            new BlockPos(10, 1, z),
            (BlockState)((BlockState)Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true))
               .setValue(TrapDoorBlock.FACING, z == 8 ? Direction.NORTH : S)
         );
      }

      for (BlockPos s : stools) {
         register.toggleSeat(h.absolutePos(s));
      }

      register.toggleSeat(h.absolutePos(new BlockPos(10, 1, 9)));
      ArrayList<BarPatron> patrons = new ArrayList<>();
      BlockPos[] starts = new BlockPos[]{
         new BlockPos(1, 1, 10), new BlockPos(2, 1, 11), new BlockPos(3, 1, 10), new BlockPos(4, 1, 11), new BlockPos(5, 1, 10), new BlockPos(11, 1, 11)
      };

      for (BlockPos start : starts) {
         BarPatron patron = customer(h, register, start);
         patron.setOrder(product("pale_lager"), false);
         Player player = h.makeMockPlayer(GameType.CREATIVE);
         player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
         patron.serve(player);
         patron.setSipsLeft(40);
         patrons.add(patron);
      }

      h.succeedWhen(
         () -> {
            for (BarPatron patronxx : patrons) {
               h.assertTrue(
                  patronxx.getVehicle() instanceof SeatEntity,
                  "Customer did not get a seat: at " + patronxx.blockPosition() + " seat " + patronxx.seat() + " state " + patronxx.state()
               );
               BlockPos seat = patronxx.getVehicle().blockPosition();
               Direction facing = Direction.fromYRot((double)patronxx.getVehicle().getYRot());
               Direction expected = seat.equals(h.absolutePos(new BlockPos(10, 1, 9))) ? Direction.WEST : Direction.NORTH;
               h.assertTrue(facing == expected, "Customer at " + seat + " faces " + facing + ", expected " + expected);
            }

            for (BarPatron patronx : patrons) {
               patronx.stopRiding();
               patronx.discard();
            }
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 900
   )
   public static void customers_walk_round_the_bar_to_a_free_seat(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");

      for (int x = 0; x <= 11; x++) {
         h.setBlock(new BlockPos(x, 1, 8), Blocks.SPRUCE_PLANKS);
         h.setBlock(new BlockPos(x, 2, 8), Blocks.SPRUCE_PLANKS);
      }

      h.setBlock(new BlockPos(2, 1, 6), Blocks.OAK_STAIRS.defaultBlockState());
      register.toggleSeat(h.absolutePos(new BlockPos(2, 1, 6)));
      BarPatron patron = customer(h, register, new BlockPos(1, 1, 10));
      patron.setOrder(product("pale_lager"), false);
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
      patron.serve(player);
      patron.setSipsLeft(40);
      h.succeedWhen(
         () -> {
            h.assertTrue(
               patron.getVehicle() instanceof SeatEntity,
               "Customer didn't make it round the bar to the seat: at "
                  + patron.blockPosition()
                  + " seat "
                  + patron.seat()
                  + " "
                  + patron.state()
                  + " "
                  + patron.navDebug()
            );
            patron.stopRiding();
            patron.discard();
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 100
   )
   public static void the_register_keeps_the_ledger_and_the_pubs_name(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      int rep = register.reputation;
      BarPatron quick = customer(h, register, new BlockPos(6, 1, 8));
      quick.setOrder(product("pale_lager"), false);
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
      h.assertTrue(quick.serve(player), "Could not serve");
      int price = BarProducts.get(product("pale_lager")).price(false);
      h.assertTrue(register.servedToday == 1 && register.takingsToday == price, "Ledger: " + register.servedToday + " served, $" + register.takingsToday);
      h.assertTrue(register.reputation == rep + 2, "Quick service should help the pub's name: " + register.reputation);
      BarPatron slow = customer(h, register, new BlockPos(4, 1, 9));
      slow.losePatience();
      h.runAfterDelay(
         3L,
         () -> {
            h.assertTrue(slow.state() == BarPatron.State.LEAVING, "An impatient customer should walk out: " + slow.state());
            h.assertTrue(
               register.walkoutsToday == 1 && register.reputation == rep - 2,
               "A walk-out should be counted and hurt the pub's name: " + register.walkoutsToday + " / " + register.reputation
            );
            quick.discard();
            slow.discard();
            h.succeed();
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 60
   )
   public static void regulars_come_back_for_the_usual(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager", "vodka", "mead");
      register.reputation = 100;
      BarPatron first = customer(h, register, new BlockPos(6, 1, 8));
      first.setOrder(product("mead"), false);
      first.setVisits(2);
      register.remember(first);
      h.assertTrue(
         register.regulars.size() == 1 && register.regulars.get(0).name().equals(first.name()),
         "A customer who stayed for a couple of rounds should become a regular"
      );
      String name = first.name();
      int skin = first.skin();
      first.discard();
      BarPatron back = null;

      for (int i = 0; i < 80 && back == null; i++) {
         BarPatron p = register.spawnCustomer(h.getLevel());
         if (p.regular()) {
            back = p;
         } else {
            p.discard();
         }
      }

      h.assertTrue(back != null, "The regular never came back");
      h.assertTrue(back.name().equals(name) && back.skin() == skin, "The regular came back as someone else: " + back.name());
      boolean usual = false;

      for (int ix = 0; ix < 40 && !usual; ix++) {
         back.chooseOrder(register);
         usual = back.product().id().equals("mead") && back.orderLine().length() > 0;
      }

      h.assertTrue(usual, "A regular should usually order their usual");
      back.discard();
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void rowdy_customers_can_be_shown_out(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      int rep = register.reputation;
      BarPatron drunk = customer(h, register, new BlockPos(5, 1, 8));
      drunk.setDrunk(8.0F);
      drunk.showOut();
      h.assertTrue(drunk.state() == BarPatron.State.LEAVING && register.reputation == rep, "A drunk shown out should leave without a fuss");
      BarPatron sober = customer(h, register, new BlockPos(7, 1, 8));
      sober.showOut();
      h.assertTrue(sober.state() == BarPatron.State.LEAVING && register.reputation == rep - 2, "Throwing out a sober customer should cost some reputation");
      drunk.discard();
      sober.discard();
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 600
   )
   public static void customers_use_stools_past_the_end_of_the_counter(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");

      for (int x = 3; x <= 9; x++) {
         h.setBlock(new BlockPos(x, 1, 6), Blocks.SPRUCE_PLANKS);
      }

      BlockPos stool = new BlockPos(10, 1, 5);
      h.setBlock(stool, Blocks.OAK_STAIRS.defaultBlockState());
      register.toggleSeat(h.absolutePos(stool));
      h.assertTrue(
         !register.behindCounter(h.absolutePos(stool)) && register.behindCounter(h.absolutePos(new BlockPos(6, 1, 5))),
         "Only the stretch behind the counter is the bartender's side"
      );
      BarPatron patron = customer(h, register, new BlockPos(6, 1, 9));
      patron.setOrder(product("pale_lager"), false);
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
      patron.serve(player);
      patron.setSipsLeft(40);
      h.succeedWhen(
         () -> {
            h.assertTrue(
               patron.getVehicle() instanceof SeatEntity && patron.getVehicle().blockPosition().equals(h.absolutePos(stool)),
               "Customer did not take the stool past the end of the counter: at " + patron.blockPosition() + " seat " + patron.seat()
            );
            patron.stopRiding();
            patron.discard();
         }
      );
   }

   @GameTest(
      template = "empty",
      timeoutTicks = 40
   )
   public static void empty_mugs_and_glasses_can_be_set_down(GameTestHelper h) {
      h.setBlock(POS.below(), Blocks.STONE);
      h.setBlock(POS.below().east(), Blocks.STONE);
      FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "mug-test"));
      player.setShiftKeyDown(true);
      int i = 0;

      for (Item item : new Item[]{(Item)Drunkard.BEER_MUG.get(), (Item)Drunkard.MARTINI_GLASS.get()}) {
         boolean var10001;
         label17: {
            BlockPos floor = h.absolutePos(POS.below().east(i));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false);
            player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            if (h.getBlockEntity(POS.east(i)) instanceof BottleEntity b && b.bottle.is(item)) {
               var10001 = true;
               break label17;
            }

            var10001 = false;
         }

         h.assertTrue(var10001, "Could not set down " + item);
         BottleEntity placed = (BottleEntity)h.getBlockEntity(POS.east(i));
         BottleEntity copy = new BottleEntity(placed.getBlockPos(), placed.getBlockState());
         copy.loadWithComponents(placed.getUpdateTag(h.getLevel().registryAccess()), h.getLevel().registryAccess());
         h.assertTrue(copy.bottle.is(item), "A placed " + item + " vanished when its data was loaded (the client would see nothing)");
         i++;
      }

      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void the_safe_keeps_the_money_and_buys_upgrades_and_supplies(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      h.setBlock(new BlockPos(9, 1, 3), (Block)Drunkard.SAFE.get());
      SafeEntity safe = (SafeEntity)h.getBlockEntity(new BlockPos(9, 1, 3));
      Player owner = h.makeMockPlayer(GameType.CREATIVE);
      safe.owner = owner.getUUID();
      Player stranger = h.makeMockPlayer(GameType.CREATIVE);
      h.assertTrue(safe.mayUse(owner) && !safe.mayUse(stranger), "Only the owner may use the safe");
      owner.getInventory().add(new ItemStack((ItemLike)Drunkard.CASH.get(), 64));
      owner.getInventory().add(new ItemStack((ItemLike)Drunkard.CASH.get(), 40));
      safe.button(owner, 0);
      h.assertTrue(
         safe.balance == 104 && owner.getInventory().items.stream().noneMatch(s -> s.is((Item)Drunkard.CASH.get())), "Deposit failed: " + safe.balance
      );
      safe.button(stranger, 4);
      h.assertTrue(safe.balance == 104, "A stranger took money out of the safe");
      safe.button(owner, 2);
      h.assertTrue(safe.balance == 94 && owner.getInventory().countItem((Item)Drunkard.CASH.get()) == 10, "Withdraw failed");
      register.till = 500;
      safe.button(owner, 5);
      h.assertTrue(safe.balance == 594 && register.till == 0, "The till did not move into the safe: " + safe.balance + " / " + register.till);
      safe.button(owner, 100 + BarUpgrades.Upgrade.COMFORT.ordinal());
      h.assertTrue(
         register.level(BarUpgrades.Upgrade.COMFORT) == 1 && safe.balance == 194,
         "Upgrade not bought: " + register.level(BarUpgrades.Upgrade.COMFORT) + " / " + safe.balance
      );
      int before = safe.balance;
      safe.button(owner, 100 + BarUpgrades.Upgrade.BARKEEP.ordinal());
      h.assertTrue(register.level(BarUpgrades.Upgrade.BARKEEP) == 0 && safe.balance == before, "An upgrade was bought without the money");
      safe.button(owner, 200 + product("pale_lager"));
      h.assertTrue(owner.getInventory().countItem((Item)Drunkard.WINE.get("pale_lager").get()) == 0 && safe.balance == before, "Supplies were sold before the deal");
      safe.supplier = true;
      safe.button(owner, 200 + product("pale_lager"));
      h.assertTrue(
         owner.getInventory().countItem((Item)Drunkard.WINE.get("pale_lager").get()) == 1
            && safe.balance == before - BarUpgrades.supplies().get(product("pale_lager")).price(),
         "Supply not delivered"
      );
      safe.balance = 10000;
      safe.button(owner, 100 + BarUpgrades.Upgrade.PREMISES.ordinal());
      h.assertTrue(register.maxCapacity() == 23, "Bigger premises did not raise the limit");
      SafeEntity loaded = new SafeEntity(safe.getBlockPos(), safe.getBlockState());
      loaded.loadWithComponents(safe.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
      h.assertTrue(loaded.balance == safe.balance && owner.getUUID().equals(loaded.owner), "The safe lost its money or owner on save");
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 60
   )
   public static void staff_clear_empties(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      h.setBlock(new BlockPos(5, 1, 6), Blocks.SPRUCE_PLANKS);
      h.setBlock(new BlockPos(5, 2, 6), (Block)Drunkard.PLACED_BOTTLE.get());
      ((BottleEntity)h.getBlockEntity(new BlockPos(5, 2, 6))).bottle = new ItemStack((ItemLike)Drunkard.BEER_MUG.get());
      h.setBlock(new BlockPos(7, 1, 6), Blocks.SPRUCE_PLANKS);
      h.setBlock(new BlockPos(7, 2, 6), (Block)Drunkard.PLACED_BOTTLE.get());
      ((BottleEntity)h.getBlockEntity(new BlockPos(7, 2, 6))).bottle = new ItemStack((ItemLike)Drunkard.WINE.get("vodka").get());
      register.collectEmpties();
      h.assertBlockNotPresent((Block)Drunkard.PLACED_BOTTLE.get(), new BlockPos(5, 2, 6));
      h.assertBlockPresent((Block)Drunkard.PLACED_BOTTLE.get(), new BlockPos(7, 2, 6));
      h.assertTrue(register.dishes.stream().anyMatch(s -> s.is((Item)Drunkard.BEER_MUG.get())), "Clearing up did not put the mug in the register");
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 60
   )
   public static void conversations_have_consequences(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      BarPatron patron = customer(h, register, new BlockPos(6, 1, 9));
      patron.setOrder(product("pale_lager"), false);
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
      patron.serve(player);
      patron.setSipsLeft(20);
      int till = register.till;
      BarDialogue.Node slow = BarDialogue.get(BarDialogue.index("slow"));
      int next = patron.answer(player, slow, 1);
      h.assertTrue(next == BarDialogue.index("slow_free") && BarDialogue.get(next).end(), "The free round should lead to its reply");
      h.assertTrue(
         register.till < till && patron.rapport() == 3,
         "A free round should refund the drink and win them over: till " + register.till + ", rapport " + patron.rapport()
      );
      h.assertTrue(!patron.speech().isEmpty(), "They should say their reply out loud");
      int silence = patron.answer(player, BarDialogue.get(BarDialogue.index("hello")), 90);
      h.assertTrue(silence == BarDialogue.index("silence"), "Running out of time should count as saying nothing");
      BarPatron drunk = customer(h, register, new BlockPos(4, 1, 9));
      drunk.setDrunk(9.0F);
      drunk.setOrder(product("pale_lager"), false);
      Player p2 = h.makeMockPlayer(GameType.CREATIVE);
      p2.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
      drunk.serve(p2);
      h.assertTrue(drunk.openingNode(player) == BarDialogue.index("drunk"), "A drunk should open with the drunk talk");
      drunk.answer(player, BarDialogue.get(BarDialogue.index("drunk")), 0);
      drunk.setSipsLeft(0);
      BarPatron door = customer(h, register, new BlockPos(8, 1, 9));
      door.answer(player, BarDialogue.get(BarDialogue.index("slow")), 2);
      h.assertTrue(door.state() == BarPatron.State.LEAVING && door.rapport() < 0, "Showing them the door should send them off unhappy");
      h.assertTrue(door.openingNode(player) == BarDialogue.index("cold"), "Someone you upset should be cold with you next time");
      patron.discard();
      drunk.discard();
      door.discard();
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 200
   )
   public static void at_a_tall_bar_customers_sit_to_order(GameTestHelper h) {
      bar(h, "pale_lager");
      h.setBlock(new BlockPos(6, 2, 6), Blocks.SPRUCE_PLANKS);
      BlockPos reg = new BlockPos(6, 3, 6);
      h.setBlock(reg, (BlockState)((Block)Drunkard.CASH_REGISTER.get()).defaultBlockState().setValue(CashRegisterBlock.FACING, Direction.NORTH));
      CashRegisterEntity register = (CashRegisterEntity)h.getBlockEntity(reg);
      register.setDoor(h.absolutePos(DOOR));
      register.stock.set(product("pale_lager"));
      register.open = true;
      h.assertTrue(register.tallBar(), "A register two blocks above the floor should count as a tall bar");
      BarPatron standing = customer(h, register, new BlockPos(6, 1, 9));
      h.runAfterDelay(80L, () -> {
         h.assertTrue(standing.distanceToSqr(Vec3.atBottomCenterOf(register.front())) > 2.0, "With no seat free, nobody should queue at a tall bar's register");
         BlockPos stool = new BlockPos(1, 1, 10);
         h.setBlock(stool, (Block)Drunkard.BAR_STOOL.get());
         register.toggleSeat(h.absolutePos(stool));
         h.succeedWhen(() -> {
            h.assertTrue(standing.getVehicle() instanceof SeatEntity && standing.unserved(), "The customer should sit down to order at a tall bar");
            standing.stopRiding();
            standing.discard();
         });
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 800
   )
   public static void a_hired_barkeep_pours_pints_but_never_sells_bottles(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager", "vodka");

      for (int x = 3; x <= 9; x++) {
         if (x != 6) {
            h.setBlock(new BlockPos(x, 1, 6), Blocks.SPRUCE_PLANKS);
         }
      }

      h.setBlock(new BlockPos(8, 1, 6), (Block)Drunkard.KEG.get());
      KegEntity keg = (KegEntity)h.getBlockEntity(new BlockPos(8, 1, 6));
      keg.beer = "pale_lager";
      keg.servings = 5;
      h.setBlock(new BlockPos(8, 2, 6), (BlockState)((Block)Drunkard.TAP_TOWER.get()).defaultBlockState().setValue(TapTowerBlock.FACING, Direction.SOUTH));
      h.setBlock(new BlockPos(9, 1, 3), (BlockState)((Block)Drunkard.GLASS_SHELF.get()).defaultBlockState().setValue(GlassShelfBlock.FACING, Direction.SOUTH));
      GlassShelfEntity shelf = (GlassShelfEntity)h.getBlockEntity(new BlockPos(9, 1, 3));
      shelf.insert(new ItemStack((ItemLike)Drunkard.BEER_MUG.get(), 4), 0);
      BlockPos[] stools = new BlockPos[]{new BlockPos(7, 1, 7), new BlockPos(5, 1, 7)};
      BarPatron[] who = new BarPatron[2];

      for (int i = 0; i < 2; i++) {
         h.setBlock(stools[i], (Block)Drunkard.BAR_STOOL.get());
         register.toggleSeat(h.absolutePos(stools[i]));
         who[i] = customer(h, register, stools[i].south());
         h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(stools[i]), who[i], h.absolutePos(REGISTER)), "Could not seat a customer");
         who[i].setOrder(product(i == 0 ? "pale_lager" : "vodka"), false);
         who[i].markOrdered();
      }

      register.upgrades[BarUpgrades.Upgrade.BARKEEP.ordinal()] = 1;
      register.keepStaff(h.getLevel());
      List<BarWorker> staff = register.staff(h.getLevel());
      h.assertTrue(staff.size() == 1, "Hiring a barkeep should put one behind the bar");
      int till = register.till;
      BarWorker worker = staff.get(0);
      boolean[] climbed = new boolean[]{false};

      for (int i = 1; i < 700; i += 5) {
         h.runAtTickTime(
            (long)i,
            () -> {
               if (worker.getY() > (double)h.absolutePos(new BlockPos(0, 1, 0)).getY() + 0.6
                  || worker.getZ() > (double)h.absolutePos(new BlockPos(0, 0, 6)).getZ()) {
                  climbed[0] = true;
               }
            }
         );
      }

      h.succeedWhen(
         () -> {
            h.assertTrue(!climbed[0], "The barkeep climbed onto something or left the bartender's side");
            h.assertTrue(
               !who[0].unserved() && who[0].getMainHandItem().is((Item)Drunkard.WINE.get("pale_lager").get()),
               "The barkeep did not serve the pint: " + who[0].state()
            );
            h.assertTrue(keg.servings == 4 && shelf.count((Item)Drunkard.BEER_MUG.get()) == 3, "The pint should come from the tap, in a mug from the shelf");
            h.assertTrue(register.till > till, "The pint was not paid for");
            h.assertTrue(who[1].unserved(), "A barkeep must never sell a whole bottle");

            for (BarPatron p : who) {
               p.stopRiding();
               p.discard();
            }

            staff.forEach(Entity::discard);
         }
      );
   }

   @GameTest(
      template = "empty",
      timeoutTicks = 20
   )
   public static void the_glass_shelf_holds_glasses_and_hands_them_out(GameTestHelper h) {
      h.setBlock(POS, (Block)Drunkard.GLASS_SHELF.get());
      GlassShelfEntity shelf = (GlassShelfEntity)h.getBlockEntity(POS);
      h.assertTrue(shelf.insert(new ItemStack((ItemLike)Drunkard.BEER_MUG.get(), 5), 4), "Mugs should go on the shelf");
      h.assertTrue(((ItemStack)shelf.glasses.get(4)).getCount() == 5, "Mugs went to the wrong place");
      h.assertTrue(
         shelf.insert(new ItemStack((ItemLike)Drunkard.MARTINI_GLASS.get(), 3), 4)
            && ((ItemStack)shelf.glasses.get(4)).getCount() == 5
            && shelf.count((Item)Drunkard.MARTINI_GLASS.get()) == 3,
         "A different glass should find its own place"
      );
      h.assertTrue(!shelf.insert(new ItemStack((ItemLike)Drunkard.WINE.get("vodka").get()), 0), "Only glassware belongs on a glass shelf");
      h.assertTrue(
         shelf.takeFrom(4).is((Item)Drunkard.BEER_MUG.get()) && ((ItemStack)shelf.glasses.get(4)).getCount() == 4, "Taking a glass should hand out one"
      );
      BlockState state = h.getBlockState(POS);
      BlockPos pos = h.absolutePos(POS);
      h.assertTrue(
         GlassShelfBlock.slotAt(state, pos, new Vec3((double)pos.getX() + 0.9, (double)pos.getY() + 0.1, (double)pos.getZ() + 0.5)) == 0,
         "Bottom-left place is wrong"
      );
      h.assertTrue(
         GlassShelfBlock.slotAt(state, pos, new Vec3((double)pos.getX() + 0.1, (double)pos.getY() + 0.9, (double)pos.getZ() + 0.5)) == 8,
         "Top-right place is wrong"
      );
      GlassShelfEntity loaded = new GlassShelfEntity(pos, state);
      loaded.loadWithComponents(shelf.getUpdateTag(h.getLevel().registryAccess()), h.getLevel().registryAccess());
      h.assertTrue(
         loaded.count((Item)Drunkard.BEER_MUG.get()) == 4 && loaded.count((Item)Drunkard.MARTINI_GLASS.get()) == 3, "The shelf lost its glasses when loaded"
      );
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 900
   )
   public static void strangers_who_chat_become_friends(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      BlockPos[] stools = new BlockPos[]{new BlockPos(4, 1, 8), new BlockPos(5, 1, 8)};
      BarPatron[] who = new BarPatron[2];

      for (int i = 0; i < 2; i++) {
         h.setBlock(stools[i], (Block)Drunkard.BAR_STOOL.get());
         register.toggleSeat(h.absolutePos(stools[i]));
         who[i] = customer(h, register, stools[i].south());
         h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(stools[i]), who[i], h.absolutePos(REGISTER)), "Could not seat a customer");
         who[i].setOrder(product("pale_lager"), false);
         who[i].markOrdered();
         Player player = h.makeMockPlayer(GameType.CREATIVE);
         player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
         who[i].serve(player);
         who[i].setSipsLeft(30);
         who[i].chatNow();
      }

      h.assertTrue(!who[0].group().equals(who[1].group()) && !register.friends(who[0].skin(), who[1].skin()), "They should start as strangers");
      h.succeedWhen(() -> {
         h.assertTrue(register.friends(who[0].skin(), who[1].skin()), "Strangers sitting side by side should get talking and become friends");

         for (BarPatron p : who) {
            p.stopRiding();
            p.discard();
         }
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 2000
   )
   public static void at_closing_time_customers_drift_home(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      BarPatron[] who = new BarPatron[4];

      for (int i = 0; i < 4; i++) {
         BlockPos stool = new BlockPos(3 + i * 2, 1, 9);
         h.setBlock(stool, (Block)Drunkard.BAR_STOOL.get());
         register.toggleSeat(h.absolutePos(stool));
         who[i] = customer(h, register, stool.south());
         h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(stool), who[i], h.absolutePos(REGISTER)), "Could not seat a customer");
         who[i].setOrder(product("pale_lager"), false);
         who[i].markOrdered();
         Player player = h.makeMockPlayer(GameType.CREATIVE);
         player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
         who[i].serve(player);
         who[i].setSipsLeft(40);
      }

      register.open = false;
      long[] left = new long[4];

      for (int t = 2; t < 1900; t += 2) {
         h.runAtTickTime((long)t, () -> {
            for (int i = 0; i < 4; i++) {
               if (left[i] == 0L && (who[i].state() == BarPatron.State.LEAVING || who[i].isRemoved())) {
                  left[i] = h.getTick();
               }
            }
         });
      }

      h.runAtTickTime(20L, () -> {
         for (BarPatron p : who) {
            h.assertTrue(p.state() != BarPatron.State.LEAVING, "Customers shouldn't all walk out the moment the store closes");
         }
      });
      h.succeedWhen(() -> {
         for (int i = 0; i < 4; i++) {
            h.assertTrue(left[i] > 0L, "Customer " + i + " never went home after closing");
         }

         long first = Math.min(Math.min(left[0], left[1]), Math.min(left[2], left[3]));
         long last = Math.max(Math.max(left[0], left[1]), Math.max(left[2], left[3]));
         h.assertTrue(last - first >= 40L, "Customers should drift home over time, not all at once: " + Arrays.toString(left));

         for (BarPatron p : who) {
            p.discard();
         }
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 1200
   )
   public static void the_barkeep_walks_out_to_customers_far_from_the_bar(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");

      for (int x = 3; x <= 9; x++) {
         if (x != 6) {
            h.setBlock(new BlockPos(x, 1, 6), Blocks.SPRUCE_PLANKS);
         }
      }

      h.setBlock(new BlockPos(8, 1, 6), (Block)Drunkard.KEG.get());
      KegEntity keg = (KegEntity)h.getBlockEntity(new BlockPos(8, 1, 6));
      keg.beer = "pale_lager";
      keg.servings = 5;
      h.setBlock(new BlockPos(8, 2, 6), (BlockState)((Block)Drunkard.TAP_TOWER.get()).defaultBlockState().setValue(TapTowerBlock.FACING, Direction.SOUTH));
      h.setBlock(new BlockPos(9, 1, 3), (BlockState)((Block)Drunkard.GLASS_SHELF.get()).defaultBlockState().setValue(GlassShelfBlock.FACING, Direction.SOUTH));
      ((GlassShelfEntity)h.getBlockEntity(new BlockPos(9, 1, 3))).insert(new ItemStack((ItemLike)Drunkard.BEER_MUG.get(), 4), 0);
      BlockPos stool = new BlockPos(11, 1, 11);
      h.setBlock(stool, (Block)Drunkard.BAR_STOOL.get());
      register.toggleSeat(h.absolutePos(stool));
      BarPatron far = customer(h, register, new BlockPos(10, 1, 11));
      h.assertTrue(
         SeatEntity.sit(h.getLevel(), h.absolutePos(stool), far, h.absolutePos(REGISTER)),
         "Could not seat the customer: passenger="
            + far.isPassenger()
            + " occupied="
            + SeatEntity.occupied(h.getLevel(), h.absolutePos(stool))
            + " state="
            + h.getLevel().getBlockState(h.absolutePos(stool))
            + " at "
            + far.blockPosition().toShortString()
            + " near="
            + h.getLevel()
               .getEntitiesOfClass(Entity.class, new AABB(h.absolutePos(stool)).inflate(2.0))
               .stream()
               .map(e -> e.getClass().getSimpleName() + "@" + e.blockPosition().toShortString() + "/" + e.tickCount)
               .toList()
      );
      far.setOrder(product("pale_lager"), false);
      far.markOrdered();
      register.upgrades[BarUpgrades.Upgrade.BARKEEP.ordinal()] = 1;
      register.keepStaff(h.getLevel());
      BarWorker worker = register.staff(h.getLevel()).get(0);
      boolean[] climbed = new boolean[]{false};
      String[] where = new String[]{""};

      for (int i = 1; i < 1100; i += 5) {
         h.runAtTickTime((long)i, () -> {
            if (worker.getY() > (double)h.absolutePos(new BlockPos(0, 1, 0)).getY() + 0.6) {
               climbed[0] = true;
               where[0] = h.absolutePos(worker.position()) + " tick " + h.getTick() + " onGround=" + worker.onGround();
            }
         });
      }

      h.succeedWhen(
         () -> {
            h.assertTrue(!climbed[0], "The barkeep climbed onto something: " + where[0]);
            h.assertTrue(
               !far.unserved() && far.getMainHandItem().is((Item)Drunkard.WINE.get("pale_lager").get()),
               "The barkeep did not bring the far customer their pint: " + far.state()
            );
            far.stopRiding();
            far.discard();
            worker.discard();
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 1400
   )
   public static void the_barkeep_finds_its_way_on_slabs_and_carpets(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");

      for (int x = 0; x < 13; x++) {
         for (int z = 1; z <= 5; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.OAK_SLAB);
         }

         for (int z = 7; z <= 11; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.RED_CARPET);
         }
      }

      for (int x = 3; x <= 9; x++) {
         if (x != 6) {
            h.setBlock(new BlockPos(x, 1, 6), Blocks.SPRUCE_PLANKS);
         }
      }

      h.setBlock(new BlockPos(8, 1, 6), (Block)Drunkard.KEG.get());
      KegEntity keg = (KegEntity)h.getBlockEntity(new BlockPos(8, 1, 6));
      keg.beer = "pale_lager";
      keg.servings = 5;
      h.setBlock(new BlockPos(8, 2, 6), (BlockState)((Block)Drunkard.TAP_TOWER.get()).defaultBlockState().setValue(TapTowerBlock.FACING, Direction.SOUTH));
      h.setBlock(new BlockPos(9, 1, 3), (BlockState)((Block)Drunkard.GLASS_SHELF.get()).defaultBlockState().setValue(GlassShelfBlock.FACING, Direction.SOUTH));
      ((GlassShelfEntity)h.getBlockEntity(new BlockPos(9, 1, 3))).insert(new ItemStack((ItemLike)Drunkard.BEER_MUG.get(), 4), 0);
      BarPatron[] who = new BarPatron[2];
      BlockPos[] stools = new BlockPos[]{new BlockPos(5, 1, 8), new BlockPos(11, 1, 11)};

      for (int i = 0; i < 2; i++) {
         h.setBlock(stools[i], (Block)Drunkard.BAR_STOOL.get());
         register.toggleSeat(h.absolutePos(stools[i]));
         who[i] = customer(h, register, stools[i].west());
         h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(stools[i]), who[i], h.absolutePos(REGISTER)), "Could not seat a customer");
         who[i].setOrder(product("pale_lager"), false);
         who[i].markOrdered();
      }

      register.upgrades[BarUpgrades.Upgrade.BARKEEP.ordinal()] = 1;
      register.keepStaff(h.getLevel());
      BarWorker worker = register.staff(h.getLevel()).get(0);
      boolean[] climbed = new boolean[]{false};
      String[] where = new String[]{""};

      for (int i = 1; i < 1300; i += 5) {
         h.runAtTickTime((long)i, () -> {
            if (worker.getY() > (double)h.absolutePos(new BlockPos(0, 1, 0)).getY() + 0.9) {
               climbed[0] = true;
               where[0] = h.absolutePos(worker.position()) + " at " + h.getTick();
            }
         });
      }

      h.succeedWhen(() -> {
         h.assertTrue(!climbed[0], "The barkeep climbed onto something: " + where[0]);

         for (int ix = 0; ix < 2; ix++) {
            h.assertTrue(!who[ix].unserved(), "The barkeep did not serve customer " + ix + " (" + who[ix].state() + ")");
         }

         for (BarPatron p : who) {
            p.stopRiding();
            p.discard();
         }

         worker.discard();
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 900
   )
   public static void the_barkeep_can_be_sent_home_and_called_back(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      register.upgrades[BarUpgrades.Upgrade.BARKEEP.ordinal()] = 1;
      register.keepStaff(h.getLevel());
      BarWorker worker = register.staff(h.getLevel()).get(0);
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      CashRegisterMenu menu = new CashRegisterMenu(0, player.getInventory(), register);
      h.assertTrue(register.wages() == 40, "A barkeep costs $40 a day");
      menu.clickMenuButton(player, 9);
      h.assertTrue(register.staffOff && worker.goingHome() && register.staff(h.getLevel()).isEmpty(), "The barkeep was not sent home");
      h.assertTrue(register.wages() == 0 && menu.staffOff(), "Staff sent home shouldn't be paid");
      h.runAtTickTime(30L, () -> {
         register.keepStaff(h.getLevel());
         h.assertTrue(register.staff(h.getLevel()).isEmpty(), "A barkeep turned up while the staff are sent home");
      });
      boolean[] calledBack = new boolean[]{false};
      h.succeedWhen(() -> {
         h.assertTrue(worker.isRemoved(), "The barkeep never left");
         if (!calledBack[0]) {
            calledBack[0] = true;
            menu.clickMenuButton(player, 9);
         }

         List<BarWorker> back = register.staff(h.getLevel());
         h.assertTrue(back.size() == 1 && !register.staffOff && register.wages() == 40, "The barkeep was not called back in");

         for (BarWorker w : back) {
            w.discard();
         }
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void wages_are_paid_from_the_safe_first(GameTestHelper h) {
      CashRegisterEntity register = bar(h);
      register.open = false;
      h.setBlock(new BlockPos(2, 1, 2), (Block)Drunkard.SAFE.get());
      SafeEntity safe = (SafeEntity)h.getBlockEntity(new BlockPos(2, 1, 2));
      safe.balance = 100;
      register.till = 30;
      register.upgrades[BarUpgrades.Upgrade.BARKEEP.ordinal()] = 1;
      register.payWages();
      h.assertTrue(safe.balance == 60 && register.till == 30, "Wages should come out of the safe: safe $" + safe.balance + ", till $" + register.till);
      safe.balance = 10;
      register.payWages();
      h.assertTrue(safe.balance == 0 && register.till == 0, "The rest should come from the till: safe $" + safe.balance + ", till $" + register.till);
      safe.balance = 5;
      register.payWages();
      h.assertTrue(register.level(BarUpgrades.Upgrade.BARKEEP) == 0 && safe.balance == 5, "With too little money the staff quit (and nothing is taken)");
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void only_as_many_customers_come_in_as_can_sit(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      register.capacity = 10;

      for (int x : new int[]{3, 9}) {
         BlockPos stool = new BlockPos(x, 1, 9);
         h.setBlock(stool, (Block)Drunkard.BAR_STOOL.get());
         register.toggleSeat(h.absolutePos(stool));
      }

      for (int i = 0; i < 8; i++) {
         register.trySpawn(h.getLevel());
      }

      int inside = register.patronCount();

      for (BarPatron p : h.getLevel()
         .getEntitiesOfClass(BarPatron.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(32.0), px -> register.getBlockPos().equals(px.registerPos()))) {
         p.discard();
      }

      h.assertTrue(inside == 2, "Two seats should let in two customers, not " + inside);
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 900
   )
   public static void customers_cross_a_patchwork_floor_to_their_seats(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");

      for (int x = 0; x < 13; x++) {
         for (int z = 7; z <= 13; z++) {
            int k = (x * 7 + z * 3) % 5;
            if (k == 0) {
               h.setBlock(new BlockPos(x, 1, z), Blocks.OAK_SLAB);
            } else if (k == 1) {
               h.setBlock(new BlockPos(x, 1, z), Blocks.RED_CARPET);
            } else if (k == 2 && (x + z) % 2 == 0) {
               h.setBlock(new BlockPos(x, 1, z), Blocks.COBWEB);
            }
         }
      }

      BlockPos stool = new BlockPos(2, 1, 8);
      h.setBlock(stool, (Block)Drunkard.BAR_STOOL.get());
      register.toggleSeat(h.absolutePos(stool));
      BarPatron far = customer(h, register, new BlockPos(11, 1, 12));
      far.setOrder(product("pale_lager"), false);
      h.succeedWhen(
         () -> {
            h.assertTrue(
               far.getVehicle() instanceof SeatEntity,
               "The customer never sat down: "
                  + far.state()
                  + " at "
                  + far.blockPosition().toShortString()
                  + " near="
                  + h.getLevel()
                     .getEntitiesOfClass(Entity.class, new AABB(h.absolutePos(stool)).inflate(2.0))
                     .stream()
                     .map(e -> e.getClass().getSimpleName() + "@" + e.blockPosition().toShortString() + "/" + e.tickCount)
                     .toList()
            );
            far.stopRiding();
            far.discard();
         }
      );
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 1500
   )
   public static void the_barkeep_copes_with_a_raised_back_bar_open_trapdoors_and_cupboard_doors(GameTestHelper h) {
      bar(h, "pale_lager");

      for (int x = 0; x <= 12; x++) {
         for (int z = 1; z <= 5; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.SPRUCE_PLANKS);
         }
      }

      for (int x = 3; x <= 9; x++) {
         h.setBlock(new BlockPos(x, 1, 6), Blocks.SPRUCE_PLANKS);
         h.setBlock(new BlockPos(x, 2, 6), Blocks.SPRUCE_PLANKS);
      }

      h.setBlock(new BlockPos(11, 1, 6), (BlockState)Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
      BlockPos reg = new BlockPos(6, 3, 6);
      h.setBlock(reg, (BlockState)((Block)Drunkard.CASH_REGISTER.get()).defaultBlockState().setValue(CashRegisterBlock.FACING, Direction.NORTH));
      CashRegisterEntity register = (CashRegisterEntity)h.getBlockEntity(reg);
      register.setDoor(h.absolutePos(DOOR));
      register.stock.set(product("pale_lager"));
      register.open = true;
      h.setBlock(
         new BlockPos(6, 2, 5),
         (BlockState)((BlockState)Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true)).setValue(TrapDoorBlock.FACING, Direction.EAST)
      );
      h.setBlock(new BlockPos(9, 2, 3), (Block)Drunkard.KEG.get());
      KegEntity keg = (KegEntity)h.getBlockEntity(new BlockPos(9, 2, 3));
      keg.beer = "pale_lager";
      keg.servings = 5;
      h.setBlock(new BlockPos(9, 3, 3), (BlockState)((Block)Drunkard.TAP_TOWER.get()).defaultBlockState().setValue(TapTowerBlock.FACING, Direction.SOUTH));
      h.setBlock(new BlockPos(2, 2, 2), (BlockState)((Block)Drunkard.GLASS_SHELF.get()).defaultBlockState().setValue(GlassShelfBlock.FACING, Direction.SOUTH));
      ((GlassShelfEntity)h.getBlockEntity(new BlockPos(2, 2, 2))).insert(new ItemStack((ItemLike)Drunkard.BEER_MUG.get(), 4), 0);
      BlockPos cupboard = new BlockPos(3, 2, 2);
      BlockState door = (BlockState)Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
      h.setBlock(cupboard, (BlockState)door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
      h.setBlock(cupboard.above(), (BlockState)door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
      h.setBlock(new BlockPos(3, 2, 1), Blocks.BARREL);
      BarPatron[] who = new BarPatron[2];
      BlockPos[] stools = new BlockPos[]{new BlockPos(5, 1, 8), new BlockPos(10, 1, 10)};

      for (int i = 0; i < 2; i++) {
         h.setBlock(stools[i], (Block)Drunkard.BAR_STOOL.get());
         register.toggleSeat(h.absolutePos(stools[i]));
         who[i] = customer(h, register, stools[i].west());
         h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(stools[i]), who[i], h.absolutePos(reg)), "Could not seat customer " + i);
         who[i].setOrder(product("pale_lager"), false);
         who[i].markOrdered();
      }

      register.upgrades[BarUpgrades.Upgrade.BARKEEP.ordinal()] = 1;
      register.keepStaff(h.getLevel());
      BarWorker worker = register.staff(h.getLevel()).get(0);
      h.assertTrue(
         register.workerHome() != null && register.workerHome().getY() == h.absolutePos(new BlockPos(0, 2, 0)).getY(),
         "The barkeep's spot should be on the back bar's floor, not on top of the trapdoor: " + register.workerHome()
      );
      boolean[] inCupboard = new boolean[]{false};

      for (int i = 1; i < 1400; i += 3) {
         h.runAtTickTime((long)i, () -> {
            BlockPos doorAt = h.absolutePos(cupboard);
            double dx = worker.getX() - ((double)doorAt.getX() + 0.5);
            double dz = worker.getZ() - ((double)doorAt.getZ() + 0.5);
            if (dx * dx + dz * dz < 0.16000000000000003 && Math.abs(worker.getY() - (double)doorAt.getY()) < 1.0) {
               inCupboard[0] = true;
            }
         });
      }

      h.succeedWhen(() -> {
         h.assertTrue(!inCupboard[0], "The barkeep walked into the cupboard door");

         for (int ix = 0; ix < 2; ix++) {
            h.assertTrue(!who[ix].unserved(), "The barkeep did not serve customer " + ix + " (" + who[ix].state() + ")");
         }

         for (BarPatron p : who) {
            p.stopRiding();
            p.discard();
         }

         worker.discard();
      });
   }

   @GameTest(
      template = "empty",
      timeoutTicks = 40
   )
   public static void supplies_need_the_suppliers_deal(GameTestHelper h) {
      h.setBlock(new BlockPos(2, 1, 2), (Block)Drunkard.SAFE.get());
      SafeEntity safe = (SafeEntity)h.getBlockEntity(new BlockPos(2, 1, 2));
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      safe.balance = 6000;
      h.assertTrue(!safe.buySupply(player, 0) && safe.balance == 6000, "Supplies should be locked until the deal is signed");
      safe.balance = 4000;
      h.assertTrue(!safe.unlockSupplier(player) && !safe.supplier, "The deal costs $5000");
      safe.balance = 6000;
      h.assertTrue(safe.unlockSupplier(player) && safe.supplier && safe.balance == 1000, "Signing the deal should cost $5000");
      h.assertTrue(safe.buySupply(player, 0) && safe.balance < 1000, "Supplies should be on sale once the deal is signed");
      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 40
   )
   public static void two_barkeeps_never_look_alike(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      register.upgrades[BarUpgrades.Upgrade.BARKEEP.ordinal()] = 2;
      register.keepStaff(h.getLevel());
      List<BarWorker> staff = register.staff(h.getLevel());
      h.assertTrue(staff.size() == 2, "Two levels of barkeep should mean two barkeeps");
      h.assertTrue(staff.get(0).skin() != staff.get(1).skin() && staff.get(0).slim() != staff.get(1).slim(), "Two barkeeps shouldn't share a skin");

      for (BarWorker w : staff) {
         w.discard();
      }

      h.succeed();
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 1700
   )
   public static void an_idle_barkeep_potters_about_behind_the_bar(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");

      for (int x = 3; x <= 9; x++) {
         h.setBlock(new BlockPos(x, 1, 6), Blocks.SPRUCE_PLANKS);
      }

      register.upgrades[BarUpgrades.Upgrade.BARKEEP.ordinal()] = 1;
      register.keepStaff(h.getLevel());
      BarWorker worker = register.staff(h.getLevel()).get(0);
      Set<BlockPos> seen = new HashSet<>();
      boolean[] strayed = new boolean[]{false};

      for (int t = 10; t < 1600; t += 10) {
         h.runAtTickTime((long)t, () -> {
            BlockPos rel = h.relativePos(worker.blockPosition());
            seen.add(rel);
            if (rel.getZ() >= 6) {
               strayed[0] = true;
            }
         });
      }

      h.succeedWhen(() -> {
         h.assertTrue(!strayed[0], "The barkeep left the bar's back area");
         h.assertTrue(seen.size() >= 4, "An idle barkeep should wander about, not stand on one spot: " + seen.size() + " places");
         worker.discard();
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 600
   )
   public static void a_barkeeps_service_earns_no_tips(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      register.reputation = 100;
      BarPatron[] who = new BarPatron[6];

      for (int i = 0; i < 6; i++) {
         BlockPos stool = new BlockPos(1 + i * 2, 1, 9);
         h.setBlock(stool, (Block)Drunkard.BAR_STOOL.get());
         register.toggleSeat(h.absolutePos(stool));
         who[i] = customer(h, register, stool.south());
         h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(stool), who[i], h.absolutePos(REGISTER)), "Could not seat customer " + i);
         who[i].setOrder(product("pale_lager"), false);
         who[i].markOrdered();
         h.assertTrue(who[i].serveFromWorker(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get())), "Could not hand over the drink");
         who[i].setSipsLeft(1);
      }

      h.succeedWhen(() -> {
         for (BarPatron p : who) {
            h.assertTrue(p.getMainHandItem().isEmpty(), "Still drinking");
         }

         h.assertTrue(register.takingsToday > 0, "The drinks were not paid for");
         h.assertTrue(register.tipsToday == 0, "A barkeep's service shouldn't earn tips: $" + register.tipsToday);

         for (BarPatron p : who) {
            p.stopRiding();
            p.discard();
         }
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 2600
   )
   public static void customers_can_bring_their_own_smoke(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      BlockPos stool = new BlockPos(5, 1, 9);
      h.setBlock(stool, (Block)Drunkard.BAR_STOOL.get());
      register.toggleSeat(h.absolutePos(stool));
      BarPatron p = customer(h, register, stool.south());
      h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(stool), p, h.absolutePos(REGISTER)), "Could not seat the customer");
      p.setOrder(product("pale_lager"), false);
      p.markOrdered();
      h.assertTrue(p.serveFromWorker(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get())), "Could not hand over the drink");
      p.giveOwnSmoke(2);
      p.setSipsLeft(1);
      float[] drunkBefore = new float[]{0.0F};
      boolean[] lit = new boolean[]{false};

      for (int t = 1; t < 2500; t += 5) {
         h.runAtTickTime((long)t, () -> {
            if (p.getMainHandItem().getItem() instanceof CigarItem) {
               if (!lit[0]) {
                  drunkBefore[0] = p.drunk();
               }

               lit[0] = true;
            }
         });
      }

      h.succeedWhen(() -> {
         h.assertTrue(lit[0], "The customer never lit their own cigar");
         h.assertTrue(p.getMainHandItem().isEmpty() && p.drunk() <= drunkBefore[0] + 0.001F, "Their own smoke shouldn't make them drunk, and it should end");
         p.stopRiding();
         p.discard();
      });
   }

   @GameTest(
      template = "bar",
      timeoutTicks = 1800
   )
   public static void a_robber_is_beaten_up_and_takes_nothing(GameTestHelper h) {
      CashRegisterEntity register = bar(h, "pale_lager");
      register.till = 100;
      BarPatron[] who = new BarPatron[3];

      for (int i = 0; i < 3; i++) {
         BlockPos stool = new BlockPos(3 + i * 3, 1, 9);
         h.setBlock(stool, (Block)Drunkard.BAR_STOOL.get());
         register.toggleSeat(h.absolutePos(stool));
         who[i] = customer(h, register, stool.south());
         h.assertTrue(SeatEntity.sit(h.getLevel(), h.absolutePos(stool), who[i], h.absolutePos(REGISTER)), "Could not seat customer " + i);
         who[i].setOrder(product("pale_lager"), false);
         who[i].markOrdered();
         Player player = h.makeMockPlayer(GameType.CREATIVE);
         player.getInventory().add(new ItemStack((ItemLike)Drunkard.WINE.get("pale_lager").get()));
         who[i].serve(player);
      }

      int till = register.till;
      BarPatron robber = register.startRobbery(h.getLevel());
      h.assertTrue(robber != null && robber.robber() && robber.skin() == 2097152, "A robber should turn up");
      BlockPos at = h.absolutePos(new BlockPos(6, 1, 11));
      robber.moveTo((double)at.getX() + 0.5, (double)at.getY(), (double)at.getZ() + 0.5, 0.0F, 0.0F);
      robber.skipEntrance();
      boolean[] fought = new boolean[]{false};

      for (int t = 5; t < 1700; t += 5) {
         h.runAtTickTime((long)t, () -> {
            for (BarPatron p : who) {
               if (p.state() == BarPatron.State.FIGHTING) {
                  fought[0] = true;
               }
            }
         });
      }

      h.succeedWhen(() -> {
         h.assertTrue(fought[0], "The customers should stand up to the robber");
         h.assertTrue(robber.state() == BarPatron.State.LEAVING || robber.isRemoved(), "The robber should have been driven out: " + robber.state());
         h.assertTrue(register.till >= till, "The robber got away with money: $" + register.till);

         for (BarPatron p : who) {
            p.stopRiding();
            p.discard();
         }

         robber.discard();
      });
   }

   @GameTest(
      template = "empty",
      timeoutTicks = 40
   )
   public static void withdraw_all_only_takes_what_fits(GameTestHelper h) {
      h.setBlock(new BlockPos(2, 1, 2), (Block)Drunkard.SAFE.get());
      SafeEntity safe = (SafeEntity)h.getBlockEntity(new BlockPos(2, 1, 2));
      Player player = h.makeMockPlayer(GameType.CREATIVE);
      safe.balance = 100000;

      for (int i = 0; i < player.getInventory().items.size(); i++) {
         player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
      }

      safe.withdraw(player, safe.balance);
      h.assertTrue(
         safe.balance == 100000 && player.getInventory().countItem((Item)Drunkard.CASH.get()) == 0, "With no room nothing should come out: $" + safe.balance
      );
      player.getInventory().items.set(5, ItemStack.EMPTY);
      player.getInventory().items.set(6, ItemStack.EMPTY);
      safe.withdraw(player, safe.balance);
      int got = player.getInventory().countItem((Item)Drunkard.CASH.get());
      h.assertTrue(
         got > 0 && safe.balance == 100000 - got, "Only what fits should leave the safe, the rest stays: took $" + got + ", $" + safe.balance + " left"
      );
      h.succeed();
   }

   // Used by the patched DrunkardGameTests: wort (beer) no longer hands back the must bottle, mead still does.
   public static int bottlesAfterStart(String kind) {
      return java.util.Arrays.asList(Drunkard.BEERS).contains(kind) ? 4 : 5;
   }
}
