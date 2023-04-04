package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbNoteButton;
import com.bitwig.extensions.framework.Layer;

import java.util.HashSet;

public class SessionLayer extends Layer {

   private final ControllerHost host;

   private final int[] colorIndex = new int[16];
   private final boolean[] sceneQueuePlayback = new boolean[8];
   private boolean sceneLaunched = false;
   private final Layer launchLayer2;
   private final Layer muteLayer;
   private final Layer soloLayer;
   private final Layer stopLayer;
   private final Layer controlLayer;
   private final Layer shiftLayer;

   private Layer currentModeLayer;
   private Mode mode = Mode.LAUNCH;
   private final HashSet<Integer> heldSoloKeys = new HashSet<>();

   private enum Mode {
      LAUNCH,
      STOP,
      SOLO,
      MUTE,
      CONTROL
   }

   public SessionLayer(final LaunchkeyMk3Extension driver) {
      super(driver.getLayers(), "SESSION_LAYER");

      host = driver.getHost();
      final TrackBank trackBank = driver.getTrackBank();
      final SceneBank sceneBank = trackBank.sceneBank();
      final Scene targetScene = trackBank.sceneBank().getScene(0);
      targetScene.clipCount().markInterested();
      trackBank.setShouldShowClipLauncherFeedback(true);

      launchLayer2 = new Layer(driver.getLayers(), "LAUNCH_LAYER2");
      muteLayer = new Layer(driver.getLayers(), "MUTE_LAYER");
      soloLayer = new Layer(driver.getLayers(), "SOLO_LAYER");
      stopLayer = new Layer(driver.getLayers(), "STOP_LAYER");
      shiftLayer = new Layer(driver.getLayers(), "LAUNCH_SHIFT_LAYER");
      controlLayer = new Layer(driver.getLayers(), "CONTROL_LAYER");
      currentModeLayer = launchLayer2;

      final RgbNoteButton[] buttons = driver.getHwControl().getSessionButtons();

      sceneBank.canScrollBackwards().markInterested();
      sceneBank.canScrollForwards().markInterested();

      final RgbCcButton row2ModeButton = driver.getHwControl().getModeRow2Button();

      row2ModeButton.bindPressed(this, this::advanceLayer, this::getModeColor);
      for (int i = 0; i < 8; i++) {
         final Track track = trackBank.getItemAt(i);
         markTrack(track);
      }

      for (int i = 0; i < 16; i++) {
         final RgbNoteButton button = buttons[i];
         final int sceneIndex = i / 8;
         final int trackIndex = i % 8;
         final Track track = trackBank.getItemAt(trackIndex);
         final Layer triggerLayer = sceneIndex == 0 ? this : launchLayer2;
         final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
         prepareSlot(slot, i);
         if (sceneIndex == 0) {
            slot.isPlaybackQueued().addValueObserver(queued -> {
               sceneQueuePlayback[trackIndex] = queued;
               if (!hasPlayQueued()) {
                  sceneLaunched = false;
               }
            });
         }

         button.bindIsPressed(triggerLayer, pressed -> {
            if (pressed) {
               handleSlot(track, slot, trackIndex, sceneIndex);
            }
         }, () -> getState(track, slot, trackIndex, sceneIndex));

         if (sceneIndex == 1) {
            button.bindPressed(stopLayer, track::stop, () -> getStopState(trackIndex, track));
            button.bindPressed(muteLayer, () -> track.mute().toggle(), () -> getMuteState(trackIndex, track));
            button.bindIsPressed(soloLayer, pressed -> handleSoloAction(pressed, trackIndex, track),
               () -> getSoloState(trackIndex, track));
            button.bindIsPressed(controlLayer, pressed -> {
            }, () -> RgbState.BLUE_LO);
         }
      }

      final RgbCcButton sceneLaunchButton = driver.getHwControl().getSceneLaunchButton();
      sceneLaunchButton.bindPressed(this, () -> doSceneLaunch(targetScene),
         () -> sceneLaunched && hasPlayQueued() ? RgbState.flash(22, 0) : RgbState.of(0));
      if (driver.isMiniVersion()) {
         driver.getShiftState().addValueObserver(shiftActive -> {
            if (isActive()) {
               shiftLayer.setIsActive(shiftActive);
            }
         });
         bindUpDownButtons(driver, shiftLayer, trackBank, sceneLaunchButton, row2ModeButton);
      } else {
         final RgbCcButton navUpButton = driver.getHwControl().getNavUpButton();
         final RgbCcButton navDownButton = driver.getHwControl().getNavDownButton();
         bindUpDownButtons(driver, this, trackBank, navUpButton, navDownButton);
      }

      currentModeLayer.activate();
   }

   private void handleSoloAction(final boolean pressed, final int trackIndex, final Track track) {
      if (pressed) {
         heldSoloKeys.add(trackIndex);
         host.println(" SOLO " + trackIndex + " > " + heldSoloKeys.size());
         track.solo().toggle(heldSoloKeys.size() < 2);
      } else {
         heldSoloKeys.remove(trackIndex);
      }
   }


   private void bindUpDownButtons(final LaunchkeyMk3Extension driver, final Layer layer, final TrackBank trackBank,
                                  final RgbCcButton upButton, final RgbCcButton downButton) {
      upButton.bindIsPressed(layer, pressed -> {
         if (pressed) {
            driver.startHold(() -> trackBank.sceneBank().scrollBackwards());
         } else {
            driver.stopHold();
         }
      }, pressed -> {
         if (trackBank.sceneBank().canScrollBackwards().get()) {
            return pressed ? RgbState.WHITE : RgbState.LOW_WHITE;
         } else {
            return RgbState.OFF;
         }
      });
      downButton.bindIsPressed(layer, pressed -> {
         if (pressed) {
            driver.startHold(() -> trackBank.sceneBank().scrollForwards());
         } else {
            driver.stopHold();
         }
      }, pressed -> {
         if (trackBank.sceneBank().canScrollForwards().get()) {
            return pressed ? RgbState.WHITE : RgbState.LOW_WHITE;
         } else {
            return RgbState.OFF;
         }
      });
   }

   private void markTrack(final Track track) {
      track.isStopped().markInterested();
      track.mute().markInterested();
      track.solo().markInterested();
      track.isQueuedForStop().markInterested();
      track.arm().markInterested();
   }

   private void advanceLayer() {
      currentModeLayer.setIsActive(false);
      switch (mode) {
         case LAUNCH:
            mode = Mode.STOP;
            currentModeLayer = stopLayer;
            break;
         case STOP:
            mode = Mode.SOLO;
            currentModeLayer = soloLayer;
            break;
         case SOLO:
            mode = Mode.MUTE;
            currentModeLayer = muteLayer;
            break;
         case MUTE:
            mode = Mode.LAUNCH;
            currentModeLayer = launchLayer2;
            break;
//         case MUTE:
//            mode = Mode.CONTROL;
//            currentModeLayer = controlLayer;
//            break;
//         case CONTROL:
//            mode = Mode.LAUNCH;
//            currentModeLayer = launchLayer2;
//            break;
      }
      heldSoloKeys.clear();
      currentModeLayer.setIsActive(true);
   }

   private RgbState getModeColor() {
      switch (mode) {
         case LAUNCH:
            return RgbState.WHITE;
         case STOP:
            return RgbState.RED;
         case MUTE:
            return RgbState.ORANGE;
         case SOLO:
            return RgbState.YELLOW;
         case CONTROL:
            return RgbState.BLUE;
      }
      return RgbState.OFF;
   }

   private void doSceneLaunch(final Scene scene) {
      if (scene.clipCount().get() > 0) {
         sceneLaunched = true;
      }
      scene.launch();
   }

   private boolean hasPlayQueued() {
      for (int i = 0; i < sceneQueuePlayback.length; i++) {
         if (sceneQueuePlayback[i]) {
            return true;
         }
      }
      return false;
   }

   private void prepareSlot(final ClipLauncherSlot slot, final int index) {
      slot.hasContent().markInterested();
      slot.isPlaying().markInterested();
      slot.isStopQueued().markInterested();
      slot.isRecordingQueued().markInterested();
      slot.isRecording().markInterested();
      slot.isPlaybackQueued().markInterested();
      slot.color().addValueObserver((r, g, b) -> colorIndex[index] = ColorLookup.toColor(r, g, b));
   }

   private void handleSlot(final Track track, final ClipLauncherSlot slot, final int trackIndex, final int sceneIndex) {
      slot.launch();
   }

   private RgbState getStopState(final int index, final Track track) {
      if (track.exists().get()) {
         if (track.isQueuedForStop().get()) {
            return RgbState.flash(5, 0);
         }
         if (track.isStopped().get()) {
            return RgbState.RED_LO;
         }
         return RgbState.RED;
      }
      return RgbState.OFF;
   }

   private RgbState getMuteState(final int index, final Track track) {
      if (track.exists().get()) {
         if (track.mute().get()) {
            return RgbState.ORANGE;
         }
         return RgbState.ORANGE_LO;
      }
      return RgbState.OFF;
   }

   private RgbState getSoloState(final int index, final Track track) {
      if (track.exists().get()) {
         if (track.solo().get()) {
            return RgbState.YELLOW;
         }
         return RgbState.YELLOW_LO;
      }
      return RgbState.OFF;
   }

   private RgbState getState(final Track track, final ClipLauncherSlot slot, final int trackIndex,
                             final int sceneIndex) {
      if (slot.hasContent().get()) {
         final int color = colorIndex[sceneIndex * 8 + trackIndex];
         if (slot.isRecordingQueued().get()) {
            return RgbState.flash(color, 5);
         } else if (slot.isRecording().get()) {
            return RgbState.pulse(5);
         } else if (slot.isPlaybackQueued().get()) {
            return RgbState.flash(color, 23);
         } else if (slot.isStopQueued().get()) {
            return RgbState.flash(color, 1);
         } else if (track.isQueuedForStop().get()) {
            return RgbState.flash(color, 1);
         } else if (slot.isPlaying().get()) {
            return RgbState.pulse(22);
         }
         return RgbState.of(color);
      }
      if (slot.isRecordingQueued().get()) {
         return RgbState.flash(5, 0); // Possibly Track Color
      } else if (track.arm().get()) {
         return RgbState.RED;
      }
      return RgbState.OFF;
   }

   @Override
   protected void onActivate() {
      super.onActivate();
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
   }

}
