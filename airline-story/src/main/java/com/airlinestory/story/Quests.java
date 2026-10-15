package com.airlinestory.story;

import com.airlinestory.entity.StoryNpc;
import com.airlinestory.registry.ModItems;
import com.airlinestory.story.Locations.Plane;
import com.airlinestory.world.IslandBuilder;
import com.airlinestory.world.PlaneBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.airlinestory.story.Objective.*;

/** The whole story, in order, plus the side quests people hand out along the way. */
public final class Quests {
    private Quests() {}

    public static final String FIRST = "arrive";
    public static final String[] CHAPTERS = {
            "", "Departures", "Security", "Airside", "Staff Only", "Gate B7", "Boarding", "Cruising Altitude", "Under the Floor",
            "Turbulence", "The Shore", "Higher Ground", "The Tidefolk", "The Unrated", "Echoes", "The Eye", "The Boat", "Last Call", "Epilogue"
    };

    private static final Map<String, Quest> QUESTS = new LinkedHashMap<>();
    private static final List<Quest> SIDES = new ArrayList<>();

    @Nullable
    public static Quest get(String id) {
        return QUESTS.get(id);
    }

    public static Collection<Quest> all() {
        return QUESTS.values();
    }

    public static List<Quest> sides() {
        return SIDES;
    }

    private static Quest q(String id, int chapter, String title, String desc) {
        Quest q = new Quest(id, chapter, title, desc);
        QUESTS.put(id, q);
        return q;
    }

    private static Quest side(String id, int chapter, String title, String desc) {
        Quest q = q(id, chapter, title, desc);
        q.side = true;
        SIDES.add(q);
        return q;
    }

    private static BlockPos b(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    private static boolean onPlane(FlagView v) {
        return v.has("on_air_plane") || v.has("on_island");
    }

    static {
        Plane g = Locations.GATE_PLANE;
        Plane a = Locations.AIR_PLANE;

        // =====================================================================================
        // Chapter 1 - Departures
        // =====================================================================================
        q("arrive", 1, "Welcome to Meridian",
                "The taxi has left you at the kerb of Meridian International. Flight MA417 to Port Ellery leaves from Gate B7 at 9:10. "
                        + "Everyone says Meridian is the friendliest airline in the world. Everyone has to say that.")
                .obj(go("terminal", "Walk into the departures hall", "The revolving glass doors are straight ahead of you, under the DEPARTURES sign."))
                .start(c -> {
                    StoryDirector d = c.director();
                    d.gamemode(c.player(), GameType.ADVENTURE);
                    c.give(ModItems.TRAVEL_JOURNAL.get(), 1);
                    d.giveOwned(c.player(), ModItems.PASSPORT.get());
                    c.give(ModItems.CARRY_ON_BAG.get(), 1);
                    c.give(ModItems.SMARTPHONE.get(), 1);
                })
                .resume(c -> c.director().gamemode(c.player(), GameType.ADVENTURE))
                .tick((c, t) -> {
                    if (t == 40) c.director().cutscene(c.player(), "intro");
                    if (t > 40 && c.has("cs:intro") && !c.has("intro_pa")) {
                        c.flag("intro_pa");
                        c.pa("Good morning and welcome to Meridian International. Please keep your belongings with you at all times. Courtesy is rated.");
                        c.hint("Follow the yellow marker. Press [J] for your Travel Journal at any time.");
                    }
                })
                .then("checkin");

        q("checkin", 1, "Check In",
                "Meridian passengers check in at the long blue desk in the middle of the hall. The queue is short. The agent's smile is very wide. "
                        + "At Meridian, you rate every interaction, and every interaction rates you back.")
                .obj(talk("dana", "Dana", "checked_in", "Check in with Dana at the Meridian desk",
                        "Dana stands behind the blue MERIDIAN AIR counter, past the departures board. Walk up to the counter and right-click her."))
                .obj(flag("rated_dana", "checkin", "Rate your check-in experience", "When the conversation ends, pick one to five stars. Be honest. Or don't."))
                .tick((c, t) -> {
                    if (t == 400 && !c.has("checked_in")) c.pa("Meridian check-in is now open for Flight 417 to Port Ellery. Desk 2. Dana is waiting. Dana is always waiting.");
                })
                .then("landside");

        q("landside", 1, "Time to Kill",
                "You have your boarding pass and an hour to spare. Check your gate on the departures board. The old woman knitting on the west benches "
                        + "has been watching you since you walked in, and the boy sitting near her keeps waving.")
                .obj(flag("read_board", "board", "Check your gate on the departures board", "The black wall of flip-signs in the middle of the hall. Right-click its centre panel.").at(b(26, 64, 15)))
                .obj(talk("edith", "Edith", "talked_edith", "Talk to the old woman knitting on the benches", "The west benches, by the big windows on the left side of the hall."))
                .obj(talk("priya", "Priya", "talked_priya", "Say hello to the mother and her son", "Priya and Leo are sitting on the same benches as Edith."))
                .tick((c, t) -> {
                    if (t == 300) c.pa("Would the owner of a small grey rabbit please... no. Never mind. It knows where it is.");
                    if (t == 1600) c.pa("Passengers on Flight 417, security is now open. Please don't keep it waiting.");
                })
                .done(c -> {
                    c.director().openBarrier("to_security");
                    c.pa("Passengers travelling on Meridian Flight 417 to Port Ellery, please make your way through security. Thank you for smiling.");
                })
                .then("security");

        side("floppy", 1, "Captain Floppy",
                "Leo has lost his rabbit, Captain Floppy. He says Captain Floppy 'went to look at the floor' near the benches. Leo is six, and very sure about this.")
                .giver("leo", v -> !onPlane(v) && v.has("checked_in"))
                .obj(flag("has_plush", "benches", "Find Captain Floppy", "Look around the west benches. Rabbits like to hide under things.").at(b(11, 64, 47)))
                .obj(talk("leo", "Leo", "floppy_returned", "Give Captain Floppy back to Leo", "Leo is with his mother."))
                .then(null);

        side("wet_floor", 1, "Slippery",
                "Gus the janitor has lost his WET FLOOR sign, and his supervisor rates him by the sign. He thinks he left it in his closet, but he's not allowed to leave the mop.")
                .giver("gus", v -> !onPlane(v) && v.has("checked_in"))
                .obj(go("closet", "Go into the janitor's closet", "The white room with a STAFF ONLY sign, south-east corner of the hall, near the security wall."))
                .obj(flag("got_sign", "closet", "Pick up the WET FLOOR sign", "It's at the back of the closet. Right-click it.").at(b(48, 64, 52)))
                .obj(flag("clue:closet_camera", "closet", "Look at the hole in the back wall", "Something is glinting in the plywood behind the buckets.").at(b(45, 64, 52)).optional())
                .obj(talk("gus", "Gus", "sign_returned", "Bring the sign back to Gus", "Gus is mopping somewhere between the closet and the check-in islands."))
                .then(null);

        // =====================================================================================
        // Chapter 2 - Security
        // =====================================================================================
        q("security", 2, "Security",
                "The glass doors to security have slid open, on the east side of the hall. Officer Reyes is running the only open lane.")
                .obj(talk("reyes", "Officer Reyes", "reyes_intro", "Speak to Officer Reyes at lane 2",
                        "Walk east through the opened doors, follow the queue, and talk to Reyes at the start of the middle lane."))
                .start(c -> c.director().spawnGroup("security"))
                .then("screening");

        q("screening", 2, "Screening",
                "Put your carry-on on the X-ray belt, then walk through the body scanner like everybody else. Don't stop in the arch.")
                .obj(flag("bag_on_belt", "security", "Put your carry-on bag on the X-ray belt",
                        "Hold the bag in your hand and right-click the conveyor belt of lane 2 (or the X-ray machine).").at(b(63, 64, 24)))
                .obj(go("scanner", "Walk through the body scanner", "The grey arch halfway down lane 2. Walk straight through it.").at(Locations.SCANNER_WALK))
                .done(c -> {
                    c.director().sound(c.player(), Snd.of(SoundEvents.NOTE_BLOCK_BIT), 1f, 0.5f);
                    c.director().schedule(4, () -> c.director().sound(c.player(), Snd.of(SoundEvents.NOTE_BLOCK_BIT), 1f, 0.5f));
                    c.hint("*BEEP* *BEEP* The arch flashes red. The X-ray operator stops the belt and leans in to look at her screen.");
                    c.director().reyesToRoom();
                })
                .then("flagged");

        q("flagged", 2, "Random Selection",
                "\"Random selection,\" says Officer Reyes, although nothing about the way he said it was random. He wants a word in private.")
                .obj(go("interview", "Follow Reyes into Secondary Screening", "The white room south of the lanes. The door is open.").at(b(68, 64, 44)))
                .obj(talk("reyes", "Officer Reyes", "reyes_cleared", "Answer Officer Reyes's questions", "Reyes is waiting in the room."))
                .done(c -> {
                    c.director().openBarrier("to_airside");
                    c.director().reyesHome();
                    c.pa("Passenger " + c.player().getGameProfile().getName() + ", thank you for your patience at security. Your patience has been rated.");
                })
                .then("coffee");

        // =====================================================================================
        // Chapter 3 - Airside
        // =====================================================================================
        q("coffee", 3, "Cloud Nine",
                "You're airside. The concourse smells of cinnamon and floor polish. There is time for a coffee before you go anywhere near your gate.")
                .obj(talk("tomas", "Tomas", "got_coffee", "Order a coffee from Tomas at Cloud Nine",
                        "The wooden counter just past security, on the north side of the plaza, under the CLOUD NINE sign."))
                .start(c -> c.director().spawnGroup("airside"))
                .then("faces");

        q("faces", 3, "Familiar Faces",
                "Tomas wrote something on your cup instead of your name: CHECK THE PILLAR BY THE LOUNGE. Then he pretended he hadn't.")
                .obj(flag("clue:missing_poster", "poster", "Look at the poster on the pillar by the lounge", "A white pillar beside the blue glass of the Meridian Prime Lounge. Right-click the poster.").at(Locations.POSTER))
                .obj(flag("watched_tv", "lounge", "Watch the news in the Prime Lounge", "Inside the lounge, on the back wall. Right-click the TV.").at(Locations.LOUNGE_TV))
                .obj(talk("halvorsen", "Mr. Halvorsen", "talked_halvorsen", "Talk to the man in the grey suit in the lounge", "He's sitting in the lounge, staring at his phone."))
                .obj(talk("tomas", "Tomas", "tomas_door", "Ask Tomas what is going on", "Back at Cloud Nine."))
                .then("backstage");

        // =====================================================================================
        // Chapter 4 - Staff Only
        // =====================================================================================
        q("backstage", 4, "Staff Only",
                "There's a door in the north wall of the plaza marked STAFF ONLY. Tomas says nobody who works here has ever been through it, and he's worked here for years. "
                        + "Stagehands patrol the other side. Keep out of their sight: sneak to stay quiet, and keep something between you and them.")
                .obj(flag("staff_door_open", "staff_door", "Open the staff door", "The keypad beside the door marked STAFF ONLY, north wall of the plaza. Tomas gave you the code.").at(Locations.STAFF_KEYPAD))
                .obj(go("backstage", "Slip through the door", "Through the staff door, then north."))
                .obj(flag("clue:call_sheet", "office", "Search the production office", "A small plywood office in the north-west corner. The paper on the desk.").at(b(104, 65, -31)))
                .obj(flag("clue:control_log", "control", "Find the control room log", "The room marked CONTROL - ON AIR in the north-east. A clipboard on the desk.").at(b(124, 66, -31)))
                .obj(flag("clue:crew_memo", "control", "Read the memo to cabin crew", "Also in the control room, at the far end of the desk.").at(b(136, 66, -31)).optional())
                .obj(flag("clue:lost_found", "backstage", "Check the lost property box", "Somewhere near the costume racks.").at(b(110, 64, -26)).optional())
                .start(c -> c.director().spawnGroup("backstage"))
                .tick((c, t) -> c.director().backstageWatch(c.player()))
                .done(c -> c.pa("Passenger " + c.player().getGameProfile().getName() + ", Meridian has noticed you are not at your gate. Please proceed to Gate B7. Thank you for your curiosity. It has been rated."))
                .then("find_gate");

        // =====================================================================================
        // Chapter 5 - Gate B7
        // =====================================================================================
        q("find_gate", 5, "Gate B7",
                "Gate B7 is at the far east end of the concourse, by the windows. Flight 417 is already at the jet bridge. Check in with the gate agent.")
                .obj(go("gate_b7", "Walk to Gate B7", "Follow the concourse east, past gates B5 and B6. The moving walkway helps."))
                .obj(talk("corinne", "Corinne", "corinne_intro", "Speak to Corinne at the B7 podium", "She stands behind the desk beside the B7 boarding podium."))
                .start(c -> {
                    c.director().spawnGroup("b7");
                    c.director().moveToGate();
                })
                .then("wait_gate");

        q("wait_gate", 5, "Fellow Passengers",
                "Boarding starts soon. The people waiting at B7 will be on your flight. You could say hello. "
                        + "You could also notice how often they check their ratings, and how little they look out of the window.")
                .obj(talk("marcus", "Marcus", "talked_marcus", "Talk to the nervous man in the hoodie", "He's pacing by the B7 windows."))
                .obj(talk("ines", "Ines", "talked_ines_gate", "Talk to the woman with the sketchbook", "She's sitting in the back row of the B7 seats."))
                .obj(talk("priya", "Priya", "talked_priya_gate", "Check on Priya and Leo", "They've found seats at B7."))
                .obj(flag("waited_gate", "gate_b7", "Take a seat at Gate B7 and wait for boarding",
                        "Right-click any seat at Gate B7. Sneak (Shift) to stand up again."))
                .done(c -> c.director().boards(1))
                .then("boarding_call");

        q("boarding_call", 5, "Now Boarding",
                "\"Meridian Flight 417 to Port Ellery is now boarding by SmileScore.\" Every departures board in the building now says ARRIVED. Scan your boarding pass at the B7 podium.")
                .obj(flag("scanned_pass", "podium", "Scan your boarding pass at the B7 podium",
                        "Hold your boarding pass and right-click the podium beside Corinne's desk.").at(Locations.B7_PODIUM))
                .start(c -> {
                    c.pa("Meridian Flight 417 to Port Ellery is now boarding. We board by SmileScore. Group one: passengers rated four point five and above.");
                    c.director().startBoarding();
                })
                .tick((c, t) -> {
                    if (t == 400) c.pa("Group two: passengers rated three point five and above. Lower-rated passengers, please remain seated. We see you.");
                    if (t == 900) c.pa("All remaining passengers may now board. Meridian thanks you for your patience.");
                })
                .then("board");

        // =====================================================================================
        // Chapter 6 - Boarding
        // =====================================================================================
        q("board", 6, "Jet Bridge",
                "Walk down the jet bridge and board the aircraft. It's warm in the bridge, and it smells of the sea.")
                .obj(go("plane_door", "Board the plane", "Through the opened gate door behind the podium, then up the jet bridge.").at(g.door()))
                .obj(talk("june", "June", "june_welcome", "Greet June, the flight attendant", "June is waiting just inside the aircraft door."))
                .then("find_seat");

        q("find_seat", 6, "14A",
                "Your seat is 14A: a window seat on the left side of the cabin. Rows are counted from the front. Stow your bag first.")
                .obj(flag("stowed_bag", "seat_14a", "Stow your carry-on in the overhead bin above row 14",
                        "Hold your bag and right-click the overhead bin above seat 14A.").at(g.seat(14, 'A').above(2)))
                .obj(flag("seated_14a", "seat_14a", "Sit down in seat 14A", "Right-click seat 14A, the window seat on the left of row 14.").at(g.seat(14, 'A')))
                .then("takeoff");

        q("takeoff", 6, "Doors to Automatic",
                "Stay in your seat while the crew prepares the cabin for departure.")
                .obj(flag("cs:takeoff", null, "Remain seated for take-off", "Stay seated. Sneaking stands you up, and June will notice."))
                .start(c -> c.director().seatbelt(Locations.GATE_PLANE, true))
                .tick((c, t) -> {
                    StoryDirector d = c.director();
                    if (t < 500 && !d.isSeated(c.player())) {
                        d.hold();
                        if (t % 80 == 0) c.hint("June: \"The seatbelt sign is on. Please return to 14A.\"");
                        return;
                    }
                    if (t == 20) c.pa("Cabin crew, arm doors and cross-check.");
                    if (t == 120) c.pa("Good morning, this is your captain. Flying time to Port Ellery is six hours and ten minutes. Sit back and relax. Relaxation is rated.");
                    if (t == 260) c.pa("In the unlikely event of a loss of cabin pressure, masks will drop from above. Secure your own mask before helping others. Smile.");
                    if (t == 400) c.pa("Cabin crew, seats for take-off.");
                    if (t == 470) d.fx(c.player(), "fade_out", 1, 30);
                    if (t == 500) d.transferToAirPlane(c.player());
                    if (t == 520) d.cutscene(c.player(), "takeoff");
                })
                .then("cruise");

        // =====================================================================================
        // Chapter 7 - Cruising altitude
        // =====================================================================================
        q("cruise", 7, "Cruising Altitude",
                "The seatbelt sign is off. Six hours to Port Ellery. Ines, in 14B, hasn't looked up from her sketchbook since you sat down. You're hungry.")
                .obj(talk("ines", "Ines", "talked_ines", "Talk to Ines in 14B", "She's sitting right next to you."))
                .obj(flag("called_june", "air_seat_14a", "Press the call button", "The little white button on the wall beside the window of 14A.").at(PlaneBuilder.callButton(a)))
                .obj(talk("june", "June", "got_meal", "Ask June for something to eat", "Press the call button first. June will come to your row."))
                .obj(talk("leo", "Leo", "leo_plane", "Check on Leo in row 12", "Two rows ahead of you, on the right side.").optional())
                .start(c -> {
                    c.director().seatbelt(Locations.AIR_PLANE, false);
                    c.pa("Ladies and gentlemen, the captain has switched off the seatbelt sign. You are free to move about the cabin. Your movements are rated.");
                })
                .resume(c -> c.director().airCabinTime())
                .then("strange");

        q("strange", 7, "Something Is Wrong",
                "Ines's sketchbook was full of an island. Of you, on that island. You need a minute to yourself.")
                .obj(flag("watched_screen", "air_seat_14a", "Watch the entertainment screen in front of 14A",
                        "Right-click the back of seat 13A, in front of yours.").at(a.seat(13, 'A')))
                .obj(flag("saw_mirror", "lavatory", "Freshen up in the rear lavatory", "Walk to the back of the cabin, open the birch door on the left, and look into the mirror (right-click it).").at(a.local(54, 2, -3)))
                .then("row20");

        q("row20", 7, "Row 20",
                "Someone in the very last row is knitting. You know that cardigan. Edith told you she wasn't flying today.")
                .obj(talkAt("edith", "Edith", "row20", "edith_plane", "Talk to Edith in row 20", "The last row, on the right side by the window."))
                .obj(flag("knocked_cockpit", "cockpit", "Knock on the cockpit door", "The grey door at the very front of the cabin, past the galley. Right-click it.").at(a.cockpitDoor()))
                .then("the_hold");

        // =====================================================================================
        // Chapter 8 - Under the Floor
        // =====================================================================================
        q("the_hold", 8, "Under the Floor",
                "The note from the cockpit says: NOBODY IS FLYING THIS PLANE. LOOK UNDER THE FLOOR. REAR GALLEY HATCH. Theo, the attendant in the rear galley, is standing right next to it.")
                .obj(talk("theo", "Theo", "theo_distracted", "Get Theo away from the rear galley", "Talk to Theo. Give him a reason to walk to the front of the plane."))
                .obj(go("hold", "Climb down into the cargo hold", "The wooden hatch in the floor of the rear galley, by the CREW ONLY sign. Open it and climb down the ladder.").at(PlaneBuilder.hatch(a)))
                .obj(flag("clue:seat_14a_dummy", "hold", "Find out what's strapped into the seat", "Near the front of the hold, between the suitcases.").at(a.local(13, -3, 1)))
                .obj(flag("clue:cargo_manifest", "hold", "Read the cargo manifest", "Somewhere among the crates at the back of the hold.").at(a.local(47, -3, 0)))
                .obj(flag("clue:june_badge", "galley", "Search the forward galley", "Behind the coffee machine at the front of the cabin.").at(a.local(8, 2, -3)).optional())
                .then("lights_out");

        q("lights_out", 8, "Lights Out",
                "A mannequin with your face. Two hundred suitcases with your name. A weather rig. Zero pilots. You need to talk to June, and then you need to sit down.")
                .obj(talk("june", "June", "june_stare", "Confront June", "She's in the forward galley, at the very front of the cabin."))
                .obj(flag("seated_air", "air_seat_14a", "Return to seat 14A", "Right-click seat 14A to sit down.").at(a.seat(14, 'A')))
                .obj(flag("stare_over", null, "Don't look away", "Stay in your seat. Whatever happens, stay in your seat."))
                .start(c -> c.director().theoHome())
                .tick((c, t) -> {
                    StoryDirector d = c.director();
                    if (!c.has("seated_air")) { if (t > 100) d.hold(); return; }
                    if (!c.has("lights_t0")) { c.flag("lights_t0"); c.state().setCounter("lights_at", t); }
                    int k = t - c.counter("lights_at");
                    if (k == 10) d.planeLights(a, 2);
                    if (k == 60) d.planeLights(a, 0);
                    if (k == 70) d.staring("air_plane", true);
                    if (k == 80) d.cutscene(c.player(), "stare");
                    if (k > 80 && c.has("cs:stare") && !c.has("stare_over")) {
                        c.flag("stare_over");
                        d.planeLights(a, 1);
                        c.hint("The lights come back. Every passenger is still turned towards you, smiling. Then, one by one, they face the front again.");
                    }
                    if (c.has("stare_over") && k % 40 == 0) d.staring("air_plane", false);
                })
                .done(c -> c.director().staring("air_plane", false))
                .then("turbulence");

        // =====================================================================================
        // Chapter 9 - Turbulence
        // =====================================================================================
        q("turbulence", 9, "Turbulence",
                "The seatbelt sign chimes back on. Something is hitting the plane, hard, from the outside.")
                .obj(flag("cs:storm", null, "Stay in your seat", "Keep seated in 14A. If you get up, you will be thrown around the cabin."))
                .start(c -> c.director().seatbelt(a, true))
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
                    if (t == 200) { d.fx(c.player(), "turbulence", 2, 0); c.flag("turb2"); d.planeLights(a, 2); }
                    if (t == 260) d.openDialogue(c.player(), "marcus_panic", d.npc("marcus"));
                    if (t == 300) d.planeLights(a, 1);
                    if (t == 420) { d.fx(c.player(), "turbulence", 3, 0); c.flag("turb3"); d.dropMasks(a); }
                    if (t == 430) c.pa("Brace. Brace. Brace. Heads down. Stay down.");
                    if (t == 520) c.halo("Passenger " + c.player().getGameProfile().getName() + ". Your rating is under review. Please hold.");
                    if (t == 580) d.planeLights(a, 0);
                    if (t > 200 && t < 640 && t % 37 == 0) d.lightningNear(c.player());
                    if (t == 640) { d.fx(c.player(), "turbulence", 0, 0); d.cutscene(c.player(), "storm"); }
                })
                .done(c -> c.director().crashToIsland(c.player()))
                .then("wake");

        // =====================================================================================
        // Chapter 10 - The Shore
        // =====================================================================================
        q("wake", 10, "Washed Ashore",
                "You are alive. The plane is in pieces along the beach, and there is no one else here. No bodies. No one at all.")
                .obj(go("crash", "Search the crash site for survivors", "The wreckage is spread along the beach around you: the fuselage, the nose in the trees, the tail in the surf."))
                .obj(count("searched_luggage", 4, "crash", "Search suitcases for supplies", "Right-click the suitcases scattered around the wreck."))
                .tick((c, t) -> {
                    if (!c.has("on_island") && !c.director().tryLand(c.player())) {
                        c.director().hold();
                        return;
                    }
                    if (t == 20) c.director().cutscene(c.player(), "shore");
                    if (t > 20 && c.has("cs:shore") && !c.has("wake_hint")) {
                        c.flag("wake_hint");
                        c.hint("You can break and place blocks again. Survive. Press [J] for your journal.");
                    }
                })
                .then("empty_seats");

        q("empty_seats", 10, "Empty Seats",
                "Every seat in the wreck is empty, and every seatbelt is still buckled. Down by the water, one seat sits apart from the rest, facing the sea.")
                .obj(flag("sat_lone_seat", "lone_seat", "Sit in the seat by the water", "West of the wreck, at the edge of the surf. Right-click it to sit."))
                .then("shelter");

        q("shelter", 10, "Make Camp",
                "It will be dark in a few hours. You need tools, a fire, and somewhere to put your back against.")
                .obj(items("tag:minecraft:logs", 8, null, "Gather logs", "Punch the trunks of palm or jungle trees. Palms grow all along the beach."))
                .obj(items("minecraft:crafting_table", 1, null, "Craft a crafting table", "Turn logs into planks, then four planks into a crafting table."))
                .obj(flag("has_campfire", null, "Make a campfire",
                        "Craft one from sticks, plant fiber and logs. Plant fiber comes from grass, ferns and palm leaves. Place it near your camp."))
                .obj(items("minecraft:wooden_pickaxe", 1, null, "Craft a pickaxe", "Sticks and planks on the crafting table.").optional())
                .then("first_night");

        q("first_night", 10, "The First Night",
                "Stay by your fire until sunrise. If you see someone standing out there in the dark, keep your eyes on them.")
                .obj(flag("dawn1", null, "Survive until morning", "There are no beds here. Stay near your fire and keep looking around."))
                .start(c -> c.director().nightFall())
                .tick((c, t) -> c.director().nightWatch(c, "dawn1", true))
                .then("water");

        // =====================================================================================
        // Chapter 11 - Higher Ground
        // =====================================================================================
        q("water", 11, "Fresh Water",
                "Your mouth tastes of salt and your head is pounding. Seawater will only make it worse. A river runs down out of the hills to the north-west of the wreck: follow it upstream.")
                .obj(go("falls", "Follow the river up to the falls", "Walk north-west from the crash site until you hit the river, then follow it uphill."))
                .obj(items("airlinestory:water_flask", 1, "falls", "Fill a flask with fresh water", "Use an empty flask on the river or the pool (you found one in a suitcase). Drink it, or keep it."))
                .obj(items("airlinestory:bitterleaf", 1, null, "Find some bitterleaf in the jungle shade", "A broad, dark-leaved plant growing under the thickest trees.").optional())
                .then("lookout");

        q("lookout", 11, "The Radio Tower",
                "From the falls you can see a rusted tower on the western ridge, its red light still blinking. Someone built that. Someone might still be listening.")
                .obj(go("lookout", "Reach the radio tower", "West of the falls, on the high ridge. Look for the lattice tower."))
                .obj(flag("lookout_top", "lookout", "Climb to the platform at the top", "The ladder on the south side of the tower."))
                .obj(flag("clue:tape_2", "lookout", "Search the platform", "There's something on the platform floor."))
                .tick((c, t) -> {
                    if (!c.has("lookout_top") && t % 10 == 0) {
                        BlockPos top = IslandBuilder.lookoutTop();
                        if (c.player().getY() >= top.getY() && c.player().blockPosition().distSqr(top) < 6 * 6) {
                            c.flag("lookout_top");
                            c.give(ModItems.ISLAND_MAP.get(), 1);
                            c.hint("Pinned to the railing with a fishhook: a hand-drawn map of the island. Far to the east, a thin column of smoke rises out of the trees.");
                        }
                    }
                })
                .then("smoke");

        q("smoke", 11, "Smoke in the East",
                "From the tower you saw a column of smoke rising on the east coast. Someone else is alive on this island.")
                .obj(go("village", "Follow the smoke east", "Head east across the island, past the river. The map shows huts on the east coast."))
                .obj(talk("nalu", "Nalu", "met_nalu", "Talk to the hunter who stopped you", "The hunter with the painted face, at the edge of the village."))
                .start(c -> c.director().spawnGroup("tribe"))
                .then("elder");

        // =====================================================================================
        // Chapter 12 - The Tidefolk
        // =====================================================================================
        q("elder", 12, "Elder Ahu",
                "Nalu has agreed not to put a spear in you, for now. The elder will decide what to do with you.")
                .obj(talk("ahu", "Elder Ahu", "met_ahu", "Speak with Elder Ahu in the longhouse", "The long thatched hall on the north side of the village. Ahu sits at the back."))
                .then("trials");

        q("trials", 12, "Three Trials",
                "Ahu will let you stay if you can give as well as take. Help Nalu, Mira and Teo. Each of them will tell you what they need.")
                .obj(talk("nalu", "Nalu", "trial_fish", "Bring 5 fish to Nalu",
                        "Fish with the rod Nalu gives you, in the sea off the village beach. Any kind of fish counts, cooked or raw."))
                .obj(talk("mira", "Mira", "trial_herbs", "Gather 6 bitterleaf for Mira",
                        "Bitterleaf is a broad dark plant that grows in the shade of the jungle. Mira works by the garden on the west side of the plaza."))
                .obj(talk("teo", "Teo", "trial_wood", "Bring 12 palm planks to Teo",
                        "Craft planks from palm logs. Teo works at the dock on the east shore."))
                .then("welcome");

        q("welcome", 12, "One of Us",
                "The Tidefolk are watching you differently now. Not kindly, exactly. But not like prey.")
                .obj(talk("ahu", "Elder Ahu", "welcomed", "Return to Elder Ahu", "In the longhouse."))
                .obj(talk("pip", "Pip", "pip_drawing", "See what Pip wants to show you", "Pip is the child running around the bonfire."))
                .obj(talk("mira", "Mira", "mira_unrated", "Ask Mira about the faces in the dark", "By her garden on the west side of the plaza."))
                .then("night_watch");

        side("hunt", 12, "The Hunt",
                "Nalu says the village needs meat before the storms, and that you need to learn which end of a spear goes in the pig. Wild pigs roam the jungle and the grassland.")
                .giver("nalu", v -> v.has("welcomed") && !v.has("bye_nalu"))
                .obj(count("boars", 3, null, "Hunt 3 wild pigs", "Pigs root around in the jungle and the meadows inland. Any weapon will do."))
                .obj(talk("nalu", "Nalu", "hunt_done", "Tell Nalu about the hunt", "Nalu, at the edge of the village."))
                .then(null);

        side("remedy", 12, "Mira's Remedy",
                "Mira's ghost tonic needs ghost orchids, which only grow in the west, where the Tidefolk will not go. She'd be grateful for a few, and she won't ask how you got them.")
                .giver("mira", v -> v.has("mira_unrated") && !v.has("bye_mira"))
                .obj(items("airlinestory:ghost_orchid", 3, "grove", "Pick 3 ghost orchids", "Pale glowing flowers. They grow around the Hollow Grove in the far west."))
                .obj(talk("mira", "Mira", "remedy_done", "Bring the orchids to Mira", "By her garden."))
                .then(null);

        // =====================================================================================
        // Chapter 13 - The Unrated
        // =====================================================================================
        q("night_watch", 13, "Night Watch",
                "On moonless nights the Unrated come to the village. Nalu has asked you to stand watch by the bonfire with him.")
                .obj(talk("nalu", "Nalu", "watch_ready", "Tell Nalu you're ready to stand watch", "Nalu, at the edge of the village."))
                .obj(flag("dawn2", "village", "Defend the village until dawn",
                        "Stay near the bonfire. They only move when no one is looking. Flares and a blade help. So does not blinking."))
                .tick((c, t) -> {
                    if (!c.has("watch_ready")) { c.director().hold(); return; }
                    if (!c.has("watch_dusk")) { c.flag("watch_dusk"); c.director().nightFall(); }
                    c.director().nightWatch(c, "dawn2", false);
                })
                .then("missing_pip");

        q("missing_pip", 13, "Missing",
                "Pip is gone. Small footprints lead west, into the dead grove the Tidefolk will not enter.")
                .obj(talk("mira", "Mira", "mira_pip", "Talk to Mira", "She's by her garden, and she is not calm."))
                .obj(go("grove", "Find the Hollow Grove", "Far to the west, past the river. The trees there have no leaves. Your map shows it."))
                .obj(talkAt("pip", "Pip", "grove", "found_pip", "Find Pip", "Somewhere in the grove, in front of the wall of photographs."))
                .obj(flag("clue:hollow_note", "grove", "Read what's scratched on the seats", "One of the seats in the grove has writing on its back.").optional())
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
                .then("echoes");

        // =====================================================================================
        // Chapter 14 - Echoes
        // =====================================================================================
        q("echoes", 14, "The Ones Before",
                "Ahu finally admits it: you are not the first to wash up here. There was a pilot who lived in the western cave. A plane that sank in the lagoon. "
                        + "A family who lived in the mangroves, until the fire. Find out what happened to them.")
                .obj(talk("ahu", "Elder Ahu", "ahu_echoes", "Hear Ahu out", "In the longhouse."))
                .obj(flag("clue:tape_1", "cave_camp", "The pilot's cave: find what he left behind", "A cave in the western cliffs, past the radio tower. Follow the tunnel to the old camp."))
                .obj(flag("got_coil", "sunken", "The sunken plane: salvage the engine coil", "In the lagoon south of the village. Dive down to the wreck. The coil glows faintly."))
                .obj(flag("clue:bell_letter", "burned", "The burned village: search the ruins", "North of the village, in the mangroves. Look in the barrel by the westernmost hut."))
                .obj(flag("clue:tape_3", "cave_camp", "Find the pilot's last tape", "Also in the cave camp.").optional())
                .obj(flag("clue:okafor_diary", "sunken", "Find the diary in the sunken cabin", "Inside the sunken fuselage.").optional())
                .obj(flag("clue:burned_photo", "burned", "Find the photograph in the ashes", "In the ruins of the easternmost hut.").optional())
                .then("what_we_were");

        q("what_we_were", 14, "What We Were",
                "A captain's voice on a tape. A cabin attendant's diary. A letter from a brother. The people of this village have forgotten who they were. You haven't.")
                .obj(talk("nalu", "Nalu", "nalu_truth", "Show Nalu the letter", "At the edge of the village."))
                .obj(talk("mira", "Mira", "mira_truth", "Tell Mira about the diary", "By her garden."))
                .obj(talk("ahu", "Elder Ahu", "ahu_eye", "Ask Ahu about the Eye on the mountain", "In the longhouse."))
                .then("the_eye");

        // =====================================================================================
        // Chapter 15 - The Eye
        // =====================================================================================
        q("the_eye", 15, "The Eye on the Mountain",
                "The grey house on the summit hums at night. Ahu says it keeps what you'll need if you want to leave, and that the keypad wants a number you already know.")
                .obj(go("peak", "Climb to the summit", "The mountain in the north of the island. The path up starts near the falls; keep climbing."))
                .obj(flag("door_open", "peak", "Open the station door", "The keypad beside the steel door. The captain's tape told you: it's always the flight number."))
                .then("the_lab");

        q("the_lab", 15, "Experience Lab 4",
                "Concrete, cold air, the smell of electronics. Someone built this place, and someone still pays its power bill.")
                .obj(go("lab", "Enter the station", "Walk through the open door."))
                .obj(flag("read_logs", "lab", "Read the logs on the terminal", "The glowing terminal in the middle of the room."))
                .obj(flag("clue:lab_contract", "lab", "Search the bunk room", "Through the iron door on the right, south-east corner.").optional())
                .obj(flag("clue:viewer_mail", "lab", "Read the printout by the monitors", "On the long desk under the wall of screens.").optional())
                .obj(flag("halo_choice", "lab", "Decide what to do about HALO", "Talk to HALO through the terminal. The transmitter is in the back room."))
                .tick((c, t) -> {
                    if (c.has("at:lab") && !c.has("lab_cs")) {
                        c.flag("lab_cs");
                        c.director().cutscene(c.player(), "lab");
                    }
                })
                .then("boat_plan");

        // =====================================================================================
        // Chapter 16 - The Boat
        // =====================================================================================
        q("boat_plan", 16, "The Way Off",
                "You have a chart of the reef. Teo has a boat in his head. Time to put the two together.")
                .obj(talk("teo", "Teo", "boat_plan", "Show Teo the navigation chart", "Teo is at the dock on the east shore."))
                .then("boat_build");

        q("boat_build", 16, "Boatwright",
                "Bring materials to the boat frame at the end of Teo's dock, one stage at a time. Right-click the frame to hand over what you carry.")
                .obj(stage("boat_1", "Keel:", "Palm logs, from the palms along every beach.",
                        v -> "palm logs (" + Math.min(16, v.counter("boat:palm_log")) + "/16)"))
                .obj(stage("boat_2", "Hull:", "Hull panels: break wreckage blocks at the crash site. Planks: craft from palm logs.",
                        v -> "hull panels (" + Math.min(8, v.counter("boat:hull_panel")) + "/8), palm planks (" + Math.min(24, v.counter("boat:palm_planks")) + "/24)"))
                .obj(stage("boat_3", "Mast & rigging:", "Rope: 3 plant fiber. Mast: 3 palm logs in a column with rope beside them.",
                        v -> "mast (" + Math.min(1, v.counter("boat:mast")) + "/1), rope (" + Math.min(8, v.counter("boat:rope")) + "/8)"))
                .obj(stage("boat_4", "Sail:", "Sail cloth: 4 airline blankets around 1 rope. Blankets come from suitcases and from Mira.",
                        v -> "sail cloth (" + Math.min(1, v.counter("boat:sail_cloth")) + "/1)"))
                .obj(stage("boat_5", "Engine & provisions:", "The engine coil from the sunken plane, fresh water from the river (fill empty flasks), and any food.",
                        v -> "engine coil (" + Math.min(1, v.counter("boat:engine_coil")) + "/1), water (" + Math.min(4, v.counter("boat:water_flask")) + "/4), food (" + Math.min(8, v.counter("boat:food")) + "/8)"))
                .then("farewell");

        // =====================================================================================
        // Chapter 17 - Last Call
        // =====================================================================================
        q("farewell", 17, "Goodbyes",
                "The boat floats. You can leave at first light. Say goodbye, and decide who, if anyone, comes with you.")
                .obj(talk("ahu", "Elder Ahu", "bye_ahu", "Say goodbye to Elder Ahu", "In the longhouse."))
                .obj(talk("mira", "Mira", "bye_mira", "Say goodbye to Mira", "By her garden."))
                .obj(talk("nalu", "Nalu", "bye_nalu", "Say goodbye to Nalu", "At the edge of the village."))
                .obj(talk("teo", "Teo", "bye_teo", "Thank Teo", "On the dock.").optional())
                .then("last_night");

        q("last_night", 17, "Last Call",
                "The Unrated are gathering on the beaches. All of them. Hold out at the dock until first light, then cast off. Or don't.")
                .obj(flag("dawn3", "dock", "Survive the final night", "Stay near the dock and the boat. Keep moving, keep looking."))
                .obj(flag("cast_off", "dock", "Take the helm at dawn", "Board the boat and right-click the wheel at its stern.").at(IslandBuilder.helmPos()))
                .start(c -> c.director().nightFall())
                .tick((c, t) -> {
                    if (!c.has("dawn3")) c.director().nightWatch(c, "dawn3", false);
                })
                .then("ending");

        q("ending", 18, "Epilogue",
                "The island grows small behind you. Or you watch the boat leave without you.")
                .obj(flag("ending_seen", null, "...", "There is nothing left to do but watch the water."))
                .tick((c, t) -> {
                    if (t == 10) c.director().cutscene(c.player(), c.has("ending_stay") ? "stay" : "escape");
                    if (t > 10 && (c.has("cs:escape") || c.has("cs:stay")) && !c.has("ending_started")) {
                        c.flag("ending_started");
                        c.director().ending(c.player());
                    }
                })
                .then("the_end");

        q("the_end", 18, "The End",
                "Thank you for flying Meridian. You can keep exploring this world for as long as you like. "
                        + "Start a new story from the title screen to see what the other choices lead to.")
                .then(null);
    }
}
