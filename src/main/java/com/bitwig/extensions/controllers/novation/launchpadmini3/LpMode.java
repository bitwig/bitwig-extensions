package com.bitwig.extensions.controllers.novation.launchpadmini3;

public enum LpMode {
    SESSION(0),
    KEYS(0x5),
    DRUMS(0x4),
    CUSTOM(0x6),
    MIXER(0x17),
    OVERVIEW(0x18);

    final int modeId;

    LpMode(final int modeId) {
        this.modeId = modeId;
    }

    public int getModeId() {
        return modeId;
    }
}
