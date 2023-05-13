package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.mackie.StringUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class ChannelStateValueHandler {

   protected final List<TrackStateChangedCallback> listeners = new ArrayList<>();
   protected String currentTrackName = "";

   public static ChannelStateValueHandler create(final Channel channel) {
      if (channel instanceof Track) {
         return new TrackStateValueHandler((Track) channel);
      }
      return new ChannelStateValueHandlerImpl(channel);
   }

   public void addValueObserver(final TrackStateChangedCallback callback) {
      listeners.add(callback);
   }

   public abstract String toCurrentValue(final String fixed, final int maxLen, final String name, final boolean exists,
                                         final boolean isGroup, final boolean isExpanded);

   public abstract String toCurrentValue(String fixed, int segmentLength);

   public String get() {
      return currentTrackName;
   }

   public static String toDisplay(final String name, final boolean exists, final boolean isGroupTrack,
                                  final boolean isExpanded, final int maxLen) {
      if (!exists) {
         return "";
      }
      if (!isGroupTrack) {
         return StringUtil.toAsciiDisplay(name, maxLen);
      } else if (isExpanded) {
         return StringUtil.toAsciiDisplay(">" + name, maxLen);
      }
      return StringUtil.toAsciiDisplay("*" + name, maxLen);
   }

}
