package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.List;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.akai.apc.common.control.ClickEncoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkRgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

@Component
public class ClipLauncher {
    
    private final Layer mainLayer;
    private final MpkColor[][] clipColors;
    private final Transport transport;
    private final List<MpkRgbButton> gridButtons;
    
    public ClipLauncher(final MpkHwElements hwElements, final LayerCollection layerCollection,
        final MpkViewControl viewControl, final Transport transport, final MpkMidiProcessor midiProcessor) {
        mainLayer = layerCollection.get(LayerCollection.LayerId.CLIP_LAUNCHER);
        gridButtons = hwElements.getGridButtons();
        final TrackBank trackBank = viewControl.getTrackBank();
        final SceneBank sceneBank = trackBank.sceneBank();
        this.transport = transport;
        clipColors = new MpkColor[trackBank.getSizeOfBank()][sceneBank.getSizeOfBank()];
        midiProcessor.addUpdateListeners(this::handleUpdateNeeded);
        trackBank.setShouldShowClipLauncherFeedback(true);
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            for (int j = 0; j < sceneBank.getSizeOfBank(); j++) {
                final int sceneIndex = j;
                clipColors[i][j] = MpkColor.OFF;
                final ClipLauncherSlot clipSlot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                final int rowIndex = sceneBank.getSizeOfBank() - sceneIndex - 1;
                final MpkRgbButton button = gridButtons.get(rowIndex * 4 + trackIndex);
                button.bindLight(mainLayer, () -> getState(track, clipSlot, trackIndex, sceneIndex));
                button.bindIsPressed(mainLayer, pressed -> handleClipPress(pressed, clipSlot));
                prepareClipSlot(clipSlot, trackIndex, sceneIndex);
            }
        }
        final ClickEncoder encoder = hwElements.getMainEncoder();
        encoder.bind(
            mainLayer, inc -> {
                MpkMk4ControllerExtension.println(" > " + inc);
            });
    }
    
    private void handleUpdateNeeded() {
        if (mainLayer.isActive()) {
            for (final MpkRgbButton button : gridButtons) {
                button.forceUpdate();
            }
        }
    }
    
    private void handleClipPress(final Boolean pressed, final ClipLauncherSlot clipSlot) {
        if (pressed) {
            clipSlot.launch();
        }
    }
    
    private MpkColor getState(final Track track, final ClipLauncherSlot slot, final int trackIndex,
        final int sceneIndex) {
        if (slot.hasContent().get()) {
            final MpkColor color = clipColors[trackIndex][sceneIndex];
            if (slot.isRecordingQueued().get()) {
                return MpkColor.RED.variant(MpkColor.BLINK1_4);
            } else if (slot.isRecording().get()) {
                return MpkColor.RED.variant(MpkColor.PULSE1_2);
            } else if (slot.isPlaybackQueued().get()) {
                return color.variant(MpkColor.BLINK1_4);
            } else if (slot.isStopQueued().get()) {
                return MpkColor.GREEN.variant(MpkColor.BLINK1_8);
            } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
                return MpkColor.GREEN.variant(MpkColor.BLINK1_8);
            } else if (slot.isPlaying().get()) {
                return MpkColor.GREEN.variant(MpkColor.PULSE1_4);
                //                if (clipLauncherOverdub.get() && track.arm().get()) {
                //                    return RgbLightState.RED.behavior(LedBehavior.PULSE_2);
                //                } else {
                //                    if (isPlaying()) {
                //                        return RgbLightState.GREEN_PLAY;
                //                    }
                //                    return RgbLightState.GREEN;
                //                }
            }
            return color;
        }
        if (slot.isRecordingQueued().get()) {
            return MpkColor.RED.variant(MpkColor.BLINK1_8);
        } else if (track.arm().get()) {
            return MpkColor.RED.variant(MpkColor.SOLID_25);
        }
        return MpkColor.OFF;
    }
    
    private void prepareClipSlot(final ClipLauncherSlot slot, final int trackIndex, final int sceneIndex) {
        slot.hasContent().markInterested();
        slot.isPlaying().markInterested();
        slot.isStopQueued().markInterested();
        slot.isRecordingQueued().markInterested();
        slot.isRecording().markInterested();
        slot.isPlaybackQueued().markInterested();
        slot.isSelected().markInterested();
        slot.color().addValueObserver((r, g, b) -> clipColors[trackIndex][sceneIndex] = MpkColor.getColor(r, g, b));
    }
    
    @Activate
    public void init() {
        MpkMk4ControllerExtension.println(" >> ACTIVATE Clip Luancher");
        mainLayer.setIsActive(true);
    }
    
}
