package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class SlotHandler {

   public void handleSlotPressed(final Track track, final ClipLauncherSlot slot, final Clip cursorClip,
                                 final ModifierValueObject modifier) {
      if (modifier.isSet(ModifierValueObject.SHIFT, ModifierValueObject.OPTION)) {
         slot.deleteObject();
      } else if (modifier.isSet(ModifierValueObject.SHIFT, ModifierValueObject.ALT)) {
         slot.select();
         cursorClip.duplicateContent();
      } else if (modifier.isAlt()) {
         track.stop();
      } else if (modifier.isOption()) {
         if (!slot.hasContent().get()) {
            slot.createEmptyClip(4);
            slot.select();
         } else {
            slot.duplicateClip();
         }
      } else if (modifier.isShift()) {
         slot.launchAlt();
      } else {
         slot.launch();
      }
   }

   public void handleSlotRelease(Track track, ClipLauncherSlot slot, Clip mainCursorClip,
                                 ModifierValueObject modifier) {
      if (modifier.isShift()) {
         slot.launchReleaseAlt();
      } else if (modifier.notSet()) {
         slot.launchRelease();
      }
   }
}
