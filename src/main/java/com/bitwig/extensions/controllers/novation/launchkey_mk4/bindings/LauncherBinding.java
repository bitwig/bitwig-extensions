package com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings;

import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.framework.Binding;

public abstract class LauncherBinding<T> extends Binding<Parameter, T> {
    protected final int targetId;
    protected String displayValue;
    protected String parameterName;
    protected String trackName;
    protected HardwareBinding hwBinding;
    protected final DisplayControl displayControl;
    private boolean init = true;
    
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
        if (isActive()) {
            displayControl.setText(targetId, 0, trackName);
        }
    }
    
    private void handleParameterName(final String parameterName) {
        this.parameterName = parameterName;
        if (isActive()) {
            displayControl.setText(targetId, 1, parameterName);
        }
    }
    
    private void handleDisplayValue(final String value) {
        this.displayValue = value;
        if (isActive()) {
            if (init) {
                displayControl.configureDisplay(targetId, 0x62);
                init = false;
            }
            displayControl.setText(targetId, 2, displayValue);
        }
    }
    
    protected abstract AbsoluteHardwareControlBinding getHardwareBinding();
    
    protected abstract void updateValue();
    
    @Override
    protected void deactivate() {
        if (hwBinding != null) {
            hwBinding.removeBinding();
            hwBinding = null;
        }
    }
    
    @Override
    protected void activate() {
        if (hwBinding != null) {
            hwBinding.removeBinding();
        }
        updateValue();
        hwBinding = getHardwareBinding();
        displayControl.configureDisplay(targetId, 0x62);
        displayControl.setText(targetId, 0, trackName);
        displayControl.setText(targetId, 1, parameterName);
        displayControl.setText(targetId, 2, displayValue);
    }
    
}
