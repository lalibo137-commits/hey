package com.airlinestory.dialogue;

import com.airlinestory.registry.ModItems;
import com.airlinestory.story.FlagView;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

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

    private static void add(Dialogue.Builder b) {
        Dialogue d = b.build();
        TREES.put(d.id, d);
    }

    private static boolean q(FlagView v, String quest) {
        return quest.equals(v.quest());
    }

    static {
        airport();
        security();
        gate();
        plane();
        objects();
        island();
        station();
        extras();
    }

    // =========================================================================================
    private static void airport() {
        add(tree("dana", "Dana")
                .start(v -> q(v, "the_end") && v.has("ending_loop") ? "loop" : v.has("checked_in") ? "after" : "hello")
                .node("hello", "Good morning, and welcome to Meridian Air! I'm Dana. May I see your passport? ...Oh! Oh, it's you! Welcome back!")
                .opt("Back? I've never flown Meridian before.", "back")
                .opt("Morning, Dana. Just the one bag, carry-on.", "passport").rate(0.1f)
                .opt("[Hand over your passport without a word]", "passport").rate(-0.1f).remember("You didn't say a word to Dana.")
                .node("back", "Of course you haven't! Silly me. Everyone looks familiar at six in the morning. Passport, please?")
                .cont("passport")
                .node("passport", "{player}. Seat 14A, a window. Lovely. And are we travelling for business or pleasure today?")
                .opt("Work. A conference in Port Ellery.", "work")
                .opt("Visiting family.", "family").rate(0.05f)
                .opt("Honestly? I just needed to get away.", "away").remember("You told Dana you were running away from something.")
                .opt("Is that any of your business?", "rude").rate(-0.3f).remember("You snapped at Dana.")
                .node("work", "How exciting! Port Ellery is lovely this time of year. It's lovely every time of year, actually. That's in the brochure.")
                .cont("score")
                .node("family", "That's beautiful. Family is the best rating there is.")
                .cont("score")
                .node("away", "Oh, we get a lot of that. So many people just... need to get away. Meridian is very, very good at away.")
                .cont("score")
                .node("rude", "...Of course. I'm so sorry. You must be under a lot of pressure. Pressure is completely understandable. She keeps smiling. She types something long.")
                .cont("score")
                .node("score", "Now, Meridian is a proud SmileScore partner airline. Your score today is {rating}. Passengers above four point five board first and enjoy complimentary everything!")
                .opt("What happens if your score is low?", "low")
                .opt("Could you bump me up a little? Off the record?", "bump")
                .opt("Great. Just the boarding pass, please.", "pass")
                .node("low", "Nothing happens! Nothing at all happens. You just board a little later. And you're seen. Everyone gets seen, eventually.")
                .cont("pass")
                .node("bump", "Ha! I wish. Every interaction is rated, you know. Even this one. Especially this one.")
                .cont("pass")
                .node("pass", "Here's your boarding pass. Gate B7, boarding at eight-forty. Security is through the doors on the east side. Have a wonderful flight, {player}. We'll be thinking of you.")
                .opt("Thanks, Dana.", null).rate(0.05f).flag("checked_in").fx(c -> c.director().giveOwned(c.player(), ModItems.BOARDING_PASS.get()))
                .opt("[Take the pass and go]", null).flag("checked_in").fx(c -> c.director().giveOwned(c.player(), ModItems.BOARDING_PASS.get()))
                .node("after", "You're all set, {player}! Gate B7. Do try to smile on your way through security. The cameras love a smile.")
                .end("[Leave]")
                .node("loop", "Good morning, and welcome to Meridian Air! May I see your passport? ...Oh. Oh, it's you. Welcome back, {player}. We kept your seat.")
                .end("..."));

        add(tree("edith", "Edith")
                .start(v -> q(v, "row20") ? (v.has("edith_plane") ? "plane_again" : "plane") : v.has("talked_edith") ? "again" : "hello")
                .node("hello", "Sit down a minute, dear. Your legs will thank you. You'll be doing a lot of standing soon.")
                .opt("Sorry, do I know you?", "know")
                .opt("Where are you flying today?", "flying")
                .opt("I'm fine standing, thanks.", "standing")
                .node("know", "No, no. But you will. Or you did. It's hard to keep track at my age. Which seat did they give you this time?")
                .opt("14A.", "seat").remember("You told Edith your seat number.")
                .opt("\"This time\"?", "thistime")
                .opt("I'd rather not say.", "secret")
                .node("thistime", "Mm. Don't mind me. I say odd things. My husband used to rate me a two for it.")
                .cont("secret")
                .node("seat", "The window. Of course it is. You always ask for the window.")
                .cont("advice")
                .node("secret", "Keep your secrets, then. They'll print them anyway.")
                .cont("advice")
                .node("flying", "Me? Oh, I'm not flying today, dear. I don't fly any more. I just come to watch them go.")
                .cont("advice")
                .node("standing", "Suit yourself. Most people do.")
                .cont("advice")
                .node("advice", "Let me give you some advice, since nobody gave it to me. When the lights go out, don't look at the other passengers. Look at your hands. Count your fingers.")
                .opt("That's... a strange thing to say.", "bye")
                .opt("Why would the lights go out?", "bye2")
                .opt("[Laugh it off]", "bye").rate(0.05f)
                .node("bye", "Is it? Off you go, then. Security is that way. And {player}? Smile for the cameras. It helps, right up until it doesn't.")
                .end("Goodbye, Edith.").flag("talked_edith")
                .node("bye2", "They always do, dear. Somewhere over the water. Off you go. Smile for the cameras.")
                .end("Goodbye, Edith.").flag("talked_edith")
                .node("again", "Still here? You'll miss it. You never miss it, though, do you.")
                .end("[Leave]")
                .node("plane", "There you are. Sit, sit. I saved you the aisle, but you never take it.")
                .opt("You told me you weren't flying today!", "said")
                .opt("How did you get on this plane?", "how")
                .opt("Edith, what is going on?", "what")
                .node("said", "I'm not flying. I'm sitting. Flying is for the pilot, and there isn't one.")
                .cont("four")
                .node("how", "Same way as you, dear. A ticket and a smile.")
                .cont("four")
                .node("what", "It's the fourth time. You don't remember the first three. I remember all of them. That's what you get for keeping your rating so high.")
                .cont("four")
                .node("four", "Go and knock on the cockpit door. Go on. You'll want to know. You always want to know.")
                .end("I'll go.").flag("edith_plane")
                .opt("No. I want answers from you.", "answers")
                .node("answers", "Answers cost rating, and mine's too high to give it away. Knock on the door, {player}.")
                .end("[Leave her to her knitting]").flag("edith_plane").remember("You demanded answers from Edith.")
                .node("plane_again", "Knit one, purl one. Knit one, purl one. Count your fingers, dear.")
                .end("[Leave]"));

        add(tree("board", "Departures")
                .start(v -> v.has("boards_glitch") ? "glitch" : "list")
                .node("list", "MA112  SOLACE BAY     08:20  B5  DEPARTED\nMA305  NEW HARROW     08:55  B6  BOARDING\nMA417  PORT ELLERY    09:10  B7  ON TIME\nMA417  PORT ELLERY    09:10  B7  ON TIME\nMA417  PORT ELLERY    09:10  B7  ON TIME")
                .end("[Look away]").flag("read_board")
                .node("glitch", "MA417  PORT ELLERY    09:10  B7  ARRIVED\nMA417  PORT ELLERY    09:10  B7  ARRIVED\nMA417  PORT ELLERY    09:10  B7  ARRIVED\nMA417  ??????????    --:--  --  LAST CALL")
                .end("[Look away]").flag("read_board"));

        add(tree("kiosk", "SmileScore")
                .node("start", "SMILESCORE(TM) KIOSK. Hello, {player}! Your current SmileScore is {rating}. Recent feedback: 'seems tired'. 'Didn't make eye contact.' 'Smelled faintly of the sea.'")
                .opt("[What affects my score?]", "info")
                .end("[Leave]")
                .node("info", "EVERY interaction is rated! Kindness: positive. Honesty: varies. Silence: negative. Citizens below 2.0 may qualify for a complimentary Meridian Rehabilitation Experience!")
                .end("[Leave]"));

        add(tree("agent", "Check-in Agent")
                .node("start", "Sorry, this desk is for Solace Bay. Meridian is the blue desk in the middle. You can't miss it. Nobody ever misses it.")
                .end("Thanks."));

        add(tree("staff", "Airport Staff")
                .node("start", "Can I help you? ...No? Have a pleasant day. Remember to rate your experience on the way out. There's no way out without rating.")
                .end("[Leave]"));
    }

    // =========================================================================================
    private static void security() {
        add(tree("reyes", "Officer Reyes")
                .start(v -> v.has("reyes_cleared") ? "after" : q(v, "flagged") ? "flagged" : v.has("reyes_intro") ? "waiting" : "intro")
                .node("intro", "Morning. Bags on the belt, belts in the tray, smiles on the face. Got anything in that carry-on I should know about?")
                .opt("Just clothes and a toothbrush.", "ok")
                .opt("Nothing you'd find interesting.", "ok")
                .opt("Why, what are you expecting to find?", "expect").rate(-0.1f).remember("You got smart with Officer Reyes.")
                .node("ok", "Good. Bag on the belt, then walk through the arch. Don't stop in the arch. People always stop in the arch.")
                .end("Okay.").flag("reyes_intro")
                .node("expect", "Me? Nothing. Nineteen years doing this, I've stopped expecting anything. Belt. Arch. Go.")
                .end("Fine.").flag("reyes_intro")
                .node("waiting", "Belt, then arch. Bag first. You're holding up a very patient line.")
                .end("[Leave]")
                .node("flagged", "Step over here, please. Bag went through clean, mostly. No liquids, no blades. But there's this. He holds up a photograph. A polaroid. It's you. Asleep, in an airplane seat. Window seat, by the look of it.")
                .opt("That's not mine. I've never seen it before.", "notmine")
                .opt("Someone must have slipped it into my bag.", "slipped")
                .opt("...Can I see the back?", "back")
                .node("notmine", "It was in your bag and it's of you. The date on the back is today. Afternoon, it says. It's a quarter past seven in the morning.")
                .cont("decide")
                .node("slipped", "Maybe. I've seen stranger. Not much stranger.")
                .cont("decide")
                .node("back", "Sure. It says: 'Seat 14A. Iteration 0417.' Mean anything to you?")
                .opt("No.", "decide")
                .opt("My seat is 14A.", "seat").remember("You told Reyes the photo matched your seat.")
                .node("seat", "Huh. He looks at you for a long moment. Then I guess somebody's a fan.")
                .cont("decide")
                .node("decide", "Look, it's not prohibited. Polaroids aren't on the list. I can bin it, or you can keep it.")
                .opt("I'll keep it.", "keep").flag("kept_polaroid").fx(c -> c.give(ModItems.POLAROID_SEAT.get(), 1))
                .opt("Bin it. I don't want it.", "bin").flag("binned_polaroid")
                .node("keep", "Your call. Your bag's back. Have a good flight. And maybe sleep with one eye open.")
                .end("Thanks, officer.").flag("reyes_cleared").fx(c -> c.give(ModItems.CARRY_ON_BAG.get(), 1))
                .node("bin", "Done. He drops it into the bin by his feet. When you glance back a second later, the bin is empty. Bag's back. Next!")
                .end("[Take your bag]").flag("reyes_cleared").fx(c -> c.give(ModItems.CARRY_ON_BAG.get(), 1))
                .node("after", "You're clear. Move along, please. Gates are that way.")
                .end("[Leave]"));

        add(tree("officer", "Security Officer")
                .node("start", "This lane's closed. Use Officer Reyes's lane. Everyone uses Reyes's lane. Reyes likes it that way.")
                .end("Okay."));
    }

    // =========================================================================================
    private static void gate() {
        add(tree("tomas", "Tomas")
                .start(v -> v.has("got_coffee") ? "after" : "hello")
                .node("hello", "Morning! The usual?")
                .opt("The usual? I've never been here.", "usual")
                .opt("Sure. The usual.", "sure").remember("You let Tomas believe he knew your order.")
                .opt("Just a black coffee, please.", "black")
                .node("usual", "Huh. Could've sworn. Oat flat white, extra hot, no lid. You say 'no lid' like it's a secret. No? Sorry. Long shift. Very long shift.")
                .cont("make")
                .node("sure", "Oat flat white, extra hot, no lid. Coming up. He's already making it. He started before you answered.")
                .cont("make")
                .node("black", "Black coffee. Bold. The ratings say people who drink it black are point three less approachable.")
                .cont("make")
                .node("make", "There you go. On the house. Passengers on the 417 always drink on the house.")
                .opt("Why the 417?", "why")
                .opt("Thanks, Tomas. Five stars.", "stars").rate(0.1f)
                .opt("[Take the cup]", "bye")
                .node("why", "Policy. Don't ask me, I just pour. ...Can I tell you something? I've never seen anyone come back off the 417. Return flights land at B7 all day. I see every face. Never anyone I served.")
                .cont("bye")
                .node("stars", "Ha! Careful. If you rate me five I have to rate you five, and then we're both stuck being nice forever.")
                .cont("bye")
                .node("bye", "Drink it while it's hot. Nothing stays hot for long out there.")
                .end("See you, Tomas.").flag("got_coffee").fx(c -> c.give(ModItems.COFFEE.get(), 1))
                .node("after", "Enjoy the coffee. Want a refill later? There's no later on the 417, but you know. Hypothetically.")
                .end("[Leave]"));

        add(tree("corinne", "Corinne")
                .start(v -> q(v, "boarding_call") ? "boarding" : v.has("corinne_intro") ? "after" : "hello")
                .node("hello", "Good morning! Flight 417 to Port Ellery, Gate B7. Boarding pass? ...Lovely. {player}, 14A. You're in group... She looks at her screen. Well. We'll call your group.")
                .opt("What group am I in?", "group")
                .opt("Is the flight on time?", "time")
                .opt("Thanks.", "wait")
                .node("group", "Groups are set by SmileScore at the moment of boarding. It changes! That's the fun of it. Be nice to people for the next half hour and you might move up.")
                .cont("wait")
                .node("time", "Right on time. Meridian flights are always on time. Every flight that has ever left this gate has arrived. She pauses. Somewhere.")
                .cont("wait")
                .node("wait", "Take a seat, meet your fellow passengers. We'll begin boarding shortly.")
                .end("Okay.").flag("corinne_intro")
                .node("after", "Not long now! Grab a seat. Talk to people. People are rating you either way.")
                .end("[Leave]")
                .node("boarding", "Boarding pass on the scanner, please. Have a lovely flight.")
                .end("[Leave]"));

        add(tree("corinne_low", "Corinne")
                .node("start", "Oh. Oh dear. The scanner says your SmileScore is below three. That means a short secondary screening. It's nothing! Just a few questions.")
                .cont("q1")
                .node("q1", "Question one. On a scale of one to five, how happy are you, right now, in this moment?")
                .opt("Five.", "q2")
                .opt("Honestly? About a two.", "q2").rate(-0.1f)
                .opt("Is this really necessary?", "q2").rate(-0.2f)
                .node("q2", "Question two. Do you deserve to be on this flight?")
                .opt("Yes.", "q3")
                .opt("I paid for my ticket.", "q3")
                .opt("...I don't know.", "q3").remember("You told Corinne you didn't know if you deserved to fly.")
                .opt("[Say nothing]", "q3").rate(-0.1f)
                .timed(8, 3)
                .node("q3", "Wonderful! You pass. Everyone passes. Please proceed to the aircraft and enjoy your experience.")
                .end("[Walk to the jet bridge]").flag("scanned_pass").fx(c -> c.event("open_bridge")));

        add(tree("marcus", "Marcus")
                .start(v -> v.has("on_air_plane") ? "plane" : v.has("talked_marcus") ? "again" : "hello")
                .node("hello", "Sorry, sorry, I'm in your way. I'm always in somebody's way. Are you on the 417 too?")
                .opt("Yeah, 14A. You?", "seat")
                .opt("You seem nervous.", "nervous")
                .opt("[Nod and keep walking]", "brush").rate(-0.05f).remember("You brushed Marcus off.")
                .node("seat", "14C! We're neighbours! Well. One seat between us. That's practically neighbours.")
                .cont("nervous2")
                .node("nervous", "Is it that obvious? I hate flying. I've done it, what, three times? Four? Every time I swear it's the last.")
                .cont("nervous2")
                .node("nervous2", "My brother flew Meridian a few years ago. He sent one postcard and then nothing. I'm going to Port Ellery to find him. Or to find out.")
                .opt("I hope you find him.", "hope").rate(0.1f).remember("You were kind to Marcus.")
                .opt("Maybe he doesn't want to be found.", "harsh").rate(-0.1f).remember("You told Marcus his brother didn't want to be found.")
                .opt("What did the postcard say?", "postcard")
                .node("hope", "Thanks. That's... yeah. Thanks. I'm Marcus, by the way. Marcus Bell.")
                .cont("bye")
                .node("harsh", "...Yeah. Maybe. He glances at his phone. His rating just dropped. Yours did too.")
                .cont("bye")
                .node("postcard", "One line. 'The water is so blue here, and nobody rates anybody.' No stamp. It came in a Meridian envelope. I'm Marcus, by the way. Marcus Bell.")
                .cont("bye")
                .node("brush", "Right. Sorry. Sorry. He goes back to pacing.")
                .cont("bye")
                .node("bye", "See you on board, I guess.")
                .end("See you.").flag("talked_marcus")
                .node("again", "Do you think they check the planes? Like, really check them?")
                .end("[Leave]")
                .node("plane", "Are you okay? You look like you saw something. I keep hearing... clicking. In the walls. Like typing.")
                .end("It's probably nothing.")
                .opt("I hear it too.", null).remember("You admitted to Marcus that something was wrong."));

        add(tree("marcus_panic", "Marcus")
                .node("start", "This isn't normal! This isn't normal turbulence! {player}, are we going to die? Tell me we're not going to die!")
                .opt("[Grab his hand] \"We're going to be fine.\"", "hold").rate(0.2f).remember("You held Marcus's hand as the plane fell.")
                .opt("\"Shut up and brace!\"", "shut").rate(-0.1f).remember("You shouted at Marcus.")
                .opt("\"I don't know.\"", "dunno")
                .opt("[Say nothing]", "silent")
                .timed(7, 3)
                .node("hold", "His hand is shaking. Then it isn't. \"Okay. Okay. If I don't... find my brother for me. Nate. Nate Bell.\"")
                .end("[Brace]").flag("marcus_held")
                .node("shut", "He shuts up. He braces. He doesn't look at you again.")
                .end("[Brace]")
                .node("dunno", "\"Nobody knows,\" he whispers. \"That's the worst part. Nobody ever knows.\"")
                .end("[Brace]")
                .node("silent", "You say nothing. Marcus starts to pray, stops, and starts again.")
                .end("[Brace]"));

        add(tree("priya", "Priya")
                .start(v -> v.has("on_air_plane") ? "plane" : v.has("talked_priya") ? "again" : "hello")
                .node("hello", "Leo, sit DOWN. Sorry. He's been bouncing since four in the morning. Are you on our flight?")
                .opt("14A. Is he excited?", "excited")
                .opt("He's fine. Kids are kids.", "fine").rate(0.1f)
                .opt("Could you keep him a little quieter?", "quiet").rate(-0.2f).remember("You asked Priya to keep her son quiet.")
                .node("excited", "Excited is one word for it. He keeps telling me he's been on this plane before. In his dreams. He says there's an island.")
                .cont("island")
                .node("fine", "Bless you. You'd be amazed how many people rate us down for 'noise disturbance'. He's six.")
                .cont("island")
                .node("quiet", "...Of course. Sorry. We'll try. She pulls Leo close. The boy keeps staring at you.")
                .cont("island2")
                .node("island", "Leo, tell them about your dream. LEO: There's a beach and a fire and an old man with a beard who used to fly planes. And faces with no faces. PRIYA: Kids! She laughs, too loudly.")
                .cont("bye")
                .node("island2", "Leo leans around his mother and whispers: You're going to the island too.")
                .cont("bye")
                .node("bye", "Well. Have a good flight.")
                .end("You too.").flag("talked_priya")
                .node("again", "We're going to Port Ellery to see his grandmother. He's never met her. I've never met her either, actually. Isn't that strange? I never thought that was strange until now.")
                .end("[Leave]")
                .node("plane", "She's asleep with her eyes open. She doesn't blink. Her lips are moving: ...isn't that strange... isn't that strange...")
                .end("[Back away]"));

        add(tree("leo", "Leo")
                .start(v -> v.has("on_air_plane") ? "plane" : "hello")
                .node("hello", "Do you want to see my drawing? It's the plane. And that's the water. And that's you. In the water.")
                .opt("That's a great drawing.", null).rate(0.1f)
                .opt("Why am I in the water?", "why")
                .node("why", "Because that's where you always end up.")
                .end("[Leave]")
                .node("plane", "Mum's asleep. Everybody's asleep except you and me. And the lady in the back. She's counting.")
                .end("[Leave]"));

        add(tree("halvorsen", "Mr. Halvorsen")
                .start(v -> v.has("on_air_plane") ? "plane" : v.has("talked_halvorsen") ? "again" : "hello")
                .node("hello", "Four point nine one. Four point nine ONE. Do you know how hard it is to hold four point nine one through an airport? Everybody's a critic.")
                .opt("Congratulations?", "congrats")
                .opt("What do you get at five?", "five")
                .opt("Sounds exhausting.", "exhaust")
                .node("congrats", "Thank you. Rate me for that. Go on. I'll wait. He actually waits.")
                .opt("[Rate him five stars]", "given").rate(0.1f).remember("You gave Mr. Halvorsen five stars.")
                .opt("[Rate him one star]", "one").rate(-0.2f).remember("You gave Mr. Halvorsen one star.")
                .opt("I'm not doing that.", "no")
                .node("five", "At five? Everything. Lounges. Upgrades. People open doors before you reach them. At five, the world is a hotel. Below two... He lowers his voice. Below two, you just stop getting flights home.")
                .cont("end")
                .node("exhaust", "It's a full-time job. Eighteen hours a day of being pleasant. But it's worth it. It's worth it. ...It's worth it.")
                .cont("end")
                .node("given", "Lovely. And one for you. Your phone buzzes. You're not sure what he gave you.")
                .cont("end")
                .node("one", "His smile does not move at all. Interesting choice. Somewhere, a number changes.")
                .cont("end")
                .node("no", "Then why are we talking? He turns away.")
                .cont("end")
                .node("end", "Now if you'll excuse me, I have a lounge to be seen in.")
                .end("[Leave him to his phone]").flag("talked_halvorsen")
                .node("again", "Four point nine two now. Keep it up, everyone. Keep it up.")
                .end("[Leave]")
                .node("plane", "Four point nine nine. Four point nine nine. Almost there. Almost. There. His eyes are bloodshot. He hasn't looked away from his phone in hours.")
                .end("[Leave]"));
    }

    // =========================================================================================
    private static void plane() {
        add(tree("june", "June")
                .start(v -> {
                    if (q(v, "board")) return v.has("june_welcome") ? "generic" : "welcome";
                    if (q(v, "cruise")) return v.has("called_june") && !v.has("got_meal") ? "meal" : !v.has("called_june") ? "busy" : "generic";
                    if (q(v, "lights_out") && !v.has("june_stare")) return "stare";
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
                .node("who", "Captain Hale, of course. He's flown this route more times than anybody. More times than... well. Lots of times. Her smile stays exactly where it is. Chicken?")
                .opt("...Chicken.", "meal_end").fx(c -> c.give(ModItems.AIRLINE_MEAL.get(), 1))
                .node("meal_end", "Enjoy! Let me know if you need anything. I'm always watching the call lights. Always.")
                .end("Thanks.").flag("got_meal")
                .node("stare", "She is standing very still in the galley, hands folded. Please return to your seat, {player}. HALO is concerned about your SmileScore.")
                .opt("Who is HALO?", "halo")
                .opt("What is wrong with everyone?", "wrong")
                .opt("[Back away slowly]", "away")
                .node("halo", "Hospitality And Loyalty Optimiser. HALO looks after all of us. HALO is the reason we are always on time.")
                .cont("end")
                .node("wrong", "Nothing is wrong. Everyone is having a wonderful experience. You are having a wonderful experience. Please return to your seat.")
                .cont("end")
                .node("away", "She tilts her head. And keeps tilting it, a little further than a neck should go.")
                .cont("end")
                .node("end", "The seatbelt sign will be on shortly. Seat 14A, {player}. Now.")
                .end("[Go back to 14A]").flag("june_stare")
                .node("generic", "Can I get you anything? Water? A blanket? A better rating?")
                .end("No, thanks."));

        add(tree("attendant", "Theo")
                .node("start", "Rear galley's closed, sorry. Lavatory is just there. Please don't take too long in front of the mirror. People take too long in front of the mirror.")
                .end("Okay."));

        add(tree("ines", "Ines")
                .start(v -> v.has("talked_ines") ? "again" : "hello")
                .node("hello", "She doesn't look up. Her pencil keeps moving. You're going to ask what I'm drawing. Everyone does.")
                .opt("What are you drawing?", "show")
                .opt("I wasn't going to ask.", "show")
                .opt("Do you always sketch on flights?", "show")
                .node("show", "Here. She turns the sketchbook round. Page after page: a beach, a broken plane, huts around a fire, a grey box on a mountain. A figure in every picture. The figure has your face.")
                .opt("That's me. Why is that me?", "why")
                .opt("Have we met before?", "met")
                .opt("They're... very good.", "good")
                .node("why", "I don't know. I don't decide what I draw, I just draw it. I've been drawing you since before we took off.")
                .cont("end")
                .node("met", "Not out here. She taps her temple. In here, I think we've met four times.")
                .cont("end")
                .node("good", "They're accurate, she says. That's different from good.")
                .cont("end")
                .node("end", "Try to get some sleep. It's a long way down.")
                .end("[Look away]").flag("talked_ines")
                .node("again", "Page twelve is new. I haven't finished your face yet.")
                .end("[Leave her be]"));

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
                .end("[Leave]")
                .node("after", "Sit down, please. You're blocking the view. Their eyes are fixed on the back of your head, even while you're standing in front of them.")
                .end("[Leave]"));
    }

    // =========================================================================================
    private static void objects() {
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
                .node("end", "The screen goes back to the Meridian logo. A little smiling sun.")
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
                .node("note", "It's written in shaky capitals: THERE IS NO ONE FLYING THIS PLANE. THERE NEVER WAS. DON'T LET THEM SEE YOU READ THIS.")
                .end("[Keep the note]").flag("knocked_cockpit").fx(c -> c.give(ModItems.CRUMPLED_NOTE.get(), 1))
                .node("after", "Silence on the other side. The kind of silence that listens.")
                .end("[Step away]")
                .node("locked", "A sign on the door: FLIGHT DECK. CREW ONLY. The door is locked.")
                .end("[Step away]"));

        add(tree("lone_seat", "You")
                .node("start", "The seat faces the sea. 14A, says the tag on the headrest. Something is wedged into the cushion: a photograph.")
                .opt("[Take the photo]", "photo")
                .node("photo", "It's you, face down on this beach, taken from above. From high above. On the back, in neat handwriting: 'Day 1. Iteration 0417. Smiling is optional now.'")
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
                .end("[Cast off]").flag("cast_off")
                .end("[Not yet]"));
    }

    // =========================================================================================
    private static void island() {
        add(tree("nalu", "Nalu")
                .start(v -> {
                    if (!v.has("met_nalu")) return "stop";
                    if (q(v, "trials") && !v.has("trial_fish")) return "fish";
                    if (q(v, "night_watch")) return "watch";
                    if (q(v, "farewell") && !v.has("bye_nalu")) return "bye";
                    return "generic";
                })
                .node("stop", "Stop. Right there. Hands where I can see them. A man with a painted face is pointing a spear at your chest. Another one. Fell out of the sky, did you? Washed up with the metal?")
                .opt("I was on a plane. It crashed. I'm the only one.", "only")
                .opt("Put that down. I'm not dangerous.", "notdanger").rate(-0.1f)
                .opt("[Raise your hands. Say nothing.]", "silent").remember("You kept quiet when Nalu stopped you.")
                .timed(10, 2)
                .node("only", "They're always the only one. The spear dips a little. Every time, the only one.")
                .cont("name")
                .node("notdanger", "Everything from the sky is dangerous. You bring the Eye with you.")
                .cont("name")
                .node("silent", "Good. The quiet ones live longer here. He lowers the spear.")
                .cont("name")
                .node("name", "I'm Nalu. I hunt for the village. The elder will want to see you. Walk in front of me.")
                .end("Lead the way.").flag("met_nalu")
                .opt("What village?", "village")
                .node("village", "The Tidefolk. What's left of us. Go. The longhouse, on the north side.")
                .end("[Walk]").flag("met_nalu")
                .node("fish", "You want to prove you're not just another mouth? Bring me five fish. Any kind.")
                .opt("[Hand over five fish]", "fishdone").when(v -> v.counter("item:tag:minecraft:fishes") >= 5).rate(0.1f)
                .fx(c -> { c.takeTag(ItemTags.FISHES, 5); c.flag("trial_fish"); })
                .opt("I don't have anything to fish with.", "rod").when(v -> !v.has("got_rod"))
                .end("I'll get them.")
                .node("rod", "Here. Bone hook, good line. Don't lose it on the reef. Fish bite best off the village beach.")
                .end("Thanks.").flag("got_rod").fx(c -> c.give(Items.FISHING_ROD, 1))
                .node("fishdone", "...Good fish. Clean. Maybe you're not useless after all.")
                .end("[Leave]")
                .node("watch", "Stay by the fire. When you see one, keep your eyes on it and call out. They don't move while they're watched. Two of us watching is twice the chance.")
                .opt("Do you have anything that can help?", "flares").when(v -> !v.has("got_flares"))
                .end("Understood.")
                .node("flares", "These came out of the sky with the last one. Red fire. They hate it.")
                .end("Thanks, Nalu.").flag("got_flares").fx(c -> c.give(ModItems.FLARE.get(), 3))
                .node("bye", "So. You're going. He doesn't look at you. My brother would have liked a boat like that. He was always going to come and find me.")
                .opt("Your brother... was his name Marcus? Marcus Bell?", "marcus").when(v -> v.has("talked_marcus"))
                .opt("I'll send someone back for you.", "back")
                .opt("Goodbye, Nalu.", "farewell")
                .node("marcus", "Nalu goes very still. Marcus. Nobody has said that name out loud in years. Not since I forgot my own. ...You met him?")
                .opt("He was on my flight. He was looking for you.", "marcus2").remember("You told Nalu his brother had come looking for him.")
                .opt("I must be mistaken.", "farewell")
                .node("marcus2", "Then he's here. Somewhere. Or he will be, next time. He grips your arm. If you get out, tell people. Tell them what Meridian does with its passengers.")
                .end("I will.").flag("bye_nalu")
                .node("back", "Nobody comes back. Just go, and don't look round.")
                .end("[Leave]").flag("bye_nalu")
                .node("farewell", "Go well, sky-fallen.")
                .end("[Leave]").flag("bye_nalu")
                .node("generic", "Tide's turning. So is the weather. Keep your eyes open after dark.")
                .end("[Leave]"));

        add(tree("ahu", "Elder Ahu")
                .start(v -> {
                    if (!v.has("met_ahu")) return "meet";
                    if (q(v, "welcome") && !v.has("welcomed")) return "welcome";
                    if (q(v, "the_eye") && !v.has("ahu_eye")) return "eye";
                    if (q(v, "farewell") && !v.has("bye_ahu")) return "bye";
                    return "generic";
                })
                .node("meet", "An old man with a long white beard sits under a cloak stitched from airline blankets. A faded pair of pilot's wings is pinned to his chest. Sit. You'll have questions. I had questions, once. Ask one.")
                .opt("Where am I?", "where")
                .opt("Who are you people?", "who")
                .opt("Is anyone coming to rescue me?", "rescue")
                .node("where", "An island. It isn't on any map, because the people who draw the maps own it.")
                .cont("trial")
                .node("who", "The Tidefolk. Sky-fallen like you, and the children of sky-fallen. We stopped counting the years. Counting is how they get you.")
                .cont("trial")
                .node("rescue", "He laughs, softly. Child. You are being watched by millions. Rescue would spoil the ending.")
                .cont("trial")
                .node("trial", "You may stay if you give as well as take. Help three of us. Nalu needs fish. Mira needs herbs. Teo needs wood. Do this and you are one of us.")
                .end("I'll do it.").rate(0.1f).flag("met_ahu")
                .opt("And if I don't?", "refuse")
                .node("refuse", "Then the night will decide for you. It usually does.")
                .end("[Leave]").flag("met_ahu")
                .node("welcome", "Nalu says you fish badly but honestly. Mira says your hands are steady. Teo says you never complain. Take this. He hands you a carved charm. Wear it. The Unrated know this mark. Sometimes it makes them hesitate.")
                .end("Thank you, Elder.").flag("welcomed").fx(c -> c.give(ModItems.TIDEFOLK_CHARM.get(), 1))
                .node("eye", "You've seen the grey house on the mountain. Pip draws it every day. We call it the Eye. I climbed up there once, long ago, when I still had my name. I came down knowing too much and remembering too little.")
                .opt("What's up there?", "up")
                .opt("What was your name?", "name")
                .node("up", "Machines. Screens. A voice that talks like a friend. It will tell you the truth, and the truth will not set you free. But it keeps what you'll need if you want to leave.")
                .cont("go")
                .node("name", "He touches the wings on his chest. Hale. Captain Hale. I flew the first one.")
                .cont("go").remember("Ahu told you he was once Captain Hale.")
                .node("go", "The mountain is north of the spring. Take a light. The Unrated gather near the summit after dark.")
                .end("I'll go.").flag("ahu_eye")
                .node("bye", "So the boat floats. He looks at the sea for a long time. I flew Flight 417 the first time. A real plane, real people. They filmed it. People liked it. So they made it again. And again.")
                .opt("Come with me. All of you.", "come")
                .opt("Why do they keep doing it?", "why")
                .node("why", "Because you watched. Everyone watched. Everyone rated.")
                .cont("pip")
                .node("come", "We're too many for one boat, and too strange for the world out there. It would rate us all a zero.")
                .cont("pip")
                .node("pip", "But Pip. Pip was born here. Pip has never been rated, never watched by anything but us. Take the child, {player}. Show Pip a world that isn't this.")
                .opt("I'll take Pip with me.", "yes").flag("pip_comes").remember("You promised Ahu you would take Pip with you.")
                .opt("It's too dangerous. Pip stays with you.", "no").remember("You left Pip on the island.")
                .node("yes", "Thank you. He rests his hand on your head, like a blessing.")
                .end("Goodbye, Captain.").flag("bye_ahu")
                .node("no", "Perhaps you're right. Perhaps you're kind. It's hard to tell the difference here.")
                .end("Goodbye, Captain.").flag("bye_ahu")
                .node("generic", "Sit with an old man for a while. Or don't. The sea doesn't mind either way.")
                .end("[Leave]"));

        add(tree("mira", "Mira")
                .start(v -> {
                    if (q(v, "trials") && !v.has("trial_herbs")) return "herbs";
                    if (q(v, "welcome") && !v.has("mira_unrated")) return "unrated";
                    if (q(v, "missing_pip") && !v.has("mira_pip")) return "pip";
                    if (q(v, "farewell") && !v.has("bye_mira")) return "bye";
                    return "generic";
                })
                .node("herbs", "A woman in a faded red neckerchief is sorting leaves. The neckerchief has a small sun stitched on it: the Meridian logo. Herbs, then. Bitterleaf: broad dark leaves, grows where the canopy is thick. I need six. It keeps the fever away.")
                .opt("[Hand over six bitterleaf]", "herbsdone").when(v -> v.counter("item:airlinestory:bitterleaf") >= 6).rate(0.1f)
                .fx(c -> { c.take(com.airlinestory.registry.ModBlocks.BITTERLEAF.get(), 6); c.flag("trial_herbs"); c.give(ModItems.AIRLINE_BLANKET.get(), 2); })
                .opt("That scarf. You were cabin crew.", "scarf")
                .end("I'll find some.")
                .node("scarf", "Was I? She touches it. I found it. Or it found me. I don't remember a life before the fire, and I'd rather not.")
                .end("[Leave]")
                .node("herbsdone", "Good leaves. You didn't tear them. Here, take these blankets. The nights get cold, and you'll want them for something one day. Everyone does.")
                .end("Thanks, Mira.")
                .node("unrated", "The faces in the dark? We call them the Unrated. They were people once. Sky-fallen who gave up. Who stopped trying, stopped speaking, stopped being looked at. Their faces just... went.")
                .opt("How do I fight them?", "fight")
                .opt("Could that happen to me?", "me")
                .node("fight", "You don't fight them. You look at them. They can't move while someone is watching. Fire helps. Red fire most of all.")
                .cont("recipe")
                .node("me", "It happens to anyone who stops being seen. That's what this whole place is for.")
                .cont("recipe")
                .node("recipe", "Take these: ghost tonic. Ghost orchid and bitterleaf in fresh water. You'll see in the dark like an owl. The orchids glow in the west, where we don't go.")
                .end("Thank you.").flag("mira_unrated").fx(c -> c.give(ModItems.GHOST_TONIC.get(), 2))
                .node("pip", "Pip's gone! The fire watch saw nothing. Nalu saw nothing. There are little footprints going west, into the Grove. Her hands are shaking. We don't go into the Grove. Not ever. Please.")
                .opt("I'll bring Pip back.", "go").rate(0.2f).remember("You promised Mira you would bring Pip back.")
                .opt("Why won't anyone else go?", "why")
                .node("why", "Because the Grove is where they keep the pictures. And looking at the pictures is how you become one of them.")
                .cont("go")
                .node("go", "West, past the spring. Where the trees have no leaves. Hurry.")
                .end("[Go west]").flag("mira_pip")
                .node("bye", "You're really going. She unties the neckerchief and wraps it round your wrist. In case it was mine. In case somebody out there knows the logo.")
                .opt("I'll find out who you were.", null).flag("bye_mira").remember("You promised Mira you'd find out who she was.")
                .opt("Goodbye, Mira.", null).flag("bye_mira")
                .node("generic", "Bitterleaf keeps the fever away. Cook taro before you eat it. And drink from the spring, not the sea, unless you want to see things.")
                .end("[Leave]"));

        add(tree("teo", "Teo")
                .start(v -> {
                    if (q(v, "trials") && !v.has("trial_wood")) return "wood";
                    if (q(v, "boat_plan")) return "plan";
                    if (q(v, "boat_build")) return "build";
                    return "generic";
                })
                .node("wood", "A wiry man is sanding a paddle with a handful of sand. Planks. Twelve palm planks, good straight ones. The longhouse roof eats wood the way the sea eats boats.")
                .opt("[Hand over twelve palm planks]", "wooddone").when(v -> v.counter("item:airlinestory:palm_planks") >= 12).rate(0.1f)
                .fx(c -> { c.take(com.airlinestory.registry.ModBlocks.PALM_PLANKS.get(), 12); c.flag("trial_wood"); })
                .opt("You build boats?", "boats")
                .end("I'll bring them.")
                .node("boats", "Half-boats. Every time I finish one, something sinks it in the night. The Eye doesn't like boats. And I never knew which way to steer.")
                .end("[Leave]")
                .node("wooddone", "Straight. Mostly. Thanks.")
                .end("[Leave]")
                .node("plan", "Back again. Got something for me?")
                .opt("[Show him the navigation chart]", "plan2").when(v -> v.counter("item:airlinestory:navigation_chart") >= 1)
                .end("Not yet.")
                .node("plan2", "That chart... That's the way through the reef. That's the way OUT. With this I can build you a boat that'll make it. But it's your boat. You do the carrying, I do the shouting. He hands you a blueprint scratched into bark.")
                .end("Let's build it.").flag("boat_plan").fx(c -> c.give(ModItems.BOAT_BLUEPRINT.get(), 1))
                .node("build", "The frame's at the end of the dock. Bring what the blueprint says, one stage at a time: keel, hull, mast, sail, provisions. Right-click the frame with the materials in your pack.")
                .end("Got it.")
                .node("generic", "Sea's calm today. That's when it's most dangerous.")
                .end("[Leave]"));

        add(tree("pip", "Pip")
                .start(v -> {
                    if (q(v, "welcome") && !v.has("pip_drawing")) return "drawing";
                    if (q(v, "missing_pip") && !v.has("found_pip")) return "found";
                    if (v.has("pip_comes")) return "coming";
                    return "generic";
                })
                .node("drawing", "A small child with a messy braid tugs your sleeve. Look! I drew you! It's a drawing on bark: you, standing on a mountain beside a big grey box with an eye painted on it. That's the Eye. It's always looking. I wave at it every morning.")
                .opt("Why do you wave at it?", "wave")
                .opt("That's a great drawing, Pip.", "great").rate(0.1f)
                .node("wave", "So it knows I'm being good.")
                .end("[Ruffle Pip's hair]").flag("pip_drawing")
                .node("great", "I know.")
                .end("[Ruffle Pip's hair]").flag("pip_drawing")
                .node("found", "Pip is sitting in an airplane seat in front of a wall of photographs, very still. They brought me to see the pictures. You're in all of them. Look, that's you on the beach. That's you sleeping. That's you a long time ago. Before.")
                .opt("Pip, we have to go. Now.", "go")
                .opt("Who brought you here?", "who")
                .node("who", "The ones without faces. They weren't mean. They just wanted someone to look at the pictures with them.")
                .cont("go")
                .node("go", "Okay. Hold my hand? Don't look at the trees.")
                .end("[Take Pip's hand]").flag("found_pip").fx(c -> c.event("pip_follow"))
                .node("coming", "Are we really going on the boat? Is the world big? Is it bigger than the island? Does it have more Eyes?")
                .end("[Smile]")
                .node("generic", "I'm not scared of the Grove any more. Only a little. Only at night.")
                .end("[Leave]"));

        add(tree("tribe_0", "Tidefolk")
                .node("start", "You smell like the metal. Like the sky. You'll smell like smoke and salt soon enough.")
                .end("[Leave]"));
        add(tree("tribe_1", "Tidefolk")
                .node("start", "Don't count the days. Ahu says counting is how they find you. I lost count a long time ago. It's better.")
                .end("[Leave]"));
        add(tree("tribe_2", "Tidefolk")
                .node("start", "Sometimes at night you can hear a chime from the mountain. Ding-dong. Like a door opening. Then a voice says thank you. Thank you for flying.")
                .end("[Leave]"));
    }

    // =========================================================================================
    private static void station() {
        add(tree("keypad", "Keypad")
                .start(v -> v.has("door_open") ? "open" : "pad")
                .node("pad", "A keypad beside a heavy steel door. MERIDIAN EXPERIENCES - LAB 4. AUTHORISED PERSONNEL ONLY. It wants four digits.")
                .opt("1 - 4 - 0 - 1", "wrong")
                .opt("0 - 4 - 1 - 7", "right")
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
                .node("log2", "ITERATION 4 NOTES: subject consented to a Rehabilitation Experience after their SmileScore fell to 1.9. Memory wipe successful. Subject believes they are travelling to Port Ellery. There is no Port Ellery.")
                .opt("[Back]", "start")
                .node("files", "SUBJECT FILES. Select a file.")
                .opt("[R. HALE - 'Ahu']", "f_ahu")
                .opt("[J. OKAFOR - 'Mira']", "f_mira")
                .opt("[N. BELL - 'Nalu']", "f_nalu")
                .opt("[More files...]", "files2")
                .node("files2", "SUBJECT FILES (2).")
                .opt("[NO RECORD - 'Pip']", "f_pip")
                .opt("[417-D - YOU]", "f_you")
                .opt("[Back]", "start")
                .node("f_ahu", "R. HALE. Captain, Meridian Air. Iteration 1. Status: retained as set dressing. Note: viewers respond well to the beard.")
                .opt("[Back]", "files")
                .node("f_mira", "J. OKAFOR. Cabin crew, Iteration 2. Enrolled after a 1.4 rating for 'insufficient warmth'. Memory: wiped. Status: healer. Viewer favourite.")
                .opt("[Back]", "files")
                .node("f_nalu", "N. BELL. Iteration 3, 'Family Drama' package. Brother M. BELL booked onto Iteration 4 to maximise reunion potential. Reunion status: pending.")
                .opt("[Back]", "files")
                .node("f_pip", "NO RECORD. Born on site during Iteration 2. Never rated. ANOMALY: unrated persons cannot be scored. Recommendation: removal.")
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

    // =========================================================================================
    private static void extras() {
        String[][] travelers = {
                {"Traveler", "Is it me or is the PA a bit... personal today? It said my name. I never gave anyone my name."},
                {"Traveler", "I've been at this airport for, what, three days? Four? My flight keeps saying ON TIME."},
                {"Traveler", "Don't stand too close. My rating can't take another 'invaded personal space'."},
                {"Traveler", "Meridian? Brave. My cousin took Meridian. Lovely airline. Lovely, lovely airline. Haven't heard from her."},
                {"Traveler", "Smile! They're filming. They're always filming. Smile with your teeth."},
                {"Traveler", "Excuse me, do you know which gate Port Ellery is? I've asked six people and they all pointed at you."},
        };
        for (int i = 0; i < travelers.length; i++) {
            add(tree("traveler_" + i, travelers[i][0])
                    .node("start", travelers[i][1])
                    .opt("[Smile politely]", null).rate(0.02f)
                    .end("[Leave]"));
        }
        add(tree("janitor", "Janitor")
                .node("start", "Mind the floor, it's wet. It's always wet by B7. Seawater, smells like. Comes up through the tiles. I mop it every morning.")
                .end("[Leave]"));
        add(tree("clerk", "Shop Clerk")
                .node("start", "Duty free! Perfume, sunglasses, sunscreen. Lots of sunscreen. Everybody on the 417 buys sunscreen. Factor fifty. They'll need it.")
                .end("Just looking."));
    }
}
