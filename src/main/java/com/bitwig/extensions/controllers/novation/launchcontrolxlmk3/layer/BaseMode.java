package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

public enum BaseMode {
    MIXER,
    DAW;
    
    public static BaseMode toMode(final int value) {
        return switch (value) {
            case 1 -> MIXER;
            case 2 -> DAW;
            default -> null;
        };
    }
}
