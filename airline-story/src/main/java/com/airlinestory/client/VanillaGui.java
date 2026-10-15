package com.airlinestory.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Draws panels the way vanilla containers look: light grey, white highlight, dark shadow, black rim. */
public final class VanillaGui {
    private VanillaGui() {}

    public static final int TEXT = 0xFF404040;
    public static final int BG = 0xFFC6C6C6;

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + 1, 0xFF000000);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + 1, y + h - 1, 0xFF000000);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BG);
        g.fill(x + 1, y + 1, x + w - 3, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + h - 3, 0xFFFFFFFF);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
    }

    /** A sunken slot-like area (inventory slot style). */
    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF8B8B8B);
        g.fill(x, y, x + w - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + h - 1, 0xFF373737);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFFFFFFFF);
    }

    /** Dark translucent box with a thin border, for HUD messages. */
    public static void hudBox(GuiGraphics g, int x, int y, int w, int h, int border) {
        g.fill(x, y, x + w, y + h, 0xC0100010);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }

    /** Word-wraps text, honouring explicit line breaks. */
    public static List<FormattedCharSequence> wrap(Font font, String text, int width) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            if (line.isEmpty()) { out.add(FormattedCharSequence.EMPTY); continue; }
            out.addAll(font.split(Component.literal(line), width));
        }
        return out;
    }

    public static int alpha(float a) {
        return Math.max(0, Math.min(255, (int) (a * 255))) << 24;
    }
}
