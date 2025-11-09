package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;

public class RelativeEncoderBinding extends LauncherBinding<RelativeHardwareKnob> {
    
    private final LaunchRelativeEncoder encoder;
    
    public RelativeEncoderBinding(final Parameter parameter, final LaunchRelativeEncoder encoder) {
        super(encoder.getEncoder(), parameter);
        parameter.discreteValueCount().addValueObserver(this::handleSteps);
        this.encoder = encoder;
    }
    
    private void handleSteps(final int discreteSteps) {
        if (isActive()) {
        
        }
    }
    
    @Override
    protected HardwareBinding getHardwareBinding() {
        return getTarget().addBinding(getSource());
    }
    
    @Override
    protected void updateValue() {
    }
    
    @Override
    protected void activate() {
        super.activate();
        encoder.setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 64);
    }
}
