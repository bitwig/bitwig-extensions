package com.bitwig.extensions.controllers.neuzeitinstruments;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.view.OverviewGrid;

@Component
public class DropViewControl {
    private final Track rootTrack;
    private final CursorTrack cursorTrack;
    private final TrackBank launcherTrackBank;
    private final TrackBank arrangerTrackBank;
    private final OverviewGrid overviewGrid;
    private final TrackBank maxTrackBank;
    
    public DropViewControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        launcherTrackBank = host.createTrackBank(4, 1, 4, true);
        arrangerTrackBank = host.createTrackBank(5, 1, 3, true);
        cursorTrack = host.createCursorTrack(1, 4);
        maxTrackBank = host.createTrackBank(64, 1, 64, false);
        overviewGrid = new OverviewGrid(maxTrackBank);
        prepareBank(launcherTrackBank);
        prepareBank(arrangerTrackBank);
        overviewGrid.setUpFocusScene(launcherTrackBank);
        rootTrack.isQueuedForStop().markInterested();
    }
    
    private void prepareBank(final TrackBank bank) {
        for (int i = 0; i < bank.getSizeOfBank(); i++) {
            prepareTrack(bank.getItemAt(i));
        }
    }
    
    public TrackBank getLauncherTrackBank() {
        return launcherTrackBank;
    }
    
    public TrackBank getArrangerTrackBank() {
        return arrangerTrackBank;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public Track getRootTrack() {
        return rootTrack;
    }
    
    public boolean hasQueuedClips(final int sceneIndex) {
        return overviewGrid.hasQueuedScenes(sceneIndex);
    }
    
    public boolean hasPlayingClips(final int sceneIndex) {
        return overviewGrid.hasPlayingClips(sceneIndex);
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
    }
    
    
}
