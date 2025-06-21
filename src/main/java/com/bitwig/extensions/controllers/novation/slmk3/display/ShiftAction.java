package com.bitwig.extensions.controllers.novation.slmk3.display;

public enum ShiftAction {
    CLICK("Metronome"),
    FILL("", "Fill"),
    CL_OVERDUB("Launcher", "Overdub"),
    CL_AUTO("Launcher", "Automat."),
    AR_AUTO("Arranger", "Automat."),
    AUTO_OVERRIDE("Restore", "Automat."),
    UNDO("Undo"),
    REDO("Redo");
    private final String row2;
    private final String row1;
    
    ShiftAction(final String name) {
        this("", name);
    }
    
    ShiftAction(final String row1, final String row2) {
        this.row1 = row1;
        this.row2 = row2;
    }
    
    public String getRow2() {
        return row2;
    }
    
    public String getRow1() {
        return row1;
    }
}
