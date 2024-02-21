package com.bitwig.extensions.controllers.akai.apc.common.led;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class VarSingleLedState extends InternalHardwareLightState {
    
    public static final VarSingleLedState OFF = new VarSingleLedState(0);
    public static final VarSingleLedState LIGHT_10= new VarSingleLedState(1);
    public static final VarSingleLedState LIGHT_25= new VarSingleLedState(2);
    public static final VarSingleLedState LIGHT_50= new VarSingleLedState(3);
    public static final VarSingleLedState LIGHT_60= new VarSingleLedState(4);
    public static final VarSingleLedState LIGHT_75= new VarSingleLedState(5);
    public static final VarSingleLedState LIGHT_90= new VarSingleLedState(6);
    public static final VarSingleLedState FULL= new VarSingleLedState(7);
    public static final VarSingleLedState PULSE_16= new VarSingleLedState(8);
    public static final VarSingleLedState PULSE_8= new VarSingleLedState(9);
    public static final VarSingleLedState PULSE_4= new VarSingleLedState(10);
    public static final VarSingleLedState PULSE_2= new VarSingleLedState(11);
    public static final VarSingleLedState BLINK_24= new VarSingleLedState(12);
    public static final VarSingleLedState BLINK_16= new VarSingleLedState(13);
    public static final VarSingleLedState BLINK_8= new VarSingleLedState(14);
    public static final VarSingleLedState BLINK_4= new VarSingleLedState(15);
    public static final VarSingleLedState BLINK_2= new VarSingleLedState(16);
    
    private final int code;
    
    protected VarSingleLedState(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code == 0 ? 0 : 1;
    }
    
    public int getChannel() {
        return code == 0 ? 0 : code-1;
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
        if(o instanceof VarSingleLedState) {
            return ((VarSingleLedState)o).code == code;
        }
        return false;
    }
}
