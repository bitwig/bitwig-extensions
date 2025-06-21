package com.bitwig.extensions.controllers.reloop;

import java.util.List;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component
public class SessionLayer extends Layer {
    private final int[] colorIndex = new int[64];
    private final MidiProcessor midiProcessor;
    private final List<RgbButton> buttons;
    private final ControllerHost host;
    private boolean isPlaying = false;
    private boolean isClipLauncherOverdub = false;
    private final TrackBank trackBank;
    
    @Inject
    GlobalStates globalStates;
    
    public SessionLayer(final Layers layers, final ControllerHost host, final MidiProcessor midiProcessor,
        final BitwigControl viewControl, final HwElements hwElements) {
        super(layers, "SESSION_LAYER");
        this.midiProcessor = midiProcessor;
        this.buttons = hwElements.getNoteButtons();
        this.host = host;
        trackBank = viewControl.getTrackBank();
        for (int i = 0; i < 32; i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            prepareTrack(track);
            for (int j = 0; j < 2; j++) {
                final int sceneIndex = j;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                prepareSlot(slot, sceneIndex, trackIndex);
                final int buttonIndex = (trackIndex / 8) * 16 + trackIndex % 8 + sceneIndex * 8;
                final RgbButton button = buttons.get(buttonIndex);
                button.bindIsPressed(this, pressed -> handleSlotPressed(slot, pressed));
                button.bindLight(this, () -> getState(track, slot, trackIndex, sceneIndex));
            }
        }
        hwElements.get(Assignment.SCENE_UP).bindRepeatHold(this, () -> trackBank.sceneBank().scrollBackwards());
        hwElements.get(Assignment.SCENE_DOWN).bindRepeatHold(this, () -> trackBank.sceneBank().scrollForwards());
    }
    
    public void navigateScenes(final int dir) {
        if (dir < 0) {
            trackBank.sceneBank().scrollForwards();
        } else {
            trackBank.sceneBank().scrollBackwards();
        }
    }
    
    public void launchScene() {
        if (isActive()) {
            trackBank.sceneBank().getScene(0).launch();
        }
    }
    
    private void prepareTrack(final Track track) {
        track.isStopped().markInterested();
        track.mute().markInterested();
        track.solo().markInterested();
        track.isQueuedForStop().markInterested();
        track.arm().markInterested();
    }
    
    protected void prepareSlot(final ClipLauncherSlot slot, final int sceneIndex, final int trackIndex) {
        slot.hasContent().markInterested();
        slot.isPlaying().markInterested();
        slot.isStopQueued().markInterested();
        slot.isRecordingQueued().markInterested();
        slot.isRecording().markInterested();
        slot.isPlaybackQueued().markInterested();
        slot.color().addValueObserver((r, g, b) -> {
            colorIndex[sceneIndex * 32 + trackIndex] = ReloopRgb.toColorValue(r, g, b);
            //            println(" %f %f %f => %d <= %d %d %d", r, g, b, colorIndex[sceneIndex * 2 + trackIndex],
            //            toColorValue(r),
            //                    toColorValue(g), toBlueValue(b, g));
        });
    }
    
    private void handleSlotPressed(final ClipLauncherSlot slot, final boolean pressed) {
        if (globalStates.getShiftState().get()) {
            slot.deleteObject();
        } else {
            slot.launch();
            if (slot.hasContent().get()) {
                slot.showInEditor();
            }
        }
    }
    
    private ReloopRgb getState(final Track track, final ClipLauncherSlot slot, final int trackIndex,
        final int sceneIndex) {
        if (slot.hasContent().get()) {
            final int color = colorIndex[sceneIndex * 32 + trackIndex];
            if (slot.isRecordingQueued().get()) {
                return midiProcessor.blinkSlow(color, ReloopRgb.DIMMED_RED);
            } else if (slot.isRecording().get()) {
                return midiProcessor.blinkSlow(color, ReloopRgb.DIMMED_RED);
            } else if (slot.isPlaybackQueued().get()) {
                return midiProcessor.blinkMid(color, 0);
            } else if (slot.isStopQueued().get()) {
                return midiProcessor.blinkMid(color, 0);
            } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
                return ReloopRgb.of(color);
            } else if (slot.isPlaying().get()) {
                if (isClipLauncherOverdub && track.arm().get()) {
                    return midiProcessor.blinkSlow(color, ReloopRgb.BRIGHT_RED);
                } else {
                    return isPlaying ? midiProcessor.blinkBrightSlow(color) : ReloopRgb.of(color | 0x40);
                }
            }
            return ReloopRgb.of(color);
        }
        if (slot.isRecordingQueued().get()) {
            return midiProcessor.blinkMid(ReloopRgb.DIMMED_RED.getColorValue(), 0);
        } else if (track.arm().get()) {
            return ReloopRgb.DIMMED_RED;
        }
        
        return ReloopRgb.OFF;
    }
    
    public void setPlaying(final boolean playing) {
        isPlaying = playing;
    }
    
    public void setClipLauncherOverdub(final boolean clipLauncherOverdub) {
        isClipLauncherOverdub = clipLauncherOverdub;
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        refreshAll();
    }
    
    public void refreshAll() {
        host.scheduleTask(() -> buttons.forEach(button -> button.restoreLastColor()), 50);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
    }
}
