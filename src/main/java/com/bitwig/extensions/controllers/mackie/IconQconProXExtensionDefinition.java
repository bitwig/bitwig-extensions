package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IconQconProXExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("d32bead3-83e4-4dc2-90f8-eb3a1a847b5d");

   private static final int MCU_API_VERSION = 15;
   private static final String SOFTWARE_VERSION = "0.9";
   private static final String DEVICE_NAME = "iCON Qcon Pro X";
   private static final Map<BasicNoteOnAssignment, Integer> noteOverrides = new HashMap<>();
   private static int unusedNoteNo = 113;

   static {
      // DAW Mode Button => Launcher
      overrideX(BasicNoteOnAssignment.GROUP, BasicNoteOnAssignment.GLOBAL_VIEW);
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
      //overrideX(BasicNoteOnAssignment.xxx, BasicNoteOnAssignment.LATCH);
   }

   private static void override(final BasicNoteOnAssignment origFunction, final BasicNoteOnAssignment takeOver) {
      noteOverrides.put(origFunction, takeOver.getNoteNo());
   }

   private static void overrideX(final BasicNoteOnAssignment origFunction, final BasicNoteOnAssignment takeOver) {
      noteOverrides.put(origFunction, takeOver.getNoteNo());
      noteOverrides.put(takeOver, unusedNoteNo++);
   }

   protected int nrOfExtenders;
   protected String[] inMidiPortNames;
   protected String[] outMidiPortNames;

   public IconQconProXExtensionDefinition() {
      this(0);
   }

   public IconQconProXExtensionDefinition(final int nrOfExtenders) {
      this.nrOfExtenders = nrOfExtenders;
      inMidiPortNames = new String[nrOfExtenders + 1];
      outMidiPortNames = new String[nrOfExtenders + 1];
      inMidiPortNames[0] = "iCON QCON Pro X V2.07";
      outMidiPortNames[0] = "iCON QCON Pro X V2.07";
      for (int i = 1; i < nrOfExtenders + 1; i++) {
         inMidiPortNames[i] = String.format("iCON QCON Ex%d G2 V1.00", i);
         outMidiPortNames[i] = String.format("iCON QCON Ex%d G2 V1.00", i);
      }
   }

   @Override
   public String getName() {
      if (nrOfExtenders == 0) {
         return IconQconProXExtensionDefinition.DEVICE_NAME;
      }
      return String.format("%s +%d EXTENDER", IconQconProXExtensionDefinition.DEVICE_NAME, nrOfExtenders);
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return IconQconProXExtensionDefinition.SOFTWARE_VERSION;
   }

   @Override
   public UUID getId() {
      return IconQconProXExtensionDefinition.DRIVER_ID;
   }

   @Override
   public String getHardwareVendor() {
      return "iCON";
   }

   @Override
   public String getHardwareModel() {
      return "iCON Qcon Pro X";
   }

   @Override
   public int getRequiredAPIVersion() {
      return IconQconProXExtensionDefinition.MCU_API_VERSION;
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
      if (platformType == PlatformType.WINDOWS) {
         list.add(inMidiPortNames, outMidiPortNames);
      } else if (platformType == PlatformType.MAC) {
         list.add(inMidiPortNames, outMidiPortNames);
      } else if (platformType == PlatformType.LINUX) {
         list.add(inMidiPortNames, outMidiPortNames);
      }
   }

   @Override
   public String getHelpFilePath() {
      return "";
   }

   @Override
   public String getSupportFolderPath() {
      return "";
   }

   @Override
   public MackieMcuProExtension createInstance(final ControllerHost host) {
      return new MackieMcuProExtension(this, host,
         new ControllerConfig(IconQconProXExtensionDefinition.noteOverrides, true, true, true), nrOfExtenders);
   }
}
