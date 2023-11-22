package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class MaschineMikroMk3ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("fa145533-5f45-4e19-77ad-0de77ffa2d6f");

   @Override
   public String getName() {
      return "Maschine Mikro Mk3";
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
      return "Native Instruments";
   }

   @Override
   public String getHardwareModel() {
      return "Maschine Mikro Mk3";
   }

   @Override
   public int getRequiredAPIVersion() {
      return 18;
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
         list.add(new String[]{"Maschine Mikro MK3"}, new String[]{"Maschine Mikro MK3"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"Maschine Mikro MK3"}, new String[]{"Maschine Mikro MK3"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"Maschine Mikro MK3"}, new String[]{"Maschine Mikro MK3"});
      }
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/Native Instruments/Maschine Mikro Mk3/Maschine Mikro Mk3.pdf";
   }

   @Override
   public String getSupportFolderPath() {
      return "Controllers/Native Instruments/Maschine Mikro MK3";
   }

   @Override
   public MaschineMikroExtension createInstance(final ControllerHost host) {
      return new MaschineMikroExtension(this, host);
   }
}
