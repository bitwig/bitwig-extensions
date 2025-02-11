package com.bitwig.extensions.controllers.novation.slmk3.value;

public enum ClipFocus {
    LAUNCHER("Launcher"),
    ARRANGER("Arranger");
    
    private final String displayName;
    
    ClipFocus(final String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
}
