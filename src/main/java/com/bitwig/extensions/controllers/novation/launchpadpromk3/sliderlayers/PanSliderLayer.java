package com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.HwElements;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.LpBaseMode;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.SysExHandler;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.ViewCursorControl;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.ControlMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class PanSliderLayer extends TrackSliderLayer {
    private final Layer sceneButtonLayer;

    public PanSliderLayer(final ViewCursorControl viewCursorControl, final HardwareSurface controlSurface,
                          final Layers layers, final MidiProcessor midiProcessor, final SysExHandler sysExHandler,
                          final HwElements hwElements) {
        super("PAN", ControlMode.PAN, controlSurface, layers, midiProcessor, sysExHandler);
        bind(viewCursorControl.getTrackBank());
        sceneButtonLayer = new Layer(layers, "VOL_SCENE_BUTTONS");
        initSceneButtons(hwElements);
    }

    @Override
    protected void bind(final TrackBank trackBank) {
        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i);
            final SliderBinding binding = new SliderBinding(mode.getCcNr(), track.pan(), sliders[i], i, midiProcessor);
            addBinding(binding);
            valueBindings.add(binding);
        }
    }

    @Override
    protected void refreshTrackColors() {
        final boolean[] exists = trackState.getExists();
        final int[] colorIndex = trackState.getColorIndex();

        for (int i = 0; i < 8; i++) {
            if (exists[i]) {
                tracksExistsColors[i] = colorIndex[i];
            } else {
                tracksExistsColors[i] = 0;
            }
        }
    }

    private void initSceneButtons(final HwElements hwElements) {
        for (int index = 0; index < 8; index++) {
            final LabeledButton sceneButton = hwElements.getSceneLaunchButtons().get(index);
            sceneButton.disable(sceneButtonLayer);
        }
    }


    @Override
    protected void onActivate() {
        super.onActivate();
        refreshTrackColors();
        sceneButtonLayer.setIsActive(true);
        modeHandler.setFaderBank(1, mode, tracksExistsColors);
        modeHandler.changeMode(LpBaseMode.FADER, mode.getBankId());
    }


    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        sceneButtonLayer.setIsActive(false);
        updateFaderState();
    }

}
