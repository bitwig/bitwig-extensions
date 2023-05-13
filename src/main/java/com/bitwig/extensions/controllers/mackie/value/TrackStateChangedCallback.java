package com.bitwig.extensions.controllers.mackie.value;

public interface TrackStateChangedCallback {
   void valueChanged(String name, boolean exists, boolean isGroup, boolean isExpanded,
                     ChannelStateValueHandler handler);
}
