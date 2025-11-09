package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;
import com.bitwig.extensions.framework.Binding;

public class SteppedEncoderBinding extends Binding<LaunchRelativeEncoder, RelativeHardwarControlBindable> {
    protected HardwareBinding hwBinding;
    
    public SteppedEncoderBinding(final LaunchRelativeEncoder encoder, final RelativeHardwarControlBindable incHandler) {
        super(new TargetId(encoder.getTargetId()), encoder, incHandler);
    }
    
    public HardwareBinding getHardwareBinding() {
        return null;
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
        if (hwBinding != null) {
            hwBinding.removeBinding();
        }
        hwBinding = getHardwareBinding();
    }
}
