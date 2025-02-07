package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import java.util.function.Function;

import com.bitwig.extensions.controllers.novation.slmk3.value.IntValue;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class IntegerControlValue extends BasicControlValue {
    private final IntValue value;
    private final int min;
    private final int max;
    
    public IntegerControlValue(final IntValue value, final int min, final int max, final int incBuffer) {
        this(value, min, max, incBuffer, null);
    }
    
    public IntegerControlValue(final IntValue value, final int min, final int max, final int incBuffer,
        final Function<Integer, String> converter) {
        super(converter == null
            ? new BasicStringValue(Integer.toString(value.get()))
            : new BasicStringValue(converter.apply(value.get())), incBuffer);
        this.value = value;
        this.min = min;
        this.max = max;
        if (converter == null) {
            this.value.addValueObserver(newValue -> displayValue.set(Integer.toString(newValue)));
        } else {
            this.value.addValueObserver(newValue -> displayValue.set(converter.apply(newValue)));
        }
    }
    
    @Override
    public void incrementBy(final int inc, final boolean modifier) {
        final int increment = incBuffer.inc(inc);
        if (increment == 0) {
            return;
        }
        final int newValue = value.get() + inc;
        if (newValue >= min && newValue <= max) {
            value.set(newValue);
        }
    }
}
