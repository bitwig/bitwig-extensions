package com.bitwig.extensions.controllers.akai.apcmk2.led;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class SingleLedState extends InternalHardwareLightState {
    
    public static final SingleLedState OFF = new SingleLedState(0);
    public static final SingleLedState ON = new SingleLedState(1);
    public static final SingleLedState BLINK = new SingleLedState(2);
    
    private final int code;
    
    private SingleLedState(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }
    
    @Override
    public boolean equals(final Object o) {
        if(o == this) {
            return true;
        }
        if(o instanceof SingleLedState) {
            return ((SingleLedState)o).code == code;
        }
        return false;
    }
}
