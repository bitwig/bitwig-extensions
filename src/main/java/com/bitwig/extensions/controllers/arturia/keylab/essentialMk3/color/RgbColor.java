package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color;

import java.util.HashMap;
import java.util.Map;

public class RgbColor {
   private static final Map<Integer, RgbColor> indexLookup = new HashMap<>();

   public static final RgbColor OFF = getColor(0, 0, 0);
   public static final RgbColor RED = getColor(127, 0, 0);
   public static final RgbColor GREEN = getColor(0, 127, 0);

   private RgbLightState brightColor;
   private RgbLightState dimColor;
   private final Map<BlinkState, RgbLightState> states = new HashMap<>();

   private final RgbLightState basicColor;
   private final int redValue;
   private final int greenValue;
   private final int blueValue;

   public static RgbColor getColor(final double red, final double green, final double blue) {
      final int rv = (int) Math.floor(red * 127);
      final int gv = (int) Math.floor(green * 127);
      final int bv = (int) Math.floor(blue * 127);
      final int colorLookup = rv << 16 | gv << 8 | bv;
      return indexLookup.computeIfAbsent(colorLookup, index -> new RgbColor(rv, gv, bv));
   }

   public static RgbColor getColor(final int red, final int green, final int blue) {
      final int colorLookup = red << 16 | green << 8 | blue;
      return indexLookup.computeIfAbsent(colorLookup, index -> new RgbColor(red, green, blue));
   }

   public RgbColor(final int red, final int green, final int blue) {
      redValue = red;
      greenValue = green;
      blueValue = blue;
      basicColor = new RgbLightState(red, green, blue, BlinkState.NONE);
   }

   public RgbLightState getColorState() {
      return basicColor;
   }

   public RgbLightState getColorState(final BlinkState state) {
      if (state == BlinkState.NONE) {
         return basicColor;
      }
      return states.computeIfAbsent(state, aState -> new RgbLightState(redValue, greenValue, blueValue, state));
   }

   RgbLightState getDimColor() {
      return basicColor;
   }

   RgbLightState getBrightColor() {
      return basicColor;
   }


}
