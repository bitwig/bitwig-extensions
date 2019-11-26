package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.Track;

public class MuteOverlay extends Overlay
{
   MuteOverlay(final LaunchpadProControllerExtension driver)
   {
      super(driver, "mute");
   }

   @Override
   public void onPadPressed(final int x, final int velocity)
   {
      final Track channel = mDriver.getTrackBank().getItemAt(x);
      final SettableBooleanValue mute = channel.mute();
      mute.toggle();
   }

   @Override
   public void paint()
   {
      super.paint();

      for (int x = 0; x < 8; ++x)
      {
         final boolean isMuted = mDriver.getTrackBank().getItemAt(x).mute().get();
         final boolean exists = mDriver.getTrackBank().getItemAt(x).exists().get();
         mDriver.getPadButton(x, 0).setColor(!exists ? Color.OFF : isMuted ? Color.MUTE : Color.MUTE_LOW);
      }
   }

   @Override
   public void paintModeButton()
   {
      final Button button = mDriver.getBottomButton(2);
      button.setColor(isActive() ? Color.MUTE : Color.MUTE_LOW);
   }
}
