package com.airlinestory.dialogue;

import com.airlinestory.registry.ModBlocks;
import com.airlinestory.registry.ModItems;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import static com.airlinestory.dialogue.Dialogue.tree;
import static com.airlinestory.dialogue.Dialogues.add;
import static com.airlinestory.dialogue.Dialogues.q;

/** Act III: the Tidefolk. */
final class IslandTalk {
    private IslandTalk() {}

    static void register() {
        nalu();
        ahu();
        mira();
        teo();
        pip();
        String[] tribe = {
                "You smell like the metal. Like the sky. You'll smell like smoke and salt soon enough.",
                "Don't count the days. Ahu says counting is how they find you. I lost count a long time ago. It's better.",
                "Sometimes at night you can hear a chime from the mountain. Ding-dong. Like a door opening. Then a voice says thank you. Thank you for flying.",
                "I dreamed of a room full of chairs, all facing the same way, and everyone in them was laughing at me. Ahu says that was before. I don't want before.",
        };
        for (int i = 0; i < tribe.length; i++) {
            add(tree("tribe_" + i, "Tidefolk").node("start", tribe[i]).end("[Leave]"));
        }
    }

    private static void nalu() {
        add(tree("nalu", "Nalu")
                .start(v -> {
                    if (!v.has("met_nalu")) return "stop";
                    if (q(v, "trials") && !v.has("trial_fish")) return "fish";
                    if (q(v, "night_watch") && !v.has("watch_ready")) return "watch";
                    if (q(v, "night_watch")) return "watching";
                    if (q(v, "what_we_were") && !v.has("nalu_truth")) return "truth";
                    if (q(v, "farewell") && !v.has("bye_nalu")) return "bye";
                    if (v.has("side:hunt") && !v.has("sidedone:hunt")) return v.counter("boars") >= 3 ? "hunt_done" : "hunting";
                    if (v.has("welcomed") && !v.has("side:hunt") && !v.has("bye_nalu")) return "hunt";
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
                .node("name", "I'm Nalu. I hunt for the village. The elder will want to see you. The longhouse, north side. Walk in front of me.")
                .end("Lead the way.").flag("met_nalu")
                .opt("What village?", "village")
                .node("village", "The Tidefolk. What's left of us. Go. The longhouse.")
                .end("[Walk]").flag("met_nalu")
                .node("fish", "You want to prove you're not just another mouth? Bring me five fish. Any kind.")
                .opt("[Hand over five fish]", "fishdone").when(v -> v.counter("item:tag:minecraft:fishes") >= 5).rate(0.1f)
                .fx(c -> { c.takeTag(ItemTags.FISHES, 5); c.flag("trial_fish"); })
                .opt("I don't have anything to fish with.", "rod").when(v -> !v.has("got_rod"))
                .end("I'll get them.")
                .node("rod", "Here. Bone hook, good line. Don't lose it on the reef. Fish bite best off the village beach.")
                .end("Thanks.").flag("got_rod").fx(c -> c.give(Items.FISHING_ROD, 1))
                .node("fishdone", "...Good fish. Clean. Maybe you're not useless after all. Here. Don't cut yourself.")
                .end("[Take the knife]").fx(c -> c.give(ModItems.STONE_KNIFE.get(), 1))
                .node("watch", "Tonight they come. On nights with no moon, the Unrated walk down from the Grove and stand at the edge of the firelight. Stand watch with me. When you see one, keep your eyes on it. They don't move while they're watched.")
                .opt("I'm ready.", "ready")
                .opt("Do you have anything that can help?", "flares").when(v -> !v.has("got_flares"))
                .end("Give me a minute.")
                .node("flares", "These came out of the sky with the last one. Red fire. They hate it. Light one and throw it, or hold it high.")
                .opt("I'm ready.", "ready").flag("got_flares").fx(c -> c.give(ModItems.FLARE.get(), 3))
                .node("ready", "Then we wait for dark. Stay by the fire. Don't wander. Don't sleep.")
                .end("[Take your place by the fire]").flag("watch_ready")
                .node("watching", "Eyes open. Eyes on the dark.")
                .end("[Leave]")
                .node("truth", "You found something in the mangroves. I can see it on you. Show me.")
                .opt("[Show him Nate's letter]", "letter")
                .opt("Not yet.", null)
                .node("letter", "He reads it. He reads it again. His lips move on the name. Nate. Nate Bell. ...That's me, isn't it? I wrote this. I don't remember the hand that wrote it.")
                .opt("Your brother Marcus was on my flight.", "marcus").when(v -> v.has("talked_marcus"))
                .opt("You lived in the mangroves, before the fire.", "fire")
                .node("marcus", "Marcus. Nalu goes very still. Little Marcus. He used to follow me everywhere. He followed me here, didn't he? They brought him here to follow me. The Family Drama package.")
                .opt("He held my hand when the plane went down.", "held").when(v -> v.has("marcus_held"))
                .opt("I'm sorry.", "sorry")
                .node("held", "Then he wasn't alone. That's... more than I had. He wipes his face with the back of his wrist. Thank you, sky-fallen. Thank you.")
                .end("[Leave him with the letter]").flag("nalu_truth").remember("You told Nalu his brother had come looking for him.")
                .node("sorry", "Don't be. Be angry. I've forgotten how. He folds the letter into his belt.")
                .end("[Leave]").flag("nalu_truth").remember("You told Nalu his brother had come looking for him.")
                .node("fire", "There were six of us. A family. The fire came at night, from nowhere, while the screens on the mountain blinked. The others walked into the Grove after. I came here and forgot. It was easier.")
                .end("[Leave]").flag("nalu_truth")
                .node("hunt", "The storms are coming. We need meat smoked before then. Can you hunt, sky-fallen, or do you only eat what the metal birds bring?")
                .opt("I can hunt.", "hunt_go").fx(c -> c.director().startSide(c.player(), "hunt"))
                .opt("Another time.", null)
                .node("hunt_go", "Three pigs. They root in the jungle and the high grass. Bring the tusks, or just come back with blood on you. I'll believe it.")
                .end("[Leave]")
                .node("hunting", "Three pigs. The jungle, the high grass. Go quiet, strike fast.")
                .end("[Leave]")
                .node("hunt_done", "Three. He grins, for the first time since you met him. You hunt like someone who's done it before. Maybe you have. Here: wear this. It tells the young ones you're no stranger.")
                .end("[Take the tusk]").rate(0.1f).flag("hunt_done").fx(c -> c.give(ModItems.BOAR_TUSK.get(), 1))
                .node("bye", "So. You're going. He doesn't look at you. If you get out, find Marcus. If he made it. If anyone ever makes it.")
                .opt("I'll send someone back for you.", "back")
                .opt("Come with me, Nate.", "come").when(v -> v.has("nalu_truth"))
                .opt("Goodbye, Nalu.", "farewell")
                .node("come", "Nate died in the fire. Nalu stays with the village. He grips your arm. Tell people what Meridian does with its passengers. That's how you bring me with you.")
                .end("I will.").flag("bye_nalu")
                .node("back", "Nobody comes back. Just go, and don't look round.")
                .end("[Leave]").flag("bye_nalu")
                .node("farewell", "Go well, sky-fallen.")
                .end("[Leave]").flag("bye_nalu")
                .node("generic", "Tide's turning. So is the weather. Keep your eyes open after dark.")
                .end("[Leave]"));
    }

    private static void ahu() {
        add(tree("ahu", "Elder Ahu")
                .start(v -> {
                    if (!v.has("met_ahu")) return "meet";
                    if (q(v, "welcome") && !v.has("welcomed")) return v.has("trial_fish") && v.has("trial_herbs") && v.has("trial_wood") ? "welcome" : "generic";
                    if (q(v, "echoes") && !v.has("ahu_echoes")) return "echoes";
                    if (q(v, "what_we_were") && !v.has("ahu_eye")) return v.has("nalu_truth") && v.has("mira_truth") ? "eye" : "wait_others";
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
                .node("echoes", "Pip is home. You walked into the Grove for a child who isn't yours. He is quiet for a long time. Then you've earned the truth, or the part of it I still have. You are not the first sky-fallen. You are the fourth.")
                .opt("The fourth?", "fourth")
                .opt("I know. The tapes, the call sheet. Iteration four.", "fourth").when(v -> v.has("clue:call_sheet") || v.has("clue:tape_2"))
                .node("fourth", "The first was a pilot. He lived in the cave in the western cliffs and talked into a machine every night. The second plane sank in the lagoon; its crew came ashore and stayed. The third was a family, who built huts in the mangroves to the north. Until the fire.")
                .opt("What happened to them?", "happened")
                .node("happened", "Go and see. Go to the cave, the lagoon, the mangroves. Whatever they left is still there. The island doesn't throw anything away. And {player}... bring back what you find. Some of us have forgotten more than we should have.")
                .end("I'll go.").flag("ahu_echoes")
                .node("wait_others", "Speak to Nalu and Mira first. What you carry belongs to them before it belongs to me.")
                .end("[Leave]")
                .node("eye", "You've seen the grey house on the mountain. Pip draws it every day. We call it the Eye. You found my tapes, didn't you? He touches the wings on his chest.")
                .opt("You're Captain Hale.", "hale")
                .opt("What's up there?", "up")
                .node("hale", "Robert Hale. I flew the first one. A real plane, real people, a real storm that wasn't real. They filmed it. People liked it. So they made it again. And again. And again.")
                .cont("up").remember("Ahu told you he was once Captain Hale.")
                .node("up", "Machines. Screens. A voice that talks like a friend. It will tell you the truth, and the truth will not set you free. But it keeps the chart of the reef, and without the chart, no boat gets past the breakers.")
                .cont("code")
                .node("code", "The door wants a number. I sat in front of it for a year before I understood. It's always the flight number, child. It is always the flight number.")
                .end("Zero, four, one, seven.").flag("ahu_eye")
                .node("bye", "So the boat floats. He looks at the sea for a long time. Flight 417 went down four times, and every time I swore it would be the last.")
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
    }

    private static void mira() {
        add(tree("mira", "Mira")
                .start(v -> {
                    if (q(v, "trials") && !v.has("trial_herbs")) return "herbs";
                    if (q(v, "welcome") && !v.has("mira_unrated")) return "unrated";
                    if (q(v, "missing_pip") && !v.has("mira_pip")) return "pip";
                    if (q(v, "what_we_were") && !v.has("mira_truth")) return "truth";
                    if (q(v, "farewell") && !v.has("bye_mira")) return "bye";
                    if (v.has("side:remedy") && !v.has("sidedone:remedy")) return "remedy_wait";
                    if (v.has("mira_unrated") && !v.has("side:remedy") && !v.has("bye_mira")) return "remedy";
                    return "generic";
                })
                .node("herbs", "A woman in a faded red neckerchief is sorting leaves. The neckerchief has a small sun stitched on it: the Meridian logo. Herbs, then. Bitterleaf: broad dark leaves, grows where the canopy is thick. I need six. It keeps the fever away.")
                .opt("[Hand over six bitterleaf]", "herbsdone").when(v -> v.counter("item:airlinestory:bitterleaf") >= 6).rate(0.1f)
                .fx(c -> { c.take(ModBlocks.BITTERLEAF.get(), 6); c.flag("trial_herbs"); c.give(ModItems.AIRLINE_BLANKET.get(), 2); })
                .opt("That scarf. You were cabin crew.", "scarf")
                .end("I'll find some.")
                .node("scarf", "Was I? She touches it. I found it. Or it found me. I don't remember a life before the island, and I'd rather not.")
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
                .node("remedy", "I'm nearly out of tonic. The orchids grow around the Grove, where none of us will go. If you're brave or stupid enough, bring me three. I won't ask which.")
                .opt("I'll bring them.", null).fx(c -> c.director().startSide(c.player(), "remedy"))
                .end("Not now.")
                .node("remedy_wait", "Three ghost orchids, from the west. Mind the trees. They watch.")
                .opt("[Give her three ghost orchids]", "remedy_done").when(v -> v.counter("item:airlinestory:ghost_orchid") >= 3)
                .fx(c -> c.take(ModBlocks.GHOST_ORCHID.get(), 3))
                .end("[Leave]")
                .node("remedy_done", "You went into the Grove. And came back with your face. She brews quickly, and pushes three warm bottles into your hands. For the nights. You'll need them more than I will.")
                .end("Thank you, Mira.").rate(0.1f).flag("remedy_done").fx(c -> c.give(ModItems.GHOST_TONIC.get(), 3))
                .node("pip", "Pip's gone! The fire watch saw nothing. Nalu saw nothing. There are little footprints going west, into the Grove. Her hands are shaking. We don't go into the Grove. Not ever. Please.")
                .opt("I'll bring Pip back.", "go").rate(0.2f).remember("You promised Mira you would bring Pip back.")
                .opt("Why won't anyone else go?", "why")
                .node("why", "Because the Grove is where they keep the pictures. And looking at the pictures is how you become one of them.")
                .cont("go")
                .node("go", "West, far west, past the river and the cliffs. Where the trees have no leaves. Hurry.")
                .end("[Go west]").flag("mira_pip")
                .node("truth", "You've been in the lagoon. You smell of it. What did you find?")
                .opt("A diary. June Okafor's. Cabin crew, Iteration 2.", "diary").when(v -> v.has("clue:okafor_diary"))
                .opt("The sunken plane. There was a cabin attendant called June.", "diary")
                .node("diary", "June. She says it slowly, like a word from a language she used to speak. June. My mother had an operation. I signed something. I was going to send her the money...")
                .opt("You're June Okafor.", "june")
                .opt("Pip was born here, wasn't Pip?", "pip_born")
                .node("june", "I was. I was a June. There's a new June now, isn't there? On the new plane. Smiling. She laughs, and it turns into something else. I hope she gets her fee.")
                .cont("pip_born")
                .node("pip_born", "The first year. I don't remember the father. I remember the night: the screens on the mountain all lit up, and a voice said 'congratulations'. Pip has never been rated. It's the only thing I've ever done that they couldn't score.")
                .end("[Hold her hand a moment]").flag("mira_truth").remember("You told Mira who she used to be.")
                .node("bye", "You're really going. She unties the neckerchief and wraps it round your wrist. In case somebody out there knows the logo. In case somebody remembers June.")
                .opt("I'll tell them who you were.", null).flag("bye_mira").remember("You promised Mira you'd tell the world who she was.")
                .opt("Goodbye, Mira.", null).flag("bye_mira")
                .node("generic", "Bitterleaf keeps the fever away. Cook taro before you eat it. And drink from the river, not the sea, unless you want to see things.")
                .end("[Leave]"));
    }

    private static void teo() {
        add(tree("teo", "Teo")
                .start(v -> {
                    if (q(v, "trials") && !v.has("trial_wood")) return "wood";
                    if (q(v, "boat_plan")) return "plan";
                    if (q(v, "boat_build")) return "build";
                    if (q(v, "farewell") && !v.has("bye_teo")) return "bye";
                    return "generic";
                })
                .node("wood", "A wiry man is sanding a paddle with a handful of sand. Planks. Twelve palm planks, good straight ones. The longhouse roof eats wood the way the sea eats boats.")
                .opt("[Hand over twelve palm planks]", "wooddone").when(v -> v.counter("item:airlinestory:palm_planks") >= 12).rate(0.1f)
                .fx(c -> { c.take(ModBlocks.PALM_PLANKS.get(), 12); c.flag("trial_wood"); })
                .opt("You build boats?", "boats")
                .end("I'll bring them.")
                .node("boats", "Half-boats. Every time I finish one, something sinks it in the night. The Eye doesn't like boats. And I never knew which way to steer, past the reef.")
                .end("[Leave]")
                .node("wooddone", "Straight. Mostly. Thanks.")
                .end("[Leave]")
                .node("plan", "Back again. Got something for me?")
                .opt("[Show him the navigation chart]", "plan2").when(v -> v.counter("item:airlinestory:navigation_chart") >= 1)
                .end("Not yet.")
                .node("plan2", "That chart... That's the way through the reef. That's the way OUT. With this I can build you a boat that'll make it. But it's your boat. You do the carrying, I do the shouting. He hands you a blueprint scratched into bark.")
                .end("Let's build it.").flag("boat_plan").fx(c -> c.give(ModItems.BOAT_BLUEPRINT.get(), 1))
                .node("build", "The frame's at the end of the dock. Bring what the blueprint says, one stage at a time: keel, hull, mast, sail, then the engine and provisions. Right-click the frame with the materials in your pack.")
                .opt("Where do I get hull panels?", "panels")
                .opt("How do I make a mast?", "mast")
                .end("Got it.")
                .node("panels", "From the metal bird you came in on. Break the wreck. It comes apart in sheets.")
                .cont("build")
                .node("mast", "Three palm logs stood in a line, rope beside. On a crafting table. Even a sky-fallen can do it.")
                .cont("build")
                .node("bye", "She'll hold. I promise she'll hold. He pats the hull. I always wanted to see one of mine reach the horizon. You'll do that for me.")
                .end("Thank you, Teo.").flag("bye_teo")
                .node("generic", "Sea's calm today. That's when it's most dangerous.")
                .end("[Leave]"));
    }

    private static void pip() {
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
    }
}
