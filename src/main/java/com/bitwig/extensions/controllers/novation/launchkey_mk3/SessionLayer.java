package com.bitwig.extensions.controllers.novation.launchkey_mk3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;

public class SessionLayer extends Layer {

   private final ControllerHost host;

   private final int[] colorIndex = new int[16];
   private final boolean[] sceneQueuePlayback = new boolean[8];
   private boolean sceneLaunched = false;
   // TODO Represent Launching Scene

   public SessionLayer(final LaunchkeyMk3Extension driver) {
      super(driver.getLayers(), "SESSION_LAYER");

      host = driver.getHost();
      final TrackBank trackBank = driver.getTrackBank();
      final SceneBank sceneBank = trackBank.sceneBank();
      final Scene targetScene = trackBank.sceneBank().getScene(0);
      targetScene.clipCount().markInterested();

      final RgbNoteButton[] buttons = driver.getHwControl().getSessionButtons();

      sceneBank.canScrollBackwards().markInterested();
      sceneBank.canScrollForwards().markInterested();

      for (int i = 0; i < 16; i++) {
         final RgbNoteButton button = buttons[i];
         final int sceneIndex = i / 8;
         final int trackIndex = i % 8;
         final Track track = trackBank.getItemAt(trackIndex);
         track.arm().markInterested();
         track.isQueuedForStop().markInterested();
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
         button.bindIsPressed(this, pressed -> {
            if (pressed) {
               handleSlot(track, slot, trackIndex, sceneIndex);
            }
         }, () -> getState(track, slot, trackIndex, sceneIndex));
      }


      final RgbCcButton navUpButton = driver.getHwControl().getNavUpButton();
      navUpButton.bindPressed(this, () -> trackBank.sceneBank().scrollBackwards(), () -> {
         if (trackBank.sceneBank().canScrollBackwards().get()) {
            return RgbState.of(2);
         } else {
            return RgbState.of(0);
         }
      });
      final RgbCcButton navDownButton = driver.getHwControl().getNavDownButton();
      navDownButton.bindPressed(this, () -> trackBank.sceneBank().scrollForwards(), () -> {
         if (trackBank.sceneBank().canScrollForwards().get()) {
            return RgbState.of(2);
         } else {
            return RgbState.of(0);
         }
      });
      final RgbCcButton sceneLaunchButton = driver.getHwControl().getSceneLaunchButton();
      sceneLaunchButton.bindPressed(this, () -> doSceneLaunch(targetScene),
         () -> sceneLaunched && hasPlayQueued() ? RgbState.flash(22, 0) : RgbState.of(0));
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
      slot.setIndication(true);
   }

   private void handleSlot(final Track track, final ClipLauncherSlot slot, final int trackIndex, final int sceneIndex) {
      slot.launch();
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
         return RgbState.of(5);
      }
      return RgbState.of(0);
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
