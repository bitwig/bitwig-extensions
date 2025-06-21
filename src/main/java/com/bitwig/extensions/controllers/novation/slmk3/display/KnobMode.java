package com.bitwig.extensions.controllers.novation.slmk3.display;

public enum KnobMode {
    DEVICE,
    TRACK,
    PROJECT,
    PAN,
    OPTION,
    SEND,
    DRUM_VOLUME(true),
    DRUM_PAN(true),
    DRUM_SENDS(true),
    SEQUENCER,
    OPTION_SHIFT;
    
    final boolean drumMode;
    
    KnobMode() {
        this(false);
    }
    
    KnobMode(final boolean isDrumMode) {
        this.drumMode = isDrumMode;
    }
    
    public boolean isDrumMode() {
        return drumMode;
    }
}
