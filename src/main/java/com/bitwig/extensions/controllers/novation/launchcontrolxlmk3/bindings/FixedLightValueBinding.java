package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchLight;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColor;
import com.bitwig.extensions.framework.Binding;

public class FixedLightValueBinding extends Binding<RgbColor, LaunchLight> {
    
    private final RgbColor color;
    
    public FixedLightValueBinding(final LaunchLight target, final RgbColor color) {
        super(target.getLightId(), color, target);
        this.color = color;
    }
    
    @Override
    protected void deactivate() {
    
    }
    
    @Override
    protected void activate() {
        getTarget().sendRgbColor(color);
    }
    
    
}
