package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.framework.di.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LaunchControlXlMk3Extension extends ControllerExtension {

   private static ControllerHost debugHost;
   private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");

   private final LaunchControlXlMk3ExtensionDefinition definition;
   private HardwareSurface surface;
   private Context diContext;

   public static void println(final String format, final Object... args) {
      if (debugHost != null) {
         final LocalDateTime now = LocalDateTime.now();
         debugHost.println(now.format(DF) + " > " + String.format(format, args));
      }
   }

   public LaunchControlXlMk3Extension(final LaunchControlXlMk3ExtensionDefinition definition,
                                      final ControllerHost host) {
      super(definition, host);
      this.definition = definition;
   }

   public void init() {
      debugHost = getHost();
      diContext = new Context(this);
      surface = diContext.getService(HardwareSurface.class);
      diContext.activate();
   }

   @Override
   public void exit() {
      // Nothing right now
      diContext.deactivate();
   }

   @Override
   public void flush() {
      surface.updateHardware();
   }

}
