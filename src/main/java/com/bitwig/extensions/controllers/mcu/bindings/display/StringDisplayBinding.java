package com.bitwig.extensions.controllers.mcu.bindings.display;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mcu.StringUtil;
import com.bitwig.extensions.controllers.mcu.bindings.ResetableBinding;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.layer.ControlMode;
import com.bitwig.extensions.controllers.mcu.value.StringValueConverter;

public class StringDisplayBinding extends AbstractDisplayBinding<StringValue> implements ResetableBinding {
    private final StringValueConverter stringConversion;
    
    public StringDisplayBinding(final DisplayManager target, final ControlMode mode,
        final DisplayTarget displayTargetIndex, final StringValue stringValue, final BooleanValue existsValue,
        final StringValueConverter stringConversion) {
        super(target, mode, displayTargetIndex, stringValue);
        if (existsValue != null) {
            existsValue.addValueObserver(this::handleExists);
        }
        stringValue.addValueObserver(this::handleValueChange);
        this.stringConversion = stringConversion;
        this.lastValue = stringConversion.convert(stringValue.get());
    }
    
    public StringDisplayBinding(final DisplayManager target, final ControlMode mode,
        final DisplayTarget displayTargetIndex, final StringValue stringValue, final BooleanValue existsValue) {
        this(target, mode, displayTargetIndex, stringValue, existsValue, s -> StringUtil.toAsciiDisplay(s, 8));
    }
    
    private void handleValueChange(final String newValue) {
        final String sendValue = stringConversion.convert(newValue);
        if (!sendValue.equals(this.lastValue)) {
            this.lastValue = sendValue;
            if (isActive()) {
                updateDisplay();
            }
        }
    }
    
    @Override
    public void reset() {
        this.lastValue = stringConversion.convert(getSource().get());
        if (isActive()) {
            updateDisplay();
        }
    }
}
