package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
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
            bindLightState(() -> computeGridLedState(slot, button), button.getLight());
         }

         final Scene scene = sceneBank.getItemAt(7 - x);
         final Button sceneButton = driver.getSceneButton(x);
         bindPressed(sceneButton, scene.launchAction());
         bindLightState(() -> computeSceneLedState(scene), sceneButton.getLight());
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

   private InternalHardwareLightState computeSceneLedState(final Scene scene)
   {
      Color color;
      if (scene.exists().get())
      {
         color = new Color(scene.color());
         if (color.isBlack())
            color = Color.SCENE;
      }
      else
         color = Color.OFF;

      return new LedState(color);
   }

   private InternalHardwareLightState computeGridLedState(final ClipLauncherSlot slot, final Button button)
   {
      final Color color = new Color(slot.color());
      final int pulse;

      if (slot.isStopQueued().get())
         pulse = Button.PULSE_STOP_QUEUED;
      else if (slot.isRecordingQueued().get())
         pulse = Button.PULSE_RECORDING_QUEUED;
      else if (slot.isPlaybackQueued().get())
         pulse = Button.PULSE_PLAYBACK_QUEUED;
      else if (slot.isRecording().get())
         pulse = Button.PULSE_RECORDING;
      else if (slot.isPlaying().get())
         pulse = Button.PULSE_PLAYING;
      else
         pulse = Button.NO_PULSE;

      return new LedState(color, pulse);
   }

   @Override
   protected String getModeDescription()
   {
      return "Clip Launcher";
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
      mShiftLayer.deactivate();
      mQuantizeLayer.deactivate();
      mDeleteLayer.deactivate();

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

   final private LaunchpadLayer mShiftLayer;
   final private LaunchpadLayer mDeleteLayer;
   final private LaunchpadLayer mQuantizeLayer;
}
