package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

import com.bitwig.extension.controller.api.ControllerHost;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugOutMk {

   private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
   private static ControllerHost host;

   public static void println(final String format, final Object... args) {
      if (host != null) {
         final LocalDateTime now = LocalDateTime.now();
         host.println(now.format(DF) + " > " + String.format(format, args));
      }
   }

   public static void registerHost(final ControllerHost host) {
      DebugOutMk.host = host;
   }
}
