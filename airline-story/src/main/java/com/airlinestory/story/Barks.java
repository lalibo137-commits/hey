package com.airlinestory.story;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Short lines people say when you walk past. Pools are keyed by the NPC's bark pool and the phase. */
public final class Barks {
    private Barks() {}

    private static final Map<String, String[]> POOLS = new HashMap<>();

    private static void add(String pool, String... lines) {
        POOLS.put(pool, lines);
    }

    /** Phase is "airport", "plane", "stare" (after the lights went out) or "island". */
    public static String pick(String pool, String phase, Random r) {
        String[] lines = POOLS.get(pool + "@" + phase);
        if (lines == null) lines = POOLS.get(pool);
        if (lines == null) lines = POOLS.get("crowd@" + phase);
        if (lines == null || lines.length == 0) return null;
        return lines[r.nextInt(lines.length)];
    }

    static {
        add("crowd@airport", "Smile, they're filming.", "Four point two. Come on, four point two...", "Is it me or is it really quiet?",
                "Have you tried the coffee? Don't.", "Excuse me. Sorry. Excuse me.", "My flight's been 'on time' for three hours.",
                "Did you see the poster by the lounge?", "Rate me, I'll rate you.", "They keep saying my name on the PA.",
                "I love Meridian. I love Meridian. I love Meridian.", "*checks phone*", "Where's gate B7 again?");
        add("crowd@plane", "Long flight.", "Is the pilot ever going to say anything?", "My screen keeps showing my own seat.",
                "Can you sit down? You're blocking the aisle.", "*snores*", "Port Ellery. Never been. Nobody's been.");
        add("crowd@stare", "...", "Sit down.", "Four point zero.", "We see you.", "Please return to your seat.", "Smile.");
        add("staff@airport", "Have a lovely day!", "Please keep moving.", "Every interaction is rated!", "Thank you for flying Meridian.");
        add("guard@airport", "This area is restricted.", "Nothing to see back here.", "Keep it moving.", "Who's on CAM 12?");
        add("tribe@island", "Sky-fallen.", "Don't count the days.", "The tide's turning.", "Fire keeps them back. Mostly.",
                "Mind the fish racks.", "Ahu wants to see you, maybe.", "The Eye is quiet today.", "Smells like rain.");
        add("dana", "Next, please!", "Welcome to Meridian!", "Smile! You're on camera.");
        add("reyes", "Belt. Arch. Go.", "Nineteen years.", "Don't stop in the arch.");
        add("tomas", "Oat flat white, extra hot, no lid...", "Coffee's on the house for the 417.", "Long shift.");
        add("edith", "Knit one, purl one.", "You always choose the window.", "Count your fingers, dear.");
        add("marcus", "Do they check the planes? Like, really check them?", "Nate would know what to do.", "I hate flying. I hate flying.");
        add("priya", "Leo, sit DOWN.", "Isn't that strange?", "We're going to see his grandmother.");
        add("leo", "Have you seen Captain Floppy?", "There's an island. I dreamed it.", "Why is everyone smiling?");
        add("halvorsen", "Four point nine one.", "Rate me. Go on.", "Eighteen hours a day of being pleasant.");
        add("ines", "*sketching*", "Page twelve isn't finished.", "Hold still.");
        add("june", "Can I get you anything?", "Seat 14A, isn't it?", "HALO is very pleased with you.");
        add("nalu@island", "Keep your eyes open.", "Fish bite at dawn.", "My brother would've liked you.");
        add("mira@island", "Bitterleaf for the fever.", "Don't drink the sea.", "Sit, I'll look at that cut.");
        add("teo@island", "Straight planks. Straight.", "The sea's calm. That's when it's dangerous.", "One day, a boat that floats.");
        add("pip@island", "Is the world bigger than the island?", "I waved at the Eye today!", "Look! A crab!");
        add("ahu@island", "Sit with an old man.", "I flew the first one.", "The sea doesn't mind.");
    }
}
