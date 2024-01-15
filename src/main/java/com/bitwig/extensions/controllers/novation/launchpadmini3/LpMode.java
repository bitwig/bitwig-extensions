package com.bitwig.extensions.controllers.novation.launchpadmini3;

public enum LpMode {
    SESSION(0, 0x14), KEYS(0x5, 0), DRUMS(0x4, 0), CUSTOM(0x6, 0), MIXER(0x17, 0x3E), OVERVIEW(0x18, 0x26);
    
    private final int buttonColor;
    final int modeId;
    
    LpMode(final int modeId, final int buttonColor) {
        this.modeId = modeId;
        this.buttonColor = buttonColor;
    }
    
    public int getModeId() {
        return modeId;
    }
    
    public int getButtonColor() {
        return buttonColor;
    }
}
