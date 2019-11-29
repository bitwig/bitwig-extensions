package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

class RGBLightState extends InternalHardwareLightState
{
   private static int colorPartFromDouble(final double x)
   {
      return Math.max(0, Math.min((int)(31.0 * x), 31));
   }

   public RGBLightState(final int red, final int green, final int blue)
   {
      super();

      assert red >= 0 && red <= 31;
      assert green >= 0 && green <= 31;
      assert blue >= 0 && blue <= 31;

      mIsOn = true;
      mRed = red;
      mGreen = green;
      mBlue = blue;
   }

   public RGBLightState(final Color color)
   {
      this(colorPartFromDouble(color.getRed()), colorPartFromDouble(color.getGreen()),
         colorPartFromDouble(color.getBlue()));
   }

   public RGBLightState()
   {
      mIsOn = false;
      mRed = 0;
      mGreen = 0;
      mBlue = 0;
   }

   public boolean isOn()
   {
      return mIsOn;
   }

   public int getRed()
   {
      return mRed;
   }

   public int getGreen()
   {
      return mGreen;
   }

   public int getBlue()
   {
      return mBlue;
   }

   public Color getColor()
   {
      return Color.fromRGB(mRed / 31.0, mGreen / 31.0, mBlue / 31.0);
   }

   @Override
   public HardwareLightVisualState getVisualState()
   {
      if (mIsOn)
         return HardwareLightVisualState.createForColor(getColor());

      return HardwareLightVisualState.createForColor(Color.blackColor());
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + mBlue;
      result = prime * result + mGreen;
      result = prime * result + (mIsOn ? 1231 : 1237);
      result = prime * result + mRed;
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
      final RGBLightState other = (RGBLightState)obj;
      if (mBlue != other.mBlue)
         return false;
      if (mGreen != other.mGreen)
         return false;
      if (mIsOn != other.mIsOn)
         return false;
      if (mRed != other.mRed)
         return false;
      return true;
   }

   private final boolean mIsOn;

   private final int mRed, mGreen, mBlue;
}
