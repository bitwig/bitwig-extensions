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
    
    protected void doNavigateDown(final LayoutType currentLayoutType) {
        switch (currentLayoutType) {
            case LAUNCHER -> {
                sceneBank.scrollForwards();
                theClip.select();
            }
            case ARRANGER -> {
                if (navigationState.isSceneNavMode()) {
                    navigationState.setSceneNavMode(false);
                    sceneBank.setIndication(false);
                    singleTrackBank.setShouldShowClipLauncherFeedback(true);
                } else {
                    singleTrackBank.scrollBy(1);
                    theClip.select();
                    theTrack.selectInMixer();
                    theTrack.selectInEditor();
                }
            }
            default -> {
            }
        }
    }
    
    protected void doNavigateUp(final LayoutType currentLayoutType) {
        switch (currentLayoutType) {
            case LAUNCHER -> {
                sceneBank.scrollBackwards();
                theClip.select();
            }
            case ARRANGER -> {
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
            default -> {
            }
        }
    }
    
    protected void doNavigateLeft(final LayoutType currentLayoutType) {
        switch (currentLayoutType) {
            case LAUNCHER -> {
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
            case ARRANGER -> {
                sceneBank.scrollBackwards();
                theClip.select();
            }
            default -> {
            }
        }
    }
    
    protected void doNavigateRight(final LayoutType currentLayoutType) {
        switch (currentLayoutType) {
            case LAUNCHER -> {
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
            case ARRANGER -> {
                sceneBank.scrollForwards();
                theClip.select();
            }
            default -> {
            }
        }
    }
    
    public void launch() {
        if (navigationState.isSceneNavMode()) {
            sceneBank.getScene(0).launch();
        } else {
            theClip.launch();
        }
    }
}
