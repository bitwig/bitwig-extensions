package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.SettableStringValue;

public class ChannelStateValueHandlerImpl extends ChannelStateValueHandler {
   private Channel channel;

   public ChannelStateValueHandlerImpl(final Channel channel) {
      this.channel = channel;
      final SettableStringValue nameValue = channel.name();
      final BooleanValue existsValue = channel.exists();
      existsValue.addValueObserver(exists -> {
         listeners.forEach(callback -> callback.valueChanged(currentTrackName, exists, false, false, this));
      });
      nameValue.addValueObserver(newString -> {
         currentTrackName = nameValue.get();
         listeners.forEach(callback -> callback.valueChanged(newString, existsValue.get(), false, false, this));
      });
      currentTrackName = nameValue.get();
   }

   @Override
   public String toCurrentValue(final String fixed, final int maxLen, final String name, final boolean exists,
                                final boolean isGroup, final boolean isExpanded) {
      if (channel.exists().get()) {
         if (fixed != null) {
            return toDisplay(fixed, exists, false, false, maxLen);
         }
         return toDisplay(name, exists, false, false, maxLen);
      }
      return "";
   }

   @Override
   public String toCurrentValue(final String fixed, final int segmentLength) {
      return toCurrentValue(fixed, segmentLength, channel.name().get(), channel.exists().get(), false, false);
   }
}
