package com.bitwig.extensions.controllers.neuzeitinstruments;

import java.util.Arrays;
import java.util.List;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.neuzeitinstruments.controls.DropButton;
import com.bitwig.extensions.controllers.neuzeitinstruments.controls.DropColorButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.PanelLayout;

@Component
public class LauncherLayer extends Layer {
    
    private static final String DROP_SCENE_LAUNCH_OPTION = "default";
    private static final String DROP_SCENE_LAUNCH_QUANT = "1/4";
    
    private final Layer launcherLayout;
    private final Layer arrangerLayout;
    private final DropMidiProcessor midiProcessor;
    private final DropViewControl viewControl;
    private final HwElements hwElements;
    protected final DropColor[][] launcherColorIndex = new DropColor[4][4];
    protected final DropColor[][] arrangerColorIndex = new DropColor[5][3];
    protected final DropColor[] sceneColors = new DropColor[4];
    private PanelLayout panelLayout = PanelLayout.LAUNCHER;
    private int sceneOffset;
    
    public LauncherLayer(final Layers layers, final DropViewControl viewControl, final HwElements hwElements,
        final Application application, final DropMidiProcessor midiProcessor) {
        super(layers, "LAUNCHER");
        this.midiProcessor = midiProcessor;
        this.viewControl = viewControl;
        this.hwElements = hwElements;
        application.panelLayout().addValueObserver(this::handlePanelLayoutChanged);
        midiProcessor.addDropRequestListener(this::handleDropRequest);
        
        launcherLayout = new Layer(layers, "LAUNCHER");
        arrangerLayout = new Layer(layers, "ARRANGER");
        final TrackBank launcherTrackBank = viewControl.getLauncherTrackBank();
        launcherTrackBank.sceneBank().scrollPosition().addValueObserver(value -> sceneOffset = value);
        for (int i = 0; i < 4; i++) {
            Arrays.fill(launcherColorIndex[i], DropColor.OFF);
        }
        for (int i = 0; i < 5; i++) {
            Arrays.fill(arrangerColorIndex[i], DropColor.OFF);
        }
        final List<DropColorButton> layer1Buttons = hwElements.getLayer1Buttons();
        initLauncherClipLaunch(layer1Buttons, viewControl.getLauncherTrackBank());
        initArrangerClipLaunch(layer1Buttons, viewControl.getArrangerTrackBank());
        
        initSceneLauncherClipLauncher(
            hwElements.getLayer2Buttons(), viewControl.getRootTrack(),
            viewControl.getLauncherTrackBank());
        initSceneLauncherArrangerLauncher(
            hwElements.getLayer2Buttons(), viewControl.getRootTrack(), viewControl.getArrangerTrackBank());
        initNavigation(hwElements);
    }
    
    private void handleDropRequest(final int value) {
        if (value == 1) {
            midiProcessor.setLayoutMode(panelLayout == PanelLayout.LAUNCHER ? 1 : 2);
            hwElements.fullButtonUpdate();
        }
    }
    
    private void initLauncherClipLaunch(final List<DropColorButton> buttonList, final TrackBank trackBank) {
        for (int i = 0; i < 16; i++) {
            final int trackIndex = i % 4;
            final int sceneIndex = i / 4;
            final DropColorButton button = buttonList.get(i);
            final Track track = trackBank.getItemAt(trackIndex);
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
            prepareSlot(slot);
            slot.color()
                .addValueObserver((r, g, b) -> launcherColorIndex[trackIndex][sceneIndex] = DropColor.fromRgb(r, g, b));
            
            button.bindPressed(launcherLayout, () -> handleSlotPressed(slot));
            button.bindRelease(arrangerLayout, () -> handleSlotReleased(slot));
            button.bindLight(launcherLayout, () -> getState(track, slot, trackIndex, sceneIndex, launcherColorIndex));
        }
        for (int i = 0; i < 4; i++) {
            final DropColorButton launcherButton = buttonList.get(i + 16);
            final Track track = trackBank.getItemAt(i);
            launcherButton.bindLight(launcherLayout, () -> handleTrackStopColor(track));
            launcherButton.bindPressed(launcherLayout, track::stop);
        }
    }
    
    private void initArrangerClipLaunch(final List<DropColorButton> buttonList, final TrackBank trackBank) {
        for (int i = 0; i < 15; i++) {
            final int trackIndex = i / 3;
            final int slotIndex = i % 3;
            final Track track = trackBank.getItemAt(trackIndex);
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(slotIndex);
            prepareSlot(slot);
            slot.color()
                .addValueObserver((r, g, b) -> arrangerColorIndex[trackIndex][slotIndex] = DropColor.fromRgb(r, g, b));
            final DropColorButton button = buttonList.get(trackIndex * 4 + slotIndex + 1);
            button.bindPressed(arrangerLayout, () -> handleSlotPressed(slot));
            button.bindRelease(arrangerLayout, () -> handleSlotReleased(slot));
            button.bindLight(arrangerLayout, () -> getState(track, slot, trackIndex, slotIndex, arrangerColorIndex));
        }
        for (int i = 0; i < 5; i++) {
            final DropColorButton button = buttonList.get(i * 4);
            final Track track = trackBank.getItemAt(i);
            button.bindLight(arrangerLayout, () -> handleTrackStopColor(track));
            button.bindPressed(arrangerLayout, track::stop);
        }
    }
    
    private void initSceneLauncherClipLauncher(final List<DropColorButton> buttons, final Track rootTrack,
        final TrackBank trackBank) {
        for (int i = 0; i < trackBank.sceneBank().getSizeOfBank(); i++) {
            final int sceneIndex = i;
            trackBank.sceneBank().getItemAt(i).color()
                .addValueObserver((r, g, b) -> sceneColors[sceneIndex] = DropColor.fromRgb(r, g, b, DropColor.GREEN));
        }
        for (int i = 0; i < 4; i++) {
            final int sceneIndex = i;
            final Scene scene = trackBank.sceneBank().getItemAt(i);
            final DropColorButton directButtonLauncher = buttons.get(i * 4 + 1);
            final DropColorButton dropButtonLauncher = buttons.get(i * 4);
            
            directButtonLauncher.bindPressed(launcherLayout, scene::launch);
            directButtonLauncher.bindRelease(launcherLayout, scene::launchRelease);
            directButtonLauncher.bindLight(launcherLayout, () -> getSceneColor(scene, sceneIndex));
            dropButtonLauncher.bindLight(launcherLayout, () -> DropColor.RED);
            buttons.get(i * 4 + 2).bindPressed(launcherLayout, () -> {});
            buttons.get(i * 4 + 2).bindLight(launcherLayout, () -> DropColor.OFF);
            buttons.get(i * 4 + 3).bindLight(launcherLayout, () -> DropColor.OFF);
            buttons.get(i * 4 + 3).bindPressed(launcherLayout, () -> {});
        }
        for (int i = 0; i < 4; i++) {
            final DropColorButton button = buttons.get(16 + i);
            if (i == 0) {
                button.bindPressed(launcherLayout, rootTrack::stop);
                button.bindLight(launcherLayout, () -> getStopStateColor(rootTrack));
            } else {
                button.bindLight(launcherLayout, () -> DropColor.OFF);
            }
        }
    }
    
    private void initSceneLauncherArrangerLauncher(final List<DropColorButton> buttons, final Track rootTrack,
        final TrackBank trackBank) {
        final SceneBank sceneBank = trackBank.sceneBank();
        for (int i = 0; i < trackBank.sceneBank().getSizeOfBank(); i++) {
            trackBank.sceneBank().getItemAt(i).exists().markInterested();
        }
        for (int i = 0; i < 3; i++) {
            final int sceneIndex = i;
            final Scene scene = sceneBank.getItemAt(i);
            final DropColorButton dropButtonLauncher = buttons.get(4 + i + 1);
            final DropColorButton directLaunchButton = buttons.get(i + 1);
            directLaunchButton.bindLight(arrangerLayout, () -> getSceneColor(scene, sceneIndex));
            directLaunchButton.bindPressed(arrangerLayout, scene::launch);
            directLaunchButton.bindPressed(arrangerLayout, scene::launchRelease);
            dropButtonLauncher.bindLight(arrangerLayout, () -> DropColor.RED);
            dropButtonLauncher.bindPressed(arrangerLayout, () -> {});
            dropButtonLauncher.bindPressed(arrangerLayout, () -> {});
        }
        for (int row = 2; row < 5; row++) {
            for (int col = 1; col < 4; col++) {
                final DropColorButton button = buttons.get(row * 4 + col);
                button.bindLight(arrangerLayout, () -> DropColor.OFF);
            }
        }
        
        for (int i = 0; i < 5; i++) {
            final DropColorButton button = buttons.get(i * 4);
            if (i == 0) {
                button.bindPressed(arrangerLayout, rootTrack::stop);
                button.bindLight(arrangerLayout, () -> getStopStateColor(rootTrack));
            } else {
                button.bindLight(arrangerLayout, () -> DropColor.OFF);
            }
        }
    }
    
    private static DropColor getStopStateColor(final Track track) {
        return track.isQueuedForStop().get() ? DropColor.WHITE.triggered() : DropColor.WHITE;
    }
    
    private DropColor getSceneColor(final Scene scene, final int index) {
        if (!scene.exists().get()) {
            return DropColor.OFF;
        }
        if (viewControl.hasQueuedClips(sceneOffset + index)) {
            return sceneColors[index].triggered();
        }
        if (viewControl.hasPlayingClips(sceneOffset + index)) {
            return sceneColors[index].playing();
        }
        
        return sceneColors[index];
    }
    
    private void initNavigation(final HwElements hwElements) {
        final DropButton navLeftButton = hwElements.getGridLeftButton();
        final DropButton navRightButton = hwElements.getGridRightButton();
        final DropButton navUpButton = hwElements.getGridUpButton();
        final DropButton navDownButton = hwElements.getGridDownButton();
        
        navLeftButton.bindRepeatHold(launcherLayout, () -> navigateTracks(-1));
        navRightButton.bindRepeatHold(launcherLayout, () -> navigateTracks(1));
        navUpButton.bindRepeatHold(launcherLayout, () -> navigateScenes(-1));
        navDownButton.bindRepeatHold(launcherLayout, () -> navigateScenes(1));
        
        navLeftButton.bindRepeatHold(arrangerLayout, () -> navigateScenes(-1));
        navRightButton.bindRepeatHold(arrangerLayout, () -> navigateScenes(1));
        navUpButton.bindRepeatHold(arrangerLayout, () -> navigateTracks(-1));
        navDownButton.bindRepeatHold(arrangerLayout, () -> navigateTracks(1));
    }
    
    private void navigateTracks(final int dir) {
        if (dir < 0) {
            viewControl.getLauncherTrackBank().scrollBackwards();
            viewControl.getArrangerTrackBank().scrollBackwards();
        } else {
            viewControl.getLauncherTrackBank().scrollForwards();
            viewControl.getArrangerTrackBank().scrollForwards();
        }
    }
    
    private void navigateScenes(final int dir) {
        if (dir < 0) {
            viewControl.getLauncherTrackBank().sceneBank().scrollBackwards();
            viewControl.getArrangerTrackBank().sceneBank().scrollBackwards();
        } else {
            viewControl.getLauncherTrackBank().sceneBank().scrollForwards();
            viewControl.getArrangerTrackBank().sceneBank().scrollForwards();
        }
    }
    
    private static DropColor handleTrackStopColor(final Track track) {
        if (track.isQueuedForStop().get()) {
            return DropColor.WHITE.triggered();
        }
        return DropColor.WHITE;
    }
    
    protected DropColor getState(final Track track, final ClipLauncherSlot slot, final int trackIndex,
        final int sceneIndex, final DropColor[][] colorTable) {
        if (slot.hasContent().get()) {
            final DropColor color = colorTable[trackIndex][sceneIndex];
            if (slot.isRecordingQueued().get()) {
                return color.recording();
            } else if (slot.isRecording().get()) {
                return color.recording();
            } else if (slot.isPlaybackQueued().get()) {
                return color.triggered();
            } else if (slot.isStopQueued().get()) {
                return color.triggered();
            } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
                return color.triggered();
            } else if (slot.isPlaying().get()) {
                return color.playing();
            }
            return color;
        }
        if (slot.isRecordingQueued().get()) {
            return DropColor.RED.triggered();
        }
        return DropColor.OFF;
    }
    
    private void handlePanelLayoutChanged(final String value) {
        if (value.equals("MIX")) {
            this.panelLayout = PanelLayout.LAUNCHER;
        } else {
            this.panelLayout = PanelLayout.ARRANGER;
        }
        midiProcessor.setLayoutMode(panelLayout == PanelLayout.LAUNCHER ? 1 : 2);
        launcherLayout.setIsActive(panelLayout == PanelLayout.LAUNCHER);
        arrangerLayout.setIsActive(panelLayout == PanelLayout.ARRANGER);
        viewControl.getArrangerTrackBank().setShouldShowClipLauncherFeedback(panelLayout == PanelLayout.ARRANGER);
        viewControl.getLauncherTrackBank().setShouldShowClipLauncherFeedback(panelLayout == PanelLayout.LAUNCHER);
    }
    
    protected void prepareSlot(final ClipLauncherSlot slot) {
        slot.hasContent().markInterested();
        slot.isPlaying().markInterested();
        slot.isStopQueued().markInterested();
        slot.isRecordingQueued().markInterested();
        slot.isRecording().markInterested();
        slot.isPlaybackQueued().markInterested();
        slot.isSelected().markInterested();
    }
    
    private void handleSlotPressed(final ClipLauncherSlot slot) {
        slot.launch();
    }
    
    private void handleSlotReleased(final ClipLauncherSlot slot) {
        slot.launchRelease();
    }
    
    @Activate
    public void init() {
        this.setIsActive(true);
        launcherLayout.setIsActive(panelLayout == PanelLayout.LAUNCHER);
        arrangerLayout.setIsActive(panelLayout == PanelLayout.ARRANGER);
    }
}
