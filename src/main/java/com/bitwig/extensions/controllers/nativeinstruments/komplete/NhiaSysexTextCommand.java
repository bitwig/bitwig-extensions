package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.MidiOut;

/**
 * Command that sends text to the Mixer Display.
 */
public class NhiaSysexTextCommand extends NhiaSysexCommand {
   private final byte[] dataArray;

   public NhiaSysexTextCommand(final int commandId) {
      dataArray = new byte[BASE_FORMAT.length];
      System.arraycopy(BASE_FORMAT, 0, dataArray, 0, BASE_FORMAT.length);
      dataArray[10] = (byte) commandId;
   }

   public void send(final MidiOut midiOut, final String text) {
      send(midiOut, 0, 0, text);
   }

   /**
    * Send track text.
    *
    * @param midiOut the MIDI out instance
    * @param track   the track
    * @param text    the text
    */
   public void send(final MidiOut midiOut, final int track, final String text) {
      send(midiOut, 0, track, text);
   }

   private void send(final MidiOut midiOut, final int value, final int track, final String text) {
      dataArray[11] = (byte) value;
      dataArray[12] = (byte) track;
      final byte[] sendarray = new byte[dataArray.length + text.length()];
      System.arraycopy(dataArray, 0, sendarray, 0, 13);
      for (int i = 0; i < text.length(); i++) {
         sendarray[13 + i] = (byte) text.charAt(i);
      }
      sendarray[sendarray.length - 1] = SYSEX_END;
      midiOut.sendSysex(sendarray);
   }

}
