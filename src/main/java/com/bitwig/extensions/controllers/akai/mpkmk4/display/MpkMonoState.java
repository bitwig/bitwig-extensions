package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class MpkMonoState extends InternalHardwareLightState {
    public static final int SOLID_10 = 0;
    public static final int SOLID_25 = 1;
    public static final int SOLID_50 = 2;
    public static final int SOLID_65 = 3;
    public static final int SOLID_75 = 4;
    public static final int SOLID_90 = 6;
    public static final int SOLID_STATE = 6;
    public static final int PULSE1_16 = 7;
    public static final int PULSE1_8 = 8;
    public static final int PULSE1_4 = 9;
    public static final int PULSE1_2 = 10;
    public static final int BLINK1_24 = 11;
    public static final int BLINK1_16 = 12;
    public static final int BLINK1_8 = 13;
    public static final int BLINK1_4 = 14;
    public static final int BLINK1_2 = 15;
    
    public static MpkMonoState FULL_ON = new MpkMonoState(6, true);
    public static MpkMonoState OFF = new MpkMonoState(6, false);
    public static MpkMonoState DIMMED = new MpkMonoState(0, true);
    
    private final int state;
    private final boolean on;
    
    private MpkMonoState(final int state, final boolean on) {
        this.state = state;
        this.on = on;
    }
    
    public boolean isOn() {
        return on;
    }
    
    public int getState() {
        return state;
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }
    
    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof final MpkMonoState that)) {
            return false;
        }
        
        return state == that.state && on == that.on;
    }
    
    @Override
    public int hashCode() {
        int result = state;
        result = 31 * result + Boolean.hashCode(on);
        return result;
    }
}
