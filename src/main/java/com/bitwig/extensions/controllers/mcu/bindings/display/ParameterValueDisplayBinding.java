package com.bitwig.extensions.controllers.mcu.bindings.display;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.layer.ControlMode;
import com.bitwig.extensions.controllers.mcu.value.DoubleValueConverter;

public class ParameterValueDisplayBinding extends AbstractDisplayBinding<Parameter> {
    private final DoubleValueConverter converter;
    
    public ParameterValueDisplayBinding(final DisplayManager target, final ControlMode mode,
        final DisplayTarget displayTargetIndex, final Parameter parameter) {
        this(target, mode, displayTargetIndex, parameter, value -> Double.toString(value));
    }
    
    public ParameterValueDisplayBinding(final DisplayManager target, final ControlMode mode,
        final DisplayTarget displayTargetIndex, final Parameter parameter, final DoubleValueConverter converter) {
        super(target, mode, displayTargetIndex, parameter);
        parameter.exists().addValueObserver(this::handleExists);
        parameter.value().addValueObserver(this::handleValueChange);
        this.converter = converter;
        this.lastValue = converter.convert(parameter.value().get());
    }
    
    private void handleValueChange(final double newValue) {
        this.lastValue = converter.convert(newValue);
        if (isActive()) {
            updateDisplay();
        }
    }
    
    
}
