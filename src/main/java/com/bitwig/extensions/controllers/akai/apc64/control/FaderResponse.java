package com.bitwig.extensions.controllers.akai.apc64.control;

import com.bitwig.extensions.controllers.akai.apc.common.MidiProcessor;

public class FaderResponse {
    private final MidiProcessor midiProcessor;
    private final int aftertouchValue;
    int lastValue = -1;

    public FaderResponse(final MidiProcessor midi, final int which) {
        aftertouchValue = 0xE0 | which;
        this.midiProcessor = midi;
    }

    public void sendValue(final double v) {
        final int value = (int) (v * 16383);
        if (value != lastValue) {
            lastValue = value;
            final int lsb = value & 0x7F;
            final int msb = value >> 7;
            midiProcessor.sendMidi(aftertouchValue, lsb, msb);
        }
    }

    public int getWhich() {
        return aftertouchValue & 0xF;
    }

    public void refresh() {
        final int lsb = lastValue & 0x7F;
        final int msb = lastValue >> 7;
        midiProcessor.sendMidi(aftertouchValue, lsb, msb);
    }

}
