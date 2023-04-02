package com.bitwig.extensions.controllers.akai.apcmk2;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class AkaiApcMiniDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("e1c116bb-71a4-4878-bfec-323d49b34107");

   public AkaiApcMiniDefinition() {
   }

   @Override
   public String getName() {
      return "Akai APC Mini mk2";
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
      return "APC Mini mk2";
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
         list.add(new String[]{"APC mini mk2", "MIDIIN2 (APC mini mk2)"}, new String[]{"APC mini mk2"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"APC mini mk2 Control", "APC mini mk2 Notes"}, new String[]{"APC Mini mk2 Control"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"APC Mini mk2 Control", "APC mini mk2 Notes"}, new String[]{"APC Mini mk2 Control"});
      }
   }

   @Override
   public AkaiApcMiniExtension createInstance(final ControllerHost host) {
      return new AkaiApcMiniExtension(this, host, new ApcConfiguration(false, 8, 0x64, 0x70, 0x7A));
   }

}
