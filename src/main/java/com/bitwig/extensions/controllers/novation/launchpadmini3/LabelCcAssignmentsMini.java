package com.bitwig.extensions.controllers.novation.launchpadmini3;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.commonsmk3.CCSource;

public enum LabelCcAssignmentsMini implements CCSource {
    DOWN(0x5C),
    UP(0x5B), //
    LEFT(0x5D), //
    RIGHT(0x5E), //
    SESSION(0x5F), //
    DRUMS(0x60), //
    KEYS(0x61), //
    USER(0x62); //

    private final int ccValue;
    private final boolean isIndexReference;

    LabelCcAssignmentsMini(final int ccValue, final boolean isIndexReference) {
        this.ccValue = ccValue;
        this.isIndexReference = isIndexReference;
    }

    LabelCcAssignmentsMini(final int ccValue) {
        this(ccValue, false);
    }

    @Override
    public int getCcValue() {
        return ccValue;
    }

    @Override
    public HardwareActionMatcher createMatcher(final MidiIn midiIn, final int matchValue) {
        return midiIn.createCCActionMatcher(0, ccValue, matchValue);
    }

    public boolean isIndexReference() {
        return isIndexReference;
    }
}
