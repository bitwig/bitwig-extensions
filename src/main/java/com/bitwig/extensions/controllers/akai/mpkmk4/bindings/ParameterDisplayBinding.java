package com.bitwig.extensions.controllers.akai.mpkmk4.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.ParameterValues;
import com.bitwig.extensions.framework.Binding;

public class ParameterDisplayBinding extends Binding<Parameter, ParameterValues> {
    
    private final ParameterValues paramValues;
    private final int index;
    private String name = "";
    private String displayValue = "";
    
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
        this.name = source.name().get();
    }
    
    private void handleName(final String name) {
        this.name = name;
        paramValues.setNames(index, name);
        invokeUpdate();
    }
    
    private void handleDisplayValue(final String value) {
        this.displayValue = value;
        paramValues.setValue(index, displayValue);
        invokeUpdate();
    }
    
    private void handleIsUpdating(final boolean updating) {
        if (updating) {
            invokeUpdate();
        }
    }
    
    private void invokeUpdate() {
        if (isActive()) {
            paramValues.update();
        }
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
    }
    
    public void update() {
    }
}
