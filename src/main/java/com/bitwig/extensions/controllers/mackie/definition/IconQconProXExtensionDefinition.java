package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mackie.ControllerConfig;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;

import java.util.UUID;

public class IconQconProXExtensionDefinition extends IconQconExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("d32bead3-83e4-4dc2-90f8-eb3a1a847b5d");

   private static final String DEVICE_NAME = "iCON Qcon Pro X";

   public IconQconProXExtensionDefinition() {
      this(0);
   }

   public IconQconProXExtensionDefinition(final int nrOfExtenders) {
      super();
      this.nrOfExtenders = nrOfExtenders;
      inMidiPortNames = new String[nrOfExtenders + 1];
      outMidiPortNames = new String[nrOfExtenders + 1];
      inMidiPortNames[0] = "iCON QCON Pro X V2.07";
      outMidiPortNames[0] = "iCON QCON Pro X V2.07";
      for (int i = 1; i < nrOfExtenders + 1; i++) {
         inMidiPortNames[i] = String.format("iCON QCON Ex%d G2 V1.00", i);
         outMidiPortNames[i] = String.format("iCON QCON Ex%d G2 V1.00", i);
      }
      initNoteOverrides();
   }

   @Override
   public String getName() {
      if (nrOfExtenders == 0) {
         return IconQconProXExtensionDefinition.DEVICE_NAME;
      }
      return String.format("%s +%d EXTENDER", IconQconProXExtensionDefinition.DEVICE_NAME, nrOfExtenders);
   }

   @Override
   public UUID getId() {
      return IconQconProXExtensionDefinition.DRIVER_ID;
   }

   @Override
   public String getHardwareModel() {
      return "iCON Qcon Pro X";
   }

   @Override
   public MackieMcuProExtension createInstance(final ControllerHost host) {
      return new MackieMcuProExtension(this, host, new ControllerConfig(noteOverrides, true, true, true),
         nrOfExtenders);
   }
}
