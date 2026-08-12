package com.bitwig.extensions.controllers.embodme;

import com.bitwig.extension.controller.api.Track;

class TrackState {
    private final Track track;
    private final int index;
    private boolean armed;
    private boolean muted;
    private boolean solo;
    private int color;
    
    public TrackState(final Track track, final int trackIndex) {
        this.track = track;
        this.index = trackIndex;
    }
    
    public Track getTrack() {
        return track;
    }
    
    public int getIndex() {
        return index;
    }
    
    public void setArmed(final boolean armed) {
        this.armed = armed;
    }
    
    public boolean isArmed() {
        return armed;
    }
    
    public boolean isMuted() {
        return muted;
    }
    
    public void setMuted(final boolean muted) {
        this.muted = muted;
    }
    
    public boolean isSolo() {
        return solo;
    }
    
    public void setSolo(final boolean solo) {
        this.solo = solo;
    }
    
    public int getColor() {
        return color;
    }
    
    public void setColor(final int color) {
        this.color = color;
    }
}
