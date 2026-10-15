package com.airlinestory.client;

import com.airlinestory.entity.StoryNpc;
import com.airlinestory.net.Net;
import com.airlinestory.story.Snd;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** After a conversation in Meridian's world, everyone rates everyone. */
public class RatingScreen extends Screen {
    private final StoryNpc npc;
    private int hover;
    private int age;

    public RatingScreen(StoryNpc npc) {
        super(Component.literal("Rate"));
        this.npc = npc;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g) {}
    @Override public void tick() { age++; }

    private int starX(int i) { return width / 2 - 60 + i * 24; }
    private int starY() { return height / 2 + 10; }

    private void rate(int stars) {
        Net.toServer(new Net.Rate(npc.getId(), stars));
        ClientFx.play(Snd.of(SoundEvents.NOTE_BLOCK_CHIME), 0.7f, 0.8f + stars * 0.15f);
        minecraft.setScreen(null);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_5) { rate(key - GLFW.GLFW_KEY_1 + 1); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE) { rate(3); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < 5; i++) {
            if (mx >= starX(i) - 10 && mx < starX(i) + 12 && my >= starY() - 4 && my < starY() + 18) { rate(i + 1); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        float a = Math.min(1f, (age + partial) / 6f);
        int bw = 200, bh = 74, bx = width / 2 - bw / 2, by = height / 2 - 34;
        VanillaGui.hudBox(g, bx, by, bw, bh, 0xFFFFD020);
        g.drawCenteredString(font, "RATE YOUR INTERACTION", width / 2, by + 6, 0xFF9A9A9A);
        String who = npc.displayName() + String.format(Locale.ROOT, "   ★ %.1f", Math.max(0, npc.smileScore()));
        g.drawCenteredString(font, who, width / 2, by + 18, 0xFFFFFFFF);
        hover = -1;
        for (int i = 0; i < 5; i++) {
            int x = starX(i), y = starY();
            if (mouseX >= x - 10 && mouseX < x + 12 && mouseY >= y - 4 && mouseY < y + 18) hover = i;
        }
        for (int i = 0; i < 5; i++) {
            boolean lit = i <= hover;
            g.pose().pushPose();
            g.pose().translate(starX(i), starY(), 0);
            g.pose().scale(2f, 2f, 1f);
            g.drawCenteredString(font, lit ? "★" : "☆", 0, 0, lit ? 0xFFFFD020 : 0xFF808080);
            g.pose().popPose();
        }
        g.drawCenteredString(font, "Click a star, or press 1-5. Everyone is rated.", width / 2, by + bh - 12, VanillaGui.alpha(a * 0.8f) | 0x909090);
    }
}
