package com.bitwig.extensions.controllers.mcu.config;

public enum EncoderBehavior {
    ACCEL, // Signed bit
    STEP,  // 1 and 127 ;
    STEP_1_65(1, 65);
    private final int upValue;
    private final int downValue;

    EncoderBehavior() {
        this(1, -1);
    }

    EncoderBehavior(final int upValue, final int downValue) {
        this.upValue = upValue;
        this.downValue = downValue;
    }

    public int getDownValue() {
        return downValue;
    }

    public int getUpValue() {
        return upValue;
    }
}
