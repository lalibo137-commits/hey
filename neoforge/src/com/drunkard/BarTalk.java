package com.drunkard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class BarTalk {
   private static final Predicate<BarTalk.Mood> ANY = m -> true;
   public static final List<BarTalk.Script> CHATS = List.of(
      chat(
         2,
         BarTalk.Mood::gossip,
         "A|Don't look now, but {other} keeps looking over here.",
         "B|Maybe they like you.|SHRUG",
         "A|Or they want my seat.|THINK",
         "B|Nobody wants your seat. It wobbles.|NONE|LAUGH"
      ),
      chat(
         2,
         BarTalk.Mood::gossip,
         "A|See {other}?|POINT",
         "B|What about them?",
         "A|Tipped double last week.",
         "B|Rich?|THINK",
         "A|Found a stronghold. Sold the map.|SHRUG",
         "B|...I'm buying them a drink.|NONE|LAUGH"
      ),
      chat(
         2,
         BarTalk.Mood::gossip,
         "A|Is that {other} from the iron farm?|THINK",
         "B|The one who flooded it?",
         "A|That's the one.|NOD",
         "B|Whatever you do, don't mention water.|SHAKE|LAUGH"
      ),
      chat(
         2,
         BarTalk.Mood::gossip,
         "A|{other} was here when I came in yesterday.",
         "B|Same seat?|THINK",
         "A|Same seat. Same drink. Same face.",
         "B|Honestly? Goals.|NOD|LAUGH"
      ),
      chat(
         2,
         BarTalk.Mood::gossip,
         "A|I think {other} is a famous builder.|THINK",
         "B|Them? What did they build?",
         "A|That giant statue by the river.",
         "B|The one that looks like a potato?|SHAKE",
         "A|It's supposed to be a horse.|SHRUG|LAUGH"
      ),
      chat(
         2,
         BarTalk.Mood::gossip,
         "A|What do you reckon {other}'s story is?|THINK",
         "B|Lost their diamonds in lava. Drinking to forget.",
         "A|That's YOUR story.|POINT",
         "B|...that's why I recognised it.|SHRUG|LAUGH"
      ),
      chat(
         2,
         m -> m.gossip() && m.drunk(),
         "A|I'm gonna go talk to {other}.",
         "B|And say what?|THINK",
         "A|Nice shirt.|NOD",
         "B|You've said that to four people tonight.|FACEPALM|LAUGH"
      ),
      chat(
         2,
         BarTalk.Mood::quickService,
         "A|Service is quick tonight.|NOD",
         "B|{bartender} knows what they're doing.|NOD",
         "A|Last place I went, I aged a year waiting.|SHRUG|LAUGH"
      ),
      chat(2, BarTalk.Mood::quickService, "A|Did you see how fast that came?|POINT", "B|Barely sat down.", "A|I'm leaving a tip. A real one.|NOD|NOD"),
      chat(
         2,
         BarTalk.Mood::slowService,
         "A|How long have we been waiting?|THINK",
         "B|I've grown a beard.|FACEPALM",
         "A|You had a beard when we came in.|POINT",
         "B|A second beard.|SHRUG|LAUGH"
      ),
      chat(
         2,
         BarTalk.Mood::slowService,
         "A|Do you think {bartender} forgot about us?|THINK",
         "B|Wave or something.",
         "A|I've been waving for five minutes.|WAVE",
         "B|Wave harder.|POINT|LAUGH"
      ),
      chat(2, BarTalk.Mood::busy, "A|Packed tonight.|NOD", "B|Word gets around.", "A|Good for {bartender}. Bad for my elbows.|SHRUG|LAUGH"),
      chat(
         2,
         BarTalk.Mood::busy,
         "A|Is it always this busy?|THINK",
         "B|Only since they got the good stuff in.",
         "A|What's the good stuff?",
         "B|Everything. It's a pub.|SHRUG|LAUGH"
      ),
      chat(2, m -> !m.busy(), "A|Quiet tonight.", "B|I like it quiet. You can hear yourself think.|NOD", "A|You think?|POINT|LAUGH"),
      chat(
         2,
         m -> !m.busy(),
         "A|Where is everyone?|THINK",
         "B|Village festival.",
         "A|We have a village festival?",
         "B|We have a pumpkin on a fence post. Same thing.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|You know what I like about this place?|THINK",
         "B|The drinks?",
         "A|Nobody asks where you got your diamonds.|NOD",
         "B|...where DID you get your diamonds?|POINT|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|First time here?",
         "B|Second. I didn't make it past the door the first time.|SHRUG",
         "A|Why not?|THINK",
         "B|Creeper by the door. Long story. Short door.|FACEPALM|LAUGH"
      ),
      chat(
         2,
         m -> m.regular(),
         "A|You're in here a lot.|POINT",
         "B|I'm a regular.|NOD",
         "A|How regular?",
         "B|{bartender} pours my drink when they hear the door.|FIST_PUMP|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|So I finally asked her out.",
         "B|And?",
         "A|She said she'd think about it.|SHRUG",
         "B|That's not a no!|POINT",
         "A|It was three weeks ago.|FACEPALM",
         "B|...okay, it's a no.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|Boss wants the whole tunnel lit by Friday.",
         "B|How long's the tunnel?|THINK",
         "A|Two thousand blocks.|FACEPALM",
         "B|That's a lot of torches.",
         "A|I'm on my eighth stack of coal.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|My neighbour built a wall right on my property line.|SHAKE",
         "B|What kind of wall?",
         "A|Cobblestone. Forty blocks high.",
         "B|What did you do?|THINK",
         "A|Put a window in it.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|My kid tamed a cat today.|FIST_PUMP",
         "B|Aw, nice.|NOD",
         "A|We now have eleven cats.|FACEPALM",
         "B|...how?",
         "A|I don't know. They keep turning up.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|I think the librarian's ripping me off.|THINK",
         "B|How?",
         "A|He put his prices up after I cured him.|SHAKE",
         "B|Curing them makes it cheaper.|POINT",
         "A|Not this guy.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|We're moving next week.",
         "B|Where to?",
         "A|A mushroom island.|NOD",
         "B|No mobs, huh?|THINK",
         "A|No mobs, no neighbours... no pub.|SHRUG",
         "B|You'll be back in a week.|POINT|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|Went fishing all day yesterday.",
         "B|Catch anything?",
         "A|Four boots and an enchanted book.|SHRUG",
         "B|Ooh. What enchantment?|THINK",
         "A|Luck of the Sea.|FACEPALM|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|Can I ask you something?|THINK",
         "B|Sure.",
         "A|Is it weird to name your sword?",
         "B|What did you name it?",
         "A|Gerald.",
         "B|...Gerald is a great name for a sword.|NOD|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|There was a raid at the village last night.|SHAKE",
         "B|Everyone okay?|THINK",
         "A|They took the bell.",
         "B|The BELL?",
         "A|We think the witch has it.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|Remember when we got lost in the Nether?|THINK",
         "B|We weren't lost. We were exploring.",
         "A|For nine days.|POINT",
         "B|Exploring thoroughly.|NOD|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|My dog figured out the pressure plate on the iron door.|FACEPALM",
         "B|Smart dog.",
         "A|He let himself out at 3 AM.",
         "B|Very smart dog.|NOD",
         "A|He came back with a skeleton bone. Proud as anything.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|I started a farm.|NOD",
         "B|What are you growing?",
         "A|Pumpkins.",
         "B|Nice. Selling them?|THINK",
         "A|Can't. I carved faces in all of them.|SHRUG",
         "B|...why?",
         "A|They looked lonely.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|Had the strangest dream last night.|THINK",
         "B|Go on.",
         "A|I was a chicken. Somebody kept throwing eggs at me.",
         "B|That's not a dream, that's Tuesday at your place.|POINT|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|Guess who I ran into today.",
         "B|Who?",
         "A|The wandering trader.",
         "B|What'd he have?|THINK",
         "A|Two llamas, a fern and a lot of confidence.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|I'm thinking of going into business.|THINK",
         "B|Doing what?",
         "A|Selling maps.",
         "B|Maps of what?",
         "A|Of this pub. People keep getting lost on the way.|POINT|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|So the house finally has a roof.|FIST_PUMP",
         "B|About time.",
         "A|And a floor.",
         "B|You didn't have a floor?|THINK",
         "A|It was a very ambitious house.|SHRUG|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|My sister got married last week.|NOD",
         "B|Congrats! Who's the lucky guy?",
         "A|A villager.|SHRUG",
         "B|...what does he do?|THINK",
         "A|Nitwit.|FACEPALM|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|You ever think about just... leaving? Sailing off?|THINK",
         "B|All the time.",
         "A|Why don't we?",
         "B|Boat's full of chests.|SHRUG",
         "A|Fair.|NOD|NOD"
      ),
      chat(2, ANY, "A|Found an ancient city today.", "B|You went IN?|SHAKE", "A|I tiptoed for three hours.", "B|And?", "A|Sneezed.|FACEPALM|LAUGH"),
      chat(
         2,
         ANY,
         "A|A creeper blew up my house today.",
         "B|Again?!|SHAKE",
         "A|Third time this week.|SHRUG",
         "B|Ever thought about stone walls?|POINT",
         "A|...huh. That's actually smart.|THINK|LAUGH"
      ),
      chat(
         2,
         ANY,
         "A|Found diamonds yesterday!|FIST_PUMP",
         "B|No way!|NONE|NOD",
         "A|Dropped them in lava a minute later.|FACEPALM",
         "B|...I'm so sorry.|SHAKE",
         "A|Drinks are on you tonight.|POINT|LAUGH"
      ),
      chat(2, ANY, "A|I tried to sleep in the Nether.", "B|Tell me you didn't.|FACEPALM", "A|The bed exploded.|SHRUG", "B|Every single time.|NONE|LAUGH"),
      chat(2, ANY, "A|My boss made me mine obsidian by hand.|SHAKE", "B|With what?!", "A|A wooden pickaxe.|FACEPALM", "B|Quit. Tonight.|POINT|NOD"),
      chat(2, ANY, "A|The wandering trader came by.", "B|Buy anything?", "A|One fern. Five emeralds.|FACEPALM", "B|That man is a menace.|SHAKE|NOD"),
      chat(
         2, m -> m.drinking(), "A|What is that, the {drink}?|POINT", "B|Want to try it?", "A|...that's mostly ice.|THINK", "B|Ice is the best part.|NOD|LAUGH"
      ),
      chat(
         2,
         m -> m.drinking(),
         "A|I always order the same thing here.",
         "B|The {drink}?|THINK",
         "A|How'd you know?",
         "B|You've ordered it four times tonight.|POINT|LAUGH"
      ),
      chat(2, m -> m.drinking(), "A|This {drink} is strong.", "B|You've had two sips.|POINT", "A|Strong sips.|NOD|LAUGH"),
      chat(2, m -> m.holding(), "A|How's the {drink}?", "B|Honestly? Perfect.|NOD", "A|I'm getting one next.|POINT|NOD"),
      chat(
         2,
         m -> m.smoking(),
         "A|Since when do you smoke?|THINK",
         "B|Since my wife's in here.",
         "A|Your wife's here?",
         "B|Exactly. Need something to do with my hands.|SHRUG|LAUGH"
      ),
      chat(2, m -> m.smoking(), "A|That smells like a campfire.", "B|It's called class.|NOD", "A|It's called a campfire.|SHRUG|LAUGH"),
      chat(2, m -> m.holding(), "A|To us!|CHEERS|CHEERS", "B|To us!|CHEERS", "A|And to whoever's paying.|POINT|LAUGH"),
      chat(
         2,
         m -> m.music(),
         "A|This one always reminds me of the old house.|THINK",
         "B|The one that burned down?",
         "A|It didn't burn down. It got struck by lightning.",
         "B|...and then burned down.|NOD|LAUGH"
      ),
      chat(2, m -> m.music(), "A|Who put this record on?", "B|Me. Problem?|SHRUG", "A|...no. It's good, actually.|NOD|LAUGH"),
      chat(2, m -> !m.music(), "A|It's too quiet in here.", "B|Somebody put a record on!|WAVE|NOD"),
      chat(
         2,
         m -> m.night(),
         "A|Walking home's going to be fun.|SHAKE",
         "B|Zombies?",
         "A|Zombies, skeletons and my wife.|FACEPALM",
         "B|Have another. Wait for sunrise.|POINT|NOD"
      ),
      chat(2, m -> m.night(), "A|Late one tonight, huh?", "B|Too late to walk home.|NOD", "A|Too early to leave.|CHEERS|CHEERS"),
      chat(2, m -> m.rain(), "A|Listen to that rain.", "B|Perfect pub weather.|NOD", "A|Every weather is pub weather.|CHEERS|CHEERS"),
      chat(2, m -> m.rain(), "A|Got soaked on the way here.", "B|No umbrella?", "A|Never been invented.|SHRUG|LAUGH"),
      chat(2, m -> m.fightNearby(), "A|Did you see that?!|POINT", "B|Somebody's getting thrown out.|NOD", "A|My money's on the loud one.|SHRUG|LAUGH"),
      chat(2, m -> m.fightNearby(), "A|Should we break it up?|THINK", "B|And spill my drink? No way.|SHAKE|LAUGH"),
      chat(2, m -> m.drunk(), "A|I love you, man.|CHEERS", "B|You've had a few.", "A|No, I mean it. *hic*|NOD", "B|Sure you do, buddy.|LAUGH|LAUGH"),
      chat(2, m -> m.drunk(), "A|Is the room spinning?", "B|No.|SHAKE", "A|Then why am I on the floor?|SHRUG|LAUGH"),
      chat(2, m -> m.drunk(), "A|I'm gonna fight a ghast.|FIST_PUMP", "B|You can't even fight the stairs.|POINT", "A|The stairs started it!|SHAKE|LAUGH"),
      chat(2, m -> m.drunk(), "A|You know what? You're my best friend.|POINT", "B|We met ten minutes ago.", "A|Best ten minutes of my life. *hic*|CHEERS|LAUGH"),
      chat(2, m -> m.drunk(), "A|I'm gonna tell {bartender} how I feel.|FIST_PUMP", "B|And how do you feel?|THINK", "A|...thirsty.|SHRUG|LAUGH"),
      chat(
         3,
         ANY,
         "A|Okay. Important question.|THINK",
         "B|Here we go.|FACEPALM",
         "A|One creeper-sized chicken or a hundred chicken-sized creepers?",
         "C|The chickens.|NOD",
         "B|There are no chickens in that question!|SHAKE",
         "C|...the big chicken then.|SHRUG|LAUGH"
      ),
      chat(3, ANY, "A|Who's got a story?", "B|I fell in lava today.|SHRUG", "C|And you're alive?|SHAKE", "B|Fell in water right after.|FIST_PUMP|LAUGH"),
      chat(3, ANY, "A|Who's buying the next round?|THINK", "B|Not me.|SHAKE", "C|Not me.|SHAKE", "A|...fine, I'll buy.|FACEPALM|LAUGH"),
      chat(3, ANY, "A|Best mob in the game. Go.", "B|Axolotl.|NOD", "C|Creeper.|FIST_PUMP", "B|You WOULD say creeper.|FACEPALM|LAUGH"),
      chat(3, ANY, "A|Remember our first night?|THINK", "B|Dug into a hill.|NOD", "C|Cried in a dirt hole.|SHRUG", "A|Good times.|CHEERS|CHEERS"),
      chat(3, ANY, "A|A toast!|CHEERS", "B|To what?|THINK", "C|To {bartender}!|CHEERS|CHEERS", "A|To {bartender}!|CHEERS|CHEERS"),
      chat(3, m -> m.gossip(), "A|Bet you {other} can't hold their drink.|POINT", "B|Bet you they can.", "C|Bet you he heard you both.|FACEPALM|LAUGH"),
      chat(
         3,
         m -> m.drunk(),
         "A|Let's go to the End tonight!|FIST_PUMP",
         "B|We don't have eyes of ender.|SHAKE",
         "C|We don't have pants.|POINT",
         "A|...tomorrow then.|SHRUG|LAUGH"
      )
   );
   public static final List<BarTalk.Script> INTROS = List.of(
      chat(2, ANY, "A|Is this seat taken?", "B|All yours.|NOD", "A|First time here?|THINK", "B|Second. You?", "A|I come here every week.|CHEERS|NOD"),
      chat(2, ANY, "A|Mind if I join you?", "B|Not at all.|NOD", "A|I'm new around here.", "B|Welcome. Stay away from the vermouth.|POINT|LAUGH"),
      chat(
         2,
         ANY,
         "A|Haven't seen you in here before.",
         "B|Just moved in. Mushroom island.|SHRUG",
         "A|No way. I'm from the village up the hill.",
         "B|Practically neighbours, then.|CHEERS|CHEERS"
      ),
      chat(
         2, ANY, "A|You look like you've had a long day.", "B|Is it that obvious?|FACEPALM", "A|Next one's on me.|CHEERS", "B|...I like you already.|NOD|LAUGH"
      ),
      chat(2, m -> m.drinking(), "A|Is that the {drink}? Any good?|POINT", "B|Best in town. Try it.|NOD", "A|I might. Cheers, stranger.|CHEERS|CHEERS"),
      chat(2, m -> m.music(), "A|Good song, this.|NOD", "B|You know it?", "A|Know it? I hum it in the mines.|SHRUG", "B|Ha! Me too.|LAUGH|LAUGH"),
      chat(2, m -> m.busy(), "A|Busy in here tonight, huh?", "B|Always is on a good night.|NOD", "A|Squeeze in, there's room.|WAVE|NOD")
   );
   public static final List<BarTalk.Script> REUNIONS = List.of(
      chat(
         2,
         ANY,
         "A|Hey! Good to see you again.|WAVE",
         "B|Couldn't stay away.|SHRUG",
         "A|How'd it go with the creeper problem?|THINK",
         "B|Stone walls. Changed my life.|FIST_PUMP|LAUGH"
      ),
      chat(2, ANY, "A|Look who it is!|POINT", "B|Back again, same as you.|CHEERS", "A|Same seat as last time?|THINK", "B|Wouldn't sit anywhere else.|NOD|NOD"),
      chat(
         2,
         ANY,
         "A|There you are! I was hoping you'd come in.|WAVE",
         "B|Wouldn't miss it.|NOD",
         "A|So did you ever find those diamonds?",
         "B|...we don't talk about the diamonds.|FACEPALM|LAUGH"
      ),
      chat(2, ANY, "A|Oh hey, it's you!|WAVE", "B|Told you I'd be back.|POINT", "A|First round's on me this time.|CHEERS", "B|I'll hold you to that.|NOD|LAUGH"),
      chat(2, m -> m.regular(), "A|Every night, you and me.|CHEERS", "B|We should get a plaque.|NOD", "A|\"Reserved for regulars.\"|POINT|LAUGH")
   );
   public static final List<BarTalk.Script> BARTENDER = List.of(
      chat(1, ANY, "A|Busy night, huh?", "A|You're doing great, though.|NOD"),
      chat(1, m -> m.holding(), "A|You make a mean {drink}.|CHEERS"),
      chat(1, ANY, "A|How long have you been running this place?|THINK", "A|Feels like it's always been here.|SHRUG"),
      chat(1, ANY, "A|Got any snacks back there?|POINT", "A|Bread? A cookie? Anything?|SHRUG"),
      chat(1, ANY, "A|This is my favourite pub.|NOD", "A|Don't tell the other pubs.|POINT"),
      chat(1, ANY, "A|What's good tonight?|THINK", "A|Actually, surprise me next round.|NOD"),
      chat(1, ANY, "A|How do you put up with us every night?|THINK", "A|Saint. You're a saint.|NOD"),
      chat(1, ANY, "A|Can I tell you something?|THINK", "A|I've never seen you blink.|POINT", "A|...anyway. Love the place.|SHRUG"),
      chat(1, ANY, "A|You ever take a night off?|THINK", "A|Didn't think so. Respect.|NOD"),
      chat(1, ANY, "A|Know any good mining spots?|THINK", "A|I'll trade you a story for one.|POINT"),
      chat(1, m -> m.quickService(), "A|Fastest bartender in the world, you.|POINT", "A|I mean it.|NOD"),
      chat(1, m -> m.slowService(), "A|No rush. Really.|SHRUG", "A|...okay, a little rush.|POINT"),
      chat(1, m -> m.busy(), "A|You need a second pair of hands back there.|POINT", "A|Not mine. I'm busy.|CHEERS"),
      chat(1, m -> m.regular(), "A|Same again tomorrow, {bartender}?|NOD", "A|Same seat, same drink, same face.|CHEERS"),
      chat(1, m -> m.gossip(), "A|Psst. {other}.|POINT", "A|Keep an eye on them. Last time they sang.|SHAKE"),
      chat(1, m -> m.smoking(), "A|Nothing beats a smoke at the bar.|NOD", "A|Don't tell my mother.|POINT"),
      chat(1, m -> m.drunk(), "A|You're my best friend, {bartender}.|CHEERS", "A|*hic* My very best friend.|NOD"),
      chat(1, m -> m.drunk(), "A|Is it just me or are there two of you?|THINK", "A|Both of you are great.|CHEERS"),
      chat(1, m -> m.music(), "A|Great music tonight!|FIST_PUMP"),
      chat(1, m -> m.night(), "A|Open this late? You're a hero.|POINT"),
      chat(1, m -> m.rain(), "A|Mind if I hide from the rain a bit?|SHRUG", "A|I'll keep ordering, promise.|NOD")
   );
   public static final List<BarTalk.Script> EVENTS = List.of(
      event(
         2,
         ANY,
         "A|Arm wrestle. You and me. Right now.|POINT",
         "B|Oh, you're on.|FIST_PUMP|NONE|WRESTLE",
         "A|Nnnngh...!|NONE|NONE|CROWD=Go go go!/Come on!/Push!/You got this!",
         "B|Grrrr...!|NONE",
         "A|YES! Still undefeated!|FIST_PUMP|NONE|CROWD=Woo!/Rematch!/No way!",
         "B|Rematch. Next week.|FACEPALM"
      ),
      event(
         2,
         m -> m.drinking() && m.drunk(),
         "A|Watch this, I can balance it on my-|POINT|NONE|SPILL",
         "A|Nooo! My {drink}!|FACEPALM|LAUGH|CROWD=Ha!/Smooth!/Ohhh!/Nice one!",
         "B|Smooth. Real smooth.|LAUGH",
         "A|Bartender! Another one, please...|WAVE"
      ),
      event(
         3,
         ANY,
         "A|Guess whose birthday it is!|FIST_PUMP",
         "B|No way! Happy birthday!|CHEERS|CLAP|CROWD=Happy birthday!/Woo!/Cheers to that!",
         "C|Speech! Speech!|CLAP|CLAP",
         "A|I love you all. Next round's on me!|CHEERS|CHEERS|ROUND"
      ),
      event(
         2,
         ANY,
         "A|Guess whose birthday it is!|FIST_PUMP",
         "B|Happy birthday, buddy!|HIGH_FIVE|NONE|HIGHFIVE",
         "B|Everybody! It's their birthday!|WAVE|NONE|CROWD=Happy birthday!/Woo!/Cheers!",
         "A|Next round's on me!|CHEERS|CHEERS|ROUND"
      ),
      event(
         1,
         m -> m.holding(),
         "A|Everybody! A toast!|WAVE|NONE|CROWD=A toast!/Hear, hear!/Cheers!",
         "A|To {bartender}, best bartender in town!|CHEERS|CHEERS|CROWD=To {bartender}!/Cheers!/Hear, hear!",
         "A|Keep the change, {bartender}!|POINT|NONE|TIP"
      ),
      event(
         2,
         m -> m.drunk(),
         "A|I'm just gonna... rest my eyes...|NONE|NONE|SLEEP",
         "A|Zzz...|NONE",
         "B|Hey. HEY! Wake up!|SHAKE",
         "A|I'm awake! I'm awake!|SHRUG|LAUGH|CROWD=Ha!/Rise and shine!/Long night?"
      ),
      event(
         2,
         m -> m.drinking(),
         "A|First to finish buys the next round.|POINT",
         "B|Deal. Go!|NOD|NONE|CHUG",
         "A|*gulp gulp gulp*|NONE|NONE|CROWD=Chug! Chug! Chug!/Go go go!",
         "B|Done! Ha!|FIST_PUMP|FACEPALM",
         "A|...I'll get the next round.|SHRUG|LAUGH|ROUND"
      ),
      event(
         2, ANY, "A|Guess who got promoted today!|FIST_PUMP", "B|No way! Up top!|HIGH_FIVE|NONE|HIGHFIVE", "A|Drinks on the new foreman!|CHEERS|CHEERS|ROUND"
      ),
      event(
         2,
         ANY,
         "A|Rock, paper, scissors for the tab.|POINT",
         "B|Fine. Ready?|NOD",
         "A|Rock, paper, scissors...|FIST_PUMP|FIST_PUMP",
         "B|Paper!|FIST_PUMP",
         "A|...I threw rock again.|FACEPALM|LAUGH"
      ),
      event(
         1,
         m -> m.music(),
         "A|Ohhh, I KNOW this one!|FIST_PUMP|NONE|CROWD=Here we go.../Oh no./Sing it!",
         "A|La la LAAA, la la la!|SWAY|SWAY|CROWD=La la laaa!/Woooo!/*sways*",
         "A|...I don't know the words.|SHRUG|LAUGH"
      ),
      event(1, ANY, "A|Has anyone seen my pickaxe?|THINK|NONE|CROWD=Nope./Which one?/Check your inventory!", "A|Oh wait. It's on my belt.|FACEPALM|LAUGH"),
      event(
         2,
         ANY,
         "A|I have something to ask you...|THINK",
         "A|Will you be my... mining partner?|NONE|NONE|CROWD=Aww!/Say yes!/Ooooh!",
         "B|Yes! A thousand times yes!|HIGH_FIVE|NONE|HIGHFIVE",
         "A|We're gonna find SO many diamonds!|FIST_PUMP|CLAP|CROWD=Woo!/Get a room!/To the happy couple!"
      ),
      event(
         2,
         ANY,
         "A|You look like you've had a long day.",
         "B|You have no idea.|SHRUG",
         "A|Next one's on me, friend.|CHEERS|NONE|ROUND",
         "B|...you're alright.|NOD"
      ),
      event(
         1,
         ANY,
         "A|Hey everyone! Why did the creeper cross the road?|POINT|NONE|CROWD=Why?/Here we go.../Tell us!",
         "A|To get to the other SSSSIDE!|LAUGH|NONE|CROWD=Boooo!/*groan*/Ha!/Terrible!",
         "A|Tough crowd.|SHRUG"
      ),
      event(1, m -> m.drunk(), "A|Hey! Watch this!|FIST_PUMP|NONE|JUMP", "A|...I meant to do that.|SHRUG|NONE|CROWD=Smooth!/Ha!/Sit down!"),
      event(2, m -> m.holding(), "A|Cheers, everybody!|CHEERS|CHEERS|CROWD=Cheers!/Salud!/Bottoms up!", "B|Bottoms up!|CHEERS"),
      event(
         2,
         m -> m.music(),
         "A|Dance-off. You and me.|POINT",
         "B|Oh, it's ON.|FIST_PUMP|NONE|CROWD=Dance-off!/Ooooh!",
         "A|*busts a move*|SWAY|CLAP",
         "B|*busts a better move*|SWAY|CLAP|CROWD=Woo!/Yeah!/Go!",
         "A|Okay, okay. You win.|SHRUG|FIST_PUMP"
      ),
      event(
         2,
         ANY,
         "A|I bet I can name every mob.|FIST_PUMP",
         "B|Go on then.|SHRUG",
         "A|Creeper, zombie, skeleton, uh... cow?|THINK|LAUGH",
         "B|That's four.|POINT|NONE|CROWD=Ha!/Four!/Nice try!",
         "A|...there's more than four?|FACEPALM"
      ),
      event(
         3,
         ANY,
         "A|Story time! So there I was, deep in a cave...|GESTURE",
         "B|Here we go.|FACEPALM",
         "A|...surrounded by forty creepers!|POINT",
         "C|Forty?!|SHAKE|NONE|CROWD=Forty?!/No way!",
         "A|Okay, two. But they were BIG.|SHRUG|LAUGH"
      ),
      event(1, ANY, "A|Whoops!|SHAKE|NONE|SMASH", "A|Sorry! Sorry. That one was empty.|FACEPALM|LAUGH|CROWD=Ohhh!/Smooth!/Taxi for one!"),
      event(
         2,
         ANY,
         "A|Heads or tails? Loser buys.|POINT",
         "B|Tails.|NOD|NONE|COIN",
         "A|...heads! Pay up!|FIST_PUMP|NONE|CROWD=Ha!/Pay up!/Ooh!",
         "B|Best of three?|SHRUG|LAUGH"
      ),
      event(
         1,
         ANY,
         "A|I brought my harp. Who wants a song?|WAVE|NONE|CROWD=Go on then!/Oh no.../Play something!",
         "A|*plays*|SWAY|NONE|TUNE",
         "A|*plays with feeling*|SWAY|SWAY|TUNE",
         "A|Thank you, thank you. I'm here all week.|NOD|CLAP|CROWD=More!/Bravo!/Don't quit your day job!"
      ),
      event(
         1,
         m -> m.rain(),
         "A|Did you hear that?|THINK|NONE|THUNDER",
         "A|Charged creeper weather, that.|SHAKE|NONE|CROWD=Nobody go outside!/Another round, then./Brr!"
      ),
      event(
         1,
         m -> m.night(),
         "A|Has anyone else not slept in three nights?|THINK|NONE|CROWD=Me!/Phantoms!/Go to bed!",
         "A|I hear flapping. I always hear flapping.|SHAKE|LAUGH"
      ),
      event(
         3,
         ANY,
         "A|Pub quiz! What do you need to make a cake?|POINT|NONE|CROWD=Milk!/Eggs!/Sugar!",
         "B|Milk, sugar, eggs, wheat!|FIST_PUMP",
         "C|And three buckets you never get back.|SHRUG|LAUGH|CROWD=Ha!/So true!/Point to that one!",
         "A|Point to the back table!|POINT|CLAP"
      ),
      event(
         2,
         ANY,
         "A|Excuse me, is this your seat?|THINK",
         "B|It's been my seat for six years.|NOD",
         "A|...I'll find another seat.|SHRUG|LAUGH|CROWD=Ooh!/Regulars, eh?/Ha!"
      ),
      event(
         2,
         ANY,
         "A|Hey, I'm Steve.|WAVE",
         "B|No, I'M Steve.|POINT",
         "A|...we're all Steve, aren't we?|THINK|NONE|CROWD=I'm Steve!/Steve!/I'm Alex, actually.",
         "B|Cheers, Steve.|CHEERS|CHEERS"
      ),
      event(
         2,
         m -> m.drinking(),
         "A|To absent friends.|CHEERS",
         "B|To absent friends.|CHEERS|NONE|CROWD=Absent friends./Hear, hear./*raises a glass*",
         "A|And to the ones who still owe me emeralds.|POINT|LAUGH"
      ),
      event(
         1,
         ANY,
         "A|I've lost my keys. Has anyone seen my keys?|THINK|NONE|CROWD=Check your pockets!/What keys?/Nope!",
         "A|...we don't have doors that need keys.|FACEPALM|LAUGH|CROWD=Ha!/Sit down!/Classic."
      ),
      event(
         2,
         ANY,
         "A|Bet you five emeralds the next one in orders a beer.|POINT",
         "B|You're on.|NOD",
         "A|Wait for it...|THINK",
         "B|...this could take all night.|SHRUG|LAUGH"
      ),
      event(1, m -> m.holding(), "A|This round's on me! Everybody!|FIST_PUMP|NONE|CROWD=Woo!/Legend!/Hero!/Cheers!", "A|...within reason.|SHRUG|LAUGH|ROUND"),
      event(
         2,
         m -> m.drunk(),
         "A|I'm gonna sing. I'm gonna sing a song.|FIST_PUMP|NONE|CROWD=Please don't./Oh no./Here we go!",
         "A|Ohhhh the creeper went BOOM...|SWAY|NONE|TUNE",
         "B|And that's enough of that.|FACEPALM|LAUGH|CROWD=Ha!/Encore!/Thank you!"
      ),
      event(
         3,
         ANY,
         "A|Raffle time! Everybody check your tickets!|WAVE|NONE|CROWD=Raffle!/Ooh!/Here we go!",
         "B|And the winning number is... seven!|POINT",
         "C|That's me! I won!|FIST_PUMP|CLAP|FIREWORK",
         "C|The prize is... a round for the whole table!|CHEERS|CHEERS|ROUND|CROWD=Congratulations!/Lucky!/Woo!"
      ),
      event(
         2,
         ANY,
         "A|I did it! I finally beat the Ender Dragon!|FIST_PUMP|NONE|FIREWORK",
         "B|No way. Prove it.|POINT|NONE|CROWD=Prove it!/Liar!/Show us!",
         "A|...it dropped an egg. It's in a chest. At home.|SHRUG",
         "B|Round for the dragon slayer!|CHEERS|CLAP|ROUND|CROWD=Dragon slayer!/Cheers!/Hear, hear!"
      ),
      event(
         2,
         ANY,
         "A|Hey! Where's my wallet?!|SHAKE",
         "B|Don't look at me.|SHRUG",
         "A|...oh. It's in my other pocket.|FACEPALM|LAUGH|CROWD=Ha!/Classic./Check your hat!"
      ),
      event(
         1,
         m -> m.holding(),
         "A|Three cheers for the bartender!|FIST_PUMP|NONE|CROWD=Hip hip!/Hooray!/Cheers!",
         "A|Hip hip...|CHEERS|CHEERS|CROWD=Hooray!/Hooray!/HOORAY!",
         "A|Now someone tip them!|POINT|NONE|TIP|CLINK"
      ),
      event(
         2,
         m -> m.drunk() && m.holding(),
         "A|Whoa! Sorry, mate!|SHAKE|NONE|SPILL",
         "B|My drink! Watch where you're going!|POINT|LAUGH|CROWD=Ohhh!/Oops!/Ha!",
         "A|Let me get you another.|CHEERS|NONE|ROUND"
      ),
      event(
         2,
         ANY,
         "A|Dart board. Loser buys.|POINT",
         "B|Easy.|FIST_PUMP|NONE|COIN",
         "A|Bullseye! ...on the wall next to it.|FACEPALM|LAUGH|CROWD=Ha!/Miss!/Nice try!"
      ),
      event(
         3,
         ANY,
         "A|Charades! Three words. Go.|POINT|NONE|CROWD=Charades!/Oh no./Ooh!",
         "B|*flaps arms*|GESTURE",
         "C|Chicken!|POINT|NONE|CROWD=Chicken!/Bat!/Phantom!",
         "B|Phantom!|FIST_PUMP|CLAP|CLINK"
      ),
      event(
         1,
         m -> m.music(),
         "A|Everybody knows this one! Sing along!|WAVE|NONE|CROWD=Oh no./Yes!/Here we go!",
         "A|La la LAAA!|SWAY|SWAY|TUNE|CROWD=La la laaa!/Sing it!/*sways*",
         "A|La la LAAAA!|FIST_PUMP|CLAP|TUNE|FIREWORK"
      ),
      event(
         2,
         ANY,
         "A|I know a secret about the bartender.|THINK",
         "B|Tell me.",
         "A|They never blink.|POINT|NONE|CROWD=Ooh!/Shh!/Creepy./He's right!",
         "B|...and I'm only noticing this now?|FACEPALM"
      ),
      event(2, ANY, "A|To new friends.|CHEERS|CHEERS|CLINK", "B|To new friends!|CHEERS|CHEERS|CLINK|CROWD=To new friends!/Cheers!/Hear, hear!"),
      event(
         2,
         ANY,
         "A|Thumb war. Best of three.|POINT",
         "B|You'll regret this.|FIST_PUMP|NONE|WRESTLE",
         "A|One, two, three, four...|NONE|NONE|CROWD=Go!/Get him!/Pin it!",
         "B|Pinned! Ha!|FIST_PUMP|FACEPALM"
      )
   );
   private static final ArrayDeque<BarTalk.Script> RECENT = new ArrayDeque<>();

   private static BarTalk.Script script(int roles, boolean event, Predicate<BarTalk.Mood> when, String... lines) {
      List<BarTalk.Line> list = new ArrayList<>();

      for (String raw : lines) {
         String[] p = raw.split("\\|");
         list.add(
            new BarTalk.Line(
               p[0].charAt(0) - 'A',
               p[1],
               p.length > 2 && !p[2].isEmpty() ? BarTalk.Emote.valueOf(p[2]) : BarTalk.Emote.NONE,
               p.length > 3 && !p[3].isEmpty() ? BarTalk.Emote.valueOf(p[3]) : BarTalk.Emote.NONE,
               p.length > 4 ? p[4] : ""
            )
         );
      }

      return new BarTalk.Script(roles, when, list, event);
   }

   private static BarTalk.Script chat(int roles, Predicate<BarTalk.Mood> when, String... lines) {
      return script(roles, false, when, lines);
   }

   private static BarTalk.Script event(int roles, Predicate<BarTalk.Mood> when, String... lines) {
      return script(roles, true, when, lines);
   }

   @Nullable
   public static BarTalk.Script pick(List<BarTalk.Script> from, int cast, BarTalk.Mood mood, RandomSource random) {
      List<BarTalk.Script> fits = new ArrayList<>();
      List<BarTalk.Script> fresh = new ArrayList<>();

      for (BarTalk.Script s : from) {
         if (s.roles() <= cast && (cast == 1 || s.roles() >= 2) && s.when().test(mood)) {
            fits.add(s);
            if (!RECENT.contains(s)) {
               fresh.add(s);
            }
         }
      }

      List<BarTalk.Script> pool = fresh.isEmpty() ? fits : fresh;
      if (pool.isEmpty()) {
         return null;
      } else {
         BarTalk.Script sx = pool.get(random.nextInt(pool.size()));
         if (sx.when() == ANY && random.nextInt(3) == 0) {
            BarTalk.Script again = pool.get(random.nextInt(pool.size()));
            if (again.when() != ANY) {
               sx = again;
            }
         }

         RECENT.addLast(sx);

         while (RECENT.size() > 24) {
            RECENT.removeFirst();
         }

         return sx;
      }
   }

   @Nullable
   public static BarTalk.Script pickEvent(int cast, int crowd, BarTalk.Mood mood, RandomSource random) {
      List<BarTalk.Script> fits = new ArrayList<>();

      for (BarTalk.Script s : EVENTS) {
         if (s.roles() <= cast && (s.roles() > 1 || crowd > 0) && s.when().test(mood)) {
            fits.add(s);
         }
      }

      return fits.isEmpty() ? null : fits.get(random.nextInt(fits.size()));
   }

   public static String fill(String text, BarTalk.Mood mood) {
      String other = mood.other() == null ? "that guy" : mood.other();
      text = text.replace("{drink}", mood.drink()).replace("{bartender}", mood.bartender()).replace("{other}", other);
      return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
   }

   private BarTalk() {
   }

   public static final class Conversation {
      final BarTalk.Script script;
      final BarPatron[] cast;
      final BarTalk.Mood mood;
      final List<BarPatron> crowd;
      @Nullable
      final Player listener;
      int line = -1;
      int timer = 10;

      Conversation(BarTalk.Script script, BarPatron[] cast, BarTalk.Mood mood, @Nullable Player listener) {
         this(script, cast, mood, listener, new ArrayList<>());
      }

      Conversation(BarTalk.Script script, BarPatron[] cast, BarTalk.Mood mood, @Nullable Player listener, List<BarPatron> crowd) {
         if (cast.length > 0 && cast[0] != null) {
            PubDebug.log(cast[0], "starts a conversation with " + (cast.length - 1) + " other(s)" + (listener != null ? " and the player" : ""));
         }

         this.script = script;
         this.cast = cast;
         this.mood = mood;
         this.listener = listener;
         this.crowd = new ArrayList<>(crowd);
      }

      void join(BarPatron listener) {
         if (!this.crowd.contains(listener) && !this.involves(listener)) {
            this.crowd.add(listener);
         }
      }

      public boolean hasListener(BarPatron p) {
         return this.crowd.contains(p);
      }

      public boolean involves(BarPatron p) {
         for (BarPatron c : this.cast) {
            if (c == p) {
               return true;
            }
         }

         return false;
      }

      boolean tick() {
         for (BarPatron p : this.cast) {
            if (!p.isAlive() || !p.canChat()) {
               return false;
            }
         }

         if (this.listener == null || this.listener.isAlive() && !(this.listener.distanceToSqr(this.cast[0]) > 64.0)) {
            BarPatron speaker = this.line >= 0 ? this.cast[this.script.lines().get(this.line).who()] : null;

            for (BarPatron px : this.cast) {
               if (px == speaker) {
                  if (this.listener != null) {
                     px.lookAtHead(this.listener);
                  } else if (this.cast.length > 1) {
                     for (BarPatron o : this.cast) {
                        if (o != px) {
                           px.lookAtHead(o);
                           break;
                        }
                     }
                  } else if (!this.crowd.isEmpty()) {
                     px.lookAtHead((Entity)this.crowd.get(px.tickCount / 60 % this.crowd.size()));
                  }
               } else if (speaker != null) {
                  px.lookAtHead(speaker);
               }
            }

            if (speaker != null) {
               for (BarPatron pxx : this.crowd) {
                  if (pxx.isAlive() && pxx.talk == null && pxx.distanceToSqr(speaker) < 196.0) {
                     pxx.lookAtHead(speaker);
                  }
               }
            }

            if (--this.timer > 0) {
               return true;
            } else if (++this.line >= this.script.lines().size()) {
               return false;
            } else {
               BarTalk.Line l = this.script.lines().get(this.line);
               BarPatron who = this.cast[l.who()];
               String text = BarTalk.fill(l.text(), this.mood);
               who.say(text);
               who.emote(l.emote());

               for (BarPatron pxxx : this.cast) {
                  if (pxxx != who && !pxxx.emoteBusy()) {
                     if (l.reaction() != BarTalk.Emote.NONE) {
                        pxxx.emoteLater(l.reaction(), 8 + pxxx.getRandom().nextInt(10));
                     } else if (pxxx.getRandom().nextInt(5) == 0) {
                        pxxx.emoteLater(BarTalk.Emote.NOD, 10 + pxxx.getRandom().nextInt(10));
                     }
                  }
               }

               if (l.reaction() != BarTalk.Emote.NONE && !this.script.event()) {
                  for (BarPatron pxxxx : this.crowd) {
                     if (pxxxx.isAlive() && pxxxx.talk == null && !pxxxx.emoteBusy() && pxxxx.getRandom().nextInt(3) != 0) {
                        pxxxx.emoteLater(l.reaction(), 10 + pxxxx.getRandom().nextInt(12));
                     }
                  }
               }

               if (!l.action().isEmpty()) {
                  who.act(l.action(), this, l);
               }

               this.timer = 36 + text.length() * 2;
               return true;
            }
         } else {
            return false;
         }
      }
   }

   public static enum Emote {
      NONE(0),
      NOD(20),
      SHAKE(24),
      LAUGH(40),
      SHRUG(26),
      POINT(30),
      FACEPALM(40),
      WAVE(32),
      GESTURE(44),
      THINK(50),
      CHEERS(30),
      FIST_PUMP(26),
      ARM_WRESTLE(110),
      HIGH_FIVE(22),
      SLEEP(170),
      CLAP(40),
      SWAY(80);

      public final int ticks;

      private Emote(int ticks) {
         this.ticks = ticks;
      }
   }

   public static record Line(int who, String text, BarTalk.Emote emote, BarTalk.Emote reaction, String action) {
   }

   public static record Mood(
      boolean music,
      boolean night,
      boolean rain,
      boolean drunk,
      boolean fightNearby,
      boolean holding,
      boolean smoking,
      String drink,
      String bartender,
      boolean friends,
      @Nullable String other,
      boolean busy,
      boolean quickService,
      boolean slowService,
      boolean regular
   ) {
      public boolean drinking() {
         return this.holding && !this.smoking;
      }

      public boolean gossip() {
         return this.other != null;
      }
   }

   public static record Script(int roles, Predicate<BarTalk.Mood> when, List<BarTalk.Line> lines, boolean event) {
   }
}
