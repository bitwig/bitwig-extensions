package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;


import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.framework.Binding;

public class ParameterDisplayBinding extends Binding<Parameter, DisplayId> implements DisableBinding {
    
    private final DisplayControl display;
    private final int targetId;
    private String titleName;
    private String parameterName;
    private String displayValue;
    private boolean disabled;
    
    public ParameterDisplayBinding(final DisplayId displayId, final StringValue titleName, final Parameter parameter) {
        super(displayId, parameter, displayId);
        this.display = displayId.display();
        this.targetId = displayId.index();
        
        parameter.name().addValueObserver(this::handleParamNameChanged);
        parameter.value().displayedValue().addValueObserver(this::handleDisplayValue);
        titleName.addValueObserver(this::handleTrackName);
        this.titleName = titleName.get();
        this.parameterName = parameter.name().get();
    }
    
    private void handleTrackName(final String trackName) {
        this.titleName = trackName;
        if (isActive() && !disabled) {
            display.setText(targetId, 0, trackName);
            display.setText(targetId, 1, parameterName);
        }
    }
    
    private void handleParamNameChanged(final String value) {
        this.parameterName = value;
        if (isActive() && !disabled) {
            display.setText(targetId, 1, parameterName);
        }
    }
    
    private void handleDisplayValue(final String value) {
        this.displayValue = value;
        if (isActive() && !disabled) {
            display.setText(targetId, 2, displayValue);
        }
    }
    
    public void setDisabled(final boolean disabled) {
        this.disabled = disabled;
        if (isActive()) {
            if (disabled) {
                deactivate();
                display.setText(targetId, 0, titleName);
                display.setText(targetId, 1, "");
                display.setText(targetId, 2, "");
            } else {
                activate();
            }
        }
    }
    
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
        if (disabled) {
            return;
        }
        display.configureDisplay(targetId, 0x62);
        display.setText(targetId, 0, titleName);
        display.setText(targetId, 1, parameterName);
        display.setText(targetId, 2, displayValue);
    }
    
}
