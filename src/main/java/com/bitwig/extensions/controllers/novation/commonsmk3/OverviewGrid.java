package com.bitwig.extensions.controllers.novation.commonsmk3;

public class OverviewGrid {

    private int numberOfScenes;
    private int numberOfTracks;

    private int trackPosition;
    private int scenePosition;

    private int focusChanelPosition = 0;


    public int getNumberOfScenes() {
        return numberOfScenes;
    }

    public void setNumberOfScenes(final int numberOfScenes) {
        this.numberOfScenes = numberOfScenes;
    }

    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    public void setNumberOfTracks(final int numberOfTracks) {
        this.numberOfTracks = numberOfTracks;
    }

    public int getTrackPosition() {
        return trackPosition;
    }

    public void setTrackPosition(final int trackPosition) {
        this.trackPosition = trackPosition;
    }

    public int getScenePosition() {
        return scenePosition;
    }

    public void setScenePosition(final int scenePosition) {
        this.scenePosition = scenePosition;
    }

    public int getFocusChanelPosition() {
        return focusChanelPosition;
    }

    public void setFocusChanelPosition(final int focusChanelPosition) {
        this.focusChanelPosition = focusChanelPosition;
    }
}
