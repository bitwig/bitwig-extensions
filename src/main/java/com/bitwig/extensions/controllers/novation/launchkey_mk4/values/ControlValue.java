package com.bitwig.extensions.controllers.novation.launchkey_mk4.values;

import com.bitwig.extensions.framework.values.BasicStringValue;

public interface ControlValue {
    BasicStringValue getDisplayValue();
    
    void incrementBy(final int inc, boolean modifier);
    
}
