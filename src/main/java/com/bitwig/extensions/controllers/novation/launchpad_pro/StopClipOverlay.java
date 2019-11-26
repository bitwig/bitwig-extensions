package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Track;

class StopClipOverlay extends Overlay
{
   private static final Color NO_TRACK_COLOR = Color.fromRgb255(0, 0, 0);
   private static final Color STOP_CLIP_ON_COLOR = Color.fromRgb255(180, 180, 180);
   private static final Color STOP_CLIP_OFF_COLOR = Color.scale(STOP_CLIP_ON_COLOR, 0.3f);

   StopClipOverlay(final LaunchpadProControllerExtension launchpadProControllerExtension)
   {
      super(launchpadProControllerExtension, "stop-clip");
   }

   @Override
   public void onPadPressed(final int x, final int velocity)
   {
      final Track channel = mDriver.getTrackBank().getItemAt(x);
      channel.stop();
   }

   @Override
   public void paint()
   {
      super.paint();

      for (int x = 0; x < 8; ++x)
      {
         final boolean isPlaying = false;
         final boolean exists = mDriver.getTrackBank().getItemAt(x).exists().get();
         mDriver.getPadButton(x, 0).setColor(!exists ? NO_TRACK_COLOR :
            isPlaying ? STOP_CLIP_ON_COLOR : STOP_CLIP_OFF_COLOR);
      }
   }

   @Override
   public void paintModeButton()
   {
      final Button button = mDriver.getButtonOnTheBottom(7);
      button.setColor(isActive() ? STOP_CLIP_ON_COLOR : STOP_CLIP_OFF_COLOR);
   }
}
