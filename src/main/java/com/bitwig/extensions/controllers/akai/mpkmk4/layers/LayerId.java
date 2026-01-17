package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

public enum LayerId {
    MAIN,
    CLIP_LAUNCHER,
    TRACK_PAD_CONTROL,
    DRUM_PAD_CONTROL,
    DEVICE_REMOTES(true),
    PROJECT_REMOTES(true),
    TRACK_REMOTES(true),
    TRACK_CONTROL,
    MIX_CONTROL,
    NAVIGATION,
    PAD_MENU_LAYER,
    SHIFT,
    OVER_LAYER;
    
    private final boolean controlsDevice;
    
    LayerId(final boolean controlsDevice) {
        this.controlsDevice = controlsDevice;
    }
    
    LayerId() {
        this(false);
    }
    
    public boolean isControlsDevice() {
        return controlsDevice;
    }
}
