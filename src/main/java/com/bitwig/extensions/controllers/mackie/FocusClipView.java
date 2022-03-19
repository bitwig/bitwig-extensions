package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;

public class FocusClipView {

   private final Clip focusClip;
   private String trackName;
   private String slotName;
   private final BasicStringValue currentClipName = new BasicStringValue("");

   public FocusClipView(final ControllerHost host) {
      focusClip = host.createLauncherCursorClip(0, 10);
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

   public BasicStringValue getCurrentClipName() {
      return currentClipName;
   }

   public Clip getFocusClip() {
      return focusClip;
   }
}