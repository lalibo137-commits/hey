package com.airlinestory.client;

import com.airlinestory.net.Net;
import com.airlinestory.story.FlagView;

import java.util.*;

/** The client's copy of the story progress (sent by the server). */
public final class ClientStory implements FlagView {
    public static final ClientStory INSTANCE = new ClientStory();

    public boolean active;
    public String quest = "";
    public final Set<String> flags = new HashSet<>();
    public final Map<String, Integer> counters = new HashMap<>();
    public float rating = 3.8f;
    public final List<String> completed = new ArrayList<>();
    public final List<String> memories = new ArrayList<>();
    public final List<String> ratingLog = new ArrayList<>();

    private ClientStory() {}

    void apply(Net.Sync s) {
        active = true;
        quest = s.quest();
        flags.clear();
        flags.addAll(s.flags());
        counters.clear();
        counters.putAll(s.counters());
        rating = s.rating();
        completed.clear();
        completed.addAll(s.completed());
        memories.clear();
        memories.addAll(s.memories());
        ratingLog.clear();
        ratingLog.addAll(s.ratingLog());
    }

    public void reset() {
        active = false;
        quest = "";
        flags.clear();
        counters.clear();
        completed.clear();
        memories.clear();
        ratingLog.clear();
    }

    @Override public boolean has(String flag) { return flags.contains(flag); }
    @Override public int counter(String key) { return counters.getOrDefault(key, 0); }
    @Override public float rating() { return rating; }
    @Override public String quest() { return quest; }
}
