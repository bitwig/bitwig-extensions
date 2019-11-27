package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class VolumeMode extends Mode
{
   public VolumeMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "volume");

      mShiftLayer = new LaunchpadLayer(driver, "volume-shift");

      final TrackBank trackBank = driver.getTrackBank();
      for (int x = 0; x < 8; ++x)
      {
         final Track track = trackBank.getItemAt(x);
         final Parameter volume = track.volume();
         for (int y = 0; y < 8; ++y)
         {
            final double padValue = y / 7.0;
            final Button button = driver.getPadButton(x, y);
            bindPressed(button, () -> volume.setImmediately(padValue));
            bindLightState(() -> {
               final double value = volume.get();
               if (!track.exists().get())
                  return LedState.OFF;
               if (value >= padValue)
                  return new LedState(track.color());
               return LedState.OFF;
            }, button.getLight());
         }
      }

      bindLightState(LedState.VOLUME_MODE, driver.getPanButton());

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
      return "Volume";
   }

   @Override
   public void onPadPressed(final int x, final int y, final int velocity)
   {
      mDriver.getTrackBank().getItemAt(x).volume().value().setRaw(y / 7.0);
   }

   @Override
   protected void doActivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.subscribe();
         track.volume().subscribe();
         track.volume().setIndication(true);
      }

      mDriver.getNoteInput().setKeyTranslationTable(LaunchpadProControllerExtension.FILTER_ALL_NOTE_MAP);
   }

   @Override
   protected void doDeactivate()
   {
      mShiftLayer.deactivate();
      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.subscribe();
         track.volume().unsubscribe();
         track.volume().setIndication(false);
      }
   }

   private final LaunchpadLayer mShiftLayer;
}
