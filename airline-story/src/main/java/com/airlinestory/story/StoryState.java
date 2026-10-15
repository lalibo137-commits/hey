package com.airlinestory.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Everything the story remembers about this world. Saved with the overworld. */
public class StoryState extends SavedData implements FlagView {
    public static final String NAME = "airlinestory_story";

    public boolean storyWorld;
    public String quest = "";
    public int questTicks;
    public final Set<String> flags = new LinkedHashSet<>();
    public final Map<String, Integer> counters = new LinkedHashMap<>();
    public float rating = 3.8f;
    public final List<String> completed = new ArrayList<>();
    /** "Name|text" lines for the journal's People page. */
    public final List<String> memories = new ArrayList<>();
    /** "+0.1|reason" lines, newest last. */
    public final List<String> ratingLog = new ArrayList<>();
    public final Map<String, UUID> npcs = new HashMap<>();
    public final Set<String> removedGroups = new HashSet<>();
    public final Set<String> spawnedGroups = new HashSet<>();

    public static StoryState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(StoryState::load, StoryState::new, NAME);
    }

    @Override public boolean has(String flag) { return flags.contains(flag); }
    @Override public int counter(String key) { return counters.getOrDefault(key, 0); }
    @Override public float rating() { return rating; }
    @Override public String quest() { return quest; }

    public void flag(String f) { if (flags.add(f)) setDirty(); }
    public void unflag(String f) { if (flags.remove(f)) setDirty(); }
    public void setCounter(String k, int v) { Integer old = counters.put(k, v); if (old == null || old != v) setDirty(); }
    public void addCounter(String k, int d) { setCounter(k, counter(k) + d); }

    public static StoryState load(CompoundTag tag) {
        StoryState s = new StoryState();
        s.storyWorld = tag.getBoolean("StoryWorld");
        s.quest = tag.getString("Quest");
        s.questTicks = tag.getInt("QuestTicks");
        s.rating = tag.contains("Rating") ? tag.getFloat("Rating") : 3.8f;
        for (Tag t : tag.getList("Flags", Tag.TAG_STRING)) s.flags.add(t.getAsString());
        CompoundTag c = tag.getCompound("Counters");
        for (String k : c.getAllKeys()) s.counters.put(k, c.getInt(k));
        for (Tag t : tag.getList("Completed", Tag.TAG_STRING)) s.completed.add(t.getAsString());
        for (Tag t : tag.getList("Memories", Tag.TAG_STRING)) s.memories.add(t.getAsString());
        for (Tag t : tag.getList("RatingLog", Tag.TAG_STRING)) s.ratingLog.add(t.getAsString());
        CompoundTag n = tag.getCompound("Npcs");
        for (String k : n.getAllKeys()) s.npcs.put(k, n.getUUID(k));
        for (Tag t : tag.getList("Removed", Tag.TAG_STRING)) s.removedGroups.add(t.getAsString());
        for (Tag t : tag.getList("Spawned", Tag.TAG_STRING)) s.spawnedGroups.add(t.getAsString());
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("StoryWorld", storyWorld);
        tag.putString("Quest", quest);
        tag.putInt("QuestTicks", questTicks);
        tag.putFloat("Rating", rating);
        tag.put("Flags", strings(flags));
        CompoundTag c = new CompoundTag();
        counters.forEach(c::putInt);
        tag.put("Counters", c);
        tag.put("Completed", strings(completed));
        tag.put("Memories", strings(memories));
        tag.put("RatingLog", strings(ratingLog));
        CompoundTag n = new CompoundTag();
        npcs.forEach(n::putUUID);
        tag.put("Npcs", n);
        tag.put("Removed", strings(removedGroups));
        tag.put("Spawned", strings(spawnedGroups));
        return tag;
    }

    private static ListTag strings(Collection<String> values) {
        ListTag l = new ListTag();
        for (String v : values) l.add(StringTag.valueOf(v));
        return l;
    }
}
