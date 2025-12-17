package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.framework.Binding;

public abstract class LauncherBinding<S> extends Binding<S, Parameter> {
    protected HardwareBinding hwBinding;
    
    public LauncherBinding(final ControlTargetId targetId, final S control, final Parameter parameter) {
        super(targetId, control, parameter);
    }
    
    protected abstract HardwareBinding getHardwareBinding();
    
    protected abstract void updateValue();
    
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
        updateValue();
        hwBinding = getHardwareBinding();
    }
    
    
}
