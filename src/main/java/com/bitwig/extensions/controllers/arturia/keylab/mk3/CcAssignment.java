package com.bitwig.extensions.controllers.arturia.keylab.mk3;

public enum CcAssignment {
    SAVE(0x29, 0xf), //
    QUANTIZE(0x2A, 0xE), //
    UNDO(0x2B, 0xD), //
    REDO(0x2C, 0xC), //
    STOP(0x14, 0x7), //
    PLAY(0x15, 0x6), //
    RECORD(0x16, 0x5), //
    TAP(0x17, 0x4), //
    LOOP(0x18, 0x8), //
    REWIND(0x19, 0x9), //
    FAST_FWD(0x1A, 0xA), //
    METRO(0x1B, 0xB), //
    BACK(0x28, 0x1D),//
    ;
    
    // ENCODER 0x74  => 0x75 Turn
    private final int ccNr;
    private final byte ledId;
    
    CcAssignment(final int ccNr, final int ledId) {
        this.ccNr = ccNr;
        this.ledId = (byte) ledId;
    }
    
    public int getCcNr() {
        return ccNr;
    }
    
    public byte getLedId() {
        return ledId;
    }
}
