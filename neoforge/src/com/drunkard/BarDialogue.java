package com.drunkard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class BarDialogue {
   public static final int SILENCE = 90;
   public static final int LEAVE = 91;
   public static final int ANSWER_SECONDS = 12;
   private static final List<BarDialogue.Node> NODES = new ArrayList<>();
   private static final Map<String, Integer> INDEX = new HashMap<>();
   public static final List<String> TOPICS = List.of(
      "hello",
      "story",
      "work",
      "joke",
      "you",
      "rumour",
      "bet",
      "love",
      "sad",
      "celebrate",
      "thinking",
      "pets",
      "farm",
      "nether",
      "villager",
      "fishing",
      "build",
      "food",
      "raid",
      "ghost",
      "dream",
      "treasure",
      "trip",
      "enchant",
      "trade",
      "tab",
      "advice",
      "dare"
   );

   private static void node(String id, String line, BarDialogue.Option... options) {
      INDEX.put(id, NODES.size());
      NODES.add(new BarDialogue.Node(id, line, List.of(options)));
   }

   private static BarDialogue.Option opt(String text, @Nullable String next, String effect) {
      return new BarDialogue.Option(text, next, effect);
   }

   public static int index(String id) {
      return INDEX.getOrDefault(id, 0);
   }

   public static BarDialogue.Node get(int index) {
      return NODES.get(Math.max(0, Math.min(NODES.size() - 1, index)));
   }

   public static int size() {
      return NODES.size();
   }

   public static String fill(String text, String drink, @Nullable String other, String bartender) {
      text = text.replace("{drink}", drink).replace("{other}", other == null ? "that one over there" : other).replace("{bartender}", bartender);
      return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
   }

   private BarDialogue() {
   }

   static {
      node("silence", "...not much of a talker, huh?");
      node(
         "drunk",
         "*hic* You... you're a good bartender. Y'know that?",
         opt("I think you've had enough, friend.", "drunk_cut", "CALM,REP+1"),
         opt("One more won't hurt!", "drunk_more", "ORDER,RAPPORT+1"),
         opt("Let me get you some water.", "drunk_water", "WATER,RAPPORT+2")
      );
      node("drunk_cut", "Wh-what? ...yeah. Yeah, maybe you're right. Night, friend.");
      node("drunk_more", "THAT'S what I'm talkin' about!");
      node("drunk_water", "Water... you're a saint. An actual saint.");
      node(
         "slow",
         "Took your time, didn't you?",
         opt("Sorry, it's mad busy tonight.", "slow_sorry", "PATIENCE"),
         opt("This one's on the house.", "slow_free", "FREE,RAPPORT+3,REP+1"),
         opt("Don't like it? There's the door.", "slow_door", "LEAVE_ANGRY,RAPPORT-3,REP-2")
      );
      node("slow_sorry", "Yeah, yeah. I can see that. Just don't forget about me.");
      node("slow_free", "...oh. Well, now I feel bad. Thanks.");
      node("slow_door", "Wow. Fine. I'm gone. Enjoy your empty pub.");
      node(
         "regular",
         "There's my favourite bartender! Busy night?",
         opt("Always is when you're here.", "regular_flatter", "RAPPORT+2"),
         opt("The usual?", "regular_usual", "USUAL"),
         opt("How've you been?", "story", "")
      );
      node("regular_flatter", "Ha! Smooth talker. That's why I keep coming back.");
      node("regular_usual", "You know me too well.");
      node(
         "cold",
         "Oh. It's you.",
         opt("Look, I'm sorry about earlier.", "cold_sorry", "RAPPORT+2"),
         opt("Can I get you anything?", "cold_drink", "ORDER"),
         opt("Suit yourself.", "cold_end", "RAPPORT-1")
      );
      node("cold_sorry", "...fine. Apology accepted. Don't make it a habit.");
      node("cold_drink", "Just the drink. Thanks.");
      node("cold_end", "Yeah. I will.");
      node(
         "drink",
         "This {drink} is something else. What's your secret?",
         opt("Family recipe. Can't tell you.", "drink_secret", "RAPPORT+1"),
         opt("Mostly love. And potatoes.", "drink_potato", "RAPPORT+2"),
         opt("You don't want to know.", "drink_dont", "")
      );
      node("drink_secret", "Keep your secrets, then. I'll keep drinking them.");
      node("drink_potato", "Ha! Potatoes. I'll take another one of those later.");
      node("drink_dont", "...now I REALLY want to know.");
      node(
         "gossip",
         "See {other}? Been staring at me all night.",
         opt("Want me to have a word?", "gossip_word", "RAPPORT+2"),
         opt("Maybe they like you.", "gossip_like", "RAPPORT+1"),
         opt("Not my problem.", "gossip_no", "RAPPORT-1")
      );
      node("gossip_word", "Would you? Thanks. You're a good one.");
      node("gossip_like", "Ha! Maybe... should I go say hi?");
      node("gossip_no", "Right. Forget I said anything.");
      node(
         "story",
         "Long day. Eight hours in a mine and nothing but gravel.",
         opt("Gravel's underrated.", "story_gravel", "RAPPORT+1"),
         opt("Next one's on the house. You've earned it.", "story_free", "FREE,RAPPORT+3,REP+1"),
         opt("Maybe try mining somewhere else?", "story_advice", "RAPPORT-1")
      );
      node("story_gravel", "Ha! You're alright, you know that?");
      node("story_free", "Seriously? You're the best. I'm telling everyone about this place.");
      node("story_advice", "Wow. Thanks for the tip, genius.");
      node(
         "leaving",
         "I should probably head home...",
         opt("One for the road?", "leaving_road", "ORDER,RAPPORT+1"),
         opt("Get home safe.", "leaving_safe", "LEAVE_HAPPY,RAPPORT+1"),
         opt("Already? The night's young!", "leaving_stay", "STAY")
      );
      node("leaving_road", "Go on then. Just the one.");
      node("leaving_safe", "Will do. Thanks for tonight.");
      node("leaving_stay", "...you're a bad influence. One more hour.");
      node(
         "smoke",
         "Hope you don't mind the smoke.",
         opt("Not at all. Enjoy it.", "smoke_ok", "RAPPORT+1"),
         opt("Use the holder, not my bar top.", "smoke_holder", ""),
         opt("Got a spare?", "smoke_spare", "RAPPORT+1")
      );
      node("smoke_ok", "Now THIS is a proper pub.");
      node("smoke_holder", "Yes, boss. Sorry, boss.");
      node("smoke_spare", "Ha! Buy your own. You've got a whole shelf of them.");
      node(
         "hello",
         "Hey, {bartender}. Good night so far?",
         opt("Best night of the week. What brings you in?", "story", ""),
         opt("Can't complain. Need anything?", "need", ""),
         opt("Just checking on you.", "hello_check", "RAPPORT+1")
      );
      node(
         "need",
         "Maybe another {drink} in a bit.",
         opt("Coming right up.", "need_now", "ORDER"),
         opt("Take your time, no rush.", "need_later", "STAY,RAPPORT+1")
      );
      node("need_now", "Ha! Eager. I like it.");
      node("need_later", "That's what I like about this place.");
      node("hello_check", "Appreciate it. Most bartenders don't bother.");
      node(
         "recommend",
         "What would you recommend tonight?",
         opt("The house special. You won't regret it.", "rec_special", "ORDER,RAPPORT+1"),
         opt("Something strong. You look like you need it.", "rec_strong", "ORDER"),
         opt("Water. Seriously.", "rec_water", "WATER,RAPPORT-1"),
         opt("Whatever you had last time.", "rec_same", "USUAL")
      );
      node("rec_special", "The special it is. Surprise me.");
      node("rec_strong", "Ha! You read me like a book.");
      node("rec_water", "...wow. Okay. Water. Living on the edge.");
      node("rec_same", "Creature of habit. Fine by me.");
      node(
         "work",
         "My boss has me digging tunnels every day. Forty blocks before lunch.",
         opt("Sounds like you deserve a raise.", "work_raise", "RAPPORT+1"),
         opt("Ever thought about quitting?", "work_quit", ""),
         opt("Forty? I do sixty before breakfast.", "work_brag", "RAPPORT-1"),
         opt("Tell me about the worst day.", "work_story", "RAPPORT+1")
      );
      node("work_raise", "Tell HIM that. Actually, don't. He'd make it eighty.");
      node(
         "work_quit",
         "Every day. Then I remember I like eating.",
         opt("There's always work behind a bar.", "work_bar", "RAPPORT+2"),
         opt("Eating's overrated.", "work_eat", "")
      );
      node("work_bar", "Ha! Careful, I might take you up on that.");
      node("work_eat", "Says the person surrounded by snacks.");
      node("work_brag", "Sure you do. And I'm the Ender Dragon.");
      node(
         "work_story",
         "Hit a lava pocket, lost my pick, then the torch went out. Then a creeper. Then my boss.",
         opt("This one's on the house.", "story_free", "FREE,RAPPORT+3"),
         opt("Sounds like a normal Tuesday.", "work_tuesday", "RAPPORT+1")
      );
      node("work_tuesday", "...you know what, it was a Tuesday.");
      node(
         "sad",
         "Sorry. Bit of a rough day. My dog ran off.",
         opt("I'm sorry. Want to talk about it?", "sad_talk", "RAPPORT+2"),
         opt("Dogs come back. They always do.", "sad_hope", "RAPPORT+1"),
         opt("Drink up, it'll take your mind off it.", "sad_drink", "ORDER"),
         opt("Have you checked the village?", "sad_village", "")
      );
      node("sad_talk", "His name's Biscuit. He likes bones and sitting on beds. Thanks for listening.");
      node("sad_hope", "You think? ...yeah. Yeah, he knows the way home.");
      node("sad_drink", "Maybe just one more.");
      node("sad_village", "...the village. He loves the butcher. Why didn't I think of that?");
      node(
         "celebrate",
         "Guess what? I finally finished my castle!",
         opt("Congratulations! Drinks on the house!", "cel_free", "FREE,RAPPORT+3,REP+1"),
         opt("Nice! Out of what?", "cel_what", "RAPPORT+1"),
         opt("Castle? Mine's bigger.", "cel_brag", "RAPPORT-1")
      );
      node("cel_free", "You're kidding! Best. Pub. Ever.");
      node("cel_what", "Cobblestone and dreams. Mostly cobblestone.");
      node("cel_brag", "Of course it is. Everybody's castle is bigger.");
      node(
         "joke",
         "Want to hear a joke?",
         opt("Go on, then.", "joke_tell", ""),
         opt("Only if it's a good one.", "joke_tell", ""),
         opt("Not really.", "joke_no", "RAPPORT-1")
      );
      node(
         "joke_tell",
         "Why don't skeletons fight each other? They don't have the guts!",
         opt("Ha! That's terrible. I love it.", "joke_laugh", "RAPPORT+2"),
         opt("Heard it before.", "joke_old", ""),
         opt("Here's a better one: why did the creeper cross the road?", "joke_back", "RAPPORT+1")
      );
      node("joke_laugh", "I've got a hundred of those. Don't worry, I'll pace myself.");
      node("joke_old", "Tough crowd tonight.");
      node("joke_back", "...why? To get to the other SSSSIDE? Ha! Okay, you win.");
      node("joke_no", "Fine. Your loss. It was a great one.");
      node(
         "you",
         "So what's your story? How'd you end up running a pub?",
         opt("Long story. Started with a barrel of grapes.", "you_grapes", "RAPPORT+1"),
         opt("I like people. Mostly.", "you_people", "RAPPORT+1"),
         opt("Lost a bet.", "you_bet", "RAPPORT+2"),
         opt("Let's talk about you instead.", "story", "")
      );
      node("you_grapes", "And now look at you. A real tycoon.");
      node("you_people", "Mostly. Ha! I'll take that as a compliment.");
      node("you_bet", "Ha! Best bet you ever lost, I reckon.");
      node(
         "rumour",
         "Heard a rumour there's a stronghold right under this pub.",
         opt("Who told you that?", "rum_who", ""),
         opt("Shh. Keep it between us.", "rum_secret", "RAPPORT+2"),
         opt("If there was, I'd be rich and closed.", "rum_rich", "RAPPORT+1")
      );
      node("rum_who", "Some miner. Third bottle in. Very convincing at the time.");
      node("rum_secret", "Ooh. My lips are sealed. Mostly.");
      node("rum_rich", "Fair point. Glad you're not rich, then.");
      node(
         "complain",
         "No offence, but this {drink} is a bit warm.",
         opt("Sorry about that. Next one's on me.", "comp_free", "FREE,RAPPORT+2"),
         opt("It's meant to be like that.", "comp_meant", ""),
         opt("Then don't drink it.", "comp_rude", "RAPPORT-2,REP-1")
      );
      node("comp_free", "Now that's how you run a pub.");
      node("comp_meant", "...is it? Huh. Fancy.");
      node("comp_rude", "Charming. Really charming.");
      node(
         "bet",
         "Bet you can't guess what I do for a living.",
         opt("Miner.", "bet_miner", ""),
         opt("Farmer.", "bet_farmer", ""),
         opt("Professional bar regular.", "bet_regular", "RAPPORT+2")
      );
      node("bet_miner", "Wrong! I'm a fisherman. Though I did mine once. Hated it.");
      node("bet_farmer", "Close! I grow pumpkins. Big ones. Award-winning, nearly.");
      node("bet_regular", "Ha! Okay, that's fair. That's very fair.");
      node(
         "love",
         "Can I ask you something? There's someone I like...",
         opt("Just tell them how you feel.", "love_tell", "RAPPORT+1"),
         opt("Buy them a drink. Here.", "love_drink", "ORDER,RAPPORT+1"),
         opt("Write them a poem on a sign.", "love_sign", "RAPPORT+1"),
         opt("Not my area, friend.", "love_no", "")
      );
      node("love_tell", "Just like that? ...okay. Okay. Tomorrow. Probably.");
      node("love_drink", "Smooth. Smooth. Okay, one for courage first.");
      node("love_sign", "A poem on a sign... that's either genius or the worst idea ever.");
      node("love_no", "Fair. You're a bartender, not a miracle worker.");
      node(
         "music",
         "Any chance you could put a record on?",
         opt("Sure, what do you want to hear?", "music_yes", "RAPPORT+1"),
         opt("The jukebox is right there.", "music_there", ""),
         opt("Only if you dance.", "music_dance", "RAPPORT+2")
      );
      node("music_yes", "Something with a beat. Surprise me.");
      node("music_there", "Ah. So it is. I'll grab one.");
      node("music_dance", "Deal. But you're not filming it.");
      node(
         "late",
         "Is it that late already?",
         opt("Last orders soon.", "late_last", "ORDER"),
         opt("Stay as long as you like.", "late_stay", "STAY,RAPPORT+1"),
         opt("Time you headed home, I think.", "late_home", "LEAVE_HAPPY")
      );
      node("late_last", "Then I'd better make it count.");
      node("late_stay", "Careful. I might move in.");
      node("late_home", "Yeah... you're right. Thanks for everything.");
      node(
         "thinking",
         "Do you ever wonder what's at the edge of the world?",
         opt("More world, probably.", "think_more", ""),
         opt("Every night, after closing.", "think_night", "RAPPORT+2"),
         opt("I think you've had enough.", "think_enough", "CALM")
      );
      node("think_more", "More world... that's deep. Or not. I can't tell any more.");
      node("think_night", "Right? Glad it's not just me.");
      node("think_enough", "...you might be right. One more thought, then home.");
      node(
         "pets",
         "Got a new cat. It sits on my chest every night and judges me.",
         opt("Cats are like that.", "pets_like", "RAPPORT+1"),
         opt("Name it after me.", "pets_name", "RAPPORT+2"),
         opt("I'm more of a parrot person.", "pets_parrot", ""),
         opt("Does it do anything useful?", "pets_useful", "")
      );
      node("pets_like", "Mine's called Gerald. He's very disappointed in all of us.");
      node("pets_name", "Ha! He'd hate that. Which is why I'm doing it.");
      node("pets_parrot", "Parrots are just small, loud cats with opinions.");
      node(
         "pets_useful",
         "He scared off a creeper once. Then hid under my bed for a week.",
         opt("Brave, in his way.", "pets_brave", "RAPPORT+1"),
         opt("A hero. Next one's on me.", "story_free", "FREE,RAPPORT+2")
      );
      node("pets_brave", "Don't tell him. It'll go to his head.");
      node(
         "farm",
         "My wheat finally came in. Three weeks and half the bone meal in the world.",
         opt("Bread for the whole pub!", "farm_bread", "RAPPORT+1"),
         opt("Ever tried melons? Less fuss.", "farm_melon", ""),
         opt("Sell me some.", "farm_sell", "RAPPORT+1")
      );
      node("farm_bread", "A loaf each! ...by the end of the year.");
      node("farm_melon", "Melons. Sure. Until they pop up next to the lava again.");
      node("farm_sell", "For you? Half price. For anyone else: full price and a lecture.");
      node(
         "nether",
         "I went through a portal last night. Ghasts. So many ghasts.",
         opt("Did you bring me anything back?", "nether_gift", "RAPPORT+1"),
         opt("That's why I stay in the pub.", "nether_pub", "RAPPORT+1"),
         opt("You went alone?", "nether_alone", "")
      );
      node("nether_gift", "A tear. And a hole in my boot. Is that something you want?");
      node("nether_pub", "Smart. Warm, dry, and nothing explodes. Mostly.");
      node(
         "nether_alone",
         "Yeah. In my defence, I had a plan.",
         opt("What was the plan?", "nether_plan", "RAPPORT+1"),
         opt("A drink for the brave. On me.", "story_free", "FREE,RAPPORT+2")
      );
      node("nether_plan", "Get in, get out. I've made it as far as 'in'.");
      node(
         "villager",
         "A villager overcharged me for a book again. Two emeralds and a sigh.",
         opt("That's the librarian's way.", "vil_lib", "RAPPORT+1"),
         opt("Haggle. They love it.", "vil_haggle", ""),
         opt("Be nice to the villagers. They talk.", "vil_talk", "RAPPORT+1")
      );
      node("vil_lib", "He hums at me while I pay. Every time.");
      node("vil_haggle", "I did! He said 'hrmm'. I don't know if that's a yes.");
      node("vil_talk", "They do. Mine told the whole village I cried at a sunset.");
      node(
         "fishing",
         "Caught a treasure chest fishing today. Contained a boot.",
         opt("A very expensive boot?", "fish_boot", ""),
         opt("What did you do with it?", "fish_what", "RAPPORT+1"),
         opt("Fishing is mostly waiting. Like running a pub.", "fish_pub", "RAPPORT+2")
      );
      node("fish_boot", "It was the left one. I own a lot of left boots now.");
      node("fish_what", "Wore it. Walked in a circle. Felt strange.");
      node("fish_pub", "So you're telling me I'm a regular fish. Rude. Accurate.");
      node(
         "build",
         "I'm building a floating house. Don't ask how it stays up.",
         opt("How does it stay up?", "build_how", ""),
         opt("Send me a screenshot.", "build_shot", "RAPPORT+1"),
         opt("Floating houses are a bad idea.", "build_bad", "RAPPORT-1")
      );
      node("build_how", "...scaffolding. A lot of scaffolding. I'm not proud.");
      node("build_shot", "I'll take one right before it falls over. For the memories.");
      node("build_bad", "It's a great idea! Until the wind. There's no wind. There shouldn't be.");
      node(
         "food",
         "Do you do food here? Bread, stew, anything?",
         opt("Just drinks. Bring your own.", "food_no", ""),
         opt("I'll see what's in the kitchen.", "food_kitchen", "RAPPORT+1"),
         opt("Have a few peanuts. They're free.", "food_peanuts", "RAPPORT+1,STAY")
      );
      node("food_no", "Fair. A pub with a policy. I respect that.");
      node("food_kitchen", "You have a kitchen?! I'll wait. I'll wait all night.");
      node("food_peanuts", "Free peanuts. Now we're talking. Another round, then!");
      node(
         "raid",
         "Raid on our village last night. We held them off with cats and spite.",
         opt("Anyone hurt?", "raid_hurt", "RAPPORT+1"),
         opt("Spite works. Next one's on the house.", "story_free", "FREE,RAPPORT+3"),
         opt("Cats? Really?", "raid_cats", "")
      );
      node("raid_hurt", "Just pride. And the bell. They took the bell again.");
      node("raid_cats", "A lot of cats. They didn't do anything. They just stared.");
      node(
         "ghost",
         "Is it true this place is haunted?",
         opt("Only by old customers.", "ghost_old", "RAPPORT+1"),
         opt("Shh. He likes the attention.", "ghost_shh", "RAPPORT+2"),
         opt("Only on slow nights.", "ghost_slow", ""),
         opt("It's the draught. Honest.", "ghost_draught", "")
      );
      node("ghost_old", "I'm going to be one of those. I'm putting it in writing.");
      node("ghost_shh", "...I think I just felt something. That was probably the lager.");
      node("ghost_slow", "Tonight's not slow. I'm not sure that's comforting.");
      node("ghost_draught", "That's what they always say. Right before the lights flicker.");
      node(
         "weather",
         "Listen to that rain. Perfect weather for a pint.",
         opt("Any weather is pub weather.", "wea_any", "RAPPORT+2"),
         opt("Stay till it stops.", "wea_stay", "STAY,RAPPORT+1"),
         opt("Hope you brought a roof.", "wea_roof", "")
      );
      node("wea_any", "Truer words. Put that on a sign.");
      node("wea_stay", "It might never stop. I could live here.");
      node("wea_roof", "I did not. Is that a thing you can lend?");
      node(
         "dream",
         "Last night I dreamt I was a bartender.",
         opt("How did you do?", "dream_how", "RAPPORT+1"),
         opt("Nightmare or dream?", "dream_which", ""),
         opt("Fancy a job?", "dream_job", "RAPPORT+2")
      );
      node("dream_how", "Terrible. I poured everything into one glass. It was a special.");
      node("dream_which", "Both. Mostly the dishes. There were a LOT of dishes.");
      node("dream_job", "Ha! Don't tempt me. I'd be useless and very happy.");
      node(
         "treasure",
         "Found a map today. X marks the spot, apparently.",
         opt("Where's the X?", "tre_where", ""),
         opt("Want company?", "tre_company", "RAPPORT+2"),
         opt("It's a trap. Maps are always a trap.", "tre_trap", "RAPPORT+1")
      );
      node("tre_where", "Somewhere under a pumpkin patch. I've dug up eleven pumpkins.");
      node("tre_company", "You'd leave the pub? For me? ...I'll let you know.");
      node("tre_trap", "Probably. But what if it isn't? That's the worst part.");
      node(
         "trip",
         "Thinking of a trip to the jungle. You ever been?",
         opt("Too many parrots.", "trip_parrot", ""),
         opt("Bring me back a cocoa bean.", "trip_cocoa", "RAPPORT+1"),
         opt("Why leave the pub?", "trip_pub", "RAPPORT+2")
      );
      node("trip_parrot", "Parrots are fine. It's the ocelots I worry about.");
      node("trip_cocoa", "One cocoa bean. A whole one. I'll frame it.");
      node("trip_pub", "That's what I keep telling myself. Every time I leave.");
      node(
         "enchant",
         "My sword's finally Sharpness V. Look at that glow.",
         opt("Impressive. What will you cut?", "enc_cut", "RAPPORT+1"),
         opt("It's just a sword.", "enc_just", "RAPPORT-1"),
         opt("Put it away, you'll scare the customers.", "enc_away", "")
      );
      node("enc_cut", "Trees mostly. Very efficiently. Very, very efficiently.");
      node("enc_just", "...it is not just a sword. It has a NAME. It's called Doris.");
      node("enc_away", "Of course. Sorry. Doris is shy anyway.");
      node(
         "trade",
         "I traded forty emeralds for a hat. Best hat I've ever owned.",
         opt("Show me the hat.", "trade_hat", "RAPPORT+1"),
         opt("Forty emeralds?!", "trade_forty", ""),
         opt("Can I try it on?", "trade_try", "RAPPORT+2")
      );
      node("trade_hat", "It's... it's a normal hat. But it's MY normal hat.");
      node("trade_forty", "It's a very good hat. Don't make me justify it.");
      node("trade_try", "Careful with it. It has sentimental value. And it cost forty emeralds.");
      node(
         "tab",
         "Could I put this on a tab? ...kidding. Mostly.",
         opt("We don't do tabs.", "tab_no", "RAPPORT-1"),
         opt("Only for regulars. You're getting there.", "tab_reg", "RAPPORT+2"),
         opt("Pay now, pay later. I don't mind.", "tab_fine", "RAPPORT+1")
      );
      node("tab_no", "Fair. I'll pay. I was only asking.");
      node("tab_reg", "Ha! I'll try to be more regular. For the credit.");
      node("tab_fine", "That's... a very trusting policy. Don't tell the others.");
      node(
         "advice",
         "Can I ask for advice? Would it be mad to quit a stable job and become a bard?",
         opt("Follow your heart. And your lute.", "adv_follow", "RAPPORT+2"),
         opt("Keep the job. Play on weekends.", "adv_job", "RAPPORT+1"),
         opt("Can you actually play?", "adv_play", "")
      );
      node("adv_follow", "That's the nicest thing anyone's said to me. I'm a little emotional.");
      node("adv_job", "Boring. Sensible. Probably right. I hate it.");
      node("adv_play", "I can play one song. It's the one about the creeper. It's three chords.");
      node(
         "dare",
         "Bet I can finish this before you finish wiping that glass.",
         opt("You're on.", "dare_on", "RAPPORT+2"),
         opt("Don't hurt yourself.", "dare_hurt", "RAPPORT+1"),
         opt("Slow down. It's not a race.", "dare_slow", "RAPPORT-1")
      );
      node("dare_on", "Ha! Ready, steady... oh. That was fast. Too fast. Whoops.");
      node("dare_hurt", "I've survived worse. Mostly my own cooking.");
      node("dare_slow", "Spoilsport. Fine. I'll sip like a tourist.");
   }

   public static record Node(String id, String line, List<BarDialogue.Option> options) {
      public boolean end() {
         return this.options.isEmpty();
      }
   }

   public static record Option(String text, @Nullable String next, String effect) {
   }
}
