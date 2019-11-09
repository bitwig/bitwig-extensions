package com.bitwig.extensions.controllers.novation.common;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.SettableColorValue;

public class DefaultPalette
{
   public static int getColorIndexClosestToColor(final SettableColorValue colorValue)
   {
      float minError = Float.MAX_VALUE;
      int colorIndex = 0;

      final Color color = Color.fromRGB(colorValue.red(), colorValue.green(), colorValue.blue());

      final int N = PALETTE.length / 3;

      for(int i=0; i<N; i++)
      {
         final int r = PALETTE[i*3];
         final int g = PALETTE[i*3+1];
         final int b = PALETTE[i*3+2];

         final float hsvError = computeHsvError(r, g, b, color);

         if (hsvError < minError)
         {
            colorIndex = i;
            minError = hsvError;
         }
      }

      return colorIndex;
   }

   private static float computeHsvError(int r, int g, int b, final Color color)
   {
      float[] hsv = new float[3];
      RGBtoHSV(r, g, b, hsv);
      float[] hsvRef = new float[3];
      RGBtoHSV(color.getRed255(), color.getGreen255(), color.getBlue255(), hsvRef);

      float hueError = (hsv[0] - hsvRef[0]) / 30;
      float sError = (hsv[1] - hsvRef[1]) * 1.6f;
      final float vScale = 1f;
      float vError = (vScale * hsv[2] - hsvRef[2]) / 40;

      final float error = hueError * hueError + vError*vError + sError*sError;

      return error;
   }

   private static int[] PALETTE =
   {
      0, 0, 0,
      187, 190, 187,
      239, 239, 239,
      255, 251, 255,
      255, 182, 215,
      255, 64, 75,
      246, 104, 111,
      251, 149, 154,
      255, 243, 229,
      255, 163, 0,
      255, 184, 79,
      255, 204, 118,
      255, 226, 173,
      255, 241, 42,
      255, 248, 142,
      255, 252, 190,
      217, 240, 203,
      122, 198, 46,
      160, 217, 121,
      179, 225, 147,
      0, 232, 178,
      0, 211, 0,
      0, 191, 39,
      0, 161, 36,
      0, 244, 194,
      0, 240, 75,
      0, 233, 64,
      0, 220, 13,
      44, 255, 219,
      0, 236, 196,
      0, 231, 177,
      0, 244, 194,
      1, 243, 241,
      0, 235, 224,
      0, 236, 215,
      0, 243, 222,
      46, 225, 255,
      2, 223, 255,
      2, 217, 255,
      2, 176, 226,
      81, 206, 255,
      4, 154, 255,
      4, 142, 237,
      3, 115, 212,
      133, 142, 255,
      4, 149, 254,
      64, 100, 255,
      69, 73, 231,
      188, 145, 255,
      144, 67, 255,
      106, 63, 214,
      87, 67, 205,
      255, 176, 255,
      255, 75, 255,
      232, 62, 245,
      232, 49, 246,
      255, 145, 234,
      251, 0, 203,
      250, 1, 193,
      251, 1, 186,
      255, 5, 45,
      255, 162, 0,
      250, 224, 0,
      55, 188, 97,
      0, 215, 0,
      0, 214, 151,
      5, 137, 255,
      42, 100, 255,
      2, 185, 217,
      66, 88, 231,
      179, 186, 208,
      145, 162, 183,
      255, 4, 32,
      206, 236, 103,
      199, 225, 0,
      65, 236, 0,
      0, 208, 0,
      0, 220, 160,
      2, 223, 255,
      4, 152, 255,
      95, 85, 255,
      188, 74, 255,
      237, 120, 227,
      198, 127, 61,
      255, 149, 0,
      157, 226, 0,
      131, 231, 44,
      57, 189, 98,
      0, 225, 8,
      34, 226, 187,
      1, 223, 224,
      145, 202, 255,
      106, 186, 252,
      175, 172, 245,
      229, 120, 245,
      255, 46, 211,
      252, 133, 0,
      220, 220, 0,
      136, 219, 0,
      251, 211, 0,
      211, 174, 28,
      0, 207, 94,
      0, 215, 180,
      114, 123, 194,
      67, 136, 228,
      236, 197, 159,
      255, 17, 45,
      255, 157, 161,
      251, 154, 86,
      235, 202, 100,
      179, 222, 107,
      136, 219, 0,
      99, 107, 195,
      205, 202, 159,
      127, 217, 195,
      203, 223, 255,
      186, 206, 250,
      161, 178, 190,
      187, 193, 215,
      212, 227, 255,
      255, 4, 0,
      223, 3, 11,
      140, 209, 80,
      0, 162, 0,
      253, 255, 0,
      189, 179, 0,
      251, 210, 0,
      245, 115, 0};

   public static void RGBtoHSV(final float r, final float g, final float b, final float[] hsv)
   {
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

      if (max != 0)
      {
         s = delta / max; // s
      }
      else
      {
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

      if (delta == 0)
      {
         h = 0;
      }
      else
      {
         if (r == max)
         {
            h = (g - b) / delta; // between yellow & magenta
         }
         else if (g == max)
         {
            h = 2 + (b - r) / delta; // between cyan & yellow
         }
         else
         {
            h = 4 + (r - g) / delta; // between magenta & cyan
         }
      }

      h *= 60; // degrees
      if (h < 0)
      {
         h += 360;
      }

      assert h >= 0 && h <= 360;
      assert s >= 0 && s <= 1;
      assert v >= 0 && v <= 1;

      hsv[0] = h;
      hsv[1] = s;
      hsv[2] = v;
   }
}
