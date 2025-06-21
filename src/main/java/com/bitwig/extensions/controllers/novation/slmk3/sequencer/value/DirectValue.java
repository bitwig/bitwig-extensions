package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.StringValue;

public class DirectValue implements ControlValue {
    
    private final SettableRangedValue value;
    private final int min;
    private final int max;
    private final double increment;
    
    public DirectValue(final SettableRangedValue value, final int min, final int max, final double increment) {
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.value.markInterested();
        this.value.displayedValue().markInterested();
    }
    
    @Override
    public StringValue getDisplayValue() {
        return this.value.displayedValue();
    }
    
    @Override
    public void incrementBy(final int inc, final boolean modifier) {
        final double amount = modifier ? 0.01 * increment : increment;
        final double newValue = Math.max(20, Math.min(666, value.getRaw() + inc * amount));
        value.setRaw(newValue);
    }
    
    @Override
    public void update() {
        
    }
}
