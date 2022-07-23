package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbNoteButton;
import com.bitwig.extensions.framework.Layer;

public class SessionLayer extends Layer {

   private final ControllerHost host;

   private final int[] colorIndex = new int[16];
   private final boolean[] sceneQueuePlayback = new boolean[8];
   private boolean sceneLaunched = false;
   private final Layer launchLayer2;
   private final Layer muteLayer;
   private final Layer soloLayer;
   private final Layer stopLayer;
   private Layer currentModeLayer;
   private Mode mode = Mode.LAUNCH;

   private enum Mode {
      LAUNCH,
      STOP,
      SOLO,
      MUTE
   }

   public SessionLayer(final LaunchkeyMk3Extension driver) {
      super(driver.getLayers(), "SESSION_LAYER");

      host = driver.getHost();
      final TrackBank trackBank = driver.getTrackBank();
      final SceneBank sceneBank = trackBank.sceneBank();
      final Scene targetScene = trackBank.sceneBank().getScene(0);
      targetScene.clipCount().markInterested();

      launchLayer2 = new Layer(driver.getLayers(), "LAUNCH_LAYER2");
      muteLayer = new Layer(driver.getLayers(), "MUTE_LAYER");
      soloLayer = new Layer(driver.getLayers(), "SOLO_LAYER");
      stopLayer = new Layer(driver.getLayers(), "STOP_LAYER");
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
         slot.setIndication(true);
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
            button.bindPressed(soloLayer, () -> track.solo().toggle(), () -> getSoloState(trackIndex, track));
            slot.setIndication(true);
         }
      }

      final RgbCcButton navUpButton = driver.getHwControl().getNavUpButton();
      navUpButton.bindIsPressed(this, pressed -> {
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
      final RgbCcButton navDownButton = driver.getHwControl().getNavDownButton();
      navDownButton.bindIsPressed(this, pressed -> {
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
      final RgbCcButton sceneLaunchButton = driver.getHwControl().getSceneLaunchButton();
      sceneLaunchButton.bindPressed(this, () -> doSceneLaunch(targetScene),
         () -> sceneLaunched && hasPlayQueued() ? RgbState.flash(22, 0) : RgbState.of(0));
      currentModeLayer.activate();
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
      }
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
      slot.color().addValueObserver((r, g, b) -> {
         colorIndex[index] = ColorLookup.toColor(r, g, b);
//         final int rv = (int) Math.floor(r * 255);
//         final int gv = (int) Math.floor(g * 255);
//         final int bv = (int) Math.floor(b * 255);
//         if (rv < 10 && gv < 10 && bv < 10) {
//            colorIndex[index] = 0; // black
//         } else if (rv > 230 && gv > 230 && bv > 230) {
//            colorIndex[index] = 3; // whit
//         } else if (rv == gv && bv == gv) {
//            final int bright = rv >> 4;
//            host.println(" B=" + bright);
//            if (bright > 7) {
//               colorIndex[index] = 2; // gray
//            } else {
//               colorIndex[index] = 1;
//            }
//         } else {
//            final ColorLookup.Hsb hsb = ColorLookup.rgbToHsb(rv, gv, bv);
//            int hueInd = hsb.hue > 6 ? hsb.hue - 1 : hsb.hue;
//            hueInd = hueInd > 13 ? 13 : hueInd;
//            colorIndex[index] = 5 + hueInd * 4 + 1;
//            if (hsb.sat < 8) {
//               colorIndex[index] -= 2;
//            } else if (hsb.bright <= 8) {
//               colorIndex[index] += 2;
//            }
//         }
//         host.println(String.format("[%02d] %d,%d,%d> => %s .. %d", index, rv, gv, bv, hsb, colorIndex[index]));
      });
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
