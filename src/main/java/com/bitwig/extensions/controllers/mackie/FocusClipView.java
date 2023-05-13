package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class FocusClipView {

   private final Clip focusClip;
   private final ControllerHost host;
   private String trackName;
   private String slotName;
   private final BasicStringValue currentClipName = new BasicStringValue("");

   private final ClipLauncherSlot focussingClipSlot;
   private final TrackBank singleTrackBank;
   private final SceneBank singleSceneBank;


   public FocusClipView(final ControllerHost host, final CursorTrack cursorTrack) {
      focusClip = host.createLauncherCursorClip(2, 10);
      this.host = host;

      singleTrackBank = host.createTrackBank(1, 0, 1);
      singleTrackBank.followCursorTrack(cursorTrack);
      singleSceneBank = singleTrackBank.sceneBank();
      final Track theTrack = singleTrackBank.getItemAt(0);
//      cursorTrack.name().addValueObserver(name -> host.println("Track name = " + name));
//      theTrack.name().addValueObserver(name -> host.println("BANK Track name = " + name));
      final ClipLauncherSlotBank slotBank = theTrack.clipLauncherSlotBank();
      focussingClipSlot = slotBank.getItemAt(0);
      focussingClipSlot.setIndication(true);
      focussingClipSlot.hasContent().markInterested();

      final Track track = focusClip.getTrack();
      track.name().addValueObserver(name -> {
         trackName = name;
         currentClipName.set(trackName + ":" + slotName);
      });
      focusClip.clipLauncherSlot().name().addValueObserver(name -> {
         slotName = name;
         currentClipName.set(trackName + ":" + slotName);
      });
   }

   public void clipAction() {
      focusClip.clipLauncherSlot().launch();
   }


   public void clipCreate(final int createLengthBeats) {
      if (!focussingClipSlot.hasContent().get()) {
         focussingClipSlot.createEmptyClip(4);
      }
   }

   public void clipAction(final int createLengthBeats, final ModifierValueObject modifier) {
      if (focussingClipSlot.hasContent().get()) {
         if (modifier.isClearSet()) {
            focusClip.clearSteps();
         } else if (modifier.isDuplicateSet()) {
            focusClip.duplicateContent();
         } else {
            focussingClipSlot.launch();
         }
      } else {
         if (modifier.isShift()) {
            focussingClipSlot.createEmptyClip(4);
         } else {
            focussingClipSlot.launch();
         }
      }
   }

   public BasicStringValue getCurrentClipName() {
      return currentClipName;
   }

   public Clip getFocusClip() {
      return focusClip;
   }

   public void navigateFocusVertical(final int direction, final boolean isPressed) {
      if (!isPressed) {
         return;
      }
      singleSceneBank.scrollBy(direction * -1);
      focussingClipSlot.select();
   }

   public void navigateFocusHorizontal(final int direction, final boolean isPressed) {
      if (!isPressed) {
         return;
      }
      singleTrackBank.scrollBy(direction);
      focussingClipSlot.select();
   }

}
