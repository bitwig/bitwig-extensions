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
   protected static final String SOFTWARE_VERSION = "1.0b";
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

   /**
    * Marks a given function to represented by a different button assignment.
    * {@code override(BasicNoteOnAssignment.GROUP, BasicNoteOnAssignment.REPLACE);}
    * states that the GROUP function is now bound to the REPLACE button, i.e. that note number.
    *
    * @param origFunction the original function
    * @param takeOver     the button assignment that now overrides the previous assignment.
    */
   protected void override(final BasicNoteOnAssignment origFunction, final BasicNoteOnAssignment takeOver) {
      noteOverrides.put(origFunction, takeOver.getNoteNo());
   }

   /**
    * Marks a given function to represented by a different button assignment and assigns the takeover function to a note
    * that has no button representation, thus making that function not available in the given implementation.
    * {@code override(BasicNoteOnAssignment.GROUP, BasicNoteOnAssignment.REPLACE);}
    * states that the GROUP function is now bound to the REPLACE button, i.e. that note number.
    *
    * @param origFunction the original function
    * @param takeOver     the button assignment that now overrides the previous assignment.
    */
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

   private static final int F1_ROW = 2;
   private static final int F2_ROW = 3;
   private static final int MAIN_OFF = 3;

   protected void initSimulationLayout(final SimulationLayout layout) {
      layout.add(BasicNoteOnAssignment.V_TRACK, "TRACK", 0, 0);
      layout.add(BasicNoteOnAssignment.V_SEND, "SEND", 1, 0);

      layout.add(BasicNoteOnAssignment.V_PAN, "PAN", 0, 1);
      layout.add(BasicNoteOnAssignment.V_PLUGIN, "DVCE", 1, 1);

      layout.add(BasicNoteOnAssignment.V_EQ, "EQ", 0, 2);
      layout.add(BasicNoteOnAssignment.V_INSTRUMENT, "INST", 1, 2);

      layout.add(BasicNoteOnAssignment.BANK_LEFT, "<B", 0, 4);
      layout.add(BasicNoteOnAssignment.BANK_RIGHT, "B>", 1, 4);
      layout.add(BasicNoteOnAssignment.TRACK_LEFT, "<T", 0, 5);
      layout.add(BasicNoteOnAssignment.TRACK_RIGHT, "T>", 1, 5);
      layout.add(BasicNoteOnAssignment.FLIP, "FLIP", 0, 6);
      layout.add(BasicNoteOnAssignment.GLOBAL_VIEW, "GL.VIEW", 1, 6);

      layout.add(BasicNoteOnAssignment.DISPLAY_NAME, "NM/V", MAIN_OFF, 0);
      layout.add(BasicNoteOnAssignment.DISPLAY_SMPTE, "SMPT", MAIN_OFF + 1, 0);

      layout.add(BasicNoteOnAssignment.F1, "F1", MAIN_OFF, F1_ROW);
      layout.add(BasicNoteOnAssignment.F2, "F2", MAIN_OFF + 1, F1_ROW);
      layout.add(BasicNoteOnAssignment.F3, "F3", MAIN_OFF + 2, F1_ROW);
      layout.add(BasicNoteOnAssignment.F4, "F4", MAIN_OFF + 3, F1_ROW);
      layout.add(BasicNoteOnAssignment.F5, "F5", MAIN_OFF + 4, F1_ROW);
      layout.add(BasicNoteOnAssignment.F6, "F6", MAIN_OFF + 5, F1_ROW);
      layout.add(BasicNoteOnAssignment.F7, "F7", MAIN_OFF + 6, F1_ROW);
      layout.add(BasicNoteOnAssignment.F8, "F8", MAIN_OFF + 7, F1_ROW);

      layout.add(BasicNoteOnAssignment.GV_MIDI_LF1, "NFX", MAIN_OFF, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_INPUTS_LF2, "AR/LN", MAIN_OFF + 1, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_AUDIO_LF3, "FILL", MAIN_OFF + 2, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_INSTRUMENT_LF4, "QUANT", MAIN_OFF + 3, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_AUX_LF5, "PBFw", MAIN_OFF + 4, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_BUSSES_LF6, "<ENT>", MAIN_OFF + 5, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_OUTPUTS_LF7, "NEW", MAIN_OFF + 6, F2_ROW);
      layout.add(BasicNoteOnAssignment.GV_USER_LF8, "STEP", MAIN_OFF + 7, F2_ROW);

      layout.add(BasicNoteOnAssignment.SHIFT, "SHIFT", MAIN_OFF, F2_ROW + 2);
      layout.add(BasicNoteOnAssignment.OPTION, "OPT", MAIN_OFF + 1, F2_ROW + 2);
      layout.add(BasicNoteOnAssignment.CONTROL, "CONTROL", MAIN_OFF, F2_ROW + 3);
      layout.add(BasicNoteOnAssignment.ALT, "ALT", MAIN_OFF + 1, F2_ROW + 3);

      layout.add(BasicNoteOnAssignment.AUTO_READ_OFF, "READ", MAIN_OFF + 2, F2_ROW + 2);
      layout.add(BasicNoteOnAssignment.AUTO_WRITE, "WRITE", MAIN_OFF + 3, F2_ROW + 2);
      layout.add(BasicNoteOnAssignment.TRIM, "TRIM", MAIN_OFF + 4, F2_ROW + 2);

      layout.add(BasicNoteOnAssignment.TOUCH, "TOUCH", MAIN_OFF + 2, F2_ROW + 3);
      layout.add(BasicNoteOnAssignment.LATCH, "LATCH", MAIN_OFF + 3, F2_ROW + 3);
      layout.add(BasicNoteOnAssignment.GROUP, "CLIP.L", MAIN_OFF + 4, F2_ROW + 3);

      layout.add(BasicNoteOnAssignment.SAVE, "SAVE", MAIN_OFF + 6, F2_ROW + 2);
      layout.add(BasicNoteOnAssignment.UNDO, "UNDO", MAIN_OFF + 7, F2_ROW + 2);
      layout.add(BasicNoteOnAssignment.CANCEL, "CANCEL", MAIN_OFF + 6, F2_ROW + 3);
      layout.add(BasicNoteOnAssignment.ENTER, "ENTER", MAIN_OFF + 7, F2_ROW + 3);

      layout.add(BasicNoteOnAssignment.MARKER, "MARKER", MAIN_OFF, F2_ROW + 5);
      layout.add(BasicNoteOnAssignment.NUDGE, "KEYS", MAIN_OFF + 1, F2_ROW + 5);
      layout.add(BasicNoteOnAssignment.CYCLE, "CYCLE", MAIN_OFF + 3, F2_ROW + 5);
      layout.add(BasicNoteOnAssignment.DROP, "P.IN", MAIN_OFF + 4, F2_ROW + 5);
      layout.add(BasicNoteOnAssignment.REPLACE, "P.OUT", MAIN_OFF + 5, F2_ROW + 5);
      layout.add(BasicNoteOnAssignment.CLICK, "CLICK", MAIN_OFF + 6, F2_ROW + 5);
      layout.add(BasicNoteOnAssignment.SOLO, "SOLO", MAIN_OFF + 7, F2_ROW + 5);

      layout.add(BasicNoteOnAssignment.REWIND, "<<", MAIN_OFF + 3, F2_ROW + 6);
      layout.add(BasicNoteOnAssignment.FFWD, ">>", MAIN_OFF + 4, F2_ROW + 6);
      layout.add(BasicNoteOnAssignment.STOP, "STP", MAIN_OFF + 5, F2_ROW + 6);
      layout.add(BasicNoteOnAssignment.PLAY, ">", MAIN_OFF + 6, F2_ROW + 6);
      layout.add(BasicNoteOnAssignment.RECORD, "Rec", MAIN_OFF + 7, F2_ROW + 6);

      layout.add(BasicNoteOnAssignment.CURSOR_LEFT, "<", MAIN_OFF, F2_ROW + 8);
      layout.add(BasicNoteOnAssignment.CURSOR_RIGHT, ">", MAIN_OFF + 2, F2_ROW + 8);
      layout.add(BasicNoteOnAssignment.CURSOR_UP, "^", MAIN_OFF + 1, F2_ROW + 7);
      layout.add(BasicNoteOnAssignment.CURSOR_DOWN, "v", MAIN_OFF + 1, F2_ROW + 9);
      layout.add(BasicNoteOnAssignment.ZOOM, "Zoom", MAIN_OFF + 1, F2_ROW + 8);
      layout.setJogWheelPos(20 * (MAIN_OFF + 4), 20 * (F2_ROW + 7));
   }
}
