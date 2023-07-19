package com.bitwig.extensions.controllers.nativeinstruments.komplete.midi;

import com.bitwig.extension.controller.api.MidiOut;

public enum ValueCommand {
   AVAILABLE(0x40),
   SELECT(0x42), //
   MUTE(0x43), //
   SOLO(0x44), //
   ARM(0x45), //
   MUTED_BY_SOLO(0x4A);

   private final NhiaSysexValueCommand sysExCommand;

   ValueCommand(int commandId) {
      this.sysExCommand = new NhiaSysexValueCommand(commandId);
   }

   public void send(MidiOut midiOut, int index, boolean value) {
      sysExCommand.send(midiOut, index, value);
   }

   public void send(MidiOut midiOut, int index, int value) {
      sysExCommand.send(midiOut, index, value);
   }
}
