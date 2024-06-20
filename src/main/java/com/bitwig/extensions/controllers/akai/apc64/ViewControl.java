package com.bitwig.extensions.controllers.akai.apc64;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apc.common.led.ColorLookup;
import com.bitwig.extensions.framework.di.Component;

@Component
public class ViewControl {

    private final TrackBank trackBank;
    private final TrackBank maxTrackBank;
    private final CursorTrack cursorTrack;
    private final Track rootTrack;
    private final Clip cursorClip;
    private final DeviceControl deviceControl;
    private int selectedTrackIndex;
    private final int[] trackColors = new int[8];
    private int cursorTrackColor = 0;
    private final OverviewGrid overviewGrid = new OverviewGrid();

    public ViewControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(8, 1, 8, true);
        maxTrackBank = host.createTrackBank(64, 1, 64, false);
        maxTrackBank.sceneBank().scrollPosition().markInterested();
        maxTrackBank.scrollPosition().markInterested();

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

        cursorTrack = host.createCursorTrack(6, 128);
        trackBank.followCursorTrack(cursorTrack);
        cursorTrack.exists().markInterested();
        for (int i = 0; i < 8; i++) {
            int index = i;
            Track track = trackBank.getItemAt(i);
            prepareTrack(track);
            track.color().addValueObserver((r, g, b) -> {
                trackColors[index] = ColorLookup.toColor(r, g, b);
            });
            track.addIsSelectedInMixerObserver(select -> {
                if (select) {
                    this.selectedTrackIndex = index;
                }
            });
        }
        setUpFocusScene();

        deviceControl = new DeviceControl(cursorTrack, rootTrack);
        cursorTrack.name().markInterested();
        cursorClip = host.createLauncherCursorClip(32, 128);
        cursorClip.setStepSize(0.125);

        cursorTrack.color().addValueObserver((r, g, b) -> {
            this.cursorTrackColor = com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup.toColor(r, g, b);
        });
        prepareTrack(cursorTrack);
    }

    private void setUpFocusScene() {
        for (int i = 0; i < 64; i++) {
            final int trackIndex = i;
            Track track = maxTrackBank.getItemAt(trackIndex);
            for (int j = 0; j < 64; j++) {
                int sceneIndex = j;
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

    public int getTrackColor(int index) {
        return trackColors[index];
    }

    public int getCursorTrackColor() {
        return cursorTrackColor;
    }

    public int getSelectedTrackIndex() {
        return selectedTrackIndex;
    }

    private void prepareTrack(final Track track) {
        track.arm().markInterested();
        track.exists().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
    }

    public void scrollToOverview(final int trackIndex, final int sceneIndex) {
        final int posX = trackIndex * 8 + overviewGrid.getTrackOffset();
        final int posY = sceneIndex * 8 + overviewGrid.getSceneOffset();
        if (posX < overviewGrid.getNumberOfTracks() && posY < overviewGrid.getNumberOfScenes()) {
            trackBank.scrollPosition().set(posX);
            trackBank.sceneBank().scrollPosition().set(posY);
        }
    }

    public boolean inOverviewGrid(final int trackIndex, final int sceneIndex) {
        return overviewGrid.inGrid(trackIndex, sceneIndex);
    }

    public boolean canScrollVertical(final int delta) {
        int newPos = overviewGrid.getScenePosition() + delta;
        return newPos >= 0 && newPos < overviewGrid.getNumberOfScenes();
    }


    public boolean canScrollHorizontal(final int delta) {
        int newPos = overviewGrid.getTrackPosition() + delta;
        return newPos >= 0 && newPos < overviewGrid.getNumberOfTracks();
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

    public Track getRootTrack() {
        return rootTrack;
    }

    public Clip getCursorClip() {
        return cursorClip;
    }

    public OverviewGrid getOverviewGrid() {
        return overviewGrid;
    }

    public DeviceControl getDeviceControl() {
        return deviceControl;
    }

    public boolean hasQueuedClips(int sceneIndex) {
        return overviewGrid.hasQueuedScenes(sceneIndex);
    }

    public boolean hasClips(int trackIndex, int sceneIndex) {
        return overviewGrid.hasClips(trackIndex, sceneIndex);
    }
}
