package com.airlinestory.client;

import com.airlinestory.AirlineStory;
import com.airlinestory.story.StoryBoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.util.Random;

/** Opened by the title screen's "Start Airline Story" button. */
public class StoryStartScreen extends Screen {
    public static final String LEVEL_ID = "airline_story";
    private static final ResourceKey<WorldPreset> PRESET = ResourceKey.create(Registries.WORLD_PRESET, AirlineStory.id("story"));
    private final Screen parent;

    public StoryStartScreen(Screen parent) {
        super(Component.literal("Airline Story"));
        this.parent = parent;
    }

    private static boolean saveExists() {
        try {
            return Minecraft.getInstance().getLevelSource().levelExists(LEVEL_ID);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void init() {
        int x = width / 2 - 100, y = height / 2 - 30;
        boolean exists = saveExists();
        Button cont = addRenderableWidget(Button.builder(Component.literal("Continue Flight 417"), b -> continueStory()).bounds(x, y, 200, 20).build());
        cont.active = exists;
        addRenderableWidget(Button.builder(Component.literal(exists ? "New Story (restart)" : "New Story"), b -> {
            if (saveExists()) {
                minecraft.setScreen(new ConfirmScreen(ok -> {
                    if (ok) newStory();
                    else minecraft.setScreen(this);
                }, Component.literal("Start over?"), Component.literal("Your current Airline Story world will be deleted. Every choice you made will be forgotten. Meridian will not forget.")));
            } else {
                newStory();
            }
        }).bounds(x, y + 24, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Other Singleplayer Worlds..."), b -> minecraft.setScreen(new SelectWorldScreen(this))).bounds(x, y + 48, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> minecraft.setScreen(parent)).bounds(x, y + 80, 200, 20).build());
    }

    private void continueStory() {
        minecraft.createWorldOpenFlows().loadLevel(this, LEVEL_ID);
    }

    private void newStory() {
        Minecraft mc = Minecraft.getInstance();
        if (saveExists()) {
            try (LevelStorageSource.LevelStorageAccess access = mc.getLevelSource().createAccess(LEVEL_ID)) {
                access.deleteLevel();
            } catch (Exception e) {
                AirlineStory.LOG.error("Could not delete the old story world", e);
            }
        }
        StoryBoot.markNewStory();
        GameRules rules = new GameRules();
        LevelSettings settings = new LevelSettings("Airline Story: Flight 417", GameType.ADVENTURE, false, Difficulty.NORMAL, false, rules,
                WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(new Random().nextLong(), false, false);
        mc.forceSetScreen(new GenericDirtMessageScreen(Component.literal("Checking in...")));
        mc.createWorldOpenFlows().createFreshLevel(LEVEL_ID, settings, options, StoryStartScreen::dimensions);
    }

    private static WorldDimensions dimensions(RegistryAccess access) {
        return access.registryOrThrow(Registries.WORLD_PRESET).getHolder(PRESET)
                .map(h -> h.value().createWorldDimensions())
                .orElseGet(() -> {
                    AirlineStory.LOG.warn("Airline Story world preset missing, using the default world");
                    return WorldPresets.createNormalWorldDimensions(access);
                });
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f - 92, 0);
        g.pose().scale(2.5f, 2.5f, 1f);
        g.drawCenteredString(font, "FLIGHT 417", 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
        g.drawCenteredString(font, "Meridian Air • Port Ellery • Gate B7 • Departs 9:10", width / 2, height / 2 - 62, 0xFF7FC8FF);
        g.drawCenteredString(font, "A single-player psychological horror story. Several hours long.", width / 2, height / 2 - 48, 0xFFAAAAAA);
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(font, "Everyone is rated.", width / 2, height - 20, 0xFF555555);
    }
}
