package com.bitwig.extensions.controllers.nativeinstruments.komplete.midi;

import com.bitwig.extension.controller.api.MidiOut;

public class NhiaSyexLevelsCommand extends NhiaSysexCommand {

   private final byte[] levelsArray;

   public NhiaSyexLevelsCommand(final int commandId) {
      levelsArray = new byte[BASE_FORMAT.length + 16];
      System.arraycopy(BASE_FORMAT, 0, levelsArray, 0, BASE_FORMAT.length);
      levelsArray[10] = (byte) commandId;
      levelsArray[11] = 2;
      levelsArray[12] = 0;
      levelsArray[levelsArray.length - 1] = SYSEX_END;
   }

   public void updateLevel(final int track, final int levelLeft, final int levelRight) {
      levelsArray[13 + track * 2] = (byte) levelLeft;
      levelsArray[14 + track * 2] = (byte) levelRight;
   }

   public void updateLeft(final int track, final int levelLeft) {
      levelsArray[13 + track * 2] = (byte) levelLeft;
   }

   public void updateRight(final int track, final int levelRight) {
      levelsArray[14 + track * 2] = (byte) levelRight;
   }

   public void update(final MidiOut midiOut) {
      midiOut.sendSysex(levelsArray);
   }
}
