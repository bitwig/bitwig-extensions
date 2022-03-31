package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.SettableStringValue;
import com.bitwig.extension.controller.api.Track;

public class TrackStateValueHandler extends ChannelStateValueHandler {

   private Track track;

   public TrackStateValueHandler(final Track track) {
      this.track = track;
      final SettableStringValue nameValue = track.name();
      final BooleanValue isGroupValue = track.isGroup();
      final BooleanValue isExpandedValue = track.isGroupExpanded();
      final BooleanValue existsValue = track.exists();
      existsValue.addValueObserver(exists -> {
         listeners.forEach(
            callback -> callback.valueChanged(currentTrackName, exists, isGroupValue.get(), isExpandedValue.get(),
               this));
      });
      nameValue.addValueObserver(newString -> {
         currentTrackName = newString;
         listeners.forEach(
            callback -> callback.valueChanged(newString, existsValue.get(), isGroupValue.get(), isExpandedValue.get(),
               this));
      });
      isGroupValue.addValueObserver(isGroup -> {
         listeners.forEach(
            callback -> callback.valueChanged(currentTrackName, existsValue.get(), isGroup, isExpandedValue.get(),
               this));
      });
      isExpandedValue.addValueObserver(newGroupExpand -> {
         listeners.forEach(
            callback -> callback.valueChanged(currentTrackName, existsValue.get(), isGroupValue.get(), newGroupExpand,
               this));
      });
      currentTrackName = nameValue.get();
   }

   @Override
   public String toCurrentValue(final String fixed, final int maxLen, final String name, final boolean exists,
                                final boolean isGroup, final boolean isExpanded) {
      if (fixed != null) {
         return toDisplay(fixed, exists, isGroup, isExpanded, maxLen);
      }
      return toDisplay(name, exists, isGroup, isExpanded, maxLen);
   }

   @Override
   public String toCurrentValue(final String fixed, final int segmentLength) {
      return toCurrentValue(fixed, segmentLength, track.name().get(), track.exists().get(), track.isGroup().get(),
         track.isGroupExpanded().get());
   }
}
