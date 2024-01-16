package com.bitwig.extensions.controllers.akai.apc40_mkii;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

class RGBLedState extends InternalHardwareLightState
{
   /** Array of colors that the protocol specifies. */
   private static final Color[] COLORS = new Color[128];

   public static final int COLOR_NONE = 0;

   public static final int COLOR_WHITE = 3;

   public static final int COLOR_RED = 5;

   public static final int COLOR_GREEN = 21;

   public static final int COLOR_YELLOW = 13;

   public static final int COLOR_RECORDING = COLOR_RED;

   public static final int COLOR_PLAYING = COLOR_GREEN;

   public static final int COLOR_PLAYING_QUEUED = COLOR_YELLOW;

   public static final int COLOR_STOPPING = COLOR_NONE;

   public static final int COLOR_SELECTED = COLOR_YELLOW;

   public static final int COLOR_SELECTABLE = 1;

   public static final int BLINK_NONE = 0;

   public static final int BLINK_PLAY_QUEUED = 14;

   public static final int BLINK_ACTIVE = 10;

   public static final int BLINK_RECORD_QUEUED = 13;

   public static final int BLINK_STOP_QUEUED = 13;

   public static final RGBLedState OFF_STATE = new RGBLedState(COLOR_NONE, COLOR_NONE, BLINK_NONE);

   /**
    * Registers a color as defined in the APC 40 mkii MIDI protocol. The color value is the velocity to use
    * for the provided RGB integer color.
    */
   private static void registerColor(final int rgb, final int value)
   {
      assert value >= 0 && value <= 127;
      assert COLORS[value] == null;

      COLORS[value] = createColorForRGBInt(rgb);
   }

   private static Color createColorForRGBInt(final int rgb)
   {
      final int red = (rgb & 0xFF0000) >> 16;
      final int green = (rgb & 0xFF00) >> 8;
      final int blue = rgb & 0xFF;

      return Color.fromRGB255(red, green, blue);
   }

   public static double[] rgbToHsv(final Color color)
   {
      final double[] hsv = new double[3];

      final double r = color.getRed();
      final double g = color.getGreen();
      final double b = color.getBlue();

      final double max = Math.max(r, Math.max(g, b));
      final double min = Math.min(r, Math.min(g, b));
      final double delta = max - min;

      // Calculate hue
      if (delta == 0)
      {
         hsv[0] = 0;
      }
      else if (max == r)
      {
         hsv[0] = (60 * ((g - b) / delta) + 360) % 360;
      }
      else if (max == g)
      {
         hsv[0] = (60 * ((b - r) / delta) + 120) % 360;
      }
      else if (max == b)
      {
         hsv[0] = (60 * ((r - g) / delta) + 240) % 360;
      }

      // Calculate saturation
      hsv[1] = (max == 0) ? 0 : (delta / max);

      // Calculate value
      hsv[2] = max;

      return hsv;
   }

   private static double colorDistance(final Color color1, final Color color2)
   {
      return 0.5 * colorDistanceRGB(color1, color2) + 0.5 * colorDistanceHSV(color1, color2);
   }

   private static double colorDistanceRGB(final Color color1, final Color color2)
   {
      final double r1 = color1.getRed();
      final double g1 = color1.getGreen();
      final double b1 = color1.getBlue();

      final double r2 = color2.getRed();
      final double g2 = color2.getGreen();
      final double b2 = color2.getBlue();

      final double dr = r2 - r1;
      final double dg = g2 - g1;
      final double db = b2 - b1;

      return Math.sqrt(dr * dr + dg * dg + db * db);
   }

   private static double colorDistanceHSV(final Color color1, final Color color2)
   {
      final double[] hsv1 = rgbToHsv(color1);
      final double[] hsv2 = rgbToHsv(color2);

      final double dh = Math.min(Math.abs(hsv1[0] - hsv2[0]), 1 - Math.abs(hsv1[0] - hsv2[0]));
      final double ds = Math.abs(hsv1[1] - hsv2[1]);
      final double dv = Math.abs(hsv1[2] - hsv2[2]);

      return Math.sqrt(dh * dh + ds * ds + dv * dv);
   }

   private static record ColorToIndexCacheEntry(int rgb, int index)
   {
   }

   private static final int colorToRGBInt(final Color color)
   {
      return color.getRed255() << 16 | color.getGreen255() << 8 | color.getBlue255();
   }

   private static final Map<Integer, Integer> HANDPICKED_RGBINT_TO_CLOSEST_COLOR_INDEX = new HashMap<>();

   private static void registerHandpickedClosestColor(final int rgb, final int colorIndex)
   {
      HANDPICKED_RGBINT_TO_CLOSEST_COLOR_INDEX.put(rgb, colorIndex);
   }

   static 
   {
      registerHandpickedClosestColor(0xFF0000, COLOR_RED);
      registerHandpickedClosestColor(0xFF00, COLOR_GREEN);
      registerHandpickedClosestColor(0xFF, 45);
      registerHandpickedClosestColor(0xFFD90F, COLOR_YELLOW);

      registerHandpickedClosestColor(0, 0);

      registerHandpickedClosestColor(14235761, 57);
      registerHandpickedClosestColor(14771857, 107);
      registerHandpickedClosestColor(5526612, 1);

      registerHandpickedClosestColor(14233124, 6);
      registerHandpickedClosestColor(15491415, 5);
      registerHandpickedClosestColor(8026746, 2);

      registerHandpickedClosestColor(16733958, 9);
      registerHandpickedClosestColor(16745278, 12);
      registerHandpickedClosestColor(13224393, 3);

      registerHandpickedClosestColor(14261520, 14);
      registerHandpickedClosestColor(14989134, 13);
      registerHandpickedClosestColor(8817068, 104);

      registerHandpickedClosestColor(7575572, 18);
      registerHandpickedClosestColor(10534988, 17);
      registerHandpickedClosestColor(10713411, 125);

      registerHandpickedClosestColor(40263, 22);
      registerHandpickedClosestColor(4111202, 21);
      registerHandpickedClosestColor(13016944, 124);

      registerHandpickedClosestColor(42644, 34);
      registerHandpickedClosestColor(4444857, 33);
      registerHandpickedClosestColor(5726662, 43);

      registerHandpickedClosestColor(39385, 38);
      registerHandpickedClosestColor(4507903, 37);
      registerHandpickedClosestColor(8686304, 115);

      registerHandpickedClosestColor(9783755, 50);
      registerHandpickedClosestColor(12351216, 49);
   }

   private static final ArrayList<ColorToIndexCacheEntry> RGB_TO_COMPUTED_CLOSEST_COLOR_INDEX_CACHE = new ArrayList<>();

   public static int getClosestColorIndex(final Color color)
   {
      if (color == null || color.getAlpha() == 0)
         return 0;

      final int rgb = colorToRGBInt(color);

      final Integer handPickedColorIndex = HANDPICKED_RGBINT_TO_CLOSEST_COLOR_INDEX.get(rgb);

      if (handPickedColorIndex != null)
         return handPickedColorIndex;

      final int MAX_CACHE_SIZE = 64;

      synchronized (RGB_TO_COMPUTED_CLOSEST_COLOR_INDEX_CACHE)
      {
         final int cacheSize = RGB_TO_COMPUTED_CLOSEST_COLOR_INDEX_CACHE.size();

         for (int i = 0; i < cacheSize; i++)
         {
            final var cacheEntry = RGB_TO_COMPUTED_CLOSEST_COLOR_INDEX_CACHE.get(i);

            if (cacheEntry.rgb == rgb)
               return cacheEntry.index;
         }

         final int colorIndex = computeClosestColorIndex(color);

         if (cacheSize == MAX_CACHE_SIZE)
            RGB_TO_COMPUTED_CLOSEST_COLOR_INDEX_CACHE.remove(MAX_CACHE_SIZE - 1);

         RGB_TO_COMPUTED_CLOSEST_COLOR_INDEX_CACHE.add(0, new ColorToIndexCacheEntry(rgb, colorIndex));

         return colorIndex;
      }
   }

   private static int computeClosestColorIndex(final Color color)
   {
      if (color == null || color.getAlpha() == 0)
         return 0;

      int closestIndex = 0;
      double closestDistance = Double.MAX_VALUE;

      for (int i = 0; i < COLORS.length; i++)
      {
         final Color currentColor = COLORS[i];
         final double distance = colorDistance(color, currentColor);

         if (distance == 0)
            return i;

         if (distance < closestDistance)
         {
            closestIndex = i;
            closestDistance = distance;
         }
      }

      return closestIndex;
   }

   public static Color getColorForColorValue(final int colorValue)
   {
      assert colorValue >= 0 && colorValue < COLORS.length;

      if (colorValue < 0 || colorValue >= COLORS.length)
         return COLORS[0];

      return COLORS[colorValue];
   }

   public static RGBLedState getBestStateForColor(final Color color)
   {
      final int colorIndex = getClosestColorIndex(color);

      return new RGBLedState(colorIndex, COLOR_NONE, BLINK_NONE);
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

   static
   {
      registerColor(0x000000, 0);
      registerColor(0x1E1E1E, 1);
      registerColor(0x7F7F7F, 2);
      registerColor(0xFFFFFF, 3);
      registerColor(0xFF4C4C, 4);
      registerColor(0xFF0000, 5);
      registerColor(0x590000, 6);
      registerColor(0x190000, 7);
      registerColor(0xFFBD6C, 8);
      registerColor(0xFF5400, 9);
      registerColor(0x591D00, 10);
      registerColor(0x271B00, 11);
      registerColor(0xFFFF4C, 12);
      registerColor(0xFFFF00, 13);
      registerColor(0x595900, 14);
      registerColor(0x191900, 15);
      registerColor(0x88FF4C, 16);
      registerColor(0x54FF00, 17);
      registerColor(0x1D5900, 18);
      registerColor(0x142B00, 19);
      registerColor(0x4CFF4C, 20);
      registerColor(0x00FF00, 21);
      registerColor(0x005900, 22);
      registerColor(0x001900, 23);
      registerColor(0x4CFF5E, 24);
      registerColor(0x00FF19, 25);
      registerColor(0x00590D, 26);
      registerColor(0x001902, 27);
      registerColor(0x4CFF88, 28);
      registerColor(0x00FF55, 29);
      registerColor(0x00591D, 30);
      registerColor(0x001F12, 31);
      registerColor(0x4CFFB7, 32);
      registerColor(0x00FF99, 33);
      registerColor(0x005935, 34);
      registerColor(0x001912, 35);
      registerColor(0x4CC3FF, 36);
      registerColor(0x00A9FF, 37);
      registerColor(0x004152, 38);
      registerColor(0x001019, 39);
      registerColor(0x4C88FF, 40);
      registerColor(0x0055FF, 41);
      registerColor(0x001D59, 42);
      registerColor(0x000819, 43);
      registerColor(0x4C4CFF, 44);
      registerColor(0x0000FF, 45);
      registerColor(0x000059, 46);
      registerColor(0x000019, 47);
      registerColor(0x874CFF, 48);
      registerColor(0x5400FF, 49);
      registerColor(0x190064, 50);
      registerColor(0x0F0030, 51);
      registerColor(0xFF4CFF, 52);
      registerColor(0xFF00FF, 53);
      registerColor(0x590059, 54);
      registerColor(0x190019, 55);
      registerColor(0xFF4C87, 56);
      registerColor(0xFF0054, 57);
      registerColor(0x59001D, 58);
      registerColor(0x220013, 59);
      registerColor(0xFF1500, 60);
      registerColor(0x993500, 61);
      registerColor(0x795100, 62);
      registerColor(0x436400, 63);
      registerColor(0x033900, 64);
      registerColor(0x005735, 65);
      registerColor(0x00547F, 66);
      registerColor(0x0000FF, 67);
      registerColor(0x00454F, 68);
      registerColor(0x2500CC, 69);
      registerColor(0x7F7F7F, 70);
      registerColor(0x202020, 71);
      registerColor(0xFF0000, 72);
      registerColor(0xBDFF2D, 73);
      registerColor(0xAFED06, 74);
      registerColor(0x64FF09, 75);
      registerColor(0x108B00, 76);
      registerColor(0x00FF87, 77);
      registerColor(0x00A9FF, 78);
      registerColor(0x002AFF, 79);
      registerColor(0x3F00FF, 80);
      registerColor(0x7A00FF, 81);
      registerColor(0xB21A7D, 82);
      registerColor(0x402100, 83);
      registerColor(0xFF4A00, 84);
      registerColor(0x88E106, 85);
      registerColor(0x72FF15, 86);
      registerColor(0x00FF00, 87);
      registerColor(0x3BFF26, 88);
      registerColor(0x59FF71, 89);
      registerColor(0x38FFCC, 90);
      registerColor(0x5B8AFF, 91);
      registerColor(0x3151C6, 92);
      registerColor(0x877FE9, 93);
      registerColor(0xD31DFF, 94);
      registerColor(0xFF005D, 95);
      registerColor(0xFF7F00, 96);
      registerColor(0xB9B000, 97);
      registerColor(0x90FF00, 98);
      registerColor(0x835D07, 99);
      registerColor(0x392b00, 100);
      registerColor(0x144C10, 101);
      registerColor(0x0D5038, 102);
      registerColor(0x15152A, 103);
      registerColor(0x16205A, 104);
      registerColor(0x693C1C, 105);
      registerColor(0xA8000A, 106);
      registerColor(0xDE513D, 107);
      registerColor(0xD86A1C, 108);
      registerColor(0xFFE126, 109);
      registerColor(0x9EE12F, 110);
      registerColor(0x67B50F, 111);
      registerColor(0x1E1E30, 112);
      registerColor(0xDCFF6B, 113);
      registerColor(0x80FFBD, 114);
      registerColor(0x9A99FF, 115);
      registerColor(0x8E66FF, 116);
      registerColor(0x404040, 117);
      registerColor(0x757575, 118);
      registerColor(0xE0FFFF, 119);
      registerColor(0xA00000, 120);
      registerColor(0x350000, 121);
      registerColor(0x1AD000, 122);
      registerColor(0x074200, 123);
      registerColor(0xB9B000, 124);
      registerColor(0x3F3100, 125);
      registerColor(0xB35F00, 126);
      registerColor(0x4B1502, 127);
   }
}
