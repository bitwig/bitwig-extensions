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

   public ViewControl(final ControllerHost host) {
      mixerTrackBank = host.createTrackBank(4, 1, 4);
      groupTrackBank = host.createTrackBank(8, 2, 1);
      cursorTrack = host.createCursorTrack(2, 8);
      masterTrack = host.createMasterTrack(16);

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
   }

   @PostConstruct
   void init() {
      cursorTrack.hasNext().markInterested();
      cursorTrack.hasPrevious().markInterested();
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
