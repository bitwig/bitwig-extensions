package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control;

import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMidiProcessor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.ControlTargetId;

public abstract class LaunchKnob {
    
    protected final int index;
    protected final int ccNr;
    
    protected final LaunchControlMidiProcessor midiProcessor;
    protected final LaunchLight light;
    
    public LaunchKnob(final int index, final int ccNr, final LaunchControlMidiProcessor midiProcessor,
        final LaunchLight light) {
        this.index = index;
        this.ccNr = ccNr;
        this.midiProcessor = midiProcessor;
        this.light = light;
    }
    
    public int getTargetId() {
        return 0xD + index;
    }
    
    public ControlTargetId getId() {
        return new ControlTargetId(getTargetId());
    }
    
    public LaunchLight getLight() {
        return light;
    }
    
    
}
