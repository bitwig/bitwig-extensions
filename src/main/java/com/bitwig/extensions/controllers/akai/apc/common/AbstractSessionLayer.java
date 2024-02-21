package com.bitwig.extensions.controllers.akai.apc.common;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.akai.apc.common.led.LedBehavior;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public abstract class AbstractSessionLayer extends Layer {
    protected final int[][] colorIndex = new int[8][8];
    protected SettableBooleanValue clipLauncherOverdub;

    public AbstractSessionLayer(final Layers layers) {
        super(layers, "SESSION_LAYER");
    }

    protected abstract boolean isPlaying();

    protected abstract boolean isShiftHeld();

    protected RgbLightState getState(final Track track, final ClipLauncherSlot slot, final int trackIndex,
                                     final int sceneIndex) {
        if (slot.hasContent().get()) {
            final int color = colorIndex[sceneIndex][trackIndex];
            if (slot.isSelected().get() && isShiftHeld()) {
                return RgbLightState.WHITE_BRIGHT;
            }
            if (slot.isRecordingQueued().get()) {
                return RgbLightState.RED.behavior(LedBehavior.BLINK_4);
            } else if (slot.isRecording().get()) {
                return RgbLightState.RED.behavior(LedBehavior.PULSE_2);
            } else if (slot.isPlaybackQueued().get()) {
                return RgbLightState.of(color, LedBehavior.BLINK_4);
            } else if (slot.isStopQueued().get()) {
                return RgbLightState.GREEN_PLAY.behavior(LedBehavior.BLINK_8);
            } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
                return RgbLightState.GREEN.behavior(LedBehavior.BLINK_8);
            } else if (slot.isPlaying().get()) {
                if (clipLauncherOverdub.get() && track.arm().get()) {
                    return RgbLightState.RED.behavior(LedBehavior.PULSE_2);
                } else {
                    if (isPlaying()) {
                        return RgbLightState.GREEN_PLAY;
                    }
                    return RgbLightState.GREEN;
                }
            }
            return RgbLightState.of(color);
        }
        if (slot.isSelected().get() && isShiftHeld()) {
            return RgbLightState.WHITE_DIM;
        }
        if (slot.isRecordingQueued().get()) {
            return RgbLightState.RED.behavior(LedBehavior.BLINK_8); // Possibly Track Color
        } else if (track.arm().get()) {
            return RgbLightState.RED.behavior(LedBehavior.LIGHT_25);
        }
        return RgbLightState.OFF;
    } // V ultra_X_39--

    protected void markTrackBank(TrackBank bank) {
        bank.canScrollBackwards().markInterested();
        bank.canScrollForwards().markInterested();
        bank.sceneBank().canScrollBackwards().markInterested();
        bank.sceneBank().canScrollForwards().markInterested();
    }

    protected void markTrack(final Track track) {
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
        slot.isSelected().markInterested();
        slot.color().addValueObserver((r, g, b) -> colorIndex[sceneIndex][trackIndex] = ColorLookup.toColor(r, g, b));
    }

}
