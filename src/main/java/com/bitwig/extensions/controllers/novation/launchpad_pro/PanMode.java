package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class PanMode extends Mode
{
   public PanMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "pan");

      mShiftLayer = new LaunchpadLayer(driver, "pan-shift");

      final TrackBank trackBank = driver.getTrackBank();
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

         final Button sceneButton = driver.getSceneButton(7 - y);
         bindPressed(sceneButton, () -> pan.setRaw(0));
         bindLightState(() -> {
            if (!track.exists().get())
               return LedState.OFF;
            if (pan.get() == 0)
               return new LedState(track.color());
            return new LedState(Color.scale(new Color(track.color()), .2f));
         }, sceneButton.getLight());
      }

      bindLightState(LedState.PAN_MODE, driver.getPanButton());

      bindLightState(() -> trackBank.canScrollForwards().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.getRightButton());
      bindLightState(() -> trackBank.canScrollBackwards().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.getLeftButton());
      bindLightState(() -> LedState.OFF, driver.getDownButton());
      bindLightState(() -> LedState.OFF, driver.getUpButton());

      bindPressed(driver.getRightButton(), trackBank.scrollForwardsAction());
      bindPressed(driver.getLeftButton(), trackBank.scrollBackwardsAction());
      mShiftLayer.bindPressed(driver.getRightButton(), trackBank.scrollPageForwardsAction());
      mShiftLayer.bindPressed(driver.getLeftButton(), trackBank.scrollPageBackwardsAction());
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
      final TrackBank trackBank = mDriver.getTrackBank();
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

      final TrackBank trackBank = mDriver.getTrackBank();

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
