package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.List;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.akai.mpkmk4.GlobalStates;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkVariant;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColor;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkMonoState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;

@Component
public class TrackPadControl {
    
    private int armHeld;
    private int soloHeld;
    private final Project project;
    
    public TrackPadControl(final MpkHwElements hwElements, final LayerCollection layerCollection,
        final MpkViewControl viewControl, final Project project, final GlobalStates globalStates) {
        
        
        final List<MpkMultiStateButton> gridButtons = hwElements.getGridButtons();
        final TrackBank trackBank = viewControl.getTrackBank();
        this.project = project;
        trackBank.setShouldShowClipLauncherFeedback(true);
        if (globalStates.getVariant().getLayoutStyle() == MpkVariant.LayoutStyle.STANDARD) {
            bindCombinedLayer(layerCollection, trackBank, gridButtons);
        } else {
            bindSeparateLayer(layerCollection, trackBank, gridButtons);
        }
    }
    
    private void bindCombinedLayer(final LayerCollection layerCollection, final TrackBank trackBank,
        final List<MpkMultiStateButton> gridButtons) {
        final Layer combinedLayer = layerCollection.get(LayerId.TRACK_PAD_CONTROL);
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            bindMute(gridButtons, 8 + trackIndex, combinedLayer, track);
            bindSoloButton(gridButtons, trackIndex + 12, combinedLayer, track);
            
            bindArmButton(gridButtons, trackIndex, combinedLayer, track);
            bindStopButton(gridButtons, trackIndex + 4, combinedLayer, track);
        }
    }
    
    private void bindSeparateLayer(final LayerCollection layerCollection, final TrackBank trackBank,
        final List<MpkMultiStateButton> gridButtons) {
        final Layer muteSoloLayer = layerCollection.get(LayerId.MUTE_SOLO_CONTROL);
        final Layer mixerLayer = layerCollection.get(LayerId.MIXER_PAD_CONTROL);
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            bindMute(gridButtons, trackIndex, muteSoloLayer, track);
            bindMute(gridButtons, trackIndex + 8, muteSoloLayer, track);
            bindSoloButton(gridButtons, trackIndex + 4, muteSoloLayer, track);
            bindSoloButton(gridButtons, trackIndex + 12, muteSoloLayer, track);
            
            bindArmButton(gridButtons, trackIndex, mixerLayer, track);
            bindArmButton(gridButtons, trackIndex + 8, mixerLayer, track);
            bindStopButton(gridButtons, trackIndex + 4, mixerLayer, track);
            bindStopButton(gridButtons, trackIndex + 12, mixerLayer, track);
        }
    }
    
    private void bindMute(final List<MpkMultiStateButton> gridButtons, final int index, final Layer layer,
        final Track track) {
        final MpkMultiStateButton muteButton = gridButtons.get(index);
        muteButton.bindLight(layer, () -> getMuteState(track));
        muteButton.bindPressed(layer, () -> track.mute().toggle());
    }
    
    
    private void bindSoloButton(final List<MpkMultiStateButton> gridButtons, final int index, final Layer combinedLayer,
        final Track track) {
        final MpkMultiStateButton soloButton = gridButtons.get(index);
        soloButton.bindLight(combinedLayer, () -> getSoloState(track));
        soloButton.bindIsPressed(combinedLayer, pressed -> handleSoloPressed(pressed, track));
    }
    
    private void bindStopButton(final List<MpkMultiStateButton> gridButtons, final int index, final Layer layer,
        final Track track) {
        final MpkMultiStateButton stopButton = gridButtons.get(index);
        stopButton.bindLight(layer, () -> getStopState(track));
        stopButton.bindPressed(layer, () -> track.stop());
    }
    
    private void bindArmButton(final List<MpkMultiStateButton> gridButtons, final int buttonIndex, final Layer layer,
        final Track track) {
        final MpkMultiStateButton armButton = gridButtons.get(buttonIndex);
        armButton.bindLight(layer, () -> getArmState(track));
        armButton.bindIsPressed(layer, pressed -> handleArmPressed(pressed, track));
    }
    
    private InternalHardwareLightState getMuteState(final Track track) {
        return track.mute().get() ? MpkColor.ORANGE : MpkColor.ORANGE.variant(MpkMonoState.SOLID_10);
    }
    
    private InternalHardwareLightState getStopState(final Track track) {
        if (track.exists().get()) {
            if (track.isQueuedForStop().get()) {
                return MpkColor.GREEN.variant(MpkMonoState.BLINK1_4);
            } else if (track.isStopped().get()) {
                return MpkColor.GREEN.variant(MpkMonoState.SOLID_10);
            }
            return MpkColor.GREEN;
        }
        return MpkColor.OFF;
    }
    
    private InternalHardwareLightState getSoloState(final Track track) {
        return track.solo().get() ? MpkColor.YELLOW : MpkColor.YELLOW.variant(MpkMonoState.SOLID_10);
    }
    
    private void handleArmPressed(final boolean pressed, final Track track) {
        if (pressed) {
            armHeld++;
            if (armHeld == 1) {
                if (track.arm().get()) {
                    track.arm().set(false);
                } else {
                    project.unarmAll();
                    track.arm().set(true);
                }
            }
        } else {
            armHeld = Math.max(0, armHeld - 1);
        }
        
    }
    
    private void handleSoloPressed(final boolean pressed, final Track track) {
        if (pressed) {
            soloHeld++;
            if (soloHeld == 1) {
                track.solo().toggle(true);
            } else if (soloHeld > 1) {
                track.solo().toggle(false);
            }
        } else {
            soloHeld = Math.max(0, soloHeld - 1);
        }
        
    }
    
    private InternalHardwareLightState getArmState(final Track track) {
        return track.arm().get() ? MpkColor.RED : MpkColor.RED.variant(MpkMonoState.SOLID_10);
    }
    
}
