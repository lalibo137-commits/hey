package com.airlinestory.dialogue;

import com.airlinestory.registry.ModItems;

import static com.airlinestory.dialogue.Dialogue.tree;
import static com.airlinestory.dialogue.Dialogues.add;
import static com.airlinestory.dialogue.Dialogues.q;

/** Act II: Flight 417 and the things on it. */
final class PlaneTalk {
    private PlaneTalk() {}

    static void register() {
        add(tree("june", "June").rateable()
                .start(v -> {
                    if (q(v, "board")) return v.has("june_welcome") ? "generic" : "welcome";
                    if (q(v, "cruise")) return v.has("called_june") && !v.has("got_meal") ? "meal" : !v.has("called_june") ? "busy" : "generic";
                    if (q(v, "lights_out") && !v.has("june_stare")) return "stare";
                    if (q(v, "the_hold")) return "hold";
                    return "generic";
                })
                .node("welcome", "Welcome aboard Meridian 417! Boarding pass? ...Perfect. 14A, down the aisle on your left. We're so glad you're back, {player}.")
                .opt("Back? Everybody keeps saying that.", "back")
                .opt("Thank you, June.", "bye").rate(0.1f)
                .node("back", "Do they? It's just something we say. 'Meridian: coming home.' It's on the napkins.")
                .cont("bye")
                .node("bye", "Overhead bins are above each row. Stow your bag, take your seat, and enjoy the experience.")
                .end("[Walk to your seat]").flag("june_welcome")
                .node("busy", "Please use your call button, {player}. It helps HALO rate my response times.")
                .end("Sorry.")
                .node("meal", "You called? What can I get you? We have a chicken, a pasta, and an apology for the pasta.")
                .opt("The chicken, please.", "meal_end").rate(0.05f).fx(c -> c.give(ModItems.AIRLINE_MEAL.get(), 1))
                .opt("Just some peanuts.", "meal_end").fx(c -> c.give(ModItems.AIRLINE_PEANUTS.get(), 3))
                .opt("Can I ask you something? Who's flying this plane?", "who")
                .node("who", "The captain, of course. He's flown this route more times than anybody. More times than... well. Lots of times. Her smile stays exactly where it is. Chicken?")
                .opt("...Chicken.", "meal_end").fx(c -> c.give(ModItems.AIRLINE_MEAL.get(), 1))
                .node("meal_end", "Enjoy! Let me know if you need anything. I'm always watching the call lights. Always.")
                .end("Thanks.").flag("got_meal")
                .node("hold", "Is everything alright, {player}? You've been walking up and down the aisle. HALO says pacing is a two point five behaviour.")
                .end("Just stretching my legs.")
                .node("stare", "She is standing very still in the galley, hands folded. You've been downstairs, {player}. Theo told me. Theo tells me everything.")
                .opt("There's a mannequin of me down there.", "dummy")
                .opt("Who is HALO?", "halo")
                .opt("Who is flying this plane, June?", "who2")
                .opt("[Back away slowly]", "away")
                .node("dummy", "For the wide shots. You can't film a person for six hours straight. People blink. People look at the camera. She glances at a corner of the ceiling, and back.")
                .cont("end")
                .node("halo", "Hospitality And Loyalty Optimiser. HALO looks after all of us. HALO is the reason we are always on time.")
                .cont("end")
                .node("who2", "Nobody, {player}. Nobody has ever flown this plane. She says it sweetly, the way you'd tell a child about the weather.")
                .cont("end")
                .node("away", "She tilts her head. And keeps tilting it, a little further than a neck should go.")
                .cont("end")
                .node("end", "The lights will go out in a moment. It's in the schedule. Please return to seat 14A, and whatever you do, {player}, keep smiling.")
                .end("[Go back to 14A]").flag("june_stare")
                .node("generic", "Can I get you anything? Water? A blanket? A better rating?")
                .end("No, thanks."));

        add(tree("theo", "Theo")
                .start(v -> q(v, "the_hold") && !v.has("theo_distracted") ? "guard" : v.has("theo_distracted") && !v.has("on_island") && q(v, "the_hold") ? "busy" : "start")
                .node("start", "Rear galley's closed, sorry. Lavatory is just there. Please don't take too long in front of the mirror. People take too long in front of the mirror.")
                .end("Okay.")
                .node("guard", "Can I help you? He's standing with one foot on the hatch in the floor. He does not move it.")
                .opt("There's a passenger being sick in row 3.", "lie").rate(-0.1f).remember("You lied to Theo to get past him.")
                .opt("June asked for you. Forward galley.", "june")
                .opt("What's under that hatch?", "what")
                .opt("[Look at the hatch, then at Theo]", "look")
                .node("lie", "Row 3? That's Mr. Halvorsen. Oh no. If he gets sick his rating gets sick. He's already moving. Don't touch anything!")
                .end("[Wait until he's gone]").flag("theo_distracted").fx(c -> c.event("theo_away"))
                .node("june", "Did she? She never asks for me. He brightens like a dog that heard its name. Stay out of the galley.")
                .end("[Wait until he's gone]").flag("theo_distracted").fx(c -> c.event("theo_away"))
                .node("what", "Luggage. Very boring luggage. Two hundred of the same suitcase. Sorry. I mean: luggage.")
                .cont("guard")
                .node("look", "He follows your eyes down to his foot. Then back up. He smiles. He keeps smiling.")
                .cont("guard")
                .node("busy", "He isn't here. Through the curtain you can hear him at the front: \"Mr. Halvorsen? Mr. Halvorsen, are you unwell?\"")
                .end("[Leave]"));

        add(tree("seat_screen", "Seat-back Screen")
                .start(v -> v.has("watched_screen") ? "again" : "first")
                .node("first", "WELCOME TO MERIDIAN SKYVIEW. Now showing: LIVE. The picture becomes a cabin. Row 14. A passenger in the window seat, filmed from behind. They lean towards their screen. So do you.")
                .opt("[Wave at the screen]", "wave")
                .opt("[Look behind you]", "behind")
                .opt("[Try to turn it off]", "off")
                .node("wave", "You raise your hand. The passenger on the screen does not. Instead they turn around, slowly, and look straight into the camera with your face.")
                .cont("end")
                .node("behind", "There's nothing behind you but the headrest of 15A and the man asleep in it. His eyes are open.")
                .cont("end")
                .node("off", "There is no off button. A caption fades in: VIEWERS: 7,214,880.")
                .cont("end")
                .node("end", "The screen goes back to the Meridian logo. A little smiling sun. Under it, small, in the corner: EPISODE 4.")
                .end("[Look away]").flag("watched_screen").fx(c -> c.event("screens_on"))
                .node("again", "VIEWERS: 7,214,880... 7,214,893... 7,215,002...")
                .end("[Look away]"));

        add(tree("seat_screen_idle", "Seat-back Screen")
                .node("start", "MERIDIAN SKYVIEW. Flight time remaining: --:--. Outside temperature: --. Distance to Port Ellery: --.")
                .end("[Look away]"));

        add(tree("mirror", "Mirror")
                .start(v -> v.has("saw_mirror") ? "again" : "first")
                .node("first", "The lavatory light buzzes. You look tired. You look... your reflection is half a second behind you. You lift your hand. It lifts its hand. Late.")
                .opt("[Stare it down]", "stare")
                .opt("[Splash water on your face]", "splash")
                .opt("[Leave. Now.]", "leave")
                .node("stare", "It smiles first. You are not smiling. Then, very slowly, you are.")
                .cont("end")
                .node("splash", "When you look up, the reflection is already looking at you. It mouths something. You think it's a number. Four.")
                .cont("end")
                .node("leave", "You fumble with the lock. Behind you, from the other side of the mirror, comes a gentle tapping.")
                .cont("end")
                .node("end", "...")
                .end("[Get out of the lavatory]").flag("saw_mirror")
                .node("again", "Just a mirror. Just you. Probably.")
                .end("[Leave]"));

        add(tree("cockpit", "Cockpit Door")
                .start(v -> v.has("knocked_cockpit") ? "after" : q(v, "row20") ? "knock" : "locked")
                .node("knock", "You knock. Nothing. You knock harder. Nothing. Then a scrape, and a folded piece of paper slides out from under the door.")
                .opt("[Pick it up]", "note")
                .node("note", "Shaky capitals, in pencil: NOBODY IS FLYING THIS PLANE. THERE NEVER WAS ANYONE. LOOK UNDER THE FLOOR: REAR GALLEY HATCH. DON'T LET THEO SEE. The handwriting looks like yours.")
                .end("[Keep the note]").flag("knocked_cockpit").fx(c -> c.give(ModItems.CRUMPLED_NOTE.get(), 1))
                .node("after", "Silence on the other side. The kind of silence that listens.")
                .end("[Step away]")
                .node("locked", "A sign on the door: FLIGHT DECK. CREW ONLY. The door is locked.")
                .end("[Step away]"));
    }
}
