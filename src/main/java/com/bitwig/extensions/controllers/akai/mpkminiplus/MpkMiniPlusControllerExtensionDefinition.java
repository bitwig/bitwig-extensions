package com.bitwig.extensions.controllers.akai.mpkminiplus;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MpkMiniPlusControllerExtensionDefinition extends ControllerExtensionDefinition {
   private final static UUID ID = UUID.fromString("429d64c5-0da1-475d-aba7-e990b9bbc2e4");
   private final static String[] PORT_VARIANTS = {"Anschluss","Port","Puerto","Porto"};

   @Override
   public String getHardwareVendor() {
      return "Akai";
   }

   @Override
   public String getHardwareModel() {
      return "MPK Mini plus";
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
      List<String[]> portNames = new ArrayList<>();

      switch (platformType) {
         case LINUX:
            portNames.add(new String[]{"MPK mini Plus MIDI 1"});
            break;
         case WINDOWS:
            portNames.add(new String[]{"MPK mini Plus"});
            break;
         case MAC:
            for(int i=0;i<PORT_VARIANTS.length;i++) {
               String port = "MPK mini Plus %s 1".formatted(PORT_VARIANTS[i]);
               portNames.add(new String[]{port});
            }
            break;
      }
   
      for(int i=0;i<portNames.size();i++) {
         list.add(portNames.get(i), portNames.get(i));
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host) {
      return new MpkMiniPlusControllerExtension(this, host);
   }

   @Override
   public String getName() {
      return "MPK Mini plus";
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
      return ID;
   }

   @Override
   public int getRequiredAPIVersion() {
      return 18;
   }

   @Override
   public String getSupportFolderPath() {
      return "Controllers/Akai/mpkminiplus";
   }
}
