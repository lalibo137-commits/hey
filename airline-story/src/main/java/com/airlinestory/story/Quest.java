package com.airlinestory.story;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** A step of the story. Hooks run on the server; text and objectives are shown in the journal. */
public final class Quest {
    public final String id;
    public final int chapter;
    public final String title;
    public final String description;
    public final List<Objective> objectives = new ArrayList<>();
    @Nullable public String next;
    @Nullable Consumer<StoryContext> onStart;
    @Nullable Consumer<StoryContext> onResume;
    @Nullable Consumer<StoryContext> onComplete;
    /** Runs every tick with the quest's tick counter (persisted, so scripts resume after a reload). */
    @Nullable BiConsumer<StoryContext, Integer> onTick;
    /** Side quests run alongside the main story once started ("side:&lt;id&gt;" flag) until finished ("sidedone:&lt;id&gt;"). */
    public boolean side;
    /** NPC who offers this side quest (gets a "?" marker while it can be started). */
    @Nullable public String giver;
    /** When the side quest can be offered. */
    java.util.function.Predicate<FlagView> available = v -> true;

    Quest(String id, int chapter, String title, String description) {
        this.id = id;
        this.chapter = chapter;
        this.title = title;
        this.description = description;
    }

    public boolean complete(FlagView v) {
        for (int i = 0; i < objectives.size(); i++) {
            if (!objectives.get(i).optional && !objectives.get(i).isDone(v, id, i)) return false;
        }
        return true;
    }

    Quest obj(Objective o) { objectives.add(o); return this; }
    Quest then(String next) { this.next = next; return this; }
    Quest start(Consumer<StoryContext> c) { this.onStart = c; return this; }
    Quest resume(Consumer<StoryContext> c) { this.onResume = c; return this; }
    Quest done(Consumer<StoryContext> c) { this.onComplete = c; return this; }
    Quest tick(BiConsumer<StoryContext, Integer> c) { this.onTick = c; return this; }
    Quest giver(String npc, java.util.function.Predicate<FlagView> when) { this.giver = npc; this.available = when; return this; }

    public boolean active(FlagView v) { return side ? v.has("side:" + id) && !v.has("sidedone:" + id) : id.equals(v.quest()); }
    public boolean offered(FlagView v) { return side && giver != null && !v.has("side:" + id) && available.test(v); }
}
