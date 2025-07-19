package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;

/**
 * Midi CC assignment constants.
 */
public enum CcAssignment {
    PLAY(0x10, "PLAY_BUTTON"), //
    RESTART(0x11, "RESTART_BUTTON"), //
    REC(0x12, "REC_BUTTON"), //
    COUNT_IN(0x13, "COUNTIN_BUTTON"), //
    STOP(0x14, "STOP_BUTTON"), //
    CLEAR(0x15), //
    LOOP(0x16, "LOOP_BUTTON"), //
    METRO(0x17, "METRO_BUTTON"), //
    TAP_TEMPO(0x18, "TAP_BUTTON"), //
    UNDO(0x20, "UNDO_BUTTON"), //
    REDO(0x21, "REDO_BUTTON"), //
    QUANTIZE(0x22), //
    AUTO(0x23, "AUTO_BUTTON"), //
    PRESS_4D_KNOB(0x60), //
    PRESS_4D_KNOB_SHIFT(0x61), //
    VOLUME_CURRENT(0x64), //
    PAN_CURRENT(0x65), //
    MUTE_CURRENT(0x66), //
    SOLO_CURRENT(0x67);
    
    private final int stateId;
    private final String name;
    
    CcAssignment(final int stateId) {
        this(stateId, null);
    }
    
    CcAssignment(final int stateId, final String name) {
        this.stateId = stateId;
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isMapped() {
        return name != null;
    }
    
    public int getStateId() {
        return stateId;
    }
    
    public HardwareActionMatcher createActionMatcher(final MidiIn midiIn, final int matchvalue) {
        return midiIn.createCCActionMatcher(15, stateId, matchvalue);
    }
    
}
