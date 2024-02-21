package com.bitwig.extensions.controllers.novation.launchpadmini3;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class LaunchPadMiniMk3ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("27c7aa71-4095-4b48-8e6f-5947f7ff6a38");

   public LaunchPadMiniMk3ExtensionDefinition() {
   }

   @Override
   public String getName() {
      return "Launchpad Mini Mk3";
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return "1.01";
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor() {
      return "Novation";
   }

   @Override
   public String getHardwareModel() {
      return "Launchpad Mini Mk3";
   }

   @Override
   public int getRequiredAPIVersion() {
      return 18;
   }

   @Override
   public int getNumMidiInPorts() {
      return 2;
   }

   @Override
   public int getNumMidiOutPorts() {
      return 1;
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/Novation/Launchpad Mini MK3.pdf";
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      final String[] inputNames = new String[2];
      final String[] outputNames = new String[1];

      switch (platformType) {
         case WINDOWS:
            inputNames[0] = "LPMiniMK3 MIDI";
            inputNames[1] = "MIDIIN2 (LPMiniMK3 MIDI)";
            outputNames[0] = "LPMiniMK3 MIDI";
            break;
         case MAC:
         case LINUX:
            inputNames[0] = "Launchpad Mini MK3 LPMiniMK3 DAW Out";
            inputNames[1] = "Launchpad Mini MK3 LPMiniMK3 MIDI Out";
            outputNames[0] = "Launchpad Mini MK3 LPMiniMK3 DAW In";
            break;
      }

      list.add(inputNames, outputNames);
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host) {
      return new LaunchpadMiniMk3ControllerExtension(this, host);
   }
}
