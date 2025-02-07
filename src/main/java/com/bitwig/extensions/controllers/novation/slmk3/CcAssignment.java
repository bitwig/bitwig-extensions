package com.bitwig.extensions.controllers.novation.slmk3;

public enum CcAssignment {
    SCREEN_UP(0x51, 0x3E),
    SCREEN_DOWN(0x52, 0x3F),
    SCENE_LAUNCH_1(0x53, 0x02),
    SCENE_LAUNCH_2(0x54, 0x03),
    PADS_UP(0x55, 0x0),
    PADS_DOWN(0x56, 0x1),
    SOFT_UP(0x57, 0x1c),
    SOFT_DOWN(0x58, 0x1d),
    GRID(0x59, 0x40),
    OPTIONS(0x5A, 0x41),
    DUPLICATE(0x5C, 0x42),
    CLEAR(0x5D, 0x43),
    TRACK_LEFT(0x66, 0x1E),
    TRACK_RIGHT(0x67, 0x1F),
    REWIND(0x70, 0x21),
    FAST_FORWARD(0x71, 0x22),
    STOP(0x72, 0x23),
    PLAY(0x73, 0x24),
    LOOP(0x74, 0x25),
    RECORD(0x75, 0x20);
    
    final int midiId;
    final int ledIndex;
    
    CcAssignment(final int midiId, final int ledIndex) {
        this.midiId = midiId;
        this.ledIndex = ledIndex;
    }
    
    public int getMidiId() {
        return midiId;
    }
    
    public int getLedIndex() {
        return ledIndex;
    }
}
