package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IconQconProG2ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("22035e35-4266-47f7-a364-f9c7284c226d");

   private static final int MCU_API_VERSION = 15;
   private static final String SOFTWARE_VERSION = "0.9";
   private static final String DEVICE_NAME = "iCON Qcon Pro G2";
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

   public IconQconProG2ExtensionDefinition() {
      this(0);
   }

   public IconQconProG2ExtensionDefinition(final int nrOfExtenders) {
      this.nrOfExtenders = nrOfExtenders;
      inMidiPortNames = new String[nrOfExtenders + 1];
      outMidiPortNames = new String[nrOfExtenders + 1];
      inMidiPortNames[0] = "iCON QCON Pro G2 V1.00";
      outMidiPortNames[0] = "iCON QCON Pro G2 V1.00";
      for (int i = 1; i < nrOfExtenders + 1; i++) {
         inMidiPortNames[i] = String.format("iCON QCON Ex%d G2 V1.00", i);
         outMidiPortNames[i] = String.format("iCON QCON Ex%d G2 V1.00", i);
      }
   }

   @Override
   public String getName() {
      if (nrOfExtenders == 0) {
         return IconQconProG2ExtensionDefinition.DEVICE_NAME;
      }
      return String.format("%s +%d EXTENDER", IconQconProG2ExtensionDefinition.DEVICE_NAME, nrOfExtenders);
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return IconQconProG2ExtensionDefinition.SOFTWARE_VERSION;
   }

   @Override
   public UUID getId() {
      return IconQconProG2ExtensionDefinition.DRIVER_ID;
   }

   @Override
   public String getHardwareVendor() {
      return "iCON";
   }

   @Override
   public String getHardwareModel() {
      return "iCON Qcon Pro G2";
   }

   @Override
   public int getRequiredAPIVersion() {
      return IconQconProG2ExtensionDefinition.MCU_API_VERSION;
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
         new ControllerConfig(IconQconProG2ExtensionDefinition.noteOverrides, false, true, false), nrOfExtenders);
   }
}
