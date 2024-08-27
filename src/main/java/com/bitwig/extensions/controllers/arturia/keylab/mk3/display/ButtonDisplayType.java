package com.bitwig.extensions.controllers.arturia.keylab.mk3.display;

public enum ButtonDisplayType {
    OFF(0),
    SIMPLE(1),
    TOGGLE_OFF(2),
    TOGGLE_ON(3),
    TAB_FOLDED(4),
    TAB_FOLDED_DIS(5),
    TAB_UNFOLDED(6),
    TAB_UNFOLDED_DIS(7),
    ICON_TAB_FOLDED(8),
    ICON_TAB_UNFOLDED(9),
    SIMPLE_ICON(10),
    TOGGLE_ICON(11);
    
    private final int id;
    
    ButtonDisplayType(final int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
}
