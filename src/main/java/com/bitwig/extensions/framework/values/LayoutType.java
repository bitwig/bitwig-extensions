package com.bitwig.extensions.framework.values;

public enum LayoutType {
    LAUNCHER("MIX"), //
    ARRANGER("ARRANGE"),
    EDIT("EDIT");
    
    private final String name;
    
    LayoutType(final String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public static LayoutType toType(final String layoutName) {
        for (final LayoutType layoutType : LayoutType.values()) {
            if (layoutType.name.equals(layoutName)) {
                return layoutType;
            }
        }
        return LAUNCHER;
    }
    
    public LayoutType other() {
        if (this == LAUNCHER) {
            return ARRANGER;
        }
        return LAUNCHER;
    }
    
}
