package com.bitwig.extensions.controllers.mcu.bindings.paramslots;

import com.bitwig.extensions.controllers.mcu.control.FaderResponse;
import com.bitwig.extensions.controllers.mcu.devices.ParamPageSlot;
import com.bitwig.extensions.framework.Binding;

public class FaderSlotBinding extends Binding<ParamPageSlot, FaderResponse> {

    private double lastValue = 0.0;

    public FaderSlotBinding(final ParamPageSlot source, final FaderResponse target) {
        super(target, source, target);
        source.getValue().addValueObserver(this::valueChange);
    }

    private void valueChange(final double value) {
        lastValue = value;
        if (isActive()) {
            getTarget().sendValue(value);
        }
    }

    @Override
    protected void deactivate() {
    }

    @Override
    protected void activate() {
        getTarget().sendValue(lastValue);
    }

}
