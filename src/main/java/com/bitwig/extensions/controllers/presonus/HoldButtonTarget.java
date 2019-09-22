package com.bitwig.extensions.controllers.presonus;

public abstract class HoldButtonTarget implements ButtonTarget
{
   @Override
   public boolean isOn(final boolean isPressed)
   {
      return isPressed;
   }

   @Override
   public void press()
   {
      setState(true);
   }

   @Override
   public void release()
   {
      setState(false);
   }

   protected abstract void setState(final boolean b);
}
