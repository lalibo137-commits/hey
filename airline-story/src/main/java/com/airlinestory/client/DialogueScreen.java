package com.airlinestory.client;

import com.airlinestory.dialogue.Dialogue;
import com.airlinestory.net.Net;
import com.airlinestory.story.Snd;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conversations, framed like a choice-driven adventure game: letterboxed view of the person you're
 * talking to, their line typed out, and up to four answers (keys 1-4). Some answers are on a timer.
 */
public class DialogueScreen extends Screen {
    private final Dialogue tree;
    private final int npcId;
    @Nullable private final Entity npc;
    private Dialogue.Node node;
    private int age;
    private int typed;
    private int timer;
    private final List<int[]> boxes = new ArrayList<>();
    private final List<Integer> visible = new ArrayList<>();
    private boolean sent;

    public DialogueScreen(Dialogue tree, String start, int npcId, @Nullable Entity npc) {
        super(Component.literal(tree.speaker));
        this.tree = tree;
        this.npcId = npcId;
        this.npc = npc;
        Dialogue.Node n = tree.node(start);
        this.node = n == null ? tree.node("start") : n;
        enterNode();
    }

    private void enterNode() {
        age = 0;
        typed = 0;
        timer = node == null ? 0 : node.timer;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String speaker() {
        if (node != null && node.speaker != null) return node.speaker;
        if (npc != null && npc.hasCustomName()) return npc.getCustomName().getString();
        return tree.speaker;
    }

    private static String fill(String s) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        String name = mc.player == null ? "Passenger" : mc.player.getGameProfile().getName();
        return s.replace("{player}", name).replace("{rating}", String.format(Locale.ROOT, "%.2f", ClientStory.INSTANCE.rating));
    }

    @Override
    public void tick() {
        if (node == null) { onClose(); return; }
        age++;
        String text = fill(node.text);
        if (typed < text.length()) {
            typed = Math.min(text.length(), typed + 2);
            if (age % 3 == 0 && !Character.isWhitespace(text.charAt(Math.max(0, typed - 1)))) {
                ClientFx.play(Snd.of(SoundEvents.NOTE_BLOCK_HAT), 0.15f, 1.6f + (age % 5) * 0.05f);
            }
        } else if (timer > 0) {
            timer--;
            if (timer == 0 && node.timeoutChoice >= 0) choose(node.timeoutChoice);
        }
        turnTowardsNpc();
    }

    private void turnTowardsNpc() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (npc == null || mc.player == null || !npc.isAlive()) return;
        double dx = npc.getX() - mc.player.getX(), dz = npc.getZ() - mc.player.getZ();
        double dy = (npc.getEyeY() - 0.1) - mc.player.getEyeY();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        float cy = mc.player.getYRot(), cp = mc.player.getXRot();
        mc.player.setYRot(cy + Mth.wrapDegrees(yaw - cy) * 0.25f);
        mc.player.setXRot(cp + (pitch - cp) * 0.25f);
        mc.player.setYHeadRot(mc.player.getYRot());
    }

    private void choose(int index) {
        if (node == null || index < 0 || index >= node.choices.size()) return;
        Dialogue.Choice c = node.choices.get(index);
        Net.toServer(new Net.Choice(npcId, tree.id, node.id, index));
        ClientFx.play(Snd.of(SoundEvents.UI_BUTTON_CLICK), 0.5f, 1f);
        if (c.next == null) {
            sent = true;
            minecraft.setScreen(null);
            return;
        }
        Dialogue.Node next = tree.node(c.next);
        if (next == null) {
            sent = true;
            minecraft.setScreen(null);
            return;
        }
        node = next;
        enterNode();
    }

    private boolean textDone() {
        return node != null && typed >= fill(node.text).length();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_4) {
            if (!textDone()) { typed = Integer.MAX_VALUE / 2; return true; }
            int slot = key - GLFW.GLFW_KEY_1;
            if (slot < visible.size()) choose(visible.get(slot));
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            if (!textDone()) { typed = Integer.MAX_VALUE / 2; return true; }
            if (visible.size() == 1) choose(visible.get(0));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!textDone()) { typed = Integer.MAX_VALUE / 2; return true; }
        for (int i = 0; i < boxes.size(); i++) {
            int[] b = boxes.get(i);
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                choose(visible.get(i));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void onClose() {
        if (!sent) Net.toServer(new Net.CloseDialogue(npcId));
        sent = true;
        super.onClose();
    }

    @Override
    public void removed() {
        if (!sent) Net.toServer(new Net.CloseDialogue(npcId));
        sent = true;
    }

    @Override
    public void renderBackground(GuiGraphics g) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (node == null) return;
        int w = width, h = height;
        float open = Math.min(1f, (age + partial) / 8f);
        int bar = (int) (h / 8f * open);
        g.fill(0, 0, w, bar, 0xFF000000);
        int panelH = Math.max(70, h / 4);
        g.fill(0, h - (int) (panelH * open), w, h, 0xE6000000);
        int top = h - panelH;

        int textX = Math.max(20, w / 2 - 200);
        int textW = Math.min(400, w - 40);
        if (npc instanceof LivingEntity le && npc.isAlive() && w > 300) {
            int size = Math.min(30, (panelH - 12) / 2);
            int px = 14 + size / 2 + 8, py = h - 6;
            g.fill(px - size / 2 - 8, top + 4, px + size / 2 + 8, h - 4, 0x60303040);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g, px, py, size,
                    (float) (px - mouseX), (float) (py - size * 1.6f - mouseY), le);
            int minX = px + size / 2 + 16;
            if (textX < minX) { textW -= minX - textX; textX = minX; }
        }

        String speaker = speaker();
        g.drawString(font, speaker.toUpperCase(Locale.ROOT), textX, top + 8, speakerColor(speaker), true);
        String text = fill(node.text);
        String shown = text.substring(0, Math.min(text.length(), typed));
        int ty = top + 22;
        for (FormattedCharSequence line : VanillaGui.wrap(font, shown, textW)) {
            g.drawString(font, line, textX, ty, 0xFFFFFFFF, true);
            ty += 10;
        }

        boxes.clear();
        visible.clear();
        if (textDone()) {
            for (int i = 0; i < node.choices.size(); i++) if (node.choices.get(i).visible(ClientStory.INSTANCE)) visible.add(i);
            int n = visible.size();
            int bw = Math.min(196, (w - 30) / 2), bh = 22;
            int gridW = n == 1 ? bw : bw * 2 + 8;
            int gx = (w - gridW) / 2;
            int rows = (n + 1) / 2;
            int gy = top - 10 - rows * (bh + 6);
            for (int k = 0; k < n; k++) {
                int col = n == 1 ? 0 : k % 2, row = n == 1 ? 0 : k / 2;
                int bx = gx + col * (bw + 8), by = gy + row * (bh + 6);
                boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
                Dialogue.Choice c = node.choices.get(visible.get(k));
                g.fill(bx, by, bx + bw, by + bh, hover ? 0xE0303050 : 0xC0101018);
                int border = hover ? 0xFFFFFF55 : 0xFF707080;
                g.fill(bx, by, bx + bw, by + 1, border);
                g.fill(bx, by + bh - 1, bx + bw, by + bh, border);
                g.fill(bx, by, bx + 1, by + bh, border);
                g.fill(bx + bw - 1, by, bx + bw, by + bh, border);
                g.drawString(font, "[" + (k + 1) + "]", bx + 5, by + 7, 0xFFFFFF55, true);
                String label = fill(c.text);
                List<FormattedCharSequence> ls = font.split(Component.literal(label), bw - 30);
                int ly = by + (ls.size() > 1 ? 2 : 7);
                for (int j = 0; j < Math.min(2, ls.size()); j++) {
                    g.drawString(font, ls.get(j), bx + 24, ly, hover ? 0xFFFFFFFF : 0xFFDDDDDD, true);
                    ly += 10;
                }
                boxes.add(new int[]{bx, by, bw, bh});
            }
            if (node.timer > 0 && timer > 0) {
                float frac = (timer - partial) / node.timer;
                int tw = (int) (gridW * Math.max(0, frac));
                int tyBar = gy - 8;
                g.fill(gx, tyBar, gx + gridW, tyBar + 3, 0x80000000);
                g.fill(gx + (gridW - tw) / 2, tyBar, gx + (gridW + tw) / 2, tyBar + 3, frac > 0.3f ? 0xFFFFFFFF : 0xFFFF5555);
            }
        } else if (age > 10) {
            g.drawString(font, "▼", textX + textW - 10, h - 14, 0xFF888888 | ((age / 8) % 2 == 0 ? 0 : 0), false);
        }
        ClientFx.renderTop(g, w, h);
    }

    private static int speakerColor(String s) {
        return switch (s) {
            case "HALO", "HALO Terminal" -> 0xFFD080FF;
            case "You", "Mirror", "Photographs" -> 0xFFAAAAAA;
            case "Departures", "SmileScore", "Seat-back Screen", "Keypad", "Transmitter", "Cockpit Door", "The Boat" -> 0xFF7FC8FF;
            default -> 0xFFFFDD55;
        };
    }
}
