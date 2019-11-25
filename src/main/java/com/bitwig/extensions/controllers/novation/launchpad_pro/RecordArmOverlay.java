package com.bitwig.extensions.controllers.novation.launchpad_pro;

public class RecordArmOverlay extends Overlay
{
   private static final Color NO_TRACK_COLOR = Color.fromRgb255(0, 0, 0);
   private static final Color RECORD_ARM_ON_COLOR = Color.fromRgb255(255, 0, 0);
   private static final Color RECORD_ARM_OFF_COLOR = Color.scale(RECORD_ARM_ON_COLOR, 0.2f);

   public RecordArmOverlay(final LaunchpadProControllerExtension launchpadProControllerExtension)
   {
      super(launchpadProControllerExtension, "record");
   }

   @Override
   public void onPadPressed(final int x, final int velocity)
   {
      mDriver.getTrackBank().getItemAt(x).arm().toggle();
   }

   @Override
   public void paint()
   {
      super.paint();

      for (int x = 0; x < 8; ++x)
      {
         final boolean isArmed = mDriver.getTrackBank().getItemAt(x).arm().get();
         final boolean exists = mDriver.getTrackBank().getItemAt(x).exists().get();
         mDriver.getPadLed(x, 0).setColor(!exists ? NO_TRACK_COLOR :
            isArmed ? RECORD_ARM_ON_COLOR : RECORD_ARM_OFF_COLOR);
      }
   }

   @Override
   public void paintModeButton()
   {
      final Led led = mDriver.getBottomLed(0);
      led.setColor(isActive() ? RECORD_ARM_ON_COLOR : RECORD_ARM_OFF_COLOR);
   }
}
