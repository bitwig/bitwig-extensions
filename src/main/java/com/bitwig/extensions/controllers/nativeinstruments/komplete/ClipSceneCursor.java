package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class ClipSceneCursor {
    private final CursorTrack cursorTrack;
    private final ClipLauncherSlot theClip;
    private final Track theTrack;
    private final NavigationState navigationState;
    protected TrackBank singleTrackBank;
    protected SceneBank sceneBank;
    
    public ClipSceneCursor(final ControllerHost host, final NavigationState navigationState) {
        singleTrackBank = host.createTrackBank(1, 0, 1);
        singleTrackBank.scrollPosition().markInterested();
        cursorTrack = host.createCursorTrack(1, 1);
        singleTrackBank.followCursorTrack(cursorTrack);
        theTrack = singleTrackBank.getItemAt(0);
        this.navigationState = navigationState;
        final ClipLauncherSlotBank slotBank = theTrack.clipLauncherSlotBank();
        singleTrackBank.setShouldShowClipLauncherFeedback(true);
        theClip = slotBank.getItemAt(0);
        
        sceneBank = singleTrackBank.sceneBank();
        sceneBank.cursorIndex().markInterested();
        sceneBank.setIndication(false);
        singleTrackBank.canScrollChannelsUp().addValueObserver(v -> navigationState.setCanGoTrackLeft(v));
        singleTrackBank.canScrollChannelsDown().addValueObserver(v -> navigationState.setCanGoTrackRight(v));
        sceneBank.canScrollBackwards().addValueObserver(v -> navigationState.setCanScrollSceneUp(v));
        sceneBank.canScrollForwards().addValueObserver(v -> navigationState.setCanScrollSceneDown(v));
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    protected void navigateDown(final LayoutType currentLayoutType) {
        if (currentLayoutType == LayoutType.LAUNCHER) {
            navigateSceneDown();
        } else {
            navigateTrackRight();
        }
    }
    
    protected void navigateUp(final LayoutType currentLayoutType) {
        if (currentLayoutType == LayoutType.LAUNCHER) {
            navigateSceneUp();
        } else {
            navigateTrackLeft();
        }
    }
    
    protected void navigateLeft(final LayoutType currentLayoutType) {
        if (currentLayoutType == LayoutType.LAUNCHER) {
            navigateTrackLeft();
        } else {
            navigateSceneUp();
        }
    }
    
    protected void navigateRight(final LayoutType currentLayoutType) {
        if (currentLayoutType == LayoutType.LAUNCHER) {
            navigateTrackRight();
        } else {
            navigateSceneDown();
        }
    }
    
    protected void navigateSceneUp() {
        sceneBank.scrollBackwards();
        theClip.select();
    }
    
    protected void navigateSceneDown() {
        sceneBank.scrollForwards();
        theClip.select();
    }
    
    protected void navigateTrackLeft() {
        if (singleTrackBank.scrollPosition().get() == 0) {
            navigationState.setSceneNavMode(true);
            sceneBank.setIndication(true);
            singleTrackBank.setShouldShowClipLauncherFeedback(false);
        } else {
            singleTrackBank.scrollBy(-1);
            theClip.select();
            theTrack.selectInMixer();
            theTrack.selectInEditor();
        }
    }
    
    protected void navigateTrackRight() {
        if (navigationState.isSceneNavMode()) {
            navigationState.setSceneNavMode(false);
            sceneBank.setIndication(false);
            singleTrackBank.setShouldShowClipLauncherFeedback(true);
        } else {
            singleTrackBank.scrollBy(1);
        }
        theClip.select();
        theTrack.selectInMixer();
        theTrack.selectInEditor();
    }
    
    public void launch() {
        if (navigationState.isSceneNavMode()) {
            sceneBank.getScene(0).launch();
        } else {
            theClip.launch();
        }
    }
}
