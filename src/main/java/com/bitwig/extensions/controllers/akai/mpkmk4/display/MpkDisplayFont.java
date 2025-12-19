package com.bitwig.extensions.controllers.akai.mpkmk4.display;

public enum MpkDisplayFont {
    PT16(0x0),
    PT16_BOLD(0x1),
    PT24(0x2),
    PT24_BOLD(0x4);
    private final int value;
    
    MpkDisplayFont(final int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
}
