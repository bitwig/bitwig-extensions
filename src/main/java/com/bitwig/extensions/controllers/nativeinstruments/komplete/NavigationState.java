package com.bitwig.extensions.controllers.nativeinstruments.komplete;

public class NavigationState {
    private boolean canScrollSceneDown = false;
    private boolean canScrollSceneUp = false;
    private boolean canGoTrackLeft = false;
    private boolean canGoTrackRight = false;
    protected boolean sceneNavMode = false;
    private Runnable stateChangeHandler;

    public void setStateChangeListener(Runnable stateChange) {
        this.stateChangeHandler = stateChange;
        fireStateChange();
    }

    public void setCanScrollSceneDown(boolean canScrollSceneDown) {
        this.canScrollSceneDown = canScrollSceneDown;
        fireStateChange();
    }

    public void setCanScrollSceneUp(boolean canScrollSceneUp) {
        this.canScrollSceneUp = canScrollSceneUp;
        fireStateChange();
    }

    public void setCanGoTrackLeft(boolean canGoTrackLeft) {
        this.canGoTrackLeft = canGoTrackLeft;
        fireStateChange();
    }

    public void setCanGoTrackRight(boolean canGoTrackRight) {
        this.canGoTrackRight = canGoTrackRight;
        fireStateChange();
    }

    public void setSceneNavMode(boolean sceneNavMode) {
        this.sceneNavMode = sceneNavMode;
        fireStateChange();
    }

    public boolean isSceneNavMode() {
        return sceneNavMode;
    }

    public int getSceneValue() {
        return (canScrollSceneDown ? 0x2 : 0x0) | (canScrollSceneUp ? 0x1 : 0x0);
    }

    public int getTrackValue() {
        return (!sceneNavMode ? 0x1 : 0x0) | (canGoTrackRight ? 0x2 : 0x0);
    }

    private void fireStateChange() {
        if (stateChangeHandler == null) {
            return;
        }
        stateChangeHandler.run();
    }
}
