package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import java.util.function.Function;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class RangeValueControlValue implements ControlValue {
    
    private final SettableRangedValue value;
    private final IncBuffer incBuffer;
    private final StringValue displayValue;
    private final double incAmount;
    
    public RangeValueControlValue(final SettableRangedValue value, final double incAmount, final int incBuffer) {
        this(value, incAmount, incBuffer, null);
    }
    
    public RangeValueControlValue(final SettableRangedValue value, final double incAmount, final int incBuffer,
        final Function<Double, String> converter) {
        this(value, null, incAmount, incBuffer, converter);
    }
    
    public RangeValueControlValue(final Parameter value, final double incAmount, final int incBuffer) {
        this(value.value(), value.displayedValue(), incAmount, incBuffer, null);
    }
    
    private RangeValueControlValue(final SettableRangedValue value, final StringValue displayValue,
        final double incAmount, final int incBuffer, final Function<Double, String> converter) {
        this.incBuffer = new IncBuffer(incBuffer);
        this.value = value;
        this.incAmount = incAmount;
        this.value.markInterested();
        
        if (displayValue == null) {
            final Function<Double, String> localConverter = converter == null ? this::convert : converter;
            final BasicStringValue basicStringValue = new BasicStringValue(localConverter.apply(this.value.get()));
            this.displayValue = basicStringValue;
            this.value.addValueObserver(v -> basicStringValue.set(localConverter.apply(v)));
        } else {
            this.displayValue = displayValue;
            this.displayValue.markInterested();
        }
    }
    
    private String convert(final double v) {
        final double value = Math.round((v * 2 - 1) * 100.0);
        return "%2.1f%%".formatted(value);
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
        final double newValue = Math.max(0, Math.min(1.0, value.get() + increment * incAmount));
        value.set(newValue);
    }
    
    @Override
    public void update() {
    
    }
}
