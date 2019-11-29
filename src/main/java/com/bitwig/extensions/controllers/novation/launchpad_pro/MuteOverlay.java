package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

class MuteOverlay extends Overlay
{
   MuteOverlay(final LaunchpadProControllerExtension driver)
   {
      super(driver, "mute");

      final TrackBank trackBank = driver.getTrackBank();
      for (int x = 0; x < 8; ++x)
      {
         final Track track = trackBank.getItemAt(x);
         final Button button = driver.getPadButton(x, 0);
         bindPressed(button, track.mute().toggleAction());
         bindLightState(() -> {
            if (track.exists().get())
               return track.mute().get() ? LedState.MUTE : LedState.MUTE_LOW;
            return LedState.OFF;
         }, button);
      }

      bindLightState(LedState.MUTE, driver.getMuteButton());
   }
}
