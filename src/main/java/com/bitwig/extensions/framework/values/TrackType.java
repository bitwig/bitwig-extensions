package com.bitwig.extensions.framework.values;

public enum TrackType {
    
    EFFECT,
    INSTRUMENT,
    AUDIO,
    MASTER,
    GROUP,
    NONE;
    
    TrackType() {
    }
    
    public static TrackType toType(final String value) {
        return switch (value) {
            case "Effect" -> EFFECT;
            case "Audio" -> AUDIO;
            case "Instrument" -> INSTRUMENT;
            case "Master" -> MASTER;
            case "Group" -> GROUP;
            default -> NONE;
        };
    }
    
    public boolean canBeArmed() {
        return this == INSTRUMENT || this == AUDIO;
    }
}
