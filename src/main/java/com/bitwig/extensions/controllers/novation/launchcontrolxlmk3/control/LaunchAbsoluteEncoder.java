package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMidiProcessor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.ControlTargetId;

public class LaunchAbsoluteEncoder extends LaunchKnob {
    private final AbsoluteHardwareKnob knob;
    
    public LaunchAbsoluteEncoder(final int index, final HardwareSurface surface,
        final LaunchControlMidiProcessor midiProcessor, final LaunchLight light) {
        super(index, index + 0xD, midiProcessor, light);
        knob = surface.createAbsoluteHardwareKnob("A_ENCODER_" + ccNr);
        midiProcessor.setAbsoluteCcMatcher(knob, ccNr, 0xF);
    }
    
    public AbsoluteHardwareKnob getKnob() {
        return knob;
    }
    
    public int getCcNr() {
        return ccNr;
    }
    
    public ControlTargetId getId() {
        return new ControlTargetId(index + 0xD);
    }
    
    public void updateValue(final int value) {
        midiProcessor.sendMidi(0xBF, ccNr, value);
    }
}
