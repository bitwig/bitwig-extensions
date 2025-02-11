package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.ValueObject;

public class ObjectControlValue<T> extends BasicControlValue {
    private final ValueObject<T> value;
    
    
    public ObjectControlValue(final ValueObject<T> value, final int incBuffer) {
        super(new BasicStringValue(value.displayedValue()), incBuffer);
        this.value = value;
        this.value.addValueObserver((newValue -> this.displayValue.set(value.displayedValue())));
    }
    
    @Override
    public void incrementBy(final int inc, final boolean modifier) {
        final int increment = incBuffer.inc(inc);
        if (increment == 0) {
            return;
        }
        this.value.increment(inc);
    }
}
