package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

public class IncBuffer {
    int currentOffset = 0;
    private final int buffer;
    
    public IncBuffer(final int buffer) {
        this.buffer = buffer;
    }
    
    public int inc(final int incValue) {
        if (currentOffset > 0 && incValue < 0 || currentOffset < 0 && incValue > 0) {
            currentOffset = 0;
            return 0;
        }
        currentOffset = currentOffset + incValue;
        if (Math.abs(currentOffset) >= buffer) {
            currentOffset = 0;
            return incValue;
        }
        return 0;
    }
}
