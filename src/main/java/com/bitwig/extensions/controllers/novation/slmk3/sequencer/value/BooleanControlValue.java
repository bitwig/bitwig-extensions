package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class BooleanControlValue extends BasicControlValue {
    private final SettableBooleanValue value;
    
    public BooleanControlValue(final SettableBooleanValue value, final String trueValue, final String falseValue) {
        super(new BasicStringValue(value.get() ? trueValue : falseValue), 15);
        this.value = value;
        this.value.addValueObserver(newValue -> displayValue.set(newValue ? trueValue : falseValue));
    }
    
    @Override
    public void incrementBy(final int inc, final boolean modifier) {
        final int increment = incBuffer.inc(inc);
        if (increment == 0) {
            return;
        }
        if (inc > 0 && !value.get()) {
            value.set(true);
        } else if (inc < 0 && value.get()) {
            value.set(false);
        }
    }
    
}
