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
      override(BasicNoteOnAssignment.GROUP, BasicNoteOnAssignment.MARKER);
      override(BasicNoteOnAssignment.MARKER, BasicNoteOnAssignment.NUDGE);
      override(BasicNoteOnAssignment.OPTION, BasicNoteOnAssignment.CANCEL);
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
         false, true) //
         .setHasDedicateVu(true) //
         .setFunctionSectionLayered(true) //
         .setUseClearDuplicateModifiers(true);
      initSimulationLayout(controllerConfig.getSimulationLayout());
      return new MackieMcuProExtension(this, host, //
         controllerConfig, nrOfExtenders);
   }


   @Override
   public String getHelpFilePath() {
      return "Controllers/iCon/iCon QCon Pro G2.pdf";
   }


   @Override
   protected void initSimulationLayout(final SimulationLayout layout) {
      super.initSimulationLayout(layout);
      layout.add(BasicNoteOnAssignment.FLIP, "FLIP", -1, 1);
      layout.add(BasicNoteOnAssignment.TRACK_LEFT, "<T", -1, 2);
      layout.add(BasicNoteOnAssignment.TRACK_RIGHT, "T>", -1, 3);
      layout.add(BasicNoteOnAssignment.BANK_LEFT, "<B", -1, 4);
      layout.add(BasicNoteOnAssignment.BANK_RIGHT, "B>", -1, 5);

      layout.add(BasicNoteOnAssignment.DISPLAY_NAME, "NM/V", 3, 0);
      layout.add(BasicNoteOnAssignment.DISPLAY_SMPTE, "SMPT", 4, 0);
      layout.add(BasicNoteOnAssignment.ALT, "CLR", 5, 0);
      layout.add(BasicNoteOnAssignment.CONTROL, "DUP", 6, 0);
      layout.add(BasicNoteOnAssignment.UNDO, "UNDO", 7, 0);

      layout.add(BasicNoteOnAssignment.GV_USER_LF8_G2, "CANCEL", 7, F2_ROW);

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

      layout.add(BasicNoteOnAssignment.NUDGE, "KEY/P.IN", 2, 5);
      layout.add(BasicNoteOnAssignment.STEP_SEQ, "SP/P.OU", 3, 5);
      layout.add(BasicNoteOnAssignment.SHIFT, "SHIFT", 4, 5);
      layout.add(BasicNoteOnAssignment.REWIND, "<<", 5, 5);
      layout.add(BasicNoteOnAssignment.CYCLE, "CYCLE", 6, 5);
      layout.add(BasicNoteOnAssignment.FFWD, ">>", 7, 5);

      layout.add(BasicNoteOnAssignment.GROUP, "CLIPS", 2, 6);
      layout.add(BasicNoteOnAssignment.MARKER, "MARKER", 3, 6);
      layout.add(BasicNoteOnAssignment.OPTION, "OPT", 4, 6);
      layout.add(BasicNoteOnAssignment.RECORD, "Rec", 5, 6);
      layout.add(BasicNoteOnAssignment.PLAY, ">", 6, 6);
      layout.add(BasicNoteOnAssignment.STOP, "STP", 7, 6);

      layout.add(BasicNoteOnAssignment.CURSOR_LEFT, "<", 0, 9);
      layout.add(BasicNoteOnAssignment.CURSOR_RIGHT, ">", 2, 9);
      layout.add(BasicNoteOnAssignment.CURSOR_UP, "^", 1, 8);
      layout.add(BasicNoteOnAssignment.CURSOR_DOWN, "v", 1, 10);
      layout.add(BasicNoteOnAssignment.ZOOM, "Zoom", 1, 9);
      layout.setJogWheelPos(20 * 4, 20 * 8);
   }
}
