package com.bitwig.extensions.controllers.nativeinstruments.komplete.binding;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Binding;

public class EncoderParameterBinding extends Binding<RelativeHardwareKnob, SettableRangedValue> {
    public static final double BASE_SENSITIVITY = 1;
    
    private HardwareBinding hwBinding;
    
    public EncoderParameterBinding(final RelativeHardwareKnob encoder, final Parameter parameter) {
        super(encoder, encoder, parameter);
    }
    
    protected RelativeHardwareControlBinding getHardwareBinding(final double sensitivity) {
        return getTarget().addBindingWithSensitivity(getSource(), sensitivity);
    }
    
    @Override
    protected void deactivate() {
        if (hwBinding != null && isActive()) {
            hwBinding.removeBinding();
        }
    }
    
    @Override
    protected void activate() {
        if (hwBinding != null) {
            hwBinding.removeBinding();
        }
        hwBinding = getHardwareBinding(BASE_SENSITIVITY);
    }
}
