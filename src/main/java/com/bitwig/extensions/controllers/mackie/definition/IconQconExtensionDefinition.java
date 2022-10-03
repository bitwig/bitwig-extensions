package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;

import java.util.ArrayList;
import java.util.List;

public abstract class IconQconExtensionDefinition extends MackieMcuProExtensionDefinition {
   private static final String SOFTWARE_VERSION = "1.0a";

   public abstract void initNoteOverrides();

   protected static final int F1_ROW = 1;
   protected static final int F2_ROW = 2;

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
      return "Controllers/Mackie/Mackie Control/Mackie Control Universal.pdf";
   }

   @Override
   protected void initSimulationLayout(final SimulationLayout layout) {
      layout.add(BasicNoteOnAssignment.F1, "F1", 0, F1_ROW);
      layout.add(BasicNoteOnAssignment.F2, "F2", 1, F1_ROW);
      layout.add(BasicNoteOnAssignment.F3, "F3", 2, F1_ROW);
      layout.add(BasicNoteOnAssignment.F4, "F4", 3, F1_ROW);
      layout.add(BasicNoteOnAssignment.F5, "F5", 4, F1_ROW);
      layout.add(BasicNoteOnAssignment.F6, "F6", 5, F1_ROW);
      layout.add(BasicNoteOnAssignment.F7, "F7", 6, F1_ROW);
      layout.add(BasicNoteOnAssignment.F8, "F8", 7, F1_ROW);
      layout.add(BasicNoteOnAssignment.GV_MIDI_LF1, "NFX", 0, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_INPUTS_LF2, "AR/LN", 1, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_AUDIO_LF3, "FILL", 2, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_INSTRUMENT_LF4, "QUANT", 3, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_AUX_LF5, "PBFw", 4, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_BUSSES_LF6, "<ENT>", 5, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_OUTPUTS_LF7, "NEW", 6, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_USER_LF8, "-", 7, F2_ROW);
   }
}
