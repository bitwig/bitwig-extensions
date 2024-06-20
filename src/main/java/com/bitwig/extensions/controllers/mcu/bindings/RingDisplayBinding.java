package com.bitwig.extensions.controllers.mcu.bindings;

import com.bitwig.extensions.controllers.mcu.control.RingDisplay;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;
import com.bitwig.extensions.framework.Binding;

public abstract class RingDisplayBinding<T> extends Binding<T, RingDisplay> {
    protected final RingDisplayType type;

    public RingDisplayBinding(final RingDisplay target, final T source, final RingDisplayType type) {
        super(target, source, target);
        this.type = type;
    }

    @Override
    protected void deactivate() {
    }

    @Override
    protected void activate() {
        getTarget().sendValue(calcValue(), false);
    }

    protected abstract int calcValue();

}
