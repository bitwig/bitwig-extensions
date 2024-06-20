package com.bitwig.extensions.controllers.akai.apc64;

import java.util.Arrays;

public enum PadMode {
    SESSION(0, true, false),
    OVERVIEW(1, true, false),
    NOTE(2, false, true),
    CHORD(3),
    CHORD_SETTINGS(4),
    DRUM(5, true, true),
    STEP_SEQUENCER(6),
    STEP_SEQUENCER_SETTINGS(7),
    PROJECT(8),
    CUSTOM(9),
    CUSTOM_SETTINGS(10),
    UNKNOWN(-1);

    private final int modeId;
    private final boolean hasLocalControl;
    private final boolean isKeyRelated;

    PadMode(int modeId, boolean hasLocalControl, boolean isKeyRelated) {
        this.modeId = modeId;
        this.hasLocalControl = hasLocalControl;
        this.isKeyRelated = isKeyRelated;
    }

    PadMode(int modeId) {
        this(modeId, false, false);
    }

    public int getModeId() {
        return modeId;
    }

    public static PadMode fromId(int id) {
        return Arrays.stream(PadMode.values()).filter(mode -> mode.getModeId() == id).findFirst().orElse(UNKNOWN);
    }

    public boolean hasLocalControl() {
        return hasLocalControl;
    }

    public boolean isKeyRelated() {
        return isKeyRelated;
    }
}
