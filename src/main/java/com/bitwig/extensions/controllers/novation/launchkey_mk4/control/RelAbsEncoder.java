package com.bitwig.extensions.controllers.novation.launchkey_mk4.control;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.MidiProcessor;

public class RelAbsEncoder {
    private final AbsoluteHardwareKnob knob;
    private final int ccNr;
    private final int channel;
    private HardwareBinding hwBinding;
    
    private final MidiProcessor midiProcessor;
    
    public RelAbsEncoder(final int ccNr, final int channel, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        knob = surface.createAbsoluteHardwareKnob("ENCODER_" + ccNr);
        midiProcessor.setAbsoluteCcMatcher(knob, ccNr, channel);
        this.ccNr = ccNr;
        this.channel = channel;
        this.midiProcessor = midiProcessor;
    }
    
    public AbsoluteHardwareKnob getKnob() {
        return knob;
    }
    
    public int getCcNr() {
        return ccNr;
    }
    
    public int getChannel() {
        return channel;
    }
    
    public void updateValue(final int value) {
        midiProcessor.sendMidi(0xB0 | channel, ccNr, value);
    }
}
