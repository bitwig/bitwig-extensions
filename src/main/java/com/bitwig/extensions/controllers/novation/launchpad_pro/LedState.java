package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class LedState implements InternalHardwareLightState
{
   public static final InternalHardwareLightState OFF = new LedState();

   LedState()
   {
      this(Color.OFF, 0);
   }

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
      return HardwareLightVisualState.createForColor(mColor.toApiColor());
   }

   public Color getColor()
   {
      return mColor;
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

   private final Color mColor;
   private final int mPulse;
}
