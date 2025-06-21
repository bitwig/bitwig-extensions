package com.bitwig.extensions.controllers.arturia.keylab.mk3.display;

public enum SmallIcon implements IdItem {
    BLUE_HAND(0),
    BROWSING_ARROW_DEFAULT(1),
    BROWSING_ARROW_DOWN(2),
    BROWSING_ARROW_LEFT(3),
    BROWSING_ARROW_RIGHT(4),
    BUTTON_ARMED_AUDIO_DEFAULT(5),
    BUTTON_ARMED_MIDI_DEFAULT(6),
    BUTTON_CUBASE_MONITOR_DEFAULT(7),
    SETTINGS(8),
    TOOL(9),
    MUTE(10),
    ARROW_DOWN(11),
    ARROW_RIGHT(12),
    SOLO(13),
    STOP(14),
    CHECK_BIG(15),
    DELETE(16),
    HEART_DEFAULT(17),
    HEART_LIKED(18),
    MIXER(19),
    SEARCH_DEFAULT(20),
    SEARCH_ON(21),
    ;
    
    private final int id;
    
    SmallIcon(final int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
}
