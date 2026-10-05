package com.drunkard;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod("drunkard")
public class Drunkard {
   public static final String ID = "drunkard";
   public static final String[] WOODS = new String[]{
      "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"
   };
   public static final String[] GRAPES = new String[]{"grenache", "merlot", "chardonnay", "pinot_noir"};
   public static final String[] BEERS = new String[]{"pale_lager", "pale_ale", "dark_lager", "wheat_beer"};
   public static final String[] DRINKS = new String[]{
      "grenache",
      "merlot",
      "chardonnay",
      "mead",
      "pale_lager",
      "pale_ale",
      "dark_lager",
      "wheat_beer",
      "tequila",
      "whiskey",
      "pinot_noir",
      "champagne",
      "brandy",
      "vodka",
      "vermouth",
      "rum",
      "moonshine"
   };
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, "drunkard");
   public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, "drunkard");
   public static final Supplier<RecipeSerializer<PourRecipe>> POUR_SHAPELESS = SERIALIZERS.register("pour_shapeless", PourRecipe.Serializer::new);
   public static final Supplier<RecipeSerializer<OnTheRocksRecipe>> ON_THE_ROCKS_RECIPE = SERIALIZERS.register(
      "on_the_rocks", () -> new SimpleCraftingRecipeSerializer(OnTheRocksRecipe::new)
   );
   public static final Supplier<RecipeSerializer<FullBottleRecipe>> FULL_BOTTLE_SHAPELESS = SERIALIZERS.register(
      "full_bottle_shapeless", FullBottleRecipe.Serializer::new
   );
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, "drunkard");
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "drunkard");
   public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "drunkard");
   public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, "drunkard");
   public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, "drunkard");
   public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, "drunkard");
   public static final Supplier<SimpleParticleType> BUBBLE = PARTICLES.register("fermentation_bubble", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> CIGAR_SMOKE = PARTICLES.register("cigar_smoke", () -> new SimpleParticleType(false));
   public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "drunkard");
   public static final Map<String, Supplier<Block>> RACKS = new LinkedHashMap<>();
   public static final Map<String, Supplier<Block>> TRELLISES = new LinkedHashMap<>();
   public static final Map<String, Supplier<Block>> TUBS = new LinkedHashMap<>();
   public static final Map<String, Supplier<Block>> WILD = new LinkedHashMap<>();
   public static final Map<String, Supplier<Item>> FRUIT = new LinkedHashMap<>();
   public static final Map<String, Supplier<Item>> SEEDS = new LinkedHashMap<>();
   public static final Map<String, Supplier<Item>> MUST = new LinkedHashMap<>();
   public static final Map<String, Supplier<Item>> WINE = new LinkedHashMap<>();
   public static final Map<String, Supplier<Item>> SPIRIT_WASH = new LinkedHashMap<>();
   public static final DeferredHolder<MobEffect, MobEffect> DRUNK = EFFECTS.register("drunk", () -> new MobEffect(MobEffectCategory.HARMFUL, 9187936) {
      });
   public static final DeferredHolder<MobEffect, MobEffect> HYDRATED = EFFECTS.register(
      "hydrated", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 5218272) {
         }
   );
   public static final DeferredHolder<MobEffect, MobEffect> FED = EFFECTS.register("fed", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 13145160) {
      });
   public static final Supplier<Item> YEAST;
   public static final Supplier<Block> TOBACCO_CROP;
   public static final Supplier<Item> TOBACCO_SEEDS;
   public static final Supplier<Item> TOBACCO_LEAF;
   public static final Supplier<Item> DRIED_TOBACCO;
   public static final Supplier<Item> CIGAR;
   public static final Supplier<Item> CIGARETTE;
   public static final Supplier<Item> CIGARETTE_PACK;
   public static final Supplier<Block> BARLEY_CROP;
   public static final Supplier<Block> HOPS_CROP;
   public static final Supplier<Item> BARLEY_SEEDS;
   public static final Supplier<Item> HOPS_SEEDS;
   public static final Supplier<Item> BARLEY;
   public static final Supplier<Item> HOPS;
   public static final Supplier<Item> MALT;
   public static final Supplier<Item> ROASTED_MALT;
   public static final Supplier<Item> LIME;
   public static final Supplier<Item> ORANGE;
   public static final Supplier<Item> SALT;
   public static final Supplier<Item> MINT;
   public static final Supplier<Item> WORMWOOD;
   public static final Supplier<Item> MOLASSES;
   public static final Supplier<Item> BITTERS;
   public static final Supplier<Item> ICE_CUBES;
   public static final Supplier<Item> ROCKS_GLASS;
   public static final Supplier<Item> HIGHBALL_GLASS;
   public static final Supplier<Item> MARGARITA_GLASS;
   public static final Supplier<Item> BEER_MUG;
   public static final Supplier<Item> PIPE;
   public static final Supplier<Item> OLIVES;
   public static final Supplier<Item> MARTINI_GLASS;
   public static final Supplier<Item> COCKTAIL_SHAKER;
   public static final Supplier<Item> MARTINI;
   public static final Supplier<Item> MARGARITA;
   public static final Supplier<Item> OLD_FASHIONED;
   public static final Supplier<Item> SCREWDRIVER;
   public static final Supplier<Item> MOJITO;
   public static final Supplier<Item> SANGRIA;
   public static final Supplier<Item> ON_THE_ROCKS;
   public static final Supplier<Block> KEG;
   public static final Supplier<BlockEntityType<KegEntity>> KEG_ENTITY;
   public static final Supplier<Block> TAP_TOWER;
   public static final Supplier<Block> HUMIDOR;
   public static final Supplier<BlockEntityType<HumidorEntity>> HUMIDOR_ENTITY;
   public static final Supplier<Block> POT_STILL;
   public static final Supplier<BlockEntityType<PotStillEntity>> POT_STILL_ENTITY;
   public static final Supplier<Item> MOLOTOV;
   public static final Supplier<EntityType<ThrownMolotov>> MOLOTOV_ENTITY;
   public static final Supplier<Item> SHOT_GLASS;
   public static final Supplier<Block> PLACED_SHOT_GLASS;
   public static final Supplier<BlockEntityType<ShotGlassEntity>> SHOT_GLASS_ENTITY;
   public static final Supplier<Block> COOLER;
   public static final Supplier<Block> BAR_STOOL;
   public static final Supplier<Block> BEACH_CHAIR;
   public static final Supplier<Block> DART_BOARD;
   public static final Supplier<EntityType<SeatEntity>> SEAT_ENTITY;
   public static final Supplier<BlockEntityType<CoolerEntity>> COOLER_ENTITY;
   public static final Supplier<Block> PLACED_BOTTLE;
   public static final Supplier<BlockEntityType<BottleEntity>> BOTTLE_ENTITY;
   public static final Supplier<Block> BARREL;
   public static final Supplier<Block> CIGAR_HOLDER;
   public static final Supplier<BlockEntityType<CigarHolderEntity>> CIGAR_HOLDER_ENTITY;
   public static final Supplier<Block> KETTLE;
   public static final Supplier<Block> STILL;
   public static final Supplier<Block> COPPER_PIPE;
   public static final Supplier<Block> TRANSFER_PUMP;
   public static final Supplier<BlockEntityType<TransferPumpEntity>> PUMP_ENTITY;
   public static final Supplier<Block> KETTLE_TOP;
   public static final Supplier<Block> STILL_TOP;
   public static final Supplier<BlockEntityType<RackEntity>> RACK_ENTITY;
   public static final Supplier<BlockEntityType<TubEntity>> TUB_ENTITY;
   public static final Supplier<BlockEntityType<BarrelEntity>> BARREL_ENTITY;
   public static final Supplier<BlockEntityType<BrewKettleEntity>> KETTLE_ENTITY;
   public static final Supplier<BlockEntityType<DistillationStillEntity>> STILL_ENTITY;
   public static final Supplier<MenuType<BarrelMenu>> BARREL_MENU;
   public static final Supplier<MenuType<MachineMenu>> MACHINE_MENU;
   public static final Supplier<CreativeModeTab> TAB;
   public static final int OLIVE_CHANCE = 6;
   public static final int SALT_CHANCE = 8;

   private static void addCreative(Output out, Set<Item> seen, ItemLike item) {
      if (seen.add(item.asItem())) {
         out.accept(item);
      }
   }

   private static Supplier<Block> block(String name, Supplier<Block> factory) {
      Supplier<Block> block = BLOCKS.register(name, factory);
      ITEMS.register(name, () -> new BlockItem(block.get(), new Properties()));
      return block;
   }

   public Drunkard(IEventBus bus) {
      BLOCKS.register(bus);
      ITEMS.register(bus);
      ENTITIES.register(bus);
      ENTITY_TYPES.register(bus);
      SERIALIZERS.register(bus);
      MENUS.register(bus);
      EFFECTS.register(bus);
      PARTICLES.register(bus);
      TABS.register(bus);
      NeoForge.EVENT_BUS.addListener(this::protectSoil);
      NeoForge.EVENT_BUS.addListener(this::sneakInteraction);
      NeoForge.EVENT_BUS.addListener(this::forageSeeds);
      bus.addListener(AlcoholNetwork::register);
   }

   private void protectSoil(FarmlandTrampleEvent event) {
      if (event.getLevel().getBlockState(event.getPos().above()).getBlock() instanceof TrellisBlock) {
         event.setCanceled(true);
      }
   }

   private void sneakInteraction(RightClickBlock event) {
      Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
      if (event.getEntity().isShiftKeyDown() && (block instanceof RackBlock || block instanceof TubBlock)) {
         event.setUseBlock(TriState.TRUE);
         event.setUseItem(TriState.FALSE);
      }
   }

   private void forageSeeds(BreakEvent event) {
      if (event.getPlayer() != null && !event.getPlayer().getAbilities().instabuild) {
         Block block = event.getState().getBlock();
         if (event.getLevel() instanceof ServerLevel level) {
            Item fruit = block != Blocks.OAK_LEAVES && block != Blocks.BIRCH_LEAVES
               ? (block == Blocks.JUNGLE_LEAVES ? LIME.get() : (block == Blocks.ACACIA_LEAVES ? ORANGE.get() : null))
               : OLIVES.get();
            if (fruit != null) {
               if (level.random.nextInt(6) == 0) {
                  Block.popResource(level, event.getPos(), new ItemStack(fruit));
               }
            } else if (block == Blocks.SAND) {
               String shore = level.getBiome(event.getPos()).unwrapKey().map(k -> k.location().getPath()).orElse("");
               if ((shore.contains("beach") || shore.contains("ocean")) && level.random.nextInt(8) == 0) {
                  Block.popResource(level, event.getPos(), new ItemStack((ItemLike)SALT.get()));
               }
            } else if (block == Blocks.SHORT_GRASS
               || block == Blocks.TALL_GRASS
               || block == Blocks.FERN
               || block == Blocks.LARGE_FERN
               || block == Blocks.DEAD_BUSH) {
               String biome = level.getBiome(event.getPos()).unwrapKey().map(k -> k.location().getPath()).orElse("");
               if (level.random.nextInt(12) == 0) {
                  Item item;
                  if (biome.contains("desert") || biome.contains("badlands") || biome.contains("savanna")) {
                     item = SEEDS.get("grenache").get();
                  } else if (biome.contains("jungle") || biome.contains("swamp")) {
                     item = HOPS_SEEDS.get();
                  } else if (!biome.contains("plains") && !biome.contains("meadow")) {
                     item = level.random.nextInt(3) == 0
                        ? SEEDS.get("pinot_noir").get()
                        : (level.random.nextBoolean() ? SEEDS.get("merlot").get() : SEEDS.get("chardonnay").get());
                  } else {
                     item = level.random.nextBoolean() ? BARLEY_SEEDS.get() : SEEDS.get("chardonnay").get();
                  }

                  if ((biome.contains("plains") || biome.contains("forest")) && level.random.nextInt(4) == 0) {
                     item = TOBACCO_SEEDS.get();
                  }

                  Block.popResource(level, event.getPos(), new ItemStack(item));
               }
            }
         }
      }
   }

   public static String fruitType(ItemStack stack) {
      return find(FRUIT, stack);
   }

   public static String mustType(ItemStack stack) {
      return find(MUST, stack);
   }

   private static String find(Map<String, Supplier<Item>> map, ItemStack stack) {
      for (Entry<String, Supplier<Item>> e : map.entrySet()) {
         if (stack.is(e.getValue().get())) {
            return e.getKey();
         }
      }

      return "";
   }

   static {
      for (String wood : WOODS) {
         RACKS.put(wood, block(wood + "_wine_rack", RackBlock::new));
         TRELLISES.put(wood, block(wood + "_trellis", TrellisBlock::new));
         TUBS.put(wood, block(wood + "_crushing_tub", TubBlock::new));
      }

      for (String grape : GRAPES) {
         WILD.put(grape, block("wild_" + grape + "_bush", () -> new WildCropBlock(grape)));
         FRUIT.put(grape, ITEMS.register(grape + "_grapes", () -> new Item(new Properties().food(new Builder().nutrition(2).saturationModifier(0.2F).build()))));
         SEEDS.put(grape, ITEMS.register(grape + "_seeds", () -> new GrapeSeedItem(grape)));
         MUST.put(grape, ITEMS.register(grape + "_grape_must", () -> new Item(new Properties().stacksTo(64).craftRemainder(Items.GLASS_BOTTLE))));
         WINE.put(grape, ITEMS.register(grape + "_wine", () -> new WineItem()));
      }

      YEAST = ITEMS.register("yeast", () -> new Item(new Properties()));

      for (String kind : new String[]{"tobacco", "barley", "hops", "mint", "wormwood"}) {
         WILD.put(kind, block("wild_" + kind, () -> new WildCropBlock(kind)));
      }

      TOBACCO_CROP = BLOCKS.register("tobacco_crop", TobaccoCropBlock::new);
      TOBACCO_SEEDS = ITEMS.register("tobacco_seeds", () -> new ItemNameBlockItem(TOBACCO_CROP.get(), new Properties()));
      TOBACCO_LEAF = ITEMS.register("tobacco_leaf", () -> new Item(new Properties()));
      DRIED_TOBACCO = ITEMS.register("dried_tobacco", () -> new Item(new Properties()));
      CIGAR = ITEMS.register("cigar", () -> new CigarItem());
      CIGARETTE = ITEMS.register("cigarette", CigaretteItem::new);
      CIGARETTE_PACK = ITEMS.register("cigarette_pack", CigarettePackItem::new);
      BARLEY_CROP = BLOCKS.register("barley_crop", () -> new BrewingCropBlock("barley"));
      HOPS_CROP = BLOCKS.register("hops_crop", () -> new BrewingCropBlock("hops"));
      BARLEY_SEEDS = ITEMS.register("barley_seeds", () -> new ItemNameBlockItem(BARLEY_CROP.get(), new Properties()));
      HOPS_SEEDS = ITEMS.register("hops_seeds", () -> new ItemNameBlockItem(HOPS_CROP.get(), new Properties()));
      BARLEY = ITEMS.register("barley", () -> new Item(new Properties()));
      HOPS = ITEMS.register("hops", () -> new Item(new Properties()));
      MALT = ITEMS.register("malt", () -> new Item(new Properties()));
      ROASTED_MALT = ITEMS.register("roasted_malt", () -> new Item(new Properties()));
      MUST.put("mead", ITEMS.register("honey_must", () -> new Item(new Properties().stacksTo(64).craftRemainder(Items.GLASS_BOTTLE))));
      WINE.put("mead", ITEMS.register("mead", WineItem::new));
      MUST.put("champagne", ITEMS.register("champagne_must", () -> new Item(new Properties().stacksTo(64).craftRemainder(Items.GLASS_BOTTLE))));
      WINE.put("champagne", ITEMS.register("champagne", WineItem::new));
      WINE.put("brandy", ITEMS.register("brandy", WineItem::new));

      for (String beer : BEERS) {
         ITEMS.register("raw_" + beer + "_wort", () -> new Item(new Properties().stacksTo(64).craftRemainder(Items.GLASS_BOTTLE)));
         MUST.put(beer, ITEMS.register(beer + "_wort", () -> new Item(new Properties().stacksTo(64).craftRemainder(Items.GLASS_BOTTLE))));
         WINE.put(beer, ITEMS.register(beer, WineItem::new));
      }

      for (String spirit : new String[]{"tequila", "whiskey"}) {
         MUST.put(spirit, ITEMS.register(spirit + "_mash", () -> new Item(new Properties().stacksTo(64).craftRemainder(Items.GLASS_BOTTLE))));
         SPIRIT_WASH.put(spirit, ITEMS.register("fermented_" + spirit + "_wash", () -> new Item(new Properties().stacksTo(64))));
         WINE.put(spirit, ITEMS.register(spirit, WineItem::new));
      }

      WINE.put("vodka", ITEMS.register("vodka", WineItem::new));
      WINE.put("vermouth", ITEMS.register("dry_vermouth", WineItem::new));
      WINE.put("rum", ITEMS.register("rum", WineItem::new));
      MUST.put("moonshine", ITEMS.register("sweet_mash", () -> new Item(new Properties().stacksTo(64).craftRemainder(Items.GLASS_BOTTLE))));
      SPIRIT_WASH.put("moonshine", ITEMS.register("sour_mash", () -> new Item(new Properties().stacksTo(64))));
      WINE.put("moonshine", ITEMS.register("moonshine", WineItem::new));
      LIME = ITEMS.register("lime", () -> new Item(new Properties().food(new Builder().nutrition(1).saturationModifier(0.2F).fast().build())));
      ORANGE = ITEMS.register("orange", () -> new Item(new Properties().food(new Builder().nutrition(3).saturationModifier(0.4F).build())));
      SALT = ITEMS.register("salt", () -> new Item(new Properties()));
      MINT = ITEMS.register("mint", () -> new Item(new Properties()));
      WORMWOOD = ITEMS.register("wormwood", () -> new Item(new Properties()));
      MOLASSES = ITEMS.register("molasses", () -> new Item(new Properties()));
      BITTERS = ITEMS.register("aromatic_bitters", () -> new Item(new Properties().stacksTo(16)));
      ICE_CUBES = ITEMS.register("ice_cubes", () -> new Item(new Properties()));
      ROCKS_GLASS = ITEMS.register("rocks_glass", () -> new Item(new Properties().stacksTo(16)));
      HIGHBALL_GLASS = ITEMS.register("highball_glass", () -> new Item(new Properties().stacksTo(16)));
      MARGARITA_GLASS = ITEMS.register("margarita_glass", () -> new Item(new Properties().stacksTo(16)));
      BEER_MUG = ITEMS.register("beer_mug", () -> new Item(new Properties().stacksTo(16)));
      PIPE = ITEMS.register("pipe", PipeItem::new);
      OLIVES = ITEMS.register("olives", () -> new Item(new Properties().food(new Builder().nutrition(1).saturationModifier(0.3F).fast().build())));
      MARTINI_GLASS = ITEMS.register("martini_glass", () -> new Item(new Properties().stacksTo(16)));
      COCKTAIL_SHAKER = ITEMS.register("cocktail_shaker", CocktailShakerItem::new);
      MARTINI = ITEMS.register("martini", () -> new CocktailItem(8.0F, MARTINI_GLASS, CocktailItem.Vessel.MARTINI, "Vodka, dry vermouth, ice, olive"));
      MARGARITA = ITEMS.register("margarita", () -> new CocktailItem(6.0F, MARGARITA_GLASS, CocktailItem.Vessel.MARGARITA, "Tequila, lime, salt rim, ice"));
      OLD_FASHIONED = ITEMS.register(
         "old_fashioned", () -> new CocktailItem(8.0F, ROCKS_GLASS, CocktailItem.Vessel.ROCKS, "Whiskey, sugar, bitters, orange peel, ice")
      );
      SCREWDRIVER = ITEMS.register("screwdriver", () -> new CocktailItem(4.0F, HIGHBALL_GLASS, CocktailItem.Vessel.HIGHBALL, "Vodka, orange juice, ice"));
      MOJITO = ITEMS.register("mojito", () -> new CocktailItem(5.0F, HIGHBALL_GLASS, CocktailItem.Vessel.HIGHBALL, "Rum, lime, mint, sugar, ice"));
      SANGRIA = ITEMS.register("sangria", () -> new CocktailItem(5.0F, HIGHBALL_GLASS, CocktailItem.Vessel.HIGHBALL, "Red wine, brandy, orange, apple, sugar"));
      ON_THE_ROCKS = ITEMS.register("on_the_rocks", OnTheRocksItem::new);
      KEG = block("keg", KegBlock::new);
      KEG_ENTITY = ENTITIES.register(
         "keg", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(KegEntity::new, new Block[]{KEG.get()}).build(null)
      );
      TAP_TOWER = block("tap_tower", TapTowerBlock::new);
      HUMIDOR = block("humidor", HumidorBlock::new);
      HUMIDOR_ENTITY = ENTITIES.register(
         "humidor", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(HumidorEntity::new, new Block[]{HUMIDOR.get()}).build(null)
      );
      POT_STILL = block("pot_still", PotStillBlock::new);
      POT_STILL_ENTITY = ENTITIES.register(
         "pot_still", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(PotStillEntity::new, new Block[]{POT_STILL.get()}).build(null)
      );
      MOLOTOV = ITEMS.register("molotov_cocktail", MolotovItem::new);
      MOLOTOV_ENTITY = ENTITY_TYPES.register(
         "molotov_cocktail",
         () -> net.minecraft.world.entity.EntityType.Builder.of(ThrownMolotov::new, MobCategory.MISC)
               .sized(0.25F, 0.25F)
               .clientTrackingRange(4)
               .updateInterval(10)
               .build("molotov_cocktail")
      );
      SHOT_GLASS = ITEMS.register("shot_glass", ShotGlassItem::new);
      PLACED_SHOT_GLASS = BLOCKS.register("placed_shot_glass", ShotGlassBlock::new);
      SHOT_GLASS_ENTITY = ENTITIES.register(
         "placed_shot_glass",
         () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(ShotGlassEntity::new, new Block[]{PLACED_SHOT_GLASS.get()}).build(null)
      );
      COOLER = block("cooler", CoolerBlock::new);
      BAR_STOOL = block(
         "bar_stool",
         () -> new SeatBlock(
               net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                  .mapColor(MapColor.METAL)
                  .noOcclusion()
                  .strength(1.5F)
                  .sound(SoundType.METAL),
               0.9375,
               Shapes.or(Block.box(3.5, 12.0, 3.5, 12.5, 15.0, 12.5), Block.box(4.0, 0.0, 4.0, 12.0, 12.0, 12.0))
            )
      );
      BEACH_CHAIR = block(
         "beach_chair",
         () -> new SeatBlock(
               net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).noOcclusion().strength(0.8F).sound(SoundType.WOOD),
               0.375,
               Shapes.or(
                  Block.box(1.5, 0.0, 1.0, 14.5, 6.0, 12.5),
                  new VoxelShape[]{
                     Block.box(1.5, 6.0, 11.0, 14.5, 14.0, 16.0), Block.box(1.5, 6.0, 2.0, 4.0, 10.0, 11.0), Block.box(12.0, 6.0, 2.0, 14.5, 10.0, 11.0)
                  }
               )
            )
      );
      DART_BOARD = block("dart_board", DartBoardBlock::new);
      SEAT_ENTITY = ENTITY_TYPES.register(
         "seat",
         () -> net.minecraft.world.entity.EntityType.Builder.of(SeatEntity::new, MobCategory.MISC)
               .sized(0.01F, 0.01F)
               .noSummon()
               .clientTrackingRange(8)
               .build("seat")
      );
      COOLER_ENTITY = ENTITIES.register(
         "cooler", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(CoolerEntity::new, new Block[]{COOLER.get()}).build(null)
      );
      PLACED_BOTTLE = BLOCKS.register("placed_bottle", PlacedBottleBlock::new);
      BOTTLE_ENTITY = ENTITIES.register(
         "placed_bottle",
         () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(BottleEntity::new, new Block[]{PLACED_BOTTLE.get()}).build(null)
      );
      BARREL = block("fermentation_barrel", BarrelBlock::new);
      CIGAR_HOLDER = block("cigar_holder", CigarHolderBlock::new);
      CIGAR_HOLDER_ENTITY = ENTITIES.register(
         "cigar_holder",
         () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(CigarHolderEntity::new, new Block[]{CIGAR_HOLDER.get()}).build(null)
      );
      KETTLE = block("brew_kettle", BrewKettleBlock::new);
      STILL = block("distillation_still", DistillationStillBlock::new);
      COPPER_PIPE = block("copper_pipe", CopperPipeBlock::new);
      TRANSFER_PUMP = block("transfer_pump", TransferPumpBlock::new);
      PUMP_ENTITY = ENTITIES.register(
         "transfer_pump",
         () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(TransferPumpEntity::new, new Block[]{TRANSFER_PUMP.get()}).build(null)
      );
      KETTLE_TOP = BLOCKS.register("brew_kettle_top", () -> new MachineTopBlock(KETTLE::get));
      STILL_TOP = BLOCKS.register("distillation_still_top", () -> new MachineTopBlock(STILL::get));
      RACK_ENTITY = ENTITIES.register(
         "wine_rack",
         () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                  RackEntity::new, RACKS.values().stream().map(Supplier::get).toArray(Block[]::new)
               )
               .build(null)
      );
      TUB_ENTITY = ENTITIES.register(
         "crushing_tub",
         () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                  TubEntity::new, TUBS.values().stream().map(Supplier::get).toArray(Block[]::new)
               )
               .build(null)
      );
      BARREL_ENTITY = ENTITIES.register(
         "fermentation_barrel",
         () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(BarrelEntity::new, new Block[]{BARREL.get()}).build(null)
      );
      KETTLE_ENTITY = ENTITIES.register(
         "brew_kettle", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(BrewKettleEntity::new, new Block[]{KETTLE.get()}).build(null)
      );
      STILL_ENTITY = ENTITIES.register(
         "distillation_still",
         () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(DistillationStillEntity::new, new Block[]{STILL.get()}).build(null)
      );
      BARREL_MENU = MENUS.register("fermentation_barrel", () -> IMenuTypeExtension.create(BarrelMenu::new));
      MACHINE_MENU = MENUS.register("fired_machine", () -> IMenuTypeExtension.create(MachineMenu::new));
      TAB = TABS.register(
         "drunkard",
         () -> CreativeModeTab.builder()
               .title(Component.translatable("itemGroup.drunkard"))
               .icon(() -> new ItemStack((ItemLike)WINE.get("merlot").get()))
               .displayItems(
                  (p, out) -> {
                     HashSet<Item> seen = new HashSet<>();

                     for (Supplier<Block> block : List.of(
                        BARREL,
                        KETTLE,
                        STILL,
                        POT_STILL,
                        TRANSFER_PUMP,
                        COPPER_PIPE,
                        KEG,
                        TAP_TOWER,
                        COOLER,
                        CIGAR_HOLDER,
                        HUMIDOR,
                        BAR_STOOL,
                        BEACH_CHAIR,
                        DART_BOARD
                     )) {
                        addCreative(out, seen, (ItemLike)block.get());
                     }

                     for (Map<String, Supplier<Block>> family : List.of(TUBS, TRELLISES, RACKS)) {
                        for (Supplier<Block> block : family.values()) {
                           addCreative(out, seen, (ItemLike)block.get());
                        }
                     }

                     for (Supplier<Block> plant : WILD.values()) {
                        addCreative(out, seen, (ItemLike)plant.get());
                     }

                     for (Supplier<Item> seed : SEEDS.values()) {
                        addCreative(out, seen, (ItemLike)seed.get());
                     }

                     for (Supplier<Item> item : List.of(BARLEY_SEEDS, HOPS_SEEDS, TOBACCO_SEEDS)) {
                        addCreative(out, seen, (ItemLike)item.get());
                     }

                     for (Supplier<Item> fruit : FRUIT.values()) {
                        addCreative(out, seen, (ItemLike)fruit.get());
                     }

                     for (Supplier<Item> item : List.of(
                        BARLEY,
                        HOPS,
                        TOBACCO_LEAF,
                        MALT,
                        ROASTED_MALT,
                        DRIED_TOBACCO,
                        YEAST,
                        OLIVES,
                        LIME,
                        ORANGE,
                        MINT,
                        WORMWOOD,
                        SALT,
                        MOLASSES,
                        BITTERS,
                        ICE_CUBES
                     )) {
                        addCreative(out, seen, (ItemLike)item.get());
                     }

                     for (Entry<String, Supplier<Item>> must : MUST.entrySet()) {
                        if (!SPIRIT_WASH.containsKey(must.getKey())) {
                           addCreative(out, seen, (ItemLike)must.getValue().get());
                        }
                     }

                     addCreative(out, seen, (ItemLike)MUST.get("moonshine").get());
                     addCreative(out, seen, (ItemLike)SPIRIT_WASH.get("moonshine").get());

                     for (Supplier<Item> drink : WINE.values()) {
                        addCreative(out, seen, (ItemLike)drink.get());
                     }

                     for (Supplier<Item> item : List.of(
                        COCKTAIL_SHAKER,
                        MARTINI_GLASS,
                        MARGARITA_GLASS,
                        ROCKS_GLASS,
                        HIGHBALL_GLASS,
                        SHOT_GLASS,
                        BEER_MUG,
                        MARTINI,
                        MARGARITA,
                        OLD_FASHIONED,
                        SCREWDRIVER,
                        MOJITO,
                        SANGRIA,
                        MOLOTOV,
                        CIGAR,
                        CIGARETTE_PACK,
                        CIGARETTE,
                        PIPE
                     )) {
                        addCreative(out, seen, (ItemLike)item.get());
                     }

                     ItemStack rocks = OnTheRocksItem.pour(new ItemStack((ItemLike)WINE.get("whiskey").get()));
                     out.accept(rocks);
                  }
               )
               .build()
      );
   }
}
