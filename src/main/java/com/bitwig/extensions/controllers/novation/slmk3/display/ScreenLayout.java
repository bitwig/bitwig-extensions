package com.bitwig.extensions.controllers.novation.slmk3.display;

public enum ScreenLayout {
    EMPTY(0),
    KNOB(1),
    BOX(2);
    private final int id;
    
    ScreenLayout(final int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
}
