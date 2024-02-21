package com.bitwig.extensions.controllers.mcu;

import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DetailEditor;
import com.bitwig.extension.controller.api.Mixer;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.McuFunction;
import com.bitwig.extensions.framework.di.Component;

@Component
public class ViewControl {
    
    private final TrackBankView globalTrackBank;
    private final TrackBankView mainTrackBank;
    private final CursorTrack cursorTrack;
    
    private final CursorDeviceControl cursorDeviceControl;
    private final Track rootTrack;
    private final CursorRemoteControlsPage projectRemotes;
    private final CursorRemoteControlsPage trackRemotes;
    private final int numberOfSends;
    private final Mixer mixer;
    private final Arranger arranger;
    private final DetailEditor detailEditor;
    
    public ViewControl(final ControllerHost host, final ControllerConfig controllerConfig,
        final GlobalStates globalStates) {
        final int numberOfHwChannels = (controllerConfig.getNrOfExtenders() + 1) * 8;
        final int nrOfScenes = controllerConfig.getAssignment(McuFunction.CLIP_LAUNCHER_MODE_2) != null ? 2 : 4;
        cursorTrack = host.createCursorTrack(8, nrOfScenes);
        cursorDeviceControl = new CursorDeviceControl(cursorTrack, 8, numberOfHwChannels);
        numberOfSends = controllerConfig.hasDirectSelect() ? 8 : 1;
        
        mainTrackBank =
            new TrackBankView(host.createMainTrackBank(numberOfHwChannels, numberOfSends, nrOfScenes), globalStates,
                false, numberOfSends);
        globalTrackBank =
            new TrackBankView(host.createTrackBank(numberOfHwChannels, numberOfSends, nrOfScenes), globalStates, true,
                numberOfSends);
        
        mainTrackBank.getTrackBank().followCursorTrack(cursorTrack);
        globalTrackBank.getTrackBank().followCursorTrack(cursorTrack);
        
        rootTrack = host.getProject().getRootTrackGroup();
        mixer = host.createMixer();
        arranger = host.createArranger();
        detailEditor = host.createDetailEditor();
        
        trackRemotes = cursorTrack.createCursorRemoteControlsPage("track-remotes", 8, null);
        projectRemotes = rootTrack.createCursorRemoteControlsPage(8);
        
        trackRemotes.pageCount().markInterested();
        projectRemotes.pageCount().markInterested();
    }
    
    public Track getRootTrack() {
        return rootTrack;
    }
    
    public CursorRemoteControlsPage getTrackRemotes() {
        return trackRemotes;
    }
    
    public CursorRemoteControlsPage getProjectRemotes() {
        return projectRemotes;
    }
    
    public TrackBank getMainTrackBank() {
        return mainTrackBank.getTrackBank();
    }
    
    public TrackBank getGlobalTrackBank() {
        return globalTrackBank.getTrackBank();
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public CursorDeviceControl getCursorDeviceControl() {
        return cursorDeviceControl;
    }
    
    public int[] getColor(final int channelOffset) {
        return mainTrackBank.getColor(channelOffset);
    }
    
    public void navigateToTrackRemotePage(final int index) {
        if (index < trackRemotes.pageCount().get()) {
            trackRemotes.selectedPageIndex().set(index);
        }
    }
    
    public void navigateToProjectRemotePage(final int index) {
        if (index < projectRemotes.pageCount().get()) {
            projectRemotes.selectedPageIndex().set(index);
        }
    }
    
    public void navigateSends(final int dir) {
        mainTrackBank.navigateSends(dir);
        globalTrackBank.navigateSends(dir);
    }
    
    public void navigateChannels(final int dir) {
        mainTrackBank.navigateChannels(dir);
        globalTrackBank.navigateChannels(dir);
    }
    
    public void navigateToSends(final int index) {
        mainTrackBank.navigateToSends(index);
        globalTrackBank.navigateToSends(index);
    }
    
    public void navigateClipVertical(final int dir) {
        mainTrackBank.getTrackBank().sceneBank().scrollBy(-dir);
    }
    
    public Mixer getMixer() {
        return mixer;
    }
    
    public Arranger getArranger() {
        return arranger;
    }
    
    public DetailEditor getDetailEditor() {
        return detailEditor;
    }
}
