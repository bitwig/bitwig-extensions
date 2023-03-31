package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchpadmini3.TrackState;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;

public abstract class TrackSliderLayer extends AbstractSliderLayer {

    protected TrackState trackState;

    public TrackSliderLayer(final String name, final HardwareSurface controlSurface, final Layers layers,
                            final MidiProcessor midiProcessor, final int baseCcNr, final int baseColor) {
        super(name, controlSurface, layers, midiProcessor, baseCcNr, baseColor);
    }

    @Inject
    public void setTrackState(final TrackState trackState) {
        this.trackState = trackState;
        trackState.addExistsListener((trackIndex, exists) -> updateFaderState());
        trackState.addColorStateListener((trackIndex, color) -> updateFaderState());
    }

    protected abstract void bind(TrackBank trackBank);

    protected void updateFaderState() {
        if (isActive()) {
            refreshTrackColors();
            //modeHandler.setFaderBank(mode == ControlMode.PAN ? 1 : 0, mode, tracksExistsColors);
            valueBindings.forEach(SliderBinding::update);
        }
    }

    @Override
    protected void refreshTrackColors() {
        final boolean[] exists = trackState.getExists();

        for (int i = 0; i < 8; i++) {
            if (exists[i]) {
                tracksExistsColors[i] = baseColor;
            } else {
                tracksExistsColors[i] = 0;
            }
        }
    }

}
