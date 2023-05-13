package com.bitwig.extensions.controllers.novation.launchpadmini3;

public enum TrackMode {
    NONE(1),
    STOP(7),
    SOLO(15),
    MUTE(11),
    ARM(7),
    CONTROL(27);
    private final int colorIndex;

    TrackMode(final int colorIndex) {
        this.colorIndex = colorIndex;
    }

    public int getColorIndex() {
        return colorIndex;
    }
}
