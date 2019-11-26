package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.SoloValue;
import com.bitwig.extension.controller.api.Track;

public class SoloOverlay extends Overlay
{
   public SoloOverlay(final LaunchpadProControllerExtension launchpadProControllerExtension)
   {
      super(launchpadProControllerExtension, "solo");
   }

   @Override
   public void onPadPressed(final int x, final int velocity)
   {
      final Track channel = mDriver.getTrackBank().getItemAt(x);
      final SoloValue solo = channel.solo();
      solo.toggle(mDriver.isShiftOn());
   }

   @Override
   public void paint()
   {
      super.paint();

      for (int x = 0; x < 8; ++x)
      {
         final boolean isSolo = mDriver.getTrackBank().getItemAt(x).solo().get();
         final boolean exists = mDriver.getTrackBank().getItemAt(x).exists().get();
         mDriver.getPadButton(x, 0).setColor(!exists ? Color.OFF : isSolo ? Color.SOLO : Color.SOLO_LOW);
      }
   }

   @Override
   public void paintModeButton()
   {
      final Button button = mDriver.getBottomButton(3);
      button.setColor(isActive() ? Color.SOLO : Color.SOLO_LOW);
   }
}
