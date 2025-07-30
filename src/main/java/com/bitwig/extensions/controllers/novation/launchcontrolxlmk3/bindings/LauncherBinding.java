package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.framework.Binding;

public abstract class LauncherBinding<T> extends Binding<Parameter, T> implements DisableBinding {
    protected final int targetId;
    protected String displayValue;
    protected String parameterName;
    protected String trackName;
    protected HardwareBinding hwBinding;
    protected final DisplayControl displayControl;
    private boolean disabled;
    
    public LauncherBinding(final int targetId, final Parameter parameter, final T target,
        final DisplayControl displayControl, final StringValue trackName, final StringValue parameterName) {
        super(parameter, parameter, target);
        this.targetId = targetId;
        this.displayControl = displayControl;
        parameterName.addValueObserver(this::handleParameterName);
        parameter.value().displayedValue().addValueObserver(this::handleDisplayValue);
        trackName.addValueObserver(this::handleTrackName);
        this.parameterName = parameterName.get();
        this.trackName = trackName.get();
    }
    
    private void handleTrackName(final String trackName) {
        this.trackName = trackName;
        if (isActive() && !disabled) {
            displayControl.setText(targetId, 0, trackName);
            displayControl.setText(targetId, 1, parameterName);
        }
    }
    
    private void handleParameterName(final String parameterName) {
        this.parameterName = parameterName;
        if (isActive() && !disabled) {
            displayControl.setText(targetId, 1, parameterName);
        }
    }
    
    private void handleDisplayValue(final String value) {
        this.displayValue = value;
        if (isActive() && !disabled) {
            displayControl.configureDisplay(targetId, 0x62);
            displayControl.setText(targetId, 2, displayValue);
        }
    }
    
    protected abstract HardwareBinding getHardwareBinding();
    
    protected abstract void updateValue();
    
    public void setDisabled(final boolean disabled) {
        this.disabled = disabled;
        if (isActive()) {
            if (disabled) {
                deactivate();
                displayControl.setText(targetId, 0, trackName);
                displayControl.setText(targetId, 1, "");
                displayControl.setText(targetId, 2, "");
            } else {
                activate();
            }
        }
    }
    
    @Override
    protected void deactivate() {
        if (hwBinding != null) {
            hwBinding.removeBinding();
            hwBinding = null;
        }
    }
    
    @Override
    protected void activate() {
        if (disabled) {
            return;
        }
        if (hwBinding != null) {
            hwBinding.removeBinding();
        }
        updateValue();
        hwBinding = getHardwareBinding();
        displayControl.configureDisplay(targetId, 0x62);
        fullTextUpdate();
    }
    
    private void fullTextUpdate() {
        displayControl.setText(targetId, 0, trackName);
        if (disabled) {
            displayControl.setText(targetId, 1, "");
            displayControl.setText(targetId, 2, "");
        } else {
            displayControl.setText(targetId, 1, parameterName);
            displayControl.setText(targetId, 2, displayValue);
        }
    }
    
}
