package com.bitwig.extensions.controllers.mackie.section;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class TrackSelectionHandler {

   protected final MackieMcuProExtension driver;

   public TrackSelectionHandler(final MackieMcuProExtension driver) {
      this.driver = driver;
   }

   public void handleTrackSelection(final Track track) {
      final ModifierValueObject modifier = driver.getModifier();
      if (track.exists().get()) {
         if (modifier.isShift()) {
            track.isGroupExpanded().toggle();
         } else if (modifier.isControl()) {
            track.deleteObject();
         } else if (modifier.isAlt()) {
            track.stop();
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
