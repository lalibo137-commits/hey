package com.drunkard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Medieval tavern style: the skin bit that marks medieval folk, and the speech they use. */
public final class Medieval {
   /** Set on patron and barkeep skins of a register whose customer style is Medieval. */
   public static final int SKIN_BIT = 4194304;
   private static final List<Object[]> RULES = new ArrayList<>();
   private static final Map<String, String> LINES = Map.ofEntries(
      Map.entry("Hey! I work here!", "Oi! I serve this house!"),
      Map.entry("Ow! What was that for, boss?", "Ow! What was that for, m'lord?"),
      Map.entry("Evening, boss.", "Good even, m'lord."),
      Map.entry("All quiet here.", "All is quiet in the hall."),
      Map.entry("Keep the taps full and I'll keep them happy.", "Keep the casks full and I shall keep them merry."),
      Map.entry("Need anything, boss?", "Dost thou need aught, m'lord?"),
      Map.entry("On it, boss!", "At once, m'lord!"),
      Map.entry("Busy busy!", "No rest for the wicked!"),
      Map.entry("Somebody put some mugs on the shelf!", "Someone fetch tankards for the shelf!"),
      Map.entry("We need shot glasses on the shelf!", "We need cups upon the shelf!"),
      Map.entry("Ran out!", "The cask is dry!"),
      Map.entry("Coming right up!", "Straightaway!"),
      Map.entry("One sec!", "Anon, anon!"),
      Map.entry("I've got you.", "I shall see to thee."),
      Map.entry("Be right with you.", "I'll be with thee presently."),
      Map.entry("Here you go!", "Here, take it!"),
      Map.entry("Enjoy!", "Drink deep!"),
      Map.entry("There you are.", "There thou art."),
      Map.entry("Cheers!", "Wassail!"),
      Map.entry("Here you go, enjoy your drink!", "Here, drink deep and be merry!"),
      Map.entry("One pint, as ordered.", "One flagon, as thou didst ask."),
      Map.entry("There you are. Anything else, just give me a shout.", "There thou art. Call out shouldst thou want aught else."),
      Map.entry("Fresh from the tap!", "Fresh from the cask!"),
      Map.entry("Give me a wave if you need anything.", "Wave to me shouldst thou need aught."),
      Map.entry("Holler if you want another.", "Call out if thou wouldst have another."),
      Map.entry("Enjoy your night!", "Fair evening to thee!"),
      Map.entry("Can I get you another?", "Shall I fetch thee another?"),
      Map.entry("Same again?", "The same again?"),
      Map.entry("Another round?", "Another round, friends?"),
      Map.entry("Go on then!", "Go on, then!"),
      Map.entry("Why not.", "Why not, I say."),
      Map.entry("You read my mind.", "Thou readest my very thoughts."),
      Map.entry("I'm good, thanks.", "I am content, I thank thee."),
      Map.entry("Not just yet.", "Not yet awhile."),
      Map.entry("Maybe in a bit.", "Mayhap in a little while."),
      Map.entry("That smells good.", "That smoke smells sweet."),
      Map.entry("Mind the holder, now.", "Mind the holder, now."),
      Map.entry("Nice smoke.", "A fine pipe of leaf."),
      Map.entry("Cheers, it's a good one.", "My thanks, 'tis a good one."),
      Map.entry("Only on a night out.", "Only on a feast night."),
      Map.entry("Don't tell my doctor.", "Tell not the apothecary."),
      Map.entry("Perfect, thanks!", "Perfect, I thank thee!"),
      Map.entry("Best in town.", "Finest in the shire."),
      Map.entry("Colder would be nice. Kidding!", "Colder would please me. I jest!"),
      Map.entry("Busy night, isn't it?", "A busy night, is it not?"),
      Map.entry("Everything alright over here?", "Is all well over here?"),
      Map.entry("Shout if you need anything.", "Call out if thou needest aught."),
      Map.entry("Good to see you in again.", "Good to see thee here again."),
      Map.entry("Haven't seen you around.", "I have not seen thee hereabouts."),
      Map.entry("First time here?", "Is this thy first time here?"),
      Map.entry("All good, thanks.", "All is well, I thank thee."),
      Map.entry("Couldn't be better.", "Never better."),
      Map.entry("Keep them coming!", "Keep them coming, barkeep!"),
      Map.entry("Nice place you've got.", "A fine house thou keepest."),
      Map.entry("I'll be back.", "I shall return."),
      Map.entry("Heard good things.", "I heard fair things spoken of it."),
      Map.entry("Mind the step on your way out.", "Mind the step on thy way out."),
      Map.entry("Last orders are late tonight.", "Last call comes late this eve."),
      Map.entry("Try the lager sometime.", "Try the lager some eve."),
      Map.entry("Will do.", "That I shall."),
      Map.entry("Good to know!", "Good to know!"),
      Map.entry("Wait up!", "Wait for me!"),
      Map.entry("Yeah, I'm beat too.", "Aye, I am weary too."),
      Map.entry("Night, everyone!", "Good night, one and all!"),
      Map.entry("Whoa, let's go.", "Gadzooks, let us away."),
      Map.entry("Get him!", "Seize him!"),
      Map.entry("Go on, lads!", "Have at him, lads!"),
      Map.entry("Teach him a lesson!", "Teach the knave a lesson!"),
      Map.entry("Evening!", "Good even!"),
      Map.entry("Night!", "Good night!"),
      Map.entry("Somebody call me a cab.", "Somebody fetch me a cart."),
      Map.entry("Honey, I'm home!", "Hark, I am home!"),
      Map.entry("Closed? Aw, man.", "Closed? Alas and alack."),
      Map.entry("Best... night... ever.", "Finest... night... ever."),
      Map.entry("Put it on my tab... kidding!", "Put it on my tab... I jest!")
   );

   static {
      // Whole phrases first (longest, most specific), then words. Each rule: regex, replacement.
      phrase("see you next time", "until we meet again");
      phrase("it's", "it is");
      phrase("what'd you", "what didst thou");
      phrase("you had", "thou hadst");
      phrase("you spilled", "thou hast spilt");
      phrase("you looking", "art thou looking");
      phrase("you psycho", "thou madman");
      phrase("you idiot", "thou fool");
      phrase("you wanna", "thou wishest to");
      phrase("sorry about that", "my apologies for that");
      phrase("that'll", "that will");
      phrase("they'll", "they shall");
      phrase("he's got", "he hath");
      phrase("she's got", "she hath");
      phrase("he's", "he is");
      phrase("she's", "she is");
      phrase("outta", "out of");
      phrase("hang on a sec", "hold a moment");
      phrase("get lost", "begone");
      phrase("back off", "stand back");
      phrase("good stuff", "fine wares");
      word("sec", "moment");
      word("lawyer", "magistrate");
      word("hooray", "huzzah");
      word("folks", "good folk");
      phrase("a lot of", "a heap of");
      phrase("how'd you", "how didst thou");
      phrase("you ever", "dost thou ever");
      phrase("why don't we", "why do we not");
      phrase("don't we", "do we not");
      phrase("don't you", "dost thou not");
      phrase("who's got", "who hath");
      phrase("everyone okay", "is everyone well");
      phrase("you okay", "art thou well");
      word("congrats", "felicitations");
      phrase("no way", "by my troth");
      phrase("you and me", "thou and I");
      phrase("of you", "of ye");
      phrase("you make", "thou makest");
      phrase("it's been", "it hath been");
      phrase("you're alright", "thou art a good sort");
      phrase("you win", "thou winnest");
      phrase("aren't we", "are we not");
      phrase("aren't you", "art thou not");
      word("alright", "very well");
      word("hey", "ho");
      word("yo", "ho");
      phrase("three cheers", "three cheers");
      phrase("bet you ever", "wager thou ever");
      word("cheers", "wassail");
      word("probably", "belike");
      word("kidding", "jesting");
      phrase("you're kidding", "thou jestest");
      phrase("night, friend", "good night, friend");
      phrase("didn't you", "didst thou not");
      phrase("didn't i", "did I not");
      phrase("mad busy", "fearsome busy");
      phrase("how've you been", "how hast thou fared");
      phrase("say hi", "bid them good morrow");
      phrase("i'd better", "I had best");
      phrase("behind a bar", "behind a counter");
      phrase("bar top", "counter");
      word("bartenders", "barkeeps");
      phrase("you're not filming it", "thou shalt tell no one of it");
      word("tycoon", "merchant lord");
      phrase("as long as you like", "as long as thou wilt");
      phrase("how you feel", "how thou feelest");
      phrase("let you know", "send thee word");
      phrase("you ever been", "hast thou ever been");
      phrase("be nice", "be kind");
      word("screenshot", "sketch");
      word("tourist", "pilgrim");
      word("weekends", "feast days");
      phrase("tonight's", "this eve is");
      phrase("top you up, miss", "fill thy cup, m'lady");
      phrase("top you up, sir", "fill thy cup, good sir");
      phrase("how's", "how fares");
      phrase("how are you", "how fare thee");
      phrase("how's it going", "how goes it");
      phrase("what's up", "what news");
      phrase("thank you so much", "I thank thee kindly");
      phrase("thank you", "I thank thee");
      phrase("thanks a lot", "many thanks");
      phrase("no thanks", "nay, I thank thee");
      phrase("see you later", "fare thee well");
      phrase("see you around", "fare thee well");
      phrase("good night", "good night");
      phrase("good morning", "good morrow");
      phrase("good evening", "good even");
      phrase("excuse me", "pardon me");
      phrase("hurry up", "make haste");
      phrase("come on", "come now");
      phrase("oh my god", "by my troth");
      phrase("oh my", "by my troth");
      phrase("oh no", "alas");
      phrase("of course", "of course");
      phrase("you know what", "knowest thou what");
      phrase("you know", "thou knowest");
      phrase("y'know", "thou knowest");
      phrase("a lot", "a great deal");
      phrase("right now", "this very moment");
      phrase("for real", "in truth");
      phrase("to be honest", "in truth");
      phrase("you guys", "ye good folk");
      phrase("you don't", "thou dost not");
      phrase("you didn't", "thou didst not");
      phrase("you can't", "thou canst not");
      phrase("you won't", "thou wilt not");
      phrase("you haven't", "thou hast not");
      phrase("you aren't", "thou art not");
      phrase("you've got", "thou hast");
      phrase("see ya", "fare thee well");
      phrase("what'd", "what did");
      phrase("shot glass", "small cup");
      phrase("shot glasses", "small cups");
      phrase("scoot over", "make room");
      word("aw", "alas");
      word("joking", "jesting");
      word("woo", "huzzah");
      word("cab", "cart");
      word("menu", "bill of fare");
      phrase("last night", "yestereve");
      phrase("last week", "a sennight past");
      phrase("next week", "within the sennight");
      phrase("this morning", "this morn");
      phrase("tonight", "this eve");
      phrase("today", "this day");
      phrase("tomorrow", "on the morrow");
      phrase("at all", "at all");
      phrase("are you", "art thou");
      phrase("were you", "wert thou");
      phrase("do you", "dost thou");
      phrase("did you", "didst thou");
      phrase("have you", "hast thou");
      phrase("can you", "canst thou");
      phrase("will you", "wilt thou");
      phrase("would you", "wouldst thou");
      phrase("could you", "couldst thou");
      phrase("should you", "shouldst thou");
      phrase("you are", "thou art");
      phrase("you were", "thou wert");
      phrase("you have", "thou hast");
      phrase("you're", "thou art");
      phrase("you've", "thou hast");
      phrase("you'll", "thou wilt");
      phrase("you'd", "thou wouldst");
      phrase("you do", "thou dost");
      phrase("you did", "thou didst");
      phrase("you can", "thou canst");
      phrase("you will", "thou wilt");
      phrase("you would", "thou wouldst");
      phrase("you could", "thou couldst");
      phrase("you should", "thou shouldst");
      phrase("you need", "thou needest");
      phrase("you want", "thou wouldst have");
      phrase("you think", "thou thinkest");
      phrase("you said", "thou saidst");
      phrase("you see", "thou seest");
      phrase("you look", "thou lookest");
      phrase("you got", "thou hast");
      phrase("you've got", "thou hast");
      phrase("i've got", "I have");
      phrase("i'm gonna", "I shall");
      phrase("i'm going to", "I shall");
      phrase("gonna", "going to");
      phrase("wanna", "wish to");
      phrase("gotta", "must");
      phrase("isn't it", "is it not");
      phrase("isn't", "is not");
      phrase("aren't", "are not");
      phrase("wasn't", "was not");
      phrase("weren't", "were not");
      phrase("don't", "do not");
      phrase("doesn't", "doth not");
      phrase("didn't", "did not");
      phrase("can't", "cannot");
      phrase("couldn't", "could not");
      phrase("won't", "shall not");
      phrase("wouldn't", "would not");
      phrase("shouldn't", "should not");
      phrase("haven't", "have not");
      phrase("hasn't", "hath not");
      phrase("i'm", "I am");
      phrase("i've", "I have");
      phrase("i'll", "I shall");
      phrase("i'd", "I would");
      phrase("we're", "we are");
      phrase("we've", "we have");
      phrase("we'll", "we shall");
      phrase("they're", "they are");
      phrase("they've", "they have");
      phrase("that's", "that is");
      phrase("what's", "what is");
      phrase("where's", "where is");
      phrase("who's", "who is");
      phrase("there's", "there be");
      phrase("here's", "here is");
      phrase("let's", "let us");
      phrase("does", "doth");
      phrase("has", "hath");
      phrase("yourself", "thyself");
      word("your", "thy");
      word("yours", "thine");
      word("you", "thee");
      word("ya", "thee");
      word("hi", "well met");
      word("hello", "good morrow");
      word("bye", "farewell");
      word("goodbye", "farewell");
      word("thanks", "my thanks");
      word("yeah", "aye");
      word("yep", "aye");
      word("yup", "aye");
      word("yes", "aye");
      word("nope", "nay");
      word("nah", "nay");
      word("okay", "very well");
      word("ok", "very well");
      word("please", "prithee");
      word("maybe", "mayhap");
      word("perhaps", "perchance");
      word("really", "truly");
      word("very", "right");
      word("seriously", "truly");
      word("honestly", "in truth");
      word("actually", "in truth");
      word("totally", "wholly");
      word("anyway", "howsoever");
      word("awesome", "splendid");
      word("amazing", "wondrous");
      word("cool", "splendid");
      word("great", "most fine");
      word("nice", "fine");
      word("wow", "zounds");
      word("whoa", "gadzooks");
      word("ugh", "fie");
      word("damn", "blast");
      word("crap", "dung");
      word("stupid", "witless");
      word("idiot", "knave");
      word("guy", "fellow");
      word("guys", "fellows");
      word("dude", "friend");
      word("bro", "brother");
      word("buddy", "friend");
      word("pal", "friend");
      word("mate", "friend");
      word("kid", "lad");
      word("kids", "children");
      word("boss", "master");
      word("money", "coin");
      word("cash", "coin");
      word("bucks", "coins");
      word("bar", "tavern");
      word("pub", "tavern");
      word("bartender", "barkeep");
      word("beer", "ale");
      word("beers", "ales");
      word("mug", "tankard");
      word("mugs", "tankards");
      word("glass", "cup");
      word("glasses", "cups");
      word("job", "trade");
      word("phone", "letter");
      word("car", "cart");
      word("doctor", "apothecary");
      word("police", "town guard");
      word("cops", "guards");
      word("weekend", "feast day");
      word("party", "feast");
      word("minutes", "moments");
      word("minute", "moment");
      word("seconds", "breaths");
      word("before", "ere");
      word("often", "oft");
      word("nothing", "naught");
      word("anything", "aught");
   }

   // Built once in the static initializer: the client and the integrated server both call speak().
   private static final Pattern COMBINED;

   static {
      RULES.sort((a, b) -> ((String)b[0]).length() - ((String)a[0]).length());
      StringBuilder alt = new StringBuilder();
      for (Object[] rule : RULES) {
         alt.append(alt.length() == 0 ? "" : "|").append('(').append(Pattern.quote((String)rule[0])).append(')');
      }

      COMBINED = Pattern.compile("(?<![\\w'])(?:" + alt + ")(?![\\w'])", Pattern.CASE_INSENSITIVE);
   }

   private static void phrase(String from, String to) {
      RULES.add(new Object[]{from, to});
   }

   private static void word(String from, String to) {
      phrase(from, to);
   }

   /** True for skins painted and spoken in the medieval style. */
   public static boolean is(int skin) {
      return (skin & SKIN_BIT) != 0;
   }

   /** Clamps a skin variant like the original code did, keeping the medieval bit. */
   public static int keep(int skin, int range) {
      return Math.floorMod(skin, range) | skin & SKIN_BIT;
   }

   public static String speak(int skin, String line) {
      return is(skin) ? speak(line) : line;
   }

   public static String speak(String line) {
      if (line == null || line.isEmpty()) {
         return line;
      }

      String exact = LINES.get(line);
      if (exact != null) {
         return exact;
      }

      String out = line;
      List<String> protectedParts = new ArrayList<>();
      Matcher braces = Pattern.compile("\\{[a-z]+\\}|\\$\\d+").matcher(out);
      StringBuilder masked = new StringBuilder();
      while (braces.find()) {
         protectedParts.add(braces.group());
         braces.appendReplacement(masked, Matcher.quoteReplacement("\u0001" + (protectedParts.size() - 1) + "\u0002"));
      }
      braces.appendTail(masked);
      out = masked.toString();

      Matcher m = COMBINED.matcher(out);
      StringBuilder sb = new StringBuilder();
      while (m.find()) {
         String to = m.group();
         for (int g = 1; g <= m.groupCount(); g++) {
            if (m.group(g) != null) {
               to = (String)RULES.get(g - 1)[1];
               break;
            }
         }

         m.appendReplacement(sb, Matcher.quoteReplacement(matchCase(m.group(), to, sentenceStart(out, m.start()))));
      }

      m.appendTail(sb);
      out = sb.toString();

      // "thee" works as an object; as the subject before a verb it reads better as "thou".
      out = out.replaceAll("(^|[.!?] |\\.\\.\\.)Thee (\\w)", "$1Thou $2");
      out = out.replaceAll("(^|[.!?] |\\.\\.\\.|\\* )It is ", "$1'Tis ");
      out = out.replaceAll("(^|[.!?] )Thee\\?", "$1And thou?");
      out = out.replaceAll("(^|[.!?] |\\.\\.\\. ?|\\* )Thee\\b", "$1Thou");
      out = out.replaceAll("(^|[.!?] )Bet (thee|thou) ", "$1I wager $2 ");
      out = out.replaceAll("\\bthee (never|always|ever|really|just|still|also|only|can|could|would|should|will|shall|must|might|may|do|did|know|think|need|want|get|go|got|said|say|see|look|take|have|had|are|were|was|brought|finish|run|dance|deserve|headed|mean|owe|came|made|met|saw|heard|found|lost|left|told)\\b", "thou $1");
      out = out.replaceAll("\\b([aA]) (ale|ales|aye)\\b", "$1n $2");
      out = out.replaceAll("(^|[.!?] |\\.\\.\\.|\\* )It was ", "$1'Twas ");
      // Money: "$5" reads as coins in a medieval tavern.
      for (int i = 0; i < protectedParts.size(); i++) {
         String part = protectedParts.get(i);
         if (part.startsWith("$")) {
            String n = part.substring(1);
            part = n + (n.equals("1") ? " coin" : " coins");
         }

         out = out.replace("\u0001" + i + "\u0002", part);
      }

      return out;
   }

   private static boolean sentenceStart(String s, int at) {
      int i = at - 1;
      while (i >= 0 && (s.charAt(i) == ' ' || s.charAt(i) == '"' || s.charAt(i) == '*')) {
         i--;
      }

      return i < 0 || ".!?".indexOf(s.charAt(i)) >= 0;
   }

   private static String matchCase(String original, String to, boolean start) {
      String firstWord = original.split("[\\s']", 2)[0];
      if (firstWord.length() > 1 && firstWord.equals(firstWord.toUpperCase()) && firstWord.chars().anyMatch(Character::isLetter)) {
         int sp = to.indexOf(' ');
         return sp < 0 ? to.toUpperCase() : to.substring(0, sp).toUpperCase() + to.substring(sp);
      } else if (to.startsWith("I ") || to.equals("I")) {
         return to;
      } else if (Character.isUpperCase(original.charAt(0)) || start) {
         return to.isEmpty() ? to : (to.charAt(0) == '\'' && to.length() > 1 ? "'" + Character.toUpperCase(to.charAt(1)) + to.substring(2) : Character.toUpperCase(to.charAt(0)) + to.substring(1));
      } else {
         return to;
      }
   }

   private Medieval() {
   }
}
