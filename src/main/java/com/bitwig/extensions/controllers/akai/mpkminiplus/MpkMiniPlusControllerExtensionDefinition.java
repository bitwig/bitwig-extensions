package com.bitwig.extensions.controllers.akai.mpkminiplus;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class MpkMiniPlusControllerExtensionDefinition extends ControllerExtensionDefinition {
   private final static UUID ID = UUID.fromString("429d64c5-0da1-475d-aba7-e990b9bbc2e4");

   @Override
   public String getHardwareVendor() {
      return "Akai";
   }

   @Override
   public String getHardwareModel() {
      return "MPK mini plus";
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
      final String inputNames[] = new String[1];
      final String outputNames[] = new String[1];

      switch (platformType) {
         case LINUX:
            inputNames[0] = "MPK mini 3 MIDI 1";
            outputNames[0] = "MPK mini 3 MIDI 1";
            break;

         case WINDOWS:
         case MAC:
            inputNames[0] = "MPK mini Plus";
            outputNames[0] = "MPK mini Plus";
            break;
      }

      list.add(inputNames, outputNames);
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host) {
      return new MpkMiniPlusControllerExtension(this, host);
   }

   @Override
   public String getName() {
      return "MPK mini plus";
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

}
