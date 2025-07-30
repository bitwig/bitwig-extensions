package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;

public class SliderBinding extends LauncherBinding<AbsoluteHardwareControl> {
    
    public SliderBinding(final int index, final Parameter parameter, final AbsoluteHardwareControl control,
        final DisplayControl displayControl, final StringValue trackName, final StringValue parameterName) {
        super(index + 0x5, parameter, control, displayControl, trackName, parameterName);
    }
    
    protected AbsoluteHardwareControlBinding getHardwareBinding() {
        return getSource().addBinding(getTarget());
    }
    
    @Override
    protected void updateValue() {
        // nothing to do
    }
    
}
