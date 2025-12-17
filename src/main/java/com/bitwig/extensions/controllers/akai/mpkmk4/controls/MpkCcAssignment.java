package com.bitwig.extensions.controllers.akai.mpkmk4.controls;

public enum MpkCcAssignment {
    
    PLAY(0x4C), //
    REC(0x4D), //
    OVER(0x4E), //
    LOOP(0x4A), //
    UNDO(0x49), //
    BANK_LEFT(0x50), //
    BANK_RIGHT(0x51), //
    BANK_AB(0xC), //
    TAP_TEMPO(0x52), //
    NOTE_REPEAT(0x52);
    
    private final int ccNr;
    
    MpkCcAssignment(final int ccNr) {
        this.ccNr = ccNr;
    }
    
    public int getCcNr() {
        return ccNr;
    }
}
