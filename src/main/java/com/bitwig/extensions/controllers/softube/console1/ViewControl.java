package com.bitwig.extensions.controllers.softube.console1;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.TrackBankFlatteningMode;
import com.bitwig.extensions.framework.di.Component;

@Component
public class ViewControl {
    
    private final Track rootTrack;
    private final CursorTrack cursorTrack;
    private final TrackBank trackBank;
    
    public ViewControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(1024, 6, 1, true);
        trackBank.setFlatteningMode(TrackBankFlatteningMode.FLATTEN);
        trackBank.setShouldIncludeAllMixerChannels(true);
        cursorTrack = host.createCursorTrack(6, 16);
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
    
    
}