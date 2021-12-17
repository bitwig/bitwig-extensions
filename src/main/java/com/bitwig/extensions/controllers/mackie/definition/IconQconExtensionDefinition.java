package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;

import java.util.ArrayList;
import java.util.List;

public class IconQconExtensionDefinition extends MackieMcuProExtensionDefinition {
   private static final String SOFTWARE_VERSION = "0.5";

   public void initNoteOverrides() {
      // DAW Mode Button => Launcher
      override(BasicNoteOnAssignment.GROUP, BasicNoteOnAssignment.GLOBAL_VIEW);
      // DVR Button => Keyboard Mode
      override(BasicNoteOnAssignment.NUDGE, BasicNoteOnAssignment.REPLACE);

      override(BasicNoteOnAssignment.CLIP_OVERDUB, BasicNoteOnAssignment.NUDGE);
      override(BasicNoteOnAssignment.GLOBAL_VIEW, BasicNoteOnAssignment.LATCH);

      override(BasicNoteOnAssignment.LATCH, BasicNoteOnAssignment.AUTO_WRITE);
      override(BasicNoteOnAssignment.AUTO_WRITE, BasicNoteOnAssignment.TOUCH);
      override(BasicNoteOnAssignment.TOUCH, BasicNoteOnAssignment.TRIM);

      overrideX(BasicNoteOnAssignment.DROP, BasicNoteOnAssignment.SAVE);
      override(BasicNoteOnAssignment.REPLACE, BasicNoteOnAssignment.UNDO);
      overrideX(BasicNoteOnAssignment.UNDO, BasicNoteOnAssignment.ENTER);
      override(BasicNoteOnAssignment.CLICK, BasicNoteOnAssignment.GROUP);
      overrideX(BasicNoteOnAssignment.STEP_SEQ, BasicNoteOnAssignment.SOLO);
   }

   protected List<String[]> getPorts(final PlatformType platformType, final String model, final int majorVersion,
                                     final int startVersion, final int endVersion) {
      final List<String[]> portList = new ArrayList<>();
      for (int version = startVersion; version < endVersion; version++) {
         final String versionString = String.format("%d.%02d", majorVersion, version);
         portList.add(getPortNames("iCON QCON Pro " + model + " V%s", model, versionString));
      }
      return portList;
   }

   protected String[] getPortNames(final String baseFormat, final String model, final String version) {
      final String[] portNames = new String[nrOfExtenders + 1];
      portNames[0] = String.format(baseFormat, version);
      for (int extIndex = 1; extIndex < nrOfExtenders + 1; extIndex++) {
         portNames[extIndex] = String.format("iCON QCON EX%d %s V%s", extIndex, model, version);
      }
      return portNames;
   }

   @Override
   public String getVersion() {
      return IconQconExtensionDefinition.SOFTWARE_VERSION;
   }

   @Override
   public String getHardwareVendor() {
      return "iCON";
   }

   @Override
   public int getRequiredAPIVersion() {
      return MackieMcuProExtensionDefinition.MCU_API_VERSION;
   }

   @Override
   public String getSupportFolderPath() {
      return "";
   }

   @Override
   public String getHelpFilePath() {
      return "";
   }
}
