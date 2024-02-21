package com.bitwig.extensions.controllers.akai.apc64.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apc.common.PanelLayout;
import com.bitwig.extensions.controllers.akai.apc.common.control.RgbButton;
import com.bitwig.extensions.controllers.akai.apc.common.led.LedBehavior;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apc.common.led.VarSingleLedState;
import com.bitwig.extensions.controllers.akai.apc64.ApcPreferences;
import com.bitwig.extensions.controllers.akai.apc64.HardwareElements;
import com.bitwig.extensions.controllers.akai.apc64.ModifierStates;
import com.bitwig.extensions.controllers.akai.apc64.ViewControl;
import com.bitwig.extensions.controllers.akai.apc64.control.SingleLedButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

public class TrackAndSceneLayer extends Layer {
    private final static String[] LAUNCH_QUANTIZE_VALUES = {"8", "4", "2", "1", "1/4", "1/8", "1/16", "1/2"};
    private final static String[] LAUNCH_QUANTIZE_DISPLAY_VALUES = {"8 Bars", "4 Bars", "2 Bars", "1 Bar", "1/4", "1" + "/8", "1/16", "1/2"};

    @Inject
    private ViewControl viewControl;
    @Inject
    private ModifierStates modifiers;
    @Inject
    private Transport transport;
    @Inject
    private MainDisplay mainDisplay;

    private final Layer horizontalLayer;
    private final Layer verticalLayer;
    private final Layer shiftLayer;

    private final Track rootTrack;
    private int sceneOffset;
    private TrackBank trackBank;
    private PanelLayout panelLayout;
    private final ApcPreferences preferences;

    public TrackAndSceneLayer(final Layers layers, final ApcPreferences preferences, final Project project) {
        super(layers, "TRACKS_AND_SCENES");
        this.horizontalLayer = new Layer(layers, "HORIZONTAL_LAYER");
        this.verticalLayer = new Layer(layers, "VERTICAL_LAYER");
        this.shiftLayer = new Layer(layers, "TRACK_SHIFT_LAYER");
        rootTrack = project.getRootTrackGroup();
        this.preferences = preferences;
        panelLayout = this.preferences.getPanelLayout().get();
        this.preferences.getPanelLayout().addValueObserver((layoutOld, layoutNew) -> {
            panelLayout = layoutNew;
            horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
            verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
        });
    }

    @PostConstruct
    public void init(final HardwareElements hwElements) {
        final int numberOfScenes = 8;
        trackBank = viewControl.getTrackBank();
        final SceneBank sceneBank = trackBank.sceneBank();

        final Scene targetScene = trackBank.sceneBank().getScene(0);
        targetScene.clipCount().markInterested();
        sceneBank.setIndication(true);
        sceneBank.scrollPosition().addValueObserver(value -> sceneOffset = value);

        modifiers.getShiftActive().addValueObserver(shiftActive -> {
            if (!preferences.useShiftForAltMode() || panelLayout == PanelLayout.VERTICAL) {
                shiftLayer.setIsActive(shiftActive);
            }
        });

        for (int sceneIndex = 0; sceneIndex < numberOfScenes; sceneIndex++) {
            final SingleLedButton sceneButton = hwElements.getSceneButton(sceneIndex);
            final int index = sceneIndex;
            final Scene scene = sceneBank.getScene(index);
            scene.clipCount().markInterested();
            sceneButton.bindPressed(verticalLayer, () -> handleScenePressed(scene, index));
            sceneButton.bindRelease(verticalLayer, () -> handleSceneReleased(scene));
            sceneButton.bindLight(verticalLayer, () -> getSceneState(index, scene));
            final Track track = viewControl.getTrackBank().getItemAt(index);
            sceneButton.bindIsPressed(horizontalLayer, pressed -> handleTrackSelect(pressed, index, track));
            sceneButton.bindLight(horizontalLayer, () -> getTrackState(index, track));
        }

        for (int i = 0; i < 8; i++) {
            final int index = i;
            final RgbButton button = hwElements.getTrackSelectButton(i);
            final Track track = viewControl.getTrackBank().getItemAt(i);
            button.bindIsPressed(verticalLayer, pressed -> handleTrackSelect(pressed, index, track));
            button.bindLight(verticalLayer, () -> getTrackColor(index, track));
            final Scene scene = sceneBank.getScene(index);
            button.bindPressed(horizontalLayer, () -> handleScenePressed(scene, index));
            button.bindRelease(horizontalLayer, () -> handleSceneReleased(scene));
            button.bindLight(horizontalLayer, () -> getSceneColor(index, scene));
        }
        initLaunchQuantizeControl(transport, hwElements);
    }

    private void initLaunchQuantizeControl(final Transport transport, final HardwareElements hwElements) {
        transport.defaultLaunchQuantization().markInterested();
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final RgbButton button = hwElements.getTrackSelectButton(i);
            button.bindLight(shiftLayer, () -> quantizeState(index));
            button.bindIsPressed(shiftLayer, pressed -> selectLaunchQuantize(index, pressed));
        }
    }

    private void handleTrackSelect(final boolean pressed, final int index, final Track track) {
        if (!pressed) {
            return;
        }
        if (modifiers.isClear()) {
            track.deleteObject();
        } else if (modifiers.isDuplicate()) {
            track.duplicate();
        } else {
            track.selectInMixer();
        }
    }

    private void handleScenePressed(final Scene scene, final int index) {
        if (modifiers.getAltActive().get() && !modifiers.isShift()) {
            scene.launchAlt();
        } else if (modifiers.isShift()) {
            handleSpecial(index);
        } else if (modifiers.isClear()) {
            scene.deleteObject();
        } else if (modifiers.isDuplicate()) {
            // TODO This needs an API Change to work reliably
        } else {
            scene.launch();
        }
    }

    private void handleSpecial(final int index) {
        if (index == 7) {
            rootTrack.stop();
        }
    }

    private void handleSceneReleased(final Scene scene) {
        if (modifiers.getAltActive().get() && !modifiers.isShift()) {
            scene.launchReleaseAlt();
        } else {
            scene.launchRelease();
        }
    }

    private VarSingleLedState getSceneState(final int index, final Scene scene) {
        if (scene.clipCount().get() > 0) {
            if (viewControl.hasQueuedClips(sceneOffset + index)) {
                return VarSingleLedState.BLINK_8;
            }
            return VarSingleLedState.LIGHT_10;
        }
        return VarSingleLedState.OFF;
    }

    private RgbLightState getSceneColor(final int index, final Scene scene) {
        if (scene.clipCount().get() > 0) {
            if (viewControl.hasQueuedClips(sceneOffset + index)) {
                return RgbLightState.WHITE_BRIGHT.behavior(LedBehavior.BLINK_8);
            }
            return RgbLightState.WHITE_BRIGHT.behavior(LedBehavior.LIGHT_25);
        }
        return RgbLightState.OFF;
    }

    private RgbLightState getTrackColor(final int index, final Track track) {
        if (track.exists().get()) {
            if (index == viewControl.getSelectedTrackIndex()) {
                return RgbLightState.WHITE_BRIGHT;
            }
            return RgbLightState.of(viewControl.getTrackColor(index), LedBehavior.LIGHT_50);
        }
        return RgbLightState.OFF;
    }

    private VarSingleLedState getTrackState(final int index, final Track track) {
        if (track.exists().get()) {
            if (index == viewControl.getSelectedTrackIndex()) {
                return VarSingleLedState.FULL;
            }
            return VarSingleLedState.LIGHT_10;
        }
        return VarSingleLedState.OFF;
    }

    private void selectLaunchQuantize(final int index, final boolean pressed) {
        if (pressed) {
            final SettableEnumValue launchQuantizeValue = transport.defaultLaunchQuantization();
            if (launchQuantizeValue.get().equals(LAUNCH_QUANTIZE_VALUES[index])) {
                launchQuantizeValue.set("none");
                mainDisplay.activatePageDisplay(MainDisplay.ScreenMode.LAUNCH_QUANTIZE, "LaunchQuantize", "none");
            } else {
                launchQuantizeValue.set(LAUNCH_QUANTIZE_VALUES[index]);
                mainDisplay.activatePageDisplay(MainDisplay.ScreenMode.LAUNCH_QUANTIZE, "LaunchQuantize",
                        LAUNCH_QUANTIZE_DISPLAY_VALUES[index]);
            }
        } else {
            mainDisplay.notifyRelease();
        }
    }

    private RgbLightState quantizeState(final int index) {
        if (transport.defaultLaunchQuantization().get().equals(LAUNCH_QUANTIZE_VALUES[index])) {
            return RgbLightState.WHITE_BRIGHT;
        }
        return RgbLightState.WHITE_DIM;
    }

    @Override
    protected void onActivate() {
        super.onActivate();
        horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
        verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
        shiftLayer.setIsActive(
                modifiers.isShift() && (panelLayout == PanelLayout.VERTICAL || !preferences.useShiftForAltMode()));
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        horizontalLayer.setIsActive(false);
        verticalLayer.setIsActive(false);
        shiftLayer.setIsActive(false);
    }

}
