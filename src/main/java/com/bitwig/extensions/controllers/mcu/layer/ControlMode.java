package com.bitwig.extensions.controllers.mcu.layer;

public enum ControlMode {
    VOLUME(true),
    PAN(true),
    SENDS(true),
    TRACK,
    STD_PLUGIN,
    EQ,
    DEVICE,
    TRACK_REMOTES,
    PROJECT_REMOTES,
    MENU;
    
    private final boolean isMixer;
    
    ControlMode() {
        this(false);
    }
    
    ControlMode(final boolean isMixer) {
        this.isMixer = isMixer;
    }
    
    public boolean isMixer() {
        return isMixer;
    }
}
