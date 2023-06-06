package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import com.bitwig.extension.controller.api.SettableColorValue;

import java.awt.*;
import java.util.Hashtable;
import java.util.Map;

public class NIColorUtil {

   private static final Hsb BLACK_HSB = new Hsb(0, 0, 0);
   private static final Map<Integer, Integer> fixedColorTable = new Hashtable<>();
   private static final Map<Integer, Integer> colorTable = new Hashtable<>();
   private static final Map<Hsb, Integer> hsbTable = new Hashtable<>();

   public static void registerHsb(Hsb hsb, int base, int offset) {
      hsbTable.put(hsb, base * 4 + offset);
   }

   static {
      registerHsb(new Hsb(1, 5, 15), 16, 3);
      registerHsb(new Hsb(3, 8, 15), 3, 1);
      registerHsb(new Hsb(3, 12, 15), 3, 1);
      registerHsb(new Hsb(3, 12, 12), 3, 1);
      registerHsb(new Hsb(5, 8, 11), 6, 1);
      registerHsb(new Hsb(5, 5, 14), 6, 1);
      registerHsb(new Hsb(5, 8, 13), 6, 1);

      registerHsb(new Hsb(8, 10, 14), 8, 1);
      registerHsb(new Hsb(8, 15, 13), 8, 1);
      registerHsb(new Hsb(8, 15, 11), 8, 1);
      registerHsb(new Hsb(8, 10, 12), 8, 1);
      registerHsb(new Hsb(8, 15, 10), 8, 1);

      registerHsb(new Hsb(8, 10, 15), 9, 1);
      registerHsb(new Hsb(8, 15, 15), 9, 1);
      registerHsb(new Hsb(8, 15, 12), 9, 1);

      registerHsb(new Hsb(13, 2, 13), 15, -1);

      fixedColorTable.put(13016944, 16);
      fixedColorTable.put(5526612, 68);
      fixedColorTable.put(8026746, 68);
      fixedColorTable.put(13224393, 68);
      fixedColorTable.put(8817068, 52);
      fixedColorTable.put(10713411, 12);
      fixedColorTable.put(5726662, 48);
      fixedColorTable.put(8686304, 48);
      fixedColorTable.put(9783755, 52);
      fixedColorTable.put(14235761, 60);
      fixedColorTable.put(14233124, 4);
      fixedColorTable.put(16733958, 8);
      fixedColorTable.put(14261520, 16);
      fixedColorTable.put(7575572, 24);
      fixedColorTable.put(40263, 28);
      fixedColorTable.put(42644, 32);
      fixedColorTable.put(39385, 44);
      fixedColorTable.put(12351216, 52);
      fixedColorTable.put(14771857, 64);
      fixedColorTable.put(15491415, 12);
      fixedColorTable.put(16745278, 12);
      fixedColorTable.put(14989134, 16);
      fixedColorTable.put(10534988, 24);
      fixedColorTable.put(4111202, 32);
      fixedColorTable.put(4444857, 36);
      fixedColorTable.put(4507903, 40);
      fixedColorTable.put(8355711, 68);
   }

   // TODO This dude needs to go
   public static int convertColor(final SettableColorValue color) {
      return convertColorX(color.red(), color.green(), color.blue());
   }

   public static int convertColor(final BitWigColor color) {
      final Integer colorIndex = fixedColorTable.get(color.getLookupIndex());
      if (colorIndex != null) {
         return colorIndex.intValue();
      }
      return 68;
   }

   public static boolean isOff(final SettableColorValue color) {
      return color.green() == 0 && color.red() == 0 && color.blue() == 0;
   }

   public static int convertColorX(final float red, final float green, final float blue) {
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

      DebugOutMs.println(" %d %d %d => HSB = %s ", rv, gv, bv, hsb);
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
      DebugOutMs.println(" off = %d ci=%d bright=%d ", off, color_index, bright);
      int color = (color_index << 2) + bright;
      return color;
   }

   public static int convertColor_(final float red, final float green, final float blue) {
      final int rv = (int) Math.floor(red * 255);
      final int gv = (int) Math.floor(green * 255);
      final int bv = (int) Math.floor(blue * 255);
      if (rv == 0 && gv == 0 && bv == 0) {
         return 0;
      }
      final int lookupIndex = rv << 16 | gv << 8 | bv;

      final Hsb hsb = rgbToHsb(rv, gv, bv);
      if (hsb.bright < 1 || hsb.sat < 3) {
         colorTable.put(lookupIndex, 68);
         return colorTable.get(lookupIndex);
      }
      int off = 0;
      if (hsb.bright + hsb.sat < 22) {
         off = 1;
      }
      if (2 <= hsb.hue && hsb.hue <= 6 && hsb.bright < 13) {
         off = 2;
      }
      final int color_index = Math.min(hsb.hue + off + 1, 16);
      final int color = color_index << 2;
      colorTable.put(lookupIndex, color);
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
