package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class LaunchControlXlMk3ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("cdee004a-1503-487c-bc13-a8311bf1724b");

   public LaunchControlXlMk3ExtensionDefinition() {
   }

   @Override
   public String getName() {
      return "Launch Control XL Mk3";
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
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
      return "Novation";
   }

   @Override
   public String getHardwareModel() {
      return "Launch Control XL Mk3";
   }

   @Override
   public int getRequiredAPIVersion() {
      return 24;
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/Novation/Launch Control XL Mk3.pdf";
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
         list.add(new String[]{"MIDIIN2 (LCXL3 1 MIDI)"}, new String[]{"MIDIOUT2 (LCXL3 1 MIDI)"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"LCXL3 1 DAW Out"}, new String[]{"LCXL3 1 DAW In"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"LCXL3 1 LCXL3 1 DAW Out"}, new String[]{"LCXL3 1 LCXL3 1 DAW In"});
      }
   }

   @Override
   public LaunchControlXlMk3Extension createInstance(final ControllerHost host) {
      return new LaunchControlXlMk3Extension(this, host);
   }
}
