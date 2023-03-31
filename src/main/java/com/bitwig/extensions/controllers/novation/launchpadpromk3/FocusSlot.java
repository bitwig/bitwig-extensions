package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.Track;

public class FocusSlot {
    private final Track track;
    private final ClipLauncherSlot slot;
    private final int slotIndex;
    private final BooleanValue equalsCursorTrack;

    public FocusSlot(final Track track, final ClipLauncherSlot slot, final int slotIndex,
                     BooleanValue equalsCursorTrack) {
        this.track = track;
        this.slot = slot;
        this.slotIndex = slotIndex;
        this.equalsCursorTrack = equalsCursorTrack;
    }

    public Track getTrack() {
        return track;
    }

    public ClipLauncherSlot getSlot() {
        return slot;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public boolean isEmpty() {
        return !slot.hasContent().get();
    }

    public boolean isCursorTrack() {
        return equalsCursorTrack.get();
    }

}
