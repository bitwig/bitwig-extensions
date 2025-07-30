package com.bitwig.extensions.controllers.nativeinstruments.komplete.midi;

import com.bitwig.extension.controller.api.MidiOut;

public enum ValueCommand {
    PAGE_COUNT_INDEX(0x74),
    SELECTION_INDEX(0x70),
    AVAILABLE(0x40),
    SELECT(0x42), //
    MUTE(0x43), //
    SOLO(0x44), //
    ARM(0x45), //
    MUTED_BY_SOLO(0x4A);
    
    private final NhiaSysexValueCommand sysExCommand;
    
    ValueCommand(final int commandId) {
        this.sysExCommand = new NhiaSysexValueCommand(commandId);
    }
    
    public void send(final MidiOut midiOut, final int index, final boolean value) {
        sysExCommand.send(midiOut, index, value);
    }
    
    public void send(final MidiOut midiOut, final int index, final int value) {
        sysExCommand.send(midiOut, index, value);
    }
    
    public void send(final MidiOut midiOut, final int index, final int[] values) {
        sysExCommand.send(midiOut, index, values);
    }
}
