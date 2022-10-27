package com.bitwig.extensions.controllers.akai.apc40_mkii;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

class RGBLedState extends InternalHardwareLightState
{
   public static final int COLOR_NONE = 0;

   public static final int COLOR_RED = 2;

   public static final int COLOR_GREEN = 18;

   public static final int COLOR_BLUE = 42;

   public static final int COLOR_YELLOW = 10;

   public static final int COLOR_RECORDING = COLOR_RED;

   public static final int COLOR_PLAYING = COLOR_GREEN;

   public static final int COLOR_PLAYING_QUEUED = COLOR_YELLOW;

   public static final int COLOR_STOPPING = COLOR_NONE;

   public static final int COLOR_SCENE = COLOR_YELLOW;

   public static final int BLINK_NONE = 0;

   public static final int BLINK_PLAY_QUEUED = 14;

   public static final int BLINK_ACTIVE = 10;

   public static final int BLINK_RECORD_QUEUED = 13;

   public static final int BLINK_STOP_QUEUED = 13;

   private static final Map<Integer, Integer> RGB_TO_COLOR_VALUE_MAP = new HashMap<>();

   private static final Map<Integer, Color> COLOR_VALUE_TO_COLOR_MAP = new HashMap<>();

   private static void registerColor(final int rgb, final int value)
   {
      COLOR_VALUE_TO_COLOR_MAP.put(value,
         Color.fromRGB255((rgb & 0xFF0000) >> 16, (rgb & 0xFF00) >> 8, rgb & 0xFF));

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

   public static int getColorValueForRGB(final int rgb)
   {
      final Integer c = RGB_TO_COLOR_VALUE_MAP.get(rgb);

      if (c != null)
         return c;

      return 13;
   }

   public static Color getColorForColorValue(final int colorValue)
   {
      return COLOR_VALUE_TO_COLOR_MAP.get(colorValue);
   }

   public RGBLedState(final int color, final int blinkColor, final int blinkType)
   {
      super();
      mColor = color;
      mBlinkColor = blinkColor;
      mBlinkType = blinkType;
   }

   public int getColor()
   {
      return mColor;
   }

   public int getBlinkColor()
   {
      return mBlinkColor;
   }

   public int getBlinkType()
   {
      return mBlinkType;
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + mBlinkColor;
      result = prime * result + mBlinkType;
      result = prime * result + mColor;
      return result;
   }

   @Override
   public boolean equals(final Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      final RGBLedState other = (RGBLedState)obj;
      if (mBlinkColor != other.mBlinkColor)
         return false;
      if (mBlinkType != other.mBlinkType)
         return false;
      if (mColor != other.mColor)
         return false;
      return true;
   }

   @Override
   public HardwareLightVisualState getVisualState()
   {
      final Color color = getColorForColorValue(mColor);

      if (mBlinkType == BLINK_NONE)
         return HardwareLightVisualState.createForColor(color);

      final Color offColor = getColorForColorValue(mBlinkColor);

      if (mBlinkType == BLINK_PLAY_QUEUED)
         return HardwareLightVisualState.createBlinking(color, offColor, 0.2, 0.2);

      return HardwareLightVisualState.createBlinking(color, offColor, 0.5, 0.5);
   }

   private final int mColor, mBlinkColor, mBlinkType;

}
