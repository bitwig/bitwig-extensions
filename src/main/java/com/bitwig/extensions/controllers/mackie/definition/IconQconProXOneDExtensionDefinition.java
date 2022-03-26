package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;

import java.util.List;
import java.util.UUID;

public class IconQconProXOneDExtensionDefinition extends IconQconExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("d32bead3-83e4-4dc2-90f8-eb3a1a847b7e");

   private static final String DEVICE_NAME = "*iCON Qcon Pro X";
   private static final int FW_MAJOR_VERSION = 2;
   private static final int FW_MINOR_VERSION_LOW = 6;
   private static final int FW_MINOR_VERSION_HIGH = 11;

   public IconQconProXOneDExtensionDefinition() {
      this(0);
   }

   public IconQconProXOneDExtensionDefinition(final int nrOfExtenders) {
      super();
      this.nrOfExtenders = nrOfExtenders;
      initNoteOverrides();
   }

   @Override
   public void initNoteOverrides() {
      // DAW Mode Button => Launcher
      override(BasicNoteOnAssignment.GROUP, BasicNoteOnAssignment.GLOBAL_VIEW);
      // DVR Button => Keyboard Mode
      override(BasicNoteOnAssignment.NUDGE, BasicNoteOnAssignment.REPLACE);
      // STEP Sequencer
      overrideX(BasicNoteOnAssignment.STEP_SEQ, BasicNoteOnAssignment.SOLO);

      // Clip Overdub
      override(BasicNoteOnAssignment.CLIP_OVERDUB, BasicNoteOnAssignment.NUDGE);

      // Automation Section
      override(BasicNoteOnAssignment.AUTO_WRITE, BasicNoteOnAssignment.TOUCH);
      override(BasicNoteOnAssignment.GLOBAL_VIEW, BasicNoteOnAssignment.LATCH);
      override(BasicNoteOnAssignment.LATCH, BasicNoteOnAssignment.AUTO_WRITE);
      override(BasicNoteOnAssignment.TOUCH, BasicNoteOnAssignment.TRIM);

      // Punch In
      overrideX(BasicNoteOnAssignment.DROP, BasicNoteOnAssignment.SAVE);
      // Punch Out
      override(BasicNoteOnAssignment.REPLACE, BasicNoteOnAssignment.UNDO);

      // Undo
      overrideX(BasicNoteOnAssignment.UNDO, BasicNoteOnAssignment.ENTER);
      // Metronome
      override(BasicNoteOnAssignment.CLICK, BasicNoteOnAssignment.GROUP);
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
         return IconQconProXOneDExtensionDefinition.DEVICE_NAME;
      }
      return String.format("%s +%d EXTENDER", IconQconProXOneDExtensionDefinition.DEVICE_NAME, nrOfExtenders);
   }

   @Override
   public UUID getId() {
      return IconQconProXOneDExtensionDefinition.DRIVER_ID;
   }

   @Override
   public String getHardwareModel() {
      return "<1D> iCON Qcon Pro X";
   }

   @Override
   public MackieMcuProExtension createInstance(final ControllerHost host) {
      return new MackieMcuProExtension(this, host, //
         new ControllerConfig(noteOverrides, ManufacturerType.ICON, SubType.PRO_X, false) //
            .setHasDedicateVu(true) //
            .setHasMasterVu(true) //
            .setFunctionSectionLayered(true) //
            .setUseClearDuplicateModifiers(true), nrOfExtenders);
   }
}
