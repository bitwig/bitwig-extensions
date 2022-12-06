package com.bitwig.extensions.controllers.arturia.keystep;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class KeyStep37ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("6102eb09-7f47-42c1-8462-239de519dcca");

   public KeyStep37ExtensionDefinition() {
   }

   @Override
   public String getName() {
      return "Keystep 37";
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return "1.0";
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor() {
      return "Arturia";
   }

   @Override
   public String getHardwareModel() {
      return "Keystep 37";
   }

   @Override
   public int getRequiredAPIVersion() {
      return 16;
   }

   @Override
   public int getNumMidiInPorts() {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts() {
      return 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[]{"Arturia KeyStep 37"}, new String[]{"Arturia KeyStep 37"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"Arturia KeyStep 37"}, new String[]{"Arturia KeyStep 37"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"Arturia KeyStep 37"}, new String[]{"Arturia KeyStep 37"});
      }
   }

   @Override
   public KeyStepProExtension createInstance(final ControllerHost host) {
      return new KeyStepProExtension(this, host);
   }
}
