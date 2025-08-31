package com.bitwig.extensions.framework.view;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class OverviewGrid {
    
    private int sceneOffset;
    private int trackOffset;
    private int numberOfScenes;
    private int numberOfTracks;
    
    private int trackPosition;
    private int scenePosition;
    
    private final int[] sceneQueuedClips;
    private final int[] playingClips;
    private final int[] clipCount;
    private final int size;
    
    private final TrackBank maxTrackBank;
    
    public OverviewGrid(final TrackBank maxTrackBank) {
        this.size = maxTrackBank.getSizeOfBank();
        this.maxTrackBank = maxTrackBank;
        sceneQueuedClips = new int[size];
        playingClips = new int[size];
        clipCount = new int[size];
        prepare(maxTrackBank);
    }
    
    public void setUpFocusScene(final TrackBank mainBank) {
        prepare(mainBank);
        mainBank.sceneBank().itemCount().addValueObserver(this::setNumberOfScenes);
        mainBank.channelCount().addValueObserver(this::setNumberOfTracks);
        mainBank.scrollPosition().addValueObserver(pos -> {
            setTrackPosition(pos);
            if (maxTrackBank.scrollPosition().get() != getTrackOffset()) {
                maxTrackBank.scrollPosition().set(getTrackOffset());
            }
        });
        mainBank.sceneBank().scrollPosition().addValueObserver(pos -> {
            setScenePosition(pos);
            if (maxTrackBank.sceneBank().scrollPosition().get() != getSceneOffset()) {
                maxTrackBank.sceneBank().scrollPosition().set(getSceneOffset());
            }
        });
        for (int i = 0; i < size; i++) {
            final int trackIndex = i;
            final Track track = maxTrackBank.getItemAt(trackIndex);
            
            final Scene scene = maxTrackBank.sceneBank().getScene(i);
            scene.clipCount().addValueObserver(count -> this.clipCount[trackIndex] = count);
            for (int j = 0; j < size; j++) {
                final int sceneIndex = j;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                slot.isPlaybackQueued().addValueObserver(isQueued -> markSceneQueued(sceneIndex, isQueued));
                slot.isPlaying().addValueObserver(isQueued -> markScenePlaying(sceneIndex, isQueued));
            }
        }
    }
    
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
        this.trackOffset = (trackPosition / size) * size;
    }
    
    public int getScenePosition() {
        return scenePosition - sceneOffset;
    }
    
    public void setScenePosition(final int scenePosition) {
        this.scenePosition = scenePosition;
        this.sceneOffset = (scenePosition / size) * size;
    }
    
    public int getSceneOffset() {
        return sceneOffset;
    }
    
    private void markSceneQueued(final int sceneIndex, final boolean isQueued) {
        if (isQueued) {
            sceneQueuedClips[sceneIndex]++;
        } else if (sceneQueuedClips[sceneIndex] > 0) {
            sceneQueuedClips[sceneIndex]--;
        }
    }
    
    private void markScenePlaying(final int sceneIndex, final boolean isPlaying) {
        if (isPlaying) {
            playingClips[sceneIndex]++;
        } else if (playingClips[sceneIndex] > 0) {
            playingClips[sceneIndex]--;
        }
    }
    
    public boolean hasClips(final int sceneIndex) {
        return this.clipCount[sceneIndex] > 0;
    }
    
    
    public boolean hasQueuedScenes(final int sceneIndex) {
        final int index = sceneIndex - sceneOffset;
        if (index >= size) {
            return false;
        }
        return this.sceneQueuedClips[index] > 0;
    }
    
    public boolean hasPlayingClips(final int sceneIndex) {
        final int index = sceneIndex - sceneOffset;
        if (index >= size) {
            return false;
        }
        return this.playingClips[index] > 0;
    }
    
    
    public boolean inGrid(final int trackIndex, final int sceneIndex) {
        final int posX = trackIndex * 8;
        final int posY = sceneIndex * 8;
        return posX < (numberOfTracks - trackOffset) && posY < (numberOfScenes - sceneOffset);
    }
    
    
    private static void prepare(final TrackBank bank) {
        bank.scrollPosition().markInterested();
        bank.sceneBank().scrollPosition().markInterested();
        for (int i = 0; i < bank.sceneBank().getSizeOfBank(); i++) {
            final Scene scene = bank.sceneBank().getScene(i);
            scene.exists().markInterested();
        }
    }
}
