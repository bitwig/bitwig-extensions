package com.bitwig.extensions.controllers.mcu.bindings;

import com.bitwig.extensions.controllers.mcu.control.RingDisplay;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;
import com.bitwig.extensions.framework.values.IntValueObject;

public class RingDisplayIntValueBinding extends RingDisplayBinding<IntValueObject> {
    
    public RingDisplayIntValueBinding(final IntValueObject source, final RingDisplay target) {
        super(target, source, RingDisplayType.FILL_LR);
        source.addValueObserver(this::valueChanged);
    }
    
    private void valueChanged(final int newValue) {
        if (isActive()) {
            getTarget().sendValue(calcValue(), false);
        }
    }
    
    @Override
    protected int calcValue() {
        if (getSource().get() == -1) {
            return 0;
        }
        return type.getOffset() + getSource().get();
    }
    
}
