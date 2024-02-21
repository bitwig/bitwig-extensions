package com.bitwig.extensions.controllers.mcu.bindings.display;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.layer.ControlMode;
import com.bitwig.extensions.controllers.mcu.value.StringValueConverter;

public class ParameterDisplayBinding extends AbstractDisplayBinding<Parameter> {
    private final StringValueConverter converter;
    
    public ParameterDisplayBinding(final DisplayManager target, final ControlMode mode,
        final DisplayTarget displayTargetIndex, final Parameter parameter) {
        this(target, mode, displayTargetIndex, parameter, s -> s);
    }
    
    public ParameterDisplayBinding(final DisplayManager target, final ControlMode mode,
        final DisplayTarget displayTargetIndex, final Parameter parameter, final StringValueConverter converter) {
        super(target, mode, displayTargetIndex, parameter);
        parameter.exists().addValueObserver(this::handleExists);
        parameter.displayedValue().addValueObserver(this::handleValueChange);
        this.converter = converter;
        this.lastValue = converter.convert(parameter.displayedValue().get());
    }
    
    private void handleValueChange(final String newValue) {
        this.lastValue = newValue;
        if (isActive()) {
            updateDisplay();
        }
    }
    
}
