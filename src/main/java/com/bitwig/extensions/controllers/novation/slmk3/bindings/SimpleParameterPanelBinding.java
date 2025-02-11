package com.bitwig.extensions.controllers.novation.slmk3.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.KnobPanel;
import com.bitwig.extensions.framework.Binding;

public class SimpleParameterPanelBinding extends Binding<Parameter, KnobPanel> {
    
    private int value;
    private final SlRgbState knobColor;
    private boolean exists;
    private String displayValue = "";
    
    public SimpleParameterPanelBinding(final Parameter parameter, final KnobPanel panel, final SlRgbState knobColor) {
        super(panel, parameter, panel);
        this.knobColor = knobColor;
        parameter.value().addValueObserver(128, this::handleValue);
        parameter.exists().addValueObserver(this::handleExists);
        parameter.displayedValue().addValueObserver(this::handleDisplayValue);
    }
    
    private void handleDisplayValue(final String value) {
        this.displayValue = value;
        if (isActive()) {
            getTarget().setText(1, this.displayValue);
        }
    }
    
    private void handleExists(final boolean exists) {
        this.exists = exists;
        if (isActive()) {
            getTarget().setKnobIconColor(exists ? knobColor : SlRgbState.OFF);
        }
    }
    
    private void handleValue(final int value) {
        this.value = value;
        if (isActive()) {
            getTarget().setKnobValue(value);
        }
    }
    
    @Override
    protected void deactivate() {
        
    }
    
    @Override
    protected void activate() {
        getTarget().setKnobIconColor(exists ? knobColor : SlRgbState.OFF);
        getTarget().setKnobValue(value);
        getTarget().setText(1, this.displayValue);
    }
}
