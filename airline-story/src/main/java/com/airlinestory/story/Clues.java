package com.airlinestory.story;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Collectible lore: notes, tapes and photographs hidden around the world. Shown in the journal. */
public final class Clues {
    private Clues() {}

    public record Clue(String id, String act, String title, String text) {}

    private static final Map<String, Clue> ALL = new LinkedHashMap<>();

    @Nullable
    public static Clue get(String id) { return ALL.get(id); }

    public static Iterable<Clue> all() { return ALL.values(); }

    public static int count() { return ALL.size(); }

    private static void add(String id, String act, String title, String text) {
        ALL.put(id, new Clue(id, act, title, text));
    }

    static {
        // ---------------- Meridian International ----------------
        add("closet_camera", "Airport", "A camera in the closet",
                "Behind the mop buckets, the back wall of the janitor's closet is painted plywood. Somebody drilled a neat round hole through it at head height. Behind the hole, a lens is focused on the closet door. A label on the housing reads: CAM 31 - LANDSIDE STORAGE - SUBJECT ROUTE B (unlikely).");
        add("missing_poster", "Airport", "MISSING: Flight 417",
                "HAVE YOU SEEN THIS PASSENGER? Last seen boarding Meridian Flight 417 to Port Ellery. The face on the poster is yours. The date at the bottom is four years ago. Someone has written under it in biro: 'still boarding'.");
        add("call_sheet", "Airport", "Call sheet, Day 1",
                "MERIDIAN EXPERIENCES - CALL SHEET - ITERATION 4, DAY 1. 06:00 Crowd in positions. 06:42 SUBJECT ARRIVES (taxi, kerbside). 07:05 Check-in (DANA). 07:30 Security beat: polaroid reveal (REYES). 08:10 Gate B7 ensemble. 09:10 BOARD. NOTES: subject's score must be kept between 2.0 and 4.0 at all times. Drama lives in the middle.");
        add("crew_memo", "Airport", "Memo to cabin crew",
                "To all Sky Hosts: we have had complaints that the subject noticed the same passengers in the same seats across iterations. Please rotate wigs. Remember: you are not acting. You are hosting. - J.");
        add("lost_found", "Airport", "Lost property tag",
                "LOST PROPERTY - ITEM: blue carry-on bag. OWNER: (your name). FOUND: Gate B7. DATE: four years ago. STATUS: returned to owner. Returned to owner. Returned to owner. Returned to owner.");
        add("control_log", "Airport", "Control room log",
                "06:44 Subject paused at the doors (good). 06:51 Viewers 2.1M. 07:12 Subject was rude to DANA (excellent - viewership up 9%). 07:40 Subject looked directly at CAM 12 for 4 seconds. Production advises: do NOT let them find the backstage door. (If they do, let them. Ratings.)");
        // ---------------- Flight 417 ----------------
        add("cargo_manifest", "Flight 417", "Cargo manifest",
                "MA417 HOLD MANIFEST. 1x ISLAND SET (BEACH, MODULAR). 4x WEATHER RIG (STORM). 200x SUITCASE, IDENTICAL, NAME TAG PRE-PRINTED. 1x SUBJECT DOUBLE (MANNEQUIN, 14A). 0x PILOT.");
        add("seat_14a_dummy", "Flight 417", "The mannequin in the hold",
                "Strapped into a seat in the cargo hold: a mannequin wearing your clothes. Polaroids taped to its chest: it, in seat 14A, on the beach, at the fire. A tag on its wrist: 'STAND-IN FOR WIDE SHOTS. DO NOT LET SUBJECT SEE.'");
        add("june_badge", "Flight 417", "June's old name badge",
                "Wedged behind the galley coffee machine: a Meridian name badge. JUNE OKAFOR - SKY HOST - ITERATION 2. The photo on it is June. She looks ten years younger and terrified.");
        // ---------------- The Island ----------------
        add("tape_1", "Island", "Flight recorder tape 1: Hale",
                "\"This is Captain Robert Hale. Day eleven. I've stopped believing there was a storm. Instruments were perfect right up until the screens told me to put her down. I put her down. There is a hum under this island. It sounds like an audience.\"");
        add("tape_2", "Island", "Flight recorder tape 2: Hale",
                "\"Day forty. Found a grey box on the summit. Door won't open. There's a keypad. I tried my birthday, my wife's birthday, the flight number. The flight number. Note to whoever comes next: it's always the flight number.\"");
        add("tape_3", "Island", "Flight recorder tape 3: Hale",
                "\"Year two, I think. More of them came today. A whole plane of them. Same plane. Same seats. One of the cabin crew is called June. She doesn't remember me. I didn't tell her she's the second June.\"");
        add("okafor_diary", "Island", "Water-stained diary",
                "\"They told us it was a reality programme about service. Fourteen days. Generous fee. I signed for my mother's operation. When the plane went down I laughed, because I thought the crash was part of the show. I'm still waiting for someone to say cut.\"");
        add("bell_letter", "Island", "Letter from Nate Bell",
                "\"Marcus - if they ever let you find this: I'm alive. I think I'm alive. Mum's score must have dropped when I vanished. I'm sorry. Don't come looking. They WANT you to come looking. It's called the Family Drama package. Love, Nate.\"");
        add("burned_photo", "Island", "Burned family photo",
                "Half a photograph, edges charred. Two boys on a beach, grinning. On the back: 'N + M, Port Ellery, summer'. You were told Port Ellery doesn't exist. The beach in the photo is this one.");
        add("set_crate", "Island", "Production crate",
                "A rotten crate half-buried in the sand. Stencilled on the side: MERIDIAN EXPERIENCES - PROP DEPT - 'WRECKAGE, LIGHT, x40'. Inside: more wreckage. Brand new.");
        add("hollow_note", "Island", "Note in the grove",
                "Scratched into the back of a seat in the Grove: I STOPPED TALKING TO THEM. I STOPPED SMILING. NOBODY LOOKED AT ME FOR A WEEK. THIS MORNING MY FACE WAS SMOOTH.");
        add("lab_contract", "Island", "Your signed contract",
                "MERIDIAN REHABILITATION EXPERIENCE - PARTICIPANT AGREEMENT. I consent to memory adjustment, continuous broadcast, and rating by the viewing public, in exchange for the restoration of my SmileScore. Signed: you. Witness: HALO. Your signature looks very tired.");
        add("viewer_mail", "Island", "Viewer mail printout",
                "TOP VIEWER COMMENTS THIS WEEK: 'Make them fight the tribe guy.' 'Is the old woman real?' 'Bring back the stare episode!!' '1 star, subject too passive.' '5 stars, I cried when they held Marcus's hand.' 'Do they know?' 'They never know.'");
    }
}
