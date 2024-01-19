package com.bitwig.extensions.controllers.akai.apc64;

public enum Apc64CcAssignments {
    SCENE_BUTTON_BASE(0x70, true), //
    GRID_BASE(0x0, true),
    STRIP_TOUCH(0x52, true),
    TRACKS_BASE(0x64, true),
    TRACK_CONTROL_BASE(0x40, true),
    NAV_LEFT(0x60),
    NAV_RIGHT(0x61),
    NAV_DOWN(0x5E),
    NAV_UP(0x5F),
    MODE_REC(0x6C),
    MODE_MUTE(0x6D),
    MODE_SOLO(0x6E),
    MODE_STOP(0x6F),
    STRIP_DEVICE(0x79),
    STRIP_VOLUME(0x7A),
    STRIP_PAN(0x7B),
    STRIP_SENDS(0x7C),
    STRIP_CHANNEL(0x7D),
    STRIP_OFF(0x7E),
    CLEAR(0x49),
    DUPLICATE(0x4A),
    FIXED(0x4C),
    QUANTIZE(0x4B),
    UNDO(0x4D),
    TEMPO(0x48),
    SHIFT(0x78),
    PLAY(0x5B),
    STOP(0x5D),
    REC(0x5C);
    
    private int stateId;
    private boolean isBaseStart;
    
    Apc64CcAssignments(final int stateId) {
        this(stateId, false);
    }
    Apc64CcAssignments(final int stateId, boolean isBaseStart) {
        this.isBaseStart = isBaseStart;
        this.stateId = stateId;
    }
    
    public int getStateId() {
        return stateId;
    }
    
    public boolean isBaseStart() {
        return isBaseStart;
    }
    
    public boolean isSingle() {
        return !isBaseStart;
    }
}
