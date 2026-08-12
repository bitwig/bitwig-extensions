package com.bitwig.extensions.controllers.embodme;

class SceneState {
    private final int index;
    private int count;
    private int state = 0;
    private int color = EraeColor.GREEN;
    private int displayColor = 0;
    private boolean exists;
    private boolean queued;
    
    public SceneState(final int index) {
        this.index = index;
    }
    
    public void setCount(final int count) {
        this.count = count;
    }
    
    public void setColor(final int color) {
        if (color == 0x7b) {
            this.color = EraeColor.GREEN;
        } else {
            this.color = color;
        }
    }
    
    public int getIndex() {
        return index;
    }
    
    public int getCount() {
        return count;
    }
    
    public int getState() {
        return state;
    }
    
    public int getDisplayColor() {
        return displayColor;
    }
    
    public boolean calcState() {
        final int oldState = this.state;
        final int oldColor = this.displayColor;
        
        if (exists) {
            if (queued) {
                displayColor = color;
            } else if (count == 0) {
                displayColor = 0;
            } else {
                displayColor = color + 2;
            }
            state = queued ? 1 : 0;
        }
        
        return oldState != state || oldColor != displayColor;
    }
    
    public void setExists(final boolean exists) {
        this.exists = exists;
    }
    
    public void setQueued(final boolean queued) {
        this.queued = queued;
    }
}
