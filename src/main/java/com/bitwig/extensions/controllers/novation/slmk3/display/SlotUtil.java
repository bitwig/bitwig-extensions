package com.bitwig.extensions.controllers.novation.slmk3.display;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.Track;

public class SlotUtil {
    
    public static void prepareSlot(final ClipLauncherSlot cs) {
        cs.exists().markInterested();
        cs.hasContent().markInterested();
        cs.isPlaybackQueued().markInterested();
        cs.isPlaying().markInterested();
        cs.isRecording().markInterested();
        cs.isRecordingQueued().markInterested();
        cs.isSelected().markInterested();
        cs.isStopQueued().markInterested();
        cs.isSelected().markInterested();
    }
    
    public static SlRgbState determineClipColor(final ClipLauncherSlot slot, final Track track,
        final SlRgbState baseColor, final boolean overdubActive) {
        if (slot.hasContent().get()) {
            if (slot.isRecordingQueued().get()) {
                return SlRgbState.RED_BLINK;
            }
            if (slot.isRecording().get()) {
                return baseColor.getBlink(SlRgbState.RED);
            }
            if (slot.isPlaybackQueued().get()) {
                return baseColor.getBlink();
            }
            if (slot.isStopQueued().get()) {
                return baseColor.getBlink(SlRgbState.WHITE_DIM);
            }
            if (track.isQueuedForStop().get()) {
                return baseColor.getBlink(SlRgbState.WHITE);
            }
            if (slot.isPlaying().get()) {
                if (track.arm().get() && overdubActive) {
                    return SlRgbState.RED_PULSE;
                }
                return SlRgbState.GREEN_PULSE;
            }
            return baseColor;
        } else {
            if (slot.isRecordingQueued().get()) {
                return SlRgbState.RED_BLINK;
            }
            if (slot.isPlaybackQueued().get()) {
                return SlRgbState.GREEN_BLINK;
            }
            if (track.arm().get()) {
                return SlRgbState.RED_DIM;
            }
        }
        return SlRgbState.OFF;
    }
    
}
