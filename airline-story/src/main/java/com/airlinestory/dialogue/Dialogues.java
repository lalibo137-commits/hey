package com.airlinestory.dialogue;

import com.airlinestory.story.Clues;
import com.airlinestory.story.FlagView;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static com.airlinestory.dialogue.Dialogue.tree;

/** Every conversation in the story. {player} and {rating} are filled in when shown. */
public final class Dialogues {
    private Dialogues() {}

    private static final Map<String, Dialogue> TREES = new HashMap<>();

    @Nullable
    public static Dialogue get(String id) {
        return TREES.get(id);
    }

    static void add(Dialogue.Builder b) {
        Dialogue d = b.build();
        TREES.put(d.id, d);
    }

    static boolean q(FlagView v, String quest) {
        return quest.equals(v.quest());
    }

    static {
        AirportTalk.register();
        PlaneTalk.register();
        IslandTalk.register();
        ObjectTalk.register();
        clues();
        crowd();
    }

    /** Every clue block opens a short reader with the clue's text. */
    private static void clues() {
        for (Clues.Clue c : Clues.all()) {
            add(tree("clue:" + c.id(), c.title())
                    .start(v -> v.has("clue:" + c.id()) ? "again" : "read")
                    .node("read", c.text()).as(c.title())
                    .end("[Keep it]").flag("clue:" + c.id())
                    .node("again", c.text()).as(c.title())
                    .end("[Put it back]"));
        }
    }

    private static void crowd() {
        String[][] travelers = {
                {"Is it me or is the PA a bit... personal today? It said my name. I never gave anyone my name.", "Maybe it's a loyalty thing.", "Maybe. I've never been loyal to anything."},
                {"I've been at this airport for, what, three days? Four? My flight keeps saying ON TIME.", "Have you asked anyone?", "Everyone says it's on time. It's very on time. It's the most on time it's ever been."},
                {"Don't stand too close. My rating can't take another 'invaded personal space'.", "Sorry.", "Thank you. Rate me for my patience? No? Fine."},
                {"Meridian? Brave. My cousin took Meridian. Lovely airline. Lovely, lovely airline. Haven't heard from her.", "How long ago?", "Four years. She sends a postcard every Christmas. Same postcard."},
                {"Smile! They're filming. They're always filming. Smile with your teeth.", "Who is filming?", "Everyone, sweetheart. Everyone and the walls."},
                {"Excuse me, do you know which gate Port Ellery is? I've asked six people and they all pointed at you.", "B7.", "B7. Of course. Everyone ends up at B7."},
                {"Have you noticed you never see anyone arrive? Departures, departures. The arrivals hall's been closed for refurbishment since I was a kid.", "That's odd.", "Is it? I didn't think so until right now."},
                {"I gave the coffee guy five stars and he cried. Is that normal? I feel like that's not normal.", "Tomas?", "He said nobody's ever rated him above three. He said thank you about forty times."},
        };
        for (int i = 0; i < travelers.length; i++) {
            String[] t = travelers[i];
            add(tree("traveler_" + i, "Traveler")
                    .node("start", t[0])
                    .opt(t[1], "reply")
                    .opt("[Smile politely]", null).rate(0.02f)
                    .end("[Leave]")
                    .node("reply", t[2])
                    .end("[Leave]"));
        }
        add(tree("passenger", "Passenger")
                .start(v -> v.has("stare_over") ? "after" : v.has("watched_screen") ? "uneasy" : "calm")
                .node("calm", "Long flight. I always watch the same film. It's about a plane. I never see the ending, I fall asleep.")
                .end("[Leave]")
                .node("uneasy", "Have you noticed the screens? Mine keeps showing the back of my own head. I've decided not to notice.")
                .end("[Leave]")
                .node("after", "They don't blink. Their mouth keeps smiling. When you step away, they whisper: four point zero. four point zero. four point zero.")
                .end("[Back away]"));
        add(tree("passenger_b", "Passenger")
                .start(v -> v.has("stare_over") ? "after" : "calm")
                .node("calm", "Port Ellery! Never been. Nobody I know has ever been. That's the appeal, isn't it?")
                .opt("Nobody you know has ever been?", "nobody")
                .end("[Leave]")
                .node("nobody", "Well, they've been. They just don't come back. Which says a lot about Port Ellery, doesn't it? Lovely place, I bet.")
                .end("[Leave]")
                .node("after", "Sit down, please. You're blocking the view. Their eyes are fixed on the back of your head, even while you're standing in front of them.")
                .end("[Leave]"));
        add(tree("passenger_c", "Passenger")
                .start(v -> v.has("stare_over") ? "after" : "calm")
                .node("calm", "Sorry, I'm in the middle of a film. A man gets on a plane and the plane never lands. It's a comedy, apparently.")
                .end("[Leave]")
                .node("after", "Their screen shows seat 14A from above. You, standing in the aisle, looking at a screen that shows you, standing in the aisle.")
                .end("[Back away]"));
    }
}
