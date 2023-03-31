package com.bitwig.extensions.controllers.novation.launchpadpromk3.layers;

import com.bitwig.extensions.controllers.novation.commonsmk3.GridButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.HwElements;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.ViewCursorControl;
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
    public void initView(final HwElements hwElements, final ViewCursorControl viewCursorControl) {
        initClipControl(hwElements);
    }

    private void initClipControl(final HwElements hwElements) {
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

    private void handleSelection(final int trackIndex, final int sceneIndex) {
        viewCursorControl.scrollToOverview(trackIndex, sceneIndex);
    }

    private RgbState getState(final int trackIndex, final int sceneIndex) {
        if (viewCursorControl.inOverviewGridFocus(trackIndex, sceneIndex)) {
            return RgbState.of(21);
        }
        if (viewCursorControl.inOverviewGrid(trackIndex, sceneIndex)) {
            return RgbState.of(8);
        }
        return RgbState.OFF;
    }

}
