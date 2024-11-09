package com.bitwig.extensions.controllers.novation.launchkey_mk4.values;

import java.util.function.IntConsumer;

public class IncrementDecelerator implements IntConsumer {
    private final IntConsumer incrementer;
    private final int minTime;
    private long lastEventTimeMs = -1;
    private int lastValue;
    
    public IncrementDecelerator(final IntConsumer incrementer, final int minTime) {
        this.incrementer = incrementer;
        this.minTime = minTime;
    }
    
    @Override
    public void accept(final int value) {
        final long time = System.currentTimeMillis();
        if (lastValue == value && (time - lastEventTimeMs) > this.minTime) {
            this.incrementer.accept(value);
            lastEventTimeMs = time;
        }
        lastValue = value;
    }
}
