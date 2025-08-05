package com.bitwig.extensions.controllers.nativeinstruments.komplete;

public enum LayoutType {
    LAUNCHER("MIX"), //
    ARRANGER("ARRANGE");
    
    private final String name;
    
    LayoutType(final String name) {
        this.name = name;
    }
    
    public static LayoutType toType(final String layoutName) {
        for (final LayoutType layoutType : LayoutType.values()) {
            if (layoutType.name.equals(layoutName)) {
                return layoutType;
            }
        }
        return LAUNCHER;
    }
    
}
