package com.bitwig.extensions.controllers.mcu.value;

import java.util.function.IntConsumer;

public class IncrementHolder {
    
    private double currentValue;
    private final IntConsumer consumer;
    private final double stepMultiplier;
    private double lastValue;
    
    public IncrementHolder(final IntConsumer consumer, final double stepMultiplier) {
        this.consumer = consumer;
        this.stepMultiplier = stepMultiplier * 100;
    }
    
    public void increment(final double value) {
        final double increment = value * stepMultiplier;
        
        if (Math.signum(lastValue) != Math.signum(value)) {
            currentValue = increment;
        } else {
            currentValue += increment;
            if (currentValue >= 1.0) {
                consumer.accept(1);
                currentValue = 0;
            } else if (currentValue <= -1.0) {
                consumer.accept(-1);
                currentValue = 0;
            }
        }
        lastValue = value;
    }
}
