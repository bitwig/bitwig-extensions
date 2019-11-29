package com.bitwig.extensions.controllers.novation.launchpad_pro;


abstract class Mode extends LaunchpadLayer
{

   Mode(final LaunchpadProControllerExtension driver, final String name)
   {
      super(driver, name + "-mode");
      mDriver = driver;
   }

   @Override
   final protected void onActivate()
   {
      doActivate();

      final String modeDescription = getModeDescription();
      if (modeDescription != null)
         mDriver.getHost().showPopupNotification(modeDescription);
   }

   protected abstract String getModeDescription();

   protected void doActivate()
   {
      /* for subclasses */
   }

   @Override
   final protected void onDeactivate()
   {
      doDeactivate();
   }

   protected void doDeactivate()
   {
      /* for subclasses */
   }

   void updateKeyTranslationTable(final Integer[] table)
   {
   }

   void onCursorClipExists(final boolean exists)
   {
   }

   final LaunchpadProControllerExtension mDriver;
}
