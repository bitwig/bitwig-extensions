package com.bitwig.extensions.controllers.mcu.value;

import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.IntValueObject;

public interface IEnumDisplayValue {
    BasicStringValue getDisplayValue();
    
    IntValueObject getRingValue();
    
    void increment(int inc);
    
    void setIndex(int index);
    
    String getEnumValue();
    
    void reset();
    
    void stepRoundRobin();
}
