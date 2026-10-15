package com.airlinestory.story;

import com.airlinestory.AirlineStory;
import com.airlinestory.block.StateInteractBlock;
import com.airlinestory.dialogue.Dialogue;
import com.airlinestory.dialogue.Dialogues;
import com.airlinestory.entity.HollowEntity;
import com.airlinestory.entity.ModEntities;
import com.airlinestory.entity.SeatEntity;
import com.airlinestory.entity.StoryNpc;
import com.airlinestory.net.Net;
import com.airlinestory.registry.ModBlocks;
import com.airlinestory.registry.ModItems;
import com.airlinestory.story.Locations.Plane;
import com.airlinestory.world.AirportBuilder;
import com.airlinestory.world.IslandBuilder;
import com.airlinestory.world.IslandTerrain;
import com.airlinestory.world.Placer;
import com.airlinestory.world.PlaneBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * Runs the story on the integrated server of a story world: quests and their scripts, NPC casting,
 * conversations, cutscenes, the world's construction and everything that goes bump in the night.
 */
public final class StoryDirector {
    @Nullable private static StoryDirector current;

    private final MinecraftServer server;
    private final StoryState state;
    private final IslandBuilder island;
    private boolean syncDirty = true;
    private boolean holdTick;
    private long tickCount;
    private final List<Delayed> delayed = new ArrayList<>();
    private final Map<String, Long> cutsceneDeadline = new HashMap<>();

    private record Delayed(long at, Runnable run) {}

    private static final List<String> TRACKED = List.of("tag:minecraft:fishes", "airlinestory:bitterleaf", "airlinestory:palm_planks",
            "airlinestory:navigation_chart");

    private StoryDirector(MinecraftServer server, StoryState state) {
        this.server = server;
        this.state = state;
        this.island = new IslandBuilder(state);
    }

    @Nullable
    public static StoryDirector get() {
        return current;
    }

    public StoryState state() { return state; }
    public MinecraftServer server() { return server; }
    public ServerLevel level() { return server.overworld(); }
    public void markSync() { syncDirty = true; }
    /** Pause the current quest's script for this tick (e.g. while the player is not in their seat). */
    public void hold() { holdTick = true; }

    // =========================================================================================
    // lifecycle
    // =========================================================================================
    static void serverStarted(MinecraftServer server) {
        current = null;
        StoryState st = StoryState.get(server);
        if (StoryBoot.consumeNewStory() && server.isSingleplayer()) {
            st.storyWorld = true;
            st.setDirty();
        }
        if (!st.storyWorld || !server.isSingleplayer()) return;
        current = new StoryDirector(server, st);
        current.init();
    }

    static void serverStopped() {
        current = null;
    }

    private void init() {
        ServerLevel level = level();
        if (!state.has("built_airport")) {
            AirlineStory.LOG.info("Building Meridian International...");
            Placer p = new Placer(level);
            AirportBuilder.build(p);
            PlaneBuilder.build(p, Locations.GATE_PLANE, true);
            PlaneBuilder.build(p, Locations.AIR_PLANE, false);
            p.clues.forEach((pos, id) -> state.clueAt.put(pos.asLong(), id));
            state.flag("built_air_plane");
            AirlineStory.LOG.info("Airport built ({} blocks)", p.placed);
            level.setDefaultSpawnPos(Locations.SPAWN, 0f);
            GameRules rules = server.getGameRules();
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);
            rules.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
            rules.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);
            rules.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, server);
            rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
            rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
            level.setDayTime(1000);
            level.setWeatherParameters(24000, 0, false, false);
            state.flag("built_airport");
        }
        if (!state.has("cast_airport")) {
            state.flag("cast_airport");
            spawnGroup("landside");
            spawnGroup("gate_crew");
        }
        state.setDirty();
    }

    void onLogin(ServerPlayer player) {
        if (state.quest.isEmpty()) {
            player.teleportTo(level(), Locations.SPAWN.getX() + 0.5, Locations.SPAWN.getY(), Locations.SPAWN.getZ() + 0.5, 0f, 0f);
            startQuest(player, Quests.FIRST);
        } else {
            Quest q = Quests.get(state.quest);
            for (String cs : new ArrayList<>(cutsceneDeadline.keySet())) state.flag("cs:" + cs);
            cutsceneDeadline.clear();
            for (String f : new ArrayList<>(state.flags)) {
                if (f.startsWith("cs:") && f.endsWith(":started")) {
                    String id = f.substring(3, f.length() - 8);
                    state.flag("cs:" + id);
                }
            }
            if (q != null && q.onResume != null) q.onResume.accept(ctx(player));
        }
        syncDirty = true;
        sync(player);
    }

    private StoryContext ctx(ServerPlayer player) {
        return new StoryContext(this, player, null, "");
    }

    @Nullable
    private ServerPlayer player() {
        List<ServerPlayer> list = server.getPlayerList().getPlayers();
        return list.isEmpty() ? null : list.get(0);
    }

    // =========================================================================================
    // ticking
    // =========================================================================================
    void tick() {
        tickCount++;
        ServerPlayer p = player();
        // build the island in the background: gently while you're busy, flat out when we need it
        if (!island.done()) {
            boolean urgent = state.has("crashing");
            island.step(level(), System.nanoTime() + (urgent ? 40_000_000L : p == null ? 0 : 6_000_000L));
            if (island.done()) {
                AirlineStory.LOG.info("Island generated.");
                island.placedClues.forEach((pos, id) -> state.clueAt.put(pos.asLong(), id));
                state.setDirty();
            }
        }
        if (p == null) return;

        if (!delayed.isEmpty()) {
            List<Delayed> due = new ArrayList<>();
            delayed.removeIf(d -> { if (d.at <= tickCount) { due.add(d); return true; } return false; });
            for (Delayed d : due) d.run.run();
        }
        for (Map.Entry<String, Long> e : new ArrayList<>(cutsceneDeadline.entrySet())) {
            if (tickCount > e.getValue()) { state.flag("cs:" + e.getKey()); cutsceneDeadline.remove(e.getKey()); syncDirty = true; }
        }

        Quest q = Quests.get(state.quest);
        if (q != null) {
            holdTick = false;
            if (q.onTick != null) q.onTick.accept(ctx(p), state.questTicks);
            if (!holdTick) state.questTicks++;
            if (tickCount % 10 == 0) updateObjectives(p, q);
        }
        if (tickCount % 10 == 5) for (Quest s : Quests.sides()) if (s.active(state)) updateObjectives(p, s);
        if (tickCount % 20 == 0) updateMarkers();
        if (tickCount % 10 == 0) barks(p);
        if (caughtCooldown > 0) caughtCooldown--;
        ambient(p);
        if (syncDirty && tickCount % 4 == 0) sync(p);
    }

    private void updateObjectives(ServerPlayer p, Quest q) {
        for (int i = 0; i < q.objectives.size(); i++) {
            Objective o = q.objectives.get(i);
            if (o.locId != null && !state.has("at:" + o.locId)) {
                Locations.Loc loc = Locations.get(o.locId);
                if (loc != null && Locations.isNear(loc, state, p.getX(), p.getY(), p.getZ())) {
                    state.flag("at:" + o.locId);
                    syncDirty = true;
                }
            }
            if (o.tracks != null) setCounter("item:" + o.tracks, count(p, matcher(o.tracks)));
            if (!state.has("obj:" + q.id + ":" + i) && o.done.test(state)) {
                state.flag("obj:" + q.id + ":" + i);
                syncDirty = true;
            }
        }
        for (String k : TRACKED) setCounter("item:" + k, count(p, matcher(k)));
        if (state.quest.equals("shelter") && count(p, s -> s.is(Items.CAMPFIRE)) > 0) state.flag("has_campfire");
        if (q.side) {
            if (q.complete(state) && q.active(state)) completeSide(p, q);
        } else if (q.complete(state) && state.quest.equals(q.id)) completeQuest(p, q);
    }

    private void setCounter(String k, int v) {
        if (state.counter(k) != v) {
            state.setCounter(k, v);
            syncDirty = true;
        }
    }

    private Predicate<ItemStack> matcher(String key) {
        if (key.startsWith("tag:")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(key.substring(4)));
            return s -> s.is(tag);
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(key));
        return s -> item != null && s.is(item);
    }

    public void startQuest(ServerPlayer p, @Nullable String id) {
        if (id == null) return;
        Quest q = Quests.get(id);
        if (q == null) return;
        state.quest = id;
        state.questTicks = 0;
        state.setDirty();
        syncDirty = true;
        AirlineStory.LOG.info("Story quest: {}", id);
        if (q.onStart != null) q.onStart.accept(ctx(p));
        if (q.onResume != null) q.onResume.accept(ctx(p));
        fx(p, "quest", q.title, q.chapter, 0);
    }

    private void completeQuest(ServerPlayer p, Quest q) {
        if (q.onComplete != null) q.onComplete.accept(ctx(p));
        if (!state.completed.contains(q.id)) state.completed.add(q.id);
        if (state.quest.equals(q.id)) startQuest(p, q.next);
    }

    public void startSide(ServerPlayer p, String id) {
        Quest q = Quests.get(id);
        if (q == null || !q.side || state.has("side:" + id)) return;
        state.flag("side:" + id);
        syncDirty = true;
        if (q.onStart != null) q.onStart.accept(ctx(p));
        fx(p, "quest", "Side quest: " + q.title, q.chapter, 1);
    }

    private void completeSide(ServerPlayer p, Quest q) {
        state.flag("sidedone:" + q.id);
        if (q.onComplete != null) q.onComplete.accept(ctx(p));
        if (!state.completed.contains(q.id)) state.completed.add(q.id);
        syncDirty = true;
        fx(p, "hint", "Side quest complete: " + q.title, 0, 0);
        sound(p, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.2f);
    }

    /** Debug: finish the current quest's objectives. */
    void skip(ServerPlayer p) {
        Quest q = Quests.get(state.quest);
        if (q == null) return;
        for (int i = 0; i < q.objectives.size(); i++) state.flag("obj:" + q.id + ":" + i);
        for (String cs : new ArrayList<>(cutsceneDeadline.keySet())) state.flag("cs:" + cs);
        completeQuest(p, q);
    }

    void jump(ServerPlayer p, String id) {
        if (Quests.get(id) != null) startQuest(p, id);
    }

    void sync(ServerPlayer p) {
        syncDirty = false;
        Net.send(p, new Net.Sync(state.quest, new ArrayList<>(state.flags), new HashMap<>(state.counters), state.rating,
                new ArrayList<>(state.completed), new ArrayList<>(state.memories), new ArrayList<>(state.ratingLog)));
    }

    public void schedule(int ticks, Runnable r) {
        delayed.add(new Delayed(tickCount + ticks, r));
    }

    // =========================================================================================
    // NPCs
    // =========================================================================================
    void register(StoryNpc npc) {
        state.npcs.put(npc.npcId(), npc.getUUID());
        state.setDirty();
    }

    @Nullable
    public StoryNpc npc(String id) {
        UUID u = state.npcs.get(id);
        if (u == null) return null;
        Entity e = level().getEntity(u);
        return e instanceof StoryNpc n && n.isAlive() ? n : null;
    }

    public void spawnGroup(String group) {
        if (state.spawnedGroups.contains(group)) return;
        state.spawnedGroups.add(group);
        state.removedGroups.remove(group);
        Cast.spawn(this, level(), group);
        state.setDirty();
    }

    public void removeGroup(String group) {
        state.removedGroups.add(group);
        state.spawnedGroups.remove(group);
        for (StoryNpc n : level().getEntities(ModEntities.NPC.get(), e -> group.equals(e.group()))) {
            if (n.getVehicle() != null) n.getVehicle().discard();
            n.discard();
        }
        state.setDirty();
    }

    void validate(StoryNpc npc) {
        if (state.removedGroups.contains(npc.group())) { npc.discard(); return; }
        UUID u = state.npcs.get(npc.npcId());
        if (u != null && !u.equals(npc.getUUID())) npc.discard();
    }

    private void updateMarkers() {
        Set<String> wanted = new HashSet<>();
        Quest q = Quests.get(state.quest);
        if (q != null) {
            for (int i = 0; i < q.objectives.size(); i++) {
                Objective o = q.objectives.get(i);
                if (o.npcId != null && !o.isDone(state, q.id, i)) wanted.add(o.npcId);
            }
        }
        Set<String> offers = new HashSet<>();
        for (Quest s : Quests.sides()) {
            if (s.offered(state)) offers.add(s.giver);
            if (s.active(state)) for (int i = 0; i < s.objectives.size(); i++) {
                Objective o = s.objectives.get(i);
                if (o.npcId != null && !o.isDone(state, s.id, i) && (i == 0 || s.objectives.get(i - 1).isDone(state, s.id, i - 1))) offers.add(o.npcId);
            }
        }
        for (StoryNpc n : level().getEntities(ModEntities.NPC.get(), e -> true))
            n.setMarker(wanted.contains(n.npcId()) ? 1 : offers.contains(n.npcId()) ? 2 : 0);
    }

    public void staring(String group, boolean on) {
        for (StoryNpc n : level().getEntities(ModEntities.NPC.get(), e -> group.equals(e.group()))) {
            if (n.npcId().equals("june")) continue;
            n.setSmiling(on);
            if (on) n.stare();
            else n.unstare();
        }
    }

    public void startBoarding() {
        int i = 0;
        List<String> ids = new ArrayList<>(Cast.SEATS.keySet());
        Collections.shuffle(ids, new Random(4));
        for (String id : ids) {
            String seat = Cast.b7Seat(id);
            if (seat == null) continue;
            schedule(20 + i * 45, () -> {
                StoryNpc n = npc(id);
                if (n != null) n.walkRoute(Cast.boardingRoute(seat), Cast.seat(Locations.GATE_PLANE, seat));
            });
            i++;
        }
    }

    private void moveNpc(String id, BlockPos pos, boolean sit, float yaw) {
        StoryNpc n = npc(id);
        if (n == null) return;
        n.leaveSeat();
        level().getChunkAt(pos);
        n.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (sit) n.sitAt(pos);
        else n.stand(pos, yaw);
    }

    /** Priya, Leo and Mr. Halvorsen make their own way to B7 while you're busy. */
    public void moveToGate() {
        int gx = Locations.GATE_B7;
        moveNpc("priya", new BlockPos(gx + 5, 64, 39), true, 0);
        moveNpc("leo", new BlockPos(gx + 6, 64, 39), true, 0);
        moveNpc("halvorsen", new BlockPos(gx - 2, 64, 44), true, 0);
        moveNpc("edith", Locations.EDITH_SEAT, true, 0);
    }

    public void reyesToRoom() {
        StoryNpc r = npc("reyes");
        if (r != null) r.walkRoute(List.of(new BlockPos(66, 64, 30), new BlockPos(68, 64, 40), Cast.teleportPoint(new BlockPos(68, 64, 43)), Locations.REYES_ROOM), null);
    }

    public void reyesHome() {
        StoryNpc r = npc("reyes");
        if (r != null) schedule(80, () -> moveNpc("reyes", Locations.REYES, false, -90f));
    }

    public void theoHome() {
        Plane a = Locations.AIR_PLANE;
        StoryNpc t = npc("theo");
        if (t != null) t.walkRoute(List.of(a.aisle(40), a.rearGalley()), null);
    }

    private int caughtCooldown;

    /** Stagehands spot you if you walk into their line of sight; sneaking and the staff lanyard help. */
    public void backstageWatch(ServerPlayer p) {
        if (tickCount % 5 != 0 || caughtCooldown > 0) return;
        double px = p.getX(), pz = p.getZ();
        if (px < AirportBuilder.BX0 || px > AirportBuilder.BX1 || pz < AirportBuilder.BZ0 || pz > -1) return;
        boolean lanyard = count(p, s -> s.is(ModItems.STAFF_LANYARD.get())) > 0;
        for (String id : List.of("stagehand_1", "stagehand_2", "pa")) {
            StoryNpc n = npc(id);
            if (n == null) continue;
            double dist = n.distanceTo(p);
            boolean sleeper = id.equals("pa");
            double range = sleeper ? 2.2 : lanyard ? 3.5 : 7.5;
            if (p.isCrouching()) range *= sleeper ? 0.5 : 0.6;
            if (dist > range || !n.hasLineOfSight(p)) continue;
            if (!sleeper) {
                Vec3 look = n.getViewVector(1f);
                Vec3 to = p.position().subtract(n.position()).normalize();
                if (look.x * to.x + look.z * to.z < 0.45) continue;
            }
            caught(p, n);
            return;
        }
    }

    public void caught(ServerPlayer p, @Nullable StoryNpc by) {
        caughtCooldown = 60;
        if (by != null) by.say(by.npcId().equals("pa") ? "Wha-? HEY! Security!" : "Hey! You can't be back here!", 60);
        fx(p, "caught", 1, 30);
        state.addCounter("caught", 1);
        rate(p, -0.15f, "Wandered into a staff-only area");
        schedule(20, () -> {
            p.teleportTo(level(), 106.5, 64, 3.5, 0f, 0f);
            int n = state.counter("caught");
            hint(p, n >= 3 ? "Marched back out again. Sneak (Shift) to shrink how far they notice you, wait behind the scaffolding until their backs are turned, and keep clear of the sleeping assistant."
                    : "A stagehand marches you back out through the staff door. \"Nothing to see back there. Have a lovely flight.\"");
        });
    }

    public void pipToGrove() {
        StoryNpc pip = npc("pip");
        BlockPos seat = IslandBuilder.grove(-1, -4);
        if (pip != null) {
            pip.teleportTo(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5);
            pip.sitAt(seat);
        }
    }

    public void pipHome() {
        StoryNpc pip = npc("pip");
        if (pip != null) pip.wander(IslandBuilder.village(3, 3), 5);
    }

    // =========================================================================================
    // conversations
    // =========================================================================================
    public void openDialogue(ServerPlayer p, String treeId, @Nullable StoryNpc npc) {
        Dialogue d = Dialogues.get(treeId);
        if (d == null) return;
        String start = d.startNode(state);
        if (d.node(start) == null) return;
        if (npc != null) npc.beginTalk(p);
        sync(p);
        boolean rate = d.rateable && npc != null && !state.has("rated:" + npc.npcId()) && !state.has("on_island");
        Net.send(p, new Net.OpenDialogue(npc == null ? -1 : npc.getId(), treeId, start, rate));
    }

    public void openLater(ServerPlayer p, String treeId, int ticks) {
        schedule(ticks, () -> openDialogue(p, treeId, null));
    }

    public void onPacket(ServerPlayer p, Object msg) {
        if (msg instanceof Net.Choice c) onChoice(p, c);
        else if (msg instanceof Net.CloseDialogue c) {
            Entity e = level().getEntity(c.npc());
            if (e instanceof StoryNpc n) n.endTalk();
        } else if (msg instanceof Net.CutsceneDone c) {
            state.flag("cs:" + c.id());
            cutsceneDeadline.remove(c.id());
            syncDirty = true;
        } else if (msg instanceof Net.EndingDone) {
            afterEnding(p);
        } else if (msg instanceof Net.Rate r) {
            onRate(p, r);
        }
    }

    private void onRate(ServerPlayer p, Net.Rate r) {
        Entity e = level().getEntity(r.npc());
        if (!(e instanceof StoryNpc n) || state.has("rated:" + n.npcId())) return;
        int stars = Math.max(1, Math.min(5, r.stars()));
        state.flag("rated:" + n.npcId());
        if (n.npcId().equals("dana")) state.flag("rated_dana");
        n.adjustScore((stars - 3) * 0.04f);
        // they rate you back: partly what you gave them, partly how you've been behaving
        double back = 3 + (stars - 3) * 0.6 + (state.rating - 3.5) * 0.8 + (level().random.nextDouble() - 0.5) * 0.6;
        int theirs = (int) Math.max(1, Math.min(5, Math.round(back)));
        state.ratingLog.add(String.format(Locale.ROOT, "+0.00|You rated %s %d stars", n.displayName(), stars));
        rate(p, (theirs - 3) * 0.07f, n.displayName() + " rated you " + theirs + " stars");
        schedule(25, () -> fx(p, "rated", n.displayName(), theirs, 0));
        syncDirty = true;
    }

    private void onChoice(ServerPlayer p, Net.Choice c) {
        Dialogue d = Dialogues.get(c.tree());
        if (d == null) return;
        Dialogue.Node node = d.node(c.node());
        if (node == null || c.index() < 0 || c.index() >= node.choices.size()) return;
        Dialogue.Choice choice = node.choices.get(c.index());
        Entity e = c.npc() >= 0 ? level().getEntity(c.npc()) : null;
        StoryNpc npc = e instanceof StoryNpc n ? n : null;
        String speaker = node.speaker != null ? node.speaker : npc != null ? npc.displayName() : d.speaker;
        choice.apply(new StoryContext(this, p, npc, speaker));
        if (c.tree().startsWith("clue:") && pendingClue != null && state.has(c.tree())) {
            level().removeBlock(pendingClue, false);
            state.clueAt.remove(pendingClue.asLong());
            sound(p, SoundEvents.BOOK_PAGE_TURN, 1f, 1f);
            fx(p, "clue", Clues.get(c.tree().substring(5)) != null ? Clues.get(c.tree().substring(5)).title() : "", 0, 0);
            pendingClue = null;
        }
        if (choice.next == null && npc != null) npc.endTalk();
        syncDirty = true;
        Quest q = Quests.get(state.quest);
        if (q != null) updateObjectives(p, q);
        sync(p);
    }

    public void rate(ServerPlayer p, float delta, String reason) {
        float old = state.rating;
        state.rating = Math.max(0f, Math.min(5f, Math.round((state.rating + delta) * 100f) / 100f));
        state.ratingLog.add(String.format(Locale.ROOT, "%+.2f|%s", delta, reason));
        while (state.ratingLog.size() > 30) state.ratingLog.remove(0);
        state.setDirty();
        syncDirty = true;
        fx(p, "rating", String.format(Locale.ROOT, "%.2f", state.rating), state.rating - old, 0);
    }

    public void remember(ServerPlayer p, String who, String text) {
        state.memories.add(who + "|" + text);
        state.setDirty();
        syncDirty = true;
        fx(p, "remember", who, 0, 0);
    }

    // =========================================================================================
    // items
    // =========================================================================================
    public void give(ServerPlayer p, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!p.getInventory().add(copy)) p.drop(copy, false);
        p.containerMenu.broadcastChanges();
        fx(p, "item", stack.getHoverName().getString(), stack.getCount(), 0);
    }

    public void giveOwned(ServerPlayer p, Item item) {
        ItemStack s = new ItemStack(item);
        s.getOrCreateTag().putString("Owner", p.getGameProfile().getName());
        give(p, s);
    }

    public int count(ServerPlayer p, Predicate<ItemStack> match) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (!s.isEmpty() && match.test(s)) n += s.getCount();
        for (ItemStack s : p.getInventory().offhand) if (!s.isEmpty() && match.test(s)) n += s.getCount();
        return n;
    }

    public boolean take(ServerPlayer p, Predicate<ItemStack> match, int n) {
        if (count(p, match) < n) return false;
        int left = n;
        for (List<ItemStack> list : List.of(p.getInventory().items, p.getInventory().offhand)) {
            for (ItemStack s : list) {
                if (left <= 0) break;
                if (!s.isEmpty() && match.test(s)) {
                    int t = Math.min(left, s.getCount());
                    s.shrink(t);
                    left -= t;
                }
            }
        }
        p.containerMenu.broadcastChanges();
        return true;
    }

    // =========================================================================================
    // effects
    // =========================================================================================
    public void fx(ServerPlayer p, String type, String text, float value, int ticks) {
        Net.send(p, new Net.Fx(type, text, value, ticks));
    }

    public void fx(ServerPlayer p, String type, float value, int ticks) {
        fx(p, type, "", value, ticks);
    }

    public void pa(ServerPlayer p, String text) {
        fx(p, "pa", text, 0, 0);
    }

    public void halo(ServerPlayer p, String text) {
        fx(p, "halo", text, 0, 0);
    }

    public void hint(ServerPlayer p, String text) {
        fx(p, "hint", text, 0, 0);
    }

    public void sound(ServerPlayer p, SoundEvent s, float vol, float pitch) {
        level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.AMBIENT, vol, pitch);
    }

    public void cutscene(ServerPlayer p, String id) {
        state.flag("cs:" + id + ":started");
        cutsceneDeadline.put(id, tickCount + Cutscenes.duration(id, state) + 200);
        sync(p);
        Net.send(p, new Net.PlayCutscene(id));
    }

    public void gamemode(ServerPlayer p, GameType type) {
        if (p.gameMode.getGameModeForPlayer() != type && !p.isCreative() && !p.isSpectator()) p.setGameMode(type);
    }

    public void openBarrier(String name) {
        AirportBuilder.barrier(new Placer(level()), name, false);
        ServerPlayer p = player();
        if (p != null) sound(p, SoundEvents.PISTON_CONTRACT, 0.6f, 1.4f);
    }

    public void boards(int mode) {
        AirportBuilder.boards(new Placer(level()), mode);
        if (mode == 1) state.flag("boards_glitch");
    }

    public boolean isSeated(ServerPlayer p) {
        return p.getVehicle() instanceof SeatEntity;
    }

    public void seat(ServerPlayer p, BlockPos pos) {
        if (p.getVehicle() != null) p.stopRiding();
        p.teleportTo(level(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 90f, 0f);
        schedule(3, () -> SeatEntity.sit(p, pos, Direction.WEST));
    }

    public void seatbelt(Plane plane, boolean on) {
        PlaneBuilder.seatbelt(new Placer(level()), plane, on);
        ServerPlayer p = player();
        if (p != null) fx(p, "chime", on ? 1 : 0, 0);
    }

    /** 0 off, 1 on, 2 flicker. */
    public void planeLights(Plane plane, int mode) {
        Placer pl = new Placer(level());
        if (mode == 2) {
            for (int i = 0; i < 8; i++) {
                boolean on = i % 2 == 1;
                schedule(i * 4 + level().random.nextInt(3), () -> PlaneBuilder.lights(new Placer(level()), plane, on));
            }
            ServerPlayer p = player();
            if (p != null) sound(p, SoundEvents.REDSTONE_TORCH_BURNOUT, 0.6f, 0.6f);
        } else {
            PlaneBuilder.lights(pl, plane, mode == 1);
        }
    }

    public void dropMasks(Plane plane) {
        PlaneBuilder.masks(new Placer(level()), plane);
        ServerPlayer p = player();
        if (p != null) sound(p, SoundEvents.ARMOR_EQUIP_LEATHER, 1f, 0.6f);
    }

    public void weather(boolean storm) {
        level().setWeatherParameters(storm ? 0 : 24000, storm ? 24000 : 0, storm, storm);
    }

    public void lightningNear(ServerPlayer p) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
        if (bolt == null) return;
        double a = level().random.nextDouble() * Math.PI * 2;
        double r = 18 + level().random.nextDouble() * 20;
        bolt.moveTo(p.getX() + Math.cos(a) * r, p.getY() - 8, p.getZ() + Math.sin(a) * r);
        bolt.setVisualOnly(true);
        level().addFreshEntity(bolt);
    }

    public void shove(ServerPlayer p, int strength) {
        if (level().random.nextFloat() < 0.08f * strength) {
            double a = level().random.nextDouble() * Math.PI * 2;
            p.push(Math.cos(a) * 0.35 * strength, 0.15 + 0.1 * strength, Math.sin(a) * 0.35 * strength);
            p.hurtMarked = true;
            if (strength >= 2 && level().random.nextFloat() < 0.15f) p.hurt(p.damageSources().generic(), 1f);
        }
    }

    public void airCabinTime() {
        level().setDayTime(11800);
    }

    // =========================================================================================
    // big transitions
    // =========================================================================================
    public void transferToAirPlane(ServerPlayer p) {
        ServerLevel level = level();
        if (!state.has("built_air_plane")) {
            PlaneBuilder.build(new Placer(level), Locations.AIR_PLANE, false);
            state.flag("built_air_plane");
        }
        for (String g : List.of("landside", "security", "airside", "backstage", "b7", "gate_crew")) removeGroup(g);
        level.getChunkAt(Locations.AIR_PLANE.seat(14, 'A'));
        spawnGroup("air_plane");
        state.flag("on_air_plane");
        airCabinTime();
        seat(p, Locations.AIR_PLANE.seat(14, 'A'));
        PlaneBuilder.seatbelt(new Placer(level), Locations.AIR_PLANE, true);
        fx(p, "fade_in", 1, 40);
    }

    /** Called when the storm cutscene ends: everything goes black until the island is ready. */
    public void crashToIsland(ServerPlayer p) {
        state.flag("crashing");
        fx(p, "black", 1, 0);
        fx(p, "turbulence", 0, 0);
    }

    /** Moves the player to the beach once the island exists. Returns true when done. */
    public boolean tryLand(ServerPlayer p) {
        if (state.has("on_island")) return true;
        if (!island.done()) {
            if (tickCount % 20 == 0) fx(p, "loading", island.percent(), 0);
            return false;
        }
        ServerLevel level = level();
        removeGroup("air_plane");
        if (p.getVehicle() != null) p.stopRiding();
        BlockPos w = IslandBuilder.wakePos();
        level.getChunkAt(w);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, w.getX(), w.getZ());
        BlockPos at = new BlockPos(w.getX(), Math.max(y, IslandTerrain.SEA + 1), w.getZ());
        p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180f, 10f);
        p.setRespawnPosition(level.dimension(), at, 180f, true, false);
        level.setDefaultSpawnPos(at, 180f);
        gamemode(p, GameType.SURVIVAL);
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, server);
        weather(false);
        level.setWeatherParameters(48000, 0, false, false);
        level.setDayTime(level.getDayTime() - level.getDayTime() % 24000 + 24000 + 23200);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 1));
        p.setHealth(Math.max(6f, p.getHealth() * 0.5f));
        spawnAnimals(level);
        state.flag("on_island");
        state.unflag("crashing");
        fx(p, "black", 0, 0);
        return true;
    }

    private void spawnAnimals(ServerLevel level) {
        Random r = new Random(99);
        List<EntityType<? extends Mob>> types = List.of(EntityType.PIG, EntityType.PIG, EntityType.CHICKEN, EntityType.CHICKEN, EntityType.RABBIT);
        int spawned = 0;
        for (int i = 0; i < 600 && spawned < 40; i++) {
            int dx = r.nextInt(300) - 150, dz = r.nextInt(300) - 150;
            if (!IslandTerrain.isLand(dx, dz) || IslandTerrain.radial(dx, dz) > 0.85 || IslandTerrain.height(dx, dz) > 95
                    || IslandTerrain.riverWater(dx, dz) >= 0 || IslandTerrain.isPool(dx, dz)) continue;
            BlockPos at = new BlockPos(Locations.ISLAND_X + dx, IslandTerrain.height(dx, dz) + 1, Locations.ISLAND_Z + dz);
            level.getChunkAt(at);
            Mob m = types.get(spawned % types.size()).create(level);
            if (m == null) continue;
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            m.setPersistenceRequired();
            level.addFreshEntity(m);
            spawned++;
        }
    }

    public void ending(ServerPlayer p) {
        String kind = state.has("ending_stay") ? "stay" : state.has("ending_free") ? "free" : "loop";
        Net.send(p, new Net.Ending(kind, state.has("pip_comes"), state.rating));
    }

    private void afterEnding(ServerPlayer p) {
        if (state.has("ending_seen")) return;
        state.flag("ending_seen");
        if (state.has("ending_stay")) {
            BlockPos v = IslandBuilder.village(2, 4);
            p.teleportTo(level(), v.getX() + 0.5, v.getY(), v.getZ() + 0.5, 0f, 0f);
        } else if (!state.has("ending_free")) {
            spawnGroup("ending_airport");
            p.teleportTo(level(), 26.5, 64, 25.5, 180f, 0f);
            gamemode(p, GameType.ADVENTURE);
            level().setDayTime(level().getDayTime() - level().getDayTime() % 24000 + 25000);
            server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            schedule(60, () -> openDialogue(p, "dana", npc("dana")));
        } else if (state.has("pip_comes")) {
            StoryNpc pip = npc("pip");
            if (pip != null) pip.follow();
        }
        syncDirty = true;
    }

    // =========================================================================================
    // nights on the island
    // =========================================================================================
    public void nightFall() {
        ServerLevel level = level();
        long t = level.getDayTime() % 24000;
        if (t < 12500) level.setDayTime(level.getDayTime() - t + 12500);
    }

    /** Spawns the Unrated around the player until dawn, then sets {@code dawnFlag}. */
    public void nightWatch(StoryContext c, String dawnFlag, boolean firstNight) {
        ServerLevel level = level();
        ServerPlayer p = c.player();
        long t = level.getDayTime() % 24000;
        boolean night = t >= 13000 && t <= 23000;
        if (night) {
            c.state().flag("night:" + dawnFlag);
            level.setDayTime(level.getDayTime() + 1);
            if (state.questTicks % 160 == 0) {
                int watchers = level.getEntitiesOfClass(HollowEntity.class, p.getBoundingBox().inflate(64)).size();
                int max = firstNight ? 2 : dawnFlag.equals("dawn3") ? 7 : 4;
                if (watchers < max) spawnHollow(p, firstNight || level.random.nextFloat() < 0.3f, firstNight);
            }
            if (state.questTicks % 900 == 300) whisper(p);
        } else if (c.state().has("night:" + dawnFlag) && t < 12000) {
            c.flag(dawnFlag);
            for (HollowEntity h : level.getEntitiesOfClass(HollowEntity.class, p.getBoundingBox().inflate(96))) h.vanish();
            c.hint("Dawn. The figures are gone. You made it through the night.");
        }
    }

    public void spawnHollow(ServerPlayer p, boolean watcher, boolean harmless) {
        ServerLevel level = level();
        for (int tries = 0; tries < 12; tries++) {
            double yaw = Math.toRadians(p.getYRot() + (watcher ? level.random.nextInt(120) - 60 : 180 + level.random.nextInt(140) - 70));
            double dist = watcher ? 24 + level.random.nextInt(14) : 18 + level.random.nextInt(12);
            int x = (int) Math.floor(p.getX() - Math.sin(yaw) * dist), z = (int) Math.floor(p.getZ() + Math.cos(yaw) * dist);
            if (!Locations.onIsland(new BlockPos(x, 64, z))) continue;
            int dx = x - Locations.ISLAND_X, dz = z - Locations.ISLAND_Z;
            if (!IslandTerrain.isLand(dx, dz)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
            if (!below.getFluidState().isEmpty()) continue;
            HollowEntity h = ModEntities.HOLLOW.get().create(level);
            if (h == null) return;
            h.configure(watcher, harmless, 20 * 60 * 5);
            h.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360, 0);
            level.addFreshEntity(h);
            return;
        }
    }

    private void whisper(ServerPlayer p) {
        String[] lines = {"...thank you for flying...", "...four point zero... four point zero...", "...please remain seated...",
                "...we see you...", "...your experience is being recorded..."};
        fx(p, "whisper", lines[level().random.nextInt(lines.length)], 0, 0);
    }

    private void ambient(ServerPlayer p) {
        Quest q = Quests.get(state.quest);
        if (q == null) return;
        if (q.chapter <= 5 && tickCount % 1800 == 900) {
            String[] lines = {
                    "Reminder: Meridian International is a SmileScore partner airport. Frowning in the terminal may affect your rating.",
                    "Would the owner of a blue carry-on bag please return to... never mind. We know where you are.",
                    "Passenger " + p.getGameProfile().getName() + ", please stop looking at the cameras.",
                    "Meridian Flight 417 to Port Ellery is on time. Meridian Flight 417 is always on time.",
                    "Unattended luggage will be rated.",
                    "The arrivals hall remains closed for refurbishment. Meridian apologises for any arrivals.",
                    "Thank you for flying Meridian. Thank you for flying Meridian. Thank you for f-"};
            pa(p, lines[(int) ((tickCount / 1800) % lines.length)]);
        }
        if (q.chapter >= 10 && q.chapter <= 17 && state.has("on_island")) {
            long t = level().getDayTime() % 24000;
            boolean night = t >= 13500 && t <= 22500;
            boolean scripted = state.quest.equals("first_night") || state.quest.equals("night_watch") || state.quest.equals("last_night");
            if (night && !scripted && tickCount % 600 == 0 && Locations.onIsland(p.blockPosition())) {
                int near = level().getEntitiesOfClass(HollowEntity.class, p.getBoundingBox().inflate(64)).size();
                boolean afterWelcome = state.completed.contains("welcome");
                if (near < (afterWelcome ? 2 : 1)) spawnHollow(p, !afterWelcome || level().random.nextBoolean(), !afterWelcome);
            }
            if (night && tickCount % 2400 == 1200 && level().random.nextFloat() < 0.5f) whisper(p);
            if (state.quest.equals("missing_pip") && !state.has("found_pip") && tickCount % 200 == 0) {
                BlockPos g = IslandBuilder.grove(0, 0);
                if (p.blockPosition().distSqr(g) < 30 * 30 && level().getEntitiesOfClass(HollowEntity.class, p.getBoundingBox().inflate(40)).size() < 3) {
                    spawnHollow(p, true, true);
                }
            }
        }
    }

    /** People nearby say things now and then: speech bubbles above their heads. */
    private void barks(ServerPlayer p) {
        String phase = state.has("on_island") ? "island" : state.has("on_air_plane") ? (state.quest.equals("lights_out") && state.has("stare_over") ? "stare" : "plane") : "airport";
        List<StoryNpc> near = level().getEntitiesOfClass(StoryNpc.class, p.getBoundingBox().inflate(7), n -> n.canBark() && !n.talking());
        if (near.isEmpty() || level().random.nextFloat() > 0.18f) return;
        StoryNpc n = near.get(level().random.nextInt(near.size()));
        if (n.marker() == 1 && level().random.nextFloat() < 0.5f) {
            n.say(phase.equals("island") ? "Sky-fallen. Over here." : "Excuse me! Have you got a minute?", 70);
        } else {
            String line = Barks.pick(n.barkPool(), phase, new Random(level().random.nextLong()));
            if (line == null) return;
            n.say(line, 70);
        }
        n.barkCooldown(300 + level().random.nextInt(400));
    }

    // =========================================================================================
    // world interactions
    // =========================================================================================
    @Nullable private BlockPos pendingClue;

    void onTalk(ServerPlayer p, StoryNpc npc) {
        openDialogue(p, npc.dialogue(), npc);
    }

    void npcHit(ServerPlayer p, StoryNpc npc) {
        if (state.has("on_island") && npc.group().equals("tribe")) {
            npc.say("Hey! Hands off, sky-fallen.", 50);
            return;
        }
        npc.say(state.has("stare_over") && state.has("on_air_plane") ? "...four point zero." : "Ow! I'm rating that.", 50);
        rate(p, -0.1f, "Assaulted " + npc.displayName());
    }

    void onKill(ServerPlayer p, Entity e) {
        if (e.getType() == EntityType.PIG && state.has("side:hunt") && !state.has("sidedone:hunt") && Locations.onIsland(e.blockPosition())) {
            state.addCounter("boars", 1);
            syncDirty = true;
        }
    }

    void onSit(ServerPlayer p, BlockPos pos) {
        String q = state.quest;
        int gx = Locations.GATE_B7;
        if (q.equals("wait_gate") && pos.getX() >= gx - 6 && pos.getX() <= gx + 6 && pos.getZ() >= 34 && pos.getZ() <= 48) {
            state.flag("waited_gate");
            hint(p, "You sit and wait. Through the window, Flight 417 waits too.");
        }
        if (q.equals("find_seat")) {
            if (pos.equals(Locations.GATE_PLANE.seat(14, 'A'))) {
                if (state.has("stowed_bag")) state.flag("seated_14a");
                else hint(p, "Stow your bag in the overhead bin above 14A first.");
            } else if (pos.getY() == Locations.GATE_PLANE.y() + 1) {
                hint(p, "This isn't your seat. You're in 14A: row 14, window, left side.");
            }
        }
        if (q.equals("lights_out") && pos.equals(Locations.AIR_PLANE.seat(14, 'A'))) {
            if (state.has("june_stare")) state.flag("seated_air");
            else hint(p, "You can't sit still. Not until you've talked to June.");
        }
        if (q.equals("empty_seats") && pos.equals(IslandBuilder.loneSeatPos())) openDialogue(p, "lone_seat", null);
        syncDirty = true;
    }

    /** The trapdoor to the hold: Theo won't let you near it unless he's been sent away. Returns true to block it. */
    boolean onHatch(ServerPlayer p) {
        if (state.has("on_island")) return false;
        if (!state.quest.equals("the_hold") || state.has("theo_distracted")) return false;
        StoryNpc theo = npc("theo");
        if (theo != null && theo.distanceTo(p) < 6) {
            theo.say("Ah-ah. Crew only.", 50);
            hint(p, "Theo is standing right beside the hatch. Get rid of him first.");
            return true;
        }
        return false;
    }

    void onInteract(ServerPlayer p, String key, BlockPos pos, BlockState bs, Direction face) {
        String q = state.quest;
        ItemStack held = p.getMainHandItem();
        switch (key) {
            case "departure_board" -> openDialogue(p, "board", null);
            case "kiosk" -> openDialogue(p, "kiosk", null);
            case "tv" -> openDialogue(p, "lounge_tv", null);
            case "poster" -> openDialogue(p, "clue:missing_poster", null);
            case "clue" -> {
                String id = state.clueAt.get(pos.asLong());
                if (id == null) { hint(p, "A scrap of paper, too faded to read."); return; }
                pendingClue = pos;
                openDialogue(p, "clue:" + id, null);
            }
            case "pickup" -> pickup(p, pos, bs);
            case "conveyor", "xray" -> {
                if (q.equals("screening") && held.is(ModItems.CARRY_ON_BAG.get())) {
                    held.shrink(1);
                    state.flag("bag_on_belt");
                    sound(p, SoundEvents.MINECART_RIDING, 0.4f, 1.6f);
                    hint(p, "Your bag rolls into the X-ray machine. Now walk through the scanner arch.");
                } else if (q.equals("screening")) {
                    hint(p, "Hold your carry-on bag in your main hand, then use the belt.");
                } else {
                    hint(p, "The belt hums and rattles. Nothing on it but grey plastic trays.");
                }
            }
            case "boarding_scanner" -> {
                if (pos.getX() != Locations.GATE_B7) { hint(p, "This podium is for a different flight."); return; }
                if (!q.equals("boarding_call")) { hint(p, "PLEASE WAIT. YOUR GROUP HAS NOT BEEN CALLED."); return; }
                if (!held.is(ModItems.BOARDING_PASS.get())) { hint(p, "Hold your boarding pass in your hand to scan it."); return; }
                if (state.rating < 3.0f) {
                    sound(p, Snd.of(SoundEvents.NOTE_BLOCK_BASS), 1f, 0.5f);
                    openDialogue(p, "corinne_low", npc("corinne"));
                } else {
                    sound(p, Snd.of(SoundEvents.NOTE_BLOCK_CHIME), 1f, 1.5f);
                    state.flag("scanned_pass");
                    openBarrier("jet_bridge");
                    hint(p, "*beep* Boarding pass accepted. Have a lovely flight, " + p.getGameProfile().getName() + ".");
                }
            }
            case "vending" -> {
                if (state.has("vend:" + pos.asLong())) { hint(p, "SOLD OUT. Thank you for smiling."); return; }
                state.flag("vend:" + pos.asLong());
                give(p, new ItemStack(ModItems.AIRLINE_PEANUTS.get(), 2));
                sound(p, SoundEvents.CHAIN_PLACE, 1f, 1.2f);
            }
            case "suitcase" -> searchLuggage(p, pos, bs);
            case "overhead_bin" -> {
                Plane g = Locations.GATE_PLANE;
                BlockPos s14 = g.seat(14, 'A');
                boolean row14 = Math.abs(pos.getX() - s14.getX()) <= 2 && pos.getZ() < g.z() && pos.getY() == g.y() + 3;
                if (q.equals("find_seat") && held.is(ModItems.CARRY_ON_BAG.get()) && row14) {
                    held.shrink(1);
                    state.flag("stowed_bag");
                    sound(p, SoundEvents.CHEST_CLOSE, 1f, 1.4f);
                    if (state.has("binned_polaroid") && !state.has("bin_polaroid")) {
                        state.flag("bin_polaroid");
                        give(p, new ItemStack(ModItems.POLAROID_SEAT.get()));
                        hint(p, "Lying in the empty bin, face up: the polaroid Reyes threw away.");
                    } else {
                        hint(p, "You stow your bag. Now take your seat: 14A.");
                    }
                } else if (q.equals("find_seat") && held.is(ModItems.CARRY_ON_BAG.get())) {
                    hint(p, "This bin is full. Your row is 14: count the row signs from the front.");
                } else if (q.equals("find_seat")) {
                    hint(p, "Hold your carry-on bag in your hand to stow it.");
                } else {
                    hint(p, "The bin is latched shut.");
                }
            }
            case "call_button" -> {
                sound(p, Snd.of(SoundEvents.NOTE_BLOCK_CHIME), 0.8f, 1.8f);
                if (q.equals("cruise") && !state.has("called_june")) {
                    state.flag("called_june");
                    StoryNpc june = npc("june");
                    Plane a = Locations.AIR_PLANE;
                    BlockPos s = a.seat(14, 'A');
                    if (june != null) june.walkRoute(List.of(a.aisle(s.getX() - a.x() - 1)), null);
                    hint(p, "*ding* June is on her way.");
                }
            }
            case "seat_screen" -> {
                if (pos.getY() == Locations.AIR_PLANE.y() + 1 && (q.equals("strange") || state.has("watched_screen"))) openDialogue(p, "seat_screen", null);
                else openDialogue(p, "seat_screen_idle", null);
            }
            case "mirror" -> {
                if (pos.getY() == Locations.AIR_PLANE.y() + 2 && (q.equals("strange") || state.has("saw_mirror"))) openDialogue(p, "mirror", null);
                else hint(p, "You look tired.");
            }
            case "galley" -> hint(p, "Tiny bottles, tiny cans, tiny sealed meals. Every one of them has a smiling sun on the lid.");
            case "monitor" -> hint(p, bs.getValue(StateInteractBlock.MODE) == 2 ? "Dark." : "Static. Then, for a moment, you, from above. Then static.");
            case "polaroid_wall" -> openDialogue(p, "polaroid_wall", null);
            case "keypad" -> {
                if (!Locations.onIsland(pos)) { openDialogue(p, "staff_keypad", null); return; }
                if (state.completed.contains("what_we_were") || state.has("door_open")) openDialogue(p, "keypad", null);
                else hint(p, "A keypad. Four digits. You have no idea what they are. Yet.");
            }
            case "halo_terminal" -> {
                if (state.has("ending_free")) hint(p, "The screen is dark except for one line: GOODBYE, " + p.getGameProfile().getName().toUpperCase(Locale.ROOT) + ".");
                else openDialogue(p, "terminal", null);
            }
            case "transmitter" -> {
                if (state.has("read_logs") || state.has("ending_loop")) openDialogue(p, "transmitter", null);
                else hint(p, "It hums. It's warm. Every red light on it is blinking in time with your heartbeat.");
            }
            case "boat_frame" -> boatFrame(p);
            case "boat_helm" -> {
                if (q.equals("last_night")) openDialogue(p, "helm", null);
                else hint(p, "The helm of Teo's boat.");
            }
            default -> {}
        }
        syncDirty = true;
    }

    private void pickup(ServerPlayer p, BlockPos pos, BlockState bs) {
        if (bs.is(ModBlocks.LOST_PLUSH.get())) {
            if (!state.has("side:floppy")) startSide(p, "floppy");
            give(p, new ItemStack(ModItems.PLUSH_RABBIT.get()));
            state.flag("has_plush");
            hint(p, "A grey rabbit with one ear. Its fur is damp, and it smells of the sea.");
        } else if (bs.is(ModBlocks.WET_FLOOR_SIGN.get())) {
            if (!state.has("side:wet_floor")) { hint(p, "A yellow WET FLOOR sign. Somebody will be looking for this."); return; }
            give(p, new ItemStack(ModItems.WET_FLOOR_SIGN.get()));
            state.flag("got_sign");
        } else if (bs.is(ModBlocks.ENGINE_PART.get())) {
            give(p, new ItemStack(ModItems.ENGINE_COIL.get()));
            state.flag("got_coil");
            hint(p, "The coil is still faintly warm, even underwater. Stamped on the casing: PROPERTY OF MERIDIAN EXPERIENCES. SET PIECE 2.");
        } else {
            return;
        }
        level().removeBlock(pos, false);
        sound(p, SoundEvents.ITEM_PICKUP, 1f, 1f);
    }

    void onCockpitDoor(ServerPlayer p) {
        openDialogue(p, "cockpit", null);
    }

    private void searchLuggage(ServerPlayer p, BlockPos pos, BlockState bs) {
        if (!state.has("on_island") || !Locations.onIsland(pos)) { hint(p, "Not yours. Probably full of sunscreen."); return; }
        if (bs.getValue(StateInteractBlock.MODE) != 0) { hint(p, "Already searched. Empty, except for sand."); return; }
        level().setBlock(pos, bs.setValue(StateInteractBlock.MODE, 1), 3);
        int n = state.counter("searched_luggage");
        state.addCounter("searched_luggage", 1);
        sound(p, SoundEvents.ARMOR_EQUIP_LEATHER, 1f, 0.8f);
        Random r = new Random(pos.asLong());
        List<ItemStack> loot = new ArrayList<>();
        switch (n) {
            case 0 -> { loot.add(new ItemStack(ModItems.EMPTY_FLASK.get())); loot.add(new ItemStack(ModItems.FLARE.get())); loot.add(new ItemStack(ModItems.AIRLINE_BLANKET.get())); }
            case 1 -> { loot.add(new ItemStack(ModItems.AIRLINE_BLANKET.get())); loot.add(new ItemStack(ModItems.AIRLINE_PEANUTS.get(), 3)); }
            case 2 -> { loot.add(new ItemStack(ModItems.STONE_KNIFE.get())); loot.add(new ItemStack(ModItems.AIRLINE_MEAL.get(), 2)); }
            default -> {
                if (r.nextFloat() < 0.6f) loot.add(new ItemStack(ModItems.AIRLINE_BLANKET.get()));
                if (r.nextFloat() < 0.5f) loot.add(new ItemStack(ModItems.AIRLINE_PEANUTS.get(), 1 + r.nextInt(3)));
                if (r.nextFloat() < 0.25f) loot.add(new ItemStack(ModItems.EMPTY_FLASK.get()));
                if (r.nextFloat() < 0.2f) loot.add(new ItemStack(ModItems.FLARE.get()));
                if (r.nextFloat() < 0.3f) loot.add(new ItemStack(Items.STRING, 1 + r.nextInt(3)));
                if (r.nextFloat() < 0.15f) loot.add(new ItemStack(Items.PAPER, 2));
            }
        }
        if (loot.isEmpty()) hint(p, "Clothes, soaked through. A paperback swollen with seawater. Nothing useful.");
        for (ItemStack s : loot) give(p, s);
        if (n == 3) hint(p, "Every suitcase has a luggage tag. Every tag says the same name. Yours.");
        if (n == 5 && state.has("floppy_returned")) hint(p, "Tucked into a side pocket: a grey rabbit with one ear. Captain Floppy. Leo was holding him when you boarded.");
    }

    private record Need(String key, int amount, Predicate<ItemStack> match) {}

    private List<Need> stageNeeds(int stage) {
        return switch (stage) {
            case 1 -> List.of(new Need("palm_log", 16, s -> s.is(ModBlocks.PALM_LOG.get().asItem())));
            case 2 -> List.of(new Need("hull_panel", 8, s -> s.is(ModItems.HULL_PANEL.get())), new Need("palm_planks", 24, s -> s.is(ModBlocks.PALM_PLANKS.get().asItem())));
            case 3 -> List.of(new Need("mast", 1, s -> s.is(ModItems.MAST.get())), new Need("rope", 8, s -> s.is(ModItems.ROPE.get())));
            case 4 -> List.of(new Need("sail_cloth", 1, s -> s.is(ModItems.SAIL_CLOTH.get())));
            case 5 -> List.of(new Need("engine_coil", 1, s -> s.is(ModItems.ENGINE_COIL.get())), new Need("water_flask", 4, s -> s.is(ModItems.WATER_FLASK.get())),
                    new Need("food", 8, s -> s.isEdible() && !s.is(ModItems.WATER_FLASK.get()) && !s.is(ModItems.SEA_WATER_FLASK.get())
                            && !s.is(ModItems.GHOST_TONIC.get()) && !s.is(ModItems.COFFEE.get())));
            default -> List.of();
        };
    }

    private void boatFrame(ServerPlayer p) {
        if (!state.quest.equals("boat_build")) {
            hint(p, state.completed.contains("boat_build") ? "The boat is ready." : "An empty boat frame. Teo's work, waiting for a reason.");
            return;
        }
        int stage = 1;
        while (stage <= 5 && state.has("boat_" + stage)) stage++;
        if (stage > 5) return;
        boolean any = false;
        boolean complete = true;
        StringBuilder missing = new StringBuilder();
        for (Need n : stageNeeds(stage)) {
            String c = "boat:" + n.key();
            int have = state.counter(c);
            int want = n.amount() - have;
            if (want > 0) {
                int give = Math.min(want, count(p, n.match()));
                if (give > 0 && take(p, n.match(), give)) {
                    state.addCounter(c, give);
                    any = true;
                }
            }
            int now = state.counter(c);
            if (now < n.amount()) {
                complete = false;
                if (missing.length() > 0) missing.append(", ");
                missing.append(n.amount() - now).append(" more ").append(n.key().replace('_', ' '));
            }
        }
        if (complete) {
            state.flag("boat_" + stage);
            IslandBuilder.boatStage(new Placer(level()), stage);
            sound(p, SoundEvents.WOOD_PLACE, 1f, 0.8f);
            sound(p, SoundEvents.ANVIL_USE, 0.4f, 1.4f);
            String[] done = {"", "The keel is laid.", "The hull is planked and patched with fuselage.", "The mast is up and rigged.",
                    "The sail is bent on. Meridian blue stripe and all.", "Provisions loaded. The boat is ready."};
            hint(p, done[stage] + (stage < 5 ? " Teo grunts approvingly." : ""));
        } else if (any) {
            sound(p, SoundEvents.WOOD_PLACE, 1f, 1.1f);
            hint(p, "Added. Still needed: " + missing + ".");
        } else {
            hint(p, "Still needed: " + missing + ".");
        }
        syncDirty = true;
    }

    public void event(ServerPlayer p, String name) {
        switch (name) {
            case "open_bridge" -> openBarrier("jet_bridge");
            case "staff_door" -> {
                AirportBuilder.staffDoor(new Placer(level()), true);
                sound(p, SoundEvents.IRON_DOOR_OPEN, 1f, 0.9f);
            }
            case "caught" -> caught(p, null);
            case "theo_away" -> {
                StoryNpc theo = npc("theo");
                Plane a = Locations.AIR_PLANE;
                if (theo != null) theo.walkRoute(List.of(a.aisle(40), a.aisle(18)), null);
            }
            case "screens_on" -> {
                PlaneBuilder.screens(new Placer(level()), Locations.AIR_PLANE, 1);
                schedule(600, () -> PlaneBuilder.screens(new Placer(level()), Locations.AIR_PLANE, 2));
            }
            case "pip_follow" -> {
                StoryNpc pip = npc("pip");
                if (pip != null) pip.follow();
            }
            case "grove_photo" -> {
                fx(p, "scare", 1, 30);
                schedule(10, () -> {
                    HollowEntity h = ModEntities.HOLLOW.get().create(level());
                    if (h == null) return;
                    Vec3 behind = p.position().subtract(p.getLookAngle().multiply(2.5, 0, 2.5));
                    h.configure(false, true, 20 * 30);
                    h.moveTo(behind.x, p.getY(), behind.z, p.getYRot(), 0);
                    level().addFreshEntity(h);
                });
            }
            case "open_lab" -> {
                IslandBuilder.openStationDoor(new Placer(level()), true);
                sound(p, SoundEvents.IRON_DOOR_OPEN, 1f, 0.5f);
            }
            case "chart" -> {
                if (count(p, s -> s.is(ModItems.NAVIGATION_CHART.get())) == 0 && !state.has("got_chart")) {
                    state.flag("got_chart");
                    give(p, new ItemStack(ModItems.NAVIGATION_CHART.get()));
                }
            }
            case "kill_transmitter" -> {
                IslandBuilder.stationPowerDown(new Placer(level()));
                sound(p, SoundEvents.BEACON_DEACTIVATE, 1f, 0.5f);
                fx(p, "glitch", 1, 60);
                for (HollowEntity h : level().getEntitiesOfClass(HollowEntity.class, p.getBoundingBox().inflate(96))) h.vanish();
                if (count(p, s -> s.is(ModItems.NAVIGATION_CHART.get())) == 0 && !state.has("got_chart")) {
                    state.flag("got_chart");
                    give(p, new ItemStack(ModItems.NAVIGATION_CHART.get()));
                }
            }
            default -> {}
        }
        syncDirty = true;
    }

    void onHollowTouch(HollowEntity h, net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer sp) {
            fx(sp, "scare", 1, 20);
            if (sp.getInventory().contains(new ItemStack(ModItems.TIDEFOLK_CHARM.get())) && level().random.nextFloat() < 0.5f) {
                h.vanish();
                hint(sp, "The charm at your neck grows warm. The figure hesitates... and is gone.");
            }
        }
    }

    void onPlace(ServerPlayer p, BlockState placed) {
        if (state.quest.equals("shelter") && placed.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)) {
            state.flag("has_campfire");
            syncDirty = true;
        }
    }

    public Component status() {
        return Component.literal("Quest: " + state.quest + " (t=" + state.questTicks + "), rating " + state.rating + ", island " + island.percent() + "%");
    }
}
