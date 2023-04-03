package com.bitwig.extensions.controllers.akai.apcmk2;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.di.Component;

@Component
public class ViewControl {

   private final TrackBank trackBank;
   private final TrackBank maxTrackBank;
   private final CursorTrack cursorTrack;
   private final Track rootTrack;
   private final PinnableCursorDevice cursorDevice;
   private final Clip cursorClip;
   private final PinnableCursorDevice primaryDevice;
   private int queuedForPlaying = 0;
   private int focusSceneIndex;

   public ViewControl(final ControllerHost host, ApcConfiguration configuration) {
      rootTrack = host.getProject().getRootTrackGroup();

      trackBank = host.createTrackBank(8, 1, configuration.getSceneRows(), true);
      maxTrackBank = host.createTrackBank(64, 1, 1, true);

      cursorTrack = host.createCursorTrack(8, 8);
      for (int i = 0; i < 8; i++) {
         prepareTrack(trackBank.getItemAt(i));
      }
      setUpFocusScene();

      cursorTrack.name().markInterested();
      cursorDevice = cursorTrack.createCursorDevice();
      cursorClip = host.createLauncherCursorClip(32 * 6, 127);
      primaryDevice = cursorTrack.createCursorDevice("drumdetection", "Pad Device", 8,
         CursorDeviceFollowMode.FIRST_INSTRUMENT);
      primaryDevice.hasDrumPads().markInterested();
      primaryDevice.exists().markInterested();

      cursorTrack.clipLauncherSlotBank().cursorIndex().addValueObserver(index -> {
         // RemoteConsole.out.println(" => {}", index);
      });
      prepareTrack(cursorTrack);
   }

   private void setUpFocusScene() {
      maxTrackBank.sceneBank().scrollPosition().addValueObserver(scrollPos -> this.focusSceneIndex = scrollPos);
      for (int i = 0; i < 64; i++) {
         Track track = maxTrackBank.getItemAt(i);
         final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(0);
         slot.isPlaybackQueued().addValueObserver(queued -> {
            if (queued) {
               queuedForPlaying++;
            } else if (queuedForPlaying > 0) {
               queuedForPlaying--;
            }
         });
      }
   }


   private void prepareTrack(final Track track) {
      track.arm().markInterested();
      track.monitorMode().markInterested();
      track.sourceSelector().hasAudioInputSelected().markInterested();
      track.sourceSelector().hasNoteInputSelected().markInterested();
   }

   public PinnableCursorDevice getPrimaryDevice() {
      return primaryDevice;
   }

   public TrackBank getTrackBank() {
      return trackBank;
   }

   public CursorTrack getCursorTrack() {
      return cursorTrack;
   }

   public Track getRootTrack() {
      return rootTrack;
   }

   public PinnableCursorDevice getCursorDevice() {
      return cursorDevice;
   }

   public boolean hasQueuedForPlaying() {
      return queuedForPlaying > 0;
   }

   public int getFocusSceneIndex() {
      return focusSceneIndex;
   }

   public void focusScene(final int sceneIndex) {
      maxTrackBank.sceneBank().scrollPosition().set(sceneIndex);
   }
}
