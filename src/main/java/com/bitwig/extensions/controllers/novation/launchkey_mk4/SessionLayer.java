package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.HashSet;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayValueTracker;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

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
    private final Layer selectLayer;
    
    private final DisplayControl displayControl;
    @Inject
    private ControllerHost host;
    
    private Layer currentModeLayer;
    private Mode mode = Mode.LAUNCH;
    private final HashSet<Integer> heldSoloKeys = new HashSet<>();
    private final LayerPool layerPool;
    private final BooleanValueObject sequencerControl;
    private final BooleanValueObject altActive = new BooleanValueObject();
    private final Transport transport;
    private int sceneColor = 0;
    private final SceneBank sceneBank;
    private final Scene targetScene;
    private final DisplayValueTracker sceneDisplayValue;
    
    private enum Mode {
        LAUNCH,
        STOP,
        SOLO,
        MUTE,
        CONTROL
    }
    
    public SessionLayer(final Layers layers, final LaunchkeyHwElements hwElements, final ViewControl viewControl,
        final LayerPool layerPool, final Transport transport, final DisplayControl displayControl) {
        super(layers, "SESSION_LAYER");
        this.layerPool = layerPool;
        this.displayControl = displayControl;
        final TrackBank trackBank = viewControl.getTrackBank();
        sceneBank = trackBank.sceneBank();
        targetScene = viewControl.getFocusScene();
        trackBank.setShouldShowClipLauncherFeedback(true);
        sequencerControl = layerPool.getSequencerControl();
        launchLayer2 = new Layer(layers, "LAUNCH_LAYER2");
        muteLayer = new Layer(layers, "MUTE_LAYER");
        soloLayer = new Layer(layers, "SOLO_LAYER");
        stopLayer = new Layer(layers, "STOP_LAYER");
        controlLayer = new Layer(layers, "CONTROL_LAYER");
        selectLayer = new Layer(layers, "SELECT_LAYER");
        currentModeLayer = launchLayer2;
        
        this.transport = transport;
        
        final RgbButton[] buttons = hwElements.getSessionButtons();
        targetScene.color().addValueObserver((r, g, b) -> this.sceneColor = ColorLookup.toColor(r, g, b));
        
        sceneDisplayValue = new DisplayValueTracker(displayControl, new BasicStringValue("Scene"), targetScene.name());
        
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
            
            button.bindIsPressed(triggerLayer, pressed -> handleSlot(pressed, track, slot));
            button.bindLight(triggerLayer, () -> getState(track, slot, trackIndex, sceneIndex));
            button.bindIsPressed(selectLayer, pressed -> handleSelect(pressed, track, slot));
            button.bindLight(selectLayer, () -> getSelectState(track, slot, trackIndex, sceneIndex));
            
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
        
        final Layer sceneLayer = layerPool.getSceneControlSession();
        final RgbButton row2ModeButton = hwElements.getButton(CcAssignments.LAUNCH_MODE);
        row2ModeButton.bindPressed(sceneLayer, this::advanceLayer);
        row2ModeButton.bindLight(sceneLayer, this::getModeColor);
        final RgbButton sceneLaunchButton = hwElements.getButton(CcAssignments.SCENE_LAUNCH);
        sceneLaunchButton.bindIsPressed(sceneLayer, pressed -> doSceneLaunch(pressed));
        sceneLaunchButton.bindLight(sceneLayer, () -> getSceneLight());
        
        final RgbButton navUpButton = hwElements.getButton(CcAssignments.NAV_UP);
        final RgbButton navDownButton = hwElements.getButton(CcAssignments.NAV_DOWN);
        navUpButton.bindRepeatHold(this, () -> this.navigateScene(-1), 500, 100);
        navUpButton.bindLightPressed(this, sceneBank.canScrollBackwards());
        navDownButton.bindRepeatHold(this, () -> navigateScene(1), 500, 100);
        navDownButton.bindLightPressed(this, sceneBank.canScrollForwards());
        currentModeLayer.activate();
        
        final RgbButton altButton = hwElements.getButton(CcAssignments.CAPTURE);
        altButton.bindLightPressed(this, altActive);
        altButton.bindIsPressed(this, this::handleAltMode);
    }
    
    private void navigateScene(final int dir) {
        if (dir > 0) {
            sceneBank.scrollForwards();
        } else {
            sceneBank.scrollBackwards();
        }
        sceneDisplayValue.notifyUpdate();
    }
    
    private void handleAltMode(final boolean altActive) {
        this.altActive.set(altActive);
        if (altActive) {
            if (sequencerControl.get()) {
                displayControl.show2Line("Modifier", "Only Select");
            } else {
                displayControl.show2Line("Modifier", "ALT Clip Launch");
            }
        } else {
            displayControl.revertToFixed();
        }
    }
    
    private RgbState getSceneLight() {
        if (sceneLaunched && hasPlayQueued()) {
            return RgbState.flash(sceneColor == 0 ? 23 : sceneColor, 0);
        }
        if (targetScene.clipCount().get() > 0) {
            return sceneColor == 0 ? RgbState.DIM_WHITE : RgbState.of(sceneColor);
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
                displayControl.show2Line("Function", "Stop");
                break;
            case STOP:
                mode = Mode.SOLO;
                currentModeLayer = soloLayer;
                displayControl.show2Line("Function", "Solo");
                break;
            case SOLO:
                mode = Mode.MUTE;
                currentModeLayer = muteLayer;
                displayControl.show2Line("Function", "Mute");
                break;
            case MUTE:
                mode = Mode.LAUNCH;
                currentModeLayer = launchLayer2;
                displayControl.show2Line("Function", "Clip Launching");
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
    
    private void doSceneLaunch(final boolean pressed) {
        if (pressed) {
            if (targetScene.clipCount().get() > 0) {
                sceneLaunched = true;
            }
            if (altActive.get()) {
                targetScene.launchAlt();
            } else {
                targetScene.launch();
            }
        } else {
            if (altActive.get()) {
                targetScene.launchReleaseAlt();
            } else {
                targetScene.launchRelease();
            }
        }
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
        slot.isSelected().markInterested();
        slot.color().addValueObserver((r, g, b) -> colorIndex[index] = ColorLookup.toColor(r, g, b));
    }
    
    
    private void handleSelect(final boolean pressed, final Track track, final ClipLauncherSlot slot) {
        if (!pressed || !track.canHoldNoteData().get()) {
            return;
        }
        track.selectInEditor();
        if (!slot.hasContent().get()) {
            slot.createEmptyClip(4);
        }
        host.scheduleTask(() -> {
            slot.select();
            if (!altActive.get()) {
                slot.launch();
            }
        }, 100);
        sequencerControl.set(false);
    }
    
    private void handleSlot(final boolean pressed, final Track track, final ClipLauncherSlot slot) {
        if (pressed) {
            if (altActive.get()) {
                slot.launchAlt();
            } else {
                slot.launch();
            }
        } else {
            if (altActive.get()) {
                slot.launchReleaseAlt();
            } else {
                slot.launchRelease();
            }
        }
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
    
    private RgbState getSelectState(final Track track, final ClipLauncherSlot slot, final int trackIndex,
        final int sceneIndex) {
        if (slot.hasContent().get()) {
            final int color = colorIndex[sceneIndex * 8 + trackIndex];
            if (slot.isSelected().get()) {
                return RgbState.WHITE;
            }
            return RgbState.pulse(color);
        }
        if (track.canHoldNoteData().get()) {
            return RgbState.RED.pulse();
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
        currentModeLayer.setIsActive(true);
        if (!sequencerControl.get()) {
            layerPool.getSceneControlSession().setIsActive(true);
            selectLayer.setIsActive(false);
        } else {
            selectLayer.setIsActive(true);
        }
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        currentModeLayer.setIsActive(false);
        layerPool.getSceneControlSession().setIsActive(false);
        selectLayer.setIsActive(false);
        altActive.set(false);
    }
    
}
