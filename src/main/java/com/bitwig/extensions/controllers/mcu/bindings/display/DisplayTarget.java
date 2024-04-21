package com.bitwig.extensions.controllers.mcu.bindings.display;

import com.bitwig.extensions.controllers.mcu.display.DisplayRow;

public record DisplayTarget(int rowIndex, int cellIndex, int sectionIndex, Object destination) {
    public static DisplayTarget of(final int rowIndex, final int cellIndex, final int sectionIndex,
        final Object destination) {
        return new DisplayTarget(rowIndex, cellIndex, sectionIndex, destination);
    }
    
    public static DisplayTarget of(final DisplayRow row, final int cellIndex, final int sectionIndex,
        final Object destination) {
        return new DisplayTarget(row.getRowIndex(), cellIndex, sectionIndex, destination);
    }
    
    public static DisplayTarget of(final DisplayRow row, final int cellIndex, final Object destination) {
        return new DisplayTarget(row.getRowIndex(), cellIndex, 0, destination);
    }
    
    public static DisplayTarget of(final int rowIndex, final int cellIndex, final Object destination) {
        return new DisplayTarget(rowIndex, cellIndex, 0, destination);
    }
    
    public static DisplayTarget of(final int rowIndex, final Object destination) {
        return new DisplayTarget(rowIndex, -1, 0, destination);
    }
    
}
