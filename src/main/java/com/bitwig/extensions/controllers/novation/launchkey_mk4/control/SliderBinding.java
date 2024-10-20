package com.bitwig.extensions.controllers.novation.launchkey_mk4.control;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.framework.Binding;

public class SliderBinding extends Binding<Parameter, AbsoluteHardwareControl> {
    
    private boolean exists;
    private String displayValue;
    private HardwareBinding hwBinding;
    private final int targetId;
    private final int index;
    private final DisplayControl displayControl;
    private String parameterName;
    private String trackName;
    private boolean init = true;
    
    public SliderBinding(final int index, final Parameter parameter, final AbsoluteHardwareControl knob,
        final DisplayControl displayControl, final StringValue trackName, final StringValue parameterName) {
        super(knob, parameter, knob);
        
        parameter.exists().addValueObserver(this::handleExists);
        parameterName.addValueObserver(this::handleParameterName);
        parameter.value().displayedValue().addValueObserver(this::handleDisplayValue);
        trackName.addValueObserver(this::handleTrackName);
        this.exists = parameter.exists().get();
        this.trackName = trackName.get();
        this.parameterName = parameterName.get();
        this.displayControl = displayControl;
        this.targetId = index + 0x05;
        this.index = index;
    }
    
    private void handleTrackName(final String trackName) {
        this.trackName = trackName;
        if (isActive()) {
            displayControl.setText(targetId, 0, trackName);
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
    
    private void handleParameterName(final String parameterName) {
        this.parameterName = parameterName;
        if (isActive()) {
            displayControl.setText(targetId, 1, parameterName);
        }
    }
    
    private void handleExists(final boolean exists) {
        this.exists = exists;
    }
    
    @Override
    protected void deactivate() {
        if (hwBinding != null) {
            hwBinding.removeBinding();
            hwBinding = null;
        }
    }
    
    protected AbsoluteHardwareControlBinding getHardwareBinding() {
        return getSource().addBinding(getTarget());
    }
    
    @Override
    protected void activate() {
        if (hwBinding != null) {
            hwBinding.removeBinding();
        }
        hwBinding = getHardwareBinding();
        displayControl.configureDisplay(targetId, 0x62);
        displayControl.setText(targetId, 0, trackName);
        displayControl.setText(targetId, 1, parameterName);
        displayControl.setText(targetId, 2, displayValue);
    }
    
}
