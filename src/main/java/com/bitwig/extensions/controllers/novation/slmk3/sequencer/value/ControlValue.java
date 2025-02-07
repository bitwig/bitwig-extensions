package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import com.bitwig.extension.controller.api.StringValue;

public interface ControlValue {
    StringValue getDisplayValue();
    
    void incrementBy(final int inc, boolean modifier);
    
    void update();
}
