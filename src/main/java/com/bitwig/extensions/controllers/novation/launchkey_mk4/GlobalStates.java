package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class GlobalStates {
    private final BooleanValueObject shiftState = new BooleanValueObject();
    
    public BooleanValueObject getShiftState() {
        return shiftState;
    }
}
