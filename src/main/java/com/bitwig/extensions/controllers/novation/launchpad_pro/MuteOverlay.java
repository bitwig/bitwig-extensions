package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

final class MuteOverlay extends Overlay
{
   MuteOverlay(final LaunchpadProControllerExtension driver)
   {
      super(driver, "mute");

      final TrackBank trackBank = driver.mTrackBank;
      for (int x = 0; x < 8; ++x)
      {
         final Track track = trackBank.getItemAt(x);
         final Button button = driver.getPadButton(x, 0);
         bindToggle(button, track.mute());
         bindLightState(() -> {
            if (track.exists().get())
               return track.mute().get() ? LedState.MUTE : LedState.MUTE_LOW;
            return LedState.OFF;
         }, button);
      }

      bindLightState(LedState.MUTE, driver.mMuteButton);
   }

   @Override
   protected void doActivate()
   {
      final TrackBank trackBank = mDriver.mTrackBank;
      for (int i = 0; i < 8; ++i)
         trackBank.getItemAt(i).mute().subscribe();
   }

   @Override
   protected void doDeactivate()
   {
      final TrackBank trackBank = mDriver.mTrackBank;
      for (int i = 0; i < 8; ++i)
         trackBank.getItemAt(i).mute().subscribe();
   }
}
