package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

final class StopClipOverlay extends Overlay
{
   StopClipOverlay(final LaunchpadProControllerExtension driver)
   {
      super(driver, "stop-clip");

      final TrackBank trackBank = driver.getTrackBank();
      for (int x = 0; x < 8; ++x)
      {
         final Track track = trackBank.getItemAt(x);
         final Button button = driver.getPadButton(x, 0);
         bindPressed(button, track.stopAction());
         bindLightState(() -> {
            if (track.exists().get())
               return !track.isStopped().get() ? LedState.STOP_CLIP_ON : LedState.STOP_CLIP_OFF;
            return LedState.OFF;
         }, button);
      }

      bindLightState(LedState.STOP_CLIP_ON, driver.getStopButton());
   }

   @Override
   protected void doActivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();
      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.subscribe();
         track.isStopped().subscribe();
         track.exists().subscribe();
      }
   }

   @Override
   protected void doDeactivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();
      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.exists().unsubscribe();
         track.isStopped().unsubscribe();
         track.unsubscribe();
      }
   }
}
