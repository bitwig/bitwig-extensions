package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

final class SendsMode extends Mode
{
   SendsMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "send");

      mShiftLayer = new LaunchpadLayer(driver, "volume-shift");

      final TrackBank trackBank = driver.getTrackBank();
      for (int x = 0; x < 8; ++x)
      {
         final Track track = trackBank.getItemAt(x);
         final SendBank sendBank = track.sendBank();
         for (int y = 0; y < 8; ++y)
         {
            final double padValue = y / 7.0;
            final Button button = driver.getPadButton(x, y);
            bindPressed(button, () -> sendBank.getItemAt(mSendIndex).value().setImmediately(padValue));
            bindLightState(() -> {
               final double value = sendBank.getItemAt(mSendIndex).value().get();
               if (!track.exists().get())
                  return LedState.OFF;
               if (value >= padValue)
                  return new LedState(track.color());
               return LedState.OFF;
            }, button.getLight());
         }
      }

      for (int y = 0; y < 8; ++y)
      {
         final int Y = y;
         final SendBank sendBank = trackBank.getItemAt(y).sendBank();
         final Send send = sendBank.getItemAt(y);
         final Button sceneButton = driver.getSceneButton(7 - y);
         bindLightState(() -> {
            if (!send.exists().get())
               return LedState.OFF;

            Color sendColor = new Color(send.sendChannelColor());

            if (sendColor.equals(Color.OFF))
               sendColor = Color.WHITE;
            final Color sendColorLow = new Color(sendColor, 0.1f);
            return new LedState(Y == mSendIndex ? sendColor : sendColorLow);
         }, sceneButton);
         bindPressed(sceneButton, () -> setSendIndex(Y));
      }

      bindLightState(LedState.SENDS_MODE, driver.getSendsButton());

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
      return "Sends";
   }

   private void setSendIndex(final int index)
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
   protected void doActivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.subscribe();
         track.color().subscribe();

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
      mShiftLayer.deactivate();

      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.unsubscribe();
         track.color().unsubscribe();

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

   private int mSendIndex = 0;
   private final LaunchpadLayer mShiftLayer;
}
