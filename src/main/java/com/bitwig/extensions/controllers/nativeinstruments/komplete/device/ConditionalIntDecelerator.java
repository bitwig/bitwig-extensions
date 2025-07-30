package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.BooleanValue;

public class ConditionalIntDecelerator implements IntConsumer {
    private final IntConsumer incrementer;
    private final int minTime;
    private long lastEventTimeMs = -1;
    private int lastValue;
    private final BooleanValue modifierActive;
    private final int modifierValue;
    
    public ConditionalIntDecelerator(final IntConsumer incrementer, final int minTime,
        final BooleanValue modifierActive, final int modifierValue) {
        this.incrementer = incrementer;
        this.minTime = minTime;
        this.modifierActive = modifierActive;
        this.modifierValue = modifierValue;
    }
    
    @Override
    public void accept(final int value) {
        final long time = System.currentTimeMillis();
        final long diff = time - lastEventTimeMs;
        if (modifierActive.get()) {
            this.incrementer.accept(value * modifierValue);
            lastEventTimeMs = time;
        } else if (lastValue != value || diff >= this.minTime) {
            this.incrementer.accept(value);
            lastEventTimeMs = time;
        }
        lastValue = value;
    }
}
