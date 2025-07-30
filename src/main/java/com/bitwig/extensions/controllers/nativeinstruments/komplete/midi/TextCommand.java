package com.bitwig.extensions.controllers.nativeinstruments.komplete.midi;

import com.bitwig.extension.controller.api.MidiOut;

public enum TextCommand {
    SELECTED_TRACK(0x41), //
    COLOR_UPDATE(0x4B), //
    TEMPO_UPDATE(0x19), //
    BANK_UPDATE(0x71),
    DAW_IDENT(0x07),
    CONFIG(0x3), //
    PARAMETER_UPDATE(0x72),
    PARAMETER_VALUE(0x73),
    PRESET_NAME(0x76),
    TRACK_SECTION(0x75),
    VOLUME(0x46), //
    PAN(0x47), //
    NAME(0x48);
    private final NhiaSysexTextCommand sysExCommand;
    
    TextCommand(final int commandId) {
        this.sysExCommand = new NhiaSysexTextCommand(commandId);
    }
    
    public void send(final MidiOut midiOut, final String text) {
        this.sysExCommand.send(midiOut, text);
    }
    
    public void send(final MidiOut midiOut, final int index, final String text) {
        this.sysExCommand.send(midiOut, index, text);
    }
    
    public void sendData(final MidiOut midiOut, final int value, final int index, final byte[] data) {
        this.sysExCommand.send(midiOut, value, index, data);
    }
    
    public void send(final MidiOut midiOut, final int id, final int index, final String text) {
        this.sysExCommand.send(midiOut, id, index, text);
    }
    
}
