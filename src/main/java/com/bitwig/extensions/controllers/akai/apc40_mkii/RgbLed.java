package com.bitwig.extensions.controllers.akai.apc40_mkii;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.ColorValue;
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

   private static final Map<Integer, Integer> COLORS = new HashMap<>();

   static
   {
      COLORS.put(0, 0);

      COLORS.put(14235761, 57);
      COLORS.put(14771857, 107);
      COLORS.put(5526612, 1);

      COLORS.put(14233124, 6);
      COLORS.put(15491415, 5);
      COLORS.put(8026746, 2);

      COLORS.put(16733958, 9);
      COLORS.put(16745278, 12);
      COLORS.put(13224393, 3);

      COLORS.put(14261520, 14);
      COLORS.put(14989134, 13);
      COLORS.put(8817068, 104);

      COLORS.put(7575572, 18);
      COLORS.put(10534988, 17);
      COLORS.put(10713411, 125);

      COLORS.put(40263, 22);
      COLORS.put(4111202, 21);
      COLORS.put(13016944, 124);

      COLORS.put(42644, 34);
      COLORS.put(4444857, 33);
      COLORS.put(5726662, 43);

      COLORS.put(39385, 38);
      COLORS.put(4507903, 37);
      COLORS.put(8686304, 115);

      COLORS.put(9783755, 50);
      COLORS.put(12351216, 49);
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

   public void setColor(float red, float green, float blue)
   {
      int r8 = (int)(red * 255);
      int g8 = (int)(green * 255);
      int b8 = (int)(blue * 255);
      int total = (r8 << 16) | (g8 << 8) | b8;

      final Integer color = COLORS.get(total);
      if (color != null)
      {
         mColor = color;
         return;
      }

      mColor = 13;
   }

   public void setColor(int color)
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

   private int mColor = COLOR_NONE;

   private int mDisplayedColor = -1;

   private int mBlinkColor = COLOR_NONE;

   private int mDisplayedBlinkColor = -1;

   private int mBlinkType = BLINK_NONE;

   private int mDisplayedBlinkType = -1;
}
