package com.bitwig.extensions.controllers.novation.launchkey_mk3;

public class ColorLookup {
   private static final Hsb BLACK_HSB = new Hsb(0, 0, 0);

   public static int toColor(final float r, final float g, final float b) {
      final int rv = (int) Math.floor(r * 255);
      final int gv = (int) Math.floor(g * 255);
      final int bv = (int) Math.floor(b * 255);
      if (rv < 10 && gv < 10 && bv < 10) {
         return 0; // black
      } else if (rv > 230 && gv > 230 && bv > 230) {
         return 3; // white
      } else if (rv == gv && bv == gv) {
         final int bright = rv >> 4;
         if (bright > 7) {
            return 2; // gray
         } else {
            return 1;
         }
      } else {
         final ColorLookup.Hsb hsb = ColorLookup.rgbToHsb(rv, gv, bv);
         int hueInd = hsb.hue > 6 ? hsb.hue - 1 : hsb.hue;
         hueInd = hueInd > 13 ? 13 : hueInd;
         int color = 5 + hueInd * 4 + 1;
         if (hsb.sat < 8) {
            color -= 2;
         } else if (hsb.bright <= 8) {
            color += 2;
         }
         return color;
      }
   }

   public static Hsb rgbToHsb(final float rv, final float gv, final float bv) {
      final float rgb_max = Math.max(Math.max(rv, gv), bv);
      final float rgb_min = Math.min(Math.min(rv, gv), bv);
      final int bright = (int) rgb_max;
      if (bright == 0) {
         return BLACK_HSB; // Black
      }
      final int sat = (int) (255 * (rgb_max - rgb_min) / bright);
      if (sat == 0) {
         return BLACK_HSB; // White
      }
      float hue = 0;
      if (rgb_max == rv) {
         hue = 0 + 43 * (gv - bv) / (rgb_max - rgb_min);
      } else if (rgb_max == gv) {
         hue = 85 + 43 * (bv - rv) / (rgb_max - rgb_min);
      } else {
         hue = 171 + 43 * (rv - gv) / (rgb_max - rgb_min);
      }
      if (hue < 0) {
         hue = 256 + hue;
      }
      return new Hsb((int) Math.floor(hue / 16.0 + 0.3), sat >> 4, bright >> 4);
   }

   public static class Hsb {
      public final int hue;
      public final int sat;
      public final int bright;

      public Hsb(final int hue, final int sat, final int bright) {
         super();
         this.hue = hue;
         this.sat = sat;
         this.bright = bright;
      }

      @Override
      public String toString() {
         final StringBuilder sb = new StringBuilder("Hsb{");
         sb.append("hue=").append(hue);
         sb.append(", sat=").append(sat);
         sb.append(", bright=").append(bright);
         sb.append('}');
         return sb.toString();
      }
   }

}
