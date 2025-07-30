package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

public class ViewControl {
    private final ClipSceneCursor clipSceneCursor;
    private final Application application;
    private final NavigationState navigationState = new NavigationState();
    private final CursorTrack cursorTrack;
    private final PinnableCursorDevice cursorDevice;
    protected TrackBank mixerTrackBank;
    protected Transport transport;
    protected Project project;
    
    public ViewControl(final ControllerHost host) {
        application = host.createApplication();
        clipSceneCursor = new ClipSceneCursor(host, navigationState);
        cursorTrack = clipSceneCursor.getCursorTrack();
        cursorDevice = cursorTrack.createCursorDevice();
        project = host.getProject();
        transport = host.createTransport();
        transport.playPosition().markInterested();
        transport.arrangerLoopStart().markInterested();
        transport.arrangerLoopDuration().markInterested();
        
        mixerTrackBank = host.createTrackBank(8, 0, 1);
        mixerTrackBank.setSkipDisabledItems(true);
        mixerTrackBank.canScrollChannelsDown().markInterested();
        mixerTrackBank.canScrollChannelsUp().markInterested();
        mixerTrackBank.followCursorTrack(cursorTrack);
        mixerTrackBank.setChannelScrollStepSize(8);
    }
    
    public Project getProject() {
        return project;
    }
    
    public NavigationState getNavigationState() {
        return navigationState;
    }
    
    public Application getApplication() {
        return application;
    }
    
    public ClipSceneCursor getClipSceneCursor() {
        return clipSceneCursor;
    }
    
    public void insertInstrument() {
        application.createInstrumentTrack(-1);
    }
    
    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public Transport getTransport() {
        return transport;
    }
    
    public TrackBank getMixerTrackBank() {
        return mixerTrackBank;
    }
}
