package com.airlinestory.registry;

import com.airlinestory.AirlineStory;
import com.airlinestory.item.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AirlineStory.MODID);
    public static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    private static RegistryObject<Item> reg(String name, Supplier<Item> sup) {
        RegistryObject<Item> ro = ITEMS.register(name, sup);
        ALL.add(ro);
        return ro;
    }

    private static Item.Properties one() { return new Item.Properties().stacksTo(1); }
    private static Item.Properties story() { return new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON); }
    private static Item.Properties food(int n, float sat) { return new Item.Properties().food(new FoodProperties.Builder().nutrition(n).saturationMod(sat).build()); }

    static {
        for (RegistryObject<Block> b : ModBlocks.ALL) {
            ITEMS.register(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties()));
        }
    }

    // ---------- Story / airport ----------
    public static final RegistryObject<Item> TRAVEL_JOURNAL = reg("travel_journal", () -> new JournalItem(story()));
    public static final RegistryObject<Item> PASSPORT = reg("passport", () -> new LoreItem(story(), 2, false));
    public static final RegistryObject<Item> BOARDING_PASS = reg("boarding_pass", () -> new LoreItem(story(), 3, false));
    public static final RegistryObject<Item> CARRY_ON_BAG = reg("carry_on_bag", () -> new LoreItem(story(), 2, false));
    public static final RegistryObject<Item> POLAROID_SEAT = reg("polaroid_seat", () -> new LoreItem(story(), 3, false));
    public static final RegistryObject<Item> POLAROID_BEACH = reg("polaroid_beach", () -> new LoreItem(story(), 3, false));
    public static final RegistryObject<Item> POLAROID_GROVE = reg("polaroid_grove", () -> new LoreItem(story(), 3, false));
    public static final RegistryObject<Item> CRUMPLED_NOTE = reg("crumpled_note", () -> new LoreItem(story(), 3, false));
    public static final RegistryObject<Item> COFFEE = reg("coffee", () -> new DrinkItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.3f).alwaysEat().effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0), 1f).build()), 1, null));
    public static final RegistryObject<Item> AIRLINE_PEANUTS = reg("airline_peanuts", () -> new LoreItem(new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(2).saturationMod(0.2f).fast().build()), 1, false));
    public static final RegistryObject<Item> AIRLINE_MEAL = reg("airline_meal", () -> new LoreItem(food(6, 0.6f).stacksTo(16), 1, false));

    // ---------- Island survival ----------
    public static final RegistryObject<Item> PLANT_FIBER = reg("plant_fiber", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PALM_FROND = reg("palm_frond", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ROPE = reg("rope", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AIRLINE_BLANKET = reg("airline_blanket", () -> new LoreItem(new Item.Properties(), 1, false));
    public static final RegistryObject<Item> SAIL_CLOTH = reg("sail_cloth", () -> new LoreItem(new Item.Properties().stacksTo(16), 1, false));
    public static final RegistryObject<Item> HULL_PANEL = reg("hull_panel", () -> new LoreItem(new Item.Properties(), 1, false));
    public static final RegistryObject<Item> MAST = reg("mast", () -> new LoreItem(new Item.Properties().stacksTo(4), 1, false));
    public static final RegistryObject<Item> EMPTY_FLASK = reg("empty_flask", () -> new FlaskItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> WATER_FLASK = reg("water_flask", () -> new DrinkItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
            .nutrition(2).saturationMod(0.6f).alwaysEat().effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1f).build()), 1, () -> ModItems.EMPTY_FLASK.get()));
    public static final RegistryObject<Item> SEA_WATER_FLASK = reg("sea_water_flask", () -> new DrinkItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
            .nutrition(0).saturationMod(0f).alwaysEat().effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 0), 1f)
            .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 300, 1), 1f).build()), 2, () -> ModItems.EMPTY_FLASK.get()));
    public static final RegistryObject<Item> GHOST_TONIC = reg("ghost_tonic", () -> new DrinkItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.2f).alwaysEat().effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0), 1f).build()), 2, () -> ModItems.EMPTY_FLASK.get()));
    public static final RegistryObject<Item> COCONUT_HALF = reg("coconut_half", () -> new Item(food(3, 0.5f)));
    public static final RegistryObject<Item> TARO_ROOT = reg("taro_root", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(2).saturationMod(0.2f).effect(() -> new MobEffectInstance(MobEffects.HUNGER, 200, 0), 0.4f).build())));
    public static final RegistryObject<Item> ROASTED_TARO = reg("roasted_taro", () -> new Item(food(6, 0.6f)));
    public static final RegistryObject<Item> FISH_SKEWER = reg("fish_skewer", () -> new Item(food(8, 0.8f)));
    public static final RegistryObject<Item> TRIBAL_STEW = reg("tribal_stew", () -> new BowlFoodItem(food(10, 1.0f).stacksTo(1)));
    public static final RegistryObject<Item> STONE_KNIFE = reg("stone_knife", () -> new SwordItem(Tiers.STONE, 2, -2.0f, new Item.Properties()));
    public static final RegistryObject<Item> MACHETE = reg("machete", () -> new SwordItem(Tiers.IRON, 4, -2.4f, new Item.Properties()));
    public static final RegistryObject<Item> FLARE = reg("flare", () -> new FlareItem(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> TIDEFOLK_CHARM = reg("tidefolk_charm", () -> new LoreItem(story(), 2, true));
    public static final RegistryObject<Item> BOAT_BLUEPRINT = reg("boat_blueprint", () -> new LoreItem(story(), 3, false));
    public static final RegistryObject<Item> NAVIGATION_CHART = reg("navigation_chart", () -> new LoreItem(story(), 3, true));
    public static final RegistryObject<Item> STATION_KEYCARD = reg("station_keycard", () -> new LoreItem(story(), 2, false));

    @SuppressWarnings("unused")
    private static Item.Properties unusedOne() { return one(); }
}
