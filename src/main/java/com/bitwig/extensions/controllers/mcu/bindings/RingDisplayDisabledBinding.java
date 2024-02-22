package com.bitwig.extensions.controllers.mcu.bindings;

import com.bitwig.extensions.controllers.mcu.control.RingDisplay;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;

public class RingDisplayDisabledBinding extends RingDisplayBinding<RingDisplayType> {

    public RingDisplayDisabledBinding(final RingDisplay target, final RingDisplayType type) {
        super(target, type, type);
    }

    @Override
    protected int calcValue() {
        return 0;
    }
}
