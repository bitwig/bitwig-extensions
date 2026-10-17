package com.bitwig.extensions.controllers.akai.mpkmk4.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.ParameterValues;
import com.bitwig.extensions.framework.Binding;

public class ParameterDisplayBinding extends Binding<Parameter, ParameterValues> {
    
    private final ParameterValues paramValues;
    private final int index;
    private String displayValue = "";
    private String name = "";
    
    public ParameterDisplayBinding(final Parameter source, final Encoder encoder, final ParameterValues target,
        final int index) {
        super(source, source, target);
        this.paramValues = target;
        this.index = index;
        encoder.getEncoder().isUpdatingTargetValue().addValueObserver(this::handleIsUpdating);
        source.value().markInterested();
        source.value().displayedValue().addValueObserver(this::handleDisplayValue);
        source.name().addValueObserver(this::handleName);
        this.displayValue = source.displayedValue().get();
    }
    
    private void handleName(final String name) {
        this.name = name;
        if (isActive()) {
            paramValues.setNames(index, name);
            paramValues.update();
        }
    }
    
    private void handleDisplayValue(final String value) {
        this.displayValue = value;
        if (isActive()) {
            paramValues.setValue(index, displayValue);
            paramValues.update();
        }
    }
    
    private void handleIsUpdating(final boolean updating) {
        if (updating && isActive()) {
            paramValues.update();
        }
    }
    
    @Override
    protected void deactivate() {
        paramValues.setValue(index, "");
        paramValues.setNames(index, "");
    }
    
    @Override
    protected void activate() {
        paramValues.setValue(index, displayValue);
        paramValues.setNames(index, name);
        paramValues.update();
    }
    
    public void update() {
    }
}
