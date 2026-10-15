package com.airlinestory.client;

import com.airlinestory.story.FlagView;
import com.airlinestory.story.Locations;
import com.airlinestory.story.Objective;
import com.airlinestory.story.Quest;
import com.airlinestory.story.Quests;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The Travel Journal: what to do next, who to talk to, where to go, plus the story so far. */
public class JournalScreen extends Screen {
    private static int tab;
    private static final String[] TABS = {"Tasks", "Story", "Clues", "People", "Score"};
    private static final int W = 256, H = 210;
    private int left, top;
    private int scroll;
    private final List<Button> tabButtons = new ArrayList<>();

    public JournalScreen() {
        super(Component.literal("Travel Journal"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        tabButtons.clear();
        int bw = (W - 16) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            Button b = Button.builder(Component.literal(TABS[i]), btn -> { tab = t; scroll = 0; refresh(); })
                    .bounds(left + 8 + i * bw, top + 18, bw - 2, 18).build();
            tabButtons.add(addRenderableWidget(b));
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(left + W / 2 - 40, top + H - 26, 80, 20).build());
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < tabButtons.size(); i++) tabButtons.get(i).active = i != tab;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, scroll - (int) (delta * 10));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ClientModEvents.JOURNAL.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        VanillaGui.panel(g, left, top, W, H);
        g.drawString(font, "Travel Journal", left + 8, top + 6, VanillaGui.TEXT, false);
        String rating = String.format(Locale.ROOT, "SmileScore %.2f", ClientStory.INSTANCE.rating);
        if (!ClientStory.INSTANCE.has("ending_free")) g.drawString(font, rating, left + W - 8 - font.width(rating), top + 6, 0xFF7A5A00, false);
        int ix = left + 7, iy = top + 40, iw = W - 14, ih = H - 72;
        VanillaGui.inset(g, ix, iy, iw, ih);
        g.enableScissor(ix + 1, iy + 1, ix + iw - 1, iy + ih - 1);
        List<Line> lines = switch (tab) {
            case 1 -> storyLines();
            case 2 -> clueLines();
            case 3 -> peopleLines();
            case 4 -> ratingLines();
            default -> objectiveLines();
        };
        int maxScroll = Math.max(0, lines.size() * 10 - (ih - 8));
        scroll = Math.min(scroll, maxScroll);
        int y = iy + 4 - scroll;
        for (Line l : lines) {
            if (y > iy - 10 && y < iy + ih) {
                if (l.seq != null) g.drawString(font, l.seq, ix + 4 + l.indent, y, l.color, l.shadow);
            }
            y += 10;
        }
        g.disableScissor();
        if (maxScroll > 0) {
            int barH = Math.max(12, (ih - 2) * (ih - 8) / (lines.size() * 10));
            int barY = iy + 1 + (int) ((ih - 2 - barH) * (scroll / (float) maxScroll));
            g.fill(ix + iw - 4, barY, ix + iw - 1, barY + barH, 0xFF555555);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    private record Line(FormattedCharSequence seq, int color, int indent, boolean shadow) {}

    private void text(List<Line> out, String s, int color, int indent) {
        for (FormattedCharSequence l : VanillaGui.wrap(font, s, W - 26 - indent)) out.add(new Line(l, color, indent, false));
    }

    private void blank(List<Line> out) {
        out.add(new Line(FormattedCharSequence.EMPTY, 0, 0, false));
    }

    private List<Line> objectiveLines() {
        List<Line> out = new ArrayList<>();
        FlagView v = ClientStory.INSTANCE;
        Quest q = Quests.get(v.quest());
        if (!ClientStory.INSTANCE.active || q == null) {
            text(out, "This journal belongs to a passenger of Meridian Flight 417.", VanillaGui.TEXT, 0);
            blank(out);
            text(out, "The story only runs in an Airline Story world. Start one with the \"Start Airline Story\" button on the title screen.", 0xFF6B0000, 0);
            return out;
        }
        String ch = Quests.CHAPTERS[Math.min(q.chapter, Quests.CHAPTERS.length - 1)];
        text(out, "CHAPTER " + q.chapter + ": " + ch.toUpperCase(Locale.ROOT), 0xFF1F3F8F, 0);
        text(out, q.title, 0xFF000000, 0);
        blank(out);
        text(out, q.description, VanillaGui.TEXT, 0);
        blank(out);
        String who = null, where = null, todo = null;
        if (!q.objectives.isEmpty()) {
            text(out, "Objectives:", 0xFF000000, 0);
            for (int i = 0; i < q.objectives.size(); i++) {
                Objective o = q.objectives.get(i);
                boolean done = o.isDone(v, q.id, i);
                text(out, (done ? "✔ " : "☐ ") + o.display(v), done ? 0xFF2E7D32 : VanillaGui.TEXT, 4);
                if (!done) {
                    if (who == null && o.npcName != null) who = o.npcName;
                    if (where == null && o.locId != null) where = o.locId;
                    if (todo == null) todo = o.hint;
                }
            }
            blank(out);
        }
        if (who != null) {
            text(out, "Who do I talk to?", 0xFF1F3F8F, 0);
            text(out, who + (where != null && Locations.get(where) != null ? " (" + Locations.get(where).label() + ")" : ""), VanillaGui.TEXT, 6);
        }
        if (where != null) {
            String dir = ClientFx.direction(where);
            Locations.Loc loc = Locations.get(where);
            text(out, "Where do I go?", 0xFF1F3F8F, 0);
            text(out, dir != null ? dir : loc != null ? loc.label() : where, VanillaGui.TEXT, 6);
        }
        if (todo != null) {
            text(out, "What am I supposed to do?", 0xFF1F3F8F, 0);
            text(out, todo, VanillaGui.TEXT, 6);
        }
        for (Quest sq : Quests.sides()) {
            if (!sq.active(v)) continue;
            text(out, "Side quest: " + sq.title, 0xFF4A3F9F, 0);
            text(out, sq.description, VanillaGui.TEXT, 4);
            for (int i = 0; i < sq.objectives.size(); i++) {
                Objective o = sq.objectives.get(i);
                boolean done = o.isDone(v, sq.id, i);
                text(out, (done ? "✔ " : o.optional ? "○ " : "☐ ") + o.display(v), done ? 0xFF2E7D32 : VanillaGui.TEXT, 8);
                if (!done && !o.optional) text(out, o.hint, 0xFF707070, 12);
            }
            blank(out);
        }
        blank(out);
        text(out, "Tips: right-click people to talk, seats to sit (sneak to stand), and story objects to use them. Press 1-4 to answer in conversations.", 0xFF707070, 0);
        return out;
    }

    private List<Line> storyLines() {
        List<Line> out = new ArrayList<>();
        int lastCh = -1;
        for (String id : ClientStory.INSTANCE.completed) {
            Quest q = Quests.get(id);
            if (q == null) continue;
            if (q.chapter != lastCh) {
                if (lastCh != -1) blank(out);
                text(out, "Chapter " + q.chapter + ": " + Quests.CHAPTERS[Math.min(q.chapter, Quests.CHAPTERS.length - 1)], 0xFF1F3F8F, 0);
                lastCh = q.chapter;
            }
            text(out, "✔ " + q.title, VanillaGui.TEXT, 4);
        }
        if (out.isEmpty()) text(out, "Nothing yet. Your trip is only just beginning.", VanillaGui.TEXT, 0);
        return out;
    }

    private List<Line> clueLines() {
        List<Line> out = new ArrayList<>();
        int found = 0;
        String act = "";
        for (com.airlinestory.story.Clues.Clue c : com.airlinestory.story.Clues.all()) {
            if (!ClientStory.INSTANCE.has("clue:" + c.id())) continue;
            if (!c.act().equals(act)) {
                if (found > 0) blank(out);
                act = c.act();
                text(out, act.toUpperCase(Locale.ROOT), 0xFF1F3F8F, 0);
            }
            found++;
            text(out, c.title(), 0xFF000000, 2);
            text(out, c.text(), VanillaGui.TEXT, 6);
            blank(out);
        }
        out.add(0, new Line(VanillaGui.wrap(font, "Evidence found: " + found + " / " + com.airlinestory.story.Clues.count(), W - 26).get(0), 0xFF7A5A00, 0, false));
        if (found == 0) text(out, "Notes, tapes and photographs are hidden all over. Look for scraps of paper on desks, shelves and floors.", VanillaGui.TEXT, 0);
        return out;
    }

    private List<Line> peopleLines() {
        List<Line> out = new ArrayList<>();
        text(out, "Things people will remember about you:", 0xFF1F3F8F, 0);
        blank(out);
        List<String> mem = ClientStory.INSTANCE.memories;
        for (int i = mem.size() - 1; i >= 0; i--) {
            String[] p = mem.get(i).split("\\|", 2);
            text(out, p[0], 0xFF000000, 0);
            text(out, p.length > 1 ? p[1] : "", VanillaGui.TEXT, 6);
        }
        if (mem.isEmpty()) text(out, "No one has noticed you yet. Give it time.", VanillaGui.TEXT, 0);
        return out;
    }

    private List<Line> ratingLines() {
        List<Line> out = new ArrayList<>();
        float r = ClientStory.INSTANCE.rating;
        if (ClientStory.INSTANCE.has("ending_free")) {
            text(out, "SmileScore: --", 0xFF000000, 0);
            text(out, "Nobody is rating you any more.", VanillaGui.TEXT, 0);
            return out;
        }
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) stars.append(r >= i - 0.25f ? "★" : "☆");
        text(out, String.format(Locale.ROOT, "Your SmileScore: %.2f  %s", r, stars), 0xFF7A5A00, 0);
        String tier = r >= 4.5 ? "Prime. Doors open before you reach them." : r >= 3.5 ? "Pleasant. You'll board in group two."
                : r >= 2.5 ? "Average. People are starting to notice." : r >= 2.0 ? "Concerning. You are being watched closely."
                : "Eligible for a Meridian Rehabilitation Experience.";
        text(out, tier, VanillaGui.TEXT, 0);
        blank(out);
        text(out, "Recent ratings:", 0xFF1F3F8F, 0);
        List<String> log = ClientStory.INSTANCE.ratingLog;
        for (int i = log.size() - 1; i >= 0 && i >= log.size() - 20; i--) {
            String[] p = log.get(i).split("\\|", 2);
            boolean up = p[0].startsWith("+");
            text(out, p[0] + "  \"" + (p.length > 1 ? p[1] : "") + "\"", up ? 0xFF2E7D32 : 0xFFB71C1C, 4);
        }
        if (log.isEmpty()) text(out, "No interactions rated yet.", VanillaGui.TEXT, 4);
        return out;
    }
}
