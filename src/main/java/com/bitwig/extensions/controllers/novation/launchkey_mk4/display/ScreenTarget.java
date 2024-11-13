package com.bitwig.extensions.controllers.novation.launchkey_mk4.display;

public enum ScreenTarget {
    STATIONARY(0x20),
    GLOBAL(0x21),
    DAW(0x22),
    DAW_DRUM(0x23),
    MIXER(0x24),
    PLUGIN(0x25),
    SENDS(0x26),
    TRANSPORT(0x27),
    VOLUME(0x28);
    final byte id;
    
    ScreenTarget(final int id) {
        this.id = (byte) id;
    }
    
    public byte getId() {
        return id;
    }
}
