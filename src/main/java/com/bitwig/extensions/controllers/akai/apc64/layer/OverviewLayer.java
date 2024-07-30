package com.bitwig.extensions.controllers.akai.apc64.layer;

import com.bitwig.extensions.controllers.akai.apc.common.control.RgbButton;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apc64.HardwareElements;
import com.bitwig.extensions.controllers.akai.apc64.ViewControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;


public class OverviewLayer extends Layer {

    private final ViewControl viewControl;

    public OverviewLayer(final Layers layers, ViewControl viewControl, HardwareElements hwElements) {
        super(layers, "OVERVIEW_LAYER");
        this.viewControl = viewControl;
        for (int i = 0; i < 8; i++) {
            final int trackIndex = i;
            for (int j = 0; j < 8; j++) {
                final int sceneIndex = j;
                final RgbButton button = hwElements.getGridButton(sceneIndex, trackIndex);
                button.bindPressed(this, () -> handleSelection(trackIndex, sceneIndex));
                button.bindLight(this, () -> getState(trackIndex, sceneIndex));
            }
        }
    }

    private void handleSelection(final int trackIndex, final int sceneIndex) {
        viewControl.scrollToOverview(trackIndex, sceneIndex);
    }

    private RgbLightState getState(final int trackIndex, final int sceneIndex) {
        if (viewControl.inOverviewGridFocus(trackIndex, sceneIndex)) {
            if (viewControl.hasClips(trackIndex, sceneIndex)) {
                return RgbLightState.ORANGE_SEL;
            }
            return RgbLightState.WHITE_SEL;
        }
        if (viewControl.hasClips(trackIndex, sceneIndex)) {
            return RgbLightState.ORANGE_FULL;
        }
        if (viewControl.inOverviewGrid(trackIndex, sceneIndex)) {
            return RgbLightState.WHITE_DIM;
        }
        return RgbLightState.OFF;
    }
}