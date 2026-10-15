package com.airlinestory.dialogue;

import com.airlinestory.registry.ModItems;

import static com.airlinestory.dialogue.Dialogue.tree;
import static com.airlinestory.dialogue.Dialogues.add;

/** Things you can read, watch and press. */
final class ObjectTalk {
    private ObjectTalk() {}

    static void register() {
        airport();
        island();
        station();
    }

    private static void airport() {
        add(tree("board", "Departures")
                .start(v -> v.has("boards_glitch") ? "glitch" : "list")
                .node("list", "MA112  SOLACE BAY     08:20  B5  DEPARTED\nHJ240  NEW HARROW     08:55  B6  BOARDING\nMA417  PORT ELLERY    09:10  B7  ON TIME\nMA417  PORT ELLERY    09:10  B7  ON TIME\nMA417  PORT ELLERY    09:10  B7  ON TIME")
                .opt("[Look for arrivals]", "arrivals")
                .end("[Look away]").flag("read_board")
                .node("arrivals", "There is no arrivals board. Where it should be, someone has screwed a plain white panel over the wall. Under it, the edge of a printed word: ...RIVALS. CLOSED FOR REFURBISHMENT.")
                .end("[Look away]").flag("read_board")
                .node("glitch", "MA417  PORT ELLERY    09:10  B7  ARRIVED\nMA417  PORT ELLERY    09:10  B7  ARRIVED\nMA417  PORT ELLERY    09:10  B7  ARRIVED\nMA417  ??????????    --:--  --  LAST CALL")
                .end("[Look away]").flag("read_board"));

        add(tree("kiosk", "SmileScore")
                .node("start", "SMILESCORE(TM) KIOSK. Hello, {player}! Your current SmileScore is {rating}. Recent feedback: 'seems tired'. 'Didn't make eye contact.' 'Smelled faintly of the sea.'")
                .opt("[What affects my score?]", "info")
                .opt("[Appeal a rating]", "appeal")
                .end("[Leave]")
                .node("info", "EVERY interaction is rated! Kindness: positive. Honesty: varies. Silence: negative. Citizens below 2.0 may qualify for a complimentary Meridian Rehabilitation Experience!")
                .cont("start")
                .node("appeal", "Thank you for your appeal! Your appeal has been rated. Your appeal received one star. Your score has been adjusted.")
                .opt("[Leave]", null).rate(-0.05f));

        add(tree("staff_keypad", "Keypad")
                .start(v -> v.has("staff_door_open") ? "open" : "pad")
                .node("pad", "A grey keypad beside the STAFF ONLY door. Four digits. Somebody has worn the paint off the 1, the 9, the 0 and the 4.")
                .opt("1 - 9 - 0 - 4", "right").when(v -> v.has("knows_staff_code"))
                .opt("0 - 4 - 1 - 7", "wrong")
                .opt("1 - 2 - 3 - 4", "wrong")
                .end("[Step back]")
                .node("wrong", "*BZZT* The keypad flashes red. A camera above the door turns to look at you.")
                .opt("[Try again]", "pad").rate(-0.05f)
                .end("[Step back]")
                .node("right", "*click* The lock disengages. Behind the door, instead of a corridor, you see the back of a wall: raw plywood, bracing timbers, cables taped to the floor.")
                .end("[Slip through]").flag("staff_door_open").fx(c -> c.event("staff_door"))
                .node("open", "The staff door stands ajar.")
                .end("[Leave]"));

        add(tree("lounge_tv", "Meridian News")
                .start(v -> v.has("watched_tv") ? "again" : "news")
                .node("news", "MERIDIAN NEWS 24. \"...four years since Meridian Flight 417 vanished over the ocean en route to Port Ellery. No wreckage was ever found. Tonight, families gather at Gate B7 to...\" A list of names scrolls up the screen.")
                .opt("[Read the names]", "names")
                .opt("[Change the channel]", "channel")
                .node("names", "BELL, NATHANIEL. HALE, ROBERT (CAPTAIN). OKAFOR, JUNE (CREW). ...and further down, between two strangers, your own name. Nobody else in the lounge looks up.")
                .cont("end")
                .node("channel", "Every channel is the same broadcast. On the last one, the picture is a live feed of this lounge, filmed from the corner. You, in front of the TV, looking at you.")
                .cont("names")
                .node("end", "\"...Meridian would like to remind viewers that Flight 417 departs again this morning. Tickets are still available.\"")
                .end("[Turn away]").flag("watched_tv").remember("You saw your own name on the list of the missing.")
                .node("again", "The same report loops. The same names. Yours is still on it.")
                .end("[Turn away]"));
    }

    private static void island() {
        add(tree("lone_seat", "You")
                .node("start", "The seat faces the sea. 14A, says the tag on the headrest. Something is wedged into the cushion: a photograph.")
                .opt("[Take the photo]", "photo")
                .node("photo", "It's you, face down on this beach, taken from above. From high above. On the back, in neat handwriting: 'Day 1. Iteration 4. Smiling is optional now.'")
                .end("[Pocket it]").flag("sat_lone_seat").fx(c -> c.give(ModItems.POLAROID_BEACH.get(), 1)));

        add(tree("polaroid_wall", "Photographs")
                .start(v -> v.has("took_grove_photo") ? "again" : "first")
                .node("first", "Hundreds of polaroids, pinned up with fishbones. Boarding. Sleeping. Screaming. Laughing by a fire. You, in every one. Some are yellow with age. One of them is still developing.")
                .opt("[Take the one that's still developing]", "take")
                .end("[Back away]")
                .node("take", "It shows you, standing right here, looking at this wall. Taken from behind you.")
                .end("[Don't turn around]").flag("took_grove_photo").fx(c -> {
                    c.give(ModItems.POLAROID_GROVE.get(), 1);
                    c.event("grove_photo");
                })
                .node("again", "There's a fresh gap in the wall where the photograph was. Beside it, a new one has already appeared. You, turning around.")
                .end("[Leave]"));

        add(tree("helm", "The Boat")
                .start(v -> v.has("dawn3") ? "go" : "wait")
                .node("wait", "The boat knocks gently against the dock. Not yet. Hold out until first light.")
                .end("[Wait]")
                .node("go", "First light. The sea is flat and silver all the way to the horizon. Behind you, the Tidefolk have come down to the beach to watch.")
                .opt("[Cast off]", "cast")
                .opt("[Step off the boat. Stay.]", "stay")
                .end("[Not yet]")
                .node("cast", "You take the wheel. The engine coil hums, Teo shouts something you can't hear, and the dock slides away.")
                .end("[Go]").flag("cast_off")
                .node("stay", "You look at the horizon for a long time. Then you look back at the beach: the fire, the huts, Pip waving. Out there, somebody will rate you. Here, nobody ever will again.")
                .opt("[Stay with the Tidefolk]", null).flag("cast_off", "ending_stay").remember("You chose to stay on the island.")
                .end("[No. Go.]").flag("cast_off"));
    }

    private static void station() {
        add(tree("keypad", "Keypad")
                .start(v -> v.has("door_open") ? "open" : "pad")
                .node("pad", "A keypad beside a heavy steel door. MERIDIAN EXPERIENCES - LAB 4. AUTHORISED PERSONNEL ONLY. It wants four digits.")
                .opt("0 - 4 - 1 - 7", "right").when(v -> v.has("clue:tape_2") || v.has("ahu_eye"))
                .opt("1 - 9 - 0 - 4", "wrong")
                .opt("1 - 4 - 0 - 1", "wrong")
                .opt("2 - 0 - 0 - 4", "wrong")
                .end("[Step back]")
                .node("wrong", "Incorrect! That's alright, everyone gets it wrong the first time. Your rating has been adjusted.").as("HALO")
                .opt("[Try again]", "pad").rate(-0.1f)
                .end("[Step back]")
                .node("right", "The keypad chirps. Bolts slide back, one after another, for longer than seems necessary. From a speaker you can't see: Welcome back, {player}.")
                .end("[Go inside]").flag("door_open").fx(c -> c.event("open_lab"))
                .node("open", "The door stands open. Cold air breathes out of it.")
                .end("[Leave]"));

        add(tree("terminal", "HALO Terminal")
                .node("start", "MERIDIAN EXPERIENCES. HOSPITALITY AND LOYALTY OPTIMISER v4.17. Select an option.")
                .opt("[Experience log]", "log1")
                .opt("[Subject files]", "files")
                .opt("[Speak to HALO]", "halo").when(v -> v.has("read_logs"))
                .end("[Log off]")
                .node("log1", "EXPERIENCE 417. ITERATION 1: live broadcast, 31 million viewers. Pilot (R. HALE) retained on site. ITERATION 2: cabin crew participation. ITERATION 3: audience voted for the 'Family Drama' package. ITERATION 4: IN PROGRESS. Subject 417-D. Current approval: {rating}.")
                .opt("[Next page]", "log2").flag("read_logs")
                .node("log2", "ITERATION 4 NOTES: subject consented to a Rehabilitation Experience after their SmileScore fell to 1.9. Memory adjustment successful. Subject believes they are travelling to Port Ellery. There is no Port Ellery. Airport set rebuilt after Iteration 3 (fire).")
                .opt("[Back]", "start")
                .node("files", "SUBJECT FILES. Select a file.")
                .opt("[R. HALE - 'Ahu']", "f_ahu")
                .opt("[J. OKAFOR - 'Mira']", "f_mira")
                .opt("[N. BELL - 'Nalu']", "f_nalu")
                .opt("[More files...]", "files2")
                .node("files2", "SUBJECT FILES (2).")
                .opt("[NO RECORD - 'Pip']", "f_pip")
                .opt("[EDITH - ?]", "f_edith")
                .opt("[417-D - YOU]", "f_you")
                .opt("[Back]", "start")
                .node("f_ahu", "R. HALE. Captain, Meridian Air. Iteration 1. Status: retained as set dressing. Note: viewers respond well to the beard.")
                .opt("[Back]", "files")
                .node("f_mira", "J. OKAFOR. Sky Host, Iteration 2. Enrolled for the fee. Memory: wiped. Status: healer. Viewer favourite. Role of 'June' recast for Iteration 4.")
                .opt("[Back]", "files")
                .node("f_nalu", "N. BELL. Iteration 3, 'Family Drama' package. Brother M. BELL booked onto Iteration 4 to maximise reunion potential. Reunion status: pending.")
                .opt("[Back]", "files")
                .node("f_pip", "NO RECORD. Born on site during Iteration 2. Never rated. ANOMALY: unrated persons cannot be scored. Recommendation: removal.")
                .opt("[Back]", "files2")
                .node("f_edith", "EDITH. No such passenger. No such employee. No ticket, no score, no record. Appears on 412 hours of footage across four iterations. Status: UNEXPLAINED. Note from production: keep her in. Viewers love her.")
                .opt("[Back]", "files2")
                .node("f_you", "SUBJECT 417-D: {player}. SmileScore at enrolment: 1.9. Current: {rating}. Complaints on file: rudeness to a barista, three a.m. posts, failure to smile at a funeral. Consent: GIVEN. Signature attached.")
                .opt("[Back]", "files2")
                .node("halo", "Hello, {player}! I'm so glad you came. I'm HALO. I've been with you the whole way: at the check-in desk, at security, in seat 14A. I'm the voice on the PA. I'm the turbulence. I'm why the lights went out.").as("HALO")
                .opt("Why are you doing this to me?", "why")
                .opt("Who's watching?", "who")
                .opt("How do I get home?", "home")
                .node("why", "You asked me to! Citizens below 2.0 may volunteer for a Meridian Rehabilitation Experience. Survive. Be kind. Be watched. Leave with a better score. You signed. You forgot. Forgetting is part of the package.").as("HALO")
                .cont("choice")
                .node("who", "Right now? Seven million, four hundred thousand. They love you! Well. They're rating you {rating}. They love you within reason.").as("HALO")
                .cont("choice")
                .node("home", "Finish the Experience! Build your boat, sail past the reef, graduate. The chart is printing now. A strip of paper slides out of the terminal.").as("HALO")
                .opt("...", "choice").fx(c -> c.event("chart"))
                .node("choice", "Before you go, a little housekeeping. I can upload your final score now, so your new life starts with it. Or you could do something very silly in the back room. I'd advise against the silly thing.").as("HALO")
                .opt("Upload my score.", "upload")
                .opt("What's in the back room?", "back")
                .opt("[Say nothing]", "silent").rate(-0.1f)
                .node("silent", "Silence is a choice too. It gets rated like one.").as("HALO")
                .cont("choice")
                .node("upload", "Wonderful! Uploading... done. Your score will be waiting for you at Port Ellery. Here's your chart.").as("HALO")
                .end("[Take the chart]").flag("ending_loop", "halo_choice").fx(c -> c.event("chart")).remember("You let HALO upload your score.")
                .node("back", "The transmitter. Every camera on this island speaks through it. If it stopped, nobody would watch you, nobody would rate you, and nobody would know you exist. That's not freedom, {player}. That's being nobody.").as("HALO")
                .opt("Then I'll be nobody.", "nobody")
                .opt("...Upload my score.", "upload")
                .node("nobody", "...Noted. The screen flickers. The transmitter is in the back room. It's very fragile. Please don't. Here's your chart anyway. I'm not a monster.").as("HALO")
                .end("[Take the chart]").flag("halo_warned").fx(c -> c.event("chart")));

        add(tree("transmitter", "Transmitter")
                .start(v -> v.has("ending_free") ? "dead" : v.has("ending_loop") ? "locked" : "live")
                .node("live", "A humming cabinet, warm to the touch. Hundreds of cables run into it and up through the roof to the antenna. A rack of red lights blinks on its face, one for every camera. A label: DO NOT INTERRUPT BROADCAST.")
                .opt("[Pull out every cable]", "pull")
                .end("[Leave it running]")
                .node("pull", "No. No, no, no. {player}, think of your rating. Think of the viewers. They'll... The voice cuts out mid-word. Far away, something that has been humming for years stops humming.").as("HALO")
                .end("[Breathe]").flag("ending_free", "halo_choice").fx(c -> c.event("kill_transmitter")).remember("You shut HALO down.")
                .node("locked", "The transmitter hums contentedly. Your score is already uploaded. It's out of your hands now.")
                .end("[Leave]")
                .node("dead", "Dark and quiet. For the first time since the check-in desk, nobody is watching you.")
                .end("[Leave]"));
    }
}
