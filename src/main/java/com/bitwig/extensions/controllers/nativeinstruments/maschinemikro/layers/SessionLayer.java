package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.commons.Colors;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbColor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SessionLayer extends Layer {
   private final RgbColor[] slotColors = new RgbColor[16];
   private final MidiProcessor midiProcessor;
   private final ModifierLayer modifierLayer;
   private SceneBank sceneBank;
   private boolean overdubEnabled;
   private RgbColor trackColor = RgbColor.OFF;

   public SessionLayer(Layers layers, HwElements hwElements, ViewControl viewControl, Transport transport,
                       MidiProcessor midiProcessor, ModifierLayer modifierLayer) {
      super(layers, "SESSION_LAYER");
      Arrays.fill(slotColors, RgbColor.OFF);
      this.midiProcessor = midiProcessor;
      this.modifierLayer = modifierLayer;
      transport.isClipLauncherOverdubEnabled().addValueObserver(overdubEnabled -> this.overdubEnabled = overdubEnabled);
      TrackBank trackBank = viewControl.getMixerTrackBank();
      sceneBank = trackBank.sceneBank();
      List<RgbButton> gridButtons = hwElements.getPadButtons();
      trackBank.setShouldShowClipLauncherFeedback(true);
      for (int i = 0; i < 4; i++) {
         final int trackIndex = i;
         Track track = trackBank.getItemAt(trackIndex);
         prepareTrack(track);
         for (int j = 0; j < 4; j++) {
            final int sceneIndex = j;
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
            int buttonIndex = sceneIndex * 4 + trackIndex;
            prepareSlot(slot, buttonIndex);
            RgbButton button = gridButtons.get(buttonIndex);
            button.bindLight(this, () -> this.getRgbState(track, slot, trackIndex, sceneIndex));
            button.bindPressed(this, () -> this.handlePress(track, slot, trackIndex, sceneIndex));
            button.bindRelease(this, () -> this.handleRelease(track, slot, trackIndex, sceneIndex));
         }
      }
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(trackBank, sceneBank, dir));
   }

   private void handleEncoder(TrackBank trackBank, SceneBank sceneBank, int dir) {
      if (modifierLayer.getShiftHeld().get()) {
         if (dir < 0) {
            trackBank.scrollBackwards();
         } else {
            trackBank.scrollForwards();
         }
      } else {
         if (dir < 0) {
            sceneBank.scrollBackwards();
         } else {
            sceneBank.scrollForwards();
         }
      }
   }

   private void handlePress(Track track, ClipLauncherSlot slot, int trackIndex, int sceneIndex) {
      if (modifierLayer.getEraseHeld().get()) {
         slot.deleteObject();
      } else if (modifierLayer.getDuplicateHeld().get()) {
         slot.duplicateClip();
      } else if (modifierLayer.getVariationHeld().get()) {
         slot.launchAlt();
      } else {
         slot.launch();
      }
   }

   private void handleRelease(Track track, ClipLauncherSlot slot, int trackIndex, int sceneIndex) {
      if (modifierLayer.getVariationHeld().get()) {
         slot.launchReleaseAlt();
      } else {
         slot.launchRelease();
      }
   }

   private InternalHardwareLightState getRgbState(Track track, ClipLauncherSlot slot, int trackIndex, int sceneIndex) {
      if (slot.hasContent().get()) {
         int buttonIndex = sceneIndex * 4 + trackIndex;
         RgbColor color = slotColors[buttonIndex];
         if (slot.isRecordingQueued().get()) {
            return midiProcessor.blinkMid(Colors.RED);
         } else if (slot.isRecording().get()) {
            return midiProcessor.blinkMid(Colors.RED);
         } else if (slot.isPlaybackQueued().get()) {
            return midiProcessor.blinkMid(color);
         } else if (slot.isStopQueued().get()) {
            return color.brightness(ColorBrightness.DIMMED); //RgbState.flash(color, 1);
         } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
            return RgbColor.GREEN.brightness(ColorBrightness.BRIGHT);
         } else if (slot.isPlaying().get()) {
            if (track.arm().get() && overdubEnabled) {
               return RgbColor.RED.brightness(ColorBrightness.BRIGHT);
            }
            return RgbColor.GREEN.brightness(ColorBrightness.BRIGHT);
         }
         return color.brightness(ColorBrightness.DIMMED);
      }

      if (slot.isRecordingQueued().get()) {
         return midiProcessor.blinkMid(Colors.RED);
      } else if (track.arm().get()) {
         return RgbColor.RED.brightness(ColorBrightness.DIMMED);
      } else if (slot.isStopQueued().get()) {
         return RgbColor.WHITE.brightness(ColorBrightness.DIMMED);
      }

      return RgbColor.OFF;
   }

   @Activate
   public void onContextActivate() {
      this.setIsActive(true);
   }

   private void prepareTrack(Track track) {
      track.isQueuedForStop().markInterested();
      track.arm().markInterested();
      track.color().addValueObserver((r, g, b) -> RgbColor.toColor(r, g, b));
   }

   private void prepareSlot(final ClipLauncherSlot slot, final int buttonIndex) {
      slot.hasContent().markInterested();
      slot.isPlaying().markInterested();
      slot.isStopQueued().markInterested();
      slot.isRecordingQueued().markInterested();
      slot.isRecording().markInterested();
      slot.isPlaybackQueued().markInterested();
      slot.color().addValueObserver((r, g, b) -> {
         slotColors[buttonIndex] = RgbColor.toColor(r, g, b);
      });
   }

}
