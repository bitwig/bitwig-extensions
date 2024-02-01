package com.bitwig.extensions.controllers.akai.apc64.control;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.akai.apc.common.led.SingleLedState;

public class FaderLightState extends InternalHardwareLightState {
    
    public static final FaderLightState OFF = new FaderLightState(0);
    public static final FaderLightState V_WHITE = new FaderLightState(1);
    public static final FaderLightState V_RED = new FaderLightState(2);
    public static final FaderLightState BIPOLOAR_WHITE = new FaderLightState(3);
    public static final FaderLightState BIPOLOAR_RED = new FaderLightState(4);
    
    private int code;
    
    private FaderLightState(int code) {
        this.code = code;
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
        if(o instanceof FaderLightState state) {
            return state.code == code;
        }
        return false;
    }
    
    public int getCode() {
        return code;
    }
}
