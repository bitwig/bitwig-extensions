package com.bitwig.extensions.controllers.akai.mpkmk4;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColor;
import com.bitwig.extensions.framework.di.Component;

@Component
public class MpkViewControl {
    private final TrackBank trackBank;
    private final CursorTrack cursorTrack;
    private final Track rootTrack;
    private final Clip cursorClip;
    private int selectedTrackIndex;
    private final MpkColor[] trackColors = new MpkColor[8];
    private final PinnableCursorDevice cursorDevice;
    private final TrackBank focusTrackBank;
    private final PinnableCursorDevice primaryDevice;
    private final DrumPadBank focusDrumPad;
    private final DrumPadBank padBank;
    private int padBankScrollPosition;
    private final Clip arrangerCursorClip;
    
    public MpkViewControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(4, 2, 4, true);
        focusTrackBank = host.createTrackBank(1, 1, 1);
        cursorTrack = host.createCursorTrack(6, 128);
        trackBank.followCursorTrack(cursorTrack);
        cursorClip = host.createLauncherCursorClip(32, 128);
        arrangerCursorClip = host.createArrangerCursorClip(32, 128);
        
        cursorClip.setStepSize(0.125);
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final int index = i;
            final Track track = trackBank.getItemAt(i);
            prepareTrack(track);
            track.color().addValueObserver((r, g, b) -> {
                trackColors[index] = MpkColor.getColor(r, g, b);
            });
            track.addIsSelectedInMixerObserver(select -> {
                if (select) {
                    this.selectedTrackIndex = index;
                }
            });
        }
        
        cursorDevice = cursorTrack.createCursorDevice();
        cursorDevice.hasNext().markInterested();
        cursorDevice.hasPrevious().markInterested();
        primaryDevice =
            cursorTrack.createCursorDevice("DrumDetection", "Pad Device", 16, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        padBank = primaryDevice.createDrumPadBank(16);
        for (int i = 0; i < padBank.getSizeOfBank(); i++) {
            final int index = i;
            final DrumPad pad = padBank.getItemAt(i);
            pad.addIsSelectedInMixerObserver(selectedInMixer -> handleSelectedInMixer(index, selectedInMixer));
        }
        padBank.scrollPosition().addValueObserver(scrollPosition -> this.padBankScrollPosition = scrollPosition);
        focusDrumPad = primaryDevice.createDrumPadBank(1);
    }
    
    private void handleSelectedInMixer(final int index, final boolean selectedInMixer) {
        if (selectedInMixer) {
            focusDrumPad.scrollPosition().set(padBankScrollPosition + index);
        }
    }
    
    private void prepareTrack(final Track track) {
        track.arm().markInterested();
        track.exists().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
        track.isQueuedForStop().markInterested();
        track.isStopped().markInterested();
    }
    
    public DrumPadBank getPadBank() {
        return padBank;
    }
    
    public TrackBank getTrackBank() {
        return trackBank;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public TrackBank getFocusTrackBank() {
        return focusTrackBank;
    }
    
    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }
    
    public PinnableCursorDevice getPrimaryDevice() {
        return primaryDevice;
    }
    
    public Track getRootTrack() {
        return rootTrack;
    }
    
    public int getSelectedTrackIndex() {
        return selectedTrackIndex;
    }
    
    public DrumPadBank getFocusDrumPad() {
        return focusDrumPad;
    }
    
    public Clip getCursorClip() {
        return cursorClip;
    }
    
    public void invokeArrangerQuantize() {
        arrangerCursorClip.quantize(1.0);
        final ClipLauncherSlot slot = arrangerCursorClip.clipLauncherSlot();
        slot.showInEditor();
    }
}
