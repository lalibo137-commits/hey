package com.airlinestory.client;

import com.airlinestory.AirlineStory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = AirlineStory.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    /** Replace the title screen's Singleplayer button with "Start Airline Story". */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen title)) return;
        for (GuiEventListener l : new ArrayList<>(event.getListenersList())) {
            if (l instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc && tc.getKey().equals("menu.singleplayer")) {
                event.removeListener(b);
                AbstractWidget story = Button.builder(Component.literal("Start Airline Story"), btn -> Minecraft.getInstance().setScreen(new StoryStartScreen(title)))
                        .bounds(b.getX(), b.getY(), b.getWidth(), b.getHeight()).build();
                event.addListener(story);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        CutscenePlayer.tick();
        DevAutotest.tick();
        ClientFx.tick();
        if (mc.player == null) return;
        while (ClientModEvents.JOURNAL.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new JournalScreen());
        }
        while (ClientModEvents.TRACKER.consumeClick()) ClientFx.hudHidden = !ClientFx.hudHidden;
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float[] s = ClientFx.shake((float) event.getPartialTick());
        if (s[0] == 0 && s[1] == 0 && s[2] == 0) return;
        event.setYaw(event.getYaw() + s[0]);
        event.setPitch(event.getPitch() + s[1]);
        event.setRoll(event.getRoll() + s[2]);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStory.INSTANCE.reset();
        ClientFx.reset();
        if (CutscenePlayer.isPlaying()) CutscenePlayer.finish(false);
    }
}
