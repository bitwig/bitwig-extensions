package com.bitwig.extensions.controllers.allenheath.xonek3;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.view.OverviewGrid;

@Component
public class ViewControl {
    
    private final Track rootTrack;
    private final TrackBank trackBank;
    private final OverviewGrid overviewGrid;
    private final TrackBank maxTrackBank;
    private final CursorTrack cursorTrack;
    private final PinnableCursorDevice cursorDevice;
    private final Project project;
    private final CursorRemoteControlsPage deviceRemotePages;
    private final CursorRemoteControlsPage projectRemotes;
    private final CursorRemoteControlsPage trackRemotes;
    private final List<TrackSpecControl> specControls = new ArrayList<>();
    
    public ViewControl(final ControllerHost host, final XoneK3GlobalStates globalStates) {
        rootTrack = host.getProject().getRootTrackGroup();
        // Create Track Bank Params  // Num Track | Num Sends | Num Scenes | has Flat List
        trackBank = host.createTrackBank(4 * globalStates.getDeviceCount(), 5, 4, true);
        maxTrackBank = host.createTrackBank(64, 1, 64, false);
        overviewGrid = new OverviewGrid(maxTrackBank);
        cursorTrack = host.createCursorTrack(2, 1);
        trackBank.followCursorTrack(cursorTrack);
        cursorDevice = cursorTrack.createCursorDevice();
        project = host.getProject();
        deviceRemotePages = cursorDevice.createCursorRemoteControlsPage(8);
        final Track rootTrack = project.getRootTrackGroup();
        projectRemotes = rootTrack.createCursorRemoteControlsPage(8);
        trackRemotes = cursorTrack.createCursorRemoteControlsPage(8);
        
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final Track track = trackBank.getItemAt(i);
            prepareTrack(track);
            specControls.add(new TrackSpecControl(i, track, host));
        }
        overviewGrid.setUpFocusScene(trackBank);
    }
    
    public TrackBank getTrackBank() {
        return trackBank;
    }
    
    private void prepareTrack(final Track track) {
        track.arm().markInterested();
        track.exists().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
        track.isGroup().markInterested();
        track.isGroupExpanded().markInterested();
        track.isMutedBySolo().markInterested();
        track.isQueuedForStop().markInterested();
        track.isStopped().markInterested();
        track.isQueuedForStop().markInterested();
    }
    
    public Track getRootTrack() {
        return rootTrack;
    }
    
    public List<TrackSpecControl> getSpecControls() {
        return specControls;
    }
    
    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }
    
    public boolean hasQueuedClips(final int sceneIndex) {
        return overviewGrid.hasQueuedScenes(sceneIndex);
    }
    
    public boolean hasPlayingClips(final int sceneIndex) {
        return overviewGrid.hasPlayingClips(sceneIndex);
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public CursorRemoteControlsPage getTrackRemotes() {
        return trackRemotes;
    }
    
    public CursorRemoteControlsPage getDeviceRemotePages() {
        return deviceRemotePages;
    }
    
    public CursorRemoteControlsPage getProjectRemotes() {
        return projectRemotes;
    }
    
}