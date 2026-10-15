package com.airlinestory.story;

import com.airlinestory.entity.StoryNpc;
import com.airlinestory.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.airlinestory.story.Objective.*;

/** The whole story, in order. */
public final class Quests {
    private Quests() {}

    public static final String FIRST = "arrive";
    public static final String[] CHAPTERS = {
            "", "Departures", "Security", "Gate B7", "Boarding", "Cruising Altitude", "Turbulence",
            "The Shore", "The Tidefolk", "The Unrated", "The Eye", "The Boat", "Last Call", "Epilogue"
    };

    private static final Map<String, Quest> QUESTS = new LinkedHashMap<>();

    @Nullable
    public static Quest get(String id) {
        return QUESTS.get(id);
    }

    public static Collection<Quest> all() {
        return QUESTS.values();
    }

    private static Quest q(String id, int chapter, String title, String desc) {
        Quest q = new Quest(id, chapter, title, desc);
        QUESTS.put(id, q);
        return q;
    }

    static {
        // =====================================================================================
        // Chapter 1 - Departures
        // =====================================================================================
        q("arrive", 1, "Welcome to Meridian",
                "The taxi has left you at the kerb of Meridian International. Flight MA417 to Port Ellery leaves from Gate B7 at 9:10. "
                        + "Everyone says Meridian is the friendliest airline in the world. Everyone has to say that.")
                .obj(go("terminal", "Enter the departures hall", "The glass doors are straight ahead of you. Walk inside."))
                .start(c -> {
                    StoryDirector d = c.director();
                    d.gamemode(c.player(), GameType.ADVENTURE);
                    c.give(ModItems.TRAVEL_JOURNAL.get(), 1);
                    d.giveOwned(c.player(), ModItems.PASSPORT.get());
                    c.give(ModItems.CARRY_ON_BAG.get(), 1);
                })
                .resume(c -> c.director().gamemode(c.player(), GameType.ADVENTURE))
                .tick((c, t) -> {
                    if (t == 60) c.director().cutscene(c.player(), "intro");
                    if (t > 60 && c.has("cs:intro") && !c.has("intro_pa")) {
                        c.flag("intro_pa");
                        c.pa("Good morning and welcome to Meridian International. Please keep your belongings with you at all times. Courtesy is rated.");
                        c.hint("Press [J] to open your Travel Journal: it always tells you what to do next.");
                    }
                })
                .then("checkin");

        q("checkin", 1, "Check In",
                "Meridian passengers check in at the blue desk at the back of the hall. The queue is short. The agent's smile is very wide.")
                .obj(talk("dana", "Dana", "checked_in", "Check in with Dana at the Meridian desk",
                        "Dana is behind the blue Meridian counter at the back of the hall. Right-click her to talk."))
                .then("before_security");

        q("before_security", 1, "Time to Kill",
                "You have your boarding pass. Check the departures board for your gate. "
                        + "The old woman on the benches by the west windows has been watching you since you walked in.")
                .obj(flag("read_board", "board", "Check the departures board", "The big screen on a pillar in the middle of the hall. Right-click it."))
                .obj(talk("edith", "Edith", "talked_edith", "Talk to the old woman on the benches", "She is knitting on the benches by the west windows."))
                .done(c -> {
                    c.director().openBarrier("to_security");
                    c.pa("Passengers travelling on Meridian Flight 417 to Port Ellery, please make your way through security. Thank you for smiling.");
                })
                .then("security");

        // =====================================================================================
        // Chapter 2 - Security
        // =====================================================================================
        q("security", 2, "Security",
                "Join the line at the security checkpoint through the doors on the east side of the hall. Officer Reyes is running the lane.")
                .obj(talk("reyes", "Officer Reyes", "reyes_intro", "Speak to Officer Reyes at the checkpoint",
                        "Go east through the opened doors. Reyes stands past the X-ray belt."))
                .then("screening");

        q("screening", 2, "Screening",
                "Put your carry-on on the X-ray belt, then walk through the body scanner like everybody else.")
                .obj(flag("bag_on_belt", "security", "Place your carry-on bag on the X-ray belt",
                        "Hold the bag in your hand and right-click the conveyor belt or the X-ray machine."))
                .obj(go("scanner", "Walk through the body scanner", "The grey arch beside the belt. Walk straight through it."))
                .done(c -> {
                    c.director().sound(c.player(), Snd.of(SoundEvents.NOTE_BLOCK_BIT), 1f, 0.5f);
                    c.director().sound(c.player(), Snd.of(SoundEvents.NOTE_BLOCK_BIT), 1f, 0.5f);
                    c.hint("*BEEP* The scanner arch flashes red.");
                })
                .then("flagged");

        q("flagged", 2, "Flagged",
                "The scanner went off, and the X-ray operator is frowning at something inside your bag. Officer Reyes would like a word.")
                .obj(talk("reyes", "Officer Reyes", "reyes_cleared", "Talk to Officer Reyes about your bag", "He's waiting at the end of the belt."))
                .done(c -> {
                    c.director().openBarrier("to_airside");
                    c.pa("Passenger " + c.player().getGameProfile().getName() + ", thank you for your patience at security. Your patience has been rated.");
                })
                .then("coffee");

        // =====================================================================================
        // Chapter 3 - Gate B7
        // =====================================================================================
        q("coffee", 3, "Cloud Nine",
                "You're airside. There is time for a coffee at Cloud Nine before you find your gate.")
                .obj(talk("tomas", "Tomas", "got_coffee", "Order a coffee from Tomas at Cloud Nine Coffee",
                        "The cafe is just past security, on the north side of the concourse."))
                .then("find_gate");

        q("find_gate", 3, "Gate B7",
                "Gate B7 is at the far east end of the concourse. Check in with the gate agent.")
                .obj(go("gate_b7", "Walk to Gate B7", "Follow the concourse east, past gates B5 and B6. The gates are by the big windows."))
                .obj(talk("corinne", "Corinne", "corinne_intro", "Speak to Corinne at the B7 podium", "She is standing at the boarding podium by the window."))
                .then("wait_gate");

        q("wait_gate", 3, "Fellow Passengers",
                "Boarding starts soon. Some of the people waiting at B7 will be on your flight. You could say hello. "
                        + "You could also notice how often they check their ratings.")
                .obj(talk("marcus", "Marcus", "talked_marcus", "Talk to the nervous man in the hoodie", "He's pacing near the B7 windows."))
                .obj(talk("priya", "Priya", "talked_priya", "Talk to the mother travelling with her son", "She's sitting in the B7 seating area."))
                .obj(talk("halvorsen", "Mr. Halvorsen", "talked_halvorsen", "Talk to the man in the grey suit", "He's standing near the B7 podium, staring at his phone."))
                .obj(flag("waited_gate", "gate_b7", "Take a seat at Gate B7 and wait for boarding",
                        "Right-click any seat at Gate B7. Sneak (Shift) to stand up again."))
                .done(c -> c.director().boards(1))
                .then("boarding_call");

        // =====================================================================================
        // Chapter 4 - Boarding
        // =====================================================================================
        q("boarding_call", 4, "Now Boarding",
                "\"Meridian Flight 417 to Port Ellery is now boarding by SmileScore.\" Scan your boarding pass at the B7 podium.")
                .obj(flag("scanned_pass", "gate_b7", "Scan your boarding pass at the B7 podium",
                        "Hold your boarding pass and right-click the podium beside Corinne."))
                .start(c -> {
                    c.pa("Meridian Flight 417 to Port Ellery is now boarding. We board by SmileScore. Group one: passengers rated four point five and above.");
                    c.director().startBoarding();
                })
                .tick((c, t) -> {
                    if (t == 400) c.pa("Group two: passengers rated three point five and above. Lower-rated passengers, please remain seated. We see you.");
                    if (t == 900) c.pa("All remaining passengers may now board. Meridian thanks you for your patience.");
                })
                .then("board");

        q("board", 4, "Jet Bridge",
                "Walk down the jet bridge and board the aircraft.")
                .obj(go("plane_door", "Board the plane", "Go through the opened gate door by the podium and follow the jet bridge."))
                .obj(talk("june", "June", "june_welcome", "Greet June, the flight attendant", "June is waiting just inside the aircraft door."))
                .then("find_seat");

        q("find_seat", 4, "14A",
                "Your seat is 14A: a window seat on the left side of the cabin. Stow your bag first.")
                .obj(flag("stowed_bag", "seat_14a", "Stow your carry-on in the overhead bin above row 14",
                        "Hold your bag and right-click the overhead bin above row 14. Rows are counted from the front."))
                .obj(flag("seated_14a", "seat_14a", "Sit down in seat 14A", "Right-click seat 14A, the window seat on the left of row 14."))
                .then("takeoff");

        q("takeoff", 4, "Doors to Automatic",
                "Stay in your seat while the crew prepares the cabin for departure.")
                .obj(flag("cs:takeoff", null, "Remain seated for take-off", "Stay seated. Sneak only if you want to be told off."))
                .start(c -> c.director().seatbelt(Locations.GATE_PLANE, true))
                .tick((c, t) -> {
                    StoryDirector d = c.director();
                    if (t < 500 && !d.isSeated(c.player())) {
                        d.hold();
                        if (t % 60 == 0) c.hint("June: \"Sir, madam, whoever you are. The seatbelt sign is on. Please return to 14A.\"");
                        return;
                    }
                    if (t == 20) c.pa("Cabin crew, arm doors and cross-check.");
                    if (t == 120) c.pa("Good morning, this is your captain, Captain Hale. Flying time to Port Ellery is six hours and ten minutes. Sit back and relax.");
                    if (t == 260) c.pa("In the unlikely event of a loss of cabin pressure, masks will drop from above. Secure your own mask before helping others. Smile.");
                    if (t == 400) c.pa("Cabin crew, seats for take-off.");
                    if (t == 470) d.fx(c.player(), "fade_out", 1, 30);
                    if (t == 500) d.transferToAirPlane(c.player());
                    if (t == 520) d.cutscene(c.player(), "takeoff");
                })
                .then("cruise");

        // =====================================================================================
        // Chapter 5 - Cruising altitude
        // =====================================================================================
        q("cruise", 5, "Cruising Altitude",
                "The seatbelt sign is off. Six hours to Port Ellery. Your seatmate hasn't looked up from her sketchbook since you sat down.")
                .obj(talk("ines", "Ines", "talked_ines", "Talk to your seatmate in 14B", "She's sitting right next to you."))
                .obj(flag("called_june", "air_seat_14a", "Press the call button", "The little white button on the wall beside seat 14A."))
                .obj(talk("june", "June", "got_meal", "Ask June for something to eat", "Press the call button first. June will come to your row."))
                .start(c -> {
                    c.director().seatbelt(Locations.AIR_PLANE, false);
                    c.pa("Ladies and gentlemen, the captain has switched off the seatbelt sign. You are free to move about the cabin. Your movements are rated.");
                })
                .resume(c -> c.director().airCabinTime())
                .then("strange");

        q("strange", 5, "Something Is Wrong",
                "Ines's sketchbook was full of an island. Of you, on that island. You need a minute to yourself.")
                .obj(flag("watched_screen", "air_seat_14a", "Watch the entertainment screen on the seat in front of 14A",
                        "Right-click the back of the seat in front of yours (seat 13A)."))
                .obj(flag("saw_mirror", "lavatory", "Freshen up in the rear lavatory", "Walk to the back of the cabin and look into the mirror (right-click it)."))
                .then("row20");

        q("row20", 5, "Row 20",
                "Someone in the very last row is knitting. You know that cardigan. Edith told you she wasn't flying today.")
                .obj(talkAt("edith", "Edith", "row20", "edith_plane", "Talk to Edith in row 20", "The last row, on the right side by the window."))
                .obj(flag("knocked_cockpit", "cockpit", "Knock on the cockpit door", "The grey door at the very front of the cabin, past the galley. Right-click it."))
                .then("lights_out");

        q("lights_out", 5, "Lights Out",
                "The note says nobody is flying this plane.")
                .obj(talk("june", "June", "june_stare", "Find June", "She's in the forward galley, at the front of the cabin."))
                .obj(flag("seated_air", "air_seat_14a", "Return to seat 14A", "Right-click seat 14A to sit down."))
                .tick((c, t) -> {
                    StoryDirector d = c.director();
                    if (t == 10) d.planeLights(Locations.AIR_PLANE, 2);
                    if (t == 60) d.planeLights(Locations.AIR_PLANE, 0);
                    if (t == 70) d.staring("air_plane", true);
                    if (t == 80) d.cutscene(c.player(), "stare");
                    if (t > 80 && c.has("cs:stare") && !c.has("stare_over")) {
                        c.flag("stare_over");
                        d.planeLights(Locations.AIR_PLANE, 1);
                        c.hint("The lights come back. Every passenger is still smiling at you.");
                    }
                    if (c.has("stare_over") && t % 20 == 0 && c.has("june_stare")) d.staring("air_plane", false);
                })
                .done(c -> c.director().staring("air_plane", false))
                .then("turbulence");

        // =====================================================================================
        // Chapter 6 - Turbulence
        // =====================================================================================
        q("turbulence", 6, "Turbulence",
                "The seatbelt sign chimes back on. Something is hitting the plane, hard, from the outside.")
                .obj(flag("cs:storm", null, "Stay in your seat", "Keep seated in 14A. Sneak only if you want to be thrown around the cabin."))
                .start(c -> c.director().seatbelt(Locations.AIR_PLANE, true))
                .resume(c -> {
                    c.director().weather(true);
                    c.director().fx(c.player(), "turbulence", c.has("turb3") ? 3 : c.has("turb2") ? 2 : 1, 0);
                })
                .tick((c, t) -> {
                    StoryDirector d = c.director();
                    boolean seated = d.isSeated(c.player());
                    if (t > 40 && !seated) {
                        d.shove(c.player(), c.has("turb3") ? 3 : c.has("turb2") ? 2 : 1);
                        if (t % 80 == 0) c.hint("You're thrown against the seats. Get back to 14A!");
                        if (t < 640) d.hold();
                        return;
                    }
                    if (t == 20) c.pa("Ladies and gentlemen, the captain has turned on the seatbelt sign. Please return to your seats. This is not a request.");
                    if (t == 40) { d.weather(true); d.fx(c.player(), "turbulence", 1, 0); c.flag("turb1"); }
                    if (t == 200) { d.fx(c.player(), "turbulence", 2, 0); c.flag("turb2"); d.planeLights(Locations.AIR_PLANE, 2); }
                    if (t == 260) d.openDialogue(c.player(), "marcus_panic", d.npc("marcus"));
                    if (t == 300) d.planeLights(Locations.AIR_PLANE, 1);
                    if (t == 420) { d.fx(c.player(), "turbulence", 3, 0); c.flag("turb3"); d.dropMasks(Locations.AIR_PLANE); }
                    if (t == 430) c.pa("Brace. Brace. Brace. Heads down. Stay down.");
                    if (t == 520) c.halo("Passenger " + c.player().getGameProfile().getName() + ". Your rating is under review. Please hold.");
                    if (t == 580) d.planeLights(Locations.AIR_PLANE, 0);
                    if (t > 200 && t < 640 && t % 37 == 0) d.lightningNear(c.player());
                    if (t == 640) { d.fx(c.player(), "turbulence", 0, 0); d.cutscene(c.player(), "storm"); }
                })
                .done(c -> c.director().crashToIsland(c.player()))
                .then("wake");

        // =====================================================================================
        // Chapter 7 - The Shore
        // =====================================================================================
        q("wake", 7, "Washed Ashore",
                "You are alive. The plane is in pieces along the beach and there is no one else here. No one at all.")
                .obj(go("crash", "Search the crash site for survivors", "The wreckage is spread along the southern beach, right around you."))
                .obj(count("searched_luggage", 3, "crash", "Search suitcases for supplies", "Right-click the suitcases scattered around the wreck."))
                .tick((c, t) -> {
                    if (!c.has("on_island") && !c.director().tryLand(c.player())) {
                        c.director().hold();
                        return;
                    }
                    if (t == 20) c.director().cutscene(c.player(), "shore");
                    if (t > 20 && c.has("cs:shore") && !c.has("wake_hint")) {
                        c.flag("wake_hint");
                        c.hint("You can break blocks again. Survive. Press [J] for your journal.");
                    }
                })
                .then("empty_seats");

        q("empty_seats", 7, "Empty Seats",
                "Every seat in the wreck is empty, and every seatbelt is still buckled. Down by the water, one seat sits apart from the rest, facing the sea.")
                .obj(flag("sat_lone_seat", "lone_seat", "Sit in the seat by the water", "West of the wreck, at the edge of the surf. Right-click it to sit."))
                .then("shelter");

        q("shelter", 7, "Make Camp",
                "It will be dark in a few hours. You need tools, a fire, and somewhere to put your back against.")
                .obj(items("tag:minecraft:logs", 8, null, "Gather logs", "Punch palm or jungle trees. Palms grow along the beach."))
                .obj(items("minecraft:crafting_table", 1, null, "Craft a crafting table", "Turn logs into planks, then four planks into a crafting table."))
                .obj(flag("has_campfire", null, "Make a campfire",
                        "Craft one from sticks, plant fiber and logs. Plant fiber comes from grass, ferns and palm leaves. Place it near your camp."))
                .then("first_night");

        q("first_night", 7, "The First Night",
                "Stay by your fire until sunrise. If you see someone standing out there in the dark, keep your eyes on them.")
                .obj(flag("dawn1", null, "Survive until morning", "There are no beds here. Stay near your fire and keep looking around."))
                .start(c -> c.director().nightFall())
                .tick((c, t) -> c.director().nightWatch(c, "dawn1", true))
                .then("smoke");

        // =====================================================================================
        // Chapter 8 - The Tidefolk
        // =====================================================================================
        q("smoke", 8, "Smoke in the East",
                "At dawn you see a column of smoke rising in the east. Someone else is alive on this island.")
                .obj(go("village", "Follow the smoke east", "Walk east along the coast from the crash site. Look for the tall column of smoke."))
                .obj(talk("nalu", "Nalu", "met_nalu", "Talk to the hunter who stopped you", "The hunter with the painted face, at the edge of the village."))
                .start(c -> c.director().spawnGroup("tribe"))
                .then("elder");

        q("elder", 8, "Elder Ahu",
                "Nalu has agreed not to put a spear in you, for now. The elder will decide what to do with you.")
                .obj(talk("ahu", "Elder Ahu", "met_ahu", "Speak with Elder Ahu", "He sits by the longhouse on the north side of the village."))
                .then("trials");

        q("trials", 8, "Three Trials",
                "Ahu will let you stay if you can give as well as take. Help Nalu, Mira and Teo.")
                .obj(talk("nalu", "Nalu", "trial_fish", "Bring 5 fish to Nalu",
                        "Fish with the rod Nalu gave you, in the sea off the village. Any kind of fish counts, cooked or raw."))
                .obj(talk("mira", "Mira", "trial_herbs", "Gather 6 bitterleaf for Mira",
                        "Bitterleaf is a broad dark plant that grows in the shade of the jungle. Mira's hut has the drying racks."))
                .obj(talk("teo", "Teo", "trial_wood", "Bring 12 palm planks to Teo",
                        "Craft planks from palm logs. Teo works at the dock on the east shore."))
                .then("welcome");

        q("welcome", 8, "One of Us",
                "The Tidefolk are watching you differently now. Not kindly, exactly. But not like prey.")
                .obj(talk("ahu", "Elder Ahu", "welcomed", "Return to Elder Ahu", "He's by the longhouse."))
                .obj(talk("pip", "Pip", "pip_drawing", "See what Pip wants to show you", "Pip is the child running around the bonfire."))
                .obj(talk("mira", "Mira", "mira_unrated", "Ask Mira about the faces in the dark", "She's at her hut, on the west side of the village."))
                .then("night_watch");

        // =====================================================================================
        // Chapter 9 - The Unrated
        // =====================================================================================
        q("night_watch", 9, "Night Watch",
                "On moonless nights the Unrated come to the village. Nalu has asked you to stand watch by the bonfire with him.")
                .obj(flag("dawn2", "village", "Defend the village until dawn",
                        "Stay near the bonfire. They only move when no one is looking. Flares and a machete help. So does not blinking."))
                .start(c -> c.director().nightFall())
                .tick((c, t) -> c.director().nightWatch(c, "dawn2", false))
                .then("missing_pip");

        q("missing_pip", 9, "Missing",
                "Pip is gone. Small footprints lead west, into the grove the Tidefolk will not enter.")
                .obj(talk("mira", "Mira", "mira_pip", "Talk to Mira", "She's at the bonfire, and she is not calm."))
                .obj(go("grove", "Find the Hollow Grove", "Go west across the island, past the spring. The trees there have no leaves."))
                .obj(talkAt("pip", "Pip", "grove", "found_pip", "Find Pip", "Somewhere in the grove. Listen for crying."))
                .obj(flag("pip_home", "village", "Bring Pip back to the village", "Pip will follow you. Walk back east to the bonfire."))
                .start(c -> c.director().pipToGrove())
                .tick((c, t) -> {
                    if (c.has("found_pip") && !c.has("pip_home")) {
                        StoryNpc pip = c.npc("pip");
                        Locations.Loc v = Locations.get("village");
                        if (pip != null && v != null && Locations.isNear(v, c.state(), pip.getX(), pip.getY(), pip.getZ())
                                && Locations.isNear(v, c.state(), c.player().getX(), c.player().getY(), c.player().getZ())) {
                            c.flag("pip_home");
                        }
                    }
                })
                .done(c -> c.director().pipHome())
                .then("the_eye");

        // =====================================================================================
        // Chapter 10 - The Eye
        // =====================================================================================
        q("the_eye", 10, "The Eye on the Mountain",
                "Ahu finally tells you about the Eye: a grey house on the summit that hums at night. Nobody who climbs up there comes back the same.")
                .obj(talk("ahu", "Elder Ahu", "ahu_eye", "Ask Elder Ahu about the Eye", "He's by the longhouse."))
                .obj(go("peak", "Climb to the summit", "The mountain is the highest point of the island, north of the spring."))
                .obj(flag("door_open", "peak", "Open the station door", "There's a keypad beside the door. You've seen the right numbers somewhere."))
                .then("the_lab");

        q("the_lab", 10, "Experience Lab 4",
                "Concrete, cold air, the smell of electronics. Someone built this place, and someone still pays its power bill.")
                .obj(go("lab", "Enter the station", "Walk through the open door."))
                .obj(flag("read_logs", "lab", "Read the logs on the terminal", "Right-click the glowing terminal in the middle of the room."))
                .obj(flag("halo_choice", "lab", "Decide what to do about HALO", "Talk to HALO through the terminal. The transmitter is in the back room."))
                .tick((c, t) -> {
                    if (c.has("at:lab") && !c.has("lab_cs")) {
                        c.flag("lab_cs");
                        c.director().cutscene(c.player(), "lab");
                    }
                })
                .then("boat_plan");

        // =====================================================================================
        // Chapter 11 - The Boat
        // =====================================================================================
        q("boat_plan", 11, "The Way Off",
                "You have a chart. Teo has a boat in his head. Time to put the two together.")
                .obj(talk("teo", "Teo", "boat_plan", "Show Teo the navigation chart", "Teo is at the dock on the east shore."))
                .then("boat_build");

        q("boat_build", 11, "Boatwright",
                "Bring materials to the boat frame at the end of Teo's dock, one stage at a time. Right-click the frame to hand over what you carry.")
                .obj(stage("boat_1", "Keel:", "Palm logs from the palms along every beach.",
                        v -> "palm logs (" + Math.min(16, v.counter("boat:palm_log")) + "/16)"))
                .obj(stage("boat_2", "Hull:", "Hull panels: break wreckage blocks at the crash site. Planks: craft from palm logs.",
                        v -> "hull panels (" + Math.min(8, v.counter("boat:hull_panel")) + "/8), palm planks (" + Math.min(24, v.counter("boat:palm_planks")) + "/24)"))
                .obj(stage("boat_3", "Mast & rigging:", "Rope: 3 plant fiber. Mast: 3 palm logs in a column with rope beside them.",
                        v -> "mast (" + Math.min(1, v.counter("boat:mast")) + "/1), rope (" + Math.min(8, v.counter("boat:rope")) + "/8)"))
                .obj(stage("boat_4", "Sail:", "Sail cloth: 4 airline blankets around 1 rope... blankets come from suitcases and from Mira.",
                        v -> "sail cloth (" + Math.min(1, v.counter("boat:sail_cloth")) + "/1)"))
                .obj(stage("boat_5", "Provisions:", "Fresh water from the spring (fill an empty flask). Any food counts.",
                        v -> "water flasks (" + Math.min(4, v.counter("boat:water_flask")) + "/4), food (" + Math.min(8, v.counter("boat:food")) + "/8)"))
                .then("farewell");

        // =====================================================================================
        // Chapter 12 - Last Call
        // =====================================================================================
        q("farewell", 12, "Goodbyes",
                "The boat floats. You can leave at first light. Say goodbye, and decide who, if anyone, comes with you.")
                .obj(talk("ahu", "Elder Ahu", "bye_ahu", "Say goodbye to Elder Ahu", "By the longhouse."))
                .obj(talk("mira", "Mira", "bye_mira", "Say goodbye to Mira", "At her hut."))
                .obj(talk("nalu", "Nalu", "bye_nalu", "Say goodbye to Nalu", "Near the bonfire."))
                .then("last_night");

        q("last_night", 12, "Last Call",
                "The Unrated are gathering on the beaches. All of them. Hold out at the dock until first light, then cast off.")
                .obj(flag("dawn3", "dock", "Survive the final night", "Stay near the dock and the boat. Keep moving, keep looking."))
                .obj(flag("cast_off", "dock", "Take the helm at dawn", "Board the boat and right-click the wheel at its stern."))
                .start(c -> c.director().nightFall())
                .tick((c, t) -> {
                    if (!c.has("dawn3")) c.director().nightWatch(c, "dawn3", false);
                })
                .then("ending");

        q("ending", 13, "Epilogue",
                "The island grows small behind you.")
                .obj(flag("ending_seen", null, "...", "There is nothing left to do but watch the water."))
                .tick((c, t) -> {
                    if (t == 10) c.director().cutscene(c.player(), "escape");
                    if (t > 10 && c.has("cs:escape") && !c.has("ending_started")) {
                        c.flag("ending_started");
                        c.director().ending(c.player());
                    }
                })
                .then("the_end");

        q("the_end", 13, "The End",
                "Thank you for flying Meridian. You can keep exploring this world for as long as you like. "
                        + "Start a new story from the title screen to see what the other choices lead to.")
                .then(null);
    }
}
