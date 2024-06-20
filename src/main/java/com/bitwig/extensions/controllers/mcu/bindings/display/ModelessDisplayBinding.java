package com.bitwig.extensions.controllers.mcu.bindings.display;

import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.framework.Binding;

public class ModelessDisplayBinding extends Binding<StringValue, DisplayManager> {
    
    private String lastValue = "";
    protected DisplayTarget targetIndex;
    
    public ModelessDisplayBinding(final DisplayManager target, final DisplayTarget targetIndex,
        final StringValue value) {
        super(targetIndex, value, target);
        this.targetIndex = targetIndex;
        value.addValueObserver(this::handleValueChange);
        value.markInterested();
        this.lastValue = value.get();
    }
    
    private void handleValueChange(final String newValue) {
        this.lastValue = newValue;
        if (isActive()) {
            updateDisplay();
        }
    }
    
    @Override
    protected void deactivate() {
    }
    
    protected void updateDisplay() {
        getTarget().sendText(targetIndex.rowIndex(), targetIndex.cellIndex(), lastValue);
    }
    
    @Override
    protected void activate() {
        
        updateDisplay();
    }
    
}
