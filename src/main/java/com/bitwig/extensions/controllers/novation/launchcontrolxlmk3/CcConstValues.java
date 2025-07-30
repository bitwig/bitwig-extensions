package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3;

public enum CcConstValues {
    PAGE_UP(0x6A),
    PAGE_DOWN(0x6B),
    TRACK_LEFT(0x67),
    TRACK_RIGHT(0x66),
    DAW_SPEC(0x68),
    SOLO_ARM_MODE(0x41),
    MUTE_SELECT_MODE(0x42),
    PLAY(0x74),
    RECORD(0x76);
    
    private final int ccNr;
    
    CcConstValues(final int ccNr) {
        this.ccNr = ccNr;
    }
    
    public int getCcNr() {
        return ccNr;
    }
}
