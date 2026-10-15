package com.airlinestory.story;

import com.airlinestory.entity.StoryNpc;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;

/** What a dialogue choice, quest hook or story script can do. Server side only. */
public final class StoryContext {
    private final StoryDirector director;
    private final ServerPlayer player;
    @Nullable private final StoryNpc npc;
    private final String speaker;

    public StoryContext(StoryDirector director, ServerPlayer player, @Nullable StoryNpc npc, String speaker) {
        this.director = director;
        this.player = player;
        this.npc = npc;
        this.speaker = speaker;
    }

    public StoryDirector director() { return director; }
    public ServerPlayer player() { return player; }
    public ServerLevel level() { return player.serverLevel(); }
    public StoryState state() { return director.state(); }
    @Nullable public StoryNpc npc() { return npc; }
    public String speaker() { return speaker; }

    public boolean has(String f) { return state().has(f); }
    public void flag(String f) { state().flag(f); director.markSync(); }
    public void unflag(String f) { state().unflag(f); director.markSync(); }
    public int counter(String k) { return state().counter(k); }
    public void addCounter(String k, int d) { state().addCounter(k, d); director.markSync(); }

    public void rate(float delta, String reason) { director.rate(player, delta, reason); }

    public void remember(String text) { director.remember(player, speaker, text); }

    public void give(ItemLike item, int n) { director.give(player, new ItemStack(item, n)); }

    public void give(ItemStack stack) { director.give(player, stack); }

    public boolean take(ItemLike item, int n) { return director.take(player, s -> s.is(item.asItem()), n); }

    public boolean takeTag(TagKey<Item> tag, int n) { return director.take(player, s -> s.is(tag), n); }

    public int count(ItemLike item) { return director.count(player, s -> s.is(item.asItem())); }

    public void pa(String text) { director.pa(player, text); }

    public void halo(String text) { director.halo(player, text); }

    public void hint(String text) { director.hint(player, text); }

    @Nullable public StoryNpc npc(String id) { return director.npc(id); }

    public void open(String dialogueId) { director.openLater(player, dialogueId, 2); }

    public void event(String name) { director.event(player, name); }
}
