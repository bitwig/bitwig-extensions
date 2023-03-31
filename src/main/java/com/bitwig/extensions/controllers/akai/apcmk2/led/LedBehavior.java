package com.bitwig.extensions.controllers.akai.apcmk2.led;

public enum LedBehavior {
    LIGHT_10(0),
    LIGHT_25(1),
    LIGHT_50(2),
    LIGHT_60(3),
    LIGHT_75(4),
    LIGHT_90(5),
    FULL(6),
    PULSE_16(7),
    PULSE_8(8),
    PULSE_4(9),
    PULSE_2(10),
    BLINK_24(11),
    BLINK_16(12),
    BLINK_8(13),
    BLINK_4(14),
    BLINK_2(15);
    final int code;
    LedBehavior(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
}
