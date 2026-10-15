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

    Quest(String id, int chapter, String title, String description) {
        this.id = id;
        this.chapter = chapter;
        this.title = title;
        this.description = description;
    }

    public boolean complete(FlagView v) {
        for (int i = 0; i < objectives.size(); i++) {
            if (!objectives.get(i).isDone(v, id, i)) return false;
        }
        return true;
    }

    Quest obj(Objective o) { objectives.add(o); return this; }
    Quest then(String next) { this.next = next; return this; }
    Quest start(Consumer<StoryContext> c) { this.onStart = c; return this; }
    Quest resume(Consumer<StoryContext> c) { this.onResume = c; return this; }
    Quest done(Consumer<StoryContext> c) { this.onComplete = c; return this; }
    Quest tick(BiConsumer<StoryContext, Integer> c) { this.onTick = c; return this; }
}
