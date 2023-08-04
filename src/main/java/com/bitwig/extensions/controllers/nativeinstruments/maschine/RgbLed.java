package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.commons.Colors;

import java.util.HashMap;
import java.util.Map;

public class RgbLed extends InternalHardwareLightState {

   private static final Map<Integer, RgbLed> cache = new HashMap<>();
   public static final RgbLed TRACK_ON = new RgbLed(Colors.LIGHT_ORANGE, ColorBrightness.DARKENED);
   public static final RgbLed TRACK_OFF = new RgbLed(Colors.LIGHT_ORANGE, ColorBrightness.BRIGHT);
   public static final RgbLed OFF = RgbLed.of(0);
   public static final RgbLed BRIGHT_WHITE = RgbLed.of(70);
   public static final RgbLed SOLO_ON = new RgbLed(Colors.YELLOW, ColorBrightness.BRIGHT);
   public static final RgbLed SOLO_OFF = new RgbLed(Colors.YELLOW, ColorBrightness.DARKENED);
   public static final RgbLed ARM_ON = new RgbLed(Colors.RED, ColorBrightness.BRIGHT);
   public static final RgbLed ARM_OFF = new RgbLed(Colors.RED, ColorBrightness.DARKENED);
   public static final RgbLed GROUP_TRACK_ACTIVE = new RgbLed(Colors.WHITE, ColorBrightness.BRIGHT);
   public static final RgbLed GROUP_TRACK_EXISTS = new RgbLed(Colors.WHITE, ColorBrightness.DARKENED);
   public static final RgbLed CREATE_TRACK = new RgbLed(Colors.WHITE, ColorBrightness.DIMMED);
   public static final RgbLed WHITE_BRIGHT = new RgbLed(Colors.WHITE, ColorBrightness.BRIGHT);

   protected int color = 0;

   protected RgbLed(final int color) {
      this.color = color;
   }

   private RgbLed(Colors color, ColorBrightness brightness) {
      this.color = color.getIndexValue(brightness);
   }

   public static RgbLed of(final int colorCode) {
      return cache.computeIfAbsent(colorCode, code -> new RgbLed(code));
   }

   @Override
   public HardwareLightVisualState getVisualState() {
      return null;
   }

   @Override
   public boolean equals(final Object obj) {
      return obj instanceof RgbLed && equals((RgbLed) obj);
   }

   public boolean equals(final RgbLed obj) {
      if (obj == this) {
         return true;
      }
      return color == obj.color;
   }

   public int getColor() {
      return color;
   }

   public int getOffColor() {
      return color;
   }

   public boolean isBlinking() {
      return false;
   }

}
