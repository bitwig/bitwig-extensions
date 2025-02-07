package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.SettableDoubleValue;
import com.bitwig.extension.controller.api.StringValue;

public class DoubleValueControlValue implements ControlValue {
    
    private final SettableDoubleValue value;
    private final IncBuffer incBuffer;
    private final BooleanValue shiftState;
    private final StringValue displayValue;
    private final double incAmount;
    private final double fineIncAmount;
    
    public DoubleValueControlValue(final SettableDoubleValue value, final StringValue displayValue,
        final BooleanValue shiftState, final double incAmount, final double fineIncAmount, final int incBuffer) {
        this.incBuffer = new IncBuffer(incBuffer);
        this.shiftState = shiftState;
        this.value = value;
        this.incAmount = incAmount;
        this.fineIncAmount = fineIncAmount;
        this.displayValue = displayValue;
    }
    
    @Override
    public StringValue getDisplayValue() {
        return displayValue;
    }
    
    @Override
    public void incrementBy(final int inc, final boolean modifier) {
        final int increment = incBuffer.inc(inc);
        if (increment == 0) {
            return;
        }
        if (shiftState.get() || modifier) {
            final double newValue = value.get() + increment * fineIncAmount;
            value.set(newValue);
        } else {
            final double newValue = value.get() + increment * incAmount;
            value.set(newValue);
        }
    }
    
    @Override
    public void update() {
    }
}
