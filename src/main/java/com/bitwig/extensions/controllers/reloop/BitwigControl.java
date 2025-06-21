package com.bitwig.extensions.controllers.reloop;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.di.Component;

@Component
public class BitwigControl {
    
    private final TrackBank trackBank;
    private final CursorTrack cursorTrack;
    private final Track rootTrack;
    private int selectTrackIndex = 0;
    private final PinnableCursorDevice cursorDevice;
    private final CursorRemoteControlsPage remotes;
    private final Clip cursorClip;
    private final DeviceBank deviceBank;
    
    public BitwigControl(final ControllerHost host) {
        trackBank = host.createTrackBank(32, 2, 2);
        trackBank.sceneBank().setIndication(true);
        trackBank.setShouldShowClipLauncherFeedback(true);
        cursorTrack = host.createCursorTrack(1, 2);
        //cursorClip = cursorTrack.createLauncherCursorClip(16, 127);
        cursorClip = host.createLauncherCursorClip(32, 127);
        cursorDevice = cursorTrack.createCursorDevice();
        remotes = cursorDevice.createCursorRemoteControlsPage(8);
        rootTrack = host.getProject().getRootTrackGroup();
        deviceBank = cursorTrack.createDeviceBank(8);
    }
    
    public CursorRemoteControlsPage getRemotes() {
        return remotes;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public TrackBank getTrackBank() {
        return trackBank;
    }
    
    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }
    
    public Clip getCursorClip() {
        return cursorClip;
    }
    
    public Track getRootTrack() {
        return rootTrack;
    }
    
    public int getSelectTrackIndex() {
        return selectTrackIndex;
    }
    
    public void setSelectTrackIndex(final int selectTrackIndex) {
        this.selectTrackIndex = selectTrackIndex;
    }
    
    public void clearAllSteps() {
        cursorClip.clearSteps();
    }
    
    public void navigateRemotes(final int dir) {
        if (dir > 0) {
            remotes.selectNext();
        } else {
            remotes.selectPrevious();
        }
    }
    
    public void navigateDevices(final int dir) {
        if (dir > 0) {
            cursorDevice.selectPrevious();
        } else {
            cursorDevice.selectNext();
        }
    }
    
    public DeviceBank getDeviceBank() {
        return deviceBank;
    }
}
