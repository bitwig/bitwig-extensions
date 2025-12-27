package com.bitwig.extensions.controllers.akai.mpkmk4.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.ParameterValues;
import com.bitwig.extensions.framework.Binding;

public class ParameterDisplayBinding extends Binding<Parameter, ParameterValues> {
    
    private final ParameterValues paramValues;
    private final int index;
    
    public ParameterDisplayBinding(final Parameter source, final Encoder encoder, final ParameterValues target, final int index) {
        super(source, source, target);
        this.paramValues = target;
        this.index = index;
        encoder.getEncoder().isUpdatingTargetValue().addValueObserver(this::handleIsUpdating);
        source.displayedValue().addValueObserver(dv -> target.setValue(index, dv));
        source.name().addValueObserver(name -> target.setNames(index, name));
    }
    
    private void handleIsUpdating(final boolean updating) {
        if (updating && isActive()) {
            paramValues.update();
        }
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
    }
}
