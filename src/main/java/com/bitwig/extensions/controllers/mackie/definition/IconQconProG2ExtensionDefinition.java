package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.ControllerConfig;
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
      overrideX(BasicNoteOnAssignment.STEP_SEQ, BasicNoteOnAssignment.REDO);
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
   public String getHelpFilePath() {
      return "";
   }

   @Override
   public String getSupportFolderPath() {
      return "";
   }

   @Override
   public MackieMcuProExtension createInstance(final ControllerHost host) {
      return new MackieMcuProExtension(this, host, new ControllerConfig(noteOverrides, false, true, false),
         nrOfExtenders);
   }
}
