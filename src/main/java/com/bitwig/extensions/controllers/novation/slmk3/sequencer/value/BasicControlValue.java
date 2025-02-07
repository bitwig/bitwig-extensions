package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import com.bitwig.extension.controller.api.SettableStringValue;
import com.bitwig.extension.controller.api.StringValue;

public abstract class BasicControlValue implements ControlValue {
    
    protected final SettableStringValue displayValue;
    protected final IncBuffer incBuffer;
    
    protected BasicControlValue(final SettableStringValue displayValue, final int incBuffer) {
        this.displayValue = displayValue;
        this.incBuffer = new IncBuffer(incBuffer);
    }
    
    @Override
    public StringValue getDisplayValue() {
        return displayValue;
    }
    
    @Override
    public void update() {
    
    }
}
