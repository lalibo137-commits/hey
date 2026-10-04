package com.drunkard;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class SkinPainter {
   static final int TOP = 0;
   static final int BOTTOM = 1;
   static final int RIGHT = 2;
   static final int FRONT = 3;
   static final int LEFT = 4;
   static final int BACK = 5;
   static final SkinPainter.Base STEVE = base(
      new Object[]{
         SkinPainter.Part.HAIR,
         3351569,
         2825741,
         4139541,
         2365448,
         3417362,
         2497034,
         SkinPainter.Part.BEARD,
         4793616,
         4332810,
         SkinPainter.Part.LIPS,
         7815733,
         SkinPainter.Part.SKIN,
         10183497,
         11170393,
         11762014,
         12026731,
         9460031,
         9395774,
         8475449,
         9723966,
         7752499,
         6963248,
         SkinPainter.Part.IRIS,
         5389705,
         SkinPainter.Part.SHIRT,
         44975,
         42148,
         300437,
         362632,
         227962,
         52428,
         32639,
         26728,
         703676,
         SkinPainter.Part.TROUSERS,
         3813769,
         4273563,
         4602533,
         SkinPainter.Part.SHOES,
         4868682,
         5592405,
         4144959,
         3618615,
         2631720
      },
      Map.of(
         SkinPainter.Part.HAIR,
         3351569,
         SkinPainter.Part.BEARD,
         4793616,
         SkinPainter.Part.LIPS,
         7815733,
         SkinPainter.Part.SKIN,
         11170393,
         SkinPainter.Part.IRIS,
         5389705,
         SkinPainter.Part.SHIRT,
         44975,
         SkinPainter.Part.TROUSERS,
         4273563,
         SkinPainter.Part.SHOES,
         4868682
      )
   );
   static final SkinPainter.Base ALEX = base(
      new Object[]{
         SkinPainter.Part.HAIR,
         15043903,
         15439935,
         15968344,
         14582062,
         SkinPainter.Part.SKIN,
         15719103,
         15454384,
         14664866,
         13940623,
         SkinPainter.Part.LIPS,
         15711153,
         SkinPainter.Part.IRIS,
         2318884,
         SkinPainter.Part.SHIRT,
         9223818,
         8106616,
         7449454,
         9750674,
         8829316,
         SkinPainter.Part.TRIM,
         8421978,
         9737835,
         SkinPainter.Part.BELT,
         2839592,
         1587222,
         SkinPainter.Part.TROUSERS,
         6570284,
         7491126,
         8148798,
         7095854,
         7688248,
         6044455,
         SkinPainter.Part.SHOES,
         5789784,
         6645093,
         7303023,
         5197647,
         4144959,
         3552822,
         2631720
      },
      Map.of(
         SkinPainter.Part.HAIR,
         15439935,
         SkinPainter.Part.SKIN,
         15719103,
         SkinPainter.Part.LIPS,
         15711153,
         SkinPainter.Part.IRIS,
         2318884,
         SkinPainter.Part.SHIRT,
         9223818,
         SkinPainter.Part.TRIM,
         8421978,
         SkinPainter.Part.BELT,
         2839592,
         SkinPainter.Part.TROUSERS,
         8148798,
         SkinPainter.Part.SHOES,
         6645093
      )
   );
   private static final int[] SKIN = new int[]{15980994, 15719103, 14726286, 13671022, 13010498, 11170393, 10183497, 9262372, 7225122, 5911072};
   private static final int[] HAIR = new int[]{
      3351569, 2759184, 1842208, 3875860, 5583641, 6961690, 9198123, 11564079, 14067802, 14926458, 15439935, 10697246, 8026746, 12434877
   };
   private static final int[] DYED = new int[]{13652106, 3829712, 9064400, 3127480};
   private static final int[] EYES = new int[]{5389705, 3895256, 2318884, 5913118, 6974070, 9071146, 2759184};
   private static final int[] TEES = new int[]{
      15263970, 2763310, 9079440, 2899558, 4086330, 8002608, 13148224, 6982320, 5925434, 6965810, 44975, 9223818, 10104880
   };
   private static final int[] HOODIES = new int[]{9079440, 2763310, 9054762, 2899558, 4086330, 5925498, 13153418, 6965810};
   private static final int[] JACKETS = new int[]{2763310, 5913122, 4876954, 5925434, 8006186, 3816010};
   private static final int[] SUITS = new int[]{2237226, 2899558, 4868690, 3811874, 5909034};
   private static final int[] TIES = new int[]{11020330, 2899558, 4090426, 14201914, 6962570, 1973794};
   private static final int[] FLANNELS = new int[]{11020330, 2902650, 4090426, 6965802, 5921378};
   private static final int[] SWEATERS = new int[]{13153418, 8002608, 2899558, 4090426, 15262416, 9067066, 4868690};
   private static final int[] JEANS = new int[]{3824272, 2767456, 1974832, 6982328, 4273563};
   private static final int[] CHINOS = new int[]{13152890, 5925434, 2899558, 6965802, 4868690, 8148798};
   private static final int[] JOGGERS = new int[]{5921378, 1973794, 2899558, 4086330};
   private static final int[] TUNICS = new int[]{9223818, 5933760, 11020330, 14201914, 6962570, 15234090, 44975, 13153418};
   private static final int[] DRESSES = new int[]{11020330, 2899558, 1973794, 4090426, 13658778, 6962570, 14201914};
   private static final int[] HATS = new int[]{9054762, 2899558, 1973794, 4086330, 6965802, 5921378};
   static final int CLEAR = -2;

   private static SkinPainter.Base base(Object[] table, Map<SkinPainter.Part, Integer> reference) {
      Map<Integer, SkinPainter.Part> parts = new HashMap<>();
      SkinPainter.Part current = null;

      for (Object o : table) {
         if (o instanceof SkinPainter.Part p) {
            current = p;
         } else {
            parts.put((Integer)o, current);
         }
      }

      return new SkinPainter.Base(parts, reference);
   }

   static <T> T pick(Random r, T[] values) {
      return values[r.nextInt(values.length)];
   }

   static int pick(Random r, int[] values) {
      return values[r.nextInt(values.length)];
   }

   public static SkinPainter.Look roll(int variant) {
      Random r = new Random((long)variant * -7046029254386353131L + 25214903917L);
      SkinPainter.Look l = new SkinPainter.Look();
      l.seed = r.nextInt();
      l.alex = (variant & 1) == 1;
      l.skin = l.alex ? 15719103 : 11170393;
      l.hair = 3351569;
      l.eyes = 5389705;
      l.hairStyle = SkinPainter.Hair.CLASSIC;
      l.facial = SkinPainter.Facial.BEARD;
      l.outfit = l.alex
         ? pick(
            r,
            new SkinPainter.Outfit[]{
               SkinPainter.Outfit.TUNIC,
               SkinPainter.Outfit.TUNIC,
               SkinPainter.Outfit.TUNIC,
               SkinPainter.Outfit.TEE,
               SkinPainter.Outfit.TEE,
               SkinPainter.Outfit.TEE,
               SkinPainter.Outfit.POLO,
               SkinPainter.Outfit.HOODIE,
               SkinPainter.Outfit.HOODIE,
               SkinPainter.Outfit.FLANNEL,
               SkinPainter.Outfit.FLANNEL,
               SkinPainter.Outfit.JACKET,
               SkinPainter.Outfit.JACKET,
               SkinPainter.Outfit.SWEATER,
               SkinPainter.Outfit.SWEATER,
               SkinPainter.Outfit.OVERALLS
            }
         )
         : pick(
            r,
            new SkinPainter.Outfit[]{
               SkinPainter.Outfit.TEE,
               SkinPainter.Outfit.TEE,
               SkinPainter.Outfit.TEE,
               SkinPainter.Outfit.TEE,
               SkinPainter.Outfit.TEE,
               SkinPainter.Outfit.POLO,
               SkinPainter.Outfit.POLO,
               SkinPainter.Outfit.HOODIE,
               SkinPainter.Outfit.HOODIE,
               SkinPainter.Outfit.FLANNEL,
               SkinPainter.Outfit.FLANNEL,
               SkinPainter.Outfit.JACKET,
               SkinPainter.Outfit.JACKET,
               SkinPainter.Outfit.SWEATER,
               SkinPainter.Outfit.VEST,
               SkinPainter.Outfit.OVERALLS
            }
         );
      l.tee = pick(r, SkinPainter.Tee.values());
      int own = l.alex ? 9223818 : '꾯';

      l.top = switch (l.outfit) {
         case HOODIE -> pick(r, HOODIES);
         case JACKET -> pick(r, JACKETS);
         case FLANNEL -> pick(r, FLANNELS);
         case SWEATER -> pick(r, SWEATERS);
         case VEST -> pick(r, SUITS);
         case TUNIC -> r.nextInt(3) == 0 ? own : pick(r, TUNICS);
         default -> r.nextInt(4) == 0 ? own : pick(r, TEES);
      };

      do {
         l.accent = pick(r, TEES);
      } while (l.accent == l.top || distance(l.accent, l.top) < 60.0);

      l.under = l.outfit == SkinPainter.Outfit.VEST ? 15592936 : (r.nextInt(3) == 0 ? own : pick(r, TEES));
      if (distance(l.under, l.top) < 60.0) {
         l.under = 15592936;
      }

      l.tie = pick(r, TIES);
      l.legs = l.outfit == SkinPainter.Outfit.OVERALLS
         ? SkinPainter.Legs.JEANS
         : (r.nextInt(5) == 0 ? SkinPainter.Legs.JOGGERS : (r.nextBoolean() ? SkinPainter.Legs.JEANS : SkinPainter.Legs.CHINOS));
      int var4;
      if (l.outfit == SkinPainter.Outfit.OVERALLS) {
         var4 = pick(r, JEANS);
      } else if (r.nextBoolean()) {
         var4 = l.alex ? 8148798 : 4273563;
      } else {
         switch (l.legs) {
            case JEANS:
               var4 = pick(r, JEANS);
               break;
            case JOGGERS:
               var4 = pick(r, JOGGERS);
               break;
            default:
               var4 = pick(r, CHINOS);
         }
      }

      l.legColour = var4;
      l.shoes = (!l.alex || !r.nextBoolean()) && r.nextInt(4) != 0 ? SkinPainter.Shoes.SNEAKERS : SkinPainter.Shoes.BOOTS;
      l.shoeColour = r.nextBoolean()
         ? (l.alex ? 6645093 : 4868682)
         : (l.shoes == SkinPainter.Shoes.BOOTS ? pick(r, new int[]{5913122, 3811866}) : pick(r, new int[]{15263976, 2763310, 9054762}));
      l.belt = pick(r, new int[]{3810838, 1973794, 5913120});
      l.watch = r.nextInt(5) == 0;
      if ((variant & 1048576) != 0) {
         l.outfit = SkinPainter.Outfit.VEST;
         l.top = pick(r, new int[]{2237226, 2899558, 3811874, 1980970, 4857386});
         l.under = 15592936;
         l.tie = pick(r, TIES);
         l.legs = SkinPainter.Legs.CHINOS;
         l.legColour = 1973794;
         l.shoes = SkinPainter.Shoes.DRESS;
         l.shoeColour = 1973794;
         l.belt = 1973794;
         l.watch = false;
      }

      if ((variant & 2097152) != 0) {
         l.robber = true;
         l.outfit = SkinPainter.Outfit.HOODIE;
         l.top = 1710622;
         l.accent = 2763312;
         l.legs = SkinPainter.Legs.JEANS;
         l.legColour = 1842208;
         l.shoes = SkinPainter.Shoes.BOOTS;
         l.shoeColour = 1381656;
         l.belt = 1381656;
         l.watch = false;
      }

      return l;
   }

   public static String describe(int variant) {
      SkinPainter.Look l = roll(variant);

      String what = switch (l.outfit) {
         case HOODIE -> "hoodie";
         case JACKET -> "jacket";
         case FLANNEL -> "flannel";
         case SWEATER -> "sweater";
         case VEST -> "waistcoat";
         case TUNIC -> "tunic";
         case POLO -> "polo";
         case OVERALLS -> "overalls";
         default -> l.tee == SkinPainter.Tee.STRIPES ? "striped shirt" : "shirt";
      };
      return l.outfit == SkinPainter.Outfit.OVERALLS ? "the one in overalls" : "the one in the " + colourName(l.top) + " " + what;
   }

   private static String colourName(int c) {
      Object[][] names = new Object[][]{
         {15263970, "white"},
         {2763310, "black"},
         {9079440, "grey"},
         {2899558, "navy"},
         {4086330, "green"},
         {8002608, "maroon"},
         {13148224, "yellow"},
         {6982320, "blue"},
         {5925434, "olive"},
         {6965810, "brown"},
         {44975, "teal"},
         {9223818, "mint"},
         {10104880, "red"},
         {11020330, "red"},
         {13153418, "tan"},
         {5913122, "brown"},
         {4876954, "denim"},
         {2902650, "blue"},
         {2237226, "black"},
         {4868690, "grey"},
         {15262416, "cream"},
         {5925498, "grey"},
         {3811874, "brown"},
         {5909034, "maroon"},
         {9054762, "red"},
         {8006186, "red"},
         {3816010, "charcoal"}
      };
      String best = "dark";
      double bestD = Double.MAX_VALUE;

      for (Object[] n : names) {
         double d = distance(c, (Integer)n[0]);
         if (d < bestD) {
            bestD = d;
            best = (String)n[1];
         }
      }

      return best;
   }

   static SkinPainter.Spot[] map(boolean slim) {
      SkinPainter.Spot[] spots = new SkinPainter.Spot[4096];
      int arm = slim ? 3 : 4;
      box(spots, SkinPainter.Limb.HEAD, 0, 0, 8, 8, 8, false);
      box(spots, SkinPainter.Limb.HEAD, 32, 0, 8, 8, 8, true);
      box(spots, SkinPainter.Limb.BODY, 16, 16, 8, 12, 4, false);
      box(spots, SkinPainter.Limb.BODY, 16, 32, 8, 12, 4, true);
      box(spots, SkinPainter.Limb.RARM, 40, 16, arm, 12, 4, false);
      box(spots, SkinPainter.Limb.RARM, 40, 32, arm, 12, 4, true);
      box(spots, SkinPainter.Limb.LARM, 32, 48, arm, 12, 4, false);
      box(spots, SkinPainter.Limb.LARM, 48, 48, arm, 12, 4, true);
      box(spots, SkinPainter.Limb.RLEG, 0, 16, 4, 12, 4, false);
      box(spots, SkinPainter.Limb.RLEG, 0, 32, 4, 12, 4, true);
      box(spots, SkinPainter.Limb.LLEG, 16, 48, 4, 12, 4, false);
      box(spots, SkinPainter.Limb.LLEG, 0, 48, 4, 12, 4, true);
      return spots;
   }

   private static void box(SkinPainter.Spot[] spots, SkinPainter.Limb limb, int u, int v, int w, int h, int d, boolean layer) {
      rect(spots, limb, 0, u + d, v, w, d, layer);
      rect(spots, limb, 1, u + d + w, v, w, d, layer);
      rect(spots, limb, 2, u, v + d, d, h, layer);
      rect(spots, limb, 3, u + d, v + d, w, h, layer);
      rect(spots, limb, 4, u + d + w, v + d, d, h, layer);
      rect(spots, limb, 5, u + d + w + d, v + d, w, h, layer);
   }

   private static void rect(SkinPainter.Spot[] spots, SkinPainter.Limb limb, int face, int x0, int y0, int w, int h, boolean layer) {
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            spots[(y0 + y) * 64 + x0 + x] = new SkinPainter.Spot(limb, face, x, y, w, h, layer);
         }
      }
   }

   public static void paint(int[] px, SkinPainter.Look l) {
      SkinPainter.Base base = l.alex ? ALEX : STEVE;
      SkinPainter.Spot[] spots = map(l.alex);
      int[] original = (int[])px.clone();

      for (int i = 0; i < px.length; i++) {
         SkinPainter.Spot s = spots[i];
         int argb = original[i];
         boolean empty = argb >>> 24 == 0;
         int rgb = argb & 16777215;
         SkinPainter.Part part = empty ? null : base.parts().get(rgb);
         float shade = part == null ? 1.0F + (hash(l.seed, i, 3) - 0.5F) * 0.12F : lum(rgb) / lum(base.reference().get(part));
         shade = Math.max(0.6F, Math.min(1.4F, shade));
         if (s != null && s.limb() == SkinPainter.Limb.HEAD && l.robber) {
            int f = s.face();
            int y = s.y();
            if (s.layer()) {
               px[i] = f != 0 && (f == 1 || y > 1) ? 0 : -15592940;
            } else if (f != 3 || y != 3 && y != 4) {
               px[i] = 0xFF000000 | mul(1973794, shade);
            }
         } else if (s != null && s.limb() != SkinPainter.Limb.HEAD) {
            Integer c = clothes(l, s, part, i);
            if (c == null) {
               if (part == null || part == SkinPainter.Part.SKIN || part == SkinPainter.Part.HAIR) {
                  continue;
               }

               c = recolour(l, part);
            }

            if (c == -2) {
               px[i] = 0;
            } else {
               px[i] = 0xFF000000 | mul(c, shade);
            }
         }
      }
   }

   private static int recolour(SkinPainter.Look l, SkinPainter.Part part) {
      return switch (part) {
         case HAIR -> l.hair;
         case BEARD -> {
            switch (l.facial) {
               case CLEAN:
               case MUSTACHE:
                  yield mul(l.skin, 0.9F);
               case STUBBLE:
                  yield mix(mul(l.skin, 0.9F), l.hair, 0.35F);
               default:
                  yield mul(l.hair, 0.85F);
            }
         }
         case SKIN -> l.skin;
         case LIPS -> mix(mul(l.skin, 0.8F), 11552840, 0.3F);
         case IRIS -> l.eyes;
         case SHIRT -> l.top;
         case TRIM -> l.accent;
         case BELT -> l.belt;
         case TROUSERS -> l.legColour;
         case SHOES -> l.shoeColour;
      };
   }

   private static Integer head(SkinPainter.Look l, SkinPainter.Spot s, SkinPainter.Part part, int i) {
      int f = s.face();
      int x = s.x();
      int y = s.y();
      int hair = l.hair;
      int skin = l.skin;
      boolean curls = hash(l.seed, i, 9) < 0.5F;
      if (!s.layer()) {
         if (part == SkinPainter.Part.HAIR) {
            switch (l.hairStyle) {
               case BUZZ:
                  return f == 0 ? mix(skin, hair, 0.7F) : mix(skin, hair, 0.55F);
               case BALD:
                  if (f == 0 || f == 3 || y <= 2) {
                     return mul(skin, 1.02F);
                  }
                  break;
               case MOHAWK:
                  if ((f != 0 || x != 3 && x != 4) && (f != 5 || x != 3 && x != 4) && (f != 3 || x != 3 && x != 4)) {
                     return mix(skin, hair, 0.2F);
                  }
            }
         }

         if (!l.alex && l.facial == SkinPainter.Facial.MUSTACHE && f == 3 && y == 6 && x >= 2 && x <= 5) {
            return mul(hair, 0.85F);
         } else {
            return !l.alex && l.facial == SkinPainter.Facial.BIG_BEARD && f == 1 ? mul(hair, 0.85F) : null;
         }
      } else {
         if (l.glasses || l.shades) {
            int frame = l.shades ? 1381658 : ((l.seed & 1) == 0 ? 1973794 : 6965802);
            if (f == 3 && y == 4) {
               if (x == 0 || x == 3 || x == 4 || x == 7) {
                  return frame;
               }

               if (l.shades) {
                  return x != 1 && x != 5 ? 1381658 : 3816008;
               }
            }

            if (f == 3 && y == 3 && !l.shades && x != 0 && x != 7 && x != 3 && x != 4) {
               return frame;
            }

            if ((f == 2 || f == 4) && y == 4 && s.fromFront() <= 2) {
               return frame;
            }
         }

         if (l.hat > 0) {
            int hat = l.hatColour;
            int dark = mul(hat, 0.78F);
            if (f == 0) {
               return hat;
            }

            if (l.hat == 1) {
               if (f == 3 && y == 0 && (x == 3 || x == 4)) {
                  return l.accent;
               }

               if (f != 1 && y <= 1) {
                  return y == 1 && f == 3 ? dark : hat;
               }
            } else if (f != 1 && y <= 2) {
               return y == 2 ? dark : hat;
            }
         }

         if (l.headphones) {
            if (f == 0 && (y == 3 || y == 4)) {
               return 2763310;
            }

            if ((f == 2 || f == 4) && y >= 3 && y <= 5 && s.fromFront() >= 2 && s.fromFront() <= 4) {
               return y == 4 && s.fromFront() == 3 ? 11020330 : 2763310;
            }
         }

         if (l.earring && f == 4 && y == 6 && s.fromFront() == 3) {
            return 14925923;
         } else {
            switch (l.hairStyle) {
               case MOHAWK:
                  if (f == 0 && (x == 3 || x == 4) || f == 3 && y == 0 && (x == 3 || x == 4) || f == 5 && y <= 2 && (x == 3 || x == 4)) {
                     return mul(hair, 1.05F);
                  }
                  break;
               case VOLUME:
                  if (f == 0 && curls || f == 3 && y == 0 && x != 3 || (f == 2 || f == 4 || f == 5) && y <= 1) {
                     return mul(hair, 1.05F);
                  }
                  break;
               case LONG:
                  if (f == 5 || (f == 2 || f == 4) && s.fromFront() >= 1 || f == 0 && y <= 2) {
                     return mul(hair, 1.05F);
                  }
                  break;
               case AFRO:
                  if (f == 0 || f == 5 && y <= 6 || (f == 2 || f == 4) && y <= 5 || f == 3 && y <= 1) {
                     return curls ? mul(hair, 1.1F) : mul(hair, 0.92F);
                  }
                  break;
               case CURLY:
                  if ((f == 0 || (f == 2 || f == 4 || f == 5) && y <= 4 || f == 3 && y == 0) && curls) {
                     return mul(hair, 1.1F);
                  }
                  break;
               case BUN:
                  if (f == 0 && y <= 2 && (x == 3 || x == 4) || f == 5 && y <= 1 && x >= 2 && x <= 5) {
                     return mul(hair, 1.05F);
                  }
            }

            if (!l.alex && l.facial == SkinPainter.Facial.BIG_BEARD) {
               if (f == 3 && y >= 6 && (y != 6 || x != 3 && x != 4)) {
                  return mul(hair, 0.85F);
               }

               if ((f == 2 || f == 4) && y >= 5 && s.fromFront() <= 2) {
                  return mul(hair, 0.85F);
               }
            }

            return null;
         }
      }
   }

   private static Integer clothes(SkinPainter.Look l, SkinPainter.Spot s, SkinPainter.Part part, int i) {
      SkinPainter.Limb limb = s.limb();
      if (part == SkinPainter.Part.HAIR) {
         return null;
      } else if (limb != SkinPainter.Limb.RLEG && limb != SkinPainter.Limb.LLEG) {
         boolean arm = limb == SkinPainter.Limb.RARM || limb == SkinPainter.Limb.LARM;
         int f = s.face();
         int x = s.x();
         int y = s.y();
         boolean cloth = part == SkinPainter.Part.SHIRT || part == SkinPainter.Part.TRIM;
         int sleeveEnd = l.alex ? 3 : 3;
         int top = l.top;
         int dark = mul(top, 0.8F);
         if (s.layer()) {
            return layer(l, s, part, i);
         } else if (arm) {
            boolean hand = f == 1 || y >= 11;
            Integer longSleeve = null;
            switch (l.outfit) {
               case HOODIE:
               case SWEATER:
                  longSleeve = y == 10 ? dark : top;
                  break;
               case JACKET:
                  longSleeve = l.accent;
                  break;
               case FLANNEL:
                  if (y <= 5) {
                     return y == 5 ? mul(top, 1.15F) : plaid(l, s);
                  }
                  break;
               case VEST:
                  longSleeve = y == 10 ? mul(l.under, 0.9F) : l.under;
               case TUNIC:
               case OVERALLS:
               default:
                  break;
               case POLO:
               case TEE:
                  if (cloth && (l.outfit == SkinPainter.Outfit.POLO || l.tee == SkinPainter.Tee.RINGER) && y == sleeveEnd) {
                     return l.accent;
                  }
                  break;
               case SUIT:
                  longSleeve = y == 10 ? l.under : top;
                  break;
               case TANK:
                  if (cloth || f == 0) {
                     return l.skin;
                  }
            }

            if (longSleeve != null && !hand && (cloth || part == SkinPainter.Part.SKIN || f == 0)) {
               return longSleeve;
            } else if (l.watch && limb == SkinPainter.Limb.LARM && y == 9 && part == SkinPainter.Part.SKIN) {
               return f == 3 && x == 1 ? 13158600 : 2763310;
            } else if (!cloth) {
               return null;
            } else {
               return switch (l.outfit) {
                  case TUNIC -> part == SkinPainter.Part.TRIM ? l.accent : top;
                  default -> top;
                  case OVERALLS -> l.accent;
                  case TEE -> tee(l, s);
                  case DRESS -> part == SkinPainter.Part.TRIM ? mul(top, 0.8F) : top;
               };
            }
         } else {
            boolean neck = part == SkinPainter.Part.SKIN;
            if (!cloth && !neck) {
               return null;
            } else {
               switch (l.outfit) {
                  case HOODIE:
                     if (f == 3 && (x == 3 || x == 4) && y >= 1 && y <= 3) {
                        return 14736592;
                     } else if (f != 3 || y != 0 || x != 3 && x != 4) {
                        return f == 3 && y >= 6 && y <= 8 && x >= 1 && x <= 6 ? y == 6 ? mul(top, 0.7F) : mul(top, 0.88F) : top;
                     } else {
                        return null;
                     }
                  case JACKET:
                     return neck ? null : f == 3 && x >= 2 && x <= 5 ? l.accent : mul(top, 0.9F);
                  case FLANNEL:
                     if (f != 3 || y > 1 || x != 3 && x != 4) {
                        if (f != 3 || y != 0 || x != 2 && x != 5) {
                           return plaid(l, s);
                        }

                        return dark;
                     }

                     return neck ? null : l.under;
                  case SWEATER:
                     if (f != 3 || y != 0 || x != 3 && x != 4) {
                        if (y == 0 && f != 0) {
                           return dark;
                        }

                        if ((f == 3 || f == 5) && y >= 3 && y <= 5) {
                           return y == 4 ? ((x + y) % 2 == 0 ? l.accent : top) : mul(l.accent, 0.9F);
                        }

                        if (y != 9 && f != 1) {
                           return top;
                        }

                        return dark;
                     }

                     return null;
                  case VEST:
                  case SUIT:
                     if ((f != 3 || x < 2 || x > 5 || y > 5) && (f != 3 || x != 3 && x != 4 || y > 8)) {
                        if (f == 3 && (x == 2 || x == 5) && y <= 6) {
                           return mul(top, 0.75F);
                        } else {
                           if (f != 3 || x != 2 || y != 7 && y != 9) {
                              return top;
                           }

                           return 1381658;
                        }
                     } else {
                        if ((x == 3 || x == 4) && y >= 1) {
                           return y == 1 ? mul(l.tie, 0.8F) : l.tie;
                        }

                        return l.under;
                     }
                  case TUNIC:
                     return neck ? null : part == SkinPainter.Part.TRIM ? l.accent : top;
                  case POLO:
                     if (f != 3 || y > 2 || x != 3 && x != 4) {
                        if ((f != 3 || y != 0 || x != 2 && x != 5) && (f != 0 || y != 3)) {
                           return neck ? null : top;
                        }

                        return mul(top, 1.15F);
                     }

                     return y == 0 ? null : y == 2 && x == 3 ? 15263970 : dark;
                  case OVERALLS:
                     boolean bib = f == 3 && y >= 3 && x >= 1 && x <= 6;
                     boolean straps = (x == 1 || x == 6) && (f == 3 || f == 5) && y <= 3 || f == 5 && y >= 4;
                     if (bib || straps || (f == 2 || f == 4) && y >= 6) {
                        if (f != 3 || y != 3 || x != 1 && x != 6) {
                           if (f == 3 && y >= 5 && y <= 6 && x >= 3 && x <= 4) {
                              return mul(l.legColour, 0.8F);
                           }

                           return l.legColour;
                        }

                        return 14925923;
                     }

                     return neck ? null : l.accent;
                  case TANK:
                     if ((f != 3 || (y != 0 || x > 1 && x < 6) && (y != 1 || x < 2 || x > 5)) && (f != 5 || y != 0 || x > 1 && x < 6) && f != 0) {
                        return neck ? null : top;
                     }

                     return l.skin;
                  case TEE:
                     return neck ? null : tee(l, s);
                  case DRESS:
                     return neck ? null : part == SkinPainter.Part.TRIM ? mul(top, 0.8F) : top;
                  default:
                     return null;
               }
            }
         }
      } else {
         return legs(l, s, part, i);
      }
   }

   private static int tee(SkinPainter.Look l, SkinPainter.Spot s) {
      int x = s.x();
      int y = s.y();

      return switch (l.tee) {
         case STRIPES -> y / 2 % 2 == 1 ? l.accent : l.top;
         case RINGER -> s.limb() == SkinPainter.Limb.BODY && s.face() == 3 && y == 0 ? l.accent : l.top;
         case RAGLAN -> s.limb() == SkinPainter.Limb.BODY ? l.top : l.accent;
         case POCKET -> s.limb() == SkinPainter.Limb.BODY && s.face() == 3 && x >= 5 && x <= 6 && y >= 3 && y <= 4
         ? (y == 3 ? mul(l.top, 0.78F) : mul(l.top, 0.88F))
         : l.top;
         case PRINT -> s.limb() == SkinPainter.Limb.BODY
            && s.face() == 3
            && y >= 3
            && y <= 6
            && x >= 2
            && x <= 5
            && hash(l.seed, x <= 3 ? x : 7 - x, y) < 0.55F
         ? l.accent
         : l.top;
         default -> l.top;
      };
   }

   private static int plaid(SkinPainter.Look l, SkinPainter.Spot s) {
      int x = s.x() + s.face() * 3;
      int y = s.y();
      boolean vx = x % 4 == 0;
      boolean vy = y % 4 == 0;
      return vx && vy ? 1973794 : (!vx && !vy ? l.top : mul(l.top, 0.62F));
   }

   private static Integer layer(SkinPainter.Look l, SkinPainter.Spot s, SkinPainter.Part part, int i) {
      int f = s.face();
      int x = s.x();
      int y = s.y();
      boolean arm = s.limb() == SkinPainter.Limb.RARM || s.limb() == SkinPainter.Limb.LARM;
      int top = l.top;
      int dark = mul(top, 0.8F);
      switch (l.outfit) {
         case HOODIE:
            if (arm || (f != 5 || y > 2) && (f != 0 || y > 1)) {
               return part == SkinPainter.Part.HAIR ? null : -2;
            }

            return mul(top, 0.9F);
         case JACKET:
            if (f == 1) {
               return -2;
            } else if (arm) {
               return y <= 10 ? (y == 10 ? dark : top) : -2;
            } else if (f == 3 && x >= 2 && x <= 5 && y >= 1) {
               return -2;
            } else {
               if ((f != 3 || x != 2 && x != 5 || y != 0) && (f != 0 || y < 2)) {
                  return y == 11 ? dark : top;
               }

               return dark;
            }
         case FLANNEL:
         case SWEATER:
         default:
            if (!l.necklace || arm || f != 3 || (y != 1 || x != 2 && x != 5) && (y != 2 || x != 3 && x != 4)) {
               if (part == SkinPainter.Part.HAIR) {
                  return null;
               } else if (part == null) {
                  return null;
               } else {
                  return arm && l.outfit == SkinPainter.Outfit.TANK ? -2 : null;
               }
            } else {
               return 14925923;
            }
         case VEST:
            if (arm || f == 1 || f == 0) {
               return -2;
            } else {
               return f == 3 && x >= 2 && x <= 5 ? -2 : y >= 10 ? -2 : top;
            }
      }
   }

   private static Integer legs(SkinPainter.Look l, SkinPainter.Spot s, SkinPainter.Part part, int i) {
      int f = s.face();
      int x = s.x();
      int y = s.y();
      int c = l.legColour;
      if (s.layer()) {
         if (l.legs == SkinPainter.Legs.SKIRT && f != 0 && f != 1 && y <= 6) {
            return y == 6 ? mul(c, 0.85F) : c;
         } else {
            return l.legs != SkinPainter.Legs.SKIRT && part != null ? -2 : null;
         }
      } else {
         int shoeFrom = l.alex && l.shoes == SkinPainter.Shoes.BOOTS ? 6 : 10;
         if (f == 1) {
            return mul(l.shoeColour, 0.7F);
         } else if (f != 0 && y >= shoeFrom) {
            if (l.shoes == SkinPainter.Shoes.SNEAKERS && y == 11) {
               return l.shoeColour == 15263976 ? 12105912 : 15263976;
            } else {
               return l.shoes != SkinPainter.Shoes.SNEAKERS || f != 3 || y != 10 || x != 1 && x != 2 ? l.shoeColour : mul(l.shoeColour, 1.15F);
            }
         } else if (part != SkinPainter.Part.SHOES && part != SkinPainter.Part.TROUSERS && part != SkinPainter.Part.SHIRT && part != SkinPainter.Part.BELT) {
            return null;
         } else {
            switch (l.legs) {
               case JEANS:
                  if (y == 9) {
                     return mul(c, 1.2F);
                  }

                  if ((f == 2 || f == 4) && x == 1) {
                     return mul(c, 1.12F);
                  }
                  break;
               case JOGGERS:
                  if (y == 9) {
                     return mul(c, 0.8F);
                  }

                  if ((f == 2 && s.limb() == SkinPainter.Limb.RLEG || f == 4 && s.limb() == SkinPainter.Limb.LLEG) && x == 1) {
                     return 14737626;
                  }
                  break;
               case SHORTS:
                  if (y >= 5 && f != 0) {
                     return y == 9 && l.seed % 2 == 0 ? 15263970 : l.skin;
                  }
                  break;
               case SKIRT:
                  if (y >= 6 && f != 0) {
                     return (l.seed & 2) == 0 ? mul(2763312, 1.0F) : l.skin;
                  }
            }

            return c;
         }
      }
   }

   static float hash(int seed, int x, int y) {
      int n = x * 374761393 + y * 668265263 + seed * 1442695041;
      n = (n ^ n >>> 13) * 1274126177;
      n ^= n >>> 16;
      return (float)(n & 65535) / 65535.0F;
   }

   static double distance(int a, int b) {
      int dr = (a >> 16 & 0xFF) - (b >> 16 & 0xFF);
      int dg = (a >> 8 & 0xFF) - (b >> 8 & 0xFF);
      int db = (a & 0xFF) - (b & 0xFF);
      return Math.sqrt((double)(dr * dr + dg * dg + db * db));
   }

   static float lum(int c) {
      return 0.3F * (float)(c >> 16 & 0xFF) + 0.59F * (float)(c >> 8 & 0xFF) + 0.11F * (float)(c & 0xFF) + 1.0F;
   }

   static int mul(int c, float f) {
      return clamp((float)(c >> 16 & 0xFF) * f) << 16 | clamp((float)(c >> 8 & 0xFF) * f) << 8 | clamp((float)(c & 0xFF) * f);
   }

   static int mix(int a, int b, float t) {
      return clamp((float)(a >> 16 & 0xFF) * (1.0F - t) + (float)(b >> 16 & 0xFF) * t) << 16
         | clamp((float)(a >> 8 & 0xFF) * (1.0F - t) + (float)(b >> 8 & 0xFF) * t) << 8
         | clamp((float)(a & 0xFF) * (1.0F - t) + (float)(b & 0xFF) * t);
   }

   static int clamp(float v) {
      return Math.max(0, Math.min(255, Math.round(v)));
   }

   private SkinPainter() {
   }

   static record Base(Map<Integer, SkinPainter.Part> parts, Map<SkinPainter.Part, Integer> reference) {
   }

   static enum Facial {
      BEARD,
      CLEAN,
      MUSTACHE,
      BIG_BEARD,
      STUBBLE;
   }

   static enum Hair {
      CLASSIC,
      BUZZ,
      BALD,
      VOLUME,
      LONG,
      MOHAWK,
      AFRO,
      BUN,
      CURLY;
   }

   static enum Legs {
      JEANS,
      CHINOS,
      JOGGERS,
      SHORTS,
      SKIRT;
   }

   static enum Limb {
      HEAD,
      BODY,
      RARM,
      LARM,
      RLEG,
      LLEG;
   }

   public static final class Look {
      public boolean alex;
      int seed;
      int skin;
      int hair;
      int eyes;
      SkinPainter.Outfit outfit;
      SkinPainter.Legs legs;
      SkinPainter.Shoes shoes;
      SkinPainter.Hair hairStyle;
      SkinPainter.Facial facial;
      SkinPainter.Tee tee;
      int top;
      int accent;
      int under;
      int tie;
      int legColour;
      int shoeColour;
      int belt;
      boolean glasses;
      boolean shades;
      boolean necklace;
      boolean watch;
      boolean earring;
      boolean headphones;
      boolean robber;
      int hat;
      int hatColour;
   }

   static enum Outfit {
      TEE,
      HOODIE,
      JACKET,
      FLANNEL,
      SUIT,
      OVERALLS,
      SWEATER,
      POLO,
      TANK,
      VEST,
      TUNIC,
      DRESS;
   }

   static enum Part {
      HAIR,
      BEARD,
      SKIN,
      LIPS,
      IRIS,
      SHIRT,
      TRIM,
      BELT,
      TROUSERS,
      SHOES;
   }

   static enum Shoes {
      SNEAKERS,
      BOOTS,
      DRESS;
   }

   static record Spot(SkinPainter.Limb limb, int face, int x, int y, int w, int h, boolean layer) {
      boolean side() {
         return this.face == 2 || this.face == 4 || this.face == 3 || this.face == 5;
      }

      int fromFront() {
         return this.face == 2 ? this.w - 1 - this.x : this.x;
      }
   }

   static enum Tee {
      PLAIN,
      PRINT,
      STRIPES,
      RINGER,
      POCKET,
      RAGLAN;
   }
}
