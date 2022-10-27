package com.bitwig.extensions.controllers.arturia.beatsteppro;

import com.bitwig.extension.controller.api.MidiOut;

public class McuFaderResponse {
    private final MidiOut midi;
    private final int aftertouchValue;
    int lastValue = -1;

    public McuFaderResponse(final MidiOut midi, final int which) {
        aftertouchValue = 0xE0 | which;
        this.midi = midi;
    }

    public void sendValue(final double v) {
        final int value = (int) (v * 16383);
        if (value != lastValue) {
            lastValue = value;
            final int lsb = value & 0x7F;
            final int msb = value >> 7;
            midi.sendMidi(aftertouchValue, lsb, msb);
        }
    }

    public int getWhich() {
        return aftertouchValue & 0xF;
    }

    public void refresh() {
        final int lsb = lastValue & 0x7F;
        final int msb = lastValue >> 7;
        midi.sendMidi(aftertouchValue, lsb, msb);
    }

}
