package com.bitwig.extensions.controllers.novation.launchpadpromk3.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.HwElements;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.LppPreferences;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.ModifierStates;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.ViewCursorControl;
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


    public SceneLaunchLayer(final Layers layers, final ViewCursorControl viewCursorControl,
                            final HwElements hwElements) {
        super(layers, "SCENE_LAYER");
        final TrackBank trackBank = viewCursorControl.getTrackBank();
        trackBank.setShouldShowClipLauncherFeedback(true);
        final SceneBank sceneBank = trackBank.sceneBank();
        sceneBank.scrollPosition().addValueObserver(value -> sceneOffset = value);
        initSceneControl(hwElements, sceneBank);
    }

    private void initSceneControl(final HwElements hwElements, final SceneBank sceneBank) {
        sceneBank.setIndication(true);
        sceneBank.cursorIndex().markInterested();
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Scene scene = sceneBank.getScene(index);
            final LabeledButton sceneButton = hwElements.getSceneLaunchButtons().get(index);
            scene.clipCount().markInterested();
            scene.color().addValueObserver((r, g, b) -> colorIndex[index] = ColorLookup.toColor(r, g, b));
            sceneButton.bindPressed(this, pressed -> handleScene(pressed, scene, index));
            sceneButton.bindLight(this, () -> getSceneColor(index, scene));
        }
    }

    @Inject
    public void setApplication(final Application application) {
        sceneCreateAction = application.getAction("Create Scene From Playing Launcher Clips");
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
                    viewCursorControl.focusScene(sceneIndex + sceneOffset);
                    scene.selectInEditor();
                    scene.launchAlt();
                } else {
                    scene.selectInEditor();
                    viewCursorControl.focusScene(sceneIndex + sceneOffset);
                }
            } else if (modifiers.noModifier()) {
                viewCursorControl.focusScene(sceneIndex + sceneOffset);
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
            if (sceneOffset + sceneIndex == viewCursorControl.getFocusSceneIndex() && viewCursorControl.hasQueuedForPlaying()) {
                return RgbState.GREEN_FLASH;
            }
            return RgbState.of(colorIndex[sceneIndex]);
        }
        return RgbState.OFF;
    }


}
