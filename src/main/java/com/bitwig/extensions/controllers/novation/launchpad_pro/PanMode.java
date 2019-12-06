package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

final class PanMode extends Mode
{
   public PanMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "pan");

      mShiftLayer = new LaunchpadLayer(driver, "pan-shift");

      final TrackBank trackBank = driver.mTrackBank;
      for (int y = 0; y < 8; ++y)
      {
         final Track track = trackBank.getItemAt(y);
         final Parameter pan = track.pan();
         for (int x = 0; x < 8; ++x)
         {
            final double padValue = padToPan(x);
            final Button button = driver.getPadButton(x, 7 - y);
            bindPressed(button, () -> pan.setRaw(padValue));
            bindLightState(() -> {
               final double value = pan.getRaw();
               if (!track.exists().get())
                  return LedState.OFF;
               if ((value < 0 && padValue < 0 && padValue >= value) ||
                  (value > 0 && padValue > 0 && padValue <= value))
                  return new LedState(track.color());
               return LedState.OFF;
            }, button.getLight());
         }

         final Button sceneButton = driver.mSceneButtons[7 - y];
         bindPressed(sceneButton, () -> pan.setRaw(0));
         bindLightState(() -> {
            if (!track.exists().get())
               return LedState.OFF;
            if (pan.get() == 0)
               return new LedState(track.color());
            return new LedState(Color.scale(new Color(track.color()), .2f));
         }, sceneButton.getLight());
      }

      bindLightState(LedState.PAN_MODE, driver.mPanButton);

      bindLightState(() -> trackBank.canScrollForwards().get() ? LedState.TRACK : LedState.TRACK_LOW,
         driver.mRightButton);
      bindLightState(() -> trackBank.canScrollBackwards().get() ? LedState.TRACK : LedState.TRACK_LOW,
         driver.mLeftButton);
      bindLightState(() -> LedState.OFF, driver.mDownButton);
      bindLightState(() -> LedState.OFF, driver.mUpButton);

      bindPressed(driver.mRightButton, trackBank.scrollForwardsAction());
      bindPressed(driver.mLeftButton, trackBank.scrollBackwardsAction());
      mShiftLayer.bindPressed(driver.mRightButton, trackBank.scrollPageForwardsAction());
      mShiftLayer.bindPressed(driver.mLeftButton, trackBank.scrollPageBackwardsAction());
   }

   @Override
   protected String getModeDescription()
   {
      return "Pan";
   }

   private double padToPan(final int x)
   {
      return x / 3.5 - 1.0;
   }

   @Override
   protected void doActivate()
   {
      final TrackBank trackBank = mDriver.mTrackBank;
      trackBank.subscribe();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.subscribe();
         track.pan().subscribe();
         track.pan().setIndication(true);
         track.color().subscribe();
      }
   }

   @Override
   protected void doDeactivate()
   {
      mShiftLayer.deactivate();

      final TrackBank trackBank = mDriver.mTrackBank;

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.pan().unsubscribe();
         track.pan().setIndication(false);
         track.color().unsubscribe();
         track.unsubscribe();
      }
   }

   private final LaunchpadLayer mShiftLayer;
}
