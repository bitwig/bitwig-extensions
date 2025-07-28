package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;

public class RelativeEncoderBinding extends LauncherBinding<LaunchRelativeEncoder> {
    
    public RelativeEncoderBinding(final Parameter parameter, final LaunchRelativeEncoder knob,
        final DisplayControl displayControl, final StringValue trackName, final StringValue parameterName) {
        super(knob.getTargetId(), parameter, knob, displayControl, trackName, parameterName);
        parameter.discreteValueCount().addValueObserver(this::handleSteps);
    }
    
    private void handleSteps(final int discreteSteps) {
        if (isActive()) {
        
        }
    }
    
    @Override
    protected HardwareBinding getHardwareBinding() {
        return getSource().addBinding(getTarget().getEncoder());
    }
    
    @Override
    protected void updateValue() {
    }
    
    @Override
    protected void activate() {
        super.activate();
        getTarget().setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 64);
    }
}
