package com.bitwig.extensions.controllers.novation.commonsmk3;

import java.util.Optional;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.FocusSlot;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component
public class ViewCursorControl {
    public static final double MAX_LENGTH_FOR_DUPLICATE = 512 * 4.0;
    
    @Inject
    Application application;
    
    private final CursorTrack cursorTrack;
    private final DeviceBank deviceBank;
    private final PinnableCursorDevice primaryDevice;
    private final TrackBank trackBank;
    private final PinnableCursorDevice cursorDevice;
    private final Clip cursorClip;
    
    private final Track rootTrack;
    private final ClipLauncherSlotBank mainTrackSlotBank;
    private final Track largeFocusTrack;
    private FocusSlot focusSlot;
    private final TrackBank maxTrackBank;
    
    private final OverviewGrid overviewGrid = new OverviewGrid();
    
    public ViewCursorControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        rootTrack.arm().markInterested();
        
        maxTrackBank = host.createTrackBank(64, 1, 64);
        maxTrackBank.sceneBank().scrollPosition().markInterested();
        maxTrackBank.scrollPosition().markInterested();
        
        
        trackBank = host.createTrackBank(8, 1, 8);
        
        trackBank.sceneBank().itemCount().addValueObserver(overviewGrid::setNumberOfScenes);
        trackBank.channelCount().addValueObserver(overviewGrid::setNumberOfTracks);
        trackBank.scrollPosition().addValueObserver(pos -> {
            overviewGrid.setTrackPosition(pos);
            if (maxTrackBank.scrollPosition().get() != overviewGrid.getTrackOffset()) {
                maxTrackBank.scrollPosition().set(overviewGrid.getTrackOffset());
            }
        });
        trackBank.sceneBank().scrollPosition().addValueObserver(pos -> {
            overviewGrid.setScenePosition(pos);
            if (maxTrackBank.sceneBank().scrollPosition().get() != overviewGrid.getSceneOffset()) {
                maxTrackBank.sceneBank().scrollPosition().set(overviewGrid.getSceneOffset());
            }
        });
        setUpFocusScene();
        
        cursorTrack = host.createCursorTrack(8, 8);
        for (int i = 0; i < 8; i++) {
            prepareTrack(trackBank.getItemAt(i));
        }
        
        cursorTrack.name().markInterested();
        cursorDevice = cursorTrack.createCursorDevice();
        cursorClip = host.createLauncherCursorClip(32 * 6, 127);
        
        cursorTrack.clipLauncherSlotBank().cursorIndex().addValueObserver(index -> {
            // RemoteConsole.out.println(" => {}", index);
        });
        prepareTrack(cursorTrack);
        
        deviceBank = cursorTrack.createDeviceBank(8);
        primaryDevice =
            cursorTrack.createCursorDevice("drumdetection", "Pad Device", 8, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        primaryDevice.hasDrumPads().markInterested();
        primaryDevice.exists().markInterested();
        
        
        final TrackBank singleTrackBank = host.createTrackBank(1, 0, 16);
        singleTrackBank.scrollPosition().markInterested();
        singleTrackBank.followCursorTrack(cursorTrack);
        largeFocusTrack = singleTrackBank.getItemAt(0);
        prepareTrack(largeFocusTrack);
        
        mainTrackSlotBank = largeFocusTrack.clipLauncherSlotBank();
        final BooleanValue equalsToCursorTrack = largeFocusTrack.createEqualsValue(cursorTrack);
        equalsToCursorTrack.markInterested();
        
        
        for (int i = 0; i < 16; i++) {
            final int index = i;
            final ClipLauncherSlot slot = mainTrackSlotBank.getItemAt(i);
            prepareSlot(slot);
            
            slot.isSelected().addValueObserver(selected -> {
                if (selected) {
                    focusSlot = new FocusSlot(largeFocusTrack, slot, index, equalsToCursorTrack);
                }
            });
        }
    }
    
    private void setUpFocusScene() {
        for (int i = 0; i < 64; i++) {
            final int trackIndex = i;
            final Track track = maxTrackBank.getItemAt(trackIndex);
            for (int j = 0; j < 64; j++) {
                final int sceneIndex = j;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                slot.hasContent().addValueObserver(hasContent -> {
                    overviewGrid.setHasClips(trackIndex, sceneIndex, hasContent);
                });
                slot.isPlaybackQueued().addValueObserver(isQueued -> {
                    overviewGrid.markSceneQueued(sceneIndex, isQueued);
                });
            }
        }
    }
    
    public boolean hasQueuedForPlaying(final int sceneIndex) {
        return overviewGrid.hasQueuedScenes(sceneIndex);
    }
    
    private void prepareTrack(final Track track) {
        track.arm().markInterested();
        track.monitorMode().markInterested();
        track.sourceSelector().hasAudioInputSelected().markInterested();
        track.sourceSelector().hasNoteInputSelected().markInterested();
    }
    
    private void prepareSlot(final ClipLauncherSlot slot) {
        slot.isRecording().markInterested();
        slot.isRecordingQueued().markInterested();
        slot.hasContent().markInterested();
        slot.name().markInterested();
        slot.isPlaying().markInterested();
        slot.isSelected().markInterested();
    }
    
    public void scrollToOverview(final int trackIndex, final int sceneIndex) {
        final int posX = trackIndex * 8;
        final int posY = sceneIndex * 8;
        if (posX < overviewGrid.getNumberOfTracks() && posY < overviewGrid.getNumberOfScenes()) {
            trackBank.scrollPosition().set(posX);
            trackBank.sceneBank().scrollPosition().set(posY);
        }
    }
    
    public boolean inOverviewGrid(final int trackIndex, final int sceneIndex) {
        final int posX = trackIndex * 8;
        final int posY = sceneIndex * 8;
        return posX < overviewGrid.getNumberOfTracks() && posY < overviewGrid.getNumberOfScenes();
    }
    
    public boolean inOverviewGridFocus(final int trackIndex, final int sceneIndex) {
        final int locX = overviewGrid.getTrackPosition() / 8;
        final int locY = overviewGrid.getScenePosition() / 8;
        return locX == trackIndex && locY == sceneIndex;
    }
    
    public TrackBank getTrackBank() {
        return trackBank;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public DeviceBank getDeviceBank() {
        return deviceBank;
    }
    
    public PinnableCursorDevice getPrimaryDevice() {
        return primaryDevice;
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
    
    public void focusSlot(final FocusSlot slot) {
        focusSlot = slot;
    }
    
    public FocusSlot getFocusSlot() {
        return focusSlot;
    }
    
    public void createNewClip() {
        if (focusSlot != null) {
            final ClipLauncherSlot slot = focusSlot.getSlot();
            if (!slot.hasContent().get()) {
                slot.createEmptyClip(4);
                slot.select();
            } else {
                cursorTrack.createNewLauncherClip(0);
                // TODO try to move to next slot if possible
            }
        } else {
            cursorTrack.createNewLauncherClip(0);
        }
    }
    
    private boolean trackOfFocusSlotArmed() {
        return focusSlot != null && focusSlot.getTrack().arm().get();
    }
    
    public void globalRecordAction(final Transport transport) {
        if (largeFocusTrack.arm().get() || trackOfFocusSlotArmed()) {
            if (focusSlot != null) {
                handleFocusedSlotOnArmedTrack(transport);
            } else {
                handleNoFocusSlotOnArmedTrack(transport);
            }
        } else if (focusSlot != null) {
            handleRecordFocusSlotNotArmed(transport);
        } else {
            transport.isClipLauncherOverdubEnabled().toggle();
        }
    }
    
    private void handleNoFocusSlotOnArmedTrack(final Transport transport) {
        final Optional<ClipLauncherSlot> playingSlot = findPlayingSlot();
        if (playingSlot.isPresent()) {
            toggleRecording(playingSlot.get(), transport);
        } else {
            findEmptySlotAndLaunch(transport, -1);
        }
    }
    
    private void handleFocusedSlotOnArmedTrack(final Transport transport) {
        if (focusSlot.getTrack().arm().get()) {
            if (focusSlot.isEmpty()) {
                recordToEmptySlot(focusSlot.getSlot(), transport);
            } else {
                toggleRecording(focusSlot.getSlot(), transport);
            }
        } else {
            findEmptySlotAndLaunch(transport, focusSlot.getSlotIndex());
        }
    }
    
    private void handleRecordFocusSlotNotArmed(final Transport transport) {
        final Track track = focusSlot.getTrack();
        if (canRecord(focusSlot.getTrack())) {
            track.arm().set(true);
            track.selectInEditor();
            if (!focusSlot.isEmpty()) {
                toggleRecording(focusSlot.getSlot(), transport);
            } else {
                recordToEmptySlot(focusSlot.getSlot(), transport);
            }
        } else {
            transport.isClipLauncherOverdubEnabled().toggle();
        }
    }
    
    private void findEmptySlotAndLaunch(final Transport transport, final int slotIndex) {
        final Optional<ClipLauncherSlot> slot = findCursorFirstEmptySlot(slotIndex);
        if (slot.isPresent()) {
            recordToEmptySlot(slot.get(), transport);
        } else {
            transport.isClipLauncherOverdubEnabled().toggle();
        }
    }
    
    private boolean canRecord(final Track track) {
        return track.sourceSelector().hasNoteInputSelected().get() || track.sourceSelector().hasAudioInputSelected()
            .get();
    }
    
    public Optional<ClipLauncherSlot> findCursorFirstEmptySlot(final int firstIndex) {
        if (firstIndex >= 0 && firstIndex < 16) {
            final ClipLauncherSlot slot = mainTrackSlotBank.getItemAt(firstIndex);
            if (!slot.hasContent().get()) {
                return Optional.of(slot);
            }
        }
        for (int i = 0; i < 16; i++) {
            final ClipLauncherSlot slot = mainTrackSlotBank.getItemAt(i);
            if (!slot.hasContent().get()) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }
    
    private void toggleRecording(final ClipLauncherSlot slot, final Transport transport) {
        if (slot.isRecordingQueued().get() || slot.isRecording().get()) {
            slot.launch();
            transport.isClipLauncherOverdubEnabled().set(false);
        } else if (!slot.isPlaying().get()) {
            slot.launch();
            transport.isClipLauncherOverdubEnabled().set(true);
        } else if (transport.isPlaying().get()) {
            transport.isClipLauncherOverdubEnabled().toggle();
        } else {
            transport.restart();
            slot.launch();
            transport.isClipLauncherOverdubEnabled().set(true);
        }
    }
    
    private void recordToEmptySlot(final ClipLauncherSlot slot, final Transport transport) {
        if (!transport.isPlaying().get()) {
            transport.restart();
        }
        slot.select();
        slot.launch();
        transport.isClipLauncherOverdubEnabled().set(true);
    }
    
    public Optional<ClipLauncherSlot> findPlayingSlot() {
        for (int i = 0; i < 16; i++) {
            final ClipLauncherSlot slot = mainTrackSlotBank.getItemAt(i);
            if (slot.hasContent().get() && slot.isPlaying().get()) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }
    
    public void handleQuantize(final boolean shift) {
        cursorClip.quantize(1.0);
        final ClipLauncherSlot slot = cursorClip.clipLauncherSlot();
        slot.showInEditor();
    }
    
    public void handleDuplication(final boolean shift) {
        if (focusSlot == null || focusSlot.isEmpty() || !focusSlot.isCursorTrack()) {
            return;
        }
        if (!shift) {
            cursorClip.duplicate();
        } else {
            if (cursorClip.getLoopLength().get() < MAX_LENGTH_FOR_DUPLICATE) {
                cursorClip.duplicateContent();
                cursorClip.clipLauncherSlot().showInEditor();
            }
        }
    }
    
    public void handleClear(final boolean shift) {
        if (focusSlot == null || focusSlot.isEmpty() || !focusSlot.isCursorTrack()) {
            return;
        }
        if (!shift) {
            cursorClip.clearSteps();
            cursorClip.clipLauncherSlot().showInEditor();
        } else {
            focusSlot.getSlot().deleteObject();
        }
    }
    
    public boolean hasClips(final int trackIndex, final int sceneIndex) {
        return overviewGrid.hasClips(trackIndex, sceneIndex);
    }
}
