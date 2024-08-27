package com.bitwig.extensions.controllers.arturia.keylab.mk3;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbColor;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.SceneState;

public class SceneFocus {
    
    private final TrackBank viewBank;
    
    private int clipCount = 0;
    private final boolean[] clipsQueued;
    private final boolean[] stopQueued;
    private final boolean[] clipsPlaying;
    private final boolean[] trackStopQueued;
    private RgbColor dimmed = RgbColor.GRAY.getDimmed();
    private RgbColor color = RgbColor.GRAY;
    
    public SceneFocus(final ControllerHost host, final Scene focusScene, final int numTracks) {
        this.viewBank = host.createTrackBank(numTracks, 0, 1, false);
        focusScene.color().addValueObserver(this::updateColor);
        clipsQueued = new boolean[numTracks];
        stopQueued = new boolean[numTracks];
        clipsPlaying = new boolean[numTracks];
        trackStopQueued = new boolean[numTracks];
        focusScene.clipCount().addValueObserver(count -> clipCount = count);
        for (int i = 0; i < numTracks; i++) {
            final int index = i;
            final Track track = this.viewBank.getItemAt(i);
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(0);
            slot.isPlaybackQueued().addValueObserver(queued -> clipsQueued[index] = queued);
            slot.isPlaying().addValueObserver(queued -> clipsPlaying[index] = queued);
            slot.isStopQueued().addValueObserver(queued -> stopQueued[index] = queued);
            track.isQueuedForStop().addValueObserver(queued -> trackStopQueued[index] = queued);
        }
    }
    
    private void updateColor(final float r, final float g, final float b) {
        color = RgbColor.getColor(r, g, b);
        dimmed = color.getDimmed();
    }
    
    public void setPosition(final int pos) {
        this.viewBank.sceneBank().scrollPosition().set(pos);
    }
    
    private RgbColor getAdjustColor() {
        if (color.equals(RgbColor.DEFAULT_SCENE)) {
            return RgbColor.GREEN;
        }
        return color;
    }
    
    private RgbColor getAdjustDimColor() {
        if (color.equals(RgbColor.DEFAULT_SCENE)) {
            return RgbColor.GREEN_DIM;
        }
        return dimmed;
    }
    
    public SceneState getSceneState(final int blinkPhase, final boolean held) {
        boolean withQueued = false;
        boolean withPlay = false;
        boolean withTrackStopQueued = false;
        
        for (int i = 0; i < clipsPlaying.length; i++) {
            if (clipsQueued[i] || stopQueued[i]) {
                withQueued = true;
            }
            if (clipsPlaying[i]) {
                withPlay = true;
            }
            if (trackStopQueued[i]) {
                withTrackStopQueued = true;
            }
        }
        if (clipCount == 0) {
            if (withTrackStopQueued) {
                return new SceneState(blinkPhase % 4 < 2 ? dimmed : color, true, held);
            }
            return new SceneState(dimmed, true, held);
        }
        final RgbColor stateColor;
        if (withQueued) {
            stateColor = blinkPhase % 4 < 2 ? dimmed : getAdjustColor();
        } else if (withPlay) {
            stateColor = getAdjustColor();
        } else {
            stateColor = getAdjustDimColor();
        }
        return new SceneState(stateColor, false, held);
    }
}
