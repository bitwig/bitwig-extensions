package com.bitwig.extensions.controllers.novation.launchpadpromk3.layers;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.PanelLayout;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.commonsmk3.ViewCursorControl;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.LpProHwElements;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.LppPreferences;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.ModifierStates;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;

public class SceneLaunchLayer extends Layer {
    @Inject
    private ModifierStates modifiers;
    @Inject
    private ViewCursorControl viewCursorControl;
    @Inject
    private LppPreferences preferences;
    
    private Action sceneCreateAction;
    private final int[] colorIndex = new int[8];
    private int sceneOffset;
    private final Layer verticalLayer;
    private final Layer horizontalLayer;
    private PanelLayout panelLayout = PanelLayout.VERTICAL;
    
    public SceneLaunchLayer(final Layers layers, final ViewCursorControl viewCursorControl,
        final LpProHwElements hwElements, final LppPreferences preferences) {
        super(layers, "SCENE_LAYER");
        verticalLayer = new Layer(layers, "SCENE_VERTICAL");
        horizontalLayer = new Layer(layers, "SCENE_HORIZONTAL");
        
        final TrackBank trackBank = viewCursorControl.getTrackBank();
        trackBank.setShouldShowClipLauncherFeedback(true);
        final SceneBank sceneBank = trackBank.sceneBank();
        sceneBank.scrollPosition().addValueObserver(value -> sceneOffset = value);
        initSceneControl(hwElements, sceneBank);
        preferences.getPanelLayout().addValueObserver((newValue -> setLayout(newValue)));
        panelLayout = preferences.getPanelLayout().get();
    }
    
    private void initSceneControl(final LpProHwElements hwElements, final SceneBank sceneBank) {
        sceneBank.setIndication(true);
        sceneBank.cursorIndex().markInterested();
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Scene scene = sceneBank.getScene(index);
            final LabeledButton sceneButton = hwElements.getSceneLaunchButtons().get(index);
            scene.clipCount().markInterested();
            scene.color().addValueObserver((r, g, b) -> colorIndex[index] = ColorLookup.toColor(r, g, b));
            sceneButton.bindPressed(verticalLayer, pressed -> handleScene(pressed, scene, index));
            sceneButton.bindLight(verticalLayer, () -> getSceneColor(index, scene));
            final LabeledButton trackButton = hwElements.getTrackSelectButtons().get(index);
            trackButton.bindPressed(horizontalLayer, pressed -> handleScene(pressed, scene, index));
            trackButton.bindLight(horizontalLayer, () -> getSceneColor(index, scene));
        }
    }
    
    @Inject
    public void setApplication(final Application application) {
        sceneCreateAction = application.getAction("Create Scene From Playing Launcher Clips");
    }
    
    public void setLayout(final PanelLayout layout) {
        panelLayout = layout;
        horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
        verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
    }
    
    private void handleScene(final boolean pressed, final Scene scene, final int sceneIndex) {
        if (pressed) {
            if (modifiers.isClear()) {
                scene.deleteObject();
            } else if (modifiers.isDuplicate()) {
                if (modifiers.isShift()) {
                    sceneCreateAction.invoke();
                } else {
                    scene.nextSceneInsertionPoint().copySlotsOrScenes(scene);
                }
            } else if (modifiers.onlyShift()) {
                if (preferences.getAltModeWithShift().get()) {
                    scene.selectInEditor();
                    scene.launchAlt();
                } else {
                    scene.selectInEditor();
                }
            } else if (modifiers.noModifier()) {
                scene.launch();
            }
        } else {
            if (modifiers.onlyShift()) {
                scene.launchReleaseAlt();
            } else if (modifiers.noModifier() && preferences.getAltModeWithShift().get()) {
                scene.launchRelease();
            }
        }
    }
    
    private RgbState getSceneColor(final int sceneIndex, final Scene scene) {
        if (scene.clipCount().get() > 0) {
            if (viewCursorControl.hasQueuedForPlaying(sceneOffset + sceneIndex)) {
                return RgbState.GREEN_FLASH;
            }
            return RgbState.of(colorIndex[sceneIndex]);
        }
        return RgbState.OFF;
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
        verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
    }
    
    @Override
    protected void onDeactivate() {
        horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
        verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
    }
}
