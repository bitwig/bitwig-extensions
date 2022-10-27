package com.bitwig.extensions.controllers.mackie.section;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class TrackSelectionHandlerIcon extends TrackSelectionHandler {

   public TrackSelectionHandlerIcon(final MackieMcuProExtension driver) {
      super(driver);
   }

   @Override
   public void handleTrackSelection(final Track track) {
      final ModifierValueObject modifier = driver.getModifier();

      if (track.exists().get()) {
         if (modifier.isSet(ModifierValueObject.SHIFT, ModifierValueObject.OPTION)) {
            track.stop();
         } else if (modifier.isShift()) {
            track.isGroupExpanded().toggle();
         } else if (modifier.isClearSet()) {
            track.deleteObject();
         } else if (modifier.isDuplicateSet()) {
            track.duplicate();
         } else if (modifier.isOption()) {
            driver.getApplication().navigateIntoTrackGroup(track);
         } else {
            track.selectInMixer();
         }
      } else {
         if (modifier.isShift()) {
            driver.getApplication().createAudioTrack(-1);
         } else if (modifier.isSet(ModifierValueObject.ALT)) {
            driver.getApplication().createEffectTrack(-1);
         } else {
            driver.getApplication().createInstrumentTrack(-1);
         }
      }
   }
}
