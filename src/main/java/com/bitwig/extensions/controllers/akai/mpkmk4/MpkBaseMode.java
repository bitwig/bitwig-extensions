package com.bitwig.extensions.controllers.akai.mpkmk4;

public enum MpkBaseMode {
    DAW(2),
    PLUGIN(1),
    NONE(-1);
    
    private int sysExId;
    
    MpkBaseMode(final int sysExId) {
        this.sysExId = sysExId;
    }
    
    public int getSysExId() {
        return sysExId;
    }
    
    public static MpkBaseMode toMode(int id) {
        for (MpkBaseMode mode : MpkBaseMode.values()) {
            if (id == mode.sysExId) {
                return mode;
            }
        }
        return MpkBaseMode.NONE;
    }
}
