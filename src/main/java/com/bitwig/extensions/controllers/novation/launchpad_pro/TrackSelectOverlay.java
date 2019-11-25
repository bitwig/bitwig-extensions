package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.TrackBank;

class TrackSelectOverlay extends Overlay
{
   TrackSelectOverlay(final LaunchpadProControllerExtension launchpadProControllerExtension)
   {
      super(launchpadProControllerExtension, "track-select");
   }

   @Override
   public void onPadPressed(final int x, final int velocity)
   {
      mDriver.getTrackBank().getItemAt(x).selectInMixer();
   }

   @Override
   public void paint()
   {
      super.paint();

      final TrackBank trackBank = mDriver.getTrackBank();
      final int selectedTrackIndex = trackBank.cursorIndex().get();
      for (int x = 0; x < 8; ++x)
      {
         final boolean isSelected = selectedTrackIndex == x;
         final boolean exists = trackBank.getItemAt(x).exists().get();
         mDriver.getPadLed(x, 0).setColor(!exists ? Color.OFF : (isSelected ? Color.TRACK : Color.TRACK_LOW));
      }
   }

   @Override
   public void paintModeButton()
   {
      final Led led = mDriver.getBottomLed(1);
      led.setColor(isActive() ? Color.TRACK : Color.TRACK_LOW);
   }
}
