package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchLight;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.GradientColor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColor;
import com.bitwig.extensions.framework.Binding;

public class LightSendValueBindings extends Binding<Parameter, LaunchLight> {
    
    private GradientColor gradient;
    protected int value;
    protected boolean exists;
    
    public LightSendValueBindings(final Send parameter, final LaunchLight target) {
        super(parameter, parameter, target);
        parameter.isPreFader().addValueObserver(this::handlePrefader);
        this.gradient = parameter.isPreFader().get() ? GradientColor.CYAN : GradientColor.YELLOW;
        
        parameter.value().addValueObserver(gradient.length(), this::handleValue);
        this.value = (int) (parameter.value().get() * (gradient.length() - 1));
        parameter.exists().addValueObserver(this::handleExists);
    }
    
    private void handlePrefader(final boolean prefader) {
        this.gradient = prefader ? GradientColor.CYAN : GradientColor.YELLOW;
        if (isActive()) {
            sendColor();
        }
    }
    
    private void handleExists(final boolean exists) {
        this.exists = exists;
        if (isActive()) {
            sendColor();
        }
    }
    
    private void handleValue(final int value) {
        if (this.value != value) {
            this.value = value;
            if (isActive()) {
                sendColor();
            }
        }
    }
    
    protected void sendColor() {
        if (!exists) {
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
