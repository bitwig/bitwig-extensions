package com.bitwig.extensions.controllers.mcu.bindings.display;

import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.display.DisplayRow;
import com.bitwig.extensions.controllers.mcu.layer.ControlMode;

public class StringRowDisplayBinding extends AbstractDisplayBinding<StringValue> {
    
    public StringRowDisplayBinding(final DisplayManager target, final ControlMode mode, final DisplayRow row,
        final int sectionIndex, final StringValue stringValue) {
        super(target, mode, DisplayTarget.of(row.getRowIndex(), -1, sectionIndex), stringValue);
        exists = true;
        stringValue.addValueObserver(this::handleValueChange);
        this.lastValue = stringValue.get();
    }
    
    protected void updateDisplay() {
        getTarget().sendText(controlMode, targetIndex.rowIndex(), lastValue);
    }
    
    private void handleValueChange(final String newValue) {
        this.lastValue = newValue;
        if (isActive()) {
            updateDisplay();
        }
    }
    
    @Override
    protected void activate() {
        updateDisplay();
    }
    
}
