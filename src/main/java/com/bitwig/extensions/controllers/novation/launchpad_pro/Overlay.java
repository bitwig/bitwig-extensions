package com.bitwig.extensions.controllers.novation.launchpad_pro;

public abstract class Overlay extends Mode
{
   Overlay(final LaunchpadProControllerExtension driver, final String name)
   {
      super(driver, name);
   }

   @Override
   protected String getModeDescription()
   {
      return null;
   }

   @Override
   public final void onPadPressed(final int x, final int y, final int velocity)
   {
   }

   @Override
   public final void onPadReleased(final int x, final int y, final int velocity, final boolean wasHeld)
   {
   }

   public abstract void onPadPressed(final int x, final int velocity);
   public abstract void onPadReleased(final int x, final int velocity);

   @Override
   void updateKeyTranslationTable(final Integer[] table)
   {
      /* Don't send notes on the bottom overlay */
      for (int x = 0; x < 8; ++x)
         table[11 + x] = -1;
   }
}
