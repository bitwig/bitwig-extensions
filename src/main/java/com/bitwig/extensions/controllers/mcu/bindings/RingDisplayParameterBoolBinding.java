package com.bitwig.extensions.controllers.mcu.bindings;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extensions.controllers.mcu.control.RingDisplay;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;

public class RingDisplayParameterBoolBinding extends RingDisplayBinding<BooleanValue> {
    
    public RingDisplayParameterBoolBinding(final BooleanValue source, final RingDisplay target) {
        super(target, source, RingDisplayType.FILL_LR);
        final int vintRange = type.getRange() + 1;
        source.addValueObserver(this::valueChanged);
    }
    
    private void valueChanged(final boolean b) {
        if (isActive()) {
            getTarget().sendValue(calcValue(), false);
        }
    }
    
    @Override
    protected int calcValue() {
        return getSource().get() ? type.getOffset() + type.getRange() : 0;
    }
    
}
