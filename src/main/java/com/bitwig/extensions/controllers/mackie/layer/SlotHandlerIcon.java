package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class SlotHandlerIcon extends SlotHandler {

   @Override
   public void handleSlotPressed(final Track track, final ClipLauncherSlot slot, final Clip cursorClip,
                                 final ModifierValueObject modifier) {
      if (modifier.isClearSet()) {
         slot.deleteObject();
      } else if (modifier.isShiftSet() && modifier.isDuplicateSet()) {
         slot.select();
         cursorClip.duplicateContent();
      } else if (modifier.isDuplicateSet()) {
         if (!slot.hasContent().get()) {
            slot.createEmptyClip(4);
            slot.select();
         } else {
            slot.duplicateClip();
         }
      } else if (modifier.isOption()) {
         track.stop();
      } else if (modifier.isShift()) {
         slot.select();
      } else {
         slot.launch();
      }
   }
}
