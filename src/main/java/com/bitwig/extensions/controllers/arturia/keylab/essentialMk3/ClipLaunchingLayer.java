package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color.BlinkState;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color.RgbColor;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color.RgbLightState;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.HwElements;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.SysExHandler;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.ViewControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.Arrays;

public class ClipLaunchingLayer extends Layer {

   private final RgbColor[] sceneSlotColors = new RgbColor[8];
   private final long[] downTime = new long[4];
   private final TimedEvent[] holdEvent = new TimedEvent[4];
   private final Layer sceneLaunchingLayer; // clips in selected scene
   private final TrackBank trackBank;
   private long clipsStopTiming = 800;

   public ClipLaunchingLayer(final Layers layers, final ViewControl viewControl, final HwElements hwElements,
                             SysExHandler sysExHandler) {
      super(layers, "CLIP LAUNCHER");

      sceneLaunchingLayer = new Layer(layers, "PER_SCENE_LAUNCHER");
      sysExHandler.registerTickTask(this::notifyTick);
      Arrays.fill(sceneSlotColors, RgbColor.OFF);
      Arrays.fill(downTime, -1);

      final RgbButton[] buttons = hwElements.getPadBankAButtons();

      trackBank = viewControl.getViewTrackBank();
      trackBank.setShouldShowClipLauncherFeedback(true);

//        final RgbBankLightState.Handler bankLightHandler = new RgbBankLightState.Handler(
//                PadBank.BANK_A, buttons);

      setUpLaunching(buttons);
//        driver.getPadBank().addValueObserver(((oldValue, newValue) -> changePadBank(newValue)));
   }

   public void setClipStopTiming(final String timingValue) {
      switch (timingValue) {
         case "Fast":
            clipsStopTiming = 500;
            break;
         case "Medium":
            clipsStopTiming = 800;
            break;
         case "Standard":
            clipsStopTiming = 1000;
            break;
      }
   }

   public void notifyTick() {
      final long time = System.currentTimeMillis();
      for (int i = 0; i < 4; i++) {
         if (downTime[i] != -1 && (time - downTime[i]) > clipsStopTiming) {
            final Track track = trackBank.getItemAt(i);
            track.stop();
         }
      }
   }

   private void changePadBank(final PadBank newValue) {
      if (!isActive()) {
         return;
      }
      activateIndication(newValue == PadBank.BANK_A);
   }

   void activateIndication(final boolean indication) {
      for (int i = 0; i < 8; i++) {
         trackBank.sceneBank().setIndication(indication);
         trackBank.setShouldShowClipLauncherFeedback(indication);
      }
   }

   private void setUpLaunching(final RgbButton[] buttons) {
      for (int i = 0; i < 8; i++) {
         final int buttonIndex = i;
         final int trackIndex = i % 4;
         final int sceneIndex = i / 4;
         final Track track = trackBank.getItemAt(trackIndex);

         final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
         prepareSlot(slot);
         track.isQueuedForStop().markInterested();
         track.arm().markInterested();
         slot.color().addValueObserver((r, g, b) -> sceneSlotColors[buttonIndex] = RgbColor.getColor(r, g, b));
         final RgbButton button = buttons[i];
         button.bindIsPressed(sceneLaunchingLayer, pressed -> handleSlotSelected(buttonIndex, track, slot, pressed));
         button.bindLight(sceneLaunchingLayer, () -> getLightState(buttonIndex, track, slot));
      }
   }

   public void navigateScenes(final int direction) {
      trackBank.sceneBank().scrollBy(direction);
   }

   public void launchScene() {
      trackBank.sceneBank().getScene(0).launch();
   }

   private void prepareSlot(final ClipLauncherSlot cs) {
      cs.exists().markInterested();
      cs.hasContent().markInterested();
      cs.isPlaybackQueued().markInterested();
      cs.isPlaying().markInterested();
      cs.isRecording().markInterested();
      cs.isRecordingQueued().markInterested();
      cs.isSelected().markInterested();
      cs.isStopQueued().markInterested();
   }

   private void handleSlotSelected(final int buttonIndex, final Track track, final ClipLauncherSlot slot,
                                   final boolean pressed) {
      final int trackIndex = buttonIndex % 4;
      if (pressed) {
         downTime[trackIndex] = System.currentTimeMillis();
         slot.select();
         if (slot.isRecording().get()) {
            slot.launch();
         }
      } else {
         final long diff = System.currentTimeMillis() - downTime[trackIndex];
         if (!slot.isRecording().get()) {
            if (diff > clipsStopTiming) {
               track.stop();
            } else {
               slot.launch();
            }
         }
         downTime[trackIndex] = -1;
      }
   }

   RgbLightState getLightState(final int index, final Track track, final ClipLauncherSlot slot) {
      final RgbColor color = sceneSlotColors[index];
      if (slot.hasContent().get()) {
         if (slot.isRecordingQueued().get()) {
            return RgbColor.RED.getColorState(BlinkState.BLINK2); //  RgbState.flash(color, 5);
         } else if (slot.isRecording().get()) {
            return RgbColor.RED.getColorState(BlinkState.BLINK3); // RgbState.pulse(5);
         } else if (slot.isPlaybackQueued().get()) {
            return color.getColorState(BlinkState.BLINK3); // RgbState.flash(color, 0);
         } else if (slot.isStopQueued().get()) {
            return color.getColorState(BlinkState.BLINK2); //  RgbState.flash(color, 1);
         } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
            return color.getColorState(BlinkState.BLINK2); //RgbState.flash(color, 1);
         } else if (slot.isPlaying().get()) {
            return RgbColor.GREEN.getColorState();
//                if (clipLauncherOverdub.get() && track.arm().get()) {
//                    return RgbState.pulse(5);
//                } else {
//                    return RgbState.pulse(22);
//                }
         }
         return color.getColorState();
      }
      if (slot.isRecordingQueued().get()) {
         return RgbColor.RED.getColorState(BlinkState.BLINK2);
      } else if (track.arm().get()) {
         return RgbColor.RED.getColorState();
      }
      return RgbLightState.OFF;
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      sceneLaunchingLayer.activate();
      //activateIndication(driver.getPadBank().get() == PadBank.BANK_A);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      sceneLaunchingLayer.deactivate();
      activateIndication(false);
   }

}
