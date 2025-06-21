package com.bitwig.extensions.controllers.arturia.keylab.mk3.display;

public enum ScreenTarget implements IdItem {
    SCREEN_1_LINE(0xA),
    SCREEN_1_LINE_INVERTED(0xB),
    SCREEN_2_LINES(0xC),
    SCREEN_2_LINES_INVERTED(0xD),
    SCREEN_2_LINES_BIG(0xE),
    TOP_ICON_1_LINE(0xF),
    TOP_ICON_2_LINES(0x10),
    POP_SCREEN_ICON_2_LINES(0x1B),
    POP_SCREEN_1_LINE(0x17),
    POP_SCREEN_2_LINES(0x18),
    POP_SCREEN_3_LINES(0x19),
    POP_SCREEN_1_LINE_LEFT_ICON(0x1A),
    POP_SCREEN_2_LINE_LEFT_ICON(0x1B),
    POP_UP_KNOB(0x1C),
    POP_UP_FADER(0x1E),
    POP_UP_TOP_ICON_2LINES(0x1F);
    
    private final int targetId;
    
    ScreenTarget(final int targetId) {
        this.targetId = targetId;
    }
    
    public int getId() {
        return targetId;
    }
}
