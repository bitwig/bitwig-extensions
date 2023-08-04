package com.bitwig.extensions.controllers.akai.mpkminiplus;

import java.util.Arrays;

enum TransportButtonMode {
   TRANSPORT("Rewind/Fast Forward"),
   DEVICE("Device Parameters"),
   TRACK_SELECTION("Track Selection");

   private final String descriptor;

   TransportButtonMode(final String descriptor) {
      this.descriptor = descriptor;
   }

   public static TransportButtonMode toMode(final String s) {
      for (final TransportButtonMode mode : TransportButtonMode.values()) {
         if (mode.descriptor.equals(s)) {
            return mode;
         }
      }
      return TRANSPORT;
   }

   public static String[] toSelector() {
      return Arrays.stream(TransportButtonMode.values()) //
         .map(TransportButtonMode::getDescriptor) //
         .toArray(String[]::new);
   }

   public String getDescriptor() {
      return descriptor;
   }
}
