package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mackie.ControllerConfig;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;

import java.util.List;
import java.util.UUID;

public class IconQconProXExtensionDefinition extends IconQconExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("d32bead3-83e4-4dc2-90f8-eb3a1a847b5d");

   private static final String DEVICE_NAME = "iCON Qcon Pro X";
   private static final int FW_MAJOR_VERSION = 2;
   private static final int FW_MINOR_VERSION_LOW = 6;
   private static final int FW_MINOR_VERSION_HIGH = 11;

   public IconQconProXExtensionDefinition() {
      this(0);
   }

   public IconQconProXExtensionDefinition(final int nrOfExtenders) {
      super();
      this.nrOfExtenders = nrOfExtenders;
      initNoteOverrides();
   }


   @Override
   protected List<String[]> getInPorts(final PlatformType platformType) {
      return getPorts(platformType, "X", FW_MAJOR_VERSION, FW_MINOR_VERSION_LOW, FW_MINOR_VERSION_HIGH);
   }

   @Override
   protected List<String[]> getOutPorts(final PlatformType platformType) {
      return getPorts(platformType, "X", FW_MAJOR_VERSION, FW_MINOR_VERSION_LOW, FW_MINOR_VERSION_HIGH);
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
