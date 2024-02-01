package com.bitwig.extensions.controllers.novation.commonsmk3;

import com.bitwig.extension.api.Color;

public class ColorLookup {
   private static final Hsb BLACK_HSB = new Hsb(0, 0, 0);

   public static int toColor(final Color color) {
      if (color == null || color.getAlpha() == 0) {
         return 0;
      }
      return toColor((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
   }

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
         final Hsb hsb = ColorLookup.rgbToHsb(rv, gv, bv);
         int hueInd = hsb.hue > 6 ? hsb.hue - 1 : hsb.hue;
         hueInd = Math.min(13, hueInd);
         int color = 5 + hueInd * 4 + 1;
         if (hsb.sat < 8) {
            color -= 2;
         } else if (hsb.bright <= 8) {
            color += 2;
         }
         // return color;
         return adjust(color);
      }
   }

   private static int adjust(final int c) {
      final int rst = (c - 2) % 4;
      if (rst == 0) {
         return c - 1;
      }
      return c;
   }

   private static Hsb rgbToHsb(final float rv, final float gv, final float bv) {
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
      float hue;
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
         final String sb = "Hsb{" + "hue=" + hue +
            ", sat=" + sat +
            ", bright=" + bright +
            '}';
         return sb;
      }
   }

   public static Color colorIndexToApiColor(final int colorIndex) {
      if (colorIndex >= 0 && colorIndex < PALETTE.length) {
         return PALETTE[colorIndex];
      }

      return PALETTE[3];
   }

   private static final Color[] PALETTE = {
      rgb(97, 97, 97),
      rgb(179, 179, 179),
      rgb(221, 221, 221),
      rgb(255, 255, 255),
      rgb(250, 179, 178),
      rgb(246, 99, 102),
      rgb(215, 98, 99),
      rgb(188, 89, 101),
      rgb(254, 242, 214),
      rgb(250, 176, 112),
      rgb(225, 135, 99),
      rgb(175, 118, 100),
      rgb(253, 252, 172),
      rgb(253, 253, 104),
      rgb(220, 221, 100),
      rgb(180, 186, 97),
      rgb(219, 255, 155),
      rgb(194, 255, 104),
      rgb(194, 255, 104),
      rgb(135, 180, 94),
      rgb(196, 252, 180),
      rgb(117, 250, 110),
      rgb(99, 228, 86),
      rgb(104, 180, 96),
      rgb(191, 255, 191),
      rgb(109, 254, 142),
      rgb(109, 219, 127),
      rgb(102, 180, 106),
      rgb(186, 255, 196),
      rgb(125, 250, 205),
      rgb(103, 226, 160),
      rgb(117, 174, 132),
      rgb(199, 255, 243),
      rgb(112, 254, 229),
      rgb(110, 222, 191),
      rgb(105, 183, 146),
      rgb(197, 244, 254),
      rgb(117, 238, 252),
      rgb(106, 202, 218),
      rgb(106, 202, 218),
      rgb(196, 221, 254),
      rgb(113, 201, 245),
      rgb(108, 161, 218),
      rgb(103, 130, 178),
      rgb(162, 137, 253),
      rgb(105, 100, 243),
      rgb(102, 97, 223),
      rgb(99, 97, 179),
      rgb(207, 178, 254),
      rgb(165, 94, 253),
      rgb(129, 98, 223),
      rgb(120, 96, 185),
      rgb(245, 185, 252),
      rgb(242, 104, 247),
      rgb(216, 100, 220),
      rgb(172, 100, 180),
      rgb(251, 180, 216),
      rgb(249, 93, 193),
      rgb(218, 101, 165),
      rgb(178, 93, 137),
      rgb(246, 119, 104)
   };

   private static Color rgb(final int r, final int g, final int b) {
      return Color.fromRGB255(r, g, b);
   }
}
