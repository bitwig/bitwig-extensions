package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;

public class RelativeEncoderBinding extends LauncherBinding<RelativeHardwareKnob> implements DisableBinding {
    
    private final LaunchRelativeEncoder encoder;
    private boolean disabled = false;
    
    public RelativeEncoderBinding(final Parameter parameter, final LaunchRelativeEncoder encoder) {
        super(encoder.getId(), encoder.getEncoder(), parameter);
        this.encoder = encoder;
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
        if (disabled) {
            return;
        }
        super.activate();
        encoder.setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 64);
    }
    
    @Override
    public void setDisabled(final boolean disabled) {
        this.disabled = disabled;
        if (disabled) {
            deactivate();
        } else {
            activate();
        }
    }
}
