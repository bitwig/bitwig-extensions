package com.bitwig.extensions.controllers.novation.launchkey_mk4;

public enum CcAssignments {
    TRACK_LEFT(0x67),
    TRACK_RIGHT(0x66),
    NAV_UP(0x6A),
    NAV_DOWN(0x6B),
    SCENE_LAUNCH(0x68),
    LAUNCH_MODE(0x69),
    PARAM_UP(0x33),
    PARAM_DOWN(0x34),
    TRACK_MODE(0x2D),
    METRO(0x4C),
    CAPTURE(0x4A),
    UNDO(0x4D),
    QUANTIZE(0x4B),
    PLAY(0x73),
    STOP(0x74),
    LOOP(0x76),
    REC(0x75);
    final int ccNr;
    
    CcAssignments(final int ccNr) {
        this.ccNr = ccNr;
    }
    
    public int getCcNr() {
        return ccNr;
    }
}
