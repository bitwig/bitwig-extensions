package com.bitwig.extensions.framework.values;

public enum MidiStatus {
    
    NOTE_OFF(0x80),
    NOTE_ON(0x90),
    CC(0xB0);
    
    private final int value;
    
    MidiStatus(final int value) {
        this.value = value;
    }
    
    public int getStatus(final int channel) {
        return this.value | channel;
    }
    
    public int getValue() {
        return value;
    }
}
