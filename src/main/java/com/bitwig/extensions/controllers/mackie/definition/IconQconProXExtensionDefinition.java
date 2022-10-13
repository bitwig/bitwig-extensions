package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
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
      final ControllerConfig controllerConfig = new ControllerConfig(noteOverrides, ManufacturerType.ICON,
         SubType.PRO_X, true, true) //
         .setHasMasterVu(true) //
         .setHasDedicateVu(true) //
         .setFunctionSectionLayered(true) //
         .setUseClearDuplicateModifiers(true);
      initSimulationLayout(controllerConfig.getSimulationLayout());
      return new MackieMcuProExtension(this, host, //
         controllerConfig, nrOfExtenders);
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/iCon/iCon QCon Pro X.pdf";
   }


   @Override
   protected void initSimulationLayout(final SimulationLayout layout) {
      super.initSimulationLayout(layout);
      layout.add(BasicNoteOnAssignment.SHIFT, "SHIFT", 4, 5);
      layout.add(BasicNoteOnAssignment.OPTION, "OPT", 5, 5);
      layout.add(BasicNoteOnAssignment.ALT, "DEL", 4, 6);
      layout.add(BasicNoteOnAssignment.CONTROL, "DUP", 5, 6);

      layout.add(BasicNoteOnAssignment.DISPLAY_NAME, "NM/V", 1, 0);
      layout.add(BasicNoteOnAssignment.DISPLAY_SMPTE, "SMPT", 2, 0);
      layout.add(BasicNoteOnAssignment.GROUP, "LNCH", 6, 0);
      layout.add(BasicNoteOnAssignment.NUDGE, "KEY", 4, 0);
      layout.add(BasicNoteOnAssignment.STEP_SEQ, "STEP", 5, 0);


      layout.add(BasicNoteOnAssignment.V_TRACK, "TRACK", 2, 3);
      layout.add(BasicNoteOnAssignment.V_SEND, "SEND", 3, 3);
      layout.add(BasicNoteOnAssignment.V_PAN, "PAN", 4, 3);
      layout.add(BasicNoteOnAssignment.V_PLUGIN, "DVCE", 5, 3);
      layout.add(BasicNoteOnAssignment.V_EQ, "EQ", 6, 3);
      layout.add(BasicNoteOnAssignment.V_INSTRUMENT, "INST", 7, 3);

      layout.add(BasicNoteOnAssignment.AUTO_READ_OFF, "READ", 2, 4);
      layout.add(BasicNoteOnAssignment.LATCH, "LATCH", 3, 4);
      layout.add(BasicNoteOnAssignment.TOUCH, "TOUCH", 4, 4);
      layout.add(BasicNoteOnAssignment.AUTO_WRITE, "WRITE", 5, 4);
      layout.add(BasicNoteOnAssignment.GLOBAL_VIEW, "GROUPS", 6, 4);
      layout.add(BasicNoteOnAssignment.CLICK, "CLICK", 7, 4);

      layout.add(BasicNoteOnAssignment.DROP, "P.IN", 6, 5);
      layout.add(BasicNoteOnAssignment.REPLACE, "P.OUT", 7, 5);
      layout.add(BasicNoteOnAssignment.CANCEL, "CANCEL", 6, 6);
      layout.add(BasicNoteOnAssignment.UNDO, "UNDO", 7, 6);
      layout.add(BasicNoteOnAssignment.MARKER, "MARKER", 6, 7);
      layout.add(BasicNoteOnAssignment.CLIP_OVERDUB, "OVR", 7, 7);

      layout.add(BasicNoteOnAssignment.REWIND, "<<", 2, 9);
      layout.add(BasicNoteOnAssignment.FFWD, ">>", 3, 9);
      layout.add(BasicNoteOnAssignment.CYCLE, "CYCLE", 4, 9);
      layout.add(BasicNoteOnAssignment.PLAY, ">", 6, 9);
      layout.add(BasicNoteOnAssignment.RECORD, "Rec", 7, 9);
      layout.add(BasicNoteOnAssignment.STOP, "STP", 5, 9);

      layout.add(BasicNoteOnAssignment.CURSOR_LEFT, "<", 0, 11);
      layout.add(BasicNoteOnAssignment.CURSOR_RIGHT, ">", 2, 11);
      layout.add(BasicNoteOnAssignment.CURSOR_UP, "^", 1, 10);
      layout.add(BasicNoteOnAssignment.CURSOR_DOWN, "v", 1, 12);
      layout.add(BasicNoteOnAssignment.ZOOM, "Zoom", 1, 11);
      layout.add(BasicNoteOnAssignment.BANK_LEFT, "<B", 4, 8);
      layout.add(BasicNoteOnAssignment.BANK_RIGHT, "B>", 5, 8);
      layout.add(BasicNoteOnAssignment.TRACK_LEFT, "<T", 6, 8);
      layout.add(BasicNoteOnAssignment.TRACK_RIGHT, "T>", 7, 8);
      layout.add(BasicNoteOnAssignment.FLIP, "FLIP", 5, 7);
      layout.setJogWheelPos(20 * 4, 20 * 10);
   }
}
