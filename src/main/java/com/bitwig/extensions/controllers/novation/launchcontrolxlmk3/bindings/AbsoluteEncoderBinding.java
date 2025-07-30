package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchAbsoluteEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;

public class AbsoluteEncoderBinding extends LauncherBinding<LaunchAbsoluteEncoder> {
    
    private int parameterValue;
    private boolean incoming = false;
    
    public AbsoluteEncoderBinding(final Parameter parameter, final LaunchAbsoluteEncoder knob,
        final DisplayControl displayControl, final StringValue trackName, final StringValue parameterName) {
        super(knob.getTargetId(), parameter, knob, displayControl, trackName, parameterName);
        
        parameter.value().addValueObserver(128, this::handleParameterValue);
        this.parameterValue = (int) (parameter.value().get() * 127);
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
    
    protected AbsoluteHardwareControlBinding getHardwareBinding() {
        return getSource().addBinding(getTarget().getKnob());
    }
    
    @Override
    protected void updateValue() {
        getTarget().updateValue(parameterValue);
    }
    
}
