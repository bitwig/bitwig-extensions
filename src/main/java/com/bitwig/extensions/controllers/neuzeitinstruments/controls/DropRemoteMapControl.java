package com.bitwig.extensions.controllers.neuzeitinstruments.controls;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.neuzeitinstruments.DropMidiProcessor;
import com.bitwig.extensions.framework.values.Midi;

public class DropRemoteMapControl {
    
    private final DropMidiProcessor midiProcessor;
    private final int ccNr;
    private long lastUpdate;
    private final int channel;
    
    public DropRemoteMapControl(final int index, final int channel, final int ccNr, final String name,
        final HardwareSurface surface, final DropMidiProcessor midiProcessor) {
        this.midiProcessor = midiProcessor;
        this.ccNr = ccNr;
        this.channel = channel;
        final AbsoluteHardwareKnob control = surface.createAbsoluteHardwareKnob("%s-%d".formatted(name, index + 1));
        midiProcessor.assignCcMatcher(control, channel, ccNr);
        control.value().addValueObserver(this::handleKnobAction);
        control.targetValue().addValueObserver(this::handleTargetValueUpdate);
    }
    
    private void handleKnobAction(final double v) {
        this.lastUpdate = System.currentTimeMillis();
    }
    
    private void handleTargetValueUpdate(final double value) {
        final int targetValue = (int) Math.round(value * 127);
        final long diff = System.currentTimeMillis() - lastUpdate;
        if (diff > 1000) {
            midiProcessor.sendMidi(Midi.CC | channel, ccNr, targetValue);
        }
    }
}
