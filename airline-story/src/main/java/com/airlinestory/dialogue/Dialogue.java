package com.airlinestory.dialogue;

import com.airlinestory.story.FlagView;
import com.airlinestory.story.StoryContext;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A conversation tree in the style of choice-driven adventure games: lines of text, up to four answers,
 * optional timed answers (silence if you wait too long) and characters that remember what you said.
 * Trees are plain Java data shared by client (screen) and server (effects).
 */
public final class Dialogue {
    public final String id;
    public final String speaker;
    /** Conversations with this person end with you rating them (and them rating you). */
    public boolean rateable;
    final Function<FlagView, String> start;
    final Map<String, Node> nodes = new LinkedHashMap<>();

    private Dialogue(String id, String speaker, Function<FlagView, String> start) {
        this.id = id;
        this.speaker = speaker;
        this.start = start;
    }

    public String startNode(FlagView v) {
        return start.apply(v);
    }

    @Nullable
    public Node node(String id) {
        return nodes.get(id);
    }

    public static final class Node {
        public final String id;
        public final String text;
        @Nullable public String speaker;
        public final List<Choice> choices = new ArrayList<>();
        /** Ticks before the timeout choice is picked automatically; 0 = wait forever. */
        public int timer;
        public int timeoutChoice = -1;

        Node(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    public static final class Choice {
        public final String text;
        @Nullable public final String next;
        Predicate<FlagView> condition = v -> true;
        @Nullable Consumer<StoryContext> effect;
        @Nullable public String remember;
        public float rating;

        Choice(String text, @Nullable String next) {
            this.text = text;
            this.next = next;
        }

        public boolean visible(FlagView v) {
            return condition.test(v);
        }

        public void apply(StoryContext ctx) {
            if (rating != 0) ctx.rate(rating, text);
            if (remember != null) ctx.remember(remember);
            if (effect != null) effect.accept(ctx);
        }
    }

    // ---------------- builder ----------------
    public static Builder tree(String id, String speaker) {
        return new Builder(id, speaker);
    }

    public static final class Builder {
        private final String id;
        private final String speaker;
        private Function<FlagView, String> start = v -> "start";
        private boolean rateable;
        private final Map<String, Node> nodes = new LinkedHashMap<>();
        private Node node;
        private Choice choice;

        Builder(String id, String speaker) {
            this.id = id;
            this.speaker = speaker;
        }

        public Builder start(Function<FlagView, String> s) { this.start = s; return this; }

        public Builder rateable() { this.rateable = true; return this; }

        /** A line spoken by the tree's speaker. */
        public Builder node(String nid, String text) {
            node = new Node(nid, text);
            nodes.put(nid, node);
            choice = null;
            return this;
        }

        /** Override who speaks this line (e.g. "You", "PA", "HALO"). */
        public Builder as(String who) { node.speaker = who; return this; }

        /** An answer leading to {@code next} (null ends the conversation). */
        public Builder opt(String text, @Nullable String next) {
            choice = new Choice(text, next);
            node.choices.add(choice);
            return this;
        }

        /** A single "continue" answer. */
        public Builder cont(@Nullable String next) { return opt(next == null ? "[Leave]" : "...", next); }

        public Builder end(String text) { return opt(text, null); }

        public Builder when(Predicate<FlagView> c) { choice.condition = c; return this; }

        public Builder rate(float r) { choice.rating = r; return this; }

        public Builder remember(String text) { choice.remember = text; return this; }

        public Builder fx(Consumer<StoryContext> e) {
            Consumer<StoryContext> prev = choice.effect;
            choice.effect = prev == null ? e : prev.andThen(e);
            return this;
        }

        public Builder flag(String... flags) {
            return fx(ctx -> { for (String f : flags) ctx.flag(f); });
        }

        /** Make the current node timed: after {@code seconds}, choice #{@code index} is picked for you. */
        public Builder timed(int seconds, int index) {
            node.timer = seconds * 20;
            node.timeoutChoice = index;
            return this;
        }

        public Dialogue build() {
            Dialogue d = new Dialogue(id, speaker, start);
            d.rateable = rateable;
            d.nodes.putAll(nodes);
            return d;
        }
    }
}
