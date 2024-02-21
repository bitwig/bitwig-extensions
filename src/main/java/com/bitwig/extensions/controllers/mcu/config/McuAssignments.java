package com.bitwig.extensions.controllers.mcu.config;

public enum McuAssignments implements ButtonAssignment {
    PLAY(94), //
    STOP(93), //
    RECORD(95), //
    REWIND(91), //
    FFWD(92), //
    AUTO_WRITE(75), //
    AUTO_READ_OFF(74), //
    TRIM(76),
    TOUCH(77),
    LATCH(78),
    GROUP(79), //
    SOLO_BASE(8, true),
    REC_BASE(0, true),
    MUTE_BASE(16, true),
    SELECT_BASE(24, true), //
    ENC_PRESS_BASE(32, true), //
    SIGNAL_BASE(104, true), //
    TOUCH_VOLUME(104, true), //
    SHIFT(70), //
    OPTION(71), //
    CONTROL(72), //
    ALT(73), //
    UNDO(81), //
    SAVE(80), // ICON VST
    CANCEL(82),
    ENTER(83), //
    MARKER(84),
    NUDGE(85),
    CYCLE(86),
    DROP(87), //     PUNCH IN
    REPLACE(88), //  PUNCH OUT
    CLICK(89),
    SOLO(90), //
    FLIP(50), //
    DISPLAY_NAME(52),
    DISPLAY_SMPTE(53), //
    BEATS_MODE(114),
    SMPTE_MODE(113), //
    V_TRACK(40),
    V_SEND(41),
    V_PAN(42),
    V_PLUGIN(43),
    V_EQ(44),
    V_INSTRUMENT(45), //
    F1(54),
    F2(55),
    F3(56),
    F4(57),
    F5(58),
    F6(59),
    F7(60),
    F8(61), //
    CURSOR_UP(96),
    CURSOR_DOWN(97),
    CURSOR_LEFT(98),
    CURSOR_RIGHT(99), //
    ZOOM(100),
    SCRUB(101), //
    BANK_LEFT(46),
    BANK_RIGHT(47), //
    TRACK_LEFT(48), //
    TRACK_RIGHT(49), //
    GLOBAL_VIEW(51), //
    GV_MIDI_LF1(62), //
    GV_INPUTS_LF2(63), //
    GV_AUDIO_LF3(64), //
    GV_INSTRUMENT_LF4(65), //
    GV_AUX_LF5(66), //
    GV_BUSSES_LF6(67), //
    GV_OUTPUTS_LF7(68), //
    GV_USER_LF8(69), //
    GV_USER_LF8_G2(51),
    REDO(71), //
    AUTO_OVERRIDE(117),  // is only overridden
    MIXER(118),  // is only overridden
    STEP_SEQ(115), // is only overridden
    CLIP_OVERDUB(116); // is only overridden

    private final int notNo;
    private final boolean baseAssignment;

    McuAssignments(final int noteNo, final boolean baseAssignment) {
        notNo = noteNo;
        this.baseAssignment = baseAssignment;
    }

    McuAssignments(final int notNo) {
        this(notNo, false);
    }

    public int getNoteNo() {
        return notNo;
    }

    public boolean isSingle() {
        return !baseAssignment;
    }

    @Override
    public int getChannel() {
        return 0;
    }
}
