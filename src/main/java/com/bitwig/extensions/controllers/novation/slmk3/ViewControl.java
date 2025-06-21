package com.bitwig.extensions.controllers.novation.slmk3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.Groove;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
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
    private final OverviewGrid overviewGrid = new OverviewGrid();
    private final PinnableCursorDevice cursorDevice;
    private final PinnableCursorDevice primaryDevice;
    private final CursorRemoteControlsPage primaryRemotes;
    private final CursorRemoteControlsPage trackRemotes;
    private final CursorRemoteControlsPage projectRemotes;
    private final List<DeviceView> devices = new ArrayList<>();
    
    private final BasicStringValue deviceDescriptor = new BasicStringValue();
    private final Groove groove;
    private final DrumPadBank padBank;
    
    @Inject
    private Transport transport;
    
    public ViewControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(8, 1, MAX_SCENES, true);
        maxTrackBank = host.createTrackBank(MAX_TRACKS, 1, MAX_SCENES, false);
        maxTrackBank.sceneBank().scrollPosition().markInterested();
        maxTrackBank.scrollPosition().markInterested();
        groove = host.createGroove();
        cursorTrack = host.createCursorTrack(2, 16);
        
        trackBank.followCursorTrack(cursorTrack);
        cursorTrack.exists().markInterested();
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Track track = trackBank.getItemAt(i);
            prepareTrack(track);
            track.addIsSelectedInMixerObserver(select -> {
                if (select) {
                    this.selectedTrackIndex.set(index);
                }
            });
        }
        setUpFocusScene();
        
        trackBank.sceneBank().scrollPosition()
            .addValueObserver(scene -> maxTrackBank.sceneBank().scrollPosition().set(scene));
        
        //deviceControl = new DeviceControl(cursorTrack, rootTrack);
        cursorTrack.name().markInterested();
        cursorClip = host.createLauncherCursorClip(32, 128);
        cursorClip.setStepSize(0.125);
        
        primaryDevice =
            cursorTrack.createCursorDevice("DrumDetection", "Pad Device", 1, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        padBank = primaryDevice.createDrumPadBank(16);
        padBank.exists().markInterested();
        padBank.scrollPosition().markInterested();
        
        cursorDevice = cursorTrack.createCursorDevice("device-control", "Device Control", 0,
            CursorDeviceFollowMode.FOLLOW_SELECTION);
        cursorDevice.isWindowOpen().markInterested();
        
        primaryRemotes = cursorDevice.createCursorRemoteControlsPage(8);
        trackRemotes = cursorTrack.createCursorRemoteControlsPage(8);
        projectRemotes = rootTrack.createCursorRemoteControlsPage(8);
        
        final DeviceBank deviceBank = cursorTrack.createDeviceBank(16);
        for (int i = 0; i < deviceBank.getSizeOfBank(); i++) {
            this.devices.add(new DeviceView(i, deviceBank, cursorDevice));
        }
        cursorDevice.name().addValueObserver(deviceName -> {
            deviceDescriptor.set(deviceName);
        });
        
        prepareTrack(cursorTrack);
        prepareSlots(cursorTrack);
    }
    
    
    private void setUpFocusScene() {
        for (int i = 0; i < MAX_TRACKS; i++) {
            final int trackIndex = i;
            final Track track = maxTrackBank.getItemAt(trackIndex);
            for (int j = 0; j < MAX_SCENES; j++) {
                final int sceneIndex = j;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                slot.hasContent().addValueObserver(hasContent -> {
                    overviewGrid.markHasClips(trackIndex, sceneIndex, hasContent);
                });
                slot.isPlaybackQueued().addValueObserver(isQueued -> {
                    overviewGrid.markSceneQueued(sceneIndex, isQueued);
                });
            }
        }
    }
    
    public Transport getTransport() {
        return transport;
    }
    
    public IntValueObject getSelectedTrackIndex() {
        return selectedTrackIndex;
    }
    
    private void prepareTrack(final Track track) {
        track.arm().markInterested();
        track.exists().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
        track.isQueuedForStop().markInterested();
        track.isGroup().markInterested();
        track.isGroupExpanded().markInterested();
        track.canHoldNoteData().markInterested();
    }
    
    private void prepareSlots(final Track track) {
        final ClipLauncherSlotBank slots = track.clipLauncherSlotBank();
        for (int i = 0; i < slots.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = slots.getItemAt(i);
            slot.isPlaying().markInterested();
            slot.hasContent().markInterested();
            slot.isSelected().markInterested();
        }
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
    
    public Track getRootTrack() {
        return rootTrack;
    }
    
    public Clip getCursorClip() {
        return cursorClip;
    }
    
    public OverviewGrid getOverviewGrid() {
        return overviewGrid;
    }
    
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
    
    public List<DeviceView> getDevices() {
        return devices;
    }
    
    public PinnableCursorDevice getPrimaryDevice() {
        return primaryDevice;
    }
    
    public BasicStringValue getDeviceDescriptor() {
        return deviceDescriptor;
    }
    
    public Groove getGroove() {
        return groove;
    }
    
    public void selectDrumDevice(final int note) {
        devices.stream().filter(device -> device.isDrumDevice()) //
            .findFirst() //
            .ifPresent(device -> device.selectKeyPad(note));
    }
    
    public static Optional<ClipLauncherSlot> filterSlot(final Track track, final Predicate<ClipLauncherSlot> check) {
        final ClipLauncherSlotBank slots = track.clipLauncherSlotBank();
        for (int i = 0; i < slots.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = slots.getItemAt(i);
            if (check.test(slot)) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }
    
    
}
