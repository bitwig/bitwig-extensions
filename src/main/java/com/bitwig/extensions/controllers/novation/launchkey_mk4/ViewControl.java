package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
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
    private final RemotePageName deviceRemotesPages;
    private final RemotePageName trackRemotesPages;
    private final RemotePageName projectRemotesPages;
    private final Clip arrangerClip;
    private final Scene focusScene;
    private final SceneBank sceneBank;
    private final String[] trackType = new String[8];
    private final boolean[] canHoldNoteData = new boolean[8];
    
    public ViewControl(final ControllerHost host) {
        Arrays.fill(trackType, "");
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(8, 1, MAX_SCENES, true);
        maxTrackBank = host.createTrackBank(MAX_TRACKS, 1, MAX_SCENES, false);
        maxTrackBank.sceneBank().scrollPosition().markInterested();
        maxTrackBank.scrollPosition().markInterested();
        
        cursorTrack = host.createCursorTrack(2, 16);
        prepareTrack(-1, cursorTrack);
        prepareSlots(cursorTrack);
        trackBank.followCursorTrack(cursorTrack);
        cursorTrack.exists().markInterested();
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Track track = trackBank.getItemAt(i);
            prepareTrack(i, track);
            track.addIsSelectedInMixerObserver(select -> {
                if (select) {
                    this.selectedTrackIndex.set(index);
                }
            });
            track.trackType().addValueObserver(type -> trackType[index] = type);
            track.canHoldNoteData().addValueObserver(canHoldNoteData -> this.canHoldNoteData[index] = canHoldNoteData);
        }
        
        sceneBank = trackBank.sceneBank();
        sceneBank.scrollPosition().addValueObserver(scene -> maxTrackBank.sceneBank().scrollPosition().set(scene));
        
        cursorClip = host.createLauncherCursorClip(16, 128);
        cursorClip.setStepSize(0.125);
        
        cursorClip.exists().markInterested();
        arrangerClip = host.createArrangerCursorClip(16, 128);
        
        primaryDevice =
            cursorTrack.createCursorDevice("DrumDetection", "Pad Device", 2, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        cursorDevice = cursorTrack.createCursorDevice("device-control", "Device Control", 0,
            CursorDeviceFollowMode.FOLLOW_SELECTION);
        cursorDevice.isWindowOpen().markInterested();
        
        primaryRemotes = cursorDevice.createCursorRemoteControlsPage(8);
        trackRemotes = cursorTrack.createCursorRemoteControlsPage(8);
        projectRemotes = rootTrack.createCursorRemoteControlsPage(8);
        deviceRemotesPages = new RemotePageName(primaryRemotes, cursorDevice.name());
        trackRemotesPages = new RemotePageName(trackRemotes, new BasicStringValue("Track Remotes"));
        projectRemotesPages = new RemotePageName(projectRemotes, new BasicStringValue("Project Remotes"));
        
        cursorDevice.name().addValueObserver(deviceName -> {
            deviceDescriptor.set(deviceName);
        });
        
        sceneBank.canScrollBackwards().markInterested();
        sceneBank.canScrollForwards().markInterested();
        focusScene = sceneBank.getScene(0);
        focusScene.clipCount().markInterested();
    }
    
    public IntValueObject getSelectedTrackIndex() {
        return selectedTrackIndex;
    }
    
    private void prepareTrack(final int index, final Track track) {
        track.arm().markInterested();
        track.exists().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
        track.trackType().markInterested();
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
    
    public boolean canBeSelectedForSeq(final int index) {
        return canHoldNoteData[index] && trackType[index].equals("Instrument");
    }
    
    public RemotePageName getDeviceRemotesPages() {
        return deviceRemotesPages;
    }
    
    public RemotePageName getTrackRemotesPages() {
        return trackRemotesPages;
    }
    
    public RemotePageName getProjectRemotesPages() {
        return projectRemotesPages;
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
    
    public Clip getArrangerClip() {
        return arrangerClip;
    }
    
    public Scene getFocusScene() {
        return focusScene;
    }
    
    public SceneBank getSceneBank() {
        return sceneBank;
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
    
    public PinnableCursorDevice getPrimaryDevice() {
        return primaryDevice;
    }
    
    public BasicStringValue getDeviceDescriptor() {
        return deviceDescriptor;
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
