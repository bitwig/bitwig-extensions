package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchLight;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.GradientColor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColor;
import com.bitwig.extensions.framework.Binding;

public class LightValueBindings extends Binding<Parameter, LaunchLight> implements DisableBinding {
    
    private final GradientColor gradient;
    protected int value;
    protected boolean exists;
    protected boolean disabled;
    
    public LightValueBindings(final Parameter parameter, final LaunchLight target, final GradientColor gradient) {
        super(target.getLightId(), parameter, target);
        this.gradient = gradient;
        
        parameter.value().addValueObserver(gradient.length(), this::handleValue);
        this.value = (int) (parameter.value().get() * (gradient.length() - 1));
        parameter.exists().addValueObserver(this::handleExists);
    }
    
    private void handleExists(final boolean exists) {
        this.exists = exists;
        if (isActive() && !disabled) {
            sendColor();
        }
    }
    
    private void handleValue(final int value) {
        if (this.value != value) {
            this.value = value;
            if (isActive() && !disabled) {
                sendColor();
            }
        }
    }
    
    public void setDisabled(final boolean disabled) {
        if (this.disabled == disabled) {
            return;
        }
        this.disabled = disabled;
        if (isActive()) {
            sendColor();
        }
    }
    
    protected void sendColor() {
        if (!exists || disabled) {
            getTarget().sendRgbColor(RgbColor.OFF);
        } else {
            getTarget().sendRgbColor(gradient.getColor(value));
        }
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
        sendColor();
    }
    
}
