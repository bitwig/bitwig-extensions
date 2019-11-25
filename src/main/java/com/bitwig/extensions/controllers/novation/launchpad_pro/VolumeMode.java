package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class VolumeMode extends Mode
{
   public VolumeMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "volume");
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
   void deactivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.subscribe();
         track.volume().unsubscribe();
         track.volume().setIndication(false);
      }

      super.deactivate();
   }

   @Override
   public void paint()
   {
      super.paint();
      paintArrows();
      paintVolumeBars();
   }

   private void paintVolumeBars()
   {
      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final double value = trackBank.getItemAt(i).volume().value().getRaw();

         final Color colorOn = mDriver.getTrackColor(i);

         for (int y = 0; y < 8; ++y)
         {
            final Led led = mDriver.getPadLed(i, y);
            if (value >= y / 7.0)
               led.setColor(colorOn);
            else
               led.clear();
         }

         mDriver.getRightLed(i).clear();
      }
   }

   private void paintArrows()
   {
      final TrackBank trackBank = mDriver.getTrackBank();
      mDriver.getTopLed(0).setColor(Color.OFF);
      mDriver.getTopLed(1).setColor(Color.OFF);
      mDriver.getTopLed(2).setColor(trackBank.canScrollChannelsUp().get() ? Color.TRACK : Color.TRACK_LOW);
      mDriver.getTopLed(3).setColor(trackBank.canScrollChannelsDown().get() ? Color.TRACK : Color.TRACK_LOW);
   }

   @Override
   public void paintModeButton()
   {
      final Led led = mDriver.getBottomLed(4);
      led.setColor(mIsActive ? Color.VOLUME : Color.VOLUME_LOW);
   }

   @Override
   public void onArrowLeftPressed()
   {
      if (mDriver.isShiftOn())
         mDriver.getTrackBank().scrollPageBackwards();
      else
         mDriver.getTrackBank().scrollBackwards();
   }

   @Override
   public void onArrowRightPressed()
   {
      if (mDriver.isShiftOn())
         mDriver.getTrackBank().scrollPageForwards();
      else
         mDriver.getTrackBank().scrollForwards();
   }
}
