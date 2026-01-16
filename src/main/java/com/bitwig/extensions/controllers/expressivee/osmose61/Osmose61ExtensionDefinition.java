package com.bitwig.extensions.controllers.expressivee.osmose61;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class Osmose61ExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("d7b14e8f-2c90-41ab-b3f6-9e82f1c4d77e");

   public Osmose61ExtensionDefinition()
   {
   }

   @Override
   public String getName() {
      return "Osmose 61";
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
      return "Osmose 61";
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
         list.add(new String[]{ "Osmose 61", "MIDIIN2 (Osmose 61)", "MIDIIN4 (Osmose 61)" },
               new String[]{ "Osmose 61 play", "MIDIOUT2 (Osmose 61)", "MIDIOUT4 (Osmose 61)" });
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"Osmose 61 play", "Osmose 61 sound engine", "Osmose 61 daw control" },
               new String[]{"Osmose 61 play", "Osmose 61 sound engine", "Osmose 61 daw control" });
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"Osmose 61 play", "Osmose 61 sound engine", "Osmose 61 daw control" },
            new String[]{"Osmose 61 play", "Osmose 61 sound engine", "Osmose 61 daw control" });
      }
   }

   @Override
   public Osmose61Extension createInstance(final ControllerHost host)
   {
      return new Osmose61Extension(this, host);
   }
}
