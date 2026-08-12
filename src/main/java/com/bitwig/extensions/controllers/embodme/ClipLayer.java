package com.bitwig.extensions.controllers.embodme;

import java.util.Arrays;
import java.util.Optional;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.view.OverviewGrid;

public class ClipLayer {
    public static final int INDEX_OFFSET_ARM = 0x21;
    public static final int INDEX_OFFSET_MUTE = 0x15;
    public static final int INDEX_OFFSET_SOLO = 0x9;
    
    public enum Mode {
        VOLUME,
        SENDS,
        GRID
    }
    
    private final Track rootTrack;
    private int sceneOffset;
    private final OverviewGrid overviewGrid;
    private boolean active;
    private final boolean horizontalMode;
    private final EraeMidiProcessor midiProcessor;
    private final TrackBank trackBank;
    private final TrackState[] trackStates;
    private final SceneState[] sceneStates;
    private final SlotState[][] slotStates;
    private final int trackSize;
    private final int sceneSize;
    private final int[] sliderValues = new int[128];
    private Mode currentHeightMode = Mode.GRID;
    private int gridHeight;
    
    private class SlotState {
        private final ClipLauncherSlot slot;
        private final TrackState trackState;
        private boolean stopQueued;
        private boolean recordingQueued;
        private boolean playbackQueued;
        private boolean recording;
        private boolean playing;
        private int color;
        private int displayColor;
        private int state;
        private boolean hasContent;
        private final int sceneIndex;
        
        public SlotState(final ClipLauncherSlot slot, final TrackState trackState, final int sceneIndex) {
            this.slot = slot;
            this.trackState = trackState;
            this.sceneIndex = sceneIndex;
        }
        
        public int getTrackIndex() {
            return trackState.getIndex();
        }
        
        public int getSceneIndex() {
            return sceneIndex;
        }
        
        public ClipLauncherSlot getSlot() {
            return slot;
        }
        
        public void setHasContent(final boolean hasContent) {
            this.hasContent = hasContent;
        }
        
        public void setStopQueued(final boolean stopQueued) {
            this.stopQueued = stopQueued;
        }
        
        public void setRecordingQueued(final boolean recordingQueued) {
            this.recordingQueued = recordingQueued;
        }
        
        public void setPlaybackQueued(final boolean playbackQueued) {
            this.playbackQueued = playbackQueued;
        }
        
        public void setRecording(final boolean recording) {
            this.recording = recording;
        }
        
        public void setPlaying(final boolean playing) {
            this.playing = playing;
        }
        
        public void setColor(final int color) {
            this.color = color;
        }
        
        public int getState() {
            return state;
        }
        
        private boolean calcState() {
            final int oldState = this.state;
            final int oldColor = this.displayColor;
            if (!hasContent) {
                if (trackState.isArmed()) {
                    this.displayColor = recordingQueued ? EraeColor.RED : EraeColor.RED_DIM;
                } else {
                    this.displayColor = EraeColor.OFF;
                }
                if (recordingQueued || stopQueued || playbackQueued) {
                    this.state = EraeColor.STATE_BLINK;
                } else {
                    this.state = EraeColor.STATE_NORMAL;
                }
            } else if (stopQueued) {
                this.state = EraeColor.STATE_BLINK;
                this.displayColor = EraeColor.GREEN;
            } else if (recordingQueued) {
                this.state = EraeColor.STATE_BLINK;
                this.displayColor = EraeColor.RED;
            } else if (playbackQueued) {
                this.state = EraeColor.STATE_BLINK;
                this.displayColor = color;
            } else if (recording) {
                this.state = EraeColor.STATE_PULSE;
                this.displayColor = EraeColor.RED;
            } else if (playing) {
                this.state = EraeColor.STATE_PULSE;
                this.displayColor = EraeColor.GREEN;
            } else {
                this.state = EraeColor.STATE_NORMAL;
                this.displayColor = color;
            }
            return oldState != state || oldColor != displayColor;
        }
        
        public int getDisplayColor() {
            return displayColor;
        }
    }
    
    public ClipLayer(final ControllerHost host, final int width, final int height,
        final EraeMidiProcessor midiProcessor, final boolean horizontalMode, final OverviewGrid overviewGrid) {
        this.horizontalMode = horizontalMode;
        this.overviewGrid = overviewGrid;
        this.trackSize = horizontalMode ? height : width;
        this.sceneSize = horizontalMode ? width : height;
        overviewGrid.addQueuedSceneListener(this::handleQueued);
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(trackSize, 2, sceneSize);
        slotStates = new SlotState[trackSize][sceneSize];
        trackStates = new TrackState[trackSize];
        sceneStates = new SceneState[sceneSize];
        Arrays.fill(sliderValues, 0);
        this.gridHeight = height;
        this.midiProcessor = midiProcessor;
        
        
        for (int i = 0; i < this.trackSize; i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(i);
            trackStates[trackIndex] = new TrackState(track, i);
            prepareTrackState(trackStates[trackIndex]);
            for (int j = 0; j < this.sceneSize; j++) {
                final int sceneIndex = j;
                final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
                final ClipLauncherSlot slot = slotBank.getItemAt(j);
                slotStates[trackIndex][sceneIndex] = new SlotState(slot, trackStates[trackIndex], sceneIndex);
                prepareSlot(slotStates[trackIndex][sceneIndex]);
            }
            final SettableRangedValue volume = track.volume().value();
            volume.addValueObserver(128, v -> handleVolumeChange(trackIndex, v));
            final SendBank sendBank = track.sendBank();
            sendBank.getItemAt(0).value().addValueObserver(128, v -> handleSendChange(trackIndex, 0, v));
            sendBank.getItemAt(1).value().addValueObserver(128, v -> handleSendChange(trackIndex, 1, v));
        }
        final SceneBank sceneBank = trackBank.sceneBank();
        sceneBank.scrollPosition().addValueObserver(sceneOffset -> {
            this.sceneOffset = sceneOffset;
        });
        
        
        for (int j = 0; j < height; j++) {
            final Scene scene = sceneBank.getScene(j);
            final SceneState sceneState = new SceneState(j);
            sceneStates[j] = sceneState;
            scene.clipCount().addValueObserver(count -> handleSceneCount(sceneState, count));
            scene.exists().addValueObserver(exists -> handleSceneExists(sceneState, exists));
            scene.color().addValueObserver((r, g, b) -> {
                final int colorIndex = EraeColor.rgbToIndex(r, g, b);
                sceneState.setColor(colorIndex);
                updateSceneLed(sceneState);
            });
        }
    }
    
    private void handleQueued(final int index, final boolean queued) {
        final int overviewOffset = overviewGrid.getSceneOffset();
        final int diff = sceneOffset - overviewOffset;
        final int target = index - diff;
        if (target >= 0 && target < this.sceneSize) { // TODO Questionable
            sceneStates[target].setQueued(queued);
            updateSceneLed(sceneStates[target]);
        }
    }
    
    private void handleSceneExists(final SceneState sceneState, final boolean exists) {
        sceneState.setExists(exists);
        updateSceneLed(sceneState);
    }
    
    private void handleSceneCount(final SceneState state, final int count) {
        state.setCount(count);
        updateSceneLed(state);
    }
    
    private void updateSceneLed(final SceneState state) {
        final boolean changed = state.calcState();
        if (isActive() && changed && state.getIndex() < gridHeight) {
            midiProcessor.sendActionButtonUpdate(state.getIndex(), state.getState(), state.getDisplayColor());
        }
    }
    
    private void prepareTrackState(final TrackState trackState) {
        trackState.getTrack().arm().addValueObserver(armed -> {
            trackState.setArmed(armed);
            handleArmUpdate(trackState);
        });
        trackState.getTrack().mute().addValueObserver(muted -> {
            trackState.setMuted(muted);
            updateMuteState(trackState);
        });
        trackState.getTrack().solo().addValueObserver(soloed -> {
            trackState.setSolo(soloed);
            updateSoloState(trackState);
        });
    }
    
    private void handleArmUpdate(final TrackState trackState) {
        if (isActive()) {
            for (int sceneIndex = 0; sceneIndex < this.sceneSize; sceneIndex++) { // TODO Watch this.
                final SlotState slot = slotStates[trackState.getIndex()][sceneIndex];
                if (slot.calcState()) {
                    sendGridUpdate(slot);
                }
            }
        }
        updateArmState(trackState);
    }
    
    private void sendGridUpdate(final SlotState slot) {
        if (horizontalMode) {
            midiProcessor.sendGridButtonUpdate(
                slot.getTrackIndex(), slot.getSceneIndex(), slot.getState(), slot.getDisplayColor());
        } else {
            midiProcessor.sendGridButtonUpdate(
                slot.getSceneIndex(), slot.getTrackIndex(), slot.getState(), slot.getDisplayColor());
        }
    }
    
    private void updateArmState(final TrackState trackState) {
        if (isActive()) {
            midiProcessor.sendActionButtonUpdate(
                trackState.getIndex() + INDEX_OFFSET_ARM, 0,
                trackState.isArmed() ? 1 : 0);
        }
    }
    
    private void updateMuteState(final TrackState trackState) {
        if (isActive()) {
            midiProcessor.sendActionButtonUpdate(
                trackState.getIndex() + INDEX_OFFSET_MUTE, 0,
                trackState.isMuted() ? 1 : 0);
        }
    }
    
    private void updateSoloState(final TrackState trackState) {
        if (isActive()) {
            midiProcessor.sendActionButtonUpdate(
                trackState.getIndex() + INDEX_OFFSET_SOLO, 0,
                trackState.isSolo() ? 1 : 0);
        }
    }
    
    private void handleSendChange(final int trackIndex, final int sendIndex, final int value) {
        final int sliderIndex = (sendIndex + 1) * 12 + trackIndex;
        this.sliderValues[sliderIndex] = value;
        if (active && currentHeightMode == Mode.SENDS) {
            midiProcessor.sendSliderUpdate(sliderIndex, value);
        }
    }
    
    private void handleVolumeChange(final int trackIndex, final int value) {
        this.sliderValues[trackIndex] = value;
        if (active && currentHeightMode == Mode.VOLUME || currentHeightMode == Mode.SENDS) {
            midiProcessor.sendSliderUpdate(trackIndex, value);
        }
    }
    
    private Optional<Track> getTrack(final int index, final int offset) {
        final int trackIndex = index - offset;
        if (trackIndex >= 0 && trackIndex < this.trackSize) {
            return Optional.of(trackBank.getItemAt(trackIndex));
        }
        return Optional.empty();
    }
    
    public void handleGridButton(final int index, final boolean pressed) {
        if (index < INDEX_OFFSET_SOLO) {
            trackBank.sceneBank().getScene(index).launch();
        } else if (index < INDEX_OFFSET_MUTE) {
            getTrack(index, INDEX_OFFSET_SOLO).ifPresent(track -> track.solo().toggle());
        } else if (index < INDEX_OFFSET_ARM) {
            getTrack(index, INDEX_OFFSET_MUTE).ifPresent(track -> track.mute().toggle());
        } else if (index < 0x2D) {
            getTrack(index, INDEX_OFFSET_ARM).ifPresent(track -> track.arm().toggle());
        } else if (index == 0x3F) {
            rootTrack.stop();
        } else if (index == 0x5A) {
            Erae2ControllerExtension.println(" STOP Transport");
        } else if (index == 0x5B) {
            Erae2ControllerExtension.println(" PLAY Transport");
        } else if (index == 0x64) {
            if (horizontalMode) {
                trackBank.scrollBy(-1);
            } else {
                trackBank.sceneBank().scrollBy(-1);
            }
        } else if (index == 0x65) {
            if (horizontalMode) {
                trackBank.scrollBy(1);
            } else {
                trackBank.sceneBank().scrollBy(1);
            }
        } else if (index == 0x66) {
            if (horizontalMode) {
                trackBank.sceneBank().scrollBy(-1);
            } else {
                trackBank.scrollBy(-1);
            }
        } else if (index == 0x67) {
            if (horizontalMode) {
                trackBank.sceneBank().scrollBy(1);
            } else {
                trackBank.scrollBy(1);
            }
        }
    }
    
    public void handleGridButton(final int rowIndex, final int columnIndex, final boolean on) {
        if (horizontalMode) {
            if (rowIndex < trackSize && columnIndex < sceneSize) {
                final ClipLauncherSlot clip =
                    trackBank.getItemAt(rowIndex).clipLauncherSlotBank().getItemAt(columnIndex);
                clip.launch();
            }
        } else {
            if (rowIndex < sceneSize && columnIndex < trackSize) {
                final ClipLauncherSlot clip =
                    trackBank.getItemAt(columnIndex).clipLauncherSlotBank().getItemAt(rowIndex);
                clip.launch();
            }
        }
    }
    
    public void setCurrentHeightMode(final int height) {
        if (this.trackSize == 12) {
            switch (height) {
                case 1 -> this.currentHeightMode = Mode.SENDS;
                case 5 -> this.currentHeightMode = Mode.VOLUME;
                case INDEX_OFFSET_SOLO -> this.currentHeightMode = Mode.GRID;
            }
        } else {
            switch (height) {
                case 1 -> this.currentHeightMode = Mode.SENDS;
                case 3 -> this.currentHeightMode = Mode.VOLUME;
                case 12 -> this.currentHeightMode = Mode.GRID;
            }
        }
        this.gridHeight = height;
        // Erae2ControllerExtension.println(" MODE=%s  %d", this.currentHeightMode, height);
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(final boolean active) {
        this.active = active;
        trackBank.setShouldShowClipLauncherFeedback(active);
        this.overviewGrid.setScenePosition(sceneOffset);
    }
    
    private void triggerPadSlotUpdate(final SlotState slot) {
        final boolean changed = slot.calcState();
        if (horizontalMode) {
            if (isActive() && changed && slot.getTrackIndex() < gridHeight) {
                sendGridUpdate(slot);
            }
        } else {
            if (isActive() && changed && slot.getSceneIndex() < gridHeight) {
                sendGridUpdate(slot);
            }
        }
    }
    
    private void prepareSlot(final SlotState slot) {
        slot.getSlot().hasContent().addValueObserver(content -> {
            slot.setHasContent(content);
            triggerPadSlotUpdate(slot);
        });
        slot.getSlot().isPlaying().addValueObserver(playing -> {
            slot.setPlaying(playing);
            triggerPadSlotUpdate(slot);
        });
        slot.getSlot().isStopQueued().addValueObserver(stopQueued -> {
            slot.setStopQueued(stopQueued);
            triggerPadSlotUpdate(slot);
        });
        slot.getSlot().isRecordingQueued().addValueObserver(recordingQueued -> {
            slot.setRecordingQueued(recordingQueued);
            triggerPadSlotUpdate(slot);
        });
        slot.getSlot().isRecording().addValueObserver(recording -> {
            slot.setRecording(recording);
            triggerPadSlotUpdate(slot);
        });
        slot.getSlot().isPlaybackQueued().addValueObserver(playback -> {
            slot.setPlaybackQueued(playback);
            triggerPadSlotUpdate(slot);
        });
        slot.getSlot().color().addValueObserver((r, g, b) -> {
            final int colorIndex = EraeColor.rgbToIndex(r, g, b);
            slot.setColor(colorIndex);
            triggerPadSlotUpdate(slot);
        });
    }
    
    
    public void updateState() {
        for (int trackIndex = 0; trackIndex < trackSize; trackIndex++) {
            for (int sceneIndex = 0; sceneIndex < gridHeight; sceneIndex++) {
                final SlotState slot = slotStates[trackIndex][sceneIndex];
                sendGridUpdate(slotStates[trackIndex][sceneIndex]);
            }
            if (currentHeightMode == Mode.VOLUME || currentHeightMode == Mode.SENDS) {
                midiProcessor.sendSliderUpdate(trackIndex, sliderValues[trackIndex]);
            }
            if (currentHeightMode == Mode.SENDS) {
                midiProcessor.sendSliderUpdate(trackIndex + 0xC, sliderValues[trackIndex + 0xC]);
                midiProcessor.sendSliderUpdate(trackIndex + 0x18, sliderValues[trackIndex + 0x18]);
            }
            updateSoloState(trackStates[trackIndex]);
            updateMuteState(trackStates[trackIndex]);
            updateArmState(trackStates[trackIndex]);
        }
        for (int sceneIndex = 0; sceneIndex < gridHeight; sceneIndex++) {
            final SceneState state = sceneStates[sceneIndex];
            midiProcessor.sendActionButtonUpdate(sceneIndex, state.getState(), state.getDisplayColor());
        }
    }
    
    public void handleSliderValue(final int sliderIndex, final int value) {
        if (sliderValues[sliderIndex] != value) {
            sliderValues[sliderIndex] = value;
            final int groupIndex = sliderIndex / 12;
            final int paramIndex = sliderIndex % 12;
            final double val = (double) value / 127.0;
            if (groupIndex == 0) {
                trackBank.getItemAt(sliderIndex).volume().set(val);
            } else if (groupIndex == 1) {
                trackBank.getItemAt(paramIndex).sendBank().getItemAt(0).value().set(val);
            } else if (groupIndex == 2) {
                trackBank.getItemAt(paramIndex).sendBank().getItemAt(1).value().set(val);
            }
        }
    }
}
