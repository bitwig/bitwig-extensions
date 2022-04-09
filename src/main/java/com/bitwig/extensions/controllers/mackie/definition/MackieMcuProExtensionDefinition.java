package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;

import java.util.*;

public class MackieMcuProExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("fa145533-5f45-4e19-81ad-1de77ffa2dab");

   protected static final int MCU_API_VERSION = 16;
   protected static final String SOFTWARE_VERSION = "1.0a";
   private static final String DEVICE_NAME = "Mackie Control";

   protected int nrOfExtenders;
   protected final Map<BasicNoteOnAssignment, Integer> noteOverrides = new HashMap<>();
   private int unusedNoteNo = 113;

   public MackieMcuProExtensionDefinition() {
      this(0);
   }

   public MackieMcuProExtensionDefinition(final int nrOfExtenders) {
      this.nrOfExtenders = nrOfExtenders;
   }

   protected List<String[]> getInPorts(final PlatformType platformType) {
      final String[] inPortNames = new String[nrOfExtenders + 1];
      inPortNames[0] = "MCU Pro USB v3.1";
      for (int i = 1; i < nrOfExtenders + 1; i++) {
         inPortNames[i] = String.format("MIDIIN%d (MCU Pro USB v3.1)", i);
      }
      final List<String[]> portList = new ArrayList<>();
      portList.add(inPortNames);
      return portList;
   }

   protected List<String[]> getOutPorts(final PlatformType platformType) {
      final String[] outPortNames = new String[nrOfExtenders + 1];
      outPortNames[0] = "MCU Pro USB v3.1";
      for (int i = 1; i < nrOfExtenders + 1; i++) {
         outPortNames[i] = String.format("MIDIOUT%d (MCU Pro USB v3.1)", i);
      }
      final List<String[]> portList = new ArrayList<>();
      portList.add(outPortNames);
      return portList;
   }

   protected void override(final BasicNoteOnAssignment origFunction, final BasicNoteOnAssignment takeOver) {
      noteOverrides.put(origFunction, takeOver.getNoteNo());
   }

   protected void overrideX(final BasicNoteOnAssignment origFunction, final BasicNoteOnAssignment takeOver) {
      noteOverrides.put(origFunction, takeOver.getNoteNo());
      noteOverrides.put(takeOver, unusedNoteNo++);
   }

   @Override
   public String getName() {
      if (nrOfExtenders == 0) {
         return MackieMcuProExtensionDefinition.DEVICE_NAME;
      }
      return String.format("%s +%d EXTENDER", MackieMcuProExtensionDefinition.DEVICE_NAME, nrOfExtenders);
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return MackieMcuProExtensionDefinition.SOFTWARE_VERSION;
   }

   @Override
   public UUID getId() {
      return MackieMcuProExtensionDefinition.DRIVER_ID;
   }

   @Override
   public String getHardwareVendor() {
      return "Mackie";
   }

   @Override
   public String getHardwareModel() {
      return "Mackie Control";
   }

   @Override
   public int getRequiredAPIVersion() {
      return MackieMcuProExtensionDefinition.MCU_API_VERSION;
   }

   @Override
   public int getNumMidiInPorts() {
      return nrOfExtenders + 1;
   }

   @Override
   public int getNumMidiOutPorts() {
      return nrOfExtenders + 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      final List<String[]> inPorts = getInPorts(platformType);
      final List<String[]> outPorts = getOutPorts(platformType);

      for (int i = 0; i < inPorts.size(); i++) {
         list.add(inPorts.get(i), outPorts.get(i));
      }
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/Mackie/Mackie Control/Mackie Control Universal.pdf";
   }

   @Override
   public String getSupportFolderPath() {
      return "";
   }

   @Override
   public MackieMcuProExtension createInstance(final ControllerHost host) {
      final ControllerConfig controllerConfig = new ControllerConfig(false) //
         .setHasDedicateVu(false)//
         .setHasMasterVu(false);
      initSimulationLayout(controllerConfig.getSimulationLayout());

      return new MackieMcuProExtension(this, host, //
         controllerConfig, nrOfExtenders);
   }

   private static final int F1_ROW = 1;
   private static final int F2_ROW = 2;

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
