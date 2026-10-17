package com.bitwig.extensions.controllers.akai.mpkmk4;

import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.ValueObject;

public class GlobalStates {
    
    private final BooleanValueObject shiftHeld = new BooleanValueObject();
    private final MpkVariant variant;
    private final BasicStringValue focusDeviceName = new BasicStringValue();
    private final ValueObject<MpkBaseMode> mpkBaseMode = new ValueObject<>(MpkBaseMode.DAW);
    
    public GlobalStates(final MpkVariant variant) {
        this.variant = variant;
    }
    
    public MpkVariant getVariant() {
        return variant;
    }
    
    public BooleanValueObject getShiftHeld() {
        return shiftHeld;
    }
    
    public BasicStringValue getFocusDeviceName() {
        return focusDeviceName;
    }
    
    public ValueObject<MpkBaseMode> getMpkBaseMode() {
        return mpkBaseMode;
    }
}
