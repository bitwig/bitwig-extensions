package com.bitwig.extensions.controllers.novation.launchkey_mk4.control;

import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.framework.Binding;

public class AbsoluteEncoderBinding extends Binding<Parameter, RelAbsEncoder> {
    
    private int parameterValue;
    private boolean exists;
    private String displayValue;
    private boolean incoming = false;
    private HardwareBinding hwBinding;
    private final int targetId;
    private final int index;
    private final DisplayControl displayControl;
    private String parameterName;
    private String trackName;
    private boolean init = true;
    
    public AbsoluteEncoderBinding(final int index, final Parameter parameter, final RelAbsEncoder knob,
        final DisplayControl displayControl, final StringValue trackName, final StringValue parameterName) {
        super(parameter, parameter, knob);
        
        parameter.value().addValueObserver(128, this::handleParameterValue);
        parameter.exists().addValueObserver(this::handleExists);
        parameterName.addValueObserver(this::handleParameterName);
        parameter.value().displayedValue().addValueObserver(this::handleDisplayValue);
        trackName.addValueObserver(this::handleTrackName);
        this.exists = parameter.exists().get();
        this.trackName = trackName.get();
        this.parameterName = parameterName.get();
        this.displayControl = displayControl;
        this.parameterValue = (int) (parameter.value().get() * 127);
        this.targetId = index + 0x15;
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
    
    private void handleParameterValue(final int value) {
        this.parameterValue = value;
        if (isActive()) {
            if (incoming) {
                incoming = false;
            } else {
                getTarget().updateValue(value);
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
    
    protected AbsoluteHardwareControlBinding getHardwareBinding() {
        return getSource().addBinding(getTarget().getKnob());
    }
    
    @Override
    protected void activate() {
        if (hwBinding != null) {
            hwBinding.removeBinding();
        }
        hwBinding = getHardwareBinding();
        getTarget().updateValue(parameterValue);
        //        LaunchkeyMk4Extension.println(" CFG Display %d", index);
        displayControl.configureDisplay(targetId, 0x62);
        displayControl.setText(targetId, 0, trackName);
        displayControl.setText(targetId, 1, parameterName);
        displayControl.setText(targetId, 2, displayValue);
    }
    
}
