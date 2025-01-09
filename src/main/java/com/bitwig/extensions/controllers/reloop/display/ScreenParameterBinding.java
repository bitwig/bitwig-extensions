package com.bitwig.extensions.controllers.reloop.display;

import com.bitwig.extension.controller.api.ContinuousHardwareControl;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.framework.Binding;

public class ScreenParameterBinding extends Binding<ContinuousHardwareControl, Parameter> {
    
    private String value = "";
    private String name = "";
    private String targetName = "";
    private boolean exists;
    private final ScreenMode screenMode;
    private final ScreenManager screenManager;
    
    public ScreenParameterBinding(final ContinuousHardwareControl control, final Parameter parameter,
        final StringValue targetName, final ScreenMode mode, final ScreenManager screenManager) {
        super(control, control, parameter);
        this.screenMode = mode;
        this.screenManager = screenManager;
        targetName.addValueObserver(this::handleTrackNameChanged);
        parameter.name().addValueObserver(this::handNameChanged);
        parameter.value().displayedValue().addValueObserver(this::handleValueChanged);
        control.isUpdatingTargetValue().addValueObserver(this::handleUpdating);
        parameter.exists().addValueObserver(this::existsChanged);
        this.name = parameter.name().get();
        this.targetName = targetName.get();
        this.exists = parameter.exists().get();
        value = parameter.displayedValue().get();
    }
    
    private void handleUpdating(final boolean updating) {
        if (isActive() && updating && exists) {
            screenManager.changeParameter(screenMode, this.targetName, name, value);
        }
    }
    
    private void existsChanged(final boolean exists) {
        this.exists = exists;
    }
    
    private void handleTrackNameChanged(final String trackName) {
        this.targetName = trackName;
    }
    
    private void handNameChanged(final String newParameterName) {
        name = newParameterName;
    }
    
    private void handleValueChanged(final String displayValue) {
        this.value = displayValue;
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
    }
}
