package com.airlinestory.story;

/** Set by the title screen right before it creates a new story world (same JVM in single player). */
public final class StoryBoot {
    private StoryBoot() {}

    private static volatile boolean pendingNew;

    public static void markNewStory() {
        pendingNew = true;
    }

    static boolean consumeNewStory() {
        boolean p = pendingNew;
        pendingNew = false;
        return p;
    }
}
