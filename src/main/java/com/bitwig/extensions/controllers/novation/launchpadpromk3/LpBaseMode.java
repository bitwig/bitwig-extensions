package com.bitwig.extensions.controllers.novation.launchpadpromk3;

public enum LpBaseMode {
    SESSION(0), //
    FADER(1),  //
    NOTE(4, true),  //
    CHORD(2, true),  //
    CUSTOM(3),  //
    SCALE_SETTINGS(5, true),  //
    SEQUENCER_SETTINGS(6, true), //
    SEQUENCER_STEPS(7, true), //
    SEQUENCER_VELOCITY(8, true), //
    SEQUENCER_PATTERN_SETTINGS(9, true), //
    SEQUENCER_PROBABILITY(10, true), //
    SEQUENCER_MUTATION(11, true), //
    SEQUENCER_MICROSTEP(12, true), //
    SEQUENCER_PROJECTS(13, true), //
    SEQUENCER_PATTERNS(14, true), //
    SEQUENCER_TEMPO(15, true), //
    SEQUENCER_SWING(16, true), //
    PROGRAMMER(17), //
    SETTINGS(18), //
    CUSTOM_MODE_SETTINGS(19);
    
    private final int sysExId;
    private final boolean noteHandler;
  
    LpBaseMode(final int sysExId, boolean noteHandler) {
        this.noteHandler = noteHandler;
        this.sysExId = sysExId;
    }
    
    LpBaseMode(final int sysExId) {
        this(sysExId, false);
    }
    
    public int getSysExId() {
        return sysExId;
    }
    
    public boolean isNoteHandler() {
        return noteHandler;
    }
    
    public static LpBaseMode toMode(final int id) {
        final LpBaseMode[] values = LpBaseMode.values();
        for (LpBaseMode v : values) {
            if (v.sysExId == id) {
                return v;
            }
        }
        return LpBaseMode.SESSION;
    }
}
