package com.bitwig.extensions.controllers.mcu.bindings.display;

import com.bitwig.extensions.controllers.mcu.display.DisplayRow;

public record DisplayTarget(int rowIndex, int cellIndex, int sectionIndex) {
    public static DisplayTarget of(final int rowIndex, final int cellIndex, final int sectionIndex) {
        return new DisplayTarget(rowIndex, cellIndex, sectionIndex);
    }
    
    public static DisplayTarget of(final DisplayRow row, final int cellIndex, final int sectionIndex) {
        return new DisplayTarget(row.getRowIndex(), cellIndex, sectionIndex);
    }
    
    public static DisplayTarget of(final DisplayRow row, final int cellIndex) {
        return new DisplayTarget(row.getRowIndex(), cellIndex, 0);
    }
    
    public static DisplayTarget of(final int rowIndex, final int cellIndex) {
        return new DisplayTarget(rowIndex, cellIndex, 0);
    }
    
    public static DisplayTarget of(final int rowIndex) {
        return new DisplayTarget(rowIndex, -1, 0);
    }
    
}
