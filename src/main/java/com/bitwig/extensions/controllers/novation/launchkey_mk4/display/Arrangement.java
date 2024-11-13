package com.bitwig.extensions.controllers.novation.launchkey_mk4.display;

public enum Arrangement {
    TWO_LINES(1),
    THREE_LINES(2),
    LINE_8NAMES(3),
    PARAMETER(4);
    final int val;
    
    Arrangement(final int val) {
        this.val = val;
    }
    
    public int getVal() {
        return val;
    }
}
