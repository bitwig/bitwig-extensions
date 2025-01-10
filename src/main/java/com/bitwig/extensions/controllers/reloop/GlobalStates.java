package com.bitwig.extensions.controllers.reloop;

import com.bitwig.extensions.framework.values.BooleanValueObject;

public class GlobalStates {
    
    private final BooleanValueObject shiftState = new BooleanValueObject();
    
    private final BooleanValueObject ccState = new BooleanValueObject();
    
    public BooleanValueObject getShiftState() {
        return shiftState;
    }
    
    public BooleanValueObject getCcState() {
        return ccState;
    }
}
