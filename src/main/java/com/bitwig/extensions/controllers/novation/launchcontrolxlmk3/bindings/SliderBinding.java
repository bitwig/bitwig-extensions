package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.Parameter;

public class SliderBinding extends LauncherBinding<AbsoluteHardwareControl> {
    
    public SliderBinding(final ControlTargetId targetId, final Parameter parameter,
        final AbsoluteHardwareControl control) {
        super(targetId, control, parameter); //index + 0x05
    }
    
    protected AbsoluteHardwareControlBinding getHardwareBinding() {
        return getTarget().addBinding(getSource());
    }
    
    @Override
    protected void updateValue() {
        // nothing to do
    }
    
}
