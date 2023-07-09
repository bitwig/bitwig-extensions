package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.commons.Colors;

import java.awt.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class RgbColor extends InternalHardwareLightState {

   private static final Map<Integer, RgbColor> cache = new HashMap<>();
   private static final Map<Integer, Integer> colorTable = new Hashtable<>();
   private static final Map<Hsb, Integer> hsbTable = new Hashtable<>();

   public static RgbColor of(final int colorCode) {
      return cache.computeIfAbsent(colorCode, code -> new RgbColor(code));
   }

   public static RgbColor of(final Colors color, ColorBrightness brightness) {
      return of(color.getIndexValue(brightness));
   }

   public static RgbColor of(final Colors color) {
      return of(color.getIndexValue(ColorBrightness.DARKENED));
   }

   public static final RgbColor OFF = RgbColor.of(0);
   public static final RgbColor GREEN = RgbColor.of(Colors.GREEN);
   public static final RgbColor RED = RgbColor.of(Colors.RED);
   public static final RgbColor WHITE = RgbColor.of(Colors.WHITE);
   public static final RgbColor ORANGE = RgbColor.of(Colors.ORANGE);
   public static final RgbColor BLUE = RgbColor.of(Colors.BLUE);
   public static final RgbColor PURPLE = RgbColor.of(Colors.PURPLE);

   private final int colorIndex;

   private RgbColor(int colorIndex) {
      this.colorIndex = colorIndex;
   }

   public RgbColor brightness(ColorBrightness brightness) {
      return of(this.getColorIndex() + brightness.getAdjust());
   }

   public RgbColor ofIndex(int index) {
      return of(index);
   }


   public int getColorIndex() {
      return colorIndex;
   }

   @Override
   public HardwareLightVisualState getVisualState() {
      return null;
   }

   @Override
   public boolean equals(final Object obj) {
      return obj instanceof RgbColor && equals((RgbColor) obj);
   }

   public boolean equals(final RgbColor obj) {
      if (obj == this) {
         return true;
      }
      return colorIndex == obj.colorIndex;
   }

   public static RgbColor toColor(final double red, final double green, final double blue) {
      return RgbColor.of(convertColor(red, green, blue));
   }

   public static int convertColor(final double red, final double green, final double blue) {
      final int rv = (int) Math.floor(red * 255);
      final int gv = (int) Math.floor(green * 255);
      final int bv = (int) Math.floor(blue * 255);
      if (rv == 0 && gv == 0 && bv == 0) {
         return 0;
      }
      final int lookupIndex = rv << 16 | gv << 8 | bv;
      Integer color = colorTable.get(lookupIndex);
      if (color != null) {
         return color.intValue();
      }
      return colorTable.computeIfAbsent(lookupIndex, key -> calcColor(rv, gv, bv));
   }

   private static int calcColor(int rv, int gv, int bv) {
      final Hsb hsb = rgbToHsb(rv, gv, bv);

      Integer fixed = hsbTable.get(hsb);
      if (fixed != null) {
         //DebugOutMs.println(" FIXED %s", fixed);
         return fixed;
      }

      if (hsb.bright < 2 || hsb.sat < 5) {
         return 68;
      }
      int off = 0;
      int bright = 0;
      if (hsb.hue >= 2 && hsb.hue <= 6) {
         off = 1;
      }
      if (hsb.bright < 13 || hsb.sat < 13) {
         off++;
      }
      int color_index = Math.min(hsb.hue + off, 16);
      int color = (color_index << 2) + bright;
      return color;
   }

   private static Hsb rgbToHsb(final int rv, final int gv, final int bv) {
      float[] hsbValues = new float[3];
      Color.RGBtoHSB(rv, gv, bv, hsbValues);
      int hr = Math.round(hsbValues[0] * 15) + 1;
      int hg = Math.round(hsbValues[1] * 15);
      int hb = Math.round(hsbValues[2] * 15);
      return new Hsb(hr, hg, hb);
   }

   public record Hsb(int hue, int sat, int bright) {
      @Override
      public String toString() {
         return String.format("Hsb(%d, %d, %d)", hue, sat, bright);
      }
   }
}
