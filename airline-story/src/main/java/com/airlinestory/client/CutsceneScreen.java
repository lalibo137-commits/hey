package com.airlinestory.client;

import com.airlinestory.story.Cutscenes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Letterbox, captions and fades during a cutscene. Holding SPACE skips. */
public class CutsceneScreen extends net.minecraft.client.gui.screens.Screen {
    private int skipHeld;

    public CutsceneScreen() {
        super(Component.literal("Cutscene"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        long win = minecraft.getWindow().getWindow();
        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            if (++skipHeld > 30) CutscenePlayer.skip();
        } else {
            skipHeld = 0;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int w = width, h = height;
        Cutscenes.Shot s = CutscenePlayer.current();
        int bar = h / 9;
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        if (s == null) return;
        int tt = CutscenePlayer.shotTicks();
        float fade = 0;
        if (s.black) fade = 1;
        if (s.fadeIn > 0 && tt < s.fadeIn) fade = Math.max(fade, 1 - (tt + partial) / s.fadeIn);
        if (s.fadeOut > 0 && tt > s.ticks - s.fadeOut) fade = Math.max(fade, (tt + partial - (s.ticks - s.fadeOut)) / s.fadeOut);
        if (fade > 0) g.fill(0, bar, w, h - bar, VanillaGui.alpha(Math.min(1, fade)));
        if (!s.title.isEmpty()) {
            float a = Math.min(1f, Math.min((tt - 10) / 20f, (s.ticks - tt) / 20f));
            if (a > 0.02f) {
                int alpha = VanillaGui.alpha(a);
                g.pose().pushPose();
                g.pose().translate(w / 2f, h * 0.66f, 0);
                g.pose().scale(2f, 2f, 1f);
                g.drawCenteredString(font, s.title, 0, 0, 0xFFFFFF | alpha);
                g.pose().popPose();
                if (!s.sub.isEmpty()) g.drawCenteredString(font, s.sub, w / 2, (int) (h * 0.66f) + 22, 0xBBBBBB | alpha);
            }
        }
        if (skipHeld > 0) g.drawString(font, "Skipping...", w - 70, h - bar / 2 - 4, 0xFF888888, false);
        else g.drawString(font, "Hold SPACE to skip", w - font.width("Hold SPACE to skip") - 8, h - bar / 2 - 4, 0xFF555555, false);
        ClientFx.renderTop(g, w, h);
    }

    @Override
    public void renderBackground(GuiGraphics g) {}
}
