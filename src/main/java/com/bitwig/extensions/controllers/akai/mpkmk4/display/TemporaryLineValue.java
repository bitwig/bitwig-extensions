package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;

public class TemporaryLineValue {
    
    private final MpkMidiProcessor midiProcessor;
    private String value;
    private int colorIndex;
    
    public TemporaryLineValue(final MpkMidiProcessor midiProcessor) {
        this.midiProcessor = midiProcessor;
    }
    
    public void setValue(final String value) {
        this.value = value;
    }
    
    public void setColorIndex(final int colorIndex) {
        this.colorIndex = colorIndex;
    }
    
    public String getValue() {
        return value;
    }
    
    public void update() {
        midiProcessor.temporaryUpdate(0, value, colorIndex);
    }
}
