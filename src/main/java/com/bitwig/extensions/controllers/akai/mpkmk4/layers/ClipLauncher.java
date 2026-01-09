package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.List;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColor;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColorLookup;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkMonoState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;

@Component
public class ClipLauncher {
    
    private final Layer mainLayer;
    private final MpkColor[][] clipColors;
    private final Transport transport;
    private final List<MpkMultiStateButton> gridButtons;
    
    public ClipLauncher(final MpkHwElements hwElements, final LayerCollection layerCollection,
        final MpkViewControl viewControl, final Transport transport, final MpkMidiProcessor midiProcessor) {
        mainLayer = layerCollection.get(LayerId.CLIP_LAUNCHER);
        gridButtons = hwElements.getGridButtons();
        final TrackBank trackBank = viewControl.getTrackBank();
        final SceneBank sceneBank = trackBank.sceneBank();
        this.transport = transport;
        clipColors = new MpkColor[trackBank.getSizeOfBank()][sceneBank.getSizeOfBank()];
        trackBank.setShouldShowClipLauncherFeedback(true);
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            for (int j = 0; j < sceneBank.getSizeOfBank(); j++) {
                final int sceneIndex = j;
                clipColors[i][j] = MpkColor.OFF;
                final ClipLauncherSlot clipSlot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                final int rowIndex = sceneBank.getSizeOfBank() - sceneIndex - 1;
                final MpkMultiStateButton button = gridButtons.get(rowIndex * 4 + trackIndex);
                button.bindLight(mainLayer, () -> getState(track, clipSlot, trackIndex, sceneIndex));
                button.bindIsPressed(mainLayer, pressed -> handleClipPress(pressed, clipSlot));
                prepareClipSlot(clipSlot, trackIndex, sceneIndex);
            }
        }
        
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final LineDisplay mainDisplay = hwElements.getMainLineDisplay();
        cursorTrack.name().addValueObserver(name -> {
            mainDisplay.setText(0, 0, name);
        });
        cursorTrack.color().addValueObserver((r, g, b) -> {
            final int index = MpkColorLookup.rgbToIndex(r, g, b);
            mainDisplay.setColorIndex(0, 0, index);
        });
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
                return MpkColor.RED.variant(MpkMonoState.BLINK1_4);
            } else if (slot.isRecording().get()) {
                return MpkColor.RED.variant(MpkMonoState.PULSE1_2);
            } else if (slot.isPlaybackQueued().get()) {
                return color.variant(MpkMonoState.BLINK1_4);
            } else if (slot.isStopQueued().get()) {
                return MpkColor.GREEN.variant(MpkMonoState.BLINK1_8);
            } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
                return MpkColor.GREEN.variant(MpkMonoState.BLINK1_8);
            } else if (slot.isPlaying().get()) {
                return MpkColor.GREEN.variant(MpkMonoState.PULSE1_4);
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
            return MpkColor.RED.variant(MpkMonoState.BLINK1_8);
        } else if (track.arm().get()) {
            return MpkColor.RED.variant(MpkMonoState.SOLID_25);
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
    
    
}
