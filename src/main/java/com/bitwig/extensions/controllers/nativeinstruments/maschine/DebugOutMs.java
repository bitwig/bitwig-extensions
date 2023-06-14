package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import com.bitwig.extension.controller.api.ControllerHost;

public class DebugOutMs {

   public static ControllerHost host;

   public static void println(final String format, final Object... args) {
      if (host != null) {
         host.println(String.format(format, args));
      }
   }
}
