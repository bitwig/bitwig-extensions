package com.bitwig.extensions.controllers.novation.launchpad_pro;

abstract class Overlay extends Mode
{
   Overlay(final LaunchpadProControllerExtension driver, final String name)
   {
      super(driver, name);
   }

   @Override
   final protected String getModeDescription()
   {
      return null;
   }

   @Override
   void updateKeyTranslationTable(final Integer[] table)
   {
      /* Don't send notes on the bottom overlay */
      for (int x = 0; x < 8; ++x)
         table[11 + x] = -1;
   }
}
