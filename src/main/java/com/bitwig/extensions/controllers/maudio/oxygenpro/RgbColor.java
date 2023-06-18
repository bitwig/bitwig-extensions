package com.bitwig.extensions.controllers.maudio.oxygenpro;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

import java.awt.*;

public class RgbColor extends InternalHardwareLightState {
   private static final int BLINK = 64;

   public static final RgbColor OFF = new RgbColor(0);
   public static final RgbColor RED = new RgbColor(3);
   public static final RgbColor ORANGE = new RgbColor(11);
   public static final RgbColor GREEN = new RgbColor(12);
   public static final RgbColor YELLOW = new RgbColor(15);
   public static final RgbColor BLUE = new RgbColor(48);
   public static final RgbColor WHITE = new RgbColor(63);

   public static final RgbColor CHARTREUSE = new RgbColor(14);
   public static final RgbColor AQUA = new RgbColor(60);
   public static final RgbColor CYAN = new RgbColor(56);
   public static final RgbColor AZURE = new RgbColor(44);
   public static final RgbColor VIOLET = new RgbColor(50);
   public static final RgbColor MAGENTA = new RgbColor(51);
   public static final RgbColor ROSE = new RgbColor(35);

   private int stateIndex;
   private RgbColor blink;

   private RgbColor(int stateIndex) {
      this.stateIndex = stateIndex;
      this.blink = new RgbColor(this);
   }

   private RgbColor(RgbColor base) {
      this.stateIndex = base.stateIndex + BLINK;
      this.blink = this;
   }

   public RgbColor getBlink() {
      return blink;
   }

   public int getStateIndex() {
      return stateIndex;
   }

   @Override
   public HardwareLightVisualState getVisualState() {
      return null;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof RgbColor) {
         RgbColor other = (RgbColor) obj;
         return other.stateIndex == stateIndex;
      }
      return false;
   }

   public static RgbColor toColor(double red, double green, double blue) {
      final int rv = (int) Math.floor(red * 255);
      final int gv = (int) Math.floor(green * 255);
      final int bv = (int) Math.floor(blue * 255);
      if (rv == 0 && gv == 0 && bv == 0) {
         return RgbColor.OFF;
      }
      Hsb hsb = rgbToHsb(rv, gv, bv);
      //DebugOutOxy.println("x Color %d %d %d  %s", rv, gv, bv, hsb);

      return toColor(hsb);
   }

   private static RgbColor toColor(Hsb hsb) {
      if (hsb.sat < 4) {
         return RgbColor.WHITE;
      }

      switch (hsb.hue) {
         case 0:
            return OFF;
         case 1, 16:
            return RED;
         case 2:
            return hsb.sat > 13 ? RED : ORANGE;
         case 3:
            return hsb.sat <= 13 ? YELLOW : ORANGE;
         case 4, 5:
            return CHARTREUSE;
         case 6:
            return AQUA;
         case 7, 8:
            return AZURE;
         case 9:
            return CYAN;
         case 10:
            return BLUE;
         case 11:
            return hsb.sat < 11 ? BLUE : VIOLET;
         case 12:
            return VIOLET;
         case 13, 14:
            return ROSE;
         case 15:
            return MAGENTA;
      }

      return WHITE;
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
