package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.commonsmk3.CCSource;

public enum LabelCcAssignments implements CCSource {
    RECORD_ARM_UNDO(1), //
    MUTE_REDO(2), //
    SOLO_CLICK(3), //
    VOLUME(4), //
    PAN(5), //
    SENDS_TAP(6), //
    DEVICE_TEMPO(7), //
    STOP_CLIP_SWING(8), //
    SHIFT(90), //
    TRACK_SEL_1(101, true), //
    TRACK_SEL_2(102, true), //
    TRACK_SEL_3(103, true), //
    TRACK_SEL_4(104, true), //
    TRACK_SEL_5(105, true), //
    TRACK_SEL_6(106, true), //
    TRACK_SEL_7(107, true), //
    TRACK_SEL_8(107, true), //
    R1_PATTERNS(89, true), //
    R2_STEPS(79, true), //
    R3_PAT_SETTINGS(69, true), //
    R4_VELOCITY(59, true), //
    R5_PROBABILITY(49, true), //
    R6_MUTATION(39, true), //
    R7_MICROSTEP(29, true), //
    R8_PRINT_TO_CLIP(19, true), //
    REC(10), //
    PLAY(20), //
    FIXED_LENGTH(30), //
    QUANTIZE(40), //
    DUPLICATE(50),
    CLEAR(60),
    DOWN(70),
    UP(80),
    LEFT(91), //
    RIGHT(92), //
    SESSION(93), //
    NOTE(94), //
    CHORD(95), //
    CUSTOM(96), //
    SEQUENCER(97), //
    PROJECTS(98);

    private final int ccValue;
    private final boolean isIndexReference;

    LabelCcAssignments(final int ccValue, final boolean isIndexReference) {
        this.ccValue = ccValue;
        this.isIndexReference = isIndexReference;
    }

    LabelCcAssignments(final int ccValue) {
        this(ccValue, false);
    }

    @Override
	public int getCcValue() {
        return ccValue;
    }

    public HardwareActionMatcher createMatcher(final MidiIn midiIn, final int matchValue) {
        return midiIn.createCCActionMatcher(0, ccValue, matchValue);
    }

    public boolean isIndexReference() {
        return isIndexReference;
    }
}
