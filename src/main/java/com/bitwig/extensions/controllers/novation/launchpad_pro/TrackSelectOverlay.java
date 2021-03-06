package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

final class TrackSelectOverlay extends Overlay
{
   TrackSelectOverlay(final LaunchpadProControllerExtension driver)
   {
      super(driver, "track-select");

      final TrackBank trackBank = driver.mTrackBank;
      for (int x = 0; x < 8; ++x)
      {
         final int X = x;
         final Track track = trackBank.getItemAt(x);
         final Button button = driver.getPadButton(x, 0);
         bindPressed(button, track::selectInMixer);
         bindLightState(() -> {
            if (track.exists().get())
               return trackBank.cursorIndex().get() == X ? LedState.TRACK : LedState.TRACK_LOW;
            return LedState.OFF;
         }, button);
      }

      bindLightState(LedState.TRACK, driver.mSelectButton);
   }
}
