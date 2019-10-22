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
}
