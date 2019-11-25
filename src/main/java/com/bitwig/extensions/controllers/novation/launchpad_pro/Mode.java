package com.bitwig.extensions.controllers.novation.launchpad_pro;


abstract class Mode extends LaunchpadLayer
{

   Mode(final LaunchpadProControllerExtension driver, final String name)
   {
      super(driver, name);
      mDriver = driver;
   }

   @Override
   final protected void onActivate()
   {
      doActivate();

      paint();

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
      paintModeButton();
   }

   protected void doDeactivate()
   {
      /* for subclasses */
   }

   void paint()
   {
      paintModeButton();
   }

   void paintModeButton()
   {
   }

   void onPadPressed(final int x, final int y, final int velocity)
   {}

   void onPadPressure(final int x, final int y, final int pressure)
   {}

   void onPadReleased(final int x, final int y, final int velocity, final boolean wasHeld) {}

   final LaunchpadProControllerExtension mDriver;

   void onArrowUpReleased()
   {
   }

   void onArrowUpPressed()
   {
   }

   void onArrowDownReleased()
   {
   }

   void onArrowDownPressed()
   {
   }

   void onArrowRightPressed()
   {

   }

   void onArrowRightReleased()
   {

   }

   void onArrowLeftPressed()
   {

   }

   void onArrowLeftReleased()
   {

   }

   void onSceneButtonPressed(final int column)
   {
   }

   void onShiftPressed()
   {
   }

   void onShiftReleased()
   {
   }

   void onDeletePressed()
   {
   }

   void onQuantizePressed()
   {
   }

   void updateKeyTranslationTable(final Integer[] table)
   {
   }

   public void onCursorClipExists(final boolean exists)
   {
   }
}
