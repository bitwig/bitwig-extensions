package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchLight;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColor;
import com.bitwig.extensions.framework.Binding;

public class BooleanLightValueBinding extends Binding<BooleanValue, LaunchLight> {
    
    private final RgbColor onColor;
    private final RgbColor offColor;
    private boolean active;
    
    public BooleanLightValueBinding(final LaunchLight light, final BooleanValue value, final RgbColor onColor,
        final RgbColor offColor) {
        super(light, value, light);
        this.onColor = onColor;
        this.offColor = offColor;
        value.addValueObserver(this::handleValue);
        this.active = value.get();
    }
    
    private void handleValue(final boolean onOff) {
        this.active = onOff;
        if (isActive()) {
            getTarget().sendRgbColor(this.active ? onColor : offColor);
        }
    }
    
    @Override
    protected void deactivate() {
    
    }
    
    @Override
    protected void activate() {
        getTarget().sendRgbColor(this.active ? onColor : offColor);
    }
    
    
}
