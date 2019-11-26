package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class LedState implements InternalHardwareLightState
{
   LedState(final Color color)
   {
      this(color, 0);
   }

   LedState(final Color color, final int pulse)
   {
      mColor = color;
      mPulse = pulse;
   }

   @Override
   public HardwareLightVisualState getVisualState()
   {
      return null;
   }

   public void setColor(final Color color)
   {
      mColor = color;
   }

   public void setColor(final float red, final float green, final float blue)
   {
      mColor.set(red, green, blue);
   }

   public Color getColor()
   {
      return mColor;
   }

   public void setPulse(final int pulseColor)
   {
      mPulse = pulseColor;
   }

   public int getPulse()
   {
      return mPulse;
   }

   @Override
   public boolean equals(final Object obj)
   {
      return obj instanceof LedState ? equals((LedState)obj) : false;
   }

   public boolean equals(final LedState obj)
   {
      if (obj == this)
         return true;

      return mColor.equals(obj.mColor) && mPulse == obj.mPulse;
   }

   public void set(final LedState other)
   {
      mColor.set(other.mColor);
      mPulse = other.mPulse;
   }

   private Color mColor;
   private int mPulse;
}
