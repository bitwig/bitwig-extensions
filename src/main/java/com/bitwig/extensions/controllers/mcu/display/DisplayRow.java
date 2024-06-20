package com.bitwig.extensions.controllers.mcu.display;

public enum DisplayRow {
    LABEL(0),
    VALUE(1);

    final int rowIndex;

    DisplayRow(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public int getRowIndex() {
        return rowIndex;
    }
}
