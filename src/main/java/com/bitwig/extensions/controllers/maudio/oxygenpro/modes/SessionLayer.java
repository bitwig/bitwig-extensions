package com.bitwig.extensions.controllers.maudio.oxygenpro.modes;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.maudio.oxygenpro.*;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.PadButton;
import com.bitwig.extensions.controllers.maudio.oxygenpro.definition.BasicMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SessionLayer extends Layer {

   private final RgbColor[] slotColors = new RgbColor[16];
   private final SceneBank sceneBank;
   private final CursorTrack cursorTrack;
   private boolean overdubEnabled;
   private RgbColor trackColor = RgbColor.OFF;
   private final int numberOfTracks;
   private ModeHandler modeHandler;
   private boolean backButtonHeld = false;
   private final boolean hasSceneLaunchButtons;

   public SessionLayer(Layers layers, HwElements hwElements, ViewControl viewControl, Transport transport,
                       OxyConfig config) {
      super(layers, "SESSION_LAYER");
      Arrays.fill(slotColors, RgbColor.OFF);

      this.hasSceneLaunchButtons = config.hasSceneLaunchButtons();
      transport.isClipLauncherOverdubEnabled().addValueObserver(overdubEnabled -> this.overdubEnabled = overdubEnabled);
      TrackBank trackBank = viewControl.getMixerTrackBank();
      List<PadButton> gridButtons = hwElements.getPadButtons();
      trackBank.setShouldShowClipLauncherFeedback(true);
      this.numberOfTracks = trackBank.getSizeOfBank();
      this.cursorTrack = viewControl.getCursorTrack();
      for (int tInd = 0; tInd < numberOfTracks; tInd++) {
         final int trackIndex = tInd;
         Track track = trackBank.getItemAt(tInd);
         prepareTrack(track);
         for (int sInd = 0; sInd < 2; sInd++) {
            final int sceneIndex = sInd;
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sInd);
            int buttonIndex = sceneIndex * numberOfTracks + trackIndex;
            prepareSlot(slot, buttonIndex);
            PadButton button = gridButtons.get(buttonIndex);
            button.bindLight(this, () -> this.getRgbState(track, slot, trackIndex, sceneIndex));
            button.bindPressed(this, () -> this.handlePress(slot));
            button.bindRelease(this, () -> this.handleRelease(slot));
         }
      }
      sceneBank = trackBank.sceneBank();
      hwElements.getButton(OxygenCcAssignments.SCENE_LAUNCH1)
         .bindPressed(this, () -> launchScene(sceneBank.getScene(0)));
      hwElements.getButton(OxygenCcAssignments.SCENE_LAUNCH1)
         .bindRelease(this, () -> releaseScene(sceneBank.getScene(0)));
      hwElements.getButton(OxygenCcAssignments.SCENE_LAUNCH2)
         .bindPressed(this, () -> launchScene(sceneBank.getScene(1)));
      hwElements.getButton(OxygenCcAssignments.SCENE_LAUNCH2)
         .bindRelease(this, () -> releaseScene(sceneBank.getScene(1)));
      hwElements.getButton(OxygenCcAssignments.ENCODER_PUSH).bindPressed(this, this::handleEncoderDown);
   }

   public void setBackButtonHeld(boolean isPressed) {
      backButtonHeld = isPressed;
   }

   public void registerModeHandler(ModeHandler modeHandler) {
      this.modeHandler = modeHandler;
   }

   private void handleEncoderDown() {
      if (this.modeHandler != null && isActive()) {
         modeHandler.changeMode(BasicMode.NOTES);
      }
   }

   public void handleBankRight() {
      if (!hasSceneLaunchButtons) {
         launchScene(sceneBank.getScene(0));

      }
   }

   public void handleBankLeft() {
      if (!hasSceneLaunchButtons) {
         launchScene(sceneBank.getScene(1));
      }
   }

   private void launchScene(Scene scene) {
      scene.launch();
   }

   private void releaseScene(Scene scene) {
      scene.launchRelease();
   }

   public void handleEncoder(int dir) {
      if (backButtonHeld) {
         if (dir < 0) {
            sceneBank.scrollBackwards();
         } else {
            sceneBank.scrollForwards();
         }
      } else {
         if (dir < 0) {
            //sceneBank.scrollBackwards();
            cursorTrack.selectPrevious();
         } else {
            //sceneBank.scrollForwards();
            cursorTrack.selectNext();
         }
      }
   }

   private void prepareTrack(Track track) {
      track.isQueuedForStop().markInterested();
      track.arm().markInterested();
      track.color().addValueObserver((r, g, b) -> trackColor = RgbColor.toColor(r, g, b));
   }

   private void handlePress(ClipLauncherSlot slot) {
      if (backButtonHeld) {
         slot.launchAlt();
      } else {
         slot.launch();
      }
   }

   private void handleRelease(ClipLauncherSlot slot) {
      if (backButtonHeld) {
         slot.launchReleaseAlt();
      } else {
         slot.launchRelease();
      }
   }

   private InternalHardwareLightState getRgbState(Track track, ClipLauncherSlot slot, int trackIndex, int sceneIndex) {
      if (slot.hasContent().get()) {
         int buttonIndex = sceneIndex * numberOfTracks + trackIndex;
         RgbColor color = slotColors[buttonIndex];
         if (slot.isRecordingQueued().get()) {
            return RgbColor.RED.getBlink();
         } else if (slot.isRecording().get()) {
            return RgbColor.RED.getBlink();
         } else if (slot.isPlaybackQueued().get()) {
            return color.getBlink();
         } else if (slot.isStopQueued().get()) {
            return color.getBlink(); //RgbState.flash(color, 1);
         } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
            return RgbColor.GREEN.getBlink();
         } else if (slot.isPlaying().get()) {
            if (track.arm().get() && overdubEnabled) {
               return RgbColor.RED.getBlink();
            }
            return RgbColor.GREEN;
         }
         return color;
      }

      if (slot.isRecordingQueued().get()) {
         return RgbColor.RED.getBlink();
      } else if (track.arm().get()) {
         return RgbColor.RED;
      } else if (slot.isStopQueued().get()) {
         return RgbColor.WHITE.getBlink();
      }

      return RgbColor.OFF;
   }

   @Activate
   public void doActivate() {
      this.setIsActive(true);
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
