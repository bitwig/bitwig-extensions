package com.bitwig.extensions.framework.values;

import com.bitwig.extension.controller.api.Transport;

/**
 * Preset Modes enumerator.
 */
public enum FocusMode {
   LAUNCHER("Launcher"),
   ARRANGER("Arranger");

   private final String descriptor;

   FocusMode(final String descriptor) {
      this.descriptor = descriptor;
   }

   public static FocusMode toMode(final String s) {
      for (final FocusMode mode : FocusMode.values()) {
         if (mode.getDescriptor().equals(s)) {
            return mode;
         }
      }
      return ARRANGER;
   }

   public String getDescriptor() {
      return descriptor;
   }

   public boolean getState(final Transport transport) {
      if (this == ARRANGER) {
         return transport.isArrangerRecordEnabled().get();
      } else {
         return transport.isClipLauncherOverdubEnabled().get();
      }
   }
}
