package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.ControllerHost;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IconQconProG2ExtensionDefinition extends IconQconExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("22035e35-4266-47f7-a364-f9c7284c226d");

   private static final String DEVICE_NAME = "iCON Qcon Pro G2";
   private static final Map<BasicNoteOnAssignment, Integer> noteOverrides = new HashMap<>();


   public IconQconProG2ExtensionDefinition() {
      this(0);
   }

   public IconQconProG2ExtensionDefinition(final int nrOfExtenders) {
      this.nrOfExtenders = nrOfExtenders;
      inMidiPortNames = new String[nrOfExtenders + 1];
      outMidiPortNames = new String[nrOfExtenders + 1];
      inMidiPortNames[0] = "iCON QCON Pro G2 V1.00";
      outMidiPortNames[0] = "iCON QCON Pro G2 V1.00";
      for (int i = 1; i < nrOfExtenders + 1; i++) {
         inMidiPortNames[i] = String.format("iCON QCON Ex%d G2 V1.00", i);
         outMidiPortNames[i] = String.format("iCON QCON Ex%d G2 V1.00", i);
      }
      initNoteOverrides();
   }

   @Override
   public String getName() {
      if (nrOfExtenders == 0) {
         return IconQconProG2ExtensionDefinition.DEVICE_NAME;
      }
      return String.format("%s +%d EXTENDER", IconQconProG2ExtensionDefinition.DEVICE_NAME, nrOfExtenders);
   }

   @Override
   public UUID getId() {
      return IconQconProG2ExtensionDefinition.DRIVER_ID;
   }

   @Override
   public String getHardwareModel() {
      return "iCON Qcon Pro G2";
   }

   @Override
   public String getHelpFilePath() {
      return "";
   }

   @Override
   public String getSupportFolderPath() {
      return "";
   }

   @Override
   public MackieMcuProExtension createInstance(final ControllerHost host) {
      return new MackieMcuProExtension(this, host,
         new ControllerConfig(IconQconProG2ExtensionDefinition.noteOverrides, false, true, false), nrOfExtenders);
   }
}
