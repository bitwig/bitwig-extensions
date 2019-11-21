package com.bitwig.extensions.controllers.akai.apc40_mkii;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.MidiOut;

public class RgbLed
{
   public static final int COLOR_NONE = 0;

   public static final int COLOR_RED = 2;

   public static final int COLOR_GREEN = 18;

   public static final int COLOR_BLUE = 42;

   public static final int COLOR_YELLOW = 10;

   public static final int COLOR_RECORDING = COLOR_RED;

   public static final int COLOR_PLAYING = COLOR_GREEN;

   public static final int COLOR_STOPPING = COLOR_NONE;

   public static final int COLOR_SCENE = COLOR_YELLOW;

   public static final int BLINK_NONE = 0;

   public static final int BLINK_PLAY_QUEUED = 12;

   public static final int BLINK_ACTIVE = 9;

   public static final int BLINK_RECORD_QUEUED = 12;

   public static final int BLINK_STOP_QUEUED = 12;

   private static final Map<Integer, Integer> RGB_TO_COLOR_VALUE_MAP = new HashMap<>();

   private static final Map<Integer, Color> COLOR_VALUE_TO_COLOR_MAP = new HashMap<>();

   private static void registerColor(final Color color, final int value)
   {
      COLOR_VALUE_TO_COLOR_MAP.put(value, color);

      final int rgb = color.getRed255() << 16 | color.getGreen255() << 8 | color.getBlue255();

      RGB_TO_COLOR_VALUE_MAP.put(rgb, value);
   }

   private static void registerColor(final int rgb, final int value)
   {
      COLOR_VALUE_TO_COLOR_MAP.put(value, Color.fromRGB255((rgb & 0xFF0000) >> 16, (rgb & 0xFF00) >> 8, rgb & 0xFF));

      RGB_TO_COLOR_VALUE_MAP.put(rgb, value);
   }

   static
   {
      registerColor(0xFF0000, COLOR_RED);
      registerColor(0xFF00, COLOR_GREEN);
      registerColor(0xFF, COLOR_BLUE);
      registerColor(0xFFD90F, COLOR_YELLOW);


      registerColor(0, 0);

      registerColor(14235761, 57);
      registerColor(14771857, 107);
      registerColor(5526612, 1);

      registerColor(14233124, 6);
      registerColor(15491415, 5);
      registerColor(8026746, 2);

      registerColor(16733958, 9);
      registerColor(16745278, 12);
      registerColor(13224393, 3);

      registerColor(14261520, 14);
      registerColor(14989134, 13);
      registerColor(8817068, 104);

      registerColor(7575572, 18);
      registerColor(10534988, 17);
      registerColor(10713411, 125);

      registerColor(40263, 22);
      registerColor(4111202, 21);
      registerColor(13016944, 124);

      registerColor(42644, 34);
      registerColor(4444857, 33);
      registerColor(5726662, 43);

      registerColor(39385, 38);
      registerColor(4507903, 37);
      registerColor(8686304, 115);

      registerColor(9783755, 50);
      registerColor(12351216, 49);
   }

   public void paint(final MidiOut midiOut, final int msg, final int data1)
   {
      if (mColor != mDisplayedColor || mBlinkColor != mDisplayedBlinkColor
         || mBlinkType != mDisplayedBlinkType)
      {
         midiOut.sendMidi(msg << 4, data1, mColor);

         if (mBlinkType != BLINK_NONE)
         {
            midiOut.sendMidi(msg << 4, data1, mBlinkColor);
            midiOut.sendMidi((msg << 4) | mBlinkType, data1, mColor);
         }
         else
         {
            midiOut.sendMidi(msg << 4, data1, mColor);
         }

         mDisplayedColor = mColor;
         mDisplayedBlinkColor = mBlinkColor;
         mDisplayedBlinkType = mBlinkType;
      }
   }

   public void setColor(final float red, final float green, final float blue)
   {
      final int r8 = (int)(red * 255);
      final int g8 = (int)(green * 255);
      final int b8 = (int)(blue * 255);
      final int total = (r8 << 16) | (g8 << 8) | b8;

      final Integer color = RGB_TO_COLOR_VALUE_MAP.get(total);
      if (color != null)
      {
         mColor = color;
         return;
      }

      mColor = 13;
   }

   public void setColor(final int color)
   {
      mColor = color;
   }

   public void setColor(final ColorValue color)
   {
      setColor(color.red(), color.green(), color.blue());
   }

   public void setBlinkType(final int blinkType)
   {
      mBlinkType = blinkType;
   }

   public void setBlinkColor(final int blinkColor)
   {
      mBlinkColor = blinkColor;
   }

   public static Color stateToColor(final int state)
   {
      return COLOR_VALUE_TO_COLOR_MAP.get(state);
   }

   public static HardwareLightVisualState stateToVisualState(final int state)
   {
      return HardwareLightVisualState.createForColor(stateToColor(state));
   }

   public int getStateAsInt()
   {
      return mColor;
   }

   private int mColor = COLOR_NONE;

   private int mDisplayedColor = -1;

   private int mBlinkColor = COLOR_NONE;

   private int mDisplayedBlinkColor = -1;

   private int mBlinkType = BLINK_NONE;

   private int mDisplayedBlinkType = -1;
}
