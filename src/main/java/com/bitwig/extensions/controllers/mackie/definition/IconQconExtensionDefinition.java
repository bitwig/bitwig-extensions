package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;

public class IconQconExtensionDefinition extends MackieMcuProExtensionDefinition {
   private static final String SOFTWARE_VERSION = "0.9";

   public void initNoteOverrides() {
      // DAW Mode Button => Launcher
      override(BasicNoteOnAssignment.GROUP, BasicNoteOnAssignment.GLOBAL_VIEW);
      // DVR Button => Keyboard Mode
      override(BasicNoteOnAssignment.NUDGE, BasicNoteOnAssignment.REPLACE);

      overrideX(BasicNoteOnAssignment.CLIP_OVERDUB, BasicNoteOnAssignment.NUDGE);
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
