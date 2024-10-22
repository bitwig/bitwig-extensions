package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.HashSet;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

@Component
public class SessionLayer extends Layer {
    
    private final int[] colorIndex = new int[16];
    private final boolean[] sceneQueuePlayback = new boolean[8];
    private boolean sceneLaunched = false;
    private final Layer launchLayer2;
    private final Layer muteLayer;
    private final Layer soloLayer;
    private final Layer stopLayer;
    private final Layer controlLayer;
    private final Layer shiftLayer;
    
    private Layer currentModeLayer;
    private Mode mode = Mode.LAUNCH;
    private final HashSet<Integer> heldSoloKeys = new HashSet<>();
    
    private enum Mode {
        LAUNCH,
        STOP,
        SOLO,
        MUTE,
        CONTROL
    }
    
    public SessionLayer(final Layers layers, final LaunchkeyHwElements hwElements, final ViewControl viewControl) {
        super(layers, "SESSION_LAYER");
        
        final TrackBank trackBank = viewControl.getTrackBank();
        final SceneBank sceneBank = trackBank.sceneBank();
        final Scene targetScene = trackBank.sceneBank().getScene(0);
        targetScene.clipCount().markInterested();
        trackBank.setShouldShowClipLauncherFeedback(true);
        
        launchLayer2 = new Layer(layers, "LAUNCH_LAYER2");
        muteLayer = new Layer(layers, "MUTE_LAYER");
        soloLayer = new Layer(layers, "SOLO_LAYER");
        stopLayer = new Layer(layers, "STOP_LAYER");
        shiftLayer = new Layer(layers, "LAUNCH_SHIFT_LAYER");
        controlLayer = new Layer(layers, "CONTROL_LAYER");
        currentModeLayer = launchLayer2;
        
        final RgbButton[] buttons = hwElements.getSessionButtons();
        
        sceneBank.canScrollBackwards().markInterested();
        sceneBank.canScrollForwards().markInterested();
        
        final RgbButton row2ModeButton = hwElements.getButton(CcAssignments.LAUNCH_MODE);
        row2ModeButton.bindPressed(this, this::advanceLayer);
        row2ModeButton.bindLight(this, this::getModeColor);
        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i);
            markTrack(track);
        }
        
        for (int i = 0; i < 16; i++) {
            final RgbButton button = buttons[i];
            final int sceneIndex = i / 8;
            final int trackIndex = i % 8;
            final Track track = trackBank.getItemAt(trackIndex);
            final Layer triggerLayer = sceneIndex == 0 ? this : launchLayer2;
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
            prepareSlot(slot, i);
            if (sceneIndex == 0) {
                slot.isPlaybackQueued().addValueObserver(queued -> {
                    sceneQueuePlayback[trackIndex] = queued;
                    if (!hasPlayQueued()) {
                        sceneLaunched = false;
                    }
                });
            }
            
            button.bindIsPressed(triggerLayer, pressed -> {
                if (pressed) {
                    handleSlot(track, slot, trackIndex, sceneIndex);
                }
            });
            button.bindLight(triggerLayer, () -> getState(track, slot, trackIndex, sceneIndex));
            
            if (sceneIndex == 1) {
                button.bindPressed(stopLayer, track::stop);
                button.bindLight(stopLayer, () -> getStopState(trackIndex, track));
                button.bindPressed(muteLayer, () -> track.mute().toggle());
                button.bindLight(muteLayer, () -> getMuteState(trackIndex, track));
                button.bindIsPressed(soloLayer, pressed -> handleSoloAction(pressed, trackIndex, track));
                button.bindLight(soloLayer, () -> getSoloState(trackIndex, track));
                button.bindIsPressed(controlLayer, pressed -> {
                });
                button.bindLight(controlLayer, () -> RgbState.BLUE_LO);
            }
        }
        
        final RgbButton sceneLaunchButton = hwElements.getButton(CcAssignments.SCENE_LAUNCH);
        sceneLaunchButton.bindPressed(this, () -> doSceneLaunch(targetScene));
        sceneLaunchButton.bindLight(this, () -> getSceneLight(targetScene));
        
        final RgbButton navUpButton = hwElements.getButton(CcAssignments.NAV_UP);
        final RgbButton navDownButton = hwElements.getButton(CcAssignments.NAV_DOWN);
        navUpButton.bindRepeatHold(this, () -> trackBank.sceneBank().scrollBackwards(), 500, 100);
        navUpButton.bindLightPressed(this, trackBank.sceneBank().canScrollBackwards());
        navDownButton.bindRepeatHold(this, () -> trackBank.sceneBank().scrollForwards(), 500, 100);
        navDownButton.bindLightPressed(this, trackBank.sceneBank().canScrollForwards());
        currentModeLayer.activate();
    }
    
    private RgbState getSceneLight(final Scene targetScene) {
        if (sceneLaunched && hasPlayQueued()) {
            return RgbState.flash(23, 0);
        }
        if (targetScene.clipCount().get() > 0) {
            return RgbState.DIM_WHITE;
        }
        return RgbState.OFF;
    }
    
    private void handleSoloAction(final boolean pressed, final int trackIndex, final Track track) {
        if (pressed) {
            heldSoloKeys.add(trackIndex);
            track.solo().toggle(heldSoloKeys.size() < 2);
        } else {
            heldSoloKeys.remove(trackIndex);
        }
    }
    
    private void markTrack(final Track track) {
        track.isStopped().markInterested();
        track.mute().markInterested();
        track.solo().markInterested();
        track.isQueuedForStop().markInterested();
        track.arm().markInterested();
    }
    
    private void advanceLayer() {
        currentModeLayer.setIsActive(false);
        switch (mode) {
            case LAUNCH:
                mode = Mode.STOP;
                currentModeLayer = stopLayer;
                break;
            case STOP:
                mode = Mode.SOLO;
                currentModeLayer = soloLayer;
                break;
            case SOLO:
                mode = Mode.MUTE;
                currentModeLayer = muteLayer;
                break;
            case MUTE:
                mode = Mode.LAUNCH;
                currentModeLayer = launchLayer2;
                break;
        }
        heldSoloKeys.clear();
        currentModeLayer.setIsActive(true);
    }
    
    private RgbState getModeColor() {
        switch (mode) {
            case LAUNCH:
                return RgbState.WHITE;
            case STOP:
                return RgbState.RED;
            case MUTE:
                return RgbState.ORANGE;
            case SOLO:
                return RgbState.YELLOW;
            case CONTROL:
                return RgbState.BLUE;
        }
        return RgbState.WHITE;
    }
    
    private void doSceneLaunch(final Scene scene) {
        if (scene.clipCount().get() > 0) {
            sceneLaunched = true;
        }
        scene.launch();
    }
    
    private boolean hasPlayQueued() {
        for (int i = 0; i < sceneQueuePlayback.length; i++) {
            if (sceneQueuePlayback[i]) {
                return true;
            }
        }
        return false;
    }
    
    private void prepareSlot(final ClipLauncherSlot slot, final int index) {
        slot.hasContent().markInterested();
        slot.isPlaying().markInterested();
        slot.isStopQueued().markInterested();
        slot.isRecordingQueued().markInterested();
        slot.isRecording().markInterested();
        slot.isPlaybackQueued().markInterested();
        slot.color().addValueObserver((r, g, b) -> colorIndex[index] = ColorLookup.toColor(r, g, b));
    }
    
    private void handleSlot(final Track track, final ClipLauncherSlot slot, final int trackIndex,
        final int sceneIndex) {
        slot.launch();
    }
    
    private RgbState getStopState(final int index, final Track track) {
        if (track.exists().get()) {
            if (track.isQueuedForStop().get()) {
                return RgbState.flash(5, 0);
            }
            if (track.isStopped().get()) {
                return RgbState.RED_LO;
            }
            return RgbState.RED;
        }
        return RgbState.OFF;
    }
    
    private RgbState getMuteState(final int index, final Track track) {
        if (track.exists().get()) {
            if (track.mute().get()) {
                return RgbState.ORANGE;
            }
            return RgbState.ORANGE_LO;
        }
        return RgbState.OFF;
    }
    
    private RgbState getSoloState(final int index, final Track track) {
        if (track.exists().get()) {
            if (track.solo().get()) {
                return RgbState.YELLOW;
            }
            return RgbState.YELLOW_LO;
        }
        return RgbState.OFF;
    }
    
    private RgbState getState(final Track track, final ClipLauncherSlot slot, final int trackIndex,
        final int sceneIndex) {
        if (slot.hasContent().get()) {
            final int color = colorIndex[sceneIndex * 8 + trackIndex];
            if (slot.isRecordingQueued().get()) {
                return RgbState.flash(color, 5);
            } else if (slot.isRecording().get()) {
                return RgbState.pulse(5);
            } else if (slot.isPlaybackQueued().get()) {
                return RgbState.flash(color, 23);
            } else if (slot.isStopQueued().get()) {
                return RgbState.flash(color, 1);
            } else if (track.isQueuedForStop().get()) {
                return RgbState.flash(color, 0);
            } else if (slot.isPlaying().get()) {
                return RgbState.pulse(22);
            }
            return RgbState.of(color);
        }
        if (slot.isRecordingQueued().get()) {
            return RgbState.flash(5, 0); // Possibly Track Color
        } else if (slot.isPlaybackQueued().get()) {
            return RgbState.flash(23, 0);
        } else if (track.arm().get()) {
            return RgbState.RED;
        }
        return RgbState.OFF;
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
    }
    
}
