package com.bitwig.extensions.controllers.nativeinstruments.komplete.midi;

import com.bitwig.extension.controller.api.MidiOut;

public enum TextCommand {
   SELECTED_TRACK(0x41), //
   VOLUME(0x46), //
   PAN(0x47), //
   NAME(0x48);
   private final NhiaSysexTextCommand sysExCommand;

   TextCommand(int commandId) {
      this.sysExCommand = new NhiaSysexTextCommand(commandId);
   }

   public void send(MidiOut midiOut, String text) {
      this.sysExCommand.send(midiOut, text);
   }

   public void send(MidiOut midiOut, int index, String text) {
      this.sysExCommand.send(midiOut, index, text);
   }
}
