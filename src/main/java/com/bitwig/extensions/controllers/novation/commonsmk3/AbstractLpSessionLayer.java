package com.bitwig.extensions.controllers.novation.commonsmk3;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class AbstractLpSessionLayer extends Layer {
    protected final int[][] colorIndex = new int[8][8];
    protected SettableBooleanValue clipLauncherOverdub;

    public AbstractLpSessionLayer(final Layers layers) {
        super(layers, "SESSION_LAYER");
    }

    protected RgbState getState(final Track track, final ClipLauncherSlot slot, final int trackIndex,
                                final int sceneIndex) {
        if (slot.hasContent().get()) {
            final int color = colorIndex[sceneIndex][trackIndex];
            if (slot.isRecordingQueued().get()) {
                return RgbState.flash(color, 5);
            } else if (slot.isRecording().get()) {
                return RgbState.pulse(5);
            } else if (slot.isPlaybackQueued().get()) {
                return RgbState.flash(color, 0);
            } else if (slot.isStopQueued().get()) {
                return RgbState.flash(22, 23); //RgbState.flash(color, 1);
            } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
                return RgbState.flash(22, 23);
            } else if (slot.isPlaying().get()) {
                if (clipLauncherOverdub.get() && track.arm().get()) {
                    return RgbState.pulse(5);
                } else {
                    return RgbState.pulse(22);
                }
            }
            return RgbState.of(color);
        }
        if (slot.isRecordingQueued().get()) {
            return RgbState.flash(5, 0); // Possibly Track Color
        } else if (track.arm().get()) {
            return RgbState.RED_LO;
        }
        return RgbState.OFF;
    }

}
