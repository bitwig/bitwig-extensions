package com.bitwig.extensions.controllers.reloop;

public enum Assignment {
    PLAY(0x50),
    STOP(0x52),
    RECORD(0x54),
    REC_OVERDUB(0x55),
    ZOOM_OUT(0x5A),
    ZOOM_IN(0x58),
    AUTO(0x5C),
    UNDO(0x57),
    STOP_ALL(0x53),
    SCENE_LAUNCH(0x51),
    GRID_BACK(0x59),
    GRID_FORWARD(0x5B),
    METRO(0x5E),
    LOOP(0x56),
    TAP(0x62),
    CST1(0x69),
    SCENE_DOWN(0x71),
    SCENE_UP(0x73),
    PREV_MARKER(0x5D),
    NEXT_MARKER(0x5F),
    MARKER(0x63),
    CST_SHIFT(0x6A),
    VIEW(0x61),
    BACK(0x65),
    SHIFT_RESET(0x74);
    
    private final int channel;
    private final int value;
    
    Assignment(final int value) {
        this(1, value);
    }
    
    Assignment(final int channel, final int value) {
        this.channel = channel;
        this.value = value;
    }
    
    public int getChannel() {
        return channel;
    }
    
    public int getValue() {
        return value;
    }
}
