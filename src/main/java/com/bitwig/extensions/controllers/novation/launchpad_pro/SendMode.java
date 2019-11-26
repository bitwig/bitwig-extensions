package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class SendMode extends Mode
{
   SendMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "send");
   }

   @Override
   protected String getModeDescription()
   {
      return "Sends";
   }

   @Override
   public void onSceneButtonPressed(final int column)
   {
      setSendIndex(7 - column);
   }

   private void setSendIndex(int index)
   {
      if (index == mSendIndex)
         return;

      mSendIndex = index;
      updateIndications();
   }

   private void updateIndications()
   {
      final TrackBank trackBank = mDriver.getTrackBank();
      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         sendBank.subscribe();

         for (int j = 0; j < 8; ++j)
            sendBank.getItemAt(j).setIndication(j == mSendIndex);
      }
   }

   @Override
   public void onPadPressed(final int x, final int y, final int velocity)
   {
      mDriver.getTrackBank().getItemAt(x).sendBank().getItemAt(mSendIndex).value().setRaw(y / 7.0);
   }

   @Override
   protected void doActivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.subscribe();

         final SendBank sendBank = track.sendBank();
         sendBank.subscribe();
         for (int j = 0; j < 8; ++j)
         {
            final Send send = sendBank.getItemAt(j);
            send.subscribe();
            send.sendChannelColor().subscribe();
            send.value().subscribe();
         }
      }

      updateIndications();
   }

   @Override
   protected void doDeactivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.unsubscribe();

         final SendBank sendBank = track.sendBank();
         sendBank.unsubscribe();
         for (int j = 0; j < 8; ++j)
         {
            final Send send = sendBank.getItemAt(j);
            send.unsubscribe();
            send.sendChannelColor().unsubscribe();
            send.value().unsubscribe();
         }
      }

      super.deactivate();
   }

   @Override
   public void paint()
   {
      super.paint();

      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Send send = trackBank.getItemAt(i).sendBank().getItemAt(mSendIndex);
         final double value = send.value().get();

         final Color colorOn = mDriver.getTrackColor(i);

         for (int y = 0; y < 8; ++y)
         {
            final Button button = mDriver.getPadButton(i, y);
            if (value >= y / 7.0)
               button.setColor(colorOn);
            else
               button.clear();
         }
      }

      for (int i = 0; i < 8; ++i)
      {
         final SendBank sendBank = trackBank.getItemAt(i).sendBank();
         final Send send = sendBank.getItemAt(i);
         if (!send.exists().get())
            mDriver.getButtonOnTheRight(7 - i).clear();
         else
         {
            Color sendColor = new Color(send.sendChannelColor());

            if (sendColor.equals(Color.OFF))
               sendColor = Color.WHITE;

            final Color sendColorLow = new Color(sendColor, 0.1f);
            mDriver.getButtonOnTheRight(7 - i).setColor(i == mSendIndex ? sendColor : sendColorLow);
         }
      }

      mDriver.getButtonOnTheTop(0).setColor(Color.OFF);
      mDriver.getButtonOnTheTop(1).setColor(Color.OFF);
      mDriver.getButtonOnTheTop(2).setColor(trackBank.canScrollChannelsUp().get() ? Color.TRACK : Color.TRACK_LOW);
      mDriver.getButtonOnTheTop(3).setColor(trackBank.canScrollChannelsDown().get() ? Color.TRACK : Color.TRACK_LOW);
   }

   @Override
   public void paintModeButton()
   {
      final Button button = mDriver.getButtonOnTheBottom(6);
      button.setColor(isActive() ? Color.SENDS : Color.SENDS_LOW);
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

   private int mSendIndex = 0;
}
