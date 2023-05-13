package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.ControllerHost;

public class DebugUtil {
   public static ControllerHost host;

   public static void println(final String format, final Object... args) {
      host.println(String.format(format, args));
   }
}
