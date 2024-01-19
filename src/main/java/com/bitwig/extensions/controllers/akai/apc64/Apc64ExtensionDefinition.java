package com.bitwig.extensions.controllers.akai.apc64;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class Apc64ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("bc2cae98-42ed-45ef-a191-aef1dfd4e00d");

   public Apc64ExtensionDefinition() {
   }

   @Override
   public String getName() {
      return "APC64";
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
      return "Akai";
   }

   @Override
   public String getHardwareModel() {
      return "APC64";
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
      return "Controllers/Akai/AKAI APC64.pdf";
   }

   // MIDIOUT2 (APC64)
   // MIDIIN2 (APC64)
   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[]{"APC64", "MIDIIN2 (APC64)"}, new String[]{"APC64"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"APC64 DAW (APC64)", "APC64 Notes (APC64)"}, new String[]{"APC64 DAW (APC64)"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"APC64 DAW (APC64)", "APC64 Notes (APC64)"}, new String[]{"APC64 DAW (APC64)"});
      }
   }

   @Override
   public Apc64Extension createInstance(final ControllerHost host) {
      return new Apc64Extension(this, host);
   }

}
