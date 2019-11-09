package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class PanMode extends Mode
{
   public PanMode(final LaunchpadProControllerExtension launchpadProControllerExtension)
   {
      super(launchpadProControllerExtension);
   }

   @Override
   protected String getModeDescription()
   {
      return "Pan";
   }

   private final double padToPan(final int x)
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
      }
   }

   @Override
   void deactivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.unsubscribe();
         track.pan().unsubscribe();
         track.pan().setIndication(false);
      }

      super.deactivate();
   }

   @Override
   public void paint()
   {
      super.paint();

      paintArrows();

      for (int i = 0; i < 8; ++i) // track iterator
      {
         final Track track = mDriver.getTrackBank().getItemAt(i);
         final double pan = track.pan().value().getRaw();

         final Color colorOn = mDriver.getTrackColor(i);
         final Color colorOff = Color.scale(colorOn, 0.2f);

         final Led centerLed = mDriver.getRightLed(7 - i);
         if (pan == 0)
            centerLed.setColor(colorOn);
         else
            centerLed.setColor(colorOff);

         for (int j = 0; j < 8; ++j) // pad iterator
         {
            final Led led = mDriver.getPadLed(j, 7 - i);
            final double padValue = padToPan(j);
            if ((pan < 0 && padValue < 0 && padValue >= pan) ||
                (pan > 0 && padValue > 0 && padValue <= pan))
               led.setColor(colorOn);
            else
               led.clear();
         }
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
      final Led led = mDriver.getBottomLed(5);
      led.setColor(mIsActive ? Color.PAN : Color.PAN_LOW);
   }

   @Override
   public void onPadPressed(final int x, final int y, final int velocity)
   {
      mDriver.getTrackBank().getItemAt(7 - y).pan().value().setRaw(padToPan(x));
   }

   @Override
   public void onSceneButtonPressed(final int column)
   {
      mDriver.getTrackBank().getItemAt(7 - column).pan().reset();
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
