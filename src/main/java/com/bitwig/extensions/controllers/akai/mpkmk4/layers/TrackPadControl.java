package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.List;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
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
        final MpkViewControl viewControl, final Project project, final MpkMidiProcessor midiProcessor) {
        final Layer mainLayer = layerCollection.get(LayerId.TRACK_PAD_CONTROL);
        final List<MpkMultiStateButton> gridButtons = hwElements.getGridButtons();
        final TrackBank trackBank = viewControl.getTrackBank();
        this.project = project;
        trackBank.setShouldShowClipLauncherFeedback(true);
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            final MpkMultiStateButton armButton = gridButtons.get(trackIndex);
            armButton.bindLight(mainLayer, () -> getArmState(track));
            armButton.bindIsPressed(mainLayer, pressed -> handleArmPressed(pressed, track));
            final MpkMultiStateButton stopButton = gridButtons.get(trackIndex + 4);
            stopButton.bindLight(mainLayer, () -> getStopState(track));
            stopButton.bindPressed(mainLayer, () -> track.stop());
            final MpkMultiStateButton muteButton = gridButtons.get(8 + trackIndex);
            muteButton.bindLight(mainLayer, () -> getMuteState(track));
            muteButton.bindPressed(mainLayer, () -> track.mute().toggle());
            final MpkMultiStateButton soloButton = gridButtons.get(12 + trackIndex);
            soloButton.bindLight(mainLayer, () -> getSoloState(track));
            soloButton.bindIsPressed(mainLayer, pressed -> handleSoloPressed(pressed, track));
        }
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
