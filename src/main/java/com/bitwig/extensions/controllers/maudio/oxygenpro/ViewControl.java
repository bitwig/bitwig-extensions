package com.bitwig.extensions.controllers.maudio.oxygenpro;

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
   private final CursorRemoteControlsPage parameterBank;

   public ViewControl(final ControllerHost host, OxyConfig config) {
      mixerTrackBank = host.createTrackBank(config.getNumberOfControls(), 1, 2);
      cursorTrack = host.createCursorTrack(1, 2);
      masterTrack = host.createMasterTrack(2);


      mixerTrackBank.followCursorTrack(cursorTrack);
      cursorClip = host.createLauncherCursorClip(32, 127);

      primaryDevice = cursorTrack.createCursorDevice("DrumDetection", "Pad Device", NUM_PADS_TRACK,
         CursorDeviceFollowMode.FIRST_INSTRUMENT);
      cursorDevice = cursorTrack.createCursorDevice("device-control", "Device Control", 0,
         CursorDeviceFollowMode.FOLLOW_SELECTION);
      parameterBank = cursorDevice.createCursorRemoteControlsPage(config.getNumberOfControls());
   }

   @PostConstruct
   void init() {
      cursorTrack.hasNext().markInterested();
      cursorTrack.hasPrevious().markInterested();
   }

   public CursorRemoteControlsPage getParameterBank() {
      return parameterBank;
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
