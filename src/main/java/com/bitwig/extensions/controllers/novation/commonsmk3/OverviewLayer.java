package com.bitwig.extensions.controllers.novation.commonsmk3;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

public class OverviewLayer extends Layer {
    
    @Inject
    private ViewCursorControl viewCursorControl;
    
    public OverviewLayer(final Layers layers) {
        super(layers, "SESSION_OVERVIEW_LAYER");
    }
    
    @PostConstruct
    public void initView(final LpHwElements hwElements, final ViewCursorControl viewCursorControl) {
        initClipControl(hwElements);
        initSceneButtons(hwElements);
    }
    
    private void initClipControl(final LpHwElements hwElements) {
        for (int i = 0; i < 8; i++) {
            final int trackIndex = i;
            for (int j = 0; j < 8; j++) {
                final int sceneIndex = j;
                final GridButton button = hwElements.getGridButton(sceneIndex, trackIndex);
                button.bindPressed(this, () -> handleSelection(trackIndex, sceneIndex));
                button.bindLight(this, () -> getState(trackIndex, sceneIndex));
            }
        }
    }
    
    private void initSceneButtons(final LpHwElements hwElements) {
        for (int i = 0; i < 8; i++) {
            final LabeledButton sceneButton = hwElements.getSceneLaunchButtons().get(i);
            sceneButton.disable(this);
        }
    }
    
    private void handleSelection(final int trackIndex, final int sceneIndex) {
        viewCursorControl.scrollToOverview(trackIndex, sceneIndex);
    }
    
    private RgbState getState(final int trackIndex, final int sceneIndex) {
        if (viewCursorControl.inOverviewGridFocus(trackIndex, sceneIndex)) {
            if (viewCursorControl.hasClips(trackIndex, sceneIndex)) {
                return RgbState.of(21);
            }
            return RgbState.of(23);
        }
        if (viewCursorControl.hasClips(trackIndex, sceneIndex)) {
            return RgbState.of(41);
        }
        if (viewCursorControl.inOverviewGrid(trackIndex, sceneIndex)) {
            return RgbState.of(1);
        }
        return RgbState.OFF;
    }
    
}
