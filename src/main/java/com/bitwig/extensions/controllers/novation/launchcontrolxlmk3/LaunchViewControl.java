package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.di.Component;

@Component
public class LaunchViewControl {

   private final TrackBank trackBank;
   private final CursorTrack cursorTrack;
   private final SendBank refSendBank;
   private final PinnableCursorDevice cursorDevice;
   private final Track rootTrack;

   public LaunchViewControl(final ControllerHost host) {
      trackBank = host.createTrackBank(8, 2, 1);
      refSendBank = trackBank.getItemAt(0).sendBank();
      for (int i = 0; i < 8; i++) {
         prepareTrack(trackBank.getItemAt(i));
      }
      cursorTrack = host.createCursorTrack(8, 1);
      prepareTrack(cursorTrack);
      trackBank.followCursorTrack(cursorTrack);

      cursorDevice = cursorTrack.createCursorDevice();

      rootTrack = host.getProject().getRootTrackGroup();

      cursorTrack.hasNext().markInterested();
      cursorTrack.hasPrevious().markInterested();
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

   public SendBank getRefSendBank() {
      return refSendBank;
   }

   public void navigateSends(final int dir) {
      for (int i = 0; i < 8; i++) {
         final Track track = trackBank.getItemAt(i);
         track.sendBank().scrollBy(dir);
      }
   }

   private void prepareTrack(final Track track) {
      track.arm().markInterested();
      track.mute().markInterested();
      track.solo().markInterested();
      track.sourceSelector().hasAudioInputSelected().markInterested();
      track.sourceSelector().hasNoteInputSelected().markInterested();
   }

}
