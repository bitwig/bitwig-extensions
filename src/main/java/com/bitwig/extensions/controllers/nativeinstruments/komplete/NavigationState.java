package com.bitwig.extensions.controllers.nativeinstruments.komplete;

public class NavigationState {
    private boolean canScrollSceneDown = false;
    private boolean canScrollSceneUp = false;
    private boolean canGoTrackRight = false;
    protected boolean sceneNavMode = false;
    
    
    public void setCanScrollSceneDown(final boolean canScrollSceneDown) {
        this.canScrollSceneDown = canScrollSceneDown;
    }
    
    public void setCanScrollSceneUp(final boolean canScrollSceneUp) {
        this.canScrollSceneUp = canScrollSceneUp;
    }
    
    
    public void setCanGoTrackRight(final boolean canGoTrackRight) {
        this.canGoTrackRight = canGoTrackRight;
    }
    
    public void setSceneNavMode(final boolean sceneNavMode) {
        this.sceneNavMode = sceneNavMode;
    }
    
    public boolean isSceneNavMode() {
        return sceneNavMode;
    }
    
    public boolean canGoTrackRight() {
        return canGoTrackRight;
    }
    
    public boolean canScrollSceneDown() {
        return canScrollSceneDown;
    }
    
    public boolean canScrollSceneUp() {
        return canScrollSceneUp;
    }
    
}
