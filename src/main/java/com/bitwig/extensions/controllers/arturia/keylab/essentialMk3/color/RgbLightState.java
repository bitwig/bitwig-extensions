package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color;

import java.util.Arrays;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class RgbLightState extends InternalHardwareLightState {
   private static final int DIM_VAL = 0x20;
   private static final int MAX_VAL = 0x7F;

   public static final RgbLightState RED = new RgbLightState(MAX_VAL, 0, 0);
   public static final RgbLightState RED_DIMMED = new RgbLightState(DIM_VAL, 0, 0);
   public static final RgbLightState BLUE = new RgbLightState(0, 0, MAX_VAL);
   public static final RgbLightState BLUE_DIMMED = new RgbLightState(0, 0, DIM_VAL);
   public static final RgbLightState BLUE_DIMMED_FL = new RgbLightState(0, 0, DIM_VAL, BlinkState.PULSING);
   public static final RgbLightState WHITE = new RgbLightState(MAX_VAL, MAX_VAL, MAX_VAL);
   public static final RgbLightState WHITE_DIMMED = new RgbLightState(DIM_VAL, DIM_VAL, DIM_VAL);
   public static final RgbLightState OFF = new RgbLightState(0, 0, 0);
   public static final RgbLightState YELLOW = new RgbLightState(MAX_VAL, MAX_VAL, 0);
   public static final RgbLightState ORANGE = new RgbLightState(0x7F, 0x48, 0);
   public static final RgbLightState ORANGE_DIMMED = new RgbLightState(0x20, 0x18, 0);
   public static final RgbLightState GREEN = new RgbLightState(0, MAX_VAL, 0);
   public static final RgbLightState GREEN_DIMMED = new RgbLightState(0, DIM_VAL, 0);

   public static final int SAT_DIM_FACTOR = 10;
   public static final int MAX_COLOR_VALUE = 127;
   public static final int SAT_AMOUNT = 35;

   private final byte[] rgb;

   private final HardwareLightVisualState visualState;
   private final BlinkState state;

   public static RgbLightState forColor(final Color color)
   {
      if (color == null || color.getAlpha() == 0)
         return OFF;

      final int red = (int)(color.getRed() * MAX_VAL);
      final int green = (int)(color.getGreen() * MAX_VAL);
      final int blue = (int)(color.getBlue() * MAX_VAL);

      return new RgbLightState(red, green, blue);
   }

   RgbLightState(final int red, final int green, final int blue, final BlinkState state) {
      rgb = saturate(red, green, blue, SAT_AMOUNT);
      this.state = state;
      visualState = HardwareLightVisualState.createForColor(Color.fromRGB(rgb[0] << 1, rgb[1] << 1, rgb[2] << 1));
   }

   private RgbLightState(final int red, final int green, final int blue) {
      rgb = new byte[]{(byte) red, (byte) green, (byte) blue};
      state = BlinkState.NONE;
      visualState = HardwareLightVisualState.createForColor(Color.fromRGB(red << 1, green << 1, blue << 1));
   }

   private RgbLightState(final int red, final int green, final int blue, final int saturationAmount) {
      rgb = saturate(red, green, blue, saturationAmount);
      state = BlinkState.NONE;
      visualState = HardwareLightVisualState.createForColor(Color.fromRGB(red << 1, green << 1, blue << 1));
   }

   @Override
   public HardwareLightVisualState getVisualState() {
      return visualState;
   }

   public void apply(final byte[] rgbCommand) {
      rgbCommand[SAT_DIM_FACTOR] = rgb[0];
      rgbCommand[11] = rgb[1];
      rgbCommand[12] = rgb[2];
      rgbCommand[13] = state.getCode();
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      final RgbLightState other = (RgbLightState) obj;
      return Arrays.equals(rgb, other.rgb) && state == other.state;
   }

   @Override
   public String toString() {
      return "RgbLightState{" + "red=" + rgb[0] + ", green=" + rgb[1] + ", blue=" + rgb[2] + '}';
   }

   private static double[] saturate_old(final double red, final double green, final double blue, final double amount) {
      final double max = Math.max(Math.max(red, green), blue);
      final double min = Math.min(Math.min(red, green), blue);
      double rv = red;
      double bv = blue;
      double gv = green;
      if (red == max) {
         rv = Math.min(1.0, red + amount);
      } else if (red == min) {
         rv = Math.max(0.0, red - amount);
      }
      if (green == max) {
         gv = Math.min(1.0, green + amount);
      } else if (green == min) {
         gv = Math.max(0.0, green - amount);
      }
      if (blue == max) {
         bv = Math.min(1.0, blue + amount);
      } else if (blue == min) {
         bv = Math.max(0.0, blue - amount);
      }

      return new double[]{rv, gv, bv};
   }

   private static byte[] saturate(final int red, final int green, final int blue, final int amount) {
      final int max = Math.max(Math.max(red, green), blue);
      final int min = Math.min(Math.min(red, green), blue);
      int rv = red;
      int bv = blue;
      int gv = green;
      if (red == max) {
         rv = Math.min(MAX_COLOR_VALUE, red + amount);
         gv = dim(gv, max);
         bv = dim(bv, max);
      } else if (red == min) {
         rv = Math.max(0, red - amount);
      }
      if (green == max) {
         gv = Math.min(MAX_COLOR_VALUE, green + amount);
         rv = dim(rv, max);
         bv = dim(bv, max);
      } else if (green == min) {
         gv = Math.max(0, green - amount);
      }
      if (blue == max) {
         bv = Math.min(MAX_COLOR_VALUE, blue + amount);
      } else if (blue == min) {
         bv = Math.max(0, blue - amount);
         rv = dim(rv, max);
         gv = dim(gv, max);
      }

      return new byte[]{(byte) (rv >> 1), (byte) (gv >> 1), (byte) (bv >> 1)};
   }

   private static int dim(final int val, final int max) {
      if (val < max) {
         return Math.max(0, val - SAT_DIM_FACTOR);
      }
      return val;
   }
}
