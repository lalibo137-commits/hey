package com.airlinestory.story;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Predicate;

/** One line of a quest's to-do list. Evaluated on both sides from the synced story flags. */
public final class Objective {
    public final String text;
    public final String hint;
    @Nullable public final String npcId;
    @Nullable public final String npcName;
    @Nullable public final String locId;
    final Predicate<FlagView> done;
    @Nullable final Function<FlagView, String> progress;
    /** Counter key the director keeps up to date from the player's inventory, if any. */
    @Nullable public final String tracks;
    /** Exact spot to guide the player to (a podium, a seat, a door), if more precise than the place. */
    @Nullable public net.minecraft.core.BlockPos at;
    /** Optional objective: shown, but not needed to finish the quest. */
    public boolean optional;

    public Objective at(net.minecraft.core.BlockPos pos) { this.at = pos; return this; }
    public Objective optional() { this.optional = true; return this; }

    private Objective(String text, String hint, @Nullable String npcId, @Nullable String npcName, @Nullable String locId,
                      Predicate<FlagView> done, @Nullable Function<FlagView, String> progress, @Nullable String tracks) {
        this.text = text;
        this.hint = hint;
        this.npcId = npcId;
        this.npcName = npcName;
        this.locId = locId;
        this.done = done;
        this.progress = progress;
        this.tracks = tracks;
    }

    public boolean isDone(FlagView v, String questId, int index) {
        return v.has("obj:" + questId + ":" + index) || done.test(v);
    }

    public String display(FlagView v) {
        return progress == null ? text : text + " " + progress.apply(v);
    }

    /** Talk to an NPC; done when the conversation sets {@code flag}. */
    public static Objective talk(String npcId, String name, String flag, String text, String hint) {
        return new Objective(text, hint, npcId, name, null, v -> v.has(flag), null, null);
    }

    public static Objective talkAt(String npcId, String name, String locId, String flag, String text, String hint) {
        return new Objective(text, hint, npcId, name, locId, v -> v.has(flag), null, null);
    }

    /** Reach a place; the director sets "at:&lt;loc&gt;" when the player gets there. */
    public static Objective go(String locId, String text, String hint) {
        return new Objective(text, hint, null, null, locId, v -> v.has("at:" + locId), null, null);
    }

    /** Done when a story flag is set. */
    public static Objective flag(String flag, @Nullable String locId, String text, String hint) {
        return new Objective(text, hint, null, null, locId, v -> v.has(flag), null, null);
    }

    /** Carry at least {@code n} of an item (or tag, "tag:ns:path"). Latches once reached. */
    public static Objective items(String key, int n, @Nullable String locId, String text, String hint) {
        String c = "item:" + key;
        return new Objective(text, hint, null, null, locId, v -> v.counter(c) >= n,
                v -> "(" + Math.min(n, v.counter(c)) + "/" + n + ")", key);
    }

    /** Done when a counter reaches {@code n}; shows progress. */
    public static Objective count(String counter, int n, @Nullable String locId, String text, String hint) {
        return new Objective(text, hint, null, null, locId, v -> v.counter(counter) >= n,
                v -> "(" + Math.min(n, v.counter(counter)) + "/" + n + ")", null);
    }

    /** A boat stage: done on a flag, with a custom progress line. */
    public static Objective stage(String flag, String text, String hint, Function<FlagView, String> progress) {
        return new Objective(text, hint, null, null, "dock", v -> v.has(flag), progress, null);
    }
}
