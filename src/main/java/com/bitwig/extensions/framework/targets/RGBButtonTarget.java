package com.bitwig.extensions.framework.targets;

import com.bitwig.extension.controller.api.ColorValue;

public interface RGBButtonTarget extends ButtonTarget
{
   float[] getRGB();

   static float[] getFromValue(ColorValue v)
   {
      float[] rgb = new float[3];
      rgb[0] = v.red();
      rgb[1] = v.green();
      rgb[2] = v.blue();
      return rgb;
   }

   static float[] mixWithValue(ColorValue v, float[] color, float mix)
   {
      float[] rgb = new float[3];
      rgb[0] = (1-mix)*v.red() + mix*color[0];
      rgb[1] = (1-mix)*v.green() + mix*color[1];
      rgb[2] = (1-mix)*v.blue() + mix*color[2];
      return rgb;
   }

   static float[] mix(float[] A, float[] B, float mix)
   {
      float[] rgb = new float[3];
      rgb[0] = (1-mix)*A[0] + mix*B[0];
      rgb[1] = (1-mix)*A[1] + mix*B[1];
      rgb[2] = (1-mix)*A[2] + mix*B[2];
      return rgb;
   }
}
