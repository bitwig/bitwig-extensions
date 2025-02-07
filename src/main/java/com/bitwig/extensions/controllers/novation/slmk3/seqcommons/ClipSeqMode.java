package com.bitwig.extensions.controllers.novation.slmk3.seqcommons;

public enum ClipSeqMode {
    KEYS("Poly/Key"),
    DRUM("Mono/Drum");
    
    private final String displayName;
    
    ClipSeqMode(final String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
