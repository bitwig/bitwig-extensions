package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ClipLauncherSlotOrSceneBank;
import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public final class SessionMode extends Mode
{
   public SessionMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "session");

      mShiftLayer = new LaunchpadLayer(driver, "session-shift");
      mDeleteLayer = new LaunchpadLayer(driver, "session-delete");
      mQuantizeLayer = new LaunchpadLayer(driver, "session-quantize");

      final TrackBank trackBank = driver.getTrackBank();
      final SceneBank sceneBank = trackBank.sceneBank();
      for (int x = 0; x < 8; ++x)
      {
         final Track track = trackBank.getItemAt(x);
         final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
         for (int y = 0; y < 8; ++y)
         {
            final ClipLauncherSlot slot = slotBank.getItemAt(7 - y);
            final Button button = driver.getPadButton(x, y);
            bindPressed(button, slot.launchAction());
            mShiftLayer.bindPressed(button, slot.selectAction());
            mDeleteLayer.bindPressed(button, slot::deleteObject);
            mQuantizeLayer.bindReleased(button, () -> {
               slot.select();
               mDriver.getCursorClip().quantize(1);
            });
            bindLightState(() -> computeGridLightState(slot, button), button.getLight());
         }

         final Scene scene = sceneBank.getItemAt(7 - x);
         final Button sceneButton = driver.getSceneButton(x);
         bindPressed(sceneButton, scene.launchAction());
         bindLightState(() -> new LedState(scene.color()), sceneButton.getLight());
      }

      bindLayer(driver.getShiftButton(), mShiftLayer);
      bindLayer(driver.getDeleteButton(), mDeleteLayer);
      bindLayer(driver.getQuantizeButton(), mQuantizeLayer);

      bindLightState(LedState.SESSION_MODE_ON, driver.getSessionButton());
      bindLightState(() -> trackBank.canScrollForwards().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.getRightButton());
      bindLightState(() -> trackBank.canScrollBackwards().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.getLeftButton());
      bindLightState(() -> sceneBank.canScrollForwards().get() ? LedState.SCENE : LedState.SCENE_LOW, driver.getDownButton());
      bindLightState(() -> sceneBank.canScrollBackwards().get() ? LedState.SCENE : LedState.SCENE_LOW, driver.getUpButton());

      bindPressed(driver.getRightButton(), trackBank.scrollForwardsAction());
      bindPressed(driver.getLeftButton(), trackBank.scrollBackwardsAction());
      bindPressed(driver.getUpButton(), sceneBank.scrollBackwardsAction());
      bindPressed(driver.getDownButton(), sceneBank.scrollForwardsAction());

      mShiftLayer.bindPressed(driver.getRightButton(), trackBank.scrollPageForwardsAction());
      mShiftLayer.bindPressed(driver.getLeftButton(), trackBank.scrollPageBackwardsAction());
      mShiftLayer.bindPressed(driver.getUpButton(), sceneBank.scrollPageBackwardsAction());
      mShiftLayer.bindPressed(driver.getDownButton(), sceneBank.scrollPageForwardsAction());
   }

   private InternalHardwareLightState computeGridLightState(final ClipLauncherSlot slot, final Button button)
   {
      final Color color = new Color(slot.color());
      final int pulse;

      if (slot.isStopQueued().get())
         pulse = button.PULSE_STOP_QUEUED;
      else if (slot.isRecordingQueued().get())
         pulse = button.PULSE_RECORDING_QUEUED;
      else if (slot.isPlaybackQueued().get())
         pulse = button.PULSE_PLAYBACK_QUEUED;
      else if (slot.isRecording().get())
         pulse = button.PULSE_RECORDING;
      else if (slot.isPlaying().get())
         pulse = button.PULSE_PLAYING;
      else
         pulse = button.NO_PULSE;

      return new LedState(color, pulse);
   }

   @Override
   protected String getModeDescription()
   {
      return "Clip Launcher";
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
   public void doDeactivate()
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
   }

   @Override
   public void paint()
   {
      if (false)
      {
         super.paint();

         paintSlots();
         paintSceneButtons();
         paintArrows();
      }
   }

   private void paintArrows()
   {
      final TrackBank trackBank = mDriver.getTrackBank();
      mDriver.getButtonOnTheTop(0).setColor(trackBank.sceneBank().canScrollBackwards().get() ? Color.SCENE : Color.SCENE_LOW);
      mDriver.getButtonOnTheTop(1).setColor(trackBank.sceneBank().canScrollForwards().get() ? Color.SCENE : Color.SCENE_LOW);
      mDriver.getButtonOnTheTop(2).setColor(trackBank.canScrollChannelsUp().get() ? Color.TRACK : Color.TRACK_LOW);
      mDriver.getButtonOnTheTop(3).setColor(trackBank.canScrollChannelsDown().get() ? Color.TRACK : Color.TRACK_LOW);
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

            final Button button = mDriver.getPadButton(i, j);
            button.setColor(slotColor.red(), slotColor.green(), slotColor.blue());

            if (slot.isStopQueued().get())
               button.setPulse(button.PULSE_STOP_QUEUED);
            else if (slot.isRecordingQueued().get())
               button.setPulse(button.PULSE_RECORDING_QUEUED);
            else if (slot.isPlaybackQueued().get())
               button.setPulse(button.PULSE_PLAYBACK_QUEUED);
            else if (slot.isRecording().get())
               button.setPulse(button.PULSE_RECORDING);
            else if (slot.isPlaying().get())
               button.setPulse(button.PULSE_PLAYING);
            else
               button.setPulse(button.NO_PULSE);
         }
      }
   }

   private void paintSceneButtons()
   {
      final SceneBank sceneBank = mDriver.getSceneBank();

      for (int i = 0; i < 8; ++i)
      {
         final Scene scene = sceneBank.getItemAt(i);
         final Button button = mDriver.getButtonOnTheRight(7 - i);
         if (scene.exists().get())
         {
            Color sceneColor = new Color(scene.color());
            if (sceneColor.isBlack())
               sceneColor = Color.SCENE;
            button.setColor(sceneColor);
         }
         else
            button.setColor(Color.OFF);
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

   final private LaunchpadLayer mShiftLayer;
   final private LaunchpadLayer mDeleteLayer;
   final private LaunchpadLayer mQuantizeLayer;
}
