package com.airlinestory.client;

import com.airlinestory.AirlineStory;
import com.airlinestory.entity.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AirlineStory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    public static final KeyMapping JOURNAL = new KeyMapping("key.airlinestory.journal", InputConstants.KEY_J, "key.categories.airlinestory");
    public static final KeyMapping TRACKER = new KeyMapping("key.airlinestory.tracker", InputConstants.KEY_K, "key.categories.airlinestory");

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.NPC.get(), NpcRenderer::new);
        event.registerEntityRenderer(ModEntities.HOLLOW.get(), HollowRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAT.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(JOURNAL);
        event.register(TRACKER);
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("story_hud", (gui, g, partial, w, h) -> {
            if (!gui.getMinecraft().options.hideGui || CutscenePlayer.isPlaying() || ClientFx.black) ClientFx.renderHud(g, w, h);
        });
    }
}
