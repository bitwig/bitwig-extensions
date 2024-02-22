package com.bitwig.extensions.controllers.mcu.bindings;

import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.mcu.control.RingDisplay;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;

public class RingDisplayValueBinding extends RingDisplayBinding<SettableRangedValue> {
    
    public RingDisplayValueBinding(final SettableRangedValue source, final RingDisplay target,
        final RingDisplayType type) {
        super(target, source, type);
        final int vintRange = type.getRange() + 1;
        source.addValueObserver(vintRange, v -> valueChange(type.getOffset() + v));
    }
    
    public void handleExists(final boolean exist) {
        if (isActive()) {
            valueChange(calcValue(exist));
        }
    }
    
    private void valueChange(final int value) {
        if (isActive()) {
            getTarget().sendValue(value, false);
        }
    }
    
    protected int calcValue(final boolean exists) {
        return exists ? type.getOffset() + (int) (getSource().get() * type.getRange()) : 0;
    }
    
    @Override
    protected int calcValue() {
        return type.getOffset() + (int) (getSource().get() * type.getRange());
    }
}
