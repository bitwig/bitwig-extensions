package com.bitwig.extensions.controllers.akai.apcmk2;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class AkaiApcKeys25Definition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("3a0cea43-3fb4-4c0e-99d3-789106a03b1f");

   public AkaiApcKeys25Definition() {
   }

   @Override
   public String getName() {
      return "Akai APC Keys25 mk2";
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return "0.7";
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor() {
      return "Akai";
   }

   @Override
   public String getHardwareModel() {
      return "APC Keys25 mk2";
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
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[]{"MIDIIN2 (APC Key 25 mk2)", "APC Key 25 mk2"},
            new String[]{"MIDIOUT2 (APC Key 25 mk2)"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"APC Key 25 mk2 Control", "APC Key 25 mk2 Keys"},
            new String[]{"APC Key 25 mk2 Control"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"APC Key 25 mk2 Control", "APC Key 25 mk2 Keys"},
            new String[]{"APC Key 25 mk2 Control"});
      }
   }

   @Override
   public AkaiApcKeys25Extension createInstance(final ControllerHost host) {
      return new AkaiApcKeys25Extension(this, host, new ApcConfiguration(true, 5, 0x40, 0x52, 0x62));
   }

}
