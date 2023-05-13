package com.bitwig.extensions.controllers.arturia.beatsteppro;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.framework.Binding;

public class McuFaderBinding extends Binding<Parameter, McuFaderResponse> {

    private double lastValue = 0.0;

    public McuFaderBinding(final Parameter source, final McuFaderResponse target) {
        super(target, source, target);
        source.value().addValueObserver(this::valueChange);
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
