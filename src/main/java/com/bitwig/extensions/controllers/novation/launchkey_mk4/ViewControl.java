package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.IntValueObject;

@Component
public class ViewControl {
    
    private static final int MAX_TRACKS = 64;
    private static final int MAX_SCENES = 2;
    private final TrackBank trackBank;
    private final TrackBank maxTrackBank;
    private final CursorTrack cursorTrack;
    private final Track rootTrack;
    private final Clip cursorClip;
    private final IntValueObject selectedTrackIndex = new IntValueObject(-1, -1, 100);
    private final PinnableCursorDevice cursorDevice;
    private final PinnableCursorDevice primaryDevice;
    private final CursorRemoteControlsPage primaryRemotes;
    private final CursorRemoteControlsPage trackRemotes;
    private final CursorRemoteControlsPage projectRemotes;
    
    private final BasicStringValue deviceDescriptor = new BasicStringValue("");
    
    public ViewControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(8, 1, MAX_SCENES, true);
        maxTrackBank = host.createTrackBank(MAX_TRACKS, 1, MAX_SCENES, false);
        maxTrackBank.sceneBank().scrollPosition().markInterested();
        maxTrackBank.scrollPosition().markInterested();
        
        cursorTrack = host.createCursorTrack(2, 2);
        
        trackBank.followCursorTrack(cursorTrack);
        cursorTrack.exists().markInterested();
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Track track = trackBank.getItemAt(i);
            prepareTrack(track);
            track.color().addValueObserver((r, g, b) -> {
                //trackColors[index] = ColorLookup.toColor(r, g, b);
            });
            track.addIsSelectedInMixerObserver(select -> {
                if (select) {
                    this.selectedTrackIndex.set(index);
                }
            });
        }
        //setUpFocusScene();
        
        trackBank.sceneBank().scrollPosition()
            .addValueObserver(scene -> maxTrackBank.sceneBank().scrollPosition().set(scene));
        
        //deviceControl = new DeviceControl(cursorTrack, rootTrack);
        cursorTrack.name().markInterested();
        cursorClip = host.createLauncherCursorClip(32, 128);
        cursorClip.setStepSize(0.125);
        
        primaryDevice =
            cursorTrack.createCursorDevice("DrumDetection", "Pad Device", 2, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        cursorDevice = cursorTrack.createCursorDevice("device-control", "Device Control", 0,
            CursorDeviceFollowMode.FOLLOW_SELECTION);
        cursorDevice.isWindowOpen().markInterested();
        
        primaryRemotes = cursorDevice.createCursorRemoteControlsPage(8);
        trackRemotes = cursorTrack.createCursorRemoteControlsPage(8);
        projectRemotes = rootTrack.createCursorRemoteControlsPage(8);
        
        final DeviceBank deviceBank = cursorTrack.createDeviceBank(16);
        //        for (int i = 0; i < deviceBank.getSizeOfBank(); i++) {
        //            this.devices.add(new DeviceView(i, deviceBank, cursorDevice));
        //        }
        cursorDevice.name().addValueObserver(deviceName -> {
            deviceDescriptor.set(deviceName);
        });
        
        prepareTrack(cursorTrack);
    }
    
    //    private void setUpFocusScene() {
    //        for (int i = 0; i < MAX_TRACKS; i++) {
    //            final int trackIndex = i;
    //            final Track track = maxTrackBank.getItemAt(trackIndex);
    //            for (int j = 0; j < MAX_SCENES; j++) {
    //                final int sceneIndex = j;
    //                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
    //                slot.hasContent().addValueObserver(hasContent -> {
    //                    overviewGrid.markHasClips(trackIndex, sceneIndex, hasContent);
    //                });
    //                slot.isPlaybackQueued().addValueObserver(isQueued -> {
    //                    overviewGrid.markSceneQueued(sceneIndex, isQueued);
    //                });
    //            }
    //        }
    //    }
    
    public IntValueObject getSelectedTrackIndex() {
        return selectedTrackIndex;
    }
    
    private void prepareTrack(final Track track) {
        track.arm().markInterested();
        track.exists().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
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
    
    public Clip getCursorClip() {
        return cursorClip;
    }
    
    //    public OverviewGrid getOverviewGrid() {
    //        return overviewGrid;
    //    }
    //
    //    public boolean hasQueuedClips(final int sceneIndex) {
    //        return overviewGrid.hasQueuedScenes(sceneIndex);
    //    }
    
    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }
    
    public CursorRemoteControlsPage getPrimaryRemotes() {
        return primaryRemotes;
    }
    
    public CursorRemoteControlsPage getTrackRemotes() {
        return trackRemotes;
    }
    
    public CursorRemoteControlsPage getProjectRemotes() {
        return projectRemotes;
    }
    
    //    public List<DeviceView> getDevices() {
    //        return devices;
    //    }
    
    public PinnableCursorDevice getPrimaryDevice() {
        return primaryDevice;
    }
    
    public BasicStringValue getDeviceDescriptor() {
        return deviceDescriptor;
    }
    
    //    public void selectDrumDevice(final int note) {
    //        devices.stream().filter(device -> device.isDrumDevice()) //
    //            .filter(device -> device.isSelected()) //
    //            .findFirst()//
    //            .ifPresent(device -> device.selectKeyPad(note));
    //    }
    
}
