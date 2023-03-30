package com.bitwig.extensions.controllers.akai.apcmk2;

public class ApcConfiguration {
    private final int sceneRows;
    private final int trackButtonBase;
    private final int sceneLaunchBase;
    private final int shiftButtonValue;
    private final boolean hasEncoders;
    
    public ApcConfiguration(
        boolean hasEncoders,
        int sceneRows,
        int trackButtonBase,
        int sceneLaunchBase,
        int shiftButtonValue) {
        this.hasEncoders = hasEncoders;
        this.sceneRows = sceneRows;
        this.trackButtonBase = trackButtonBase;
        this.sceneLaunchBase = sceneLaunchBase;
        this.shiftButtonValue = shiftButtonValue;
    }
    
    public boolean isHasEncoders() {
        return hasEncoders;
    }
    
    public int getSceneRows() {
        return sceneRows;
    }
    
    public int getTrackButtonBase() {
        return trackButtonBase;
    }
    
    public int getSceneLaunchBase() {
        return sceneLaunchBase;
    }
    
    public int getShiftButtonValue() {
        return shiftButtonValue;
    }
}
