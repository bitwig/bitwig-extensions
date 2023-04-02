package com.bitwig.extensions.controllers.novation.launchpadmini3;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class LaunchPadXExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("6da975db-1967-47d5-8b5f-3ee29e84e0c7");

   public LaunchPadXExtensionDefinition() {
   }

   @Override
   public String getName() {
      return "Launchpad X";
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
      return "Novation";
   }

   @Override
   public String getHardwareModel() {
      return "Launchpad X";
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


//    Added MIDI IN: LPX MIDI
//    Added MIDI IN: MIDIIN2 (LPX MIDI)
//    Added MIDI OUT: LPX MIDI
//    Added MIDI OUT: MIDIOUT2 (LPX MIDI)

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      final String[] inputNames = new String[2];
      final String[] outputNames = new String[1];

      switch (platformType) {
         case LINUX:
         case WINDOWS:
            inputNames[0] = "LPX MIDI";
            inputNames[1] = "MIDIIN2 (LPX MIDI)";
            outputNames[0] = "LPX MIDI";
            break;
         case MAC:
            inputNames[0] = "Launchpad X MK3 LPX DAW Out";
            inputNames[1] = "Launchpad X MK3 LPX MIDI Out";
            outputNames[0] = "Launchpad X MK3 LPX DAW In";
            break;
      }

      list.add(inputNames, outputNames);
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host) {
      return new LaunchpadXControllerExtension(this, host);
   }
}
