package com.bitwig.extensions.controllers.novation.launchkey_mk4;

public enum EncoderMode {
    PLUGIN,
    MIXER,
    SENDS,
    TRANSPORT,
    CUSTOM;
    
    EncoderMode() {
    }
    
    public static EncoderMode toMode(final int id) {
        return switch (id) {
            case 1 -> MIXER;
            case 2 -> PLUGIN;
            case 3 -> TRANSPORT;
            case 4 -> SENDS;
            default -> CUSTOM;
        };
    }
}
