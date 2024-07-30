package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.commonsmk3.AbstractLpSessionLayer;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.GridButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LaunchPadButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LaunchpadDeviceConfig;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.PanelLayout;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.commonsmk3.ViewCursorControl;
import com.bitwig.extensions.controllers.novation.launchpadmini3.LpMiniHwElements;
import com.bitwig.extensions.controllers.novation.launchpadmini3.LabelCcAssignmentsMini;
import com.bitwig.extensions.controllers.novation.launchpadmini3.LpMode;
import com.bitwig.extensions.controllers.novation.launchpadmini3.TrackMode;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.FocusSlot;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

public class SessionLayer extends AbstractLpSessionLayer {
    
    public static final int MOMENTARY_TIME = 500;
    private static final int MODE_INACTIVE_COLOR = 1;
    
    private final int[] sceneColorIndex = new int[8];
    private final int[] sceneColorHorizontal = new int[8];
    private final int[] sceneColorHorizontalInactive = new int[8];
    private final boolean[] selectionField = new boolean[8];
    
    private final TrackControlLayer verticalTrackControlLayer;
    private final TrackControlLayer horizontalTrackControlLayer;
    private final SceneControl sceneControlVertical;
    private final SceneControl sceneControlHorizontal;
    private TrackControlLayer currentTrackControlLayer;
    private SceneControl currentSceneControl;
    
    private final Map<ControlMode, AbstractSliderLayer> controlSliderLayers = new HashMap<>();
    private int sceneOffset;
    
    private LpMode lpMode = LpMode.SESSION;
    private ControlMode controlMode = ControlMode.NONE;
    private TrackMode trackMode = TrackMode.NONE;
    private PanelLayout panelLayout = PanelLayout.VERTICAL;
    
    private ControlMode stashedControlMode = ControlMode.NONE;
    private TrackMode stashedTrackMode = TrackMode.NONE;
    
    private final Layer sceneTrackControlLayer;
    private final Layer sceneControlHorizontalLayer;
    
    private final Layer verticalLayer;
    private final Layer horizontalLayer;
    
    private SendsSliderLayer sendsSliderLayer;
    
    @Inject
    private ViewCursorControl viewCursorControl;
    @Inject
    private MidiProcessor midiProcessor;
    @Inject
    private LaunchpadDeviceConfig config;
    @Inject
    private Transport transport;
    
    private LabeledButton modeButton = null;
    
    private boolean shiftHeld = false;
    
    private final List<TrackMode> miniModeSequenceXtra =
        List.of(TrackMode.NONE, TrackMode.STOP, TrackMode.SOLO, TrackMode.MUTE, TrackMode.CONTROL);
    
    public SessionLayer(final Layers layers, final Transport transport, final ControllerHost host) {
        super(layers);
        verticalLayer = new Layer(layers, "VERTICAL_LAUNCHING");
        horizontalLayer = new Layer(layers, "HORIZONTAL_LAUNCHING");
        
        sceneControlHorizontalLayer = new Layer(layers, "SCENE_CONTROL_DEFAULT");
        sceneTrackControlLayer = new Layer(layers, "SCENE_TRACK_CONTROL");
        
        sceneControlVertical = new SceneControl(this, layers);
        sceneControlHorizontal = new SceneControl(this, layers);
        
        verticalTrackControlLayer = new TrackControlLayer(layers, this, transport, host, PanelLayout.VERTICAL);
        horizontalTrackControlLayer = new TrackControlLayer(layers, this, transport, host, PanelLayout.HORIZONTAL);
        currentTrackControlLayer = verticalTrackControlLayer;
        currentSceneControl = sceneControlVertical;
    }
    
    @PostConstruct
    protected void init(final ControllerHost host, final Transport transport, final LpMiniHwElements hwElements) {
        clipLauncherOverdub = transport.isClipLauncherOverdubEnabled();
        clipLauncherOverdub.markInterested();
        
        final Clip cursorClip = viewCursorControl.getCursorClip();
        cursorClip.getLoopLength().markInterested();
        
        final TrackBank trackBank = viewCursorControl.getTrackBank();
        trackBank.setShouldShowClipLauncherFeedback(true);
        
        final SceneBank sceneBank = trackBank.sceneBank();
        final Scene targetScene = trackBank.sceneBank().getScene(0);
        targetScene.clipCount().markInterested();
        
        initClipControl(hwElements, trackBank);
        initNavigation(hwElements, trackBank, sceneBank);
        
        final int n = config.isMiniVersion() ? 7 : 8;
        for (int i = 0; i < n; i++) {
            final int index = i;
            final LabeledButton button = hwElements.getSceneLaunchButtons().get(i);
            final Track track = trackBank.getItemAt(i);
            track.addIsSelectedInMixerObserver(selectedInMixer -> selectionField[index] = selectedInMixer);
            button.bindPressed(sceneControlHorizontalLayer, pressed -> handleTrackSelect(pressed, track));
            button.bindLight(sceneControlHorizontalLayer, () -> getTrackColorSelect(index, track));
        }
        
        if (config.isMiniVersion()) {
            initTrackControlSceneButtons(hwElements, sceneTrackControlLayer);
        } else {
            initTrackControlXSceneButtons(hwElements, sceneTrackControlLayer);
        }
        
        initSceneControl(hwElements, sceneBank);
    }
    
    private void handleTrackSelect(final boolean pressed, final Track track) {
        if (!pressed) {
            return;
        }
        track.selectInMixer();
    }
    
    private RgbState getTrackColorSelect(final int index, final Track track) {
        if (track.exists().get()) {
            if (selectionField[index]) {
                return RgbState.WHITE;
            }
            return RgbState.DIM_WHITE;
        }
        return RgbState.OFF;
    }
    
    @Activate
    public void activation() {
        setIsActive(true);
        if (modeButton != null) {
            modeButton.refresh();
        }
    }
    
    @Override
    public void setLayout(final PanelLayout layout) {
        if (layout == panelLayout) {
            return;
        }
        panelLayout = layout;
        
        horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
        verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
        
        currentTrackControlLayer.reset();
        currentTrackControlLayer.deactivateLayer();
        currentTrackControlLayer =
            panelLayout == PanelLayout.VERTICAL ? verticalTrackControlLayer : horizontalTrackControlLayer;
        currentTrackControlLayer.applyMode(trackMode);
        currentTrackControlLayer.activateControlLayer(true);
        
        currentSceneControl.setActive(false);
        applyPanelModeToSceneControl();
        if (lpMode == LpMode.MIXER) {
            currentTrackControlLayer.applyMode(trackMode);
            currentTrackControlLayer.activateControlLayer(lpMode == LpMode.MIXER);
        }
    }
    
    private void applyPanelModeToSceneControl() {
        if (panelLayout == PanelLayout.VERTICAL) {
            currentSceneControl = sceneControlVertical;
            if (lpMode == LpMode.MIXER) {
                sceneTrackControlLayer.setIsActive(true);
                sceneControlHorizontalLayer.setIsActive(false);
                currentSceneControl.setActive(false);
            } else {
                sceneTrackControlLayer.setIsActive(false);
                sceneControlHorizontalLayer.setIsActive(false);
                currentSceneControl.setActive(true);
            }
        } else {
            currentSceneControl = sceneControlHorizontal;
            if (lpMode == LpMode.MIXER) {
                sceneTrackControlLayer.setIsActive(true);
                sceneControlHorizontalLayer.setIsActive(false);
                currentSceneControl.setActive(true);
            } else {
                sceneTrackControlLayer.setIsActive(false);
                sceneControlHorizontalLayer.setIsActive(true);
                currentSceneControl.setActive(true);
            }
        }
    }
    
    public void setShiftHeld(final boolean value) {
        this.shiftHeld = value;
    }
    
    public boolean isShiftHeld() {
        return shiftHeld;
    }
    
    public void registerControlLayer(final ControlMode controlMode, final AbstractSliderLayer sliderLayer) {
        controlSliderLayers.put(controlMode, sliderLayer);
        if (controlMode == ControlMode.SENDS && sliderLayer instanceof SendsSliderLayer) {
            sendsSliderLayer = (SendsSliderLayer) sliderLayer;
            sendsSliderLayer.addControlModeRemoveListener(this::sendRemoved);
        }
    }
    
    private void initNavigation(final LpMiniHwElements hwElements, final TrackBank trackBank,
        final SceneBank sceneBank) {
        final LabeledButton upButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.UP);
        final LabeledButton downButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.DOWN);
        final LabeledButton leftButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.LEFT);
        final LabeledButton rightButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.RIGHT);
        sceneBank.canScrollForwards().markInterested();
        sceneBank.canScrollBackwards().markInterested();
        trackBank.canScrollForwards().markInterested();
        trackBank.canScrollBackwards().markInterested();
        final RgbState baseColor = RgbState.of(1);
        final RgbState pressedColor = RgbState.of(3);
        
        downButton.bindRepeatHold(verticalLayer, () -> sceneBank.scrollBy(1));
        downButton.bindHighlightButton(verticalLayer, sceneBank.canScrollForwards(), baseColor, pressedColor);
        
        upButton.bindRepeatHold(verticalLayer, () -> sceneBank.scrollBy(-1));
        upButton.bindHighlightButton(verticalLayer, sceneBank.canScrollBackwards(), baseColor, pressedColor);
        
        leftButton.bindRepeatHold(verticalLayer, () -> trackBank.scrollBy(-1));
        leftButton.bindHighlightButton(verticalLayer, trackBank.canScrollBackwards(), baseColor, pressedColor);
        
        rightButton.bindRepeatHold(verticalLayer, () -> trackBank.scrollBy(1));
        rightButton.bindHighlightButton(verticalLayer, trackBank.canScrollForwards(), baseColor, pressedColor);
        
        
        downButton.bindRepeatHold(horizontalLayer, () -> trackBank.scrollBy(1));
        downButton.bindHighlightButton(horizontalLayer, trackBank.canScrollForwards(), baseColor, pressedColor);
        
        upButton.bindRepeatHold(horizontalLayer, () -> trackBank.scrollBy(-1));
        upButton.bindHighlightButton(horizontalLayer, trackBank.canScrollBackwards(), baseColor, pressedColor);
        
        leftButton.bindRepeatHold(horizontalLayer, () -> sceneBank.scrollBy(-1));
        leftButton.bindHighlightButton(horizontalLayer, sceneBank.canScrollBackwards(), baseColor, pressedColor);
        
        rightButton.bindRepeatHold(horizontalLayer, () -> sceneBank.scrollBy(1));
        rightButton.bindHighlightButton(horizontalLayer, sceneBank.canScrollForwards(), baseColor, pressedColor);
        
    }
    
    private void initClipControl(final LpMiniHwElements hwElements, final TrackBank trackBank) {
        for (int i = 0; i < 8; i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            markTrack(track);
            for (int j = 0; j < 8; j++) {
                final int sceneIndex = j;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                prepareSlot(slot, sceneIndex, trackIndex);
                final GridButton button = hwElements.getGridButton(sceneIndex, trackIndex);
                button.bindPressed(verticalLayer, pressed -> handleSlot(pressed, slot));
                button.bindLight(verticalLayer, () -> getState(track, slot, trackIndex, sceneIndex));
                final GridButton buttonHorizontal = hwElements.getGridButton(trackIndex, sceneIndex);
                buttonHorizontal.bindPressed(horizontalLayer, pressed -> handleSlot(pressed, slot));
                buttonHorizontal.bindLight(horizontalLayer, () -> getState(track, slot, trackIndex, sceneIndex));
            }
        }
        
        verticalTrackControlLayer.initClipControl(hwElements, trackBank);
        verticalTrackControlLayer.initControlLayer(hwElements, viewCursorControl);
        horizontalTrackControlLayer.initClipControl(hwElements, trackBank);
        horizontalTrackControlLayer.initControlLayer(hwElements, viewCursorControl);
    }
    
    private void initSceneControl(final LpMiniHwElements hwElements, final SceneBank sceneBank) {
        sceneBank.setIndication(true);
        sceneBank.scrollPosition().addValueObserver(value -> sceneOffset = value);
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Scene scene = sceneBank.getScene(index);
            scene.color().addValueObserver((r, g, b) -> {
                sceneColorIndex[index] = ColorLookup.toColor(r, g, b);
                sceneColorHorizontal[index] = adjustHorizontal(sceneColorIndex[index]);
                sceneColorHorizontalInactive[index] = darkenHorizontal(sceneColorIndex[index]);
            });
        }
        
        final List<LaunchPadButton> sceneButtonsVertical = new ArrayList<>();
        final int n = config.isMiniVersion() ? 7 : 8;
        for (int i = 0; i < n; i++) {
            sceneButtonsVertical.add(hwElements.getSceneLaunchButtons().get(i));
        }
        sceneControlVertical.initSceneControl(sceneBank, sceneButtonsVertical);
        final List<LaunchPadButton> sceneButtonsHorizontal = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            sceneButtonsHorizontal.add(hwElements.getGridButton(7, i));
        }
        sceneControlHorizontal.initSceneControl(sceneBank, sceneButtonsHorizontal);
        
        if (config.isMiniVersion()) {
            modeButton = hwElements.getSceneLaunchButtons().get(7);
            modeButton.bindPressed(sceneControlVertical.getLayer(), this::changeModeMini);
            modeButton.bindLight(sceneControlVertical.getLayer(), () -> RgbState.of(trackMode.getColorIndex()));
            
            modeButton.bindPressed(sceneControlHorizontalLayer, this::changeModeMini);
            modeButton.bindLight(sceneControlHorizontalLayer, () -> RgbState.of(trackMode.getColorIndex()));
        }
    }
    
    private static int darkenHorizontal(final int colorIndex) {
        if (colorIndex < 4) {
            return 1;
        }
        return colorIndex + 2;
    }
    
    public static int adjustHorizontal(final int colorIndex) {
        if (colorIndex == 0) {
            return 0;
        }
        if (colorIndex == 1) {
            return 3;
        }
        return colorIndex;
    }
    
    private void initTrackControlXSceneButtons(final LpMiniHwElements hwElements, final Layer layer) {
        initVolumeControl(hwElements, layer, 0);
        initPanControl(hwElements, layer, 1);
        initSendsAControl(hwElements, layer, 2);
        initSendsBControl(hwElements, layer, 3);
        initStopControl(hwElements, layer, 4);
        initMuteControl(hwElements, layer, 5);
        initSoloControl(hwElements, layer, 6);
        initArmControl(hwElements, layer, 7);
    }
    
    private void initTrackControlSceneButtons(final LpMiniHwElements hwElements, final Layer layer) {
        initVolumeControl(hwElements, layer, 0);
        initPanControl(hwElements, layer, 1);
        initSendsAControl(hwElements, layer, 2);
        initSendsBControl(hwElements, layer, 3);
        initDeviceControl(hwElements, layer, 4);
        initStopControl(hwElements, layer, 5);
        initMuteControl(hwElements, layer, 6);
        initSoloControl(hwElements, layer, 7);
    }
    
    private void initVolumeControl(final LpMiniHwElements hwElements, final Layer layer, final int index) {
        final LabeledButton volumeButton = hwElements.getSceneLaunchButtons().get(index);
        volumeButton.bindPressReleaseAfter(this, () -> intoControlMode(ControlMode.VOLUME), this::returnToPreviousMode,
            MOMENTARY_TIME);
        volumeButton.bindLight(layer,
            () -> controlMode == ControlMode.VOLUME ? RgbState.of(9) : RgbState.of(MODE_INACTIVE_COLOR));
    }
    
    private void initPanControl(final LpMiniHwElements hwElements, final Layer layer, final int index) {
        final LabeledButton panButton = hwElements.getSceneLaunchButtons().get(index);
        panButton.bindPressReleaseAfter(this, () -> intoControlMode(ControlMode.PAN), this::returnToPreviousMode,
            MOMENTARY_TIME);
        panButton.bindLight(layer,
            () -> controlMode == ControlMode.PAN ? RgbState.of(9) : RgbState.of(MODE_INACTIVE_COLOR));
    }
    
    private void initSendsAControl(final LpMiniHwElements hwElements, final Layer layer, final int index) {
        final LabeledButton sendsAButton = hwElements.getSceneLaunchButtons().get(index);
        sendsAButton.bindPressReleaseAfter(this, () -> intoControlMode(ControlMode.SENDS_A), this::returnToPreviousMode,
            MOMENTARY_TIME);
        sendsAButton.bindLight(layer, () -> getSendsState(ControlMode.SENDS_A));
    }
    
    private void initSendsBControl(final LpMiniHwElements hwElements, final Layer layer, final int index) {
        final LabeledButton sendsBButton = hwElements.getSceneLaunchButtons().get(index);
        sendsBButton.bindPressReleaseAfter(this, () -> intoControlMode(ControlMode.SENDS_B), this::returnToPreviousMode,
            MOMENTARY_TIME);
        sendsBButton.bindLight(layer, () -> getSendsState(ControlMode.SENDS_B));
    }
    
    private void initDeviceControl(final LpMiniHwElements hwElements, final Layer layer, final int index) {
        final LabeledButton deviceButton = hwElements.getSceneLaunchButtons().get(index);
        deviceButton.bindPressReleaseAfter(this, () -> intoControlMode(ControlMode.DEVICE), this::returnToPreviousMode,
            MOMENTARY_TIME);
        deviceButton.bindLight(layer,
            () -> controlMode == ControlMode.DEVICE ? RgbState.of(33) : RgbState.of(MODE_INACTIVE_COLOR));
    }
    
    private void initStopControl(final LpMiniHwElements hwElements, final Layer layer, final int index) {
        final LabeledButton stopButton = hwElements.getSceneLaunchButtons().get(index);
        stopButton.bindPressReleaseAfter(this, () -> intoTrackMode(TrackMode.STOP), this::returnToPreviousMode,
            MOMENTARY_TIME);
        stopButton.bindLight(layer,
            () -> trackMode == TrackMode.STOP ? RgbState.of(5) : RgbState.of(MODE_INACTIVE_COLOR));
    }
    
    private void initMuteControl(final LpMiniHwElements hwElements, final Layer layer, final int index) {
        final LabeledButton muteButton = hwElements.getSceneLaunchButtons().get(index);
        muteButton.bindPressReleaseAfter(this, () -> intoTrackMode(TrackMode.MUTE), this::returnToPreviousMode,
            MOMENTARY_TIME);
        muteButton.bindLight(layer,
            () -> trackMode == TrackMode.MUTE ? RgbState.of(9) : RgbState.of(MODE_INACTIVE_COLOR));
    }
    
    private void initSoloControl(final LpMiniHwElements hwElements, final Layer layer, final int index) {
        final LabeledButton soloButton = hwElements.getSceneLaunchButtons().get(index);
        soloButton.bindPressReleaseAfter(this, () -> intoTrackMode(TrackMode.SOLO), this::returnToPreviousMode,
            MOMENTARY_TIME);
        soloButton.bindLight(layer,
            () -> trackMode == TrackMode.SOLO ? RgbState.of(13) : RgbState.of(MODE_INACTIVE_COLOR));
    }
    
    private void initArmControl(final LpMiniHwElements hwElements, final Layer layer, final int index) {
        final LabeledButton soloButton = hwElements.getSceneLaunchButtons().get(index);
        soloButton.bindPressReleaseAfter(this, () -> intoTrackMode(TrackMode.ARM), this::returnToPreviousMode,
            MOMENTARY_TIME);
        soloButton.bindLight(
            layer, () -> trackMode == TrackMode.ARM ? RgbState.of(5) : RgbState.of(MODE_INACTIVE_COLOR));
    }
    
    private void sendRemoved(final ControlMode modeRemoved) {
        if (controlMode == modeRemoved) {
            final AbstractSliderLayer currentMode = controlSliderLayers.get(controlMode.getRefMode());
            if (currentMode != null) {
                currentMode.setIsActive(false);
            }
            controlMode = ControlMode.NONE;
        }
    }
    
    public RgbState getSendsState(final ControlMode mode) {
        if (sendsSliderLayer.canBeEntered(mode)) {
            return controlMode == mode ? RgbState.of(13) : RgbState.of(MODE_INACTIVE_COLOR);
        }
        return RgbState.OFF;
    }
    
    private void intoTrackMode(final TrackMode mode) {
        if (controlMode != ControlMode.NONE) {
            switchToMode(ControlMode.NONE);
        }
        if (trackMode == mode) {
            trackMode = TrackMode.NONE;
        } else {
            trackMode = mode;
            verticalTrackControlLayer.resetCounts();
        }
        currentTrackControlLayer.applyMode(trackMode);
    }
    
    public void intoControlMode(final ControlMode mode) {
        final AbstractSliderLayer currentMode = controlSliderLayers.get(controlMode.getRefMode());
        final AbstractSliderLayer modeLayer = controlSliderLayers.get(mode.getRefMode());
        if (modeLayer != null && !modeLayer.canBeEntered(mode)) {
            return;
        }
        if (currentMode != null) {
            currentMode.setIsActive(false);
        }
        if (controlMode == mode) {
            controlMode = ControlMode.NONE;
            midiProcessor.toLayout(0x00);
        } else {
            controlMode = mode;
            if (modeLayer != null) {
                modeLayer.setIsActive(true);
                if (modeLayer instanceof SendsSliderLayer sendsSliderLayer) {
                    sendsSliderLayer.setControl(mode);
                }
            }
        }
        trackMode = TrackMode.NONE;
        currentTrackControlLayer.applyMode(trackMode);
    }
    
    public void returnToPreviousMode(final boolean longPress) {
        if (longPress) {
            if (stashedControlMode != controlMode) {
                switchToMode(stashedControlMode);
            }
            if (stashedTrackMode != trackMode) {
                trackMode = stashedTrackMode;
                currentTrackControlLayer.applyMode(trackMode);
            }
        } else {
            stashedControlMode = controlMode;
            stashedTrackMode = trackMode;
        }
    }
    
    private void switchToMode(final ControlMode newMode) {
        final AbstractSliderLayer currentMode = controlSliderLayers.get(controlMode.getRefMode());
        final AbstractSliderLayer modeLayer = controlSliderLayers.get(newMode.getRefMode());
        if (modeLayer != null && !modeLayer.canBeEntered(newMode)) {
            return;
        }
        if (currentMode != null) {
            currentMode.setIsActive(false);
        }
        if (modeLayer instanceof final SendsSliderLayer sendingLayer) {
            sendingLayer.setControl(newMode);
        }
        if (modeLayer != null) {
            modeLayer.setIsActive(true);
        }
        if (newMode == ControlMode.NONE && controlMode != ControlMode.NONE) {
            midiProcessor.toLayout(0x00);
        }
        controlMode = newMode;
    }
    
    public void setMode(final LpMode lpMode) {
        this.lpMode = lpMode;
        applyPanelModeToSceneControl();
        final AbstractSliderLayer currentSliderMode = controlSliderLayers.get(controlMode.getRefMode());
        if (currentSliderMode != null) {
            currentSliderMode.setIsActive(lpMode != LpMode.SESSION);
        }
        currentTrackControlLayer.activateControlLayer(lpMode == LpMode.MIXER);
        if (lpMode == LpMode.MIXER) {
            currentTrackControlLayer.applyMode(trackMode);
        }
    }
    
    private void changeModeMini() { // change to sequence
        trackMode = getNextMode(miniModeSequenceXtra);
        currentTrackControlLayer.applyMode(trackMode);
    }
    
    private TrackMode getNextMode(final List<TrackMode> sequence) {
        final int index = sequence.indexOf(trackMode);
        if (index == -1) {
            return sequence.get(0);
        }
        return sequence.get((index + 1) % sequence.size());
    }
    
    private void markTrack(final Track track) {
        track.exists().markInterested();
        track.isStopped().markInterested();
        track.mute().markInterested();
        track.solo().markInterested();
        track.isQueuedForStop().markInterested();
        track.arm().markInterested();
    }
    
    private void prepareSlot(final ClipLauncherSlot slot, final int sceneIndex, final int trackIndex) {
        slot.hasContent().markInterested();
        slot.isPlaying().markInterested();
        slot.isStopQueued().markInterested();
        slot.isRecordingQueued().markInterested();
        slot.isRecording().markInterested();
        slot.isPlaybackQueued().markInterested();
        slot.color().addValueObserver((r, g, b) -> colorIndex[sceneIndex][trackIndex] = ColorLookup.toColor(r, g, b));
    }
    
    private void handleSlot(final boolean pressed, final ClipLauncherSlot slot) {
        if (pressed) {
            if (shiftHeld) {
                slot.launchAlt();
            } else {
                slot.launch();
            }
        } else {
            if (shiftHeld) {
                slot.launchReleaseAlt();
            } else {
                slot.launchRelease();
            }
        }
    }
    
    void handleScene(final boolean pressed, final Scene scene, final int sceneIndex) {
        if (pressed) {
            if (shiftHeld) {
                scene.launchAlt();
            } else {
                scene.launch();
            }
        } else {
            if (shiftHeld) {
                scene.launchReleaseAlt();
            } else {
                scene.launchRelease();
            }
        }
    }
    
    RgbState getSceneColorVertical(final int sceneIndex, final Scene scene) {
        if (scene.clipCount().get() > 0) {
            if (viewCursorControl.hasQueuedForPlaying(sceneOffset + sceneIndex)) {
                return RgbState.GREEN_FLASH;
            }
            return RgbState.of(sceneColorIndex[sceneIndex]);
        }
        return RgbState.OFF;
    }
    
    RgbState getSceneColorHorizontal(final int sceneIndex, final Scene scene) {
        if (scene.clipCount().get() > 0) {
            if (viewCursorControl.hasQueuedForPlaying(sceneOffset + sceneIndex)) {
                return RgbState.GREEN_FLASH;
            }
            return RgbState.of(sceneColorHorizontal[sceneIndex]);
        }
        return RgbState.of(sceneColorHorizontalInactive[sceneIndex]);
    }
    
    public RgbState getRecordButtonColorRegular() {
        final FocusSlot focusSlot = viewCursorControl.getFocusSlot();
        if (focusSlot != null) {
            final ClipLauncherSlot slot = focusSlot.getSlot();
            if (slot.isRecordingQueued().get()) {
                return RgbState.flash(5, 0);
            }
            if (slot.isRecording().get() || slot.isRecordingQueued().get()) {
                return RgbState.pulse(5);
            }
        }
        if (transport.isClipLauncherOverdubEnabled().get()) {
            return RgbState.RED;
        } else {
            return RgbState.of(7);
        }
    }
    
    
    @Override
    protected void onActivate() {
        super.onActivate();
        applyPanelModeToSceneControl();
        horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
        verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
        currentTrackControlLayer.activateControlLayer(lpMode == LpMode.MIXER);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        currentTrackControlLayer.deactivateLayer();
        currentSceneControl.setActive(false);
        sceneTrackControlLayer.setIsActive(false);
        horizontalLayer.setIsActive(false);
        verticalLayer.setIsActive(false);
    }
    
    
}
