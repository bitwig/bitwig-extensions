package com.bitwig.extensions.controllers.expressivee.osmose49;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class Osmose49ExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("a3c9f4b2-7e1d-4f3a-9c8e-12f4b6d9a8c1");

   public Osmose49ExtensionDefinition() {
   }

   @Override
   public String getName() {
      return "Osmose 49";
   }

   @Override
   public String getAuthor() {
      return "Expressive E";
   }

   @Override
   public String getVersion() {
      return "0.4";
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
      return "Osmose 49";
   }

   @Override
   public int getRequiredAPIVersion() {
      return 22;
   }

   @Override
   public int getNumMidiInPorts() {
      return 3;
   }

   @Override
   public int getNumMidiOutPorts() {
      return 3;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
         final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[]{ "Osmose 49", "MIDIIN2 (Osmose 49)", "MIDIIN4 (Osmose 49)" },
               new String[]{ "Osmose 49 play", "MIDIOUT2 (Osmose 49)", "MIDIOUT4 (Osmose 49)" });
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{ "Osmose 49 play", "Osmose 49 sound engine", "Osmose 49 daw control" },
               new String[]{ "Osmose 49 play", "Osmose 49 sound engine", "Osmose 49 daw control" });
      }
      else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{ "Osmose 49 play", "Osmose 49 sound engine", "Osmose 49 daw control" },
               new String[]{ "Osmose 49 play", "Osmose 49 sound engine", "Osmose 49 daw control" });
      }
   }

   @Override
   public Osmose49Extension createInstance(final ControllerHost host) {
      return new Osmose49Extension(this, host);
   }
}
