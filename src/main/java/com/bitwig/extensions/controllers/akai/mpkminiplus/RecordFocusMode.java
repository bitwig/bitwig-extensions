package com.bitwig.extensions.controllers.akai.mpkminiplus;

import java.util.Arrays;

enum RecordFocusMode {
   LAUNCHER("Launcher"),
   ARRANGER("Arranger");
   //CLIP_LAUNCHER("Clip Record/Launch");

   private final String descriptor;

   RecordFocusMode(final String descriptor) {
      this.descriptor = descriptor;
   }

   public static RecordFocusMode toMode(final String s) {
      for (final RecordFocusMode mode : RecordFocusMode.values()) {
         if (mode.descriptor.equals(s)) {
            return mode;
         }
      }
      return ARRANGER;
   }

   public static String[] toSelector() {
      return Arrays.stream(RecordFocusMode.values()) //
         .map(RecordFocusMode::getDescriptor) //
         .toArray(String[]::new);
   }

   public String getDescriptor() {
      return descriptor;
   }
}
