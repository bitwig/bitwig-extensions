package com.bitwig.extensions.controllers.akai.mpkmk4;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
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
    
    public MpkViewControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(4, 2, 4, true);
        cursorTrack = host.createCursorTrack(6, 128);
        trackBank.followCursorTrack(cursorTrack);
        cursorClip = host.createLauncherCursorClip(32, 128);
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
    }
    
    private void prepareTrack(final Track track) {
        track.arm().markInterested();
        track.exists().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
        track.isQueuedForStop().markInterested();
        track.isStopped().markInterested();
    }
    
    public TrackBank getTrackBank() {
        return trackBank;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public Track getRootTrack() {
        return rootTrack;
    }
    
    public int getSelectedTrackIndex() {
        return selectedTrackIndex;
    }
}
