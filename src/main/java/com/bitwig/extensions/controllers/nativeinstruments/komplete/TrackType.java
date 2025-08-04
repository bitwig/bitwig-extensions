package com.bitwig.extensions.controllers.nativeinstruments.komplete;

/**
 * The track types as recognized by KK.
 */
public enum TrackType {
    NONE(0, ""), //
    UNSPECIFIED(1, "Unspec"), //
    INSTRUMENT(2, "Instrument"), //
    AUDIO(3, "Audio"), //
    GROUP(4, "Group"), //
    RETURN_BUS(5, "Effect"), //
    MASTER(6, "Master"); //
    
    private final int id;
    private final String type;
    
    TrackType(final int id, final String type) {
        this.id = id;
        this.type = type;
    }
    
    public int getId() {
        return id;
    }
    
    public static TrackType toType(final String type) {
        for (final TrackType trackType : TrackType.values()) {
            if (trackType.type.equals(type)) {
                return trackType;
            }
        }
        return UNSPECIFIED;
    }
}
