package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchAbsoluteEncoder;

public class AbsoluteEncoderBinding extends LauncherBinding<AbsoluteHardwareControl> {
    
    private int parameterValue;
    private boolean incoming = false;
    private final LaunchAbsoluteEncoder knob;
    
    public AbsoluteEncoderBinding(final Parameter parameter, final LaunchAbsoluteEncoder knob) {
        super(knob.getId(), knob.getKnob(), parameter);
        
        this.knob = knob;
        
        parameter.value().addValueObserver(128, this::handleParameterValue);
        this.parameterValue = (int) (parameter.value().get() * 127);
    }
    
    private void handleParameterValue(final int value) {
        this.parameterValue = value;
        if (isActive()) {
            if (incoming) {
                incoming = false;
            } else {
                knob.updateValue(value);
            }
        }
    }
    
    protected AbsoluteHardwareControlBinding getHardwareBinding() {
        return getSource().addBinding(getTarget());
    }
    
    @Override
    protected void updateValue() {
        knob.updateValue(parameterValue);
    }
    
}
