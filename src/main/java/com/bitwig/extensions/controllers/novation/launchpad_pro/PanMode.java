package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class PanMode extends Mode
{
   public PanMode(final LaunchpadProControllerExtension launchpadProControllerExtension)
   {
      super(launchpadProControllerExtension, "pan");
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
   protected void doDeactivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.unsubscribe();
         track.pan().unsubscribe();
         track.pan().setIndication(false);
      }
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

         final Button centerButton = mDriver.getButtonOnTheRight(7 - i);
         if (pan == 0)
            centerButton.setColor(colorOn);
         else
            centerButton.setColor(colorOff);

         for (int j = 0; j < 8; ++j) // pad iterator
         {
            final Button button = mDriver.getPadButton(j, 7 - i);
            final double padValue = padToPan(j);
            if ((pan < 0 && padValue < 0 && padValue >= pan) ||
                (pan > 0 && padValue > 0 && padValue <= pan))
               button.setColor(colorOn);
            else
               button.clear();
         }
      }
   }

   private void paintArrows()
   {
      final TrackBank trackBank = mDriver.getTrackBank();
      mDriver.getButtonOnTheTop(0).setColor(Color.OFF);
      mDriver.getButtonOnTheTop(1).setColor(Color.OFF);
      mDriver.getButtonOnTheTop(2).setColor(trackBank.canScrollChannelsUp().get() ? Color.TRACK : Color.TRACK_LOW);
      mDriver.getButtonOnTheTop(3).setColor(trackBank.canScrollChannelsDown().get() ? Color.TRACK : Color.TRACK_LOW);
   }

   @Override
   public void paintModeButton()
   {
      final Button button = mDriver.getButtonOnTheBottom(5);
      button.setColor(isActive() ? Color.PAN : Color.PAN_LOW);
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
