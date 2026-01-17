package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.Optional;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.di.Component;

@Component
public class MpkFocusClip {
    private final Clip mainCursoClip;
    private final int slotRange;
    private final CursorTrack cursorTrack;
    private final Transport transport;
    private final ClipLauncherSlotBank slotBank;
    private int selectedSlotIndex;
    
    public MpkFocusClip(final ControllerHost host, final Transport transport, final MpkViewControl viewControl) {
        this.cursorTrack = viewControl.getCursorTrack();
        this.transport = transport;
        mainCursoClip = host.createLauncherCursorClip(16, 1);
        slotBank = cursorTrack.clipLauncherSlotBank();
        slotRange = slotBank.getSizeOfBank();
        for (int i = 0; i < slotBank.getSizeOfBank(); i++) {
            final int index = i;
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            slot.isRecording().markInterested();
            slot.isPlaying().markInterested();
            slot.hasContent().markInterested();
            slot.exists().markInterested();
            slot.isSelected().addValueObserver(selected -> this.handleSelection(selected, index));
        }
    }
    
    private void handleSelection(final boolean selected, final int index) {
        if (selected) {
            this.selectedSlotIndex = index;
        }
    }
    
    public void setSelectedSlotIndex(final int pos) {
        this.selectedSlotIndex = pos;
        if (selectedSlotIndex < slotBank.getSizeOfBank()) {
            slotBank.getItemAt(selectedSlotIndex).select();
        }
    }
    
    public void invokeRecord() {
        if (selectedSlotIndex != -1) {
            final ClipLauncherSlot slot = slotBank.getItemAt(selectedSlotIndex);
            if (!transport.isPlaying().get()) {
                transport.play();
            }
            if (slot.isRecording().get()) {
                slot.launch();
                transport.isClipLauncherOverdubEnabled().set(false);
            } else if (slot.isPlaying().get()) {
                transport.isClipLauncherOverdubEnabled().toggle();
            } else {
                slot.launch();
                transport.isClipLauncherOverdubEnabled().set(true);
            }
        } else {
            findNextEmptySlot(true).ifPresent(slot -> {
                slot.launch();
                transport.isClipLauncherOverdubEnabled().set(true);
            });
        }
    }
    
    private Optional<ClipLauncherSlot> findNextEmptySlot(final boolean select) {
        ClipLauncherSlot lastEmpty = null;
        int lastEmptyIndex = -1;
        for (int i = slotRange - 1; i >= 0; i--) {
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            if (slot.hasContent().get()) {
                if (lastEmpty != null) {
                    break;
                }
            } else {
                lastEmpty = slot;
                lastEmptyIndex = i;
            }
        }
        if (lastEmpty != null) {
            if (select) {
                slotBank.select(lastEmptyIndex);
            }
            return Optional.of(lastEmpty);
        }
        return Optional.empty();
    }
    
    public void quantize(final double amount) {
        mainCursoClip.quantize(amount);
    }
    
    
}
