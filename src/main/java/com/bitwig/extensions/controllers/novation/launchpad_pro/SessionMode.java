package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ClipLauncherSlotOrSceneBank;
import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public final class SessionMode extends Mode
{
   private final Color SESSION_ON_COLOR = new Color(1.f, 1.f, 0.f);
   private final Color SESSION_OFF_COLOR = new Color(SESSION_ON_COLOR, 0.1f);

   public SessionMode(final LaunchpadProControllerExtension launchpadProControllerExtension)
   {
      super(launchpadProControllerExtension, "session");
   }

   @Override
   protected String getModeDescription()
   {
      return "Clip Launcher";
   }

   @Override
   public void paintModeButton()
   {
      final Led led = mDriver.getTopLed(4);
      led.setColor(isActive() ? SESSION_ON_COLOR : SESSION_OFF_COLOR);
   }

   @Override
   public void onPadPressed(final int x, final int y, final int velocity)
   {
      final Track channel = mDriver.getTrackBank().getItemAt(x);
      final ClipLauncherSlotBank clipLauncherSlots = channel.clipLauncherSlotBank();
      final int slotIndex = 7 - y;

      if (mDriver.isShiftOn())
         clipLauncherSlots.select(slotIndex);
      else if (mDriver.isDeleteOn())
         clipLauncherSlots.getItemAt(slotIndex).deleteObject();
      else if (mDriver.isQuantizeOn())
      {
         clipLauncherSlots.select(slotIndex);
         mDriver.getCursorClip().quantize(1);
      }
      else
         clipLauncherSlots.launch(slotIndex);
   }

   @Override
   public void onSceneButtonPressed(final int column)
   {
      final ClipLauncherSlotOrSceneBank clipLauncherScenes = mDriver.getTrackBank().sceneBank();
      clipLauncherScenes.launch(7 - column);
   }

   @Override
   public void doActivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();
      final SceneBank sceneBank = mDriver.getSceneBank();
      sceneBank.setIndication(true);

      for (int i = 0; i < 8; ++i)
      {
         final Track track = trackBank.getItemAt(i);
         track.subscribe();

         final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
         slotBank.setIndication(true);

         for (int j = 0; j < 8; ++j)
         {
            final ClipLauncherSlot slot = slotBank.getItemAt(j);

            slot.subscribe();
            slot.color().subscribe();
            slot.isSelected().subscribe();
            slot.isPlaying().subscribe();
            slot.isPlaybackQueued().subscribe();
            slot.isRecording().subscribe();
            slot.isRecordingQueued().subscribe();
            slot.hasContent().subscribe();
         }

         final Scene scene = sceneBank.getItemAt(i);
         scene.color().subscribe();
         scene.exists().subscribe();
         scene.subscribe();
      }
   }

   @Override
   public void deactivate()
   {
      final TrackBank trackBank = mDriver.getTrackBank();
      final SceneBank sceneBank = mDriver.getSceneBank();
      sceneBank.setIndication(false);

      for (int i = 0; i < 8; ++i)
      {
         final Track channel = trackBank.getItemAt(i);
         channel.unsubscribe();

         final ClipLauncherSlotBank slotBank = channel.clipLauncherSlotBank();
         slotBank.setIndication(false);

         for (int j = 0; j < 8; ++j)
         {
            final ClipLauncherSlot slot = slotBank.getItemAt(j);

            slot.color().unsubscribe();
            slot.isSelected().unsubscribe();
            slot.isPlaying().unsubscribe();
            slot.isPlaybackQueued().unsubscribe();
            slot.isRecording().unsubscribe();
            slot.isRecordingQueued().unsubscribe();
            slot.hasContent().unsubscribe();
            slot.unsubscribe();
         }

         final Scene scene = sceneBank.getItemAt(i);
         scene.color().unsubscribe();
         scene.exists().unsubscribe();
         scene.unsubscribe();
      }

      super.deactivate();
   }

   @Override
   public void paint()
   {
      super.paint();

      paintSlots();
      paintSceneButtons();
      paintArrows();
   }

   private void paintArrows()
   {
      final TrackBank trackBank = mDriver.getTrackBank();
      mDriver.getTopLed(0).setColor(trackBank.sceneBank().canScrollBackwards().get() ? Color.SCENE : Color.SCENE_LOW);
      mDriver.getTopLed(1).setColor(trackBank.sceneBank().canScrollForwards().get() ? Color.SCENE : Color.SCENE_LOW);
      mDriver.getTopLed(2).setColor(trackBank.canScrollChannelsUp().get() ? Color.TRACK : Color.TRACK_LOW);
      mDriver.getTopLed(3).setColor(trackBank.canScrollChannelsDown().get() ? Color.TRACK : Color.TRACK_LOW);
   }

   private void paintSlots()
   {
      final TrackBank trackBank = mDriver.getTrackBank();

      for (int i = 0; i < 8; ++i)
      {
         final Track channel = trackBank.getItemAt(i);

         for (int j = 0; j < 8; ++j)
         {
            final ClipLauncherSlotBank slotBank = channel.clipLauncherSlotBank();
            final ClipLauncherSlot slot = slotBank.getItemAt(7 - j);
            final ColorValue slotColor = slot.color();

            final Led padLed = mDriver.getPadLed(i, j);
            padLed.setColor(slotColor.red(), slotColor.green(), slotColor.blue());

            if (slot.isStopQueued().get())
               padLed.setPulse(padLed.PULSE_STOP_QUEUED);
            else if (slot.isRecordingQueued().get())
               padLed.setPulse(padLed.PULSE_RECORDING_QUEUED);
            else if (slot.isPlaybackQueued().get())
               padLed.setPulse(padLed.PULSE_PLAYBACK_QUEUED);
            else if (slot.isRecording().get())
               padLed.setPulse(padLed.PULSE_RECORDING);
            else if (slot.isPlaying().get())
               padLed.setPulse(padLed.PULSE_PLAYING);
            else
               padLed.setPulse(Led.NO_PULSE);
         }
      }
   }

   private void paintSceneButtons()
   {
      final SceneBank sceneBank = mDriver.getSceneBank();

      for (int i = 0; i < 8; ++i)
      {
         final Scene scene = sceneBank.getItemAt(i);
         final Led led = mDriver.getRightLed(7 - i);
         if (scene.exists().get())
         {
            Color sceneColor = new Color(scene.color());
            if (sceneColor.isBlack())
               sceneColor = Color.SCENE;
            led.setColor(sceneColor);
         }
         else
            led.setColor(Color.OFF);
      }
   }

   @Override
   public void onArrowDownPressed()
   {
      if (mDriver.isShiftOn())
         mDriver.getTrackBank().sceneBank().scrollPageForwards();
      else
         mDriver.getTrackBank().sceneBank().scrollForwards();
   }

   @Override
   public void onArrowUpPressed()
   {
      if (mDriver.isShiftOn())
         mDriver.getTrackBank().sceneBank().scrollPageBackwards();
      else
         mDriver.getTrackBank().sceneBank().scrollBackwards();
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
