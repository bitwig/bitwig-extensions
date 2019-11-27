package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class RecordArmOverlay extends Overlay
{
   public RecordArmOverlay(final LaunchpadProControllerExtension driver)
   {
      super(driver, "record");

      final TrackBank trackBank = driver.getTrackBank();
      for (int x = 0; x < 8; ++x)
      {
         final Track track = trackBank.getItemAt(x);
         final Button button = driver.getPadButton(x, 0);
         final SettableBooleanValue arm = track.arm();
         bindPressed(button, arm.toggleAction());
         bindLightState(() -> {
            if (track.exists().get())
               return arm.get() ? LedState.REC_ON : LedState.REC_OFF;
            return LedState.OFF;
         }, button);
      }

      bindLightState(LedState.REC_ON, driver.getArmButton());
   }
}
