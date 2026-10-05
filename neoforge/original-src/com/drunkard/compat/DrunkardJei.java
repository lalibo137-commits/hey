package com.drunkard.compat;

import com.drunkard.AlcoholSystem;
import com.drunkard.BarrelMenu;
import com.drunkard.Drunkard;
import com.drunkard.TrellisBlock;
import com.drunkard.WineData;
import com.drunkard.client.BarrelScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

@JeiPlugin
public class DrunkardJei implements IModPlugin {
   public static IJeiRuntime runtime;
   public static final RecipeType<DrunkardJei.Process> CRUSHING = RecipeType.create("drunkard", "crushing", DrunkardJei.Process.class);
   public static final RecipeType<DrunkardJei.Process> FERMENTING = RecipeType.create("drunkard", "fermenting", DrunkardJei.Process.class);
   public static final RecipeType<DrunkardJei.Process> GROWING = RecipeType.create("drunkard", "growing", DrunkardJei.Process.class);
   public static final RecipeType<DrunkardJei.Process> MASHING = RecipeType.create("drunkard", "mashing", DrunkardJei.Process.class);
   public static final RecipeType<DrunkardJei.Process> DISTILLING = RecipeType.create("drunkard", "distilling", DrunkardJei.Process.class);

   public void onRuntimeAvailable(IJeiRuntime value) {
      runtime = value;
      List<ItemStack> retired = Drunkard.ITEMS.getEntries().stream().filter(i -> {
         String n = i.getId().getPath();
         return (n.startsWith("raw_") || n.endsWith("_mash") || n.endsWith("_wash")) && !n.equals("sweet_mash") && !n.equals("sour_mash");
      }).map(i -> new ItemStack((ItemLike)i.get())).toList();
      value.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, retired);
   }

   public void onRuntimeUnavailable() {
      runtime = null;
   }

   public ResourceLocation getPluginUid() {
      return ResourceLocation.fromNamespaceAndPath("drunkard", "jei");
   }

   public void registerCategories(IRecipeCategoryRegistration r) {
      IGuiHelper gui = r.getJeiHelpers().getGuiHelper();
      r.addRecipeCategories(
         new IRecipeCategory[]{
            new DrunkardJei.Category(gui, CRUSHING, "Grape Crushing", (ItemLike)Drunkard.TUBS.get("oak").get()),
            new DrunkardJei.Category(gui, FERMENTING, "Fermentation", (ItemLike)Drunkard.BARREL.get()),
            new DrunkardJei.Category(gui, GROWING, "Growing & Harvesting", (ItemLike)Drunkard.TRELLISES.get("oak").get()),
            new DrunkardJei.Category(gui, MASHING, "Brewing Wort", (ItemLike)Drunkard.KETTLE.get()),
            new DrunkardJei.Category(gui, DISTILLING, "Distillation", (ItemLike)Drunkard.STILL.get())
         }
      );
   }

   public void registerRecipes(IRecipeRegistration r) {
      ArrayList<DrunkardJei.Process> crush = new ArrayList<>();
      ArrayList<DrunkardJei.Process> ferment = new ArrayList<>();
      ArrayList<DrunkardJei.Process> grow = new ArrayList<>();
      ArrayList<DrunkardJei.Process> wort = new ArrayList<>();
      ArrayList<DrunkardJei.Process> distill = new ArrayList<>();

      for (String grape : Drunkard.GRAPES) {
         crush.add(
            new DrunkardJei.Process(
               List.of(new ItemStack((ItemLike)Drunkard.FRUIT.get(grape).get(), 16), new ItemStack(Items.GLASS_BOTTLE, 4)),
               List.of(new ItemStack((ItemLike)Drunkard.MUST.get(grape).get(), 4)),
               List.of("Fill with 16 grapes; stomp 8 times.")
            )
         );
         grow.add(
            new DrunkardJei.Process(
               List.of(new ItemStack((ItemLike)Drunkard.SEEDS.get(grape).get()), new ItemStack((ItemLike)Drunkard.TRELLISES.get("oak").get())),
               List.of(new ItemStack((ItemLike)Drunkard.FRUIT.get(grape).get(), 2)),
               List.of("Plant above soil; harvest when ripe.")
            )
         );
      }

      for (String drink : Drunkard.DRINKS) {
         if (Drunkard.MUST.containsKey(drink) && !Drunkard.SPIRIT_WASH.containsKey(drink)) {
            ferment.add(
               new DrunkardJei.Process(
                  List.of(
                     new ItemStack((ItemLike)Drunkard.MUST.get(drink).get()),
                     new ItemStack((ItemLike)Drunkard.YEAST.get()),
                     new ItemStack(Items.GLASS_BOTTLE, 4)
                  ),
                  List.of(new ItemStack((ItemLike)Drunkard.WINE.get(drink).get(), 4), new ItemStack(Items.GLASS_BOTTLE)),
                  List.of("60 seconds")
               )
            );
         }
      }

      for (String spirit : new String[]{"tequila", "whiskey", "vodka", "rum"}) {
         distill.add(
            new DrunkardJei.Process(
               List.of(
                  new ItemStack(Items.WATER_BUCKET),
                  new ItemStack(
                     (ItemLike)(spirit.equals("tequila")
                        ? Items.CACTUS
                        : (spirit.equals("vodka") ? Items.POTATO : (spirit.equals("rum") ? (ItemLike)Drunkard.MOLASSES.get() : (ItemLike)Drunkard.BARLEY.get())))
                  ),
                  new ItemStack(Items.COAL),
                  new ItemStack(Items.GLASS_BOTTLE, 4)
               ),
               List.of(new ItemStack((ItemLike)Drunkard.WINE.get(spirit).get(), 4)),
               List.of("20 seconds")
            )
         );
      }

      distill.add(
         new DrunkardJei.Process(
            List.of(new ItemStack((ItemLike)Drunkard.WINE.get("pinot_noir").get()), new ItemStack(Items.COAL)),
            List.of(new ItemStack((ItemLike)Drunkard.WINE.get("brandy").get())),
            List.of("20 seconds · any grape wine")
         )
      );
      ferment.add(
         new DrunkardJei.Process(
            List.of(
               new ItemStack((ItemLike)Drunkard.MUST.get("moonshine").get()),
               new ItemStack((ItemLike)Drunkard.YEAST.get()),
               new ItemStack(Items.GLASS_BOTTLE, 4)
            ),
            List.of(new ItemStack((ItemLike)Drunkard.SPIRIT_WASH.get("moonshine").get(), 4), new ItemStack(Items.GLASS_BOTTLE)),
            List.of("60 seconds · sour mash for the pot still")
         )
      );
      distill.add(
         new DrunkardJei.Process(
            List.of(
               new ItemStack((ItemLike)Drunkard.SPIRIT_WASH.get("moonshine").get(), 4), new ItemStack(Items.CAMPFIRE), new ItemStack(Items.GLASS_BOTTLE, 4)
            ),
            List.of(new ItemStack((ItemLike)Drunkard.WINE.get("moonshine").get(), 4)),
            List.of("Pot still over a lit campfire · 30 s", "Pipe it straight from the barrel")
         )
      );

      for (String kind : Drunkard.BEERS) {
         ItemStack grain = new ItemStack(
            (ItemLike)(kind.equals("dark_lager")
               ? (ItemLike)Drunkard.ROASTED_MALT.get()
               : (kind.equals("wheat_beer") ? Items.WHEAT : (kind.equals("pale_lager") ? (ItemLike)Drunkard.BARLEY.get() : (ItemLike)Drunkard.MALT.get())))
         );
         wort.add(
            new DrunkardJei.Process(
               List.of(
                  new ItemStack(Items.WATER_BUCKET),
                  grain,
                  new ItemStack((ItemLike)Drunkard.HOPS.get(), kind.equals("pale_ale") ? 2 : 1),
                  new ItemStack(Items.COAL),
                  new ItemStack(Items.GLASS_BOTTLE, 4)
               ),
               List.of(new ItemStack((ItemLike)Drunkard.MUST.get(kind).get(), 4)),
               List.of("15 seconds")
            )
         );
      }

      for (String kind : new String[]{"barley", "hops"}) {
         grow.add(
            new DrunkardJei.Process(
               List.of(
                  new ItemStack(kind.equals("barley") ? (ItemLike)Drunkard.BARLEY_SEEDS.get() : (ItemLike)Drunkard.HOPS_SEEDS.get()),
                  new ItemStack(Items.FARMLAND)
               ),
               List.of(new ItemStack(kind.equals("barley") ? (ItemLike)Drunkard.BARLEY.get() : (ItemLike)Drunkard.HOPS.get(), 2)),
               List.of("Harvest when ripe; plant regrows.")
            )
         );
      }

      grow.add(
         new DrunkardJei.Process(
            List.of(new ItemStack((ItemLike)Drunkard.TOBACCO_SEEDS.get()), new ItemStack(Items.FARMLAND)),
            List.of(new ItemStack((ItemLike)Drunkard.TOBACCO_LEAF.get(), 3)),
            List.of("Two blocks tall when ripe.")
         )
      );
      r.addRecipes(CRUSHING, crush);
      r.addRecipes(FERMENTING, ferment);
      r.addRecipes(GROWING, grow);
      r.addRecipes(MASHING, wort);
      r.addRecipes(DISTILLING, distill);
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.TRANSFER_PUMP.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{Component.literal("Input behind; pipes at the arrow. Water source underneath supplies water. Right-click pauses.")}
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.COPPER_PIPE.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{Component.literal("Connect to machine sides, top, bottom or back.")}
      );

      for (Supplier<Block> rack : Drunkard.RACKS.values()) {
         r.addIngredientInfo(
            new ItemStack((ItemLike)rack.get()),
            VanillaTypes.ITEM_STACK,
            new Component[]{Component.literal("Stores 9 drinks. Right-click to store; sneak-right-click to retrieve. Ages wine.")}
         );
      }

      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.CIGAR.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{
            Component.literal(
               "Hold use to light and puff; release after a short draw to exhale. Each puff gives Resistance I and Haste I for 45 seconds. Sneak-use switches between mouth and hand. Burns for eight minutes."
            )
         }
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.CIGAR_HOLDER.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{Component.literal("Use with a cigar or cigarette to rest it. Empty-hand use retrieves it. Lit ones keep burning.")}
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.CIGARETTE_PACK.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{
            Component.literal(
               "Holds 20 cigarettes. Use to take one out. In an inventory, right-click to take one out or put an unlit one back. Sneak-use on a surface to set it down; empty hand picks it up."
            )
         }
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.CIGARETTE.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{
            Component.literal(
               "Smoked like a cigar: hold use to light and puff, sneak-use for mouth or hand. Each puff gives Haste I and Speed I for 30 seconds. Fits the cigar holder. Burns for three minutes."
            )
         }
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.COOLER.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{
            Component.literal(
               "Holds 12 drinks in ice. Use a drink on it to add it; the drinks shuffle each time. Empty hand takes the last one out or opens and closes the lid; sneak toggles the lid. Breaking it keeps the drinks inside the item."
            )
         }
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.SHOT_GLASS.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{
            Component.literal(
               "Place it on a surface and use any drink bottle on it to pour a shot (one sip from the bottle). Pick it up with an empty hand, then use it to take the shot. A spirit shot is one drink."
            )
         }
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.MOLOTOV.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{
            Component.literal(
               "Needs a FULL, unopened bottle of vodka, whiskey, tequila or brandy, wool for the rag, string to tie it and a slime ball to thicken the fuel. Use to light and throw; it bursts into a ring of fire and ignites nearby creatures."
            )
         }
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.OLIVES.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{Component.literal("Sometimes dropped when you break oak or birch leaves (about 1 in 6). Garnish for a martini.")}
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.MARTINI.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{
            Component.literal(
               "Shake vodka, dry vermouth, ice and an olive into a martini glass. The bottles each lose one sip and the shaker is kept. Two drinks strong. Sneak-use on a table to set it down."
            )
         }
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.WINE.get("vermouth").get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{
            Component.literal(
               "Fortified, aromatised wine: craft a full bottle of Chardonnay with brandy and wormwood. The wine becomes the vermouth; the brandy loses one sip."
            )
         }
      );
      info(r, Drunkard.MARGARITA, "Tequila, lime, salt and ice cubes in a margarita glass, with the shaker. The tequila loses one sip. One and a half drinks.");
      info(r, Drunkard.OLD_FASHIONED, "Whiskey, sugar, aromatic bitters, an orange and ice cubes in a rocks glass. Two drinks.");
      info(r, Drunkard.SCREWDRIVER, "Vodka, an orange and ice cubes in a highball glass. One drink.");
      info(r, Drunkard.MOJITO, "Rum, lime, mint, sugar and ice cubes in a highball glass. About one drink.");
      info(r, Drunkard.SANGRIA, "Any red wine, brandy, an orange, an apple and sugar in a highball glass. About one drink.");
      info(
         r,
         Drunkard.ON_THE_ROCKS,
         "A rocks glass, ice cubes and any drink with at least two sips left: a double poured over ice. Chilled drinks are refreshing."
      );
      info(
         r,
         Drunkard.MUST.get("moonshine"),
         "Sweet mash: 3 wheat, 2 sugar and a water bottle. Ferment it with yeast in the fermentation barrel to get sour mash for the pot still."
      );
      info(r, Drunkard.SPIRIT_WASH.get("moonshine"), "Sour mash: fermented sweet mash. Run 2-4 bottles through the pot still to make moonshine.");
      info(r, Drunkard.ICE_CUBES, "Craft from ice or packed ice (9 cubes). Used in cocktails and drinks on the rocks.");
      info(
         r,
         Drunkard.TAP_TOWER,
         "A three-tap beer tower for the bar counter. Each tap draws from a keg up to three blocks below it: the middle tap from the keg under the tower, the side taps from the kegs under the blocks to its left and right. Use an empty beer mug on a tap to pull a pint; empty hand shows what is on tap."
      );
      info(r, Drunkard.LIME, "Sometimes dropped when you break jungle leaves (about 1 in 6).");
      info(r, Drunkard.ORANGE, "Sometimes dropped when you break acacia leaves (about 1 in 6).");
      info(r, Drunkard.SALT, "Smelt a water bottle, or dig sand on beaches and ocean shores (about 1 in 8).");
      info(r, Drunkard.MOLASSES, "Smelt sugar cane. Distil it into rum.");
      info(r, Drunkard.BITTERS, "Pour vodka over wormwood and orange peel into a glass bottle.");
      info(r, Drunkard.MINT, "Harvest wild mint in forests and swamps.");
      info(r, Drunkard.WORMWOOD, "Harvest wild wormwood in plains, meadows and savannas. Flavours vermouth and bitters.");
      info(
         r,
         Drunkard.PIPE,
         "Hold tobacco leaf or dried tobacco in the other hand and use the pipe to pack and light it, then smoke it like a cigar. Each puff gives Regeneration I for 10 seconds and Luck for a minute. Burns for six minutes and can be packed again."
      );
      info(
         r,
         Drunkard.HUMIDOR,
         "Stores 8 unlit cigars or cigarettes. They age while inside: 3 days make them aged (stronger, longer buffs), 10 days make them vintage."
      );
      info(
         r,
         Drunkard.KEG,
         "A steel beer keg. Use a full bottle of beer on it to fill it (up to 32 pints of one beer); transfer pumps can fill it too. Use an empty beer mug to draw a pint, or put it under a counter and pour from a tap tower above. Keeps its beer when broken."
      );
      info(r, Drunkard.BEER_MUG, "Draw a pint from a keg. The mug comes back when you finish it.");
      info(
         r,
         Drunkard.POT_STILL,
         "Pour in 2-4 bottles of sour mash and set it on a lit campfire. After 30 seconds, draw off one jar per bottle with empty glass bottles. Copper pipes connect on the back, sides and top: piped sour mash keeps its bottle inside, the still bottles its moonshine by itself, and a transfer pump behind it pulls the jars out."
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.BAR_STOOL.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{Component.literal("Use with an empty hand to sit. Sneak to stand up.")}
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.BEACH_CHAIR.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{Component.literal("Use with an empty hand to sit back. Sneak to stand up.")}
      );
      r.addIngredientInfo(
         new ItemStack((ItemLike)Drunkard.DART_BOARD.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{Component.literal("Bar decoration. Hang it on the side of a block.")}
      );

      for (Supplier<Item> drinkx : Drunkard.WINE.values()) {
         float abv = WineData.defaultAlcohol(new ItemStack((ItemLike)drinkx.get()));
         r.addIngredientInfo(
            new ItemStack((ItemLike)drinkx.get()),
            VanillaTypes.ITEM_STACK,
            new Component[]{
               Component.literal(
                  String.format(
                     Locale.ROOT,
                     "%.1f%% ABV. Ten sips per bottle; each sip is %.1f standard drinks (one spirit shot = 1.0). You black out at 10 drinks in your body.",
                     abv,
                     AlcoholSystem.drinksPerSip(abv)
                  )
               )
            }
         );
      }

      Map<String, String> locations = Map.of(
         "grenache",
         "Savanna, Savanna Plateau, Windswept Savanna",
         "merlot",
         "Forest, Flower Forest, Birch Forest",
         "chardonnay",
         "Plains, Meadow",
         "pinot_noir",
         "Dark Forest, Old Growth Birch Forest, Taiga",
         "barley",
         "Plains, Meadow",
         "hops",
         "Forest, Taiga, Swamp",
         "tobacco",
         "Savanna, Plains, Sparse Jungle",
         "mint",
         "Forest, Flower Forest, Birch Forest, Swamp",
         "wormwood",
         "Plains, Meadow, Savanna"
      );

      for (Entry<String, Supplier<Block>> plant : Drunkard.WILD.entrySet()) {
         r.addIngredientInfo(
            new ItemStack((ItemLike)plant.getValue().get()),
            VanillaTypes.ITEM_STACK,
            new Component[]{
               Component.literal(
                  "Wild plant: find in "
                     + locations.get(plant.getKey())
                     + (
                        !plant.getKey().equals("mint") && !plant.getKey().equals("wormwood")
                           ? ". Harvest for seeds or crops; replant on suitable soil."
                           : ". Harvest it and it grows back."
                     )
               )
            }
         );
      }
   }

   private static void info(IRecipeRegistration r, Supplier<? extends ItemLike> item, String text) {
      r.addIngredientInfo(new ItemStack(item.get()), VanillaTypes.ITEM_STACK, new Component[]{Component.literal(text)});
   }

   public void registerRecipeCatalysts(IRecipeCatalystRegistration r) {
      r.addRecipeCatalyst(new ItemStack((ItemLike)Drunkard.BARREL.get()), new RecipeType[]{FERMENTING});
      r.addRecipeCatalyst(new ItemStack((ItemLike)Drunkard.KETTLE.get()), new RecipeType[]{MASHING});
      r.addRecipeCatalyst(new ItemStack((ItemLike)Drunkard.STILL.get()), new RecipeType[]{DISTILLING});
      r.addRecipeCatalyst(new ItemStack((ItemLike)Drunkard.POT_STILL.get()), new RecipeType[]{DISTILLING});

      for (Supplier<Block> tub : Drunkard.TUBS.values()) {
         r.addRecipeCatalyst(new ItemStack((ItemLike)tub.get()), new RecipeType[]{CRUSHING});
      }

      for (Supplier<Block> trellis : Drunkard.TRELLISES.values()) {
         r.addRecipeCatalyst(new ItemStack((ItemLike)trellis.get()), new RecipeType[]{GROWING});
      }

      r.addRecipeCatalyst(new ItemStack(Items.FARMLAND), new RecipeType[]{GROWING});
   }

   public void registerRecipeTransferHandlers(IRecipeTransferRegistration r) {
      r.addRecipeTransferHandler(BarrelMenu.class, Drunkard.BARREL_MENU.get(), FERMENTING, 0, 3, 4, 36);
   }

   public void registerGuiHandlers(IGuiHandlerRegistration r) {
      r.addRecipeClickArea(BarrelScreen.class, 59, 82, 58, 8, new RecipeType[]{FERMENTING});
   }

   private static class Category implements IRecipeCategory<DrunkardJei.Process> {
      private final RecipeType<DrunkardJei.Process> type;
      private final Component title;
      private final IDrawable icon;
      private final IDrawable background;
      private final IDrawable slot;

      Category(IGuiHelper gui, RecipeType<DrunkardJei.Process> type, String title, ItemLike item) {
         this.type = type;
         this.title = Component.literal(title);
         this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(item));
         this.background = gui.createBlankDrawable(174, 68);
         this.slot = gui.getSlotDrawable();
      }

      public RecipeType<DrunkardJei.Process> getRecipeType() {
         return this.type;
      }

      public Component getTitle() {
         return this.title;
      }

      public IDrawable getBackground() {
         return this.background;
      }

      public IDrawable getIcon() {
         return this.icon;
      }

      public void setRecipe(IRecipeLayoutBuilder b, DrunkardJei.Process p, IFocusGroup f) {
         for (int i = 0; i < p.inputs().size(); i++) {
            IRecipeSlotBuilder s = b.addSlot(RecipeIngredientRole.INPUT, 7 + i * 20, 15).setBackground(this.slot, -1, -1);
            if (this.type == DrunkardJei.GROWING && i == 1 && p.inputs().get(1).getItem() instanceof BlockItem bi && bi.getBlock() instanceof TrellisBlock) {
               s.addItemStacks(Drunkard.TRELLISES.values().stream().map(t -> new ItemStack((ItemLike)t.get())).toList());
            } else if (p.inputs().get(i).is(Items.COAL)) {
               s.addItemStacks(List.of(new ItemStack(Items.COAL), new ItemStack(Items.CHARCOAL)));
            } else if (this.type == DrunkardJei.DISTILLING && p.inputs().get(i).is(Drunkard.BARLEY.get())) {
               s.addItemStacks(List.of(new ItemStack((ItemLike)Drunkard.BARLEY.get()), new ItemStack((ItemLike)Drunkard.MALT.get())));
            } else if (this.type == DrunkardJei.DISTILLING && p.inputs().get(i).is(Drunkard.SPIRIT_WASH.get("tequila").get())) {
               s.addItemStacks(List.of(p.inputs().get(i), new ItemStack((ItemLike)Drunkard.MUST.get("tequila").get())));
            } else if (this.type == DrunkardJei.DISTILLING && p.inputs().get(i).is(Drunkard.SPIRIT_WASH.get("whiskey").get())) {
               s.addItemStacks(List.of(p.inputs().get(i), new ItemStack((ItemLike)Drunkard.MUST.get("whiskey").get())));
            } else {
               s.addItemStack(p.inputs().get(i));
            }
         }

         for (int ix = 0; ix < p.outputs().size(); ix++) {
            b.addSlot(RecipeIngredientRole.OUTPUT, 124 + ix * 26, 15).setBackground(this.slot, -1, -1).addItemStack(p.outputs().get(ix));
         }
      }

      public void draw(DrunkardJei.Process p, IRecipeSlotsView slots, GuiGraphics g, double x, double y) {
         Font font = Minecraft.getInstance().font;
         g.drawString(font, "→", 108, 19, -8824012, false);

         for (int i = 0; i < p.notes().size(); i++) {
            g.drawString(font, p.notes().get(i), 5, 45 + i * 12, -12240596, false);
         }
      }
   }

   public static record Process(List<ItemStack> inputs, List<ItemStack> outputs, List<String> notes) {
   }
}
