package com.bitwig.extensions.controllers.akai.apc64;

public class ApcOverviewGrid {
    
    private int sceneOffset;
    private int trackOffset;
    private int numberOfScenes;
    private int numberOfTracks;
    
    private int trackPosition;
    private int scenePosition;
    
    private final int[][] hasClips = new int[8][8];
    private final int[] sceneQueuedClips = new int[64];
    
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
        return trackPosition - trackOffset;
    }
    
    public int getTrackOffset() {
        return trackOffset;
    }
    
    public void setTrackPosition(final int trackPosition) {
        this.trackPosition = trackPosition;
        this.trackOffset = (trackPosition / 64) * 64;
    }
    
    public int getScenePosition() {
        return scenePosition - sceneOffset;
    }
    
    public void setScenePosition(final int scenePosition) {
        this.scenePosition = scenePosition;
        this.sceneOffset = (scenePosition / 64) * 64;
    }
    
    public int getSceneOffset() {
        return sceneOffset;
    }
    
    public void markSceneQueued(final int sceneIndex, final boolean isQueued) {
        if (isQueued) {
            sceneQueuedClips[sceneIndex]++;
        } else if (sceneQueuedClips[sceneIndex] > 0) {
            sceneQueuedClips[sceneIndex]--;
        }
    }
    
    public void setHasClips(final int trackIndex, final int sceneIndex, final boolean hasClip) {
        final int gridScene = (sceneIndex) / 8;
        final int gridTrack = (trackIndex) / 8;
        if (hasClip) {
            this.hasClips[gridTrack][gridScene]++;
        } else if (this.hasClips[gridTrack][gridScene] > 0) {
            this.hasClips[gridTrack][gridScene]--;
        }
    }
    
    public boolean hasClips(final int trackIndex, final int sceneIndex) {
        return this.hasClips[trackIndex][sceneIndex] > 0;
    }
    
    public boolean hasQueuedScenes(final int sceneIndex) {
        final int index = sceneIndex - sceneOffset;
        if (index > 63) {
            return false;
        }
        return this.sceneQueuedClips[sceneIndex - sceneOffset] > 0;
    }
    
    public boolean inGrid(final int trackIndex, final int sceneIndex) {
        final int posX = trackIndex * 8;
        final int posY = sceneIndex * 8;
        return posX < (numberOfTracks - trackOffset) && posY < (numberOfScenes - sceneOffset);
    }
}
