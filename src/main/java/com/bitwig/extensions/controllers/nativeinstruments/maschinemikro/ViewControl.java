package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.PostConstruct;

@Component
public class ViewControl {
   public static final int NUM_PADS_TRACK = 8;

   private final PinnableCursorDevice primaryDevice;
   private final CursorTrack cursorTrack;
   private final TrackBank mixerTrackBank;
   private final PinnableCursorDevice cursorDevice;
   private final Clip cursorClip;
   private final MasterTrack masterTrack;
   private final TrackBank groupTrackBank;
   private final TrackBank maxTrackBank;
   private int queuedForPlaying = 0;
   private int focusSceneIndex;

   public ViewControl(final ControllerHost host) {
      mixerTrackBank = host.createTrackBank(4, 1, 4);
      groupTrackBank = host.createTrackBank(8, 2, 1);
      cursorTrack = host.createCursorTrack(2, 8);
      masterTrack = host.createMasterTrack(16);
      maxTrackBank = host.createTrackBank(64, 1, 1);

      for (int i = 0; i < cursorTrack.sendBank().getSizeOfBank(); i++) {
         Send send = cursorTrack.sendBank().getItemAt(i);
         send.exists().markInterested();
         send.sendChannelColor().markInterested();
      }
      mixerTrackBank.followCursorTrack(cursorTrack);
      cursorClip = host.createLauncherCursorClip(16, 127);
      primaryDevice = cursorTrack.createCursorDevice("DrumDetection", "Pad Device", NUM_PADS_TRACK,
         CursorDeviceFollowMode.FIRST_INSTRUMENT);
      cursorDevice = cursorTrack.createCursorDevice("device-control", "Device Control", 0,
         CursorDeviceFollowMode.FOLLOW_SELECTION);
      setUpFocusScene();
   }

   @PostConstruct
   void init() {
      cursorTrack.hasNext().markInterested();
      cursorTrack.hasPrevious().markInterested();
   }

   private void setUpFocusScene() {
      maxTrackBank.sceneBank().scrollPosition().addValueObserver(scrollPos -> this.focusSceneIndex = scrollPos);
      for (int i = 0; i < maxTrackBank.getSizeOfBank(); i++) {
         Track track = maxTrackBank.getItemAt(i);
         final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(0);
         slot.isPlaybackQueued().addValueObserver(this::trackPlaybackQueuedForScene);
      }
   }

   private void trackPlaybackQueuedForScene(boolean queued) {
      if (queued) {
         queuedForPlaying++;
      } else if (queuedForPlaying > 0) {
         queuedForPlaying--;
      }
   }

   public void focusScene(final int sceneIndex) {
      maxTrackBank.sceneBank().scrollPosition().set(sceneIndex);
   }

   public boolean hasQueuedForPlaying() {
      return queuedForPlaying > 0;
   }

   public int getFocusSceneIndex() {
      return focusSceneIndex;
   }

   public TrackBank getGroupTrackBank() {
      return groupTrackBank;
   }

   public MasterTrack getMasterTrack() {
      return masterTrack;
   }

   public CursorTrack getCursorTrack() {
      return cursorTrack;
   }

   public PinnableCursorDevice getCursorDevice() {
      return cursorDevice;
   }

   public PinnableCursorDevice getPrimaryDevice() {
      return primaryDevice;
   }

   public TrackBank getMixerTrackBank() {
      return mixerTrackBank;
   }

}
