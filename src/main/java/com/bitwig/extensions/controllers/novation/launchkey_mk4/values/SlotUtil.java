package com.bitwig.extensions.controllers.novation.launchkey_mk4.values;

import com.bitwig.extension.controller.api.ClipLauncherSlot;

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
    }
    
}
