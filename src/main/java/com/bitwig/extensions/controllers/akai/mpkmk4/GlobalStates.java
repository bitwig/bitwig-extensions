package com.bitwig.extensions.controllers.akai.mpkmk4;

import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class GlobalStates {
    
    private final BooleanValueObject shiftHeld = new BooleanValueObject();
    private final MpkMk4ControllerExtension.Variant variant;
    private final BasicStringValue focusDeviceName =  new BasicStringValue();
    
    public GlobalStates(final MpkMk4ControllerExtension.Variant variant) {
        this.variant = variant;
    }
    
    public MpkMk4ControllerExtension.Variant getVariant() {
        return variant;
    }
    
    public BooleanValueObject getShiftHeld() {
        return shiftHeld;
    }
    
    public BasicStringValue getFocusDeviceName() {
        return focusDeviceName;
    }
}
