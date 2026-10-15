package com.airlinestory.story;

/** Read-only story progress, available on the server (StoryState) and the client (synced copy). */
public interface FlagView {
    boolean has(String flag);

    int counter(String key);

    float rating();

    String quest();

    default boolean all(String... flags) {
        for (String f : flags) if (!has(f)) return false;
        return true;
    }

    default boolean any(String... flags) {
        for (String f : flags) if (has(f)) return true;
        return false;
    }
}
