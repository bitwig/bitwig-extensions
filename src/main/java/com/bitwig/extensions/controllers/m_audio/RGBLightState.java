package com.bitwig.extensions.controllers.m_audio;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiOut;

public class RGBLightState extends InternalHardwareLightState {
   public final static int BLINK = 64;
   public static final RGBLightState OFF = new RGBLightState(State.OFF);
   //public static final RGBLightState ON = new RGBLightState(State.ON);
   public static final RGBLightState WHITE = new RGBLightState(State.WHITE);
   public static final RGBLightState CHARTREUSE = new RGBLightState(State.CHARTREUSE);
   public static final RGBLightState GREEN = new RGBLightState(State.GREEN);
   public static final RGBLightState AQUA = new RGBLightState(State.AQUA);
   public static final RGBLightState CYAN = new RGBLightState(State.CYAN);
   public static final RGBLightState AZURE = new RGBLightState(State.AZURE);
   public static final RGBLightState BLUE = new RGBLightState(State.BLUE);
   public static final RGBLightState VIOLET = new RGBLightState(State.VIOLET);
   public static final RGBLightState MAGENTA = new RGBLightState(State.MAGENTA);
   public static final RGBLightState ROSE = new RGBLightState(State.ROSE);
   public static final RGBLightState RED = new RGBLightState(State.RED);
   public static final RGBLightState ORANGE = new RGBLightState(State.ORANGE);
   public static final RGBLightState YELLOW = new RGBLightState(State.YELLOW);
   public static final RGBLightState GREEN_BLINK = new RGBLightState(State.GREEN_BLINK);
   public static final RGBLightState RED_BLINK = new RGBLightState(State.RED_BLINK);
   public static final RGBLightState YELLOW_BLINK = new RGBLightState(State.YELLOW_BLINK);

   public RGBLightState(final State state) {
      super();
      mState = state;
   }

   public RGBLightState(final Color color) {
      State bestFit = State.OFF;
      float bestDif = 1000;
      for (State state : State.values()) {
         final State s = state;
         if (s != State.OFF) {
            float dif = computeHsvError(s.getColor().getRed255(), s.getColor().getGreen255(), s.getColor().getBlue255(),
                  color);
            if (dif < bestDif) {
               bestDif = dif;
               bestFit = s;
            }
         }
      }
      mState = bestFit;
   }

   @Override
   public HardwareLightVisualState getVisualState() {
      return HardwareLightVisualState.createForColor(mState.getColor());
   }

   public int getMessage() {
      return mState.getMessage();
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      final RGBLightState other = (RGBLightState) obj;
      return mState == other.mState;
   }

   public static void send(final MidiOut midi, final int cc, final int state, final boolean blink) {
      if (blink)
         midi.sendMidi(0x90, cc, state + BLINK);
      else
         midi.sendMidi(0x90, cc, state);
   }

   public static void send(final MidiOut midi, final int cc, final int state, final int channel) {
      if (state != 0)
         midi.sendMidi(0xb0+channel, cc, 127);
      else 
         midi.sendMidi(0xb0+channel, cc, state);
   }

   private enum State {
      OFF(null, 0),
      //ON(null, 127),
      CHARTREUSE(Color.fromRGB255(146, 208, 80), 14),
      GREEN(Color.fromRGB255(1, 176, 80), 12),
      GREEN_BLINK(Color.fromRGB255(1, 176, 80), 12 + BLINK),
      AQUA(Color.fromRGB255(129, 232, 202), 60),
      CYAN(Color.fromRGB255(1, 176, 240), 56),
      AZURE(Color.fromRGB255(82, 142, 213), 44),
      BLUE(Color.fromRGB255(0, 0, 255), 48),
      VIOLET(Color.fromRGB255(102, 0, 203), 50),
      MAGENTA(Color.fromRGB255(244, 0, 245), 51),
      ROSE(Color.fromRGB255(255, 51, 153), 35),
      RED(Color.fromRGB255(255, 0, 0), 3),
      RED_BLINK(Color.fromRGB255(1, 176, 80), 3 + BLINK),
      ORANGE(Color.fromRGB255(250, 188, 2), 11),
      YELLOW(Color.fromRGB255(250, 250, 3), 15),
      YELLOW_BLINK(Color.fromRGB255(250, 250, 3), 15 + BLINK),
      WHITE(Color.fromRGB255(255, 255, 255), 63);

      private State(final Color color, final int msg) {
         mColor = color;
         mMessage = msg;
      }

      public Color getColor() {
         return mColor;
      }

      public int getMessage() {
         return mMessage;
      }

      private final Color mColor;
      private final int mMessage;
   }

   private final State mState;

   public static void RGBtoHSV(final float r, final float g, final float b, final float[] hsv) {
      assert r >= 0 && r <= 1;
      assert g >= 0 && g <= 1;
      assert b >= 0 && b <= 1;
      assert hsv != null;
      assert hsv.length == 3;

      float min, max, delta;
      float h, s, v;

      min = Math.min(Math.min(r, g), b);
      max = Math.max(Math.max(r, g), b);
      v = max; // v

      delta = max - min;

      if (max != 0) {
         s = delta / max; // s
      } else {
         // r = g = b = 0 // s = 0, v is undefined
         s = 0;
         h = 0;
         assert h >= 0 && h <= 360;
         assert s >= 0 && s <= 1;
         assert v >= 0 && v <= 1;

         hsv[0] = h;
         hsv[1] = s;
         hsv[2] = v;
         return;
      }

      if (delta == 0) {
         h = 0;
      } else {
         if (r == max) {
            h = (g - b) / delta; // between yellow & magenta
         } else if (g == max) {
            h = 2 + (b - r) / delta; // between cyan & yellow
         } else {
            h = 4 + (r - g) / delta; // between magenta & cyan
         }
      }

      h *= 60; // degrees
      if (h < 0) {
         h += 360;
      }

      assert h >= 0 && h <= 360;
      assert s >= 0 && s <= 1;
      assert v >= 0 && v <= 1;

      hsv[0] = h;
      hsv[1] = s;
      hsv[2] = v;
   }

   private static float computeHsvError(int r, int g, int b, final Color color) {
      float[] hsv = new float[3];
      RGBtoHSV(r, g, b, hsv);
      float[] hsvRef = new float[3];
      RGBtoHSV(color.getRed255(), color.getGreen255(), color.getBlue255(), hsvRef);

      float hueError = (hsv[0] - hsvRef[0]) / 30;
      float sError = (hsv[1] - hsvRef[1]) * 1.6f;
      final float vScale = 1f;
      float vError = (vScale * hsv[2] - hsvRef[2]) / 40;

      final float error = hueError * hueError + vError * vError + sError * sError;

      return error;
   }
}
