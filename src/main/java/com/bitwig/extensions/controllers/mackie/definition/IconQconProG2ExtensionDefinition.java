package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;

import java.util.List;
import java.util.UUID;

public class IconQconProG2ExtensionDefinition extends IconQconExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("22035e35-4266-47f7-a364-f9c7284c226d");

   private static final String DEVICE_NAME = "iCON Qcon Pro G2";
   private static final int MAJOR_VERSION = 1;
   private static final int MINOR_VERSION_LOW = 9;
   private static final int MINOR_VERSION_HIGH = 14;

   @Override
   public void initNoteOverrides() {
      // DVR Button => Keyboard Mode
      overrideX(BasicNoteOnAssignment.NUDGE, BasicNoteOnAssignment.SAVE);
      // STEP Sequencer
      overrideX(BasicNoteOnAssignment.STEP_SEQ, BasicNoteOnAssignment.UNDO);

      override(BasicNoteOnAssignment.SHIFT, BasicNoteOnAssignment.ENTER);
      // ALT is Duplicate
      override(BasicNoteOnAssignment.ALT, BasicNoteOnAssignment.OPTION);
      // CONTROL is Clear
      override(BasicNoteOnAssignment.CONTROL, BasicNoteOnAssignment.SHIFT);

      // Automation Section
      override(BasicNoteOnAssignment.AUTO_WRITE, BasicNoteOnAssignment.TOUCH);
      override(BasicNoteOnAssignment.GLOBAL_VIEW, BasicNoteOnAssignment.LATCH);
      override(BasicNoteOnAssignment.LATCH, BasicNoteOnAssignment.AUTO_WRITE);
      override(BasicNoteOnAssignment.TOUCH, BasicNoteOnAssignment.TRIM);

      // Undo
      override(BasicNoteOnAssignment.UNDO, BasicNoteOnAssignment.CONTROL);
      override(BasicNoteOnAssignment.CLICK, BasicNoteOnAssignment.GROUP);

      // Lower left row Launcher | Marker | Option
      override(BasicNoteOnAssignment.GROUP, BasicNoteOnAssignment.REPLACE);
      override(BasicNoteOnAssignment.MARKER, BasicNoteOnAssignment.CLICK);
      override(BasicNoteOnAssignment.OPTION, BasicNoteOnAssignment.SOLO);
   }

   public IconQconProG2ExtensionDefinition() {
      this(0);
   }

   public IconQconProG2ExtensionDefinition(final int nrOfExtenders) {
      this.nrOfExtenders = nrOfExtenders;
      initNoteOverrides();
   }

   @Override
   protected List<String[]> getInPorts(final PlatformType platformType) {
      return getPorts(platformType, "G2", MAJOR_VERSION, MINOR_VERSION_LOW, MINOR_VERSION_HIGH);
   }

   @Override
   protected List<String[]> getOutPorts(final PlatformType platformType) {
      return getPorts(platformType, "G2", MAJOR_VERSION, MINOR_VERSION_LOW, MINOR_VERSION_HIGH);
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
   public MackieMcuProExtension createInstance(final ControllerHost host) {
      final ControllerConfig controllerConfig = new ControllerConfig(noteOverrides, ManufacturerType.ICON, SubType.G2,
         false) //
         .setHasDedicateVu(true) //
         .setFunctionSectionLayered(true) //
         .setUseClearDuplicateModifiers(true);
      initSimuLayout(controllerConfig.getSimulationLayout());
      return new MackieMcuProExtension(this, host, //
         controllerConfig, nrOfExtenders);
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/Icon/Mackie Control/Mackie Control Universal.pdf";
   }

}
