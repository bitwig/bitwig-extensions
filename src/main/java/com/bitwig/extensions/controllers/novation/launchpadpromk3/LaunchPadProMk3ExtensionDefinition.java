package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class LaunchPadProMk3ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("27c7aa71-4095-4b48-8e6f-4947f7ff6a38");

   public LaunchPadProMk3ExtensionDefinition() {
   }

   @Override
   public String getName() {
      return "Launchpad Pro Mk3";
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return "0.95";
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
      return "Launchpad Pro Mk3";
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
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      final String[] inputNames = new String[2];
      final String[] outputNames = new String[1];

      switch (platformType) {
         case WINDOWS:
            inputNames[0] = "MIDIIN3 (LPProMK3 MIDI)";
            inputNames[1] = "LPProMK3 MIDI";
            outputNames[0] = "MIDIOUT3 (LPProMK3 MIDI)";
            break;
         case MAC:
         case LINUX:
            inputNames[0] = "Launchpad Pro MK3 LPProMK3 DAW";
            inputNames[1] = "Launchpad Pro MK3 LPProMK3 MIDI";
            outputNames[0] = "Launchpad Pro MK3 LPProMK3 DAW";
            break;
      }

      list.add(inputNames, outputNames);
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/Novation/Launchpad Pro Mk3.pdf";
   }

   @Override
   public LaunchpadProMk3ControllerExtension createInstance(final ControllerHost host) {
      return new LaunchpadProMk3ControllerExtension(this, host);
   }
}
