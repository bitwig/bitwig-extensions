package com.bitwig.extensions.controllers.expressivee.osmoseCE49;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class OsmoseCE49ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("3e7b1c42-5fd0-4e88-8a3c-4c1b92ef77d4");

   public OsmoseCE49ExtensionDefinition() {
   }

   @Override
   public String getName() {
      return "Osmose CE 49";
   }

   @Override
   public String getAuthor() {
      return "Expressive E";
   }

   @Override
   public String getVersion() {
      return "0.1";
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor() {
      return "Expressive E";
   }

   @Override
   public String getHardwareModel() {
      return "Osmose CE 49";
   }

   @Override
   public int getRequiredAPIVersion() {
      return 22;
   }

   @Override
   public int getNumMidiInPorts() {
      return 2;
   }

   @Override
   public int getNumMidiOutPorts() {
      return 2;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
         final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[] { "Osmose CE 49", "MIDIIN3 (Osmose CE 49)" },
               new String[] { "Osmose CE 49", "MIDIOUT3 (Osmose CE 49)" });
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[] { "Osmose CE 49 play", "Osmose CE 49 daw control" },
               new String[] { "Osmose CE 49 play", "Osmose CE 49 daw control" });
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[] { "Osmose CE 49 play", "Osmose CE 49 daw control" },
               new String[] { "Osmose CE 49 play", "Osmose CE 49 daw control" });
      }
   }

   @Override
   public OsmoseCE49Extension createInstance(final ControllerHost host) {
      return new OsmoseCE49Extension(this, host);
   }
}
