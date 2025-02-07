package com.bitwig.extensions.controllers.novation.slmk3;

import java.util.Arrays;

public class OverviewGrid {
    
    private final int[] hasClips = new int[2];
    private final int[] sceneQueuedClips = new int[2];
    
    public void markSceneQueued(final int sceneIndex, final boolean isQueued) {
        if (isQueued) {
            sceneQueuedClips[sceneIndex]++;
        } else if (sceneQueuedClips[sceneIndex] > 0) {
            sceneQueuedClips[sceneIndex]--;
        }
    }
    
    public void markHasClips(final int trackIndex, final int sceneIndex, final boolean hasClip) {
        if (hasClip) {
            hasClips[sceneIndex]++;
        } else if (sceneQueuedClips[sceneIndex] > 0) {
            hasClips[sceneIndex]--;
        }
    }
    
    public boolean hasClips(final int sceneIndex) {
        return this.hasClips[sceneIndex] > 0;
    }
    
    public boolean hasQueuedScenes(final int sceneIndex) {
        return this.sceneQueuedClips[sceneIndex] > 0;
    }
    
    public void info() {
        SlMk3Extension.println(" Que %s Has %s", Arrays.toString(this.sceneQueuedClips),
            Arrays.toString(this.hasClips));
    }
    
}
